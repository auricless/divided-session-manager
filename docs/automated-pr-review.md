# Automated PR reviews

Native Codex GitHub review supplies independent feedback; GitHub Actions supplies deterministic checks. No API-key review action is used.

## Setup

Connect this repository and enable **Automatic review** in [Codex review settings](https://chatgpt.com/settings/code-review). This account setting cannot be enabled by repository YAML. PR opening already triggers review; do not immediately add `@codex review`. For an updated head, check for queued, running, or completed native activity before requesting one review.

## Context budget

- Start with the diff, changed tests, directly affected callers/contracts, and applicable `AGENTS.md` rules. Follow dependencies only to investigate a plausible defect; scope is a starting point, not a ban on necessary investigation.
- Keep the PR description about the final change: Notion links, What, Why, relevant requirement IDs, and concise validation links/results. Aim for a review-focus paragraph of roughly 100–200 words when useful; do not paste the SRS, logs, ticket history, or successive status updates.
- The SRS remains authoritative. Read only matching clauses in the local snapshot. Retrieve the source when those clauses are missing, stale, or inconsistent, rather than fetching the whole document by default.
- Put component-specific invariants in nested `AGENTS.md` files. Prioritize readability/extensibility, security, and performance/correctness; leave formatting, lint, static analysis, coverage, and full test suites to CI. Reproduce suspected defects with focused checks.
- On follow-up review, identify the last reviewed SHA and changed paths. Prioritize that delta while checking interactions with the full PR. Native review still decides what repository context it needs; these instructions are not a hard token cap.

[OpenAI's guidance](https://learn.chatgpt.com/docs/third-party/github) recommends concise, scoped rules and mechanical checks in CI. Review instructions cannot guarantee exhaustive coverage, duration, or usage savings. The documented CLI `review_model` setting is not evidence of a model control for native GitHub review; do not silently change models or claim a smaller native model is configured.

## Review cycle and merge gate

Keep one PR open at a time. Batch implementation, regression tests, and documentation before pushing a review-ready head; avoid requesting another full review for every small intermediate edit. Review findings require a failing regression test, a fix, relevant passing checks, and a fresh review of the final head. Do not add a parallel agent review unless the user authorizes replacing the native review path.

Merge only after current-head CI and current-head review complete with no unresolved actionable findings, under the user's merge authorization. An older clean review, pending reaction, or quota failure is insufficient. This policy does not create a required GitHub check or change branch protection.

## Slow or stalled reviews

After roughly 30 minutes without a verdict, make one bounded diagnostic check: compare the requested SHA, latest native status/receipt, and any explicit failure or quota message. Report the unresolved gate and stop active polling until new evidence or user direction arrives. This is an observation threshold, not a service timeout or proof of worker failure.

GitHub's running marker does not reveal worker liveness, model, token consumption, queue time, or reasoning time. Do not duplicate requests, create empty commits, restart a review, or waive the gate merely because it is slow. No feature work should continue when the user has requested review-policy work first.
