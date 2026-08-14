# CLAUDE.md - logisights-be

Backend for Logisights, a parcel-delivery platform for the Kenyan market. Quarkus + PostgreSQL, sibling repo to `logisights-FE` (Next.js frontend). See `../logisights-FE` for the frontend that consumes this API, and [ARCHITECTURE.md](ARCHITECTURE.md) for how the pieces fit together.

## Stack & architectural decisions

- **Quarkus 3.38 + PostgreSQL**, modular monolith, not microservices. Single small team, tightly coupled domain (parcels/users/payments reference each other constantly).
- **Auth: self-issued JWT via `smallrye-jwt`, not Keycloak.** DondooHomes (a sibling project) uses Keycloak, but that's a heavier fit for its case. Logisights has only 4 fixed roles (SENDER/DRIVER/PICKUP/ADMIN) and no SSO/enterprise-IdP requirement, so a Keycloak container plus realm/theme config would be overhead without payoff. Roles are carried as a JWT `groups` claim; `@RolesAllowed` on resources enforces them.
- **Email: Resend**, called via a thin typed REST client (`notification/client/ResendApi.java`) since Resend has no official Java SDK. Templates are Qute (`src/main/resources/templates/notification/`), not React Email, since this is a Java backend, but they borrow the good structural patterns from the `email-template-builder` skill: a shared base layout with header/footer partials, a dark-mode `prefers-color-scheme` CSS block, and a single `MailSender` interface (`notification/MailSender.java`) so callers never talk to Resend directly.
- **Payments: M-Pesa (Safaricom Daraja)**, isolated entirely inside the `payment` package (`payment/client/DarajaApi.java`, `payment/service/MpesaService.java`). Nothing outside that package knows Daraja-specific request/response shapes, so a second provider could be added later without touching `parcel` or `auth`.
- **Pricing is server-authoritative**: `parcel/service/PricingService.java` is the single place cost is computed (`base + per-kg x weight`), replacing the frontend's client-side calculation so a tampered client can't under-quote a delivery.

## Module layout

Each module follows `resource` (JAX-RS) then `service` (business logic, `@Transactional` boundary) then `repository` (Panache) then `entity`. DTOs (records) sit at the resource boundary; entities never leak into JSON responses.

| Package | Owns | Notes |
|---|---|---|
| `auth` | `users`, `email_verification_tokens`, `password_reset_tokens` | Register/login/verify/reset. Tokens are stored as SHA-256 hashes only (`auth/service/TokenGenerator.java`); the raw token is emailed once. |
| `parcel` | `parcels`, `parcel_status_history` | Booking, tracking, status transitions, pricing. |
| `driver` | `driver_earnings` | Delivery task list (reuses `ParcelService`), earnings summary. |
| `pickup` | `pickup_points`, `pickup_inventory`, `pickup_activity_log` | Pickup-point inventory plus check-in/check-out log. |
| `payment` | `payments` | M-Pesa STK push plus callback webhook. |
| `notification` | (none) | Resend email client plus Qute templates. |
| `admin` | (none) | User management plus analytics endpoints. All stats are live SQL aggregates over the tables above (`admin/service/AdminService.java`), not separately stored. |
| `common` | (none) | Shared enums (`UserRole`, `ParcelStatus`, ...) and `ApiException`/`ApiExceptionMapper` for consistent JSON error bodies. |

## Local development

```bash
docker compose up -d                 # starts Postgres on :5435 (5432 is taken by dondooHomes locally)

cp src/main/resources/example.application.properties src/main/resources/application.properties

# Generate a dev-only JWT signing keypair (gitignored, regenerate per checkout):
cd src/main/resources
openssl genrsa -out privateKey.pem 2048
openssl rsa -in privateKey.pem -pubout -out publicKey.pem
cd ../../..

mvn quarkus:dev                      # http://localhost:8080, Flyway migrates on start
```

`application.properties` is gitignored since it's where real local secrets end up; `example.application.properties` is the tracked template with placeholder defaults. Required env vars (see that file for defaults/fallbacks): `DB_USERNAME`, `DB_PASSWORD`, `DB_URL`, `RESEND_API_KEY`, `MAIL_FROM_ADDRESS`, `FRONTEND_BASE_URL`, `MPESA_CONSUMER_KEY`, `MPESA_CONSUMER_SECRET`, `MPESA_SHORTCODE`, `MPESA_PASSKEY`, `MPESA_CALLBACK_URL`, `CORS_ORIGINS`.

`GET /q/health` for liveness. Flyway migration lives at `src/main/resources/db/migration/V1__init_schema.sql`. This is the schema of record; don't hand-edit tables outside a new migration.

## Testing

`mvn test` runs against a **separate `logisights_test` database** (`%test.quarkus.datasource.*` in `application.properties`), never the `logisights` dev database. `docker/init-test-db.sql` provisions it automatically on a fresh `docker compose up -d` (existing local volumes need `CREATE DATABASE logisights_test OWNER logisights;` run once manually). The test datasource uses Hibernate `drop-and-create` and skips Flyway, so each `@QuarkusTest` run rebuilds its own schema from the entities and can never touch or be blocked by dev data. CI (`.github/workflows/ci.yml`) spins up its own throwaway Postgres service with that database name, so this isolation holds in both places. `UserRepositoryTest` asserts `current_database() = 'logisights_test'` as a regression guard on this wiring.
