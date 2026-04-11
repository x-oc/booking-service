#!/bin/bash

BASE_URL="${BASE_URL:-http://localhost:28800}"
CONTENT_TYPE="application/json"

CORE_DB_SERVICE="${CORE_DB_SERVICE:-db-core}"
PROPERTY_DB_SERVICE="${PROPERTY_DB_SERVICE:-db-property}"

TMP_BODY_FILE=$(mktemp)
trap 'rm -f "$TMP_BODY_FILE"' EXIT

FAILURES=0

print_block() {
  echo ""
  echo "============================================================"
  echo "$1"
  echo "============================================================"
}

pass() {
  echo "PASS: $1"
}

fail() {
  echo "FAIL: $1"
  FAILURES=$((FAILURES + 1))
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

extract_probe_property_id() {
  local text="$1"
  echo "$text" | sed -n 's/.*propertyId=\([0-9][0-9]*\).*/\1/p' | head -n1
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

db_query_property() {
  local sql="$1"
  docker compose exec -T "$PROPERTY_DB_SERVICE" psql -U airbnb -d airbnb_property -t -A -c "$sql"
}

db_query_core() {
  local sql="$1"
  docker compose exec -T "$CORE_DB_SERVICE" psql -U airbnb -d airbnb_core -t -A -c "$sql"
}

print_block "PRECHECK"
if ! docker compose ps --status running | grep -q "$CORE_DB_SERVICE"; then
  echo "Service $CORE_DB_SERVICE is not running"
  exit 1
fi
if ! docker compose ps --status running | grep -q "$PROPERTY_DB_SERVICE"; then
  echo "Service $PROPERTY_DB_SERVICE is not running"
  exit 1
fi
pass "Both database containers are running"

print_block "AUTH SETUP"
ts=$(date +%s)
GUEST_EMAIL="tx_guest_${ts}@test.com"
HOST_EMAIL="tx_host_${ts}@test.com"
ADMIN_EMAIL="tx_admin_${ts}@test.com"
PASSWD="password123"

call_api "Register guest" "POST" "${BASE_URL}/api/auth/register" "" \
  "{\"email\":\"${GUEST_EMAIL}\",\"password\":\"${PASSWD}\",\"firstName\":\"Guest\",\"lastName\":\"Tx\",\"phoneNumber\":\"+79000000001\",\"role\":\"GUEST\"}"
call_api "Register host" "POST" "${BASE_URL}/api/auth/register" "" \
  "{\"email\":\"${HOST_EMAIL}\",\"password\":\"${PASSWD}\",\"firstName\":\"Host\",\"lastName\":\"Tx\",\"phoneNumber\":\"+79000000002\",\"role\":\"HOST\"}"
call_api "Register admin" "POST" "${BASE_URL}/api/auth/register" "" \
  "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${PASSWD}\",\"firstName\":\"Admin\",\"lastName\":\"Tx\",\"phoneNumber\":\"+79000000003\",\"role\":\"ADMIN\"}"

call_api "Login guest" "POST" "${BASE_URL}/api/auth/login" "" \
  "{\"email\":\"${GUEST_EMAIL}\",\"password\":\"${PASSWD}\"}"
GUEST_ID=$(extract_json_number "$LAST_BODY" "id")

call_api "Login host" "POST" "${BASE_URL}/api/auth/login" "" \
  "{\"email\":\"${HOST_EMAIL}\",\"password\":\"${PASSWD}\"}"
HOST_ID=$(extract_json_number "$LAST_BODY" "id")

call_api "Login admin" "POST" "${BASE_URL}/api/auth/login" "" \
  "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${PASSWD}\"}"
ADMIN_TOKEN=$(extract_json_value "$LAST_BODY" "token")

if [ -n "$GUEST_ID" ] && [ -n "$HOST_ID" ] && [ -n "$ADMIN_TOKEN" ]; then
  pass "Auth setup complete"
else
  fail "Auth setup failed"
  echo "Cannot continue without IDs/token"
  exit 1
fi

print_block "SCENARIO 1: COMMIT (forceFailure=false)"
PROBE1="probe_success_${ts}"
call_api "TX probe commit" "POST" "${BASE_URL}/api/v1/bookings/admin/tx-probe" "$ADMIN_TOKEN" \
  "{\"hostId\":${HOST_ID},\"guestId\":${GUEST_ID},\"forceFailure\":false,\"probeKey\":\"${PROBE1}\"}"

if [ "$LAST_CODE" = "200" ]; then
  pass "Probe commit endpoint returned 200"
else
  fail "Probe commit endpoint expected 200, got ${LAST_CODE}"
fi

PROBE1_PROPERTY_ID=$(extract_probe_property_id "$LAST_BODY")
if [ -n "$PROBE1_PROPERTY_ID" ]; then
  pass "Parsed propertyId=${PROBE1_PROPERTY_ID} from probe response"
else
  fail "Could not parse propertyId from commit response"
fi

P1_PROP_COUNT=$(db_query_property "select count(*) from properties where id=${PROBE1_PROPERTY_ID};")
P1_BOOKING_COUNT=$(db_query_core "select count(*) from bookings where property_id=${PROBE1_PROPERTY_ID};")

if [ "$P1_PROP_COUNT" = "1" ]; then
  pass "Property row committed in property DB"
else
  fail "Expected property row in property DB, got count=${P1_PROP_COUNT}"
fi

if [ "$P1_BOOKING_COUNT" = "1" ]; then
  pass "Booking row committed in core DB"
else
  fail "Expected booking row in core DB, got count=${P1_BOOKING_COUNT}"
fi

print_block "SCENARIO 2: FORCED ROLLBACK (forceFailure=true)"
PROBE2="probe_rollback_${ts}"
call_api "TX probe forced rollback" "POST" "${BASE_URL}/api/v1/bookings/admin/tx-probe" "$ADMIN_TOKEN" \
  "{\"hostId\":${HOST_ID},\"guestId\":${GUEST_ID},\"forceFailure\":true,\"probeKey\":\"${PROBE2}\"}"

if [ "$LAST_CODE" = "400" ]; then
  pass "Forced rollback endpoint returned 400 as expected"
else
  fail "Forced rollback endpoint expected 400, got ${LAST_CODE}"
fi

PROBE2_PROPERTY_ID=$(extract_probe_property_id "$LAST_BODY")
if [ -n "$PROBE2_PROPERTY_ID" ]; then
  P2_PROP_COUNT=$(db_query_property "select count(*) from properties where id=${PROBE2_PROPERTY_ID};")
  P2_BOOKING_COUNT=$(db_query_core "select count(*) from bookings where property_id=${PROBE2_PROPERTY_ID};")

  if [ "$P2_PROP_COUNT" = "0" ]; then
    pass "Property row rolled back in property DB"
  else
    fail "Expected rollback in property DB, got count=${P2_PROP_COUNT}"
  fi

  if [ "$P2_BOOKING_COUNT" = "0" ]; then
    pass "Booking row rolled back in core DB"
  else
    fail "Expected rollback in core DB, got count=${P2_BOOKING_COUNT}"
  fi
else
  fail "Could not parse propertyId from forced rollback response"
fi

print_block "SCENARIO 3: CORE DB UNAVAILABLE"
PROBE3="probe_core_down_${ts}"
echo "Stopping ${CORE_DB_SERVICE}..."
docker compose stop "$CORE_DB_SERVICE" >/dev/null

call_api "TX probe while core DB stopped" "POST" "${BASE_URL}/api/v1/bookings/admin/tx-probe" "$ADMIN_TOKEN" \
  "{\"hostId\":${HOST_ID},\"guestId\":${GUEST_ID},\"forceFailure\":false,\"probeKey\":\"${PROBE3}\"}"

if [ "$LAST_CODE" != "200" ]; then
  pass "Probe failed while core DB unavailable (expected)"
else
  fail "Probe unexpectedly succeeded while core DB unavailable"
fi

echo "Starting ${CORE_DB_SERVICE}..."
docker compose start "$CORE_DB_SERVICE" >/dev/null

for i in $(seq 1 30); do
  if docker compose exec -T "$CORE_DB_SERVICE" pg_isready -U airbnb -d airbnb_core >/dev/null 2>&1; then
    pass "Core DB is back"
    break
  fi
  sleep 1
  if [ "$i" = "30" ]; then
    fail "Core DB did not become ready in time"
  fi
done

P3_PROP_COUNT=$(db_query_property "select count(*) from properties where title='TX_PROBE_${PROBE3}';")
if [ "$P3_PROP_COUNT" = "0" ]; then
  pass "Property DB insert was rolled back when core DB was down"
else
  fail "Expected no TX_PROBE_${PROBE3} row, got count=${P3_PROP_COUNT}"
fi

print_block "RESULT"
if [ "$FAILURES" -eq 0 ]; then
  echo "ALL CHECKS PASSED"
  exit 0
else
  echo "CHECKS FAILED: ${FAILURES}"
  exit 1
fi
