# Divided Session Manager

Divided Session Manager is a project for assigning church event attendees to small discussion groups and facilitators. It addresses the registration team's need to place walk-ins quickly while keeping group rules and capacity visible. The intended product is an event workflow; this repository contains its Java grouping engine and stateless assignment API. Registration screens are a later milestone.

The [project tracker](https://app.notion.com/p/Session-Manager-Project-Tracker-31c61e61aca181538410fd3e3cdebdcc) holds the development tickets. The [draft SRS v1.0](https://docs.google.com/document/d/1AuslVQYFWfaqpQPtCLFsc3tIh2b8LNPNU0TySDOxwJE/edit?usp=drivesdk) is the ultimate source of truth for design; it takes precedence over conflicting ticket checklists. The first release is the core assignment library: it produces a final assignment when all required conditions are met and accepts explicit organizer overrides for recomputation. The Spring Boot service, storage, screens, and roster publishing are outside that first release.

## Current state

| Area | State |
| --- | --- |
| `session-manager-core` | Domain objects, segmentation, sizing, facilitator assignment, immutable final/proposal results, explicit exceptions, and coverage checks are implemented. Phase 1A passed Java 17 CI and native Codex review and is merged. |
| `session-manager-service` | Runnable Spring Boot wrapper and assignment API, merged after Java 17/21 CI, Sonar and native Codex review. |
| Docker and Compose | Multi-stage non-root JRE image and Compose stack merged in PR #13 after CI built the image, verified size <300 MB/JRE/user, passed the HTTP smoke and native review. |
| PostgreSQL and event CRUD | Flyway/JPA event persistence and REST CRUD implemented; real database CI and native review are required before delivery. |
| Registration UI, participant CRUD, printable rosters | Remaining Phase 2 work, tracked in the SRS acceptance audit. |

## What the core accepts and returns

`GroupAssignmentService.assign(config, participants, facilitators)` accepts a `SessionConfig`, attendees, and facilitators. It returns an `AssignmentResult` with groups, an explicit state, actionable blockers, unassigned attendees with reasons, and warnings. Facilitators do not count toward participant group size.

Results are immutable snapshots, including their groups and facilitator qualifications. `getState()` returns `FINAL` when every formed group has a LEADER and respects the supplied event cap and minimum-size rule; otherwise it returns `NON_FINAL_PROPOSAL`. An empty assignment is final. `getBlockers()` identifies affected groups by their one-based position in `getGroups()`, with a code and an action for the organizer. Excluded attendees remain in the unassigned list and do not block a staffed assignment. Mutable `Group` objects can still be used during assignment construction; mutation methods on groups returned in a result throw `UnsupportedOperationException`.

Configuration defaults to `SEPARATED` gender mode, target size 4, all age tiers active, and Young Adult life-stage splitting enabled. The organizer must explicitly supply a positive cap for every session. The event API stores named configurations; participant registration and screens remain separate Phase 2 work.

Default age tiers are Teens (13–15), Youth (16–22), and Young Adults (23+). Per-session inclusive ranges can override these boundaries. All three ranges must be present, ordered, and contiguous, with no overlaps. Ranges start at 13 or above; attendees outside the configured interval or in an inactive tier are returned as unassigned. For Young Adults, the life-stage values are `STUDENT` and `WORKING_PROFESSIONAL`.

```java
SessionConfig defaults = SessionConfig.builder().capSize(6).build();

SessionConfig custom = SessionConfig.builder()
        .capSize(8)
        .targetSize(5)
        .genderMode(GenderMode.MIXED)
        .lifeStageSubgroupingEnabled(false)
        .ageTierBoundaries(Map.of(
                AgeTier.TEENS, new AgeRange(13, 17),
                AgeTier.YOUTH, new AgeRange(18, 25),
                AgeTier.YOUNG_ADULT, new AgeRange(26, Integer.MAX_VALUE)))
        .build();
```

`SessionConfig.create(...)` remains available for explicit settings. Configuration copies its collections so later caller changes cannot alter a built session. `Participant.getAgeTier(config)` resolves the tier for an event. Newly constructed input participants and `AgeTier.fromAge(...)` use the default bands. Assignment results contain immutable participant copies whose no-argument `getAgeTier()` exposes the session's derived tier, including no tier for an attendee outside the configured ranges. Original inputs remain unchanged and reusable across sessions. Segmentation and assignment always use the event's configuration. Participant values reject blank names, nonpositive ages, and missing gender; session-derived Young Adults require a life stage. Facilitators validate name, role, gender, and a non-null qualification list without null elements. An empty list is valid, but the facilitator remains ineligible unless an explicit age-tier exception is supplied. Groups validate facilitator roles and expose participant-only size. Participant equality includes name, age, gender, and life stage.

## Current assignment behavior

1. Eligible attendees are segmented by age tier, by gender when `SEPARATED`, and by Young Adult life stage when enabled.
2. Each segment starts near `ceil(attendee count / target size)`, bounded by the hard cap and ALG-08's minimum of two attendees where possible. Attendees are distributed evenly within that segment.
3. Leaders are matched across all segments, with Youth first. Flexible leaders can be reassigned to preserve coverage for restricted groups. When staffing is short, the engine tries segment consolidation that reduces the number of missing leaders while keeping the cap hard, and recomputes staffing against the shared pool.
4. Leaders and then optional assistants are assigned from the supplied lists. Age-tier qualification and, in separated mode, matching facilitator gender are required unless explicitly waived for that facilitator. Candidates needing no exception are preferred. Each roster instance can staff at most one group; duplicate instances are rejected.
5. A group without a leader remains in a **non-final proposal**, with a warning and an actionable blocker. A leaderless group cannot be finalized.

For a run with organizer exceptions, use the four-argument overload. Each exception refers to the exact facilitator instance in that run's roster and independently permits an age-tier or gender mismatch. It never changes a facilitator's role, participant segmentation, or the hard cap. No reason field is required. Applied exceptions appear in result warnings; a later run without exceptions restores the default rules.

```java
FacilitatorException permission = new FacilitatorException(leader,
        false, // keep age-tier qualification required
        true); // explicitly permit this facilitator's gender mismatch
AssignmentResult result = service.assign(config, participants, facilitators, List.of(permission));
```

The code processes Youth segments first. Configuration checks the required positive cap, positive target no larger than cap, a nonempty active-tier list without nulls, and valid age boundaries. Participant, facilitator and explicit exception inputs are validated before assignment.

ALG-08 permits a one-person group only when it is the only group in its tier/gender segment. If the cap or enabled life-stage separation makes that impossible, the engine preserves those constraints and returns `MINIMUM_GROUP_SIZE` blockers. For example, three attendees under a cap of two remain a proposal with groups of two and one; the engine never raises the cap or silently merges life stages.

## Gaps against the first-release requirements

| Requirement | Current state |
| --- | --- |
| Configurable age-tier boundaries (CFG-04/06, PRT-02, NFR 5.3) | Implemented per session, including validation and assignment integration; defaults remain in `AgeTier`. |
| No one-person group when a segment has multiple groups (ALG-08) | Sizing avoids singletons where feasible; unavoidable conflicts produce a proposal with blockers. |
| At least 80% core algorithm test coverage (NFR 5.3) | JaCoCo measures 100% line coverage and 99.28% branch coverage in the service package with 114 passing tests. Maven verify checks the package against the 80% line / 70% branch targets. |
| Review and overrides | Explicit facilitator qualification/gender exceptions are supported through a run-scoped overload. Further participant-level editing remains outside the first release. |

## Project plan

- **Phase 1A — core library:** Core behavior, coverage checks, Java 17 CI, and native Codex review are complete and merged.
- **Phase 1B — service wrapper (outside first release):** The Spring Boot module and `POST /api/v1/sessions/assign` are implemented locally with service tests. The wrapper/API and Docker/Compose passed CI and native review and merged in PRs #11–13.
- **Later event platform:** The current proposal includes registration, an organizer review screen, persistence, group displays, and printable rosters. These are outside the first algorithm deliverable.

## Decisions to resolve

- Which other overrides can an admin make, such as moving one attendee or merging groups? The configured cap remains hard for a run; an organizer may change the event cap and rerun. Override inputs need an explicit flag, but no reason in the first release.
- What are the permitted limits on participant-level overrides?
- What should happen when age, gender, or Young Adult life stage is missing, and how are walk-ins or edits handled after groups are formed?
- Which later platform features are essential for the first real event, and what attendee data must be retained?

## Development

PRs use [subscription-backed Codex review](docs/automated-pr-review.md) alongside Maven verification in GitHub Actions. Enable the repository connection and automatic review in Codex account settings. Local work follows TDD; fixes are pushed for a fresh native review. The SRS remains the ultimate design authority, with a [versioned requirements snapshot](docs/srs-core-requirements.md) available to the reviewer.

This is a Java 17 multi-module Maven project. Maven 3.6.3+ is required.

```bash
mvn test
mvn package
mvn verify
```

Run these commands from the repository root. They build and test the standalone core and the Spring Boot wrapper. The core entry point is `org.c3cavite.core.service.GroupAssignmentService`.

To start the wrapper on port 8080, first install the reactor artifacts, then run from the service module:

Start PostgreSQL and export `DATABASE_PASSWORD` as described in [database setup](docs/event-api.md) before running the service.

```bash
mvn install
cd session-manager-service
mvn spring-boot:run
```

Alternatively, after packaging, run `java -jar session-manager-service/target/session-manager-service-1.0-SNAPSHOT.jar` from the repository root. Stop the process with Ctrl+C.

POST the [sample request](session-manager-service/examples/assign-request.json) to `/api/v1/sessions/assign`; see the [API contract and curl instructions](docs/assignment-api.md). Successful computations return final assignments or proposals with blockers. Invalid input returns HTTP 400 with a clean message. The stateless assignment endpoint stores no request data. The separate event API persists event configuration in PostgreSQL; see [event API and database setup](docs/event-api.md).

For the container setup, set `POSTGRES_PASSWORD`, then run `docker compose up --build` from the repository root and use the same sample on localhost:8080. See [container commands and verification status](docs/container-runtime.md).

Spring Boot 4.1.1 is managed through a BOM imported only by the service module. The MVC starter is `spring-boot-starter-webmvc`, the current replacement for the older `spring-boot-starter-web` name in SM-20. The core has no Spring dependencies; check with `mvn dependency:tree -pl session-manager-core`.

`mvn verify` generates `session-manager-core/target/site/jacoco/index.html` and `jacoco.xml`. The report includes domain classes, while coverage requirements apply only to `org.c3cavite.core.service`. A deliberately stricter 100% branch probe was verified to fail, confirming that the package filter enforces the rule. The one uncovered branch is the null-gender guard for a manually constructed group in SEPARATED mode; the validated assignment entry point does not create that combination.

The PR workflow enforces the same JaCoCo thresholds through Maven verify and uploads the HTML/XML report as a `jacoco-pr-*` Actions artifact for 14 days. Reports are retained when a coverage check fails after report generation. A successful build without its expected coverage report fails artifact publication.

Java style, formatting, bug checks, and SonarQube Cloud setup are documented in [Java quality checks](docs/java-quality.md).

The service startup integration test invokes the actual main method. JaCoCo also writes its report under `session-manager-service/target/site/jacoco/`; CI retains both module reports. Algorithm coverage thresholds remain scoped to the core service package.

See the [full SRS acceptance audit](docs/srs-acceptance-audit.md) for requirement-to-test evidence and the remaining Phase 2 roadmap.
