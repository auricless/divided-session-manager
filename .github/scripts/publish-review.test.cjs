const { test } = require('node:test');
const assert = require('node:assert/strict');
const { publishReview } = require('./publish-review.cjs');

const head = 'a'.repeat(40);
const finding = { priority: 'P1', title: 'Preserve the hard cap', body: 'This branch can exceed the configured cap.', path: 'Core.java', line: 11 };

function fixture({ currentHead = head, state = 'open', draft = false, patch = '@@ -10,1 +10,2 @@\n context\n+added' } = {}) {
  const reviews = [], failures = [];
  const github = {
    rest: { pulls: {
      get: async () => ({ data: { state, draft, head: { sha: currentHead } } }),
      listFiles: () => {},
      createReview: async request => { reviews.push(request); return { data: { html_url: 'https://example.com/review' } }; },
    } },
    paginate: async () => [{ filename: 'Core.java', patch }],
  };
  const core = { info: () => {}, setFailed: message => failures.push(message) };
  const context = { repo: { owner: 'owner', repo: 'repo' }, payload: { pull_request: { number: 7 } } };
  return { github, core, context, expectedHead: head, reviews, failures };
}

test('a clean review posts a commit-bound result without failing the check', async () => {
  const f = fixture();
  await publishReview({ ...f, reviewText: JSON.stringify({ summary: 'No actionable issues found.', findings: [] }) });
  assert.equal(f.reviews.length, 1);
  assert.equal(f.reviews[0].commit_id, head);
  assert.equal(f.reviews[0].event, 'COMMENT');
  assert.deepEqual(f.reviews[0].comments, []);
  assert.deepEqual(f.failures, []);
});

test('actionable findings become inline threads and fail the check', async () => {
  const f = fixture();
  await publishReview({ ...f, reviewText: JSON.stringify({ summary: 'Cap regression.', findings: [finding] }) });
  assert.equal(f.reviews[0].comments[0].line, 11);
  assert.equal(f.reviews[0].comments[0].side, 'RIGHT');
  assert.match(f.reviews[0].comments[0].body, /P1/);
  assert.equal(f.failures.length, 1);
});

test('invalid or empty output cannot produce a passing review', async () => {
  for (const reviewText of ['', '{', '{}', '{"summary":"Fine","findings":null}',
      JSON.stringify({ summary: 'Invalid', findings: [{ ...finding, priority: 'unknown' }] })]) {
    const f = fixture();
    await assert.rejects(publishReview({ ...f, reviewText }));
    assert.equal(f.reviews.length, 0);
  }
});

test('a changed head cannot publish a stale pass', async () => {
  const f = fixture({ currentHead: 'b'.repeat(40) });
  await assert.rejects(publishReview({ ...f, reviewText: '{"summary":"Fine","findings":[]}' }), /head changed/i);
  assert.equal(f.reviews.length, 0);
});

test('closed and draft pull requests do not receive reviews', async () => {
  for (const options of [{ state: 'closed' }, { draft: true }]) {
    const f = fixture(options);
    await assert.rejects(publishReview({ ...f, reviewText: '{"summary":"Fine","findings":[]}' }));
    assert.equal(f.reviews.length, 0);
  }
});

test('unavailable diff positions remain visible and still block the check', async () => {
  const f = fixture();
  await publishReview({ ...f, reviewText: JSON.stringify({ summary: 'Issue outside a commentable line.',
    findings: [{ ...finding, line: 999 }] }) });
  assert.deepEqual(f.reviews[0].comments, []);
  assert.match(f.reviews[0].body, /Core.java:999/);
  assert.match(f.reviews[0].body, /Preserve the hard cap/);
  assert.equal(f.failures.length, 1);
});

test('deleted lines and context do not count as newly added review lines', async () => {
  const f = fixture({ patch: '@@ -10,2 +10,2 @@\n-old\n+new\n context' });
  await publishReview({ ...f, reviewText: JSON.stringify({ summary: 'Context-only position.',
    findings: [{ ...finding, line: 11 }] }) });
  assert.deepEqual(f.reviews[0].comments, []);
  assert.equal(f.failures.length, 1);
});

test('GitHub publication errors cannot be reported as successful reviews', async () => {
  const f = fixture();
  f.github.rest.pulls.createReview = async () => { throw new Error('GitHub unavailable'); };
  await assert.rejects(publishReview({ ...f, reviewText: '{"summary":"Fine","findings":[]}' }), /GitHub unavailable/);
});
