# Repository migration

The project is moving to two maintained repositories:

| Repository | Responsibility | Distribution |
| --- | --- | --- |
| `session-manager-core` | Framework-free Java grouping library | Versioned Maven JAR/POM in AWS CodeArtifact |
| `divided-session-service` | React UI and Spring Boot API, depending on the core through Maven | Complete application image in AWS ECR |

The service image runs one Java process that serves the compiled UI and API. PostgreSQL remains a separate database. AWS credentials are needed to publish or download artifacts, not by the application after its image has been pulled.

This replaces the separate UI/API release images described in section 9. The [SRS distribution revision, section 10](https://docs.google.com/document/d/1AuslVQYFWfaqpQPtCLFsc3tIh2b8LNPNU0TySDOxwJE/edit) defines the new requirements. Existing grouping, live-display safety, persistence and quality requirements remain unchanged.

## Current status

Extraction and release configuration are being prepared. The current monorepo still works and remains available during migration. AWS account setup and the first registry releases are pending; preparing a workflow does not establish a successful publication.

The new service declares a fixed core version in its POM. Before CodeArtifact is ready, CI can install that version from a pinned commit of the core repository. The release workflow must instead resolve the published CodeArtifact artifact.

## Completion

Before archiving this repository:

1. Publish both replacement repositories and pass their current-head CI and native review.
2. Publish the core to CodeArtifact and resolve/run it from a fresh Maven consumer.
3. Publish the complete app image to ECR, pull it, and verify the UI/API against PostgreSQL, including persistence after recreation.
4. Point this README to the verified replacements, preserve this history, then archive the monorepo.

Work is tracked in Notion: [core extraction](https://app.notion.com/p/40261e61aca18185a25ac4896783195e), [service extraction](https://app.notion.com/p/40261e61aca181b48cb1d6b7e420de35), [AWS preparation](https://app.notion.com/p/40261e61aca181a88cced6bccc47d879), and [release verification and archival](https://app.notion.com/p/40261e61aca181d2b9aec35a32ec89f1).
