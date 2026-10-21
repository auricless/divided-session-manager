# Assignment API

Scope: SM-21, SRS OUT-03 and section 6.2. This stateless Phase 1B wrapper exposes the standalone assignment engine; it does not store an event or publish rosters.

## Acceptance criteria

- `POST /api/v1/sessions/assign` accepts separate request DTOs for configuration, participants, facilitators, and optional explicit facilitator exceptions.
- Configuration requires an explicit positive `capSize`; omitted optional settings preserve core defaults. Participant and facilitator lists are required, but may be empty.
- Successful requests return HTTP 200 with `state`, groups, actionable blockers, warnings, unassigned attendees and reasons, and counts. Staffing shortages remain proposals, not HTTP errors.
- Each exception selects the exact facilitator by its zero-based `facilitatorIndex` in the request roster. Its independent flags permit age-tier and gender mismatches for this run only. It never waives role, cap, or participant rules.
- A mapper translates request and response DTOs outside the controller. Business decisions and validation remain in the plain Java core.
- Invalid configuration, demographics, exception indexes, malformed JSON, and invalid enum values return HTTP 400 with a clean error message and no stack trace.
- Integration tests exercise a real local HTTP server. The runnable service must also accept the documented sample through curl.

## Run a request

From the repository root, install the modules with `mvn install`, then start the service with `mvn -pl session-manager-service spring-boot:run`. In another terminal:

```bash
curl --fail-with-body -sS http://localhost:8080/api/v1/sessions/assign \
  -H 'Content-Type: application/json' \
  --data-binary @session-manager-service/examples/assign-request.json
```

The sample forms one Youth / MALE group, staffed by Sam, with state `FINAL` and two assigned participants. Remove its facilitator and rerun to receive `NON_FINAL_PROPOSAL` with a `MISSING_LEADER` blocker. HTTP 200 means the computation succeeded; inspect `state` before treating a roster as final.

## Request contract

| Field | Meaning |
| --- | --- |
| `config.capSize` | Required positive integer hard cap; facilitators do not count toward it. |
| `config.targetSize` | Optional positive integer, default 4, no larger than cap. A cap below 4 needs an explicit smaller target. |
| `config.genderMode` | `SEPARATED` (default) or `MIXED`. |
| `config.activeTiers` | Nonempty array of `TEENS`, `YOUTH`, `YOUNG_ADULT`; all active by default. |
| `config.lifeStageSubgroupingEnabled` | Boolean, default true. |
| `config.ageTierBoundaries` | Optional object with all three tiers, each containing inclusive integer `minAge` and `maxAge`. Core validation requires ordered, contiguous ranges starting at 13 or above. |
| `participants` | Required array. Each item needs nonblank `name`, positive integer `age`, and `MALE`/`FEMALE` gender. Session-derived Young Adults also need `STUDENT` or `WORKING_PROFESSIONAL` life stage. |
| `facilitators` | Required array. Each item needs nonblank `name`, `LEADER`/`ASSISTANT` role, gender, and nonempty `qualifiedTiers`. |
| `facilitatorExceptions` | Optional array; each item needs a valid zero-based `facilitatorIndex`. Boolean `allowAgeTierMismatch` and `allowGenderMismatch` default to false. |

Omitted or null optional configuration fields retain the core defaults. Omitted or null exceptions mean no exceptions. Null roster entries are invalid. Enum values are case-sensitive. JSON types must match the contract: numeric strings, fractional caps or ages, ordinal enum values, and string boolean flags are rejected.

For example, to explicitly permit the first facilitator's gender mismatch for this request, add:

```json
"facilitatorExceptions": [
  { "facilitatorIndex": 0, "allowGenderMismatch": true }
]
```

The index identifies an entry in this request only, not a stored person. Names are not unique identifiers. Resubmitting without the exception restores strict matching. There is no authentication or persistence in this Phase 1B wrapper; exceptions are supplied by its caller.

## Response contract

The response contains `state` (`FINAL` or `NON_FINAL_PROPOSAL`), `groups`, `blockers`, `unassignedParticipants`, `warnings`, `totalGroups`, and `totalAssignedParticipants`.

Each group exposes its tier/gender/life-stage targets, participant records including their session-derived `ageTier`, leader, optional assistant, and participant-only `size`. Mixed or unsplit targets and unfilled staff slots are null. Each blocker has a `code`, one-based `groupNumber`, and actionable `message`. Unassigned entries retain their participant details and a `reason`. An empty or wholly excluded roster can be final with zero groups.

Invalid requests return HTTP 400 and `{"message":"..."}`. Staffing shortages and feasible-constraint conflicts return HTTP 200 proposals with blockers; they are not malformed input.

Automated PR review remains required before this implementation is merged.
