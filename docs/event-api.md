# Persistent events

SRS Phase 2A adds PostgreSQL and Spring Data JPA outside the standalone core. Flyway owns schema changes; Hibernate validates the migrated schema instead of creating tables. The original `POST /api/v1/sessions/assign` computation remains stateless.

## Local database and startup

Set a local database password, then start the complete stack:

```bash
export POSTGRES_PASSWORD="$(openssl rand -hex 24)"
docker compose up --build --detach --wait
```

The UI and API proxy are at localhost:8080. The API and PostgreSQL ports are private by default. The `postgres-data` volume survives container recreation. `docker compose down` retains it; `docker compose down --volumes` deletes it. Retain the password when reusing the existing volume; changing an environment variable does not change an initialized PostgreSQL account's password.

For a host JVM, opt into the loopback database port with `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --wait db`, then export `DATABASE_PASSWORD="$POSTGRES_PASSWORD"` and run the documented Maven/JAR command. Optional `DATABASE_URL` defaults to `jdbc:postgresql://localhost:5432/session_manager`; `DATABASE_USER` defaults to `session_manager`. Do not put credentials in source files.

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

PR verification and Sonar workflows run tests against PostgreSQL 17.11 service containers. The core's own dependency tree remains free of persistence/framework dependencies. Container CI separately builds/starts the UI/API/database stack and checks its images, browser workflow, private networking, persistence and recovery.

## Event participants and latest assignment

Participant routes are nested under `/api/v1/events/{eventId}/participants`: POST creates (201/Location), GET lists, and GET/PUT/DELETE with `/{participantId}` reads, replaces or removes a participant. Participant IDs are UUIDs; an ID belonging to another event returns 404 for all operations. Fields are `name`, integer `age`, `gender` and optional `lifeStage`; the current event configuration determines the response's derived `ageTier`. Names contain 1–200 characters, ages are positive, and session-derived Young Adults require a life stage. Under-13 attendees may be registered and remain excluded with warnings during assignment.

`POST /api/v1/events/{eventId}/assignment` takes only `facilitators` and optional `facilitatorExceptions`, using the same roster-instance indices and explicit permissions as the stateless API. The configuration and participants come from the stored event, never from the caller's replacement roster. The response contains `eventId`, `eventName`, `revision` and `result` (the existing assignment response). Both FINAL and NON_FINAL_PROPOSAL results may be stored; an exception applies only to that POST, and warnings preserve its visibility.

`GET /api/v1/events/{eventId}/assignment` returns the current snapshot, or 204 when there is none. Creating, updating or deleting a participant, or changing event configuration, clears the snapshot. Configuration changes incompatible with an existing participant's required fields are rejected without committing. Event-row locks serialize registration, configuration and assignment changes so an older computation cannot overwrite a newer roster. Event deletion cascades to participants; no assignment-history feature is added.

`ParticipantWorkflowTest` covers HTTP ownership, validation, explicit exception withdrawal and invalidation. `RegistrationConcurrencyTest` mixes 20 concurrent registrations/assignments and checks the latest count plus cascading deletion. Restart acceptance checks both the roster and latest snapshot after closing and reopening the application context.
