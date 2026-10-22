# Local three-container runtime

SRS v1.1 section 9, SM-38–41: an independent React UI server, Spring Boot API
(including the framework-free core library), and PostgreSQL. Docker Compose
orchestrates their startup, private connections, health checks and durable data.
This is a local deployment; public hosting, TLS and authentication are separate
requirements.

## Start and inspect

Install Docker Engine and the Compose plugin using the package manager for your
platform. From the repository root, set `POSTGRES_PASSWORD` to a local secret in
your environment. The configuration refuses to start without it; no usable
password is committed. Then run:

```bash
docker compose up --build --detach --wait
```

Open http://localhost:8080/events. Set `UI_PORT` to change that loopback port.
Only the UI port is published by default. The UI serves compiled React assets
and proxies `/api/` to the API without changing the path or turning API errors
into the React shell. Direct event/live-display URLs also load the UI. The UI
runtime contains Nginx and static assets; Node is confined to its build stage.

```bash
docker compose ps
docker compose logs --follow ui session-manager-service db
docker compose exec session-manager-service wget -q -O - http://127.0.0.1:8080/actuator/health/readiness
sh scripts/smoke-assignment-api.sh
```

PostgreSQL must be healthy before the API starts, and API readiness must pass
before the UI starts. Readiness includes database connectivity and exposes only
UP/DOWN; liveness remains independent of the database. Only health is exposed
by Actuator, and it is not routed through the UI proxy. The UI's own health check
checks its HTTP listener. Docker's DNS resolver lets Nginx discover a recreated
API container instead of keeping its old address indefinitely.

The UI and API share the application network. Only the API and database share
the internal data network. Optional host debugger/database access is explicit:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build --detach --wait
```

That override adds loopback-only API 8081 and database 5432 ports. `API_PORT` and
`DB_PORT` customize them. Do not use these overrides to publish publicly. Host-side
frontend/API development is described in [web workflow](web-workflow.md).

## Stop, rebuild and recover

```bash
docker compose down
docker compose up --build --detach --wait
```

Normal shutdown/recreation retains the `postgres-data` named volume. Flyway
migrations and Hibernate schema validation run at API startup. Updating an
existing database password also requires changing that database user's password;
the PostgreSQL initialization environment only initializes an empty volume.

The services use `restart: unless-stopped`. Compose health dependencies control
startup; Docker does not automatically restart a running service merely because
its health status changes. During a database outage API readiness returns 503;
after the database recovers, its connection pool can reconnect. Inspect logs and
readiness before resuming work. Restart a service explicitly when required with
`docker compose restart SERVICE`. The API allows 20 seconds for graceful request
shutdown within a 30-second Compose grace period; UI and database also have
bounded shutdown grace periods.

`docker compose down --volumes` explicitly destroys this stack's event/roster/
assignment data. Use it only for disposable tests or when deletion is intended.
Backups and production recovery policies are outside this revision.

## Build boundaries and verification

Both image builds use the repository root as context. The API needs the parent
POM and sibling core module; the UI needs the locked frontend sources. The API
ships a non-root Java 17 JRE and executable JAR, without Maven/JDK/Node. The UI
uses an unprivileged Nginx runtime. No database credentials enter frontend build
arguments or assets. The API image must remain smaller than 300 MB as measured
by Docker's uncompressed image size.

`python3 scripts/test-compose-contract.py` checks the resolved topology, port
boundaries, required password, startup dependencies and durable volume without
requiring daemon access. Three tests failed against the former two-service
configuration before this implementation; all four now pass.

The **Container build and HTTP smoke** CI job builds both images and waits for
all three real containers. Chromium exercises registration, proposals, leader
assignment, cross-client invalidation, current DOM at print time and CSV download.
`scripts/smoke-container-stack.py --project session-manager-ci` additionally checks
routes/assets/security headers/API errors, image users/tools/size, network/port
boundaries, CRUD, safe CSV, database outage/readiness/liveness/recovery, and data
persistence after recreating all containers without deleting the volume. It
modifies and recreates its specified stack: use a disposable test project only.

Local Docker daemon access is unavailable, so no local image build or three-
container runtime pass is claimed. The earlier PR #13 CI evidence applies only
to the previous API/database topology. This deployment requires its own passing
current-head CI and native review before it is considered delivered.


The first full-stack preflight [CI run 39817001524](https://github.com/auricless/divided-session-manager/actions/runs/39817001524)
passed on the temporary `chore/container-acceptance` branch (f220251): both images,
Chromium's complete workflow and the HTTP/isolation/image/persistence/database
recovery checks succeeded. This is CI evidence, not a local Docker run. The
sequential deployment PR must repeat the checks on its own proposed merge and
pass native review before delivery.


Health probes do not require histogram metrics. The Actuator starter excludes
the unused metrics starter and Jakarta metrics bindings because they pull
HdrHistogram 2.2.2, which has no patched release for CVE-2026-14686. A classpath
regression first detected that library, then passed with it absent while the
healthy/unavailable/liveness tests still pass. The packaged JAR is also checked
for its absence. This removes the dependency rather than accepting its risk.
Browser setup invokes the installed, locked Playwright CLI directly; an
executable policy regression rejects package-install fallback.
