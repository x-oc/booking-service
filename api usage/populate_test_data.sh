#!/bin/bash

BASE_URL=http://localhost:28800
CONTENT_TYPE="application/json"

echo "=== Регистрация 5 гостей ==="
for i in {1..5}; do
  curl -X POST ${BASE_URL}/api/auth/register \
    -H "Content-Type: ${CONTENT_TYPE}" \
    -d "{
      \"email\": \"guest${i}@test.com\",
      \"password\": \"password123\",
      \"firstName\": \"Guest${i}\",
      \"lastName\": \"Testov${i}\",
      \"phoneNumber\": \"+7900123456${i}\",
      \"role\": \"GUEST\"
    }"
  echo ""
done

echo ""
echo "=== Регистрация 5 хостов ==="
for i in {1..5}; do
  curl -X POST ${BASE_URL}/api/auth/register \
    -H "Content-Type: ${CONTENT_TYPE}" \
    -d "{
      \"email\": \"host${i}@test.com\",
      \"password\": \"password123\",
      \"firstName\": \"Host${i}\",
      \"lastName\": \"Owner${i}\",
      \"phoneNumber\": \"+7900765432${i}\",
      \"role\": \"HOST\"
    }"
  echo ""
done

echo ""
echo "=== Получение токенов и ID для гостей ==="
GUEST1_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/auth/login \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -d '{"email": "guest1@test.com", "password": "password123"}')
GUEST1_TOKEN=$(echo $GUEST1_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
GUEST1_ID=$(echo $GUEST1_RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

GUEST2_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/auth/login \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -d '{"email": "guest2@test.com", "password": "password123"}')
GUEST2_TOKEN=$(echo $GUEST2_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
GUEST2_ID=$(echo $GUEST2_RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

GUEST3_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/auth/login \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -d '{"email": "guest3@test.com", "password": "password123"}')
GUEST3_TOKEN=$(echo $GUEST3_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
GUEST3_ID=$(echo $GUEST3_RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

GUEST4_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/auth/login \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -d '{"email": "guest4@test.com", "password": "password123"}')
GUEST4_TOKEN=$(echo $GUEST4_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
GUEST4_ID=$(echo $GUEST4_RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

GUEST5_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/auth/login \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -d '{"email": "guest5@test.com", "password": "password123"}')
GUEST5_TOKEN=$(echo $GUEST5_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
GUEST5_ID=$(echo $GUEST5_RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

echo ""
echo "=== Получение токенов и ID для хостов ==="
HOST1_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/auth/login \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -d '{"email": "host1@test.com", "password": "password123"}')
HOST1_TOKEN=$(echo $HOST1_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
HOST1_ID=$(echo $HOST1_RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

HOST2_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/auth/login \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -d '{"email": "host2@test.com", "password": "password123"}')
HOST2_TOKEN=$(echo $HOST2_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
HOST2_ID=$(echo $HOST2_RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

HOST3_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/auth/login \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -d '{"email": "host3@test.com", "password": "password123"}')
HOST3_TOKEN=$(echo $HOST3_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
HOST3_ID=$(echo $HOST3_RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

HOST4_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/auth/login \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -d '{"email": "host4@test.com", "password": "password123"}')
HOST4_TOKEN=$(echo $HOST4_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
HOST4_ID=$(echo $HOST4_RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

HOST5_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/auth/login \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -d '{"email": "host5@test.com", "password": "password123"}')
HOST5_TOKEN=$(echo $HOST5_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
HOST5_ID=$(echo $HOST5_RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

echo "Guest IDs: $GUEST1_ID, $GUEST2_ID, $GUEST3_ID, $GUEST4_ID, $GUEST5_ID"
echo "Host IDs: $HOST1_ID, $HOST2_ID, $HOST3_ID, $HOST4_ID, $HOST5_ID"

echo ""
echo "=== Создание жилищ (properties) ==="

extract_property_id() {
  echo "$1" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
}

create_property() {
  local host_token="$1"
  local title="$2"
  local address="$3"
  local response
  response=$(curl -s -X POST ${BASE_URL}/api/v1/properties \
    -H "Content-Type: ${CONTENT_TYPE}" \
    -H "Authorization: Bearer ${host_token}" \
    -d "{\"title\":\"${title}\",\"address\":\"${address}\"}")
  extract_property_id "$response"
}

PROP1=$(create_property "${HOST1_TOKEN}" "Host1 Loft A" "Nevsky 1, Saint-Petersburg")
PROP2=$(create_property "${HOST2_TOKEN}" "Host2 Studio A" "Rubinshteina 12, Saint-Petersburg")
PROP3=$(create_property "${HOST3_TOKEN}" "Host3 Flat A" "Liteyny 8, Saint-Petersburg")
PROP4=$(create_property "${HOST1_TOKEN}" "Host1 Loft B" "Nevsky 15, Saint-Petersburg")
PROP5=$(create_property "${HOST2_TOKEN}" "Host2 Studio B" "Rubinshteina 20, Saint-Petersburg")
PROP6=$(create_property "${HOST3_TOKEN}" "Host3 Flat B" "Liteyny 30, Saint-Petersburg")
PROP7=$(create_property "${HOST4_TOKEN}" "Host4 Apt A" "Fontanka 5, Saint-Petersburg")
PROP8=$(create_property "${HOST4_TOKEN}" "Host4 Apt B" "Fontanka 11, Saint-Petersburg")
PROP9=$(create_property "${HOST5_TOKEN}" "Host5 House A" "Petrogradskaya 22, Saint-Petersburg")
PROP10=$(create_property "${HOST5_TOKEN}" "Host5 House B" "Petrogradskaya 35, Saint-Petersburg")
PROP11=$(create_property "${HOST1_TOKEN}" "Host1 Loft C" "Nevsky 44, Saint-Petersburg")

echo "Property IDs: $PROP1, $PROP2, $PROP3, $PROP4, $PROP5, $PROP6, $PROP7, $PROP8, $PROP9, $PROP10, $PROP11"

echo ""
echo "=== Создание бронирований ==="

# Функция для извлечения ID бронирования из JSON ответа
extract_booking_id() {
  echo "$1" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
}

# Guest1 создает бронирования у разных хостов
echo "Guest1 создает бронирования..."
BOOKING1_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/v1/bookings \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -H "Authorization: Bearer ${GUEST1_TOKEN}" \
  -d "{\"propertyId\": ${PROP1}, \"hostId\": ${HOST1_ID}, \"checkInDate\": \"2026-03-01\", \"checkOutDate\": \"2026-03-05\", \"totalAmount\": 5000.00}")
BOOKING1=$(extract_booking_id "$BOOKING1_RESPONSE")
echo "  Created booking $BOOKING1"

BOOKING2_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/v1/bookings \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -H "Authorization: Bearer ${GUEST1_TOKEN}" \
  -d "{\"propertyId\": ${PROP2}, \"hostId\": ${HOST2_ID}, \"checkInDate\": \"2026-03-10\", \"checkOutDate\": \"2026-03-15\", \"totalAmount\": 8000.00}")
BOOKING2=$(extract_booking_id "$BOOKING2_RESPONSE")
echo "  Created booking $BOOKING2"

BOOKING3_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/v1/bookings \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -H "Authorization: Bearer ${GUEST1_TOKEN}" \
  -d "{\"propertyId\": ${PROP3}, \"hostId\": ${HOST3_ID}, \"checkInDate\": \"2026-04-01\", \"checkOutDate\": \"2026-04-07\", \"totalAmount\": 12000.00}")
BOOKING3=$(extract_booking_id "$BOOKING3_RESPONSE")
echo "  Created booking $BOOKING3"

# Guest2 создает бронирования
echo "Guest2 создает бронирования..."
BOOKING4_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/v1/bookings \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -H "Authorization: Bearer ${GUEST2_TOKEN}" \
  -d "{\"propertyId\": ${PROP4}, \"hostId\": ${HOST1_ID}, \"checkInDate\": \"2026-03-20\", \"checkOutDate\": \"2026-03-25\", \"totalAmount\": 6000.00}")
BOOKING4=$(extract_booking_id "$BOOKING4_RESPONSE")
echo "  Created booking $BOOKING4"

BOOKING5_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/v1/bookings \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -H "Authorization: Bearer ${GUEST2_TOKEN}" \
  -d "{\"propertyId\": ${PROP5}, \"hostId\": ${HOST2_ID}, \"checkInDate\": \"2026-04-10\", \"checkOutDate\": \"2026-04-15\", \"totalAmount\": 9000.00}")
BOOKING5=$(extract_booking_id "$BOOKING5_RESPONSE")
echo "  Created booking $BOOKING5"

# Guest3 создает бронирования
echo "Guest3 создает бронирования..."
BOOKING6_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/v1/bookings \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -H "Authorization: Bearer ${GUEST3_TOKEN}" \
  -d "{\"propertyId\": ${PROP6}, \"hostId\": ${HOST3_ID}, \"checkInDate\": \"2026-03-15\", \"checkOutDate\": \"2026-03-20\", \"totalAmount\": 7000.00}")
BOOKING6=$(extract_booking_id "$BOOKING6_RESPONSE")
echo "  Created booking $BOOKING6"

BOOKING7_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/v1/bookings \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -H "Authorization: Bearer ${GUEST3_TOKEN}" \
  -d "{\"propertyId\": ${PROP7}, \"hostId\": ${HOST4_ID}, \"checkInDate\": \"2026-04-20\", \"checkOutDate\": \"2026-04-25\", \"totalAmount\": 10000.00}")
BOOKING7=$(extract_booking_id "$BOOKING7_RESPONSE")
echo "  Created booking $BOOKING7"

# Guest4 создает бронирования
echo "Guest4 создает бронирования..."
BOOKING8_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/v1/bookings \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -H "Authorization: Bearer ${GUEST4_TOKEN}" \
  -d "{\"propertyId\": ${PROP8}, \"hostId\": ${HOST4_ID}, \"checkInDate\": \"2026-03-25\", \"checkOutDate\": \"2026-03-30\", \"totalAmount\": 5500.00}")
BOOKING8=$(extract_booking_id "$BOOKING8_RESPONSE")
echo "  Created booking $BOOKING8"

BOOKING9_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/v1/bookings \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -H "Authorization: Bearer ${GUEST4_TOKEN}" \
  -d "{\"propertyId\": ${PROP9}, \"hostId\": ${HOST5_ID}, \"checkInDate\": \"2026-05-01\", \"checkOutDate\": \"2026-05-05\", \"totalAmount\": 6500.00}")
BOOKING9=$(extract_booking_id "$BOOKING9_RESPONSE")
echo "  Created booking $BOOKING9"

# Guest5 создает бронирования
echo "Guest5 создает бронирования..."
BOOKING10_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/v1/bookings \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -H "Authorization: Bearer ${GUEST5_TOKEN}" \
  -d "{\"propertyId\": ${PROP10}, \"hostId\": ${HOST5_ID}, \"checkInDate\": \"2026-04-05\", \"checkOutDate\": \"2026-04-10\", \"totalAmount\": 7500.00}")
BOOKING10=$(extract_booking_id "$BOOKING10_RESPONSE")
echo "  Created booking $BOOKING10"

BOOKING11_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/v1/bookings \
  -H "Content-Type: ${CONTENT_TYPE}" \
  -H "Authorization: Bearer ${GUEST5_TOKEN}" \
  -d "{\"propertyId\": ${PROP11}, \"hostId\": ${HOST1_ID}, \"checkInDate\": \"2026-05-10\", \"checkOutDate\": \"2026-05-15\", \"totalAmount\": 8500.00}")
