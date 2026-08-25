# Local Development Setup

## 1. Start infrastructure (Docker)

```bash
docker-compose up -d
```

This starts MySQL on `3306` and MailHog (SMTP on `1025`, web UI on `8025`).

## 2. Configure `.env` for local dev

```properties
RECAPTCHA_ENABLED=false
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USER=root
MAIL_PASS=root
MAIL_STARTTLS=false
```

These are already the defaults in `application.yml` so if you have no `.env` overrides for mail it works automatically.

## 3. Run in IntelliJ

Open `BlackjackApplication.java` → click **Run**.

## 4. View emails

Open `http://localhost:8025` in browser — all outgoing emails appear there.

## Ports

| Service      | Port |
|--------------|------|
| App          | 8080 |
| MailHog UI   | 8025 |
| MailHog SMTP | 1025 |
| MySQL        | 3306 |
