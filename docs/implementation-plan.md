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
- Return participant copies with the session's derived tier while preserving reusable input values and previous results.
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

Self-review pass 1 found an inconsistent output contract: group metadata used the event's tier,
but returned participant records exposed cached default tiers. Three failing regression tests
confirmed assigned and unassigned output mismatches and reuse across sessions. The fix returns
immutable session-specific participant copies. Full Maven verification then passed all 66 tests.

Iteration 1 passed agent review and was merged in PR #3. The user authorized merging after agent review for this session. No coverage claim is made yet.

## Iteration 2: explicit assignment result states (SM-12)

Acceptance criteria:

- Return FINAL only when every formed group has a LEADER; empty input is an empty final result.
- Return NON_FINAL_PROPOSAL with actionable, numbered group blockers for missing leaders, invalid leader roles, or a violated supplied cap.
- Preserve groups, counts, warnings, and unassigned attendees with reasons.
- Capture immutable result snapshots, including group contents and facilitator qualifications.
- Retain the existing constructor and JavaBean getters; actual JSON serialization remains Phase 1B as stated in SM-12.

TDD: the four initial immutability regression tests fail in the 70-test suite. State/blocker and service tests were added before their APIs.
After implementation, all 77 tests and full Maven verification pass. The result copies group contents, protects mutation methods, and defensively copies facilitator qualification lists. No dependencies were added.
PR #4 passed agent review and was merged; SM-12 is Done.

## Iteration 3: explicit facilitator exceptions (SM-14 / SM-28)

The SRS is the ultimate design authority. The older SM-28 body allows automatic gender fallback; the current SRS FAC-04/05 and ticket Notes require an explicit exception, which this iteration follows.

Acceptance criteria:

- Require age-tier qualification and matching gender in SEPARATED mode for leaders and assistants by default.
- Allow independent, explicit qualification/gender exceptions for a specific roster instance for one run, without a reason field.
- Never waive role, participant demographics, or the hard cap; never reuse a roster instance for multiple groups.
- Prefer staff needing no exception and report applied exceptions in warnings.
- Reuse the same eligibility rules for consolidation counts and assignment.

TDD: three regressions failed in 80 tests before implementation. New exception tests preceded their APIs. Initial implementation passes all 87 tests and full Maven verification.

## Next iterations

- SM-12: explicit final/proposal results and actionable blockers.
- SM-14 / SM-28: facilitator qualification and gender constraints with explicit exceptions.
- SM-15 / SM-29: orchestration, staffing consolidation, overrides, hard caps, and ALG-08.
- SM-18: measure and enforce at least 80% coverage of the core algorithm package.

Keep each iteration reviewable in a PR. Tickets move to In Review after verification and remain there until reviewed.
