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
PR #5 review pass 1 identified stale role assignments when callers reuse a Group after withdrawing exceptions. One regression failed in 88 tests before clearing unfilled role slots on recomputation.
All 88 tests passed after the fix; review pass 2 passed and PR #5 was merged. SM-14/28 are Done.

## Iteration 4: staffing-aware orchestration and minimum sizes (SM-15 / SM-29)

Acceptance criteria:

- Match leaders across the shared roster without starving a restricted tier; apply normal rules before explicit exceptions.
- Consolidate within each demographic segment only when it reduces missing leaders and respects the configured hard cap.
- Distribute attendees evenly; avoid singleton groups where possible. SRS ALG-08 applies to tier/gender segments, including their life-stage groups. Return a minimum-size blocker when cap or life-stage constraints prevent compliance.
- Preserve Youth priority, deterministic ordering, participant isolation, final/proposal states, exception visibility, and stateless reruns.
- Reject null inputs and include an under-13 exclusion warning.

TDD: seven regressions failed in 95 tests, then validation expanded the red suite to eight failures in 96 tests. A matching refinement failed once in 97 tests before normal-only matching was added ahead of exception matching. All 98 tests pass, including 625 sizing configurations. Full Maven verification passes.

An informal CLI performance/invariant check ran 200 attendees, ten target/staffing combinations, 10 warmups and 20 measured runs per combination. Maximum measured time was 237.800 ms; counts and hard caps were checked. This is local evidence, not a standardized benchmark.

## Remaining review and implementation gates

- SM-31 / PR #7: subscription-backed native review and API-free Java 17 Maven CI passed review and merged. Automatic review on PR open is verified; do not post a duplicate manual review request for the same head.
- SM-8 / SM-9 / SM-11: model validation and SRS-compliant empty-qualification exceptions passed review and merged in PR #8.
- SM-13 / SM-27 / SM-18: segmentation acceptance cases and coverage checks are on `test/core-coverage`, passed Java 17 CI and native review and merged in PR #9.
- SM-20–23: complete the later Spring Boot wrapper, assignment endpoint, Dockerfile, and local Compose setup as a separate milestone after the core implementation.

Keep each iteration reviewable in a PR. Tickets move to In Review after verification and remain there until reviewed.

## Model audit: SM-8 / SM-9 / SM-11

TDD found four model validation/equality failures in 103 tests. New size/setter and session-aware participant validation tests preceded their APIs. The model update validates facilitator fields and qualifications, implements value equality, guards group roles and null attendees, exposes size/setter aliases, requires participant gender, and validates Young Adult life stage against the event tier. All 106 tests and full Maven verification pass. Existing Role enum naming is retained for compatibility.

Native review found that rejecting empty facilitator qualification lists contradicted SRS FAC-05: an organizer must be able to grant an explicit qualification exception even when the facilitator has no qualified tier. Two regressions failed before relaxing that constructor restriction. Null lists and null entries remain invalid; empty lists remain strictly ineligible without the run-scoped exception. All 108 tests and Maven verify pass after the fix.

PR #8 passed Java 17 CI and fresh native review with no findings and merged as cb17fb6. PR #7 established subscription-backed GitHub review and API-free Maven verification in Actions. Subsequent changes use that native review and TDD fix cycle rather than duplicate local review.

## Coverage and segmentation verification: SM-13 / SM-27 / SM-18

JaCoCo 0.8.15 generates HTML/XML reports during Maven verify. Its package rule checks 80% line coverage and 70% branch coverage for the algorithm service package; domain classes appear in the report but are outside those thresholds.

The 114-test suite passes with 177/177 service lines covered (100%) and 137/138 branches covered (99.28%). Added acceptance cases exercise empty/single/all-tier segmentation, unknown/inactive tier exclusion, qualification-only exceptions, null roster/exception entries, and refusal to consolidate a staffed segment when doing so cannot help another tier.

A deliberately stricter 100% branch check exposed an initial package-filter mismatch. The rule now uses the Java package name, `org.c3cavite.core.service`. The 100% probe fails against the actual 99.28% coverage, while a full verify with the ticket thresholds passes. The remaining uncovered branch is a null-gender guard for a manually constructed separated group; the validated entry point never creates one. No production algorithm changes or runtime dependencies were needed.

The coverage iteration passed Java 17 CI and native review of ef01180 and merged in PR #9 as 4dee986. PRs remain sequential. PR opening triggers review automatically; request another review only if an updated head has no automatic review queued or completed.

The PR Maven workflow also publishes the JaCoCo HTML/XML directory as a commit-specific Actions artifact. Coverage failure remains a failing Maven check; an available report is uploaded even after a threshold failure, and missing reports cannot silently pass after a successful build. Live publication is verified separately from local coverage measurements.

## Java quality workflow

User-requested Checkstyle, Google Java Format, SpotBugs, and SonarQube Cloud integration are tracked in [the quality plan](java-quality.md). This is engineering tooling, without changes to SRS business behavior.
