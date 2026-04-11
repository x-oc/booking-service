#!/bin/bash

BASE_URL="${BASE_URL:-http://localhost:28800}"
CONTENT_TYPE="application/json"

timestamp=$(date +%s)
GUEST_EMAIL="guest_${timestamp}@test.com"
HOST_EMAIL="host_${timestamp}@test.com"
ADMIN_EMAIL="admin_${timestamp}@test.com"
PASS="password123"

GUEST2_EMAIL="guest2_${timestamp}@test.com"
HOST2_EMAIL="host2_${timestamp}@test.com"

TMP_BODY_FILE=$(mktemp)
trap 'rm -f "$TMP_BODY_FILE"' EXIT

date_plus() {
  local days="$1"
  date -d "+${days} days" +%F
}

require_id() {
  local id_value="$1"
  local label="$2"
  if [ -z "$id_value" ]; then
    echo "ERROR: ${label} is empty. Previous response: ${LAST_BODY}"
    exit 1
  fi
}

BOOKING1_CHECKIN=$(date_plus 7)
BOOKING1_CHECKOUT=$(date_plus 12)
BOOKING2_CHECKIN=$(date_plus 20)
BOOKING2_CHECKOUT=$(date_plus 24)
BOOKING3_CHECKIN=$(date_plus 30)
BOOKING3_CHECKOUT=$(date_plus 35)
BOOKING3_UPDATE_CHECKIN=$(date_plus 31)
BOOKING3_UPDATE_CHECKOUT=$(date_plus 36)
BOOKING_ADMIN_NEG_CHECKIN=$(date_plus 40)
BOOKING_ADMIN_NEG_CHECKOUT=$(date_plus 44)

print_block() {
  echo ""
  echo "============================================================"
  echo "$1"
  echo "============================================================"
}

extract_json_value() {
  local body="$1"
  local key="$2"
  echo "$body" | sed -n "s/.*\"${key}\":\"\\([^\"]*\\)\".*/\\1/p" | head -n1
}

extract_json_number() {
  local body="$1"
  local key="$2"
  echo "$body" | sed -n "s/.*\"${key}\":\\([0-9][0-9]*\\).*/\\1/p" | head -n1
}

