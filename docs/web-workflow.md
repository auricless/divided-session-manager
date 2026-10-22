# React registration and group display

Authority: SRS v1.1 section 9. React lives in `frontend/` and uses the existing JSON APIs. The Java core and API retain their grouping/validation rules; the API no longer renders Thymeleaf or bundles UI assets. [Deployment requirements](web-deployment-requirements.md) track the separate UI image and three-container orchestration work.

## Local development

Start PostgreSQL and the API as described in [event API setup](event-api.md). Use Node 24.21.0 (at least 24.15 on the 24.x line) for the frontend:

```bash
npm ci --prefix frontend
npm run dev --prefix frontend
```

Open the URL printed by Vite, normally localhost:5173/events. The development server proxies relative `/api/` requests to localhost:8080. For another local API port, set `API_PROXY_TARGET` before starting Vite; it is a server-side development setting and is not bundled into the browser. `npm run build --prefix frontend` emits independent production assets in `frontend/dist`; it does not copy them into the API JAR. Vite's development/preview server is for local work; the production UI server belongs to SM-38.

## Workflow

1. Create an event with an explicit maximum cap. Target size, gender mode, active tiers, configurable contiguous age ranges and Young Adult life-stage splitting are available. A blank final upper age means open-ended.
2. Register, edit or remove participants. Session-derived Young Adults require a life stage. Invalid values stay in the form for correction; names are rendered as React text.
3. Enter available facilitators and qualifications. Separate exception checkboxes apply to that run only, reset after a successful computation, and appear in result warnings. The facilitator form is not retained after leaving the page.
4. Generate groups. Leaderless results remain proposals with actions. Groups, counts, leaders/assistants, warnings and unassigned reasons remain visible. Roster/config edits invalidate the stored snapshot; name-only event edits preserve it.
5. Open `/events/{id}/groups` for live display. Both screens poll every two seconds. Mismatched event/snapshot revisions, connection errors and obsolete requests cannot expose a verified roster. Back/forward restoration immediately refreshes and resumes one polling cycle; React unmount cancels work/listeners.
6. Print or export CSV. Each action requires a current result from its own refresh; unavailable output disables both controls. The print stylesheet hides controls and keeps group cards together. The CSV API quotes/escapes cells, guards spreadsheet formulas, includes proposal/blocker/warning/unassigned details and rejects invalidated snapshots with 409.

No database credentials enter React. The UI server/proxy must retain security headers and same-origin API routing in SM-38; API responses retain no-store and security headers now. Authentication/public deployment are not part of this local workflow.

## Verification

```bash
npm run check --prefix frontend
npm test --prefix frontend
npm run build --prefix frontend
npm audit --prefix frontend --audit-level=low
mvn -Pquality verify
```

React component and feed tests cover event defaults/explicit cap, settings, participant edit/removal and validation retention, proposal/exception withdrawal, revision mismatch, failures/retry, repeated cache restoration and obsolete responses. The tests were introduced before the React entry point; API-only routing first failed with 200 where independent UI routes require backend 404. Full Java PostgreSQL verification continues to enforce core/API/security behavior and quality gates.

Earlier Thymeleaf browser acceptance covered create/register, proposals/final, explicit exceptions, cross-client invalidation/recompute, participant edit/removal and parsed CSV download. That is historical evidence, not proof of the React migration. Current React browser and exact-head CI/native review evidence must be recorded in [PR #17](https://github.com/auricless/divided-session-manager/pull/17) before closing SM-36/37. Paper printing and public deployment have not been performed.

## React migration acceptance (2026-10-22)

- Clean npm install, ESLint/React-hooks/Prettier checks, 13 React/feed tests, production build and npm dependency audit pass; no reported dependency vulnerabilities. Full PostgreSQL Java quality verification passes all 183 tests.
- Against a confirmed restarted API-only JAR (backend /events returns 404) and separate Vite server, actual browser acceptance created an event with an explicit cap, retained invalid Young Adult input, saved the corrected attendee, showed a leaderless proposal, reached FINAL with an eligible leader, displayed explicit exceptions/reset and returned to a proposal after withdrawal.
- The React live view cleared an invalidated roster/disabled controls after a separate client registered, then automatically displayed a recomputed FINAL with two attendees. The participant edit was saved through React. CSV fetched through the same UI-origin API proxy parsed as four rows, twelve columns and two matching current attendees.
- Browser automation timed out at participant-removal confirmation; that browser action is not claimed as passed. The React component test verifies confirm/cancel, owned DELETE and refreshed roster. Physical printing and a React-browser CSV download were not performed; component tests verify verification-before-print/export, and the HTTP CSV response is parsed independently.
- Earlier browser Back checks reached an older running Thymeleaf process and are not evidence for the updated implementation. Persisted navigation, stale responses and single polling cycles are covered by the automated feed/React tests. Actual three-container acceptance remains SM-38–41.
