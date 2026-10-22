# Registration and group display

SRS section 7 Phase 2B uses Thymeleaf and the existing JSON APIs. Start the PostgreSQL/service stack as described in [event API setup](event-api.md), then open `/events` on localhost:8080.

1. Create an event with an explicit maximum group cap. Target size, gender mode, active tiers, configurable contiguous boundaries and Young Adult life-stage splitting are available in the form. Leave the final upper age blank for an open-ended range.
2. Register or edit participants in the event page. Life stage is required for session-derived Young Adults. Server validation preserves invalid form values so corrections do not require re-entry.
3. Add available facilitators with roles, gender and qualified tiers. Explicit age-tier and gender exceptions are separate checkboxes for that run; they reset after successful computation and appear in result warnings. The facilitator form is intentionally not retained after leaving the page.
4. Generate groups. A leaderless result stays a proposal with actionable blockers. Warnings and excluded/unassigned people remain visible. Config/roster edits invalidate the current assignment; a name-only edit preserves it.
5. Open the event's live display at `/events/{id}/groups`. Both registration and display views poll every two seconds and discard superseded responses. A separate client's edits clear stale groups and disable print/export. Connection failures clear unverified results while retrying.
6. Print the displayed roster using the browser print dialog, or download CSV. Controls refresh current data before use. The print stylesheet hides controls and keeps group cards together. CSV includes state, summary, participants, leaders/assistants, blockers, warnings and unassigned reasons. Every cell is quoted/escaped, formula-leading user values are prefixed with an apostrophe, and an invalidated snapshot returns 409 instead of exporting stale data.

Browser code uses safe DOM text rendering and external modules. Templates escape names, a content-security policy restricts scripts/styles to this origin, and event/attendee responses use no-store. Mutations use the validated JSON API; no cross-origin permissions are enabled. Authentication is not defined by the SRS roadmap and is not added; this is a local development workflow rather than a public deployment.

## Evidence

- `RegistrationPageTest` first failed for absent pages, then found an enum-map template lookup bug; after the fix it verifies escaped event names, configuration/permission controls, security headers and assets.
- `GroupDisplayExportTest` first failed for the absent view; it now exercises print/export controls, proposal/blocked state, CSV quoting/formula protection and 409 after invalidation.
- Full PostgreSQL `-Pquality clean verify` passes 183 tests. Browser module syntax and actionlint also pass.
- Actual browser acceptance created a named event, registered Alex, received a leaderless proposal, added qualified Sam and obtained FINAL, then applied visible exceptions and verified they were withdrawn on the next run.
- The dedicated live page observed a registration from another HTTP client without reloading: it cleared the result and disabled print/export, then refreshed to FINAL with two attendees after recomputation. The browser downloaded CSV; Python's CSV reader verified all rows have 12 columns and the two participant records match the displayed assignment.
- Paper printing and public deployment were not performed. Current-head CI, dependency-risk readback and native review are enforced before merging; [PR #17](https://github.com/auricless/divided-session-manager/pull/17) records their delivery status.
