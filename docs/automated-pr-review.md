# Automated PR reviews

PRs use Codex's subscription-backed GitHub integration for review, and GitHub Actions for tests. No OpenAI API key is used by `.github/workflows/pr-review.yml`.

## Enable the GitHub integration

An account with repository push/admin permission must configure the connected repository in [Codex review settings](https://chatgpt.com/settings/code-review):

1. Connect `auricless/divided-session-manager` to Codex if it is not already connected.
2. Choose the repository and enable **Automatic review** under **Review code**.
3. For an existing PR, or after pushing a fix, post `@codex review` to request a review of its current head. Verify the bot responds and completes the review; a request comment alone is not a completed review.

The setting lives in the Codex account, not in this repository's YAML. Repository files cannot enable it on their own. See [OpenAI's GitHub review setup](https://learn.chatgpt.com/docs/third-party/github).

Automatic GitHub review is included with supported ChatGPT subscriptions, including Plus, subject to review usage limits. It is separate from API-key billing. The previous API-based action failed because no API credits were available; its jobs, credential references, and custom publisher have been removed. Existing secrets are not changed or consumed. See [official pricing](https://learn.chatgpt.com/docs/pricing).

## Review guidance and limits

The root `AGENTS.md` contains **Code Review Rules** prioritizing human readability, extensibility, security, performance, and correctness. It points reviewers to `docs/srs-core-requirements.md`, a versioned snapshot of the authoritative Drive SRS. Refresh that snapshot when the SRS changes; ticket checklists cannot override the SRS.

The native integration posts prioritized review feedback directly on GitHub. Repository rules set the review priorities; they do not guarantee exhaustive coverage. Our live setup review produced P2/P3 findings as well. The separate Security Review product is not required for this setup.

## Tests and merge gate

The **Maven verification** Actions job runs the full reactor's `mvn verify` on Java 17 for opened, reopened, updated, and ready PRs. It tests GitHub's proposed merge commit from the workflow event, so the build includes both the PR and base branch; a regression guard checks that checkout matches the event's merge SHA. Draft PRs wait until ready. Its token has only `contents: read`, checkouts do not persist credentials, and actions are pinned to commits. It does not post reviews, call OpenAI, apply fixes, or merge.

A green Maven check is not a review verdict. The implementation agent must check both test results and the native review for the current head before using the user's merge authorization. Do not treat an older review, a pending reaction, or a quota failure as approval. If the integration cannot produce a current review, keep the PR open and report the blocker. Existing branch protection is unchanged; no new required GitHub review check is claimed by this workflow.

## Fix and re-review cycle

Keep one PR open at a time. Implement and test locally through TDD, commit and push through Git CLI, and wait for the current commit's native review. Reproduce actionable findings with a failing test, fix them, rerun verification, and push again. Request a fresh `@codex review` after fixes and confirm completion before resolving the findings and merging. Do not post a duplicate manual agent review in chat.

## Local validation

```bash
actionlint .github/workflows/pr-review.yml
mvn verify
```

Account activation and a completed live review must be verified separately from these local checks.
