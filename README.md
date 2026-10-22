# Divided Session Manager

An app for organizing church event attendees into small discussion groups. Register attendees, configure group sizes and age tiers, assign facilitators, and share or print the resulting roster.

## How it works

Attendees are grouped by age tier, optionally separated by gender and Young Adult life stage. The organizer sets a target size and a required maximum group size. The grouping engine balances attendees within those rules and assigns qualified leaders and optional assistants.

A result is final only when every group meets the rules and has a leader. Otherwise, the app shows a proposal with the issues to resolve. Organizer exceptions can permit specific facilitator qualification or gender mismatches; they never raise the group cap.

## Technology

| Component | Stack |
| --- | --- |
| Grouping engine | Standalone Java 17 library (`session-manager-core`) |
| API | Spring Boot 4, Spring Data JPA, Flyway (`session-manager-service`) |
| UI | React 19, Vite, Nginx (`frontend`) |
| Database | PostgreSQL 17 |
| Local deployment | Docker Compose: separate UI, API, and database containers |

The browser connects to Nginx, which serves React and forwards `/api/` requests to the API. The API calls the grouping engine and stores events, attendees, and assignment results in PostgreSQL. Only the UI port is exposed by default.

## Install and run

Install Git and Docker with Docker Compose, then:

```sh
git clone https://github.com/auricless/divided-session-manager.git
cd divided-session-manager
export POSTGRES_PASSWORD='replace-with-your-own-local-password'
docker compose up --build --detach --wait
```

Open [localhost:8080/events](http://localhost:8080/events). Docker builds the app; separate Java and Node installations are not needed for this setup. Set `UI_PORT` before starting Compose to use another port.

Keep the same database password when reusing an existing database. Stop with `docker compose down`; saved data remains in a named volume. Adding `--volumes` deletes that data.

This setup binds to localhost and has no sign-in system. See the [container guide](docs/container-runtime.md) for logs, health checks, development ports, and recovery.

## Use the app

1. Create an event and set its maximum group size. Adjust the target size, age ranges, and grouping options as needed.
2. Register attendees with their name, age, and gender. Young Adults also need a life stage.
3. Add the available facilitators, their roles, and qualified age tiers, then generate groups. Facilitator entries apply to the current screen; enter them again after leaving it.
4. Resolve proposal blockers and generate again until the roster is ready. Attendee or configuration changes invalidate the previous assignment.
5. Open the live group display, print the roster, or download CSV. The display refreshes as assignments change.

## Development

Use Maven 3.9+, JDK 21 (compiles for Java 17), and Node 24. The tested Node version is pinned in `frontend/.nvmrc`.

```sh
mvn -Pquality verify
npm ci --ignore-scripts --prefix frontend
npm run check --prefix frontend
npm test --prefix frontend
npm run build --prefix frontend
```

Maven runs the Java tests, coverage checks, formatter, Checkstyle, and SpotBugs. Local tests use an isolated H2 database by default; CI also tests against PostgreSQL.

See [frontend development](docs/web-workflow.md), [event API](docs/event-api.md), and [assignment API](docs/assignment-api.md) for local servers and API contracts.

## License

No license file has been added to this repository yet.
