# Kazik

Казино на внутриигровой валюте **наеБАЛЛЫ**: пополнение картой через API Z-банка, слоты с джекпотом и картинкой-призом, донаты автору по СБП.
Лабораторная работа №1: модульный монолит на Kotlin + Spring Boot.

## Запуск

```bash
cd server
cp .env.example .env
docker compose up --build
```

Swagger: http://localhost:8081/swagger-ui.html → **Authorize** (`kazik-swagger`), пользователи `player` / `admin`, пароль — `KAZIK_DEMO_PASSWORD` из `.env`.

## Тесты

```bash
cd server
./gradlew integrationTest
```
