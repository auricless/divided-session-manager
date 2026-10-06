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

`GroupAssignmentService.assign(config, participants, facilitators)` accepts a `SessionConfig`, attendees, and facilitators. It returns an `AssignmentResult` with groups, unassigned attendees with reasons, and warnings. Facilitators do not count toward participant group size.

The current configuration specifies a gender mode (`SEPARATED` or `MIXED`), target and maximum participant counts per group, active age tiers, and whether to split Young Adults by life stage. There is no stored event, registration process, user interface, or default configuration in the code.

Current age tiers are Teens (13–15), Youth (16–22), and Young Adults (23+). Attendees under 13 or in an inactive tier are returned as unassigned. For Young Adults, the life-stage values are `STUDENT` and `WORKING_PROFESSIONAL`. The first-release defaults use `MALE` and `FEMALE` gender values, these age bands, and a Young Adult life-stage split that organizers can turn off per event. The intended core must let organizers configure age-tier boundaries per event; the current `AgeTier` enum does not.

The draft SRS specifies `SEPARATED` gender mode and target size 4 as defaults, with a cap supplied for each event. The current `SessionConfig` requires callers to supply all of these values and the life-stage toggle; it does not apply defaults itself.

## Current assignment behavior

1. Eligible attendees are segmented by age tier, by gender when `SEPARATED`, and by Young Adult life stage when enabled.
2. Each segment starts with `ceil(attendee count / target size)` groups. Attendees are distributed evenly within that segment.
3. When that count exceeds the number of available leaders qualified for the segment's tier, the engine may reduce the group count toward the configured cap. This calculation does not reserve leaders across segments.
4. Leaders and then optional assistants are assigned from the supplied lists. Qualification by age tier is required. In separated mode, a matching facilitator gender is preferred, but a different gender is currently accepted.
5. A group without a qualified available leader remains in the result with a `null` leader and a warning. This is current behavior. The intended behavior is to return a **non-final proposal** showing the shortage so an admin can resolve it. An admin may explicitly assign a facilitator despite the normal age-tier qualification or gender preference. A leaderless group must never be finalized, and the engine must not automatically relax a demographic boundary. An explicit override flag is enough for the first release; a reason is not required.

The code processes Youth segments first. The current result has no explicit draft/final state or override workflow. `SessionConfig.create(...)` checks positive target size, target no larger than cap, and a nonempty active-tier list. Other inputs are not comprehensively validated yet.

## Gaps against the first-release requirements

| Requirement | Current state |
| --- | --- |
| Configurable age-tier boundaries (NFR 5.3) | Required for the first release; boundaries are currently fixed in `AgeTier`. |
| No one-person group when a segment has multiple groups (ALG-08) | The current group-count and distribution logic can produce one. |
| At least 80% core algorithm test coverage (NFR 5.3) | Tests exist; coverage has not been measured here. |
| Review and overrides | The first-release core must accept explicit organizer overrides and recompute or validate assignments; the precise input and result contract is still being decided. |

## Project plan

- **Phase 1A — core library:** Implementation exists. Align shortage handling and configurable defaults with the intended workflow; measure the tracker’s 80% core algorithm coverage goal before marking the milestone complete.
- **Phase 1B — service wrapper (planned, outside first release):** Add the Spring Boot module, `POST /api/v1/sessions/assign`, service tests, and runnable container setup after the core contract is stable.
- **Later event platform:** The current proposal includes registration, an organizer review screen, persistence, group displays, and printable rosters. These are outside the first algorithm deliverable.

## Decisions to resolve

- Which other overrides can an admin make, such as moving one attendee or merging groups? The configured cap remains hard for a run; an organizer may change the event cap and rerun. Override inputs need an explicit flag, but no reason in the first release.
- How should the engine represent a non-final proposal, its blockers, and the explicit facilitator exception in its result and inputs?
- How should the engine validate configurable age-tier boundaries (for example, gaps or overlaps), and what are the permitted limits on participant-level overrides?
- What should happen when age, gender, or Young Adult life stage is missing, and how are walk-ins or edits handled after groups are formed?
- Which later platform features are essential for the first real event, and what attendee data must be retained?

## Development

This is a Java 17 multi-module Maven project. Maven 3.6+ is required.

```bash
mvn test
mvn package
```

Run these commands from the repository root. They currently build and test the core plus the placeholder service module. The core entry point is `org.c3cavite.core.service.GroupAssignmentService`; there is no application command or HTTP endpoint yet.
