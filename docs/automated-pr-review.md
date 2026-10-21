# Automated PR reviews

`.github/workflows/pr-review.yml` replaces local manual agent reviews with GitHub Actions review on opened, reopened, updated, and ready PRs.

1. **Maven and publisher tests** runs `mvn verify` on Java 17 and the Node test suite for publishing reviews.
2. **Codex analysis** uses the official [OpenAI Codex Action](https://developers.openai.com/codex/github-action/) in a read-only permission profile with sudo privileges dropped. It reads the full changelist, surrounding source, and the versioned SRS requirements snapshot. Its priorities are human readability, extensibility, security, performance, and correctness.
3. **Codex review** validates the structured result and current PR head, posts actionable findings as inline review threads where the diff supports them, and fails if any findings remain. Non-commentable findings remain visible in the summary and still fail the check. Failed tests, failed analysis, invalid output, and publication errors cannot produce a passing verdict.

The workflow posts `COMMENT` reviews and exposes its verdict as the **Codex review** check. It does not need GitHub's permission to issue formal approval reviews. Existing branch protection remains unchanged; require this check in a branch ruleset if merges must be enforced by GitHub itself.

## Credentials and trust

Configure `OPENAI_API_KEY` as a **repository Actions secret**. A key stored only in the `prod` environment is not used: that environment permits protected branches, while PR jobs run against PR refs. The workflow does not change production environment policies or deploy the application.

Only repository branches receive automatic Codex review. Fork PRs fail the review gate without exposing the API key; a maintainer must bring an approved change onto a repository branch. Draft PRs wait until ready. The Codex action also checks the triggering user's repository write access.

Review execution has a read-only GitHub token. Only the separate publisher receives `pull-requests: write`; it receives the review result, not the OpenAI key. Checkouts do not persist credentials. Workflow actions and the Codex CLI version are pinned. The optional repository variable `CODEX_REVIEW_MODEL` selects a model; otherwise the Codex CLI default applies.

The [marketplace bot originally considered](https://github.com/marketplace/actions/codex-pr-review-bot) only posts a Markdown comment and offers neither a custom prompt nor a machine-readable verdict. The official action supports the repository's review prompt and output schema, allowing inline threads and a real failing check.

## Fix and re-review cycle

Implement and test locally through TDD, push through Git CLI, and wait for the current commit's review. If findings appear, reproduce them with failing tests, fix them, rerun verification, and push again. Every update triggers fresh automated analysis. Resolve threads once the fix is confirmed. Merge only after tests and **Codex review** pass for the current head, under the user's current merge authorization.

The workflow reviews and reports; it does not apply fixes or merge PRs. During this session the implementation agent handles the TDD cycle and authorized merges, without posting a duplicate local agent review.

`docs/srs-core-requirements.md` is a review snapshot of the authoritative Drive SRS. Update it when that SRS changes; ticket checklists cannot override the SRS.

## Local validation

```bash
node .github/scripts/publish-review.test.cjs
actionlint .github/workflows/pr-review.yml
mvn verify
```
