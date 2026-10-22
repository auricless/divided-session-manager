import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import test from 'node:test';

// Load the browser module without changing how .js files are served or adding a bundler.
const source = await readFile(new URL('../../main/resources/static/assets/assignment-view.js', import.meta.url), 'utf8');
const {createAssignmentView} = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);
const eventPath = '/api/v1/events/example';
const snapshot = revision => ({revision, result: {
  state: 'FINAL', totalGroups: 0, totalAssignedParticipants: 0,
  blockers: [], warnings: [], groups: [], unassignedParticipants: []
}});
const deferred = () => {
  let resolve;
  const promise = new Promise(done => { resolve = done; });
  return {promise, resolve};
};
const flush = () => new Promise(resolve => setImmediate(resolve));

function fixture(t) {
  const saved = {window: globalThis.window, document: globalThis.document, fetch: globalThis.fetch};
  const requests = [];
  const timers = new Map();
  const events = new EventTarget();
  let nextTimer = 0;
  globalThis.window = {
    addEventListener: events.addEventListener.bind(events),
    setTimeout(callback) { timers.set(++nextTimer, callback); return nextTimer; },
    clearTimeout(id) { timers.delete(id); }
  };
  const node = () => ({
    children: [], dataset: {}, textContent: '',
    append(...children) { this.children.push(...children); },
    replaceChildren(...children) { this.children = children; }
  });
  globalThis.document = {createElement: node};
  globalThis.fetch = path => {
    const pending = deferred();
    requests.push({path, answer(data, status = 200) {
      pending.resolve({ok: status < 400, status, text: async () => data === null ? '' : JSON.stringify(data)});
    }});
    return pending.promise;
  };
  t.after(() => Object.assign(globalThis, saved));
  const available = [];
  const eventNames = [];
  const target = node();
  const view = createAssignmentView(eventPath, target, {
    onAvailable(value) { available.push(value); },
    onEvent(value) { eventNames.push(value.name); }
  });
  return {
    view, requests, timers, available, eventNames, target,
    respondPair(offset, eventRevision, assignment) {
      assert.equal(requests[offset].path, eventPath);
      assert.equal(requests[offset + 1].path, `${eventPath}/assignment`);
      requests[offset].answer({name: 'Current event', revision: eventRevision});
      requests[offset + 1].answer(assignment, assignment === null ? 204 : 200);
    },
    fire(type, persisted = true) { events.dispatchEvent(Object.assign(new Event(type), {persisted})); },
    async tick() {
      assert.equal(timers.size, 1, 'exactly one next poll is scheduled');
      const [id, callback] = timers.entries().next().value;
      timers.delete(id);
      callback();
      await flush();
    }
  };
}

test('a mismatched revision clears a previously available roster before print/export refresh', async t => {
  const f = fixture(t);
  f.view.show(snapshot(1));
  const refresh = f.view.refresh();
  f.respondPair(0, 2, snapshot(1));
  const refreshed = await refresh;
  assert.equal(f.available.at(-1), null, 'stale roster must disable print/export');
  assert.equal(refreshed, null);
  assert.equal(f.target.children.some(child => child.dataset.state === 'FINAL'), false);
});

test('matching revisions render the current assignment and a missing snapshot clears it', async t => {
  const f = fixture(t);
  const current = snapshot(2);
  let refresh = f.view.refresh();
  f.respondPair(0, 2, current);
  assert.deepEqual(await refresh, current);
  assert.deepEqual(f.available.at(-1), current);
  assert.deepEqual(f.eventNames, ['Current event']);
  refresh = f.view.refresh();
  f.respondPair(2, 3, null);
  assert.equal(await refresh, null);
  assert.equal(f.available.at(-1), null);
});

test('back-forward restoration immediately refreshes and resumes a single polling loop', async t => {
  const f = fixture(t);
  f.view.start();
  f.respondPair(0, 1, snapshot(1));
  await flush();
  f.fire('pagehide');
  assert.equal(f.timers.size, 0);
  assert.equal(f.available.at(-1), null, 'hidden page must not retain enabled export controls');
  f.fire('pageshow');
  assert.equal(f.requests.length, 4, 'restoration starts a fresh request immediately');
  f.respondPair(2, 2, snapshot(2));
  await flush();
  assert.equal(f.available.at(-1).revision, 2);
  await f.tick();
  f.respondPair(4, 2, snapshot(2));
  await flush();
  assert.equal(f.timers.size, 1);
  // A second navigation must have the same lifecycle behavior.
  f.fire('pagehide');
  f.fire('pageshow');
  assert.equal(f.requests.length, 8);
  f.respondPair(6, 3, snapshot(3));
  await flush();
  assert.equal(f.timers.size, 1);
});

test('an in-flight response from before pagehide cannot render or schedule a duplicate loop', async t => {
  const f = fixture(t);
  f.view.start();
  f.fire('pagehide');
  f.fire('pageshow');
  assert.equal(f.requests.length, 4);
  f.respondPair(2, 2, snapshot(2));
  await flush();
  f.respondPair(0, 1, snapshot(1));
  await flush();
  assert.equal(f.available.at(-1).revision, 2);
  assert.equal(f.timers.size, 1);
});

test('repeated starts do not create concurrent polling loops', async t => {
  const f = fixture(t);
  f.view.start();
  f.view.start();
  assert.equal(f.requests.length, 2);
  f.respondPair(0, 1, snapshot(1));
  await flush();
  assert.equal(f.timers.size, 1);
});

test('a direct refresh failure clears verified controls and the poller retries', async t => {
  const f = fixture(t);
  f.view.show(snapshot(1));
  const refresh = f.view.refresh();
  f.requests[0].answer({message: 'Service unavailable'}, 503);
  f.requests[1].answer(null, 204);
  await assert.rejects(refresh, /Service unavailable/);
  assert.equal(f.available.at(-1), null);
  assert.match(f.target.children[0].textContent, /Cannot verify/);
  f.view.start();
  f.respondPair(2, 2, snapshot(2));
  await flush();
  assert.equal(f.available.at(-1).revision, 2);
  assert.equal(f.timers.size, 1);
});
