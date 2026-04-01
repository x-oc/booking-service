## 🚀 Как запускать

### 1. Клонируйте репозиторий

### 2. Настройте окружение (.env) и введите свои данные
```
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=superSecretKeyHereSuperSecretKeyHereSuperSecretKeyHereSuperSecretKeyHere
JWT_EXPIRATION_MS=86400000
```

### 3. Соберите jar файл приложения
```
gradle build
```

### 5. Перенесите свои .env и app.jar в один репозиторий на гелиосе, например, с помощью SFTP

### 6. Пробросьте порты с локальной машины на гелиос, где XXXXXX - ваш ИСУ
```
ssh -p 2222 -L 28800:localhost:28800 sXXXXXX@helios.cs.ifmo.ru
```

### 7. Зайдите в директорию с .env и app.jar и запустите программу
```
java -jar app.jar
```

## После запуска доступны:
- 📚 http://localhost:28800/swagger-ui.html API документация

## Модель доступа (RBAC + privileges)

Роли в системе:
- `GUEST`
- `HOST`
- `ADMIN`

Ключевые привилегии:
- `BOOKING_CREATE`, `BOOKING_UPDATE`, `BOOKING_DELETE`, `BOOKING_PAY`
- `BOOKING_CONFIRM`, `BOOKING_REJECT`
- `BOOKING_FORCE_STATUS`, `BOOKING_SUPPORT_PROCESS`, `BOOKING_SUPPORT_REJECT`, `BOOKING_SUPPORT_LIST`
- `BOOKING_SUPPORT_REQUEST`, `BOOKING_VIEW_OWN`, `BOOKING_VIEW_ANY`
- `BOOKING_LIST_HOST`, `BOOKING_LIST_GUEST`
- `PROPERTY_CREATE`, `PROPERTY_VIEW`, `PROPERTY_VIEW_HOST_LIST`

Проверка прав реализована на уровне сервисов через `@PreAuthorize` по привилегиям.

## Транзакции

- Для управления транзакциями используется Spring JTA + Atomikos.
- Границы бизнес-транзакций реализованы программно через `TransactionTemplate`.
- Программные транзакции используются в основных прецедентах (создание/изменение/подтверждение/оплата/админ-обработка бронирований и т.д.).

## Модель цены бронирования

- В `Property` хранится `basePricePerDay`.
- В `BookingRequest` гость передает только `propertyId`, `checkInDate`, `checkOutDate`.
- `hostId` и `totalAmount` вычисляются на сервере:
	- `hostId` берется из владельца выбранного объекта.
	- `totalAmount = basePricePerDay * количество_дней`.
