# Automated PR reviews

Native Codex GitHub review supplies independent feedback; GitHub Actions supplies deterministic checks. No API-key review action is used.

## Setup

Connect this repository and enable **Automatic review** in [Codex review settings](https://chatgpt.com/settings/code-review). This account setting cannot be enabled by repository YAML. PR opening already triggers review; do not immediately add `@codex review`. For an updated head, check native activity before making its initial request. The bounded retry rule below is the only exception to avoiding duplicate requests.

## Context budget

- Start with the diff, changed tests, directly affected callers/contracts, and applicable `AGENTS.md` rules. Follow dependencies only to investigate a plausible defect; scope is a starting point, not a ban on necessary investigation.
- Keep the PR description about the final change: Notion links, What, Why, relevant requirement IDs, and concise validation links/results. Aim for a review-focus paragraph of roughly 100–200 words when useful; do not paste the SRS, logs, ticket history, or successive status updates.
- The SRS remains authoritative. Read only matching clauses in the local snapshot. Retrieve the source when those clauses are missing, stale, or inconsistent, rather than fetching the whole document by default.
- Put component-specific invariants in nested `AGENTS.md` files. Prioritize readability/extensibility, security, and performance/correctness; leave formatting, lint, static analysis, coverage, and full test suites to CI. Reproduce suspected defects with focused checks.
- On follow-up review, identify the last reviewed SHA and changed paths. Prioritize that delta while checking interactions with the full PR. Native review still decides what repository context it needs; these instructions are not a hard token cap.

[OpenAI's guidance](https://learn.chatgpt.com/docs/third-party/github) recommends concise, scoped rules and mechanical checks in CI. Review instructions cannot guarantee exhaustive coverage, duration, or usage savings. The documented CLI `review_model` setting is not evidence of a model control for native GitHub review; do not silently change models or claim a smaller native model is configured.

## Review cycle and merge gate

Independent PRs can be developed, opened, reviewed, fixed, and merged in parallel. Base them on the target branch without importing another PR’s unmerged changes. Order prerequisite work or use an explicit stack when changes depend on one another; coordinate overlapping changes. Apply the current-head CI/review gate to each PR independently. Batch implementation, regression tests, and documentation before pushing a review-ready head; avoid requesting another full review for every small intermediate edit. Review findings require a failing regression test, a fix, relevant passing checks, and a fresh review of the final head. Do not add a parallel agent review unless the user authorizes replacing the native review path.

Merge only after current-head CI and current-head review complete with no unresolved actionable findings, under the user's merge authorization. An older clean review, pending reaction, or quota failure is insufficient. This policy does not create a required GitHub check or change branch protection.

## Missing review response: one retry

1. Record the PR number, current head SHA, initial trigger time, and request comment ID when applicable. Automatic review on PR opening counts as attempt one. Arrange a follow-up no later than 15 minutes after the trigger; do not poll continuously.
2. At that follow-up, read the current head and native review activity. A completed review or actionable feedback for that head is a response; process it normally. A reaction, queued marker, or running marker alone is insufficient. If there is still no result, post exactly one `@codex review` retry for the unchanged head and record its time/comment ID. This bounded retry is allowed even if the original marker still says running.
3. Check again no later than 15 minutes after the retry. If it also produces no result, notify the user with the PR link, head, request times and observed status, keep the merge blocked, and stop retrying. Do not send more tags for that head without new user direction. An explicit terminal authentication/quota error is actionable feedback: report it rather than hiding it behind repeated requests.

Track attempts durably per PR/head, and reconcile existing comments before posting if a previous write's outcome is uncertain. A new head ends the old timer; inspect its automatic activity and track its own initial attempt. Closed, merged, or draft PRs need no retry. Independent PRs have separate timers and gates.

Native status does not expose worker liveness, model, tokens, queue time, or reasoning time. A retry is not proof that the prior worker failed and does not relax the completed current-head review requirement. Use the thread's scheduled follow-up tool when leaving the turn; do not claim future monitoring without arranging it.
