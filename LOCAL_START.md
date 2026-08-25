# Local Development Setup

## 1. Start infrastructure (Docker)

```bash
docker-compose up -d
```

This starts MySQL on `3306` and MailHog (SMTP on `1025`, web UI on `8025`).

## 2. Configure `.env` for local dev

No `.env` overrides needed — all defaults in `application.yml` point to local services (MailHog, SQLite, reCAPTCHA disabled). Just start Docker and run the app.

> **Production note:** ensure `RECAPTCHA_ENABLED=true` is set as an environment variable in Render.

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
