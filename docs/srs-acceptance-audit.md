# SRS acceptance audit

Authority: [Drive SRS v1.0](https://docs.google.com/document/d/1AuslVQYFWfaqpQPtCLFsc3tIh2b8LNPNU0TySDOxwJE/edit), fetched 2026-10-22; last modified 2026-10-01T12:03:43.551Z. The [complete fetched text](srs-v1.0.txt) includes architecture, roadmap and first-release exclusions, beyond the [core requirements snapshot](srs-core-requirements.md).

The user's current scope includes the Phase 2A/2B roadmap. The document's first-release exclusions do not remove those explicitly named later deliverables. Authentication, notifications, scheduling, analytics and mobile remain unspecified future work; do not invent their acceptance criteria.

## Functional requirement evidence

Core test classes below are in `session-manager-core/src/test/java/org/c3cavite/core`; HTTP tests are `SessionAssignmentApiTest` in the service module. Run `mvn verify` to execute them and enforce JaCoCo. Rows describe observed acceptance evidence, not a formal proof over every possible input.

| Requirement | Implementation | Acceptance test evidence |
| --- | --- | --- |
| CFG-01 | SessionConfig default gender mode | SessionConfigTest defaults; SegmentationStrategyTest separated/mixed |
| CFG-02 | SessionConfig default/positive target | SessionConfigTest defaults/invalid sizes |
| CFG-03 | SessionConfig explicit cap | SessionConfigTest missing cap; HTTP invalid request cases |
| CFG-04 | Configured boundaries and active tiers | SessionConfigTest, GroupAssignmentServiceTest.filtersAndAssignsUsingTheCurrentSessionsAgeBoundaries |
| CFG-05 | Life-stage toggle | SegmentationStrategyTest.canDisableLifeStageSplit |
| CFG-06 | Config validation, hard cap | SessionConfigTest; SrsAcceptanceTest.validBoundaryConfigurationsPreserveEveryAttendeeAndHardConstraint |
| PRT-01 | Participant validation | ParticipantTest; HTTP malformed request cases |
| PRT-02 | Participant.forSession tier derivation | ParticipantTest; GroupAssignmentServiceTest.filtersAndAssignsUsingTheCurrentSessionsAgeBoundaries |
| PRT-03 | Session-derived Young Adult life stage | ParticipantTest; HTTP preservesYoungAdultLifeStagesAndHardCaps |
| PRT-04 | Empty final result | GroupAssignmentServiceTest.emptyInputReturnsAnEmptyFinalResult; HTTP acceptsEmptyRostersAsAnEmptyFinalResult |
| PRT-05 | Under-13 exclusion and warning | GroupAssignmentServiceTest.underThirteenExclusionIncludesAWarning |
| FAC-01 | Facilitator validation/value snapshot | FacilitatorTest.requiredFieldsAndQualificationsAreValidated |
| FAC-02 | Exactly one leader or proposal | AssignmentResultTest; HTTP returnsAnActionableProposalInsteadOfFinalizingALeaderlessGroup |
| FAC-03 | Optional assistant, distinct roster use | FacilitatorAssignerTest; GroupTest role checks |
| FAC-04 | Gender eligibility and explicit exception | FacilitatorAssignerTest; GroupAssignmentServiceTest.genderMismatchCannotSilentlyFinalizeASeparatedGroup |
| FAC-05 | Staffing consolidation, explicit permission | GroupAssignmentServiceTest.shortageConsolidatesOnlyWithinTheHardCap and emptyQualificationsRequireAnExplicitExceptionForThatRun |
| ALG-01 | Separated gender segments | SegmentationStrategyTest.testSegmentSeparatedGender; SrsAcceptanceTest invariants |
| ALG-02 | Separate age tiers | SegmentationStrategyTest.allThreeAgeTiersStaySeparate; SrsAcceptanceTest invariants |
| ALG-03 | Young Adult life-stage segments | SegmentationStrategyTest.testSegmentLifeStage; SrsAcceptanceTest invariants |
| ALG-04 | Target-based initial group count | GroupAssignmentServiceTest.staffedSegmentsStayNearTargetWhenConsolidationCannotHelpAnotherTier |
| ALG-05 | Hard cap under staffing shortage | GroupAssignmentServiceTest.shortageConsolidatesOnlyWithinTheHardCap; SrsAcceptanceTest invariants |
| ALG-06 | Balanced distribution | GroupAssignmentServiceTest.sizingPreservesAttendeesCapsAndBalancedGroupsAcrossSmallConfigurations (625 configurations) |
| ALG-07 | Youth ordering | FacilitatorAssignerTest Youth-priority cases; service orders Youth before Teens/Young Adults |
| ALG-08 | Minimum size or explicit blocker | GroupAssignmentServiceTest.avoidsSingletonGroupsEvenWhenTargetIsOne, impossibleMinimumSizeReturnsProposalWithoutBreakingTheCap, lifeStageSeparationIsNotRelaxedToHideASingleton |
| OUT-01 | Explicit immutable state/blockers | AssignmentResultTest; HTTP final/proposal acceptance |
| OUT-02 | Counts and warnings | GroupAssignmentServiceTest; HTTP returnsUnassignedAttendeesAndWarningsWithoutLosingTheirDetails |
| OUT-03 | JSON DTO mapping | SessionAssignmentApiTest success/invalid cases; actual container HTTP smoke |
| OUT-04 | Unassigned reasons, leader blockers | GroupAssignmentServiceTest.reportsUnknownTierOutsideConfiguredBoundaries; HTTP unassigned/proposal acceptance |

## Algorithm, quality and architecture evidence

- Sections 4.1–4.3: existing sizing/segmentation/staffing/edge tests plus `SrsAcceptanceTest` exercise 80 size/cap/mode combinations (including zero, singleton, 199/200 and Integer.MAX_VALUE cap), preserving identities, counts, demographic rules, hard cap and unique leader use.
- NFR 5.1 performance: `SrsAcceptanceTest.assignsTwoHundredAttendeesWithinFiveHundredMilliseconds` warms up and measures 12 combinations of separated/mixed mode, cap 1/6/Integer.MAX_VALUE and sufficient/scarce staffing, asserting each 200-attendee call finishes under 500 ms. Machine scheduling affects this acceptance measurement; it is not a throughput benchmark.
- NFR 5.1 statelessness: `sharedServiceAndInputsAreIndependentAcrossConcurrentInvocations` shares one service and immutable rosters across 20 concurrent calls with different configurations, checks each output against its own sequential result, and verifies the original run remains unchanged. Existing exception-withdrawal tests cover permissions across runs.
- NFR 5.2 reliability: boundary/invariant tests above, 625 sizing configurations and invalid-input tests cover known edge classes. Tests cannot prove all conceivable valid inputs; newly found defects require regression tests.
- NFR 5.3: rules reside in configurable core values; Maven verify enforces 80% line/70% branch service-package coverage. `-Pquality` checks Google Java Format, Checkstyle and SpotBugs.
- NFR 5.4 / section 6.1: standalone Maven Java 17 core has only JUnit test dependencies; service concerns remain outside core. `mvn dependency:tree -pl session-manager-core` checks that boundary.
- Section 6.2 / Phase 1B: service endpoint, executable JAR, Dockerfile and Compose are implemented. PR #13 passed actual image build/runtime/non-root/JRE/size/HTTP checks and exact-head native review before merge. See [container evidence](container-runtime.md).

## Roadmap gaps and implementation order

| SRS deliverable | Current evidence | Notion task |
| --- | --- | --- |
| Performance/reliability evidence and full traceability | Acceptance tests and this audit added; PR checks/review required | [Audit](https://app.notion.com/p/40161e61aca181b4aa8ff6ab5001c772) |
| PostgreSQL schema, Spring Data JPA, event CRUD | Implemented; EventApiTest and restart acceptance pass on real PostgreSQL; CI/review gate pending | [Persistence and events](https://app.notion.com/p/40161e61aca181ba8c4afb8636a15c26) |
| Participant CRUD and event workflow integration | Not implemented | [Participants and assignment](https://app.notion.com/p/40161e61aca181e88ec4e6098cd6ad5c) |
| React or Thymeleaf registration UI | Not implemented; choose Thymeleaf to reuse Java service and validation | [Registration UI](https://app.notion.com/p/40161e61aca181589385e9d19c7a4d1e) |
| Real-time group display, print/export | Not implemented | [Display and export](https://app.notion.com/p/40161e61aca181f3bd00ff4b54522f1d) |

Persistence must stay outside core, use real PostgreSQL verification and repeatable migrations, and retain the existing stateless assignment API. Store only the current event assignment needed for cross-client display; roster/config edits must invalidate it. This does not add unspecified assignment-history or scheduling features. UI and exports must preserve proposal states and explicit permissions and escape untrusted names.
