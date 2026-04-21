# OTP Service

Backend-сервис для генерации, рассылки и валидации одноразовых паролей (OTP).

## Стек

- Java 17, Maven
- PostgreSQL 17, JDBC, HikariCP
- `com.sun.net.httpserver` (JDK built-in)
- JWT (JJWT 0.12.6), BCrypt
- SLF4J + Logback

## Требования

- JDK 17+
- Maven 3.6+
- PostgreSQL 17
- *(опционально)* MailHog — для тестирования EMAIL канала
- *(опционально)* SMPPsim — для тестирования SMS канала
- *(опционально)* Telegram Bot Token — для Telegram канала

## Быстрый старт

```bash
# 1. Создать базу данных
psql -U postgres -c "CREATE DATABASE otpdb;"

# 2. Собрать fat JAR
mvn clean package

# 3. Запустить (схема применяется автоматически)
java -jar target/otp-service-1.0-SNAPSHOT.jar
```

Сервер запускается на порту `8080`.

## Конфигурация

Все настройки хранятся в файлах `src/main/resources/`:

### `application.properties`
```properties
server.port=8080
db.url=jdbc:postgresql://localhost:5432/otpdb
db.username=postgres
db.password=postgres
jwt.secret=change-this-secret-must-be-at-least-32-characters-long!!
jwt.expiry_seconds=3600
otp.file_output_dir=otp_codes
```

### `email.properties`
```properties
mail.smtp.host=localhost
mail.smtp.port=1025
mail.smtp.auth=false
mail.from=noreply@otpservice.local
```

Для реальной почты (например Gmail):
```properties
mail.smtp.host=smtp.gmail.com
mail.smtp.port=587
mail.smtp.auth=true
mail.smtp.starttls.enable=true
mail.smtp.username=your@gmail.com
mail.smtp.password=your-app-password
mail.from=your@gmail.com
```

### `sms.properties`
```properties
smpp.host=localhost
smpp.port=2775
smpp.system_id=smppclient1
smpp.password=password
smpp.source_addr=OTPService
```

### `telegram.properties`
```properties
telegram.bot_token=YOUR_BOT_TOKEN_HERE
telegram.chat_id=YOUR_CHAT_ID_HERE
```

Как получить `chat_id`:
1. Создать бота через [@BotFather](https://t.me/BotFather), получить токен
2. Написать боту любое сообщение
3. Открыть в браузере: `https://api.telegram.org/bot<TOKEN>/getUpdates`
4. Найти `chat.id` в ответе

## Каналы отправки OTP

### FILE (без внешних зависимостей)
Коды сохраняются в директорию `otp_codes/` в корне проекта.

### EMAIL — MailHog (эмулятор)
```bash
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
```
Веб-интерфейс: http://localhost:8025

### SMS — SMPPsim
1. Скачать [SMPPsim](http://www.seleniumsoftware.com/user-guide.htm)
2. Запустить: `java -jar SMPPSim.jar` (порт 2775)
3. Логин/пароль по умолчанию в `config/smppsim.props`

### Telegram
Заполнить `telegram.properties` (см. выше).

## API

### POST /auth/register
Регистрация пользователя. Только один ADMIN может существовать.

```json
{"login": "user1", "password": "secret", "role": "USER"}
```

Ответ `201`:
```json
{"id": 1, "login": "user1", "role": "USER"}
```

Ошибки: `400` (невалидные данные), `409` (логин занят или admin уже существует)

---

### POST /auth/login
```json
{"login": "user1", "password": "secret"}
```

Ответ `200`:
```json
{"token": "<jwt>"}
```

Токен передаётся в заголовке: `Authorization: Bearer <token>`

---

### PUT /admin/config *(ADMIN)*
Изменить параметры OTP-кодов.

```json
{"codeLength": 6, "ttlSeconds": 300}
```

Ответ `200`:
```json
{"codeLength": 6, "ttlSeconds": 300}
```

---

### GET /admin/users *(ADMIN)*
Список всех пользователей с ролью USER.

Ответ `200`:
```json
[{"id": 1, "login": "user1", "role": "USER", "createdAt": "..."}]
```

---

### DELETE /admin/users/{id} *(ADMIN)*
Удалить пользователя и все его OTP-коды.

Ответ `204` (успех) или `404` (не найден).

---

### POST /otp/generate *(USER)*
Сгенерировать OTP и отправить через выбранный канал.

```json
{
  "operationId": "transfer-42",
  "channel": "FILE",
  "destination": "user@example.com"
}
```

`channel`: `FILE` | `EMAIL` | `SMS` | `TELEGRAM`

`destination`: email-адрес (EMAIL), номер телефона (SMS), игнорируется для FILE и TELEGRAM.

Ответ `200`:
```json
{"message": "OTP sent successfully"}
```

---

### POST /otp/validate *(USER)*
Проверить OTP-код.

```json
{"operationId": "transfer-42", "code": "123456"}
```

Ответ `200` (верный):
```json
{"valid": true}
```

Ответ `400` (неверный или истёкший):
```json
{"valid": false, "error": "Invalid or expired OTP"}
```

## Тестирование

```bash
# Запустить сервис, затем:
chmod +x test.sh
./test.sh
```

Скрипт проверяет регистрацию, авторизацию, разграничение ролей, генерацию и валидацию OTP (включая истечение срока).

## Логи

Консоль + файл `logs/otp-service.log` (ротация по дням, хранение 30 дней).

Каждый запрос логируется с методом, путём, статусом, IP клиента, пользователем и временем выполнения. Ошибки 4xx — уровень `WARN`, ошибки 5xx — `ERROR`.

## Структура проекта

```
src/main/java/com/otpservice/
├── Main.java
├── config/       AppConfig.java
├── db/           DatabaseManager.java
├── model/        User, OtpConfig, OtpCode
├── repository/   UserRepository, OtpConfigRepository, OtpCodeRepository
├── service/      AuthService, OtpService, OtpExpiryScheduler
│   └── notification/  Email, SMS, Telegram, File
├── handler/      AuthHandler, AdminHandler, OtpHandler
├── filter/       JwtAuthFilter, LoggingFilter
└── util/         JwtUtil, JsonUtil, HttpUtil
```
