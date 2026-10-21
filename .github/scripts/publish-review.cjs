'use strict';

function parseReview(text) {
  const review = JSON.parse(text);
  if (!review || typeof review.summary !== 'string' || !review.summary.trim()
      || review.summary.length > 6000 || !Array.isArray(review.findings) || review.findings.length > 20) {
    throw new Error('Codex returned an invalid review; no passing verdict can be published.');
  }
  for (const finding of review.findings) {
    if (!finding || !['P0', 'P1', 'P2', 'P3'].includes(finding.priority)
        || typeof finding.title !== 'string' || !finding.title.trim() || finding.title.length > 200
        || typeof finding.body !== 'string' || !finding.body.trim() || finding.body.length > 6000
        || typeof finding.path !== 'string' || !finding.path.trim() || finding.path.length > 1000
        || !Number.isSafeInteger(finding.line) || finding.line < 1) {
      throw new Error('Codex returned an invalid finding; no passing verdict can be published.');
    }
  }
  return review;
}

function addedLines(patch) {
  const added = new Set();
  let right = null;
  for (const line of (patch || '').split('\n')) {
    const hunk = /^@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@/.exec(line);
    if (hunk) { right = Number(hunk[1]); continue; }
    if (right === null || line.startsWith('\\')) continue;
    if (line.startsWith('+')) { added.add(right); right++; }
    else if (line.startsWith(' ')) right++;
  }
  return added;
}

async function publishReview({ github, context, core, reviewText, expectedHead }) {
  if (!/^[a-f0-9]{40}$/.test(expectedHead || '')) throw new Error('Missing or invalid reviewed commit.');
  const review = parseReview(reviewText);
  const request = { ...context.repo, pull_number: context.payload.pull_request.number };
  const { data: current } = await github.rest.pulls.get(request);
  if (current.head.sha !== expectedHead) throw new Error('PR head changed; wait for a review of the new commit.');
  if (current.state !== 'open' || current.draft) throw new Error('Only open, ready pull requests can receive a review.');

  const files = await github.paginate(github.rest.pulls.listFiles, { ...request, per_page: 100 });
  const linesByPath = new Map(files.map(file => [file.filename, addedLines(file.patch)]));
  const comments = [], fallback = [];
  for (const finding of review.findings) {
    const body = `**[${finding.priority}] ${finding.title}**\n\n${finding.body}`;
    if (linesByPath.get(finding.path)?.has(finding.line)) {
      comments.push({ path: finding.path, line: finding.line, side: 'RIGHT', body });
    } else {
      // Binary, removed, truncated, or unchanged positions must never hide a blocking finding.
      fallback.push(`${body}\n\nLocation: ${finding.path}:${finding.line}`);
    }
  }
  const verdict = review.findings.length ? 'Changes requested' : 'Passed — no actionable findings';
  const body = [`## Automated Codex review: ${verdict}`, `Commit: \`${expectedHead}\``, review.summary,
    ...fallback, `<!-- codex-review:${expectedHead} -->`].join('\n\n');
  const { data: posted } = await github.rest.pulls.createReview({ ...request, commit_id: expectedHead,
    event: 'COMMENT', body, comments });
  core.info(`Published review: ${posted.html_url}`);
  if (review.findings.length) core.setFailed(`${review.findings.length} actionable review finding(s) must be fixed and re-reviewed.`);
}

module.exports = { publishReview };