call_api() {
  local label="$1"
  local method="$2"
  local url="$3"
  local auth_header="$4"
  local body="$5"

  print_block "$label"
  echo "REQUEST: ${method} ${url}"
  if [ -n "$auth_header" ]; then
    echo "AUTH: ${auth_header}"
  fi
  if [ -n "$body" ]; then
    echo "BODY: ${body}"
  fi

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

print_block "START"
echo "BASE_URL=${BASE_URL}"

# -------------------- AUTH --------------------
call_api "AUTH register guest" "POST" "${BASE_URL}/api/auth/register" "" \
  "{\"email\":\"${GUEST_EMAIL}\",\"password\":\"${PASS}\",\"firstName\":\"Guest\",\"lastName\":\"One\",\"phoneNumber\":\"+79001111111\",\"role\":\"GUEST\"}"

call_api "AUTH register host" "POST" "${BASE_URL}/api/auth/register" "" \
  "{\"email\":\"${HOST_EMAIL}\",\"password\":\"${PASS}\",\"firstName\":\"Host\",\"lastName\":\"One\",\"phoneNumber\":\"+79002222222\",\"role\":\"HOST\"}"

call_api "AUTH register admin" "POST" "${BASE_URL}/api/auth/register" "" \
  "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${PASS}\",\"firstName\":\"Admin\",\"lastName\":\"One\",\"phoneNumber\":\"+79003333333\",\"role\":\"ADMIN\"}"

call_api "AUTH register guest2" "POST" "${BASE_URL}/api/auth/register" "" \
  "{\"email\":\"${GUEST2_EMAIL}\",\"password\":\"${PASS}\",\"firstName\":\"Guest\",\"lastName\":\"Two\",\"phoneNumber\":\"+79004444444\",\"role\":\"GUEST\"}"

call_api "AUTH register host2" "POST" "${BASE_URL}/api/auth/register" "" \
  "{\"email\":\"${HOST2_EMAIL}\",\"password\":\"${PASS}\",\"firstName\":\"Host\",\"lastName\":\"Two\",\"phoneNumber\":\"+79005555555\",\"role\":\"HOST\"}"

call_api "AUTH login guest" "POST" "${BASE_URL}/api/auth/login" "" \
  "{\"email\":\"${GUEST_EMAIL}\",\"password\":\"${PASS}\"}"
GUEST_TOKEN=$(extract_json_value "$LAST_BODY" "token")
GUEST_ID=$(extract_json_number "$LAST_BODY" "id")

call_api "AUTH login host" "POST" "${BASE_URL}/api/auth/login" "" \
  "{\"email\":\"${HOST_EMAIL}\",\"password\":\"${PASS}\"}"
HOST_TOKEN=$(extract_json_value "$LAST_BODY" "token")
HOST_ID=$(extract_json_number "$LAST_BODY" "id")

call_api "AUTH login admin" "POST" "${BASE_URL}/api/auth/login" "" \
  "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${PASS}\"}"
ADMIN_TOKEN=$(extract_json_value "$LAST_BODY" "token")
ADMIN_ID=$(extract_json_number "$LAST_BODY" "id")

call_api "AUTH login guest2" "POST" "${BASE_URL}/api/auth/login" "" \
  "{\"email\":\"${GUEST2_EMAIL}\",\"password\":\"${PASS}\"}"
GUEST2_TOKEN=$(extract_json_value "$LAST_BODY" "token")
GUEST2_ID=$(extract_json_number "$LAST_BODY" "id")

call_api "AUTH login host2" "POST" "${BASE_URL}/api/auth/login" "" \
  "{\"email\":\"${HOST2_EMAIL}\",\"password\":\"${PASS}\"}"
HOST2_TOKEN=$(extract_json_value "$LAST_BODY" "token")
HOST2_ID=$(extract_json_number "$LAST_BODY" "id")

echo ""
echo "TOKENS/IDS:"
echo "  guest:  id=${GUEST_ID}, token_len=${#GUEST_TOKEN}"
echo "  host:   id=${HOST_ID}, token_len=${#HOST_TOKEN}"
echo "  admin:  id=${ADMIN_ID}, token_len=${#ADMIN_TOKEN}"
echo "  guest2: id=${GUEST2_ID}, token_len=${#GUEST2_TOKEN}"
echo "  host2:  id=${HOST2_ID}, token_len=${#HOST2_TOKEN}"

# -------------------- PROPERTIES --------------------
call_api "PROPERTIES create property #1 by host" "POST" "${BASE_URL}/api/v1/properties" "${HOST_TOKEN}" \
  "{\"title\":\"Loft Test 1\",\"address\":\"Nevsky 100\",\"basePricePerDay\":2000.00}"
PROPERTY_ID=$(extract_json_number "$LAST_BODY" "id")
require_id "$PROPERTY_ID" "PROPERTY_ID"

call_api "PROPERTIES create property #2 by host2" "POST" "${BASE_URL}/api/v1/properties" "${HOST2_TOKEN}" \
  "{\"title\":\"Studio Test 2\",\"address\":\"Rubinshteina 20\",\"basePricePerDay\":1800.00}"
PROPERTY2_ID=$(extract_json_number "$LAST_BODY" "id")
require_id "$PROPERTY2_ID" "PROPERTY2_ID"

call_api "PROPERTIES get current host properties" "GET" "${BASE_URL}/api/v1/properties/host" "${HOST_TOKEN}" ""
call_api "PROPERTIES get available properties" "GET" "${BASE_URL}/api/v1/properties" "${GUEST_TOKEN}" ""
call_api "PROPERTIES get property by id" "GET" "${BASE_URL}/api/v1/properties/${PROPERTY_ID}" "${GUEST_TOKEN}" ""

# -------------------- BOOKINGS --------------------
# create booking (main flow)
call_api "BOOKINGS create #1 (guest -> host)" "POST" "${BASE_URL}/api/v1/bookings" "${GUEST_TOKEN}" \
  "{\"propertyId\":${PROPERTY_ID},\"checkInDate\":\"${BOOKING1_CHECKIN}\",\"checkOutDate\":\"${BOOKING1_CHECKOUT}\"}"
BOOKING_ID=$(extract_json_number "$LAST_BODY" "id")
require_id "$BOOKING_ID" "BOOKING_ID"

# create booking for reject flow
call_api "BOOKINGS create #2 (will reject)" "POST" "${BASE_URL}/api/v1/bookings" "${GUEST2_TOKEN}" \
  "{\"propertyId\":${PROPERTY2_ID},\"checkInDate\":\"${BOOKING2_CHECKIN}\",\"checkOutDate\":\"${BOOKING2_CHECKOUT}\"}"
BOOKING2_ID=$(extract_json_number "$LAST_BODY" "id")
require_id "$BOOKING2_ID" "BOOKING2_ID"

# create mutable booking for update/delete
call_api "BOOKINGS create #3 (for update/delete)" "POST" "${BASE_URL}/api/v1/bookings" "${GUEST_TOKEN}" \
  "{\"propertyId\":${PROPERTY2_ID},\"checkInDate\":\"${BOOKING3_CHECKIN}\",\"checkOutDate\":\"${BOOKING3_CHECKOUT}\"}"
BOOKING3_ID=$(extract_json_number "$LAST_BODY" "id")
require_id "$BOOKING3_ID" "BOOKING3_ID"

# -------------------- ACCESS NEGATIVE CHECKS (expected 403) --------------------
call_api "SECURITY host cannot pay booking (expected 403)" "POST" "${BASE_URL}/api/v1/bookings/${BOOKING_ID}/pay" "${HOST_TOKEN}" ""
call_api "SECURITY guest cannot force status (expected 403)" "PATCH" "${BASE_URL}/api/v1/bookings/${BOOKING_ID}/force-status" "${GUEST_TOKEN}" \
  "{\"status\":\"FORCED_COMPLETED\"}"
call_api "SECURITY admin cannot create booking as guest flow (expected 403)" "POST" "${BASE_URL}/api/v1/bookings" "${ADMIN_TOKEN}" \
  "{\"propertyId\":${PROPERTY_ID},\"checkInDate\":\"${BOOKING_ADMIN_NEG_CHECKIN}\",\"checkOutDate\":\"${BOOKING_ADMIN_NEG_CHECKOUT}\"}"

call_api "BOOKINGS update #3 by guest" "PUT" "${BASE_URL}/api/v1/bookings/${BOOKING3_ID}" "${GUEST_TOKEN}" \
  "{\"propertyId\":${PROPERTY2_ID},\"checkInDate\":\"${BOOKING3_UPDATE_CHECKIN}\",\"checkOutDate\":\"${BOOKING3_UPDATE_CHECKOUT}\"}"
call_api "BOOKINGS delete #3 by guest" "DELETE" "${BASE_URL}/api/v1/bookings/${BOOKING3_ID}" "${GUEST_TOKEN}" ""

call_api "BOOKINGS get by id #1 (guest)" "GET" "${BASE_URL}/api/v1/bookings/${BOOKING_ID}" "${GUEST_TOKEN}" ""
call_api "BOOKINGS confirm #1 by host" "PATCH" "${BASE_URL}/api/v1/bookings/${BOOKING_ID}/confirm" "${HOST_TOKEN}" ""
call_api "BOOKINGS pay #1 by guest (may fail 20%)" "POST" "${BASE_URL}/api/v1/bookings/${BOOKING_ID}/pay" "${GUEST_TOKEN}" ""

call_api "BOOKINGS reject #2 by host2" "PATCH" "${BASE_URL}/api/v1/bookings/${BOOKING2_ID}/reject" "${HOST2_TOKEN}" ""

# support request flow (requires paid/active status, so we try after pay)
call_api "BOOKINGS support request #1 by guest" "POST" "${BASE_URL}/api/v1/bookings/${BOOKING_ID}/support-request" "${GUEST_TOKEN}" ""
call_api "BOOKINGS support requests list by admin" "GET" "${BASE_URL}/api/v1/bookings/support-requests?page=0&size=10&sortBy=createdAt&direction=DESC" "${ADMIN_TOKEN}" ""
call_api "BOOKINGS support request reject #1 by admin" "POST" "${BASE_URL}/api/v1/bookings/${BOOKING_ID}/support-request/reject" "${ADMIN_TOKEN}" ""
call_api "BOOKINGS support request #1 by guest (again)" "POST" "${BASE_URL}/api/v1/bookings/${BOOKING_ID}/support-request" "${GUEST_TOKEN}" ""
call_api "BOOKINGS support request process #1 by admin" "POST" "${BASE_URL}/api/v1/bookings/${BOOKING_ID}/support-request/process" "${ADMIN_TOKEN}" ""

# force status flow (admin)
call_api "BOOKINGS force status #2 by admin to FORCED_COMPLETED" "PATCH" "${BASE_URL}/api/v1/bookings/${BOOKING2_ID}/force-status" "${ADMIN_TOKEN}" \
  "{\"status\":\"FORCED_COMPLETED\"}"

# list/filter endpoints
call_api "BOOKINGS /host list (host)" "GET" "${BASE_URL}/api/v1/bookings/host?page=0&size=10&sortBy=createdAt&direction=DESC" "${HOST_TOKEN}" ""
call_api "BOOKINGS /host filter by guestId+status+date" "GET" "${BASE_URL}/api/v1/bookings/host?guestId=${GUEST_ID}&status=PAID&date=2026-04-12&page=0&size=10&sortBy=createdAt&direction=DESC" "${HOST_TOKEN}" ""
call_api "BOOKINGS /host filter by guestEmail" "GET" "${BASE_URL}/api/v1/bookings/host?guestEmail=${GUEST_EMAIL}&page=0&size=10&sortBy=createdAt&direction=DESC" "${HOST_TOKEN}" ""

call_api "BOOKINGS /guest list (guest)" "GET" "${BASE_URL}/api/v1/bookings/guest?page=0&size=10&sortBy=createdAt&direction=DESC" "${GUEST_TOKEN}" ""
call_api "BOOKINGS /guest filter by hostId+status+date" "GET" "${BASE_URL}/api/v1/bookings/guest?hostId=${HOST_ID}&status=PAID&date=2026-04-12&page=0&size=10&sortBy=createdAt&direction=DESC" "${GUEST_TOKEN}" ""
call_api "BOOKINGS /guest filter by hostEmail" "GET" "${BASE_URL}/api/v1/bookings/guest?hostEmail=${HOST_EMAIL}&page=0&size=10&sortBy=createdAt&direction=DESC" "${GUEST_TOKEN}" ""

print_block "DONE"
echo "Script finished. Review response codes/bodies above."



