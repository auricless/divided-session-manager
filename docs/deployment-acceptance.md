# SRS v1.1 deployment acceptance

Authority: [SRS v1.1 section 9](https://docs.google.com/document/d/1AuslVQYFWfaqpQPtCLFsc3tIh2b8LNPNU0TySDOxwJE/edit).
The original 28 functional rules and core evidence remain in [the core audit](srs-acceptance-audit.md).

| Requirement | Implementation and verification | Evidence boundary |
| --- | --- | --- |
| WEB-01 independent React frontend | `frontend/package.json`, lockfile, Dockerfile; API-only routing Java tests; frontend build | Local build/tests pass; independent image preflight CI passes |
| WEB-02 registration workflow | React EventList/RegistrationPage; component CRUD/config/validation/exception tests; Chromium registration/proposal/final workflow | Local React/API browser test passes; production proxy preflight CI passes |
| WEB-03 live display | Shared revision-verified feed; two-second polling, remote settings/roster/editor refresh; lifecycle, mismatch and failure/retry regressions | 24 frontend tests pass; Chromium cross-client invalidation passes locally |
| WEB-04 print/CSV | Exact-refresh snapshot committed before print; CSV API escaping, proposal/blocker/warning details and invalidation checks | Component and Chromium DOM-at-print tests pass; actual browser CSV download passes; physical printer untested |
| DEP-01 independent images | Node build/static Nginx runtime; Java build/JRE runtime; PostgreSQL 17.11 | Image build/runtime tools and users checked by actual-stack CI; preflight passes |
| DEP-02 Compose orchestration | Three services, one documented start command | Resolved Compose contract passes; runtime preflight CI passes |
| DEP-03 same-origin routes/proxy | Nginx `/api/` upstream retains paths and backend errors; direct React routes/static assets | HTTP route/asset/error checks and Chromium through production proxy pass in preflight and PR CI |
| DEP-04 private networks/ports | App network UI/API; internal data network API/database; only configurable UI loopback port by default; explicit debug override | Resolved contract passes; running-container network/port checks enforced by actual-stack CI |
| DEP-05 startup/health | PostgreSQL health -> database-aware API readiness -> UI; minimal health payloads | Java readiness regression passes (healthy -> unavailable, liveness stays UP); real PostgreSQL outage/recovery preflight passes |
| DEP-06 durable data | Named PostgreSQL volume; existing Flyway migrations and ORM validation | Prior event/participant restart tests pass; full Compose down/up persistence check enforced by actual-stack CI |
| DEP-07 credentials | Required environment password; no password default; no build argument/runtime UI database credentials | Missing-password contract passes; frontend build has no credential inputs |
| DEP-08 runtime security/size | Unprivileged UI/API; JRE-only API <300 MB; browser security headers | Updated image-size/user/tool and production proxy header checks enforced by actual-stack CI |
| DEP-09 local operation/recovery | Restart policies, bounded graceful shutdown, logs/inspection/rebuild/retention/deletion/recovery instructions | Documented in container runtime; live database recovery enforced by actual-stack CI |
| VER-01 tests/quality | React component/feed and parser regressions; Java PostgreSQL reactor; JaCoCo/Checkstyle/SpotBugs/formatter | 24 frontend and 185 Java tests pass locally with quality checks; Chromium local workflow passes |
| VER-02 actual-stack acceptance | `container-checks.yml`, Chromium `e2e/registration.spec.js`, `smoke-container-stack.py` | Actual-stack CI run 39821249908 passes; a local dev-server pass alone does not establish this criterion |
| VER-03 review/security workflow | Independent PRs in parallel, prerequisites ordered; current-head CI/native review; authenticated Sonar active-risk inspection; dependency audit | PR #17 and PR #18 merged after their own current-head CI/Sonar/native review gates |

[PR #18](https://github.com/auricless/divided-session-manager/pull/18) merged as
`618b107da5a1a6ff8611c4ed58af80c888e5da6a` after all six CI checks and native
review passed at `10179f3` with no findings or unresolved threads. Authenticated
Sonar readback reported zero active dependency risks. [Actual-stack CI run
39821249908](https://github.com/auricless/divided-session-manager/actions/runs/39821249908)
verified the independent images, browser workflow, proxy/security/isolation,
event and participant CRUD, durable data, and database outage/recovery.
Local Docker access and physical printing were not verified; print-DOM and
browser CSV download were verified. Later changes still require their own gates.
