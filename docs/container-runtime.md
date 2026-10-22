# Local container runtime

Scope: SM-22 and SM-23. Build both Maven modules, package the executable service, and run it in a non-root Java 17 JRE image. The local Compose stack has one service and no database.

## Build and run

From the repository root:

```bash
docker build -f session-manager-service/Dockerfile -t session-manager-service:local .
docker run --rm -p 127.0.0.1:8080:8080 session-manager-service:local
```

Or start Compose:

```bash
docker compose up --build
```

The root build context is required: the service depends on the sibling core module and repository parent POM. The older ticket command using only the service directory as context cannot build this multi-module project from source. Compose uses the same root context with the Dockerfile in `session-manager-service`.

Port 8080 is bound to localhost for development. Run the documented [API sample](assignment-api.md) from another terminal, or `sh scripts/smoke-assignment-api.sh`. The smoke script needs curl and Python 3 on the host; these tools are not added to the runtime image.

Stop Compose with Ctrl+C, then run `docker compose down` to remove the local stack. The service stores no state. `restart: unless-stopped` applies when the stack is left running in the background.

## Acceptance and verification

- The multi-stage build uses Maven with Java 17; only the executable JAR enters the Java 17 JRE runtime stage.
- The runtime uses numeric user/group 10001, exposes 8080, and starts with `java -jar`. No JDK or Maven is copied into the runtime.
- `.dockerignore` excludes Git metadata, local IDE files, build output, and environment files.
- `docker compose config --quiet` must validate the stack.
- The runtime image must be smaller than 300 MB, measured by `docker image inspect` (uncompressed local image size).
- The container must accept the sample request and return FINAL with one staffed group and two attendees.

The **Container build and HTTP smoke** Actions job builds and starts Compose, checks size and non-root/JRE configuration, exercises the endpoint, and cleans up the stack. Maven verification runs separately; the image build skips running unit tests.

At implementation time the local Docker daemon denied access. Image build, runtime behavior, and size remain unverified until that CI job runs successfully or local daemon access is available. Compose syntax and published image tags can be checked without daemon access; they do not prove a runnable image.

The reusable smoke script has passed against the packaged executable JAR on localhost, returning the expected final assignment. This validates the request/assertion path, not the Docker image.
