# Persistent events

SRS Phase 2A adds PostgreSQL and Spring Data JPA outside the standalone core. Flyway owns schema changes; Hibernate validates the migrated schema instead of creating tables. The original `POST /api/v1/sessions/assign` computation remains stateless.

## Local database and startup

Set a local database password, then start the complete stack:

```bash
export POSTGRES_PASSWORD="$(openssl rand -hex 24)"
docker compose up --build
```

The service is at localhost:8080 and PostgreSQL at localhost:5432. The `postgres-data` volume survives container recreation. `docker compose down` retains it; `docker compose down --volumes` deletes it. Retain the password when reusing the existing volume; changing an environment variable does not change an initialized PostgreSQL account's password.

For a host JVM, start only `docker compose up -d db`, then export `DATABASE_PASSWORD="$POSTGRES_PASSWORD"` and run the documented Maven/JAR command. Optional `DATABASE_URL` defaults to `jdbc:postgresql://localhost:5432/session_manager`; `DATABASE_USER` defaults to `session_manager`. Do not put credentials in source files.

## Event contract

| Method | Path | Behavior |
| --- | --- | --- |
| POST | `/api/v1/events` | Create; 201 with a Location header |
| GET | `/api/v1/events` | List in name/id order |
| GET | `/api/v1/events/{id}` | Read; unknown ID returns 404 |
| PUT | `/api/v1/events/{id}` | Replace name/config; returns updated event |
| DELETE | `/api/v1/events/{id}` | Delete; 204, unknown ID returns 404 |

Example request:

```json
{"name":"Sunday session","config":{"capSize":6}}
```

Responses contain UUID `id`, trimmed `name`, fully resolved `config`, and a database `revision`. Names must contain 1–200 characters. Config has the same explicit cap, validated age boundaries, defaults and optional flags as the assignment API. Invalid requests return 400. Update/delete operations lock the event row within a transaction. No date, recurrence, authentication or assignment-history policy is invented by this contract.

## Verification

`EventApiTest` exercises HTTP CRUD, resolved defaults, invalid input and missing/malformed identifiers. `EventPersistenceTest` closes one full application context and starts another, then reads the original event and removes its test data.

Default Maven tests use a test-scoped H2 database for an isolated quick run. To run the same tests against PostgreSQL:

```bash
TEST_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/session_manager \
TEST_DATABASE_PASSWORD="$POSTGRES_PASSWORD" \
mvn --batch-mode --no-transfer-progress -Pquality clean verify
```

PR verification and Sonar workflows run tests against PostgreSQL 17.11 service containers. The core's own dependency tree remains free of persistence/framework dependencies. Container CI separately starts the Compose database/app and checks the actual image and HTTP endpoint.