BOOKING11=$(extract_booking_id "$BOOKING11_RESPONSE")
echo "  Created booking $BOOKING11"

echo ""
echo "=== Подтверждение бронирований хостами ==="
# Host1 подтверждает некоторые бронирования
if [ ! -z "$BOOKING1" ]; then
  echo "  Host1 подтверждает бронирование $BOOKING1"
  curl -s -X PATCH ${BASE_URL}/api/v1/bookings/${BOOKING1}/confirm \
    -H "Authorization: Bearer ${HOST1_TOKEN}" > /dev/null
fi

if [ ! -z "$BOOKING4" ]; then
  echo "  Host1 подтверждает бронирование $BOOKING4"
  curl -s -X PATCH ${BASE_URL}/api/v1/bookings/${BOOKING4}/confirm \
    -H "Authorization: Bearer ${HOST1_TOKEN}" > /dev/null
fi

# Host2 подтверждает
if [ ! -z "$BOOKING2" ]; then
  echo "  Host2 подтверждает бронирование $BOOKING2"
  curl -s -X PATCH ${BASE_URL}/api/v1/bookings/${BOOKING2}/confirm \
    -H "Authorization: Bearer ${HOST2_TOKEN}" > /dev/null
fi

if [ ! -z "$BOOKING5" ]; then
  echo "  Host2 подтверждает бронирование $BOOKING5"
  curl -s -X PATCH ${BASE_URL}/api/v1/bookings/${BOOKING5}/confirm \
    -H "Authorization: Bearer ${HOST2_TOKEN}" > /dev/null
