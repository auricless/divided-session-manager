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

- SM-31 / PR #7: live automated review is blocked on OpenAI API quota. The Codex subscription reset did not resolve the separate API quota failure. Do not merge without the automated verdict.
- SM-8 / SM-9 / SM-11: model validation changes are saved on `fix/domain-validation`, pending their own automated-review PR.
- SM-13 / SM-27 / SM-18: segmentation acceptance cases and coverage checks are saved on `test/core-coverage`, pending automated review after the model change.
- SM-20–23: complete the later Spring Boot wrapper, assignment endpoint, Dockerfile, and local Compose setup as a separate milestone after the core implementation.

Keep each iteration reviewable in a PR. Tickets move to In Review after verification and remain there until reviewed.

## Model audit: SM-8 / SM-9 / SM-11

TDD found four model validation/equality failures in 103 tests. New size/setter and session-aware participant validation tests preceded their APIs. The model update validates facilitator fields and qualifications, implements value equality, guards group roles and null attendees, exposes size/setter aliases, requires participant gender, and validates Young Adult life stage against the event tier. All 106 tests and full Maven verification pass. Existing Role enum naming is retained for compatibility.

Work is saved on fix/domain-validation. Per the user's updated review workflow, subsequent PR reviews will be performed by GitHub Actions instead of local agent review. Automated-review setup is the next priority.

## Coverage and segmentation verification: SM-13 / SM-27 / SM-18

JaCoCo 0.8.15 generates HTML/XML reports during Maven verify. Its package rule checks 80% line coverage and 70% branch coverage for the algorithm service package; domain classes appear in the report but are outside those thresholds.

The 112-test suite passes with 177/177 service lines covered (100%) and 137/138 branches covered (99.28%). Added acceptance cases exercise empty/single/all-tier segmentation, unknown/inactive tier exclusion, qualification-only exceptions, null roster/exception entries, and refusal to consolidate a staffed segment when doing so cannot help another tier.

A deliberately stricter 100% branch check exposed an initial package-filter mismatch. The rule now uses the Java package name, `org.c3cavite.core.service`. The 100% probe fails against the actual 99.28% coverage, while a full verify with the ticket thresholds passes. The remaining uncovered branch is a null-gender guard for a manually constructed separated group; the validated entry point never creates one. No production algorithm changes or runtime dependencies were needed.

These local branches are implemented and verified, not merged or approved. PRs remain sequential, and the GitHub Action is the reviewer.

## Phase 1B bootstrap: SM-20

Acceptance criteria: a runnable Spring Boot service on port 8080, a Maven dependency on the standalone core, and no Spring dependencies in the core. Import Boot's BOM only in the service module to retain the repository's Maven parent. Boot 4.1.1 supports Java 17; its `spring-boot-starter-webmvc` replaces the older starter name in the ticket.

TDD: the startup test first failed because no Spring Boot application configuration existed. Adding the application entry point and a core assignment-service bean makes the full reactor pass 113 tests and package an executable JAR. The test starts an actual HTTP server on a random port and calls the core through the wired bean. No assignment endpoint is included in this iteration.

CLI smoke checks started both `mvn spring-boot:run` from the service module and the packaged JAR on port 8080, verified the expected HTTP 404 before endpoints exist, and stopped both processes. The core dependency tree contains only test-scoped JUnit dependencies and no Spring dependencies.

This branch still requires automated PR review; the API quota blocker remains independent of local test success.

## Phase 1B assignment API: SM-21

The [API contract](assignment-api.md) records the acceptance criteria and runnable sample. Separate request/response DTOs and a mapper keep HTTP concerns outside the core; the controller delegates assignment decisions. Exceptions select an exact roster instance through a zero-based request index.

TDD: all 25 initial HTTP cases failed with 404 before the endpoint existed. After implementation they passed. Six additional cases then exposed silent Jackson coercion of fractional caps/ages, numeric enum ordinals, numeric strings, and string boolean flags. The service now rejects those types using Jackson 3 settings. Full Maven verify passes 144 tests (112 core, 32 service), including final/proposal states, exceptions and their withdrawal, role preservation, custom boundaries, life stages, caps, exclusions, empty input, and clean invalid-input responses.

A CLI smoke run using `mvn spring-boot:run` on port 8080 and the documented curl sample returned FINAL with one staffed group and two participants. A missing-cap request returned HTTP 400 with a capSize message and no stack trace. This implementation is pending agent review; the user has temporarily returned to our own agent review because no API credits are available.
