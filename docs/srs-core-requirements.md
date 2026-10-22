# First-release core requirements

Source: [SRS_SmallGroupSessionManager_v1.0](https://docs.google.com/document/d/1AuslVQYFWfaqpQPtCLFsc3tIh2b8LNPNU0TySDOxwJE/edit). Version 1.0 draft; requirements snapshot captured 2026-10-22. The Google Drive SRS is the ultimate design authority. Keep this review snapshot synchronized when that document changes; ticket checklists and implementation notes cannot override it.

## Functional requirements

- **CFG-01:** The system shall use SEPARATED gender mode by default unless the event organizer selects MIXED for the event.
- **CFG-02:** The system shall use a default target group size of 4 participants unless the event organizer supplies another positive target size.
- **CFG-03:** The system shall require a maximum group cap parameter to be explicitly set by the event organizer. There is no system default — the organizer must provide this value for every event to encourage deliberate group size planning.
- **CFG-04:** The system shall accept active age tiers and configurable per-event age boundaries, using 13–15, 16–22, and 23+ as defaults.
- **CFG-05:** The system shall split Young Adults by STUDENT and WORKING_PROFESSIONAL life stage by default and allow the organizer to turn that split off per event.
- **CFG-06:** Configuration validation shall reject a missing cap, a target size of zero or less, a target larger than the cap, and invalid age boundaries such as gaps or overlaps. The cap remains hard for a run; an organizer may change the event cap and rerun.
- **PRT-01:** Each participant record shall contain: name, age (integer), and gender (MALE / FEMALE) for the first release.
- **PRT-02:** The system shall derive age tier from the participant's age using the event's configured boundaries, defaulting to Teens (13–15), Youth (16–22), and Young Adult (23+).
- **PRT-03:** Participants in the Young Adult tier shall carry a life stage attribute: STUDENT or WORKING_PROFESSIONAL.
- **PRT-04:** The system shall accept a list of zero or more participants as input. An empty list shall return an empty assignment result.
- **PRT-05:** Participants below age 13 shall be flagged with a warning and excluded from automated assignment (handled separately).
- **FAC-01:** Each facilitator record shall contain: name, role (LEADER or ASSISTANT), gender, and a list of age tiers they are qualified for.
- **FAC-02:** Each final group shall have exactly one LEADER facilitator. A non-final proposal may show an unstaffed group as a blocker.
- **FAC-03:** Each group may optionally be assigned one ASSISTANT facilitator if available.
- **FAC-04:** In SEPARATED mode, the system shall use a facilitator of the group's gender by default. A different-gender assignment is allowed only through an explicit organizer exception.
- **FAC-05:** If qualified facilitators cannot cover all groups, the system shall consolidate within the configured cap where possible and return a non-final proposal with actionable blockers. It shall not finalize a leaderless group or relax a demographic rule automatically. An organizer may explicitly override facilitator qualification or gender preference; no reason field is required in the first release.
- **ALG-01:** In SEPARATED gender mode, participants shall only be placed in groups with participants of the same gender.
- **ALG-02:** Participants shall only be placed in groups with other participants of the same age tier.
- **ALG-03:** Within the Young Adult tier, if life-stage sub-grouping is enabled, Students and Working Professionals shall be placed in separate groups.
- **ALG-04:** The algorithm shall aim to produce groups at the target size before opening a new group.
- **ALG-05:** No group shall exceed the maximum cap size as configured for the event. The algorithm shall treat the configured cap as a hard constraint that cannot be exceeded regardless of facilitator availability.
- **ALG-06:** The algorithm shall distribute remaining participants as evenly as possible when a tier's count does not divide cleanly into the target size.
- **ALG-07:** The Youth tier shall be given priority for sub-group splitting if participant count exceeds the cap in that tier.
- **ALG-08:** The system shall not create a group with fewer than 2 participants unless it is the only group in its tier/gender segment.
- **OUT-01:** The system shall return groups and an explicit final or non-final proposal state. A non-final proposal shall identify blockers that require organizer action.
- **OUT-02:** The result shall include metadata: total groups formed, total participants assigned, any warnings.
- **OUT-03:** The result shall be serializable to JSON for consumption by a downstream service.
- **OUT-04:** If a participant cannot be placed in a valid group, the result shall list that participant as unassigned with a reason. A group lacking a Leader remains a non-final proposal with a blocker.

## Algorithm and non-functional constraints

- Partition by gender when SEPARATED, age tier always, and Young Adult life stage when enabled. Preserve that order of constraints and the hard cap.
- Start near ceil(segment size / target), distribute evenly, assign one Leader and an optional Assistant, and consolidate within the hard cap on shortage. Finalize only when every required condition holds; otherwise retain actionable proposal blockers.
- A singleton is allowed only when it is the only group in its tier/gender segment (ALG-08). If other hard constraints prevent compliance, keep those constraints and report a proposal rather than silently relaxing a rule.
- Under 13 attendees are excluded with warnings; ages outside configured tiers are unassigned with an UNKNOWN_TIER reason.
- Up to 200 attendees must be assigned in under 500 ms on a standard developer machine. Each invocation is independent and stateless.
- Valid input combinations must not cause unhandled exceptions. Invalid configuration and required inputs are validated before assignment.
- Business rules belong in configurable core objects. Prefer single-responsibility methods, meaningful names, and no magic numbers. Target at least 80% test coverage of the core algorithm package.
- First release: Java 17+ standalone Maven JAR with JUnit 5 and in-memory result objects. No web-framework dependency is required. The Spring Boot wrapper, Docker image, database, UI, authentication, and roster publishing are later milestones.