fi

# Host3 подтверждает
if [ ! -z "$BOOKING3" ]; then
  echo "  Host3 подтверждает бронирование $BOOKING3"
  curl -s -X PATCH ${BASE_URL}/api/v1/bookings/${BOOKING3}/confirm \
    -H "Authorization: Bearer ${HOST3_TOKEN}" > /dev/null
fi

if [ ! -z "$BOOKING6" ]; then
  echo "  Host3 подтверждает бронирование $BOOKING6"
  curl -s -X PATCH ${BASE_URL}/api/v1/bookings/${BOOKING6}/confirm \
    -H "Authorization: Bearer ${HOST3_TOKEN}" > /dev/null
fi

# Host4 подтверждает
if [ ! -z "$BOOKING7" ]; then
  echo "  Host4 подтверждает бронирование $BOOKING7"
  curl -s -X PATCH ${BASE_URL}/api/v1/bookings/${BOOKING7}/confirm \
    -H "Authorization: Bearer ${HOST4_TOKEN}" > /dev/null
fi

if [ ! -z "$BOOKING8" ]; then
  echo "  Host4 подтверждает бронирование $BOOKING8"
  curl -s -X PATCH ${BASE_URL}/api/v1/bookings/${BOOKING8}/confirm \
    -H "Authorization: Bearer ${HOST4_TOKEN}" > /dev/null
