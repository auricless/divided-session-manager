# Phase 1A implementation progress

Sources: [SRS v1.0](https://docs.google.com/document/d/1AuslVQYFWfaqpQPtCLFsc3tIh2b8LNPNU0TySDOxwJE/edit)
and [Notion tickets](https://app.notion.com/p/ffe4ef31d5d14ce28291f1839ba3fceb).
The first release is the standalone Java core library. The service wrapper is a later milestone.

## Iteration 1: configuration and event-specific age tiers

Tickets: SM-6, SM-8, SM-10, SM-26.

Acceptance criteria:

- Apply SEPARATED, target 4, all age tiers, and enabled Young Adult life-stage splitting by default (CFG-01/02/04/05).
- Require an explicit positive cap; reject nonpositive targets and targets above the cap (CFG-03/06).
- Support immutable per-event age boundaries, retaining 13–15 / 16–22 / 23+ defaults; reject missing ranges, gaps, overlaps, and reversed ranges (CFG-04/06).
- Resolve participant tiers against the current session during segmentation and assignment, including active-tier filtering (PRT-02).
- Preserve the existing factory and default participant getter for existing callers; add session-aware tier lookup.
- Complete participant value-object validation and equality from SM-8; leave positive under-13 ages available for unassigned reporting (PRT-05).
- Verify with failing tests before implementation, then the full Maven reactor.

Boundary policy: configure all three named tier ranges independently of the active-tier selection.
Ranges are inclusive, follow tier order, and are contiguous; active tiers select which attendees are eligible.
The supported minimum is 13. Ages outside the configured interval have no tier.
The first or last range may bound the supported interval, matching the SRS UNKNOWN_TIER case.

TDD evidence:

- Baseline: 26 tests passed.
- Existing API regression tests: 38 tests ran, with 11 expected failures before implementation.
- Additional builder, range, and integration tests were added before their production APIs.
- With the domain APIs implemented, 65 tests ran with three expected integration failures: segmentation, active-tier filtering, and UNKNOWN_TIER handling still used default bands.
- After integrating session-aware lookup, all 65 tests passed with zero failures or errors.
- Full `mvn verify` passed and built both module JARs on Java 21 with the existing Java 17 source/target settings. The service JAR remains an empty placeholder.

Iteration 1 is implemented and verified; review is pending. No coverage claim is made yet.

## Next iterations

- SM-12: explicit final/proposal results and actionable blockers.
- SM-14 / SM-28: facilitator qualification and gender constraints with explicit exceptions.
- SM-15 / SM-29: orchestration, staffing consolidation, overrides, hard caps, and ALG-08.
- SM-18: measure and enforce at least 80% coverage of the core algorithm package.

Keep each iteration reviewable in a PR. Tickets move to In Review after verification and remain there until reviewed.
