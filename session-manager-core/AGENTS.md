# Core review constraints

Apply these only to affected assignment behavior; consult the matching clauses in [core requirements](../docs/srs-core-requirements.md) when needed.

- Preserve participant counts, configured age boundaries, demographic/life-stage separation, hard cap, minimum group size, Youth priority, and unique facilitator use. The performance target is 200 attendees in under 500 ms; benchmark only when the change puts that target at risk.
- Each final group needs one Leader. Shortages remain proposals with actionable blockers; empty input returns a final empty assignment. Facilitator exceptions must be explicit, visible, and run-scoped, and cannot waive role, participant rules, or the hard cap.
- Keep this library independent of HTTP, Spring, persistence, and serialization configuration. Preserve reusable, immutable results and independently evaluated runs.