fi

# Host5 подтверждает
if [ ! -z "$BOOKING9" ]; then
  echo "  Host5 подтверждает бронирование $BOOKING9"
  curl -s -X PATCH ${BASE_URL}/api/v1/bookings/${BOOKING9}/confirm \
    -H "Authorization: Bearer ${HOST5_TOKEN}" > /dev/null
fi

if [ ! -z "$BOOKING10" ]; then
  echo "  Host5 подтверждает бронирование $BOOKING10"
  curl -s -X PATCH ${BASE_URL}/api/v1/bookings/${BOOKING10}/confirm \
    -H "Authorization: Bearer ${HOST5_TOKEN}" > /dev/null
fi

echo ""
echo "=== Оплата бронирований гостями ==="
# Guest1 оплачивает
if [ ! -z "$BOOKING1" ]; then
  echo "  Guest1 оплачивает бронирование $BOOKING1"
  curl -s -X POST ${BASE_URL}/api/v1/bookings/${BOOKING1}/pay \
    -H "Authorization: Bearer ${GUEST1_TOKEN}" > /dev/null
fi

if [ ! -z "$BOOKING2" ]; then
  echo "  Guest1 оплачивает бронирование $BOOKING2"
  curl -s -X POST ${BASE_URL}/api/v1/bookings/${BOOKING2}/pay \
    -H "Authorization: Bearer ${GUEST1_TOKEN}" > /dev/null
fi

# Guest2 оплачивает
if [ ! -z "$BOOKING4" ]; then
  echo "  Guest2 оплачивает бронирование $BOOKING4"
  curl -s -X POST ${BASE_URL}/api/v1/bookings/${BOOKING4}/pay \
    -H "Authorization: Bearer ${GUEST2_TOKEN}" > /dev/null
fi

# Guest3 оплачивает
if [ ! -z "$BOOKING6" ]; then
  echo "  Guest3 оплачивает бронирование $BOOKING6"
  curl -s -X POST ${BASE_URL}/api/v1/bookings/${BOOKING6}/pay \
    -H "Authorization: Bearer ${GUEST3_TOKEN}" > /dev/null
fi

# Guest4 оплачивает
if [ ! -z "$BOOKING8" ]; then
  echo "  Guest4 оплачивает бронирование $BOOKING8"
  curl -s -X POST ${BASE_URL}/api/v1/bookings/${BOOKING8}/pay \
    -H "Authorization: Bearer ${GUEST4_TOKEN}" > /dev/null
fi

# Guest5 оплачивает
if [ ! -z "$BOOKING10" ]; then
  echo "  Guest5 оплачивает бронирование $BOOKING10"
  curl -s -X POST ${BASE_URL}/api/v1/bookings/${BOOKING10}/pay \
    -H "Authorization: Bearer ${GUEST5_TOKEN}" > /dev/null
fi

# Host1 отклоняет одно бронирование
echo ""
echo "=== Отклонение одного бронирования ==="
if [ ! -z "$BOOKING11" ]; then
  echo "  Host1 отклоняет бронирование $BOOKING11"
  curl -s -X PATCH ${BASE_URL}/api/v1/bookings/${BOOKING11}/reject \
    -H "Authorization: Bearer ${HOST1_TOKEN}" > /dev/null
fi

echo ""
echo "=== Готово! Данные заполнены ==="
echo "Создано бронирований:"
echo "  - CREATED: ${BOOKING3}, ${BOOKING5}, ${BOOKING7}, ${BOOKING9}"
echo "  - AWAITING_PAYMENT: ${BOOKING11}"
echo "  - PAID: ${BOOKING1}, ${BOOKING2}, ${BOOKING4}, ${BOOKING6}, ${BOOKING8}, ${BOOKING10}"
echo "  - REJECTED: ${BOOKING11}"
echo ""
echo "Теперь можно тестировать фильтрацию:"
echo "  - GET /api/v1/bookings/host?guestId=1&status=PAID"
echo "  - GET /api/v1/bookings/guest?hostId=1&date=2026-03-03"
echo "  - GET /api/v1/bookings/host?guestEmail=guest1@test.com&date=2026-03-02"

