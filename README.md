# Divided Session Manager

Divided Session Manager is a project for assigning church event attendees to small discussion groups and facilitators. It addresses the registration team's need to place walk-ins quickly while keeping group rules and capacity visible. The intended product is an event workflow; this repository currently contains its Java grouping engine, not a usable registration application.

The [project tracker](https://app.notion.com/p/Session-Manager-Project-Tracker-31c61e61aca181538410fd3e3cdebdcc) holds the development tickets. The [draft SRS v1.0](https://docs.google.com/document/d/1AuslVQYFWfaqpQPtCLFsc3tIh2b8LNPNU0TySDOxwJE/edit?usp=drivesdk) records the first-release requirements and planned later phases. The first release is the core assignment library: it should produce a final assignment automatically when all required conditions are met and accept explicit organizer overrides for recomputation or validation. The Spring Boot service, storage, screens, and roster publishing are outside that first release.

## Current state

| Area | State |
| --- | --- |
| `session-manager-core` | Implemented: domain objects, segmentation, group sizing, facilitator assignment, result and warnings, with JUnit tests. Phase 1A needs review against the intended rules and coverage target. |
| `session-manager-service` | Placeholder Maven module only. No Spring Boot application or REST endpoint yet. |
| Docker and Compose | Not implemented. |
| Registration UI, persistence, printable rosters | Proposed for a later phase; not implemented. |

## What the core accepts and returns

`GroupAssignmentService.assign(config, participants, facilitators)` accepts a `SessionConfig`, attendees, and facilitators. It returns an `AssignmentResult` with groups, an explicit state, actionable blockers, unassigned attendees with reasons, and warnings. Facilitators do not count toward participant group size.

Results are immutable snapshots, including their groups and facilitator qualifications. `getState()` returns `FINAL` when every formed group has a LEADER and respects the supplied event cap; otherwise it returns `NON_FINAL_PROPOSAL`. An empty assignment is final. `getBlockers()` identifies affected groups by their one-based position in `getGroups()`, with a code and an action for the organizer. Excluded attendees remain in the unassigned list and do not block a staffed assignment. Mutable `Group` objects can still be used during assignment construction; mutation methods on groups returned in a result throw `UnsupportedOperationException`.

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

`SessionConfig.create(...)` remains available for explicit settings. Configuration copies its collections so later caller changes cannot alter a built session. `Participant.getAgeTier(config)` resolves the tier for an event. Newly constructed input participants and `AgeTier.fromAge(...)` use the default bands. Assignment results contain immutable participant copies whose no-argument `getAgeTier()` exposes the session's derived tier, including no tier for an attendee outside the configured ranges. Original inputs remain unchanged and reusable across sessions. Segmentation and assignment always use the event's configuration. Participant values reject blank names and nonpositive ages; their equality includes name, age, gender, and life stage.

## Current assignment behavior

1. Eligible attendees are segmented by age tier, by gender when `SEPARATED`, and by Young Adult life stage when enabled.
2. Each segment starts with `ceil(attendee count / target size)` groups. Attendees are distributed evenly within that segment.
3. When that count exceeds the number of available leaders qualified for the segment's tier, the engine may reduce the group count toward the configured cap. This calculation does not reserve leaders across segments.
4. Leaders and then optional assistants are assigned from the supplied lists. Qualification by age tier is required. In separated mode, a matching facilitator gender is preferred, but a different gender is currently accepted.
5. A group without a leader remains in a **non-final proposal**, with a warning and an actionable blocker. Explicit facilitator exceptions are the next iteration; a leaderless group cannot be finalized.

The code processes Youth segments first. Configuration checks the required positive cap, positive target no larger than cap, a nonempty active-tier list without nulls, and valid age boundaries. Other inputs are not comprehensively validated yet.

## Gaps against the first-release requirements

| Requirement | Current state |
| --- | --- |
| Configurable age-tier boundaries (CFG-04/06, PRT-02, NFR 5.3) | Implemented per session, including validation and assignment integration; defaults remain in `AgeTier`. |
| No one-person group when a segment has multiple groups (ALG-08) | The current group-count and distribution logic can produce one. |
| At least 80% core algorithm test coverage (NFR 5.3) | Tests exist; coverage has not been measured here. |
| Review and overrides | The first-release core must accept explicit organizer overrides and recompute or validate assignments; the precise input and result contract is still being decided. |

## Project plan

- **Phase 1A — core library:** Implementation exists. Align shortage handling and configurable defaults with the intended workflow; measure the tracker’s 80% core algorithm coverage goal before marking the milestone complete.
- **Phase 1B — service wrapper (planned, outside first release):** Add the Spring Boot module, `POST /api/v1/sessions/assign`, service tests, and runnable container setup after the core contract is stable.
- **Later event platform:** The current proposal includes registration, an organizer review screen, persistence, group displays, and printable rosters. These are outside the first algorithm deliverable.

## Decisions to resolve

- Which other overrides can an admin make, such as moving one attendee or merging groups? The configured cap remains hard for a run; an organizer may change the event cap and rerun. Override inputs need an explicit flag, but no reason in the first release.
- How should the explicit facilitator exception be represented in assignment inputs?
- What are the permitted limits on participant-level overrides?
- What should happen when age, gender, or Young Adult life stage is missing, and how are walk-ins or edits handled after groups are formed?
- Which later platform features are essential for the first real event, and what attendee data must be retained?

## Development

This is a Java 17 multi-module Maven project. Maven 3.6+ is required.

```bash
mvn test
mvn package
```

Run these commands from the repository root. They currently build and test the core plus the placeholder service module. The core entry point is `org.c3cavite.core.service.GroupAssignmentService`; there is no application command or HTTP endpoint yet.
