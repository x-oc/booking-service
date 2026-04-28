#!/bin/bash

BASE_URL="${BASE_URL:-http://localhost:28800}"
WORKER_SERVICE="${WORKER_SERVICE:-app-worker}"
CONTENT_TYPE="application/json"
TMP_BODY_FILE=$(mktemp)
trap 'rm -f "$TMP_BODY_FILE"' EXIT

print_block() {
  echo ""
  echo "============================================================"
  echo "$1"
  echo "============================================================"
}

extract_json_value() {
  local body="$1"
  local key="$2"
  echo "$body" | sed -n "s/.*\"${key}\":\"\([^\"]*\)\".*/\1/p" | head -n1
}

extract_json_number() {
  local body="$1"
  local key="$2"
  echo "$body" | sed -n "s/.*\"${key}\":\([0-9][0-9]*\).*/\1/p" | head -n1
}

call_api() {
  local label="$1"
  local method="$2"
  local url="$3"
  local auth_header="$4"
  local body="$5"

  print_block "$label"
  echo "REQUEST: ${method} ${url}"

  local code
  if [ -n "$body" ] && [ -n "$auth_header" ]; then
    code=$(curl -s -o "$TMP_BODY_FILE" -w "%{http_code}" -X "$method" "$url" \
      -H "Content-Type: ${CONTENT_TYPE}" \
      -H "Authorization: Bearer ${auth_header}" \
      -d "$body")
  elif [ -n "$body" ]; then
    code=$(curl -s -o "$TMP_BODY_FILE" -w "%{http_code}" -X "$method" "$url" \
      -H "Content-Type: ${CONTENT_TYPE}" \
      -d "$body")
  elif [ -n "$auth_header" ]; then
    code=$(curl -s -o "$TMP_BODY_FILE" -w "%{http_code}" -X "$method" "$url" \
      -H "Authorization: Bearer ${auth_header}")
  else
    code=$(curl -s -o "$TMP_BODY_FILE" -w "%{http_code}" -X "$method" "$url")
  fi

  LAST_CODE="$code"
  LAST_BODY="$(cat "$TMP_BODY_FILE")"
  echo "RESPONSE CODE: ${LAST_CODE}"
  echo "RESPONSE BODY: ${LAST_BODY}"
}

print_block "PRECHECK"
if ! docker compose ps --status running | grep -q "app-worker"; then
  echo "Service app-worker is not running"
  exit 1
fi

ts=$(date +%s)
GUEST_EMAIL="async_guest_${ts}@test.com"
HOST_EMAIL="async_host_${ts}@test.com"
PASS="password123"
CHECK_IN=$(date -d "+5 days" +%F)
CHECK_OUT=$(date -d "+7 days" +%F)

call_api "Register guest" "POST" "${BASE_URL}/api/auth/register" "" \
  "{\"email\":\"${GUEST_EMAIL}\",\"password\":\"${PASS}\",\"firstName\":\"Guest\",\"lastName\":\"Async\",\"phoneNumber\":\"+79000000011\",\"role\":\"GUEST\"}"

call_api "Register host" "POST" "${BASE_URL}/api/auth/register" "" \
  "{\"email\":\"${HOST_EMAIL}\",\"password\":\"${PASS}\",\"firstName\":\"Host\",\"lastName\":\"Async\",\"phoneNumber\":\"+79000000012\",\"role\":\"HOST\"}"

call_api "Login guest" "POST" "${BASE_URL}/api/auth/login" "" \
  "{\"email\":\"${GUEST_EMAIL}\",\"password\":\"${PASS}\"}"
GUEST_TOKEN=$(extract_json_value "$LAST_BODY" "token")

call_api "Login host" "POST" "${BASE_URL}/api/auth/login" "" \
  "{\"email\":\"${HOST_EMAIL}\",\"password\":\"${PASS}\"}"
HOST_TOKEN=$(extract_json_value "$LAST_BODY" "token")

call_api "Create property" "POST" "${BASE_URL}/api/v1/properties" "$HOST_TOKEN" \
  "{\"title\":\"Async Test Flat\",\"address\":\"Tverskaya 10\",\"basePricePerDay\":2500.00}"
PROPERTY_ID=$(extract_json_number "$LAST_BODY" "id")

call_api "Create booking" "POST" "${BASE_URL}/api/v1/bookings" "$GUEST_TOKEN" \
  "{\"propertyId\":${PROPERTY_ID},\"checkInDate\":\"${CHECK_IN}\",\"checkOutDate\":\"${CHECK_OUT}\"}"
BOOKING_ID=$(extract_json_number "$LAST_BODY" "id")

call_api "Confirm booking" "PATCH" "${BASE_URL}/api/v1/bookings/${BOOKING_ID}/confirm" "$HOST_TOKEN" ""

call_api "Queue payment" "POST" "${BASE_URL}/api/v1/bookings/${BOOKING_ID}/pay" "$GUEST_TOKEN" ""
if [ "$LAST_CODE" != "202" ]; then
  echo "Expected 202 from async pay endpoint, got ${LAST_CODE}"
  exit 1
fi

STATUS_NOW=$(extract_json_value "$LAST_BODY" "status")
echo "Current status right after /pay: ${STATUS_NOW}"
if [ "$STATUS_NOW" != "PAYMENT_PROCESSING" ]; then
  echo "Expected status PAYMENT_PROCESSING immediately after queueing payment"
  exit 1
fi

print_block "WAIT FOR WORKER RESULT"
FINAL_STATUS=""
for i in $(seq 1 30); do
  sleep 2
  call_api "Poll booking status attempt ${i}" "GET" "${BASE_URL}/api/v1/bookings/${BOOKING_ID}" "$GUEST_TOKEN" ""
  STATUS_NOW=$(extract_json_value "$LAST_BODY" "status")
  if [ "$STATUS_NOW" = "PAID" ] || [ "$STATUS_NOW" = "ACTIVE" ] || [ "$STATUS_NOW" = "CANCELLED_EXPIRED" ]; then
    FINAL_STATUS="$STATUS_NOW"
    break
  fi
done

if [ -z "$FINAL_STATUS" ]; then
  echo "Payment was not processed in expected time"
  exit 1
fi

echo "Final status after async processing: ${FINAL_STATUS}"

print_block "VERIFY WORKER LOGS"
if docker compose logs "$WORKER_SERVICE" --tail 300 | grep -q "processing payment task for booking ${BOOKING_ID}"; then
  echo "PASS: Payment task processed by ${WORKER_SERVICE}"
else
  echo "FAIL: Worker logs do not contain payment processing evidence for booking ${BOOKING_ID}"
  exit 1
fi

print_block "RESULT"
echo "PASS: async payment was queued on API instance and processed on worker instance"
