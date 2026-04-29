#!/bin/bash

BASE_URL="${BASE_URL:-http://localhost:28800}"
CONTENT_TYPE="application/json"

timestamp=$(date +%s)
GUEST_EMAIL="payment_guest_${timestamp}@test.com"
HOST_EMAIL="payment_host_${timestamp}@test.com"
PASS="password123"

TMP_BODY_FILE=$(mktemp)
trap 'rm -f "$TMP_BODY_FILE"' EXIT

date_plus() {
  local days="$1"
  date -d "+${days} days" +%F
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

  echo ""
  echo ">>> $label"
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
  echo "RESPONSE: ${LAST_CODE}"
  echo "${LAST_BODY}" | head -c 500
  if [ ${#LAST_BODY} -gt 500 ]; then
    echo "... (truncated)"
  else
    echo ""
  fi
}

print_block() {
  echo ""
  echo "============================================================"
  echo "$1"
  echo "============================================================"
}

require_id() {
  local id_value="$1"
  local label="$2"
  if [ -z "$id_value" ]; then
    echo "ERROR: ${label} is empty. Response: ${LAST_BODY}"
    exit 1
  fi
}

print_block "1. REGISTRATION"

call_api "Register GUEST" "POST" "${BASE_URL}/api/auth/register" "" \
  "{\"email\":\"${GUEST_EMAIL}\",\"password\":\"${PASS}\",\"firstName\":\"Payment\",\"lastName\":\"Guest\",\"phoneNumber\":\"+79001111111\",\"role\":\"GUEST\"}"

call_api "Register HOST" "POST" "${BASE_URL}/api/auth/register" "" \
  "{\"email\":\"${HOST_EMAIL}\",\"password\":\"${PASS}\",\"firstName\":\"Payment\",\"lastName\":\"Host\",\"phoneNumber\":\"+79002222222\",\"role\":\"HOST\"}"

print_block "2. LOGIN"

call_api "Login GUEST" "POST" "${BASE_URL}/api/auth/login" "" \
  "{\"email\":\"${GUEST_EMAIL}\",\"password\":\"${PASS}\"}"
GUEST_TOKEN=$(extract_json_value "$LAST_BODY" "token")
GUEST_ID=$(extract_json_number "$LAST_BODY" "id")
require_id "$GUEST_TOKEN" "GUEST_TOKEN"

call_api "Login HOST" "POST" "${BASE_URL}/api/auth/login" "" \
  "{\"email\":\"${HOST_EMAIL}\",\"password\":\"${PASS}\"}"
HOST_TOKEN=$(extract_json_value "$LAST_BODY" "token")
HOST_ID=$(extract_json_number "$LAST_BODY" "id")
require_id "$HOST_TOKEN" "HOST_TOKEN"

echo ""
echo " Users created:"
echo "   GUEST: id=${GUEST_ID}, email=${GUEST_EMAIL}"
echo "   HOST:  id=${HOST_ID}, email=${HOST_EMAIL}"

print_block "3. CREATE PROPERTY"

call_api "Create property by HOST" "POST" "${BASE_URL}/api/v1/properties" "${HOST_TOKEN}" \
  "{\"title\":\"1C Test Apartment\",\"address\":\"Test Address 123\",\"basePricePerDay\":3500.00}"
PROPERTY_ID=$(extract_json_number "$LAST_BODY" "id")
require_id "$PROPERTY_ID" "PROPERTY_ID"

echo " Property created: id=${PROPERTY_ID}"

print_block "4.1 PAYMENT #1 - Weekend stay"

CHECKIN1=$(date_plus 7)
CHECKOUT1=$(date_plus 10)

call_api "Create booking #1" "POST" "${BASE_URL}/api/v1/bookings" "${GUEST_TOKEN}" \
  "{\"propertyId\":${PROPERTY_ID},\"checkInDate\":\"${CHECKIN1}\",\"checkOutDate\":\"${CHECKOUT1}\"}"
BOOKING1_ID=$(extract_json_number "$LAST_BODY" "id")
require_id "$BOOKING1_ID" "BOOKING1_ID"

call_api "Confirm booking #1 by HOST" "PATCH" "${BASE_URL}/api/v1/bookings/${BOOKING1_ID}/confirm" "${HOST_TOKEN}" ""

call_api "PAY booking #1" "POST" "${BASE_URL}/api/v1/bookings/${BOOKING1_ID}/pay" "${GUEST_TOKEN}" ""
echo " Payment #1 sent for booking ${BOOKING1_ID}"

print_block "4.2 PAYMENT #2 - Week stay"

CHECKIN2=$(date_plus 14)
CHECKOUT2=$(date_plus 21)

call_api "Create booking #2" "POST" "${BASE_URL}/api/v1/bookings" "${GUEST_TOKEN}" \
  "{\"propertyId\":${PROPERTY_ID},\"checkInDate\":\"${CHECKIN2}\",\"checkOutDate\":\"${CHECKOUT2}\"}"
BOOKING2_ID=$(extract_json_number "$LAST_BODY" "id")
require_id "$BOOKING2_ID" "BOOKING2_ID"

call_api "Confirm booking #2 by HOST" "PATCH" "${BASE_URL}/api/v1/bookings/${BOOKING2_ID}/confirm" "${HOST_TOKEN}" ""

call_api "PAY booking #2" "POST" "${BASE_URL}/api/v1/bookings/${BOOKING2_ID}/pay" "${GUEST_TOKEN}" ""
echo " Payment #2 sent for booking ${BOOKING2_ID}"

print_block "4.3 PAYMENT #3 - Long stay"

CHECKIN3=$(date_plus 30)
CHECKOUT3=$(date_plus 45)

call_api "Create booking #3" "POST" "${BASE_URL}/api/v1/bookings" "${GUEST_TOKEN}" \
  "{\"propertyId\":${PROPERTY_ID},\"checkInDate\":\"${CHECKIN3}\",\"checkOutDate\":\"${CHECKOUT3}\"}"
BOOKING3_ID=$(extract_json_number "$LAST_BODY" "id")
require_id "$BOOKING3_ID" "BOOKING3_ID"

call_api "Confirm booking #3 by HOST" "PATCH" "${BASE_URL}/api/v1/bookings/${BOOKING3_ID}/confirm" "${HOST_TOKEN}" ""

call_api "PAY booking #3" "POST" "${BASE_URL}/api/v1/bookings/${BOOKING3_ID}/pay" "${GUEST_TOKEN}" ""
echo " Payment #3 sent for booking ${BOOKING3_ID}"

print_block "5. SUMMARY"

echo ""
echo " 3 платежа отправлены в 1С!"
echo ""
echo " Информация для проверки в 1С:"
echo ""
echo "┌─────────────────────────────────────────────────────────┐"
echo "│ БРОНИРОВАНИЕ 1 (выходные)                               │"
echo "│   ID:        ${BOOKING1_ID}"
echo "│   Даты:      ${CHECKIN1} → ${CHECKOUT1}"
echo "│   Дней:      3"
echo "│   Сумма:     ~10500 руб. (3500 × 3)"
echo "├─────────────────────────────────────────────────────────┤"
echo "│ БРОНИРОВАНИЕ 2 (неделя)                                 │"
echo "│   ID:        ${BOOKING2_ID}"
echo "│   Даты:      ${CHECKIN2} → ${CHECKOUT2}"
echo "│   Дней:      7"
echo "│   Сумма:     ~24500 руб. (3500 × 7)"
echo "├─────────────────────────────────────────────────────────┤"
echo "│ БРОНИРОВАНИЕ 3 (длительное)                             │"
echo "│   ID:        ${BOOKING3_ID}"
echo "│   Даты:      ${CHECKIN3} → ${CHECKOUT3}"
echo "│   Дней:      15"
echo "│   Сумма:     ~52500 руб. (3500 × 15)"
echo "├─────────────────────────────────────────────────────────┤"
echo "│ ПОЛЬЗОВАТЕЛИ:                                            │"
echo "│   Гость:     ${GUEST_EMAIL} (id: ${GUEST_ID})"
echo "│   Хозяин:    ${HOST_EMAIL} (id: ${HOST_ID})"
echo "│   Property:  id=${PROPERTY_ID}, цена за день: 3500 руб."
echo "└─────────────────────────────────────────────────────────┘"
echo ""
echo " Что должно появиться в 1С:"
echo "   - 3 записи о платежах (в журнале регистрации)"
echo "   - Данные каждого платежа (bookingId, сумма, email гостя/хозяина)"
echo ""
echo " Суммарно отправлено средств: ~87500 руб."
echo ""

print_block "DONE"
