# Divided Session Manager

Divided Session Manager is a project for assigning church event attendees to small discussion groups and facilitators. It addresses the registration team's need to place walk-ins quickly while keeping group rules and capacity visible. The intended product is an event workflow; this repository currently contains its Java grouping engine, not a usable registration application.

The [project tracker](https://app.notion.com/p/Session-Manager-Project-Tracker-31c61e61aca181538410fd3e3cdebdcc) holds the development tickets. The [draft SRS v1.0](https://docs.google.com/document/d/1AuslVQYFWfaqpQPtCLFsc3tIh2b8LNPNU0TySDOxwJE/edit?usp=drivesdk) is the ultimate source of truth for design; it takes precedence over conflicting ticket checklists. The first release is the core assignment library: it produces a final assignment when all required conditions are met and accepts explicit organizer overrides for recomputation. The Spring Boot service, storage, screens, and roster publishing are outside that first release.

## Current state

| Area | State |
| --- | --- |
| `session-manager-core` | Implemented: domain objects, segmentation, group sizing, facilitator assignment, result and warnings, with JUnit tests. Phase 1A needs review against the intended rules and coverage target. |
| `session-manager-service` | Placeholder Maven module only. No Spring Boot application or REST endpoint yet. |
| Docker and Compose | Not implemented. |
| Registration UI, persistence, printable rosters | Proposed for a later phase; not implemented. |

## What the core accepts and returns

`GroupAssignmentService.assign(config, participants, facilitators)` accepts a `SessionConfig`, attendees, and facilitators. It returns an `AssignmentResult` with groups, an explicit state, actionable blockers, unassigned attendees with reasons, and warnings. Facilitators do not count toward participant group size.

Results are immutable snapshots, including their groups and facilitator qualifications. `getState()` returns `FINAL` when every formed group has a LEADER and respects the supplied event cap and minimum-size rule; otherwise it returns `NON_FINAL_PROPOSAL`. An empty assignment is final. `getBlockers()` identifies affected groups by their one-based position in `getGroups()`, with a code and an action for the organizer. Excluded attendees remain in the unassigned list and do not block a staffed assignment. Mutable `Group` objects can still be used during assignment construction; mutation methods on groups returned in a result throw `UnsupportedOperationException`.

Configuration defaults to `SEPARATED` gender mode, target size 4, all age tiers active, and Young Adult life-stage splitting enabled. The organizer must explicitly supply a positive cap for every session. There is no stored event, registration process, or user interface.

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

The code processes Youth segments first. Configuration checks the required positive cap, positive target no larger than cap, a nonempty active-tier list without nulls, and valid age boundaries. Other inputs are not comprehensively validated yet.

ALG-08 permits a one-person group only when it is the only group in its tier/gender segment. If the cap or enabled life-stage separation makes that impossible, the engine preserves those constraints and returns `MINIMUM_GROUP_SIZE` blockers. For example, three attendees under a cap of two remain a proposal with groups of two and one; the engine never raises the cap or silently merges life stages.

## Gaps against the first-release requirements

| Requirement | Current state |
| --- | --- |
| Configurable age-tier boundaries (CFG-04/06, PRT-02, NFR 5.3) | Implemented per session, including validation and assignment integration; defaults remain in `AgeTier`. |
| No one-person group when a segment has multiple groups (ALG-08) | Sizing avoids singletons where feasible; unavoidable conflicts produce a proposal with blockers. |
| At least 80% core algorithm test coverage (NFR 5.3) | Tests exist; coverage has not been measured here. |
| Review and overrides | Explicit facilitator qualification/gender exceptions are supported through a run-scoped overload. Further participant-level editing remains outside the first release. |

## Project plan

- **Phase 1A — core library:** Implementation exists. Align shortage handling and configurable defaults with the intended workflow; measure the tracker’s 80% core algorithm coverage goal before marking the milestone complete.
- **Phase 1B — service wrapper (planned, outside first release):** Add the Spring Boot module, `POST /api/v1/sessions/assign`, service tests, and runnable container setup after the core contract is stable.
- **Later event platform:** The current proposal includes registration, an organizer review screen, persistence, group displays, and printable rosters. These are outside the first algorithm deliverable.

## Decisions to resolve

- Which other overrides can an admin make, such as moving one attendee or merging groups? The configured cap remains hard for a run; an organizer may change the event cap and rerun. Override inputs need an explicit flag, but no reason in the first release.
- What are the permitted limits on participant-level overrides?
- What should happen when age, gender, or Young Adult life stage is missing, and how are walk-ins or edits handled after groups are formed?
- Which later platform features are essential for the first real event, and what attendee data must be retained?

## Development

PRs use [subscription-backed Codex review](docs/automated-pr-review.md) alongside Maven verification in GitHub Actions. Enable the repository connection and automatic review in Codex account settings. Local work follows TDD; fixes are pushed for a fresh native review. The SRS remains the ultimate design authority, with a [versioned requirements snapshot](docs/srs-core-requirements.md) available to the reviewer.

This is a Java 17 multi-module Maven project. Maven 3.6+ is required.

```bash
mvn test
mvn package
```

Run these commands from the repository root. They currently build and test the core plus the placeholder service module. The core entry point is `org.c3cavite.core.service.GroupAssignmentService`; there is no application command or HTTP endpoint yet.
