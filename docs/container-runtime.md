# Local container runtime

Scope: SM-22 and SM-23. Build both Maven modules, package the executable service, and run it in a non-root Java 17 JRE image. The Phase 2 Compose stack includes the service and a healthy PostgreSQL 17.11 database with durable storage.

## Build and run

From the repository root, set `POSTGRES_PASSWORD` to a local secret and pass it as `DATABASE_PASSWORD` when starting a standalone service container. The service requires a reachable PostgreSQL instance; use Compose for the complete stack.

```bash
docker build -f session-manager-service/Dockerfile -t session-manager-service:local .
docker run --rm -p 127.0.0.1:8080:8080 \
  -e DATABASE_URL -e DATABASE_USER -e DATABASE_PASSWORD session-manager-service:local
```

Or start Compose:

```bash
docker compose up --build
```

The root build context is required: the service depends on the sibling core module and repository parent POM. The older ticket command using only the service directory as context cannot build this multi-module project from source. Compose uses the same root context with the Dockerfile in `session-manager-service`.

Ports 8080 (HTTP) and 5432 (PostgreSQL) are bound to localhost for development. Run the documented [API sample](assignment-api.md) from another terminal, or `sh scripts/smoke-assignment-api.sh`. The smoke script needs curl and Python 3 on the host; these tools are not added to the runtime image.

Stop Compose with Ctrl+C, then run `docker compose down` to remove containers while retaining PostgreSQL data. `docker compose down --volumes` intentionally deletes local event data. The assignment endpoint remains stateless; event configuration is persistent. `restart: unless-stopped` applies when the stack is left running in the background.

## Acceptance and verification

- The multi-stage build uses Maven with Java 17; only the executable JAR enters the Java 17 JRE runtime stage.
- The runtime uses numeric user/group 10001, exposes 8080, and starts with `java -jar`. No JDK or Maven is copied into the runtime.
- `.dockerignore` excludes Git metadata, local IDE files, build output, and environment files.
- `docker compose config --quiet` must validate the stack.
- The runtime image must be smaller than 300 MB, measured by `docker image inspect` (uncompressed local image size).
- The container must accept the sample request and return FINAL with one staffed group and two attendees.

The **Container build and HTTP smoke** Actions job builds and starts Compose, checks size and non-root/JRE configuration, exercises the endpoint, and cleans up the stack. Maven verification runs separately; the image build skips running unit tests.

Local Docker daemon access was unavailable. PR #13 CI run 39754948591 built and started the actual image, passed the <300 MB, non-root user and JRE-only checks, and verified the assignment HTTP response. Native review passed on 916ed94 before merge as 10e0b0f. The startup-reset regression first reproduced curl exit 56 and now passes with bounded retries. These are CI runtime results; no local Docker build is claimed.

The reusable smoke script has passed against the packaged executable JAR on localhost, returning the expected final assignment. This validates the request/assertion path, not the Docker image.
