# React and container deployment requirements

Authority: [SRS v1.1](https://docs.google.com/document/d/1AuslVQYFWfaqpQPtCLFsc3tIh2b8LNPNU0TySDOxwJE/edit), section 9, revised 2026-10-22. [Full text snapshot](srs-v1.1.txt). The v1.0 source is retained as history; the 28 core functional requirements are unchanged.

The required topology is browser -> UI static server/reverse proxy -> Spring Boot API -> PostgreSQL. The standalone Java core remains a dependency of the API. React assets belong to an independent frontend build and image; they are not bundled in the API JAR.

| Work | Requirement coverage | Notion task | State |
| --- | --- | --- | --- |
| React event/config/participant/facilitator workflow | WEB-01/02, VER-01/03 | [SM-36](https://app.notion.com/p/40161e61aca181589385e9d19c7a4d1e) | React implemented in PR #17; current-head CI passes, native review pending |
| React live/proposal/print/export and lifecycle safety | WEB-03/04, VER-01/03 | [SM-37](https://app.notion.com/p/40161e61aca181f3bd00ff4b54522f1d) | 23 frontend tests pass; current-head native review pending |
| Independent UI container and same-origin proxy | DEP-01/03/04/08 | [SM-38](https://app.notion.com/p/40161e61aca1814998a2ccfc44dacf0c) | Implemented locally; actual-stack CI/native review pending |
| Database-aware API readiness | DEP-01/05/07/08/09 | [SM-39](https://app.notion.com/p/40161e61aca181f69d3af12bf3c95456) | Implemented locally; actual-stack CI/native review pending |
| Three-service Compose, private networks and durable data | DEP-02/04/05/06/07/09 | [SM-40](https://app.notion.com/p/40161e61aca181359ac9d4208715b133) | Implemented locally; actual-stack CI/native review pending |
| Actual-stack CI and complete acceptance traceability | WEB-01–04, DEP-01–09, VER-01–03 | [SM-41](https://app.notion.com/p/40161e61aca1816080d1f6ca5d27c0cd) | Implemented locally; actual-stack CI/native review pending |

## Delivery order and acceptance

Keep one PR open at a time. Finish the React migration in PR #17, then deliver UI image/proxy, API readiness, Compose orchestration and full-stack acceptance in reviewable increments. Dependency links in Notion establish the prerequisites; actual runtime evidence closes a ticket, not this plan.

Only the UI's configurable localhost port is published by default. UI/API and API/database use separate networks; optional host-development ports require a documented loopback override. The UI reverse-proxies /api/ without changing API error responses, while direct React routes load the UI shell. Required credentials come from environment configuration with no usable committed password.

Compose waits for healthy PostgreSQL, database-aware API readiness and the UI server. Named storage survives normal shutdown/recreation. UI/API runtimes stay unprivileged; the API image remains below 300 MB with no Node, Maven or JDK. CI must build/run all three containers, verify routing/health/isolation and exercise persistence after recreation. React component/lifecycle tests retain the stale-revision and back/forward fixes; existing PostgreSQL Java tests, algorithm coverage and quality/security gates remain required.

Public deployment, auth/TLS, Kubernetes, clustering, autoscaling, backup policy and extra business services are not specified by this revision.

[Detailed section 9 acceptance evidence](deployment-acceptance.md) distinguishes local checks from the required actual-stack CI results.
