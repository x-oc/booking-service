## 🚀 Как запускать

```bash
# 1. Клонируйте репозиторий

# 2. Настройте окружение (.env) и введите свои данные
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=superSecretKeyHereSuperSecretKeyHereSuperSecretKeyHereSuperSecretKeyHere
JWT_EXPIRATION_MS=86400000

# 3. Соберите jar файл приложения
gradle build

# 5. Перенесите свои .env и app.jar в один репозиторий на гелиосе, например, с помощью SFTP

# 6. Пробросьте порты с локальной машины на гелиос, где XXXXXX - ваш ИСУ
ssh -p 2222 -L 28800:localhost:28800 sXXXXXX@helios.cs.ifmo.ru

# 7. Зайдите в директорию с .env и app.jar и запустите программу
java -jar app.jar
```

## После запуска доступны:
- 📚 http://localhost:28800/swagger-ui.html API документация
