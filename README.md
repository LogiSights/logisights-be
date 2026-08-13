# Logisights Backend

Backend API for Logisights, a parcel delivery platform for the Kenyan market. Serves the [logisights-FE](https://github.com/LogiSights/logisights-FE) Next.js frontend: sender booking and tracking, driver delivery management, pickup-point inventory, M-Pesa payments, and admin analytics across four roles (SENDER, DRIVER, PICKUP, ADMIN).

## Stack

Quarkus 3.38, PostgreSQL, Flyway migrations, JWT auth (`smallrye-jwt`), M-Pesa Daraja integration, and Resend-backed transactional email over Qute templates. See [CLAUDE.md](CLAUDE.md) for the module layout and architectural decisions.

## Local development

```bash
docker compose up -d                 # starts Postgres on :5435

cd src/main/resources                # generate a dev-only JWT signing keypair (gitignored)
openssl genrsa -out privateKey.pem 2048
openssl rsa -in privateKey.pem -pubout -out publicKey.pem
cd ../../..

./mvnw quarkus:dev                   # http://localhost:8080, Flyway migrates on start
```

Health check: `GET /q/health`. Required env vars are listed in `application.properties` alongside their dev defaults.

## Testing

```bash
./mvnw test      # unit tests
./mvnw verify     # tests + JaCoCo coverage gate (90% line coverage on business logic)
```

Coverage excludes entities, DTOs, REST resources, and generated REST client interfaces — the gate targets the service layer where the business rules live.

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs `mvn verify` on every push and pull request against `main`, enforcing the coverage gate and uploading the JaCoCo report as a build artifact.

## Packaging

```bash
./mvnw package                                      # target/quarkus-app/quarkus-run.jar
./mvnw package -Dnative                              # native executable (requires GraalVM)
./mvnw package -Dnative -Dquarkus.native.container-build=true   # native build via container
```
