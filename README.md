# Logisights Backend

Backend API for Logisights, a parcel delivery platform for the Kenyan market. Serves the [logisights-FE](https://github.com/LogiSights/logisights-FE) Next.js frontend: sender booking and tracking, driver delivery management, pickup-point inventory, M-Pesa payments, and admin analytics across four roles (SENDER, DRIVER, PICKUP, ADMIN).

See [ARCHITECTURE.md](ARCHITECTURE.md) for how the frontend, this API, the database, and the external providers (Resend, M-Pesa) fit together.

## Stack

Quarkus 3.38, PostgreSQL, Flyway migrations, JWT auth (`smallrye-jwt`), M-Pesa Daraja integration, and Resend-backed transactional email over Qute templates. See [CLAUDE.md](CLAUDE.md) for the module layout and architectural decisions.

## Local development

```bash
docker compose up -d                 # starts Postgres on :5435

cp src/main/resources/example.application.properties src/main/resources/application.properties

cd src/main/resources                # generate a dev-only JWT signing keypair (gitignored)
openssl genrsa -out privateKey.pem 2048
openssl rsa -in privateKey.pem -pubout -out publicKey.pem
cd ../../..

./mvnw quarkus:dev                   # http://localhost:8080, Flyway migrates on start
```

`application.properties` is gitignored so real local secrets never get committed. `example.application.properties` is the tracked template with placeholder defaults; copy it before first run.

Health check: `GET /q/health`.

## Testing

```bash
./mvnw test      # unit tests
./mvnw verify     # tests + JaCoCo coverage gate (90% line coverage on business logic)
```

Tests run against a separate `logisights_test` database, never the dev database. See the Testing section in [CLAUDE.md](CLAUDE.md) for the isolation setup. Coverage excludes entities, DTOs, REST resources, and generated REST client interfaces; the gate targets the service layer where the business rules live.

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs `mvn verify` on every push and pull request against `main`, enforcing the coverage gate and uploading the JaCoCo report as a build artifact.

## Packaging

```bash
./mvnw package                                      # target/quarkus-app/quarkus-run.jar
./mvnw package -Dnative                              # native executable (requires GraalVM)
./mvnw package -Dnative -Dquarkus.native.container-build=true   # native build via container
```
