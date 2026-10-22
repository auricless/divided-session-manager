'use strict';

export async function api(path, method = 'GET', body) {
  const response = await fetch(path, {
    method,
    headers: body === undefined ? {} : {'Content-Type': 'application/json'},
    body: body === undefined ? undefined : JSON.stringify(body),
    cache: 'no-store'
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) throw new Error(data?.message || data?.detail || `Request failed (${response.status}).`);
  return data;
}

export function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
}

function list(title, values, parent, className) {
  if (!values.length) return;
  parent.append(element('h3', title));
  const items = element('ul', undefined, className);
  values.forEach(value => items.append(element('li', value)));
  parent.append(items);
}

function renderAssignment(snapshot, view) {
  view.replaceChildren();
  if (!snapshot) {
    view.append(element('p', 'No current assignment. Generate groups after roster or settings changes.', 'muted'));
    return;
  }
  const result = snapshot.result;
  const final = result.state === 'FINAL';
  const badge = element('p', final ? 'Final assignment' : 'Proposal — action required', final ? 'badge' : 'badge proposal');
  badge.dataset.state = result.state;
  view.append(badge, element('p', `${result.totalGroups} ${result.totalGroups === 1 ? 'group' : 'groups'} · ${result.totalAssignedParticipants} assigned ${result.totalAssignedParticipants === 1 ? 'participant' : 'participants'}`));
  list('Actions required', result.blockers.map(blocker => blocker.message), view, 'warning');
  list('Warnings', result.warnings, view, 'warning');
  result.groups.forEach((group, index) => {
    const card = element('section', undefined, 'group');
    card.append(element('h3', `Group ${index + 1} · ${[group.ageTierTarget, group.genderTarget, group.lifeStageTarget].filter(Boolean).join(' / ')}`));
    card.append(element('p', `Leader: ${group.leader?.name || 'Not assigned — blocker'}`));
    if (group.assistant) card.append(element('p', `Assistant: ${group.assistant.name}`));
    list(`${group.size} ${group.size === 1 ? 'participant' : 'participants'}`, group.participants.map(person => `${person.name} (${person.age})`), card);
    view.append(card);
  });
  list('Unassigned participants', result.unassignedParticipants.map(item => `${item.participant.name}: ${item.reason}`), view, 'warning');
}


export function createAssignmentView(eventPath, target, options = {}) {
  let sequence = 0;
  let stopped = false;
  let started = false;
  let pollCycle = 0;
  let timer;
  function draw(snapshot) {
    renderAssignment(snapshot, target);
    options.onAvailable?.(snapshot);
  }
  async function refresh() {
    if (stopped) return null;
    const current = ++sequence;
    try {
      const [event, snapshot] = await Promise.all([api(eventPath), api(`${eventPath}/assignment`)]);
      if (stopped || current !== sequence) return null;
      options.onEvent?.(event);
      // Concurrent GETs can straddle a roster/config change. Never expose that stale snapshot.
      const verified = snapshot?.revision === event.revision ? snapshot : null;
      draw(verified);
      return verified;
    } catch (error) {
      if (!stopped && current === sequence) {
        target.replaceChildren(element('p', `Cannot verify the current assignment: ${error.message} Retrying…`, 'warning'));
        options.onAvailable?.(null);
      }
      throw error;
    }
  }
  async function poll(cycle) {
    try { await refresh(); }
    catch { /* refresh already clears unverified results; keep retrying this polling cycle. */ }
    finally {
      if (!stopped && cycle === pollCycle) timer = window.setTimeout(() => poll(cycle), 2000);
    }
  }
  function start() {
    if (started && !stopped) return;
    started = true;
    stopped = false;
    return poll(++pollCycle);
  }
  window.addEventListener('pagehide', () => {
    stopped = true;
    ++sequence;
    ++pollCycle;
    window.clearTimeout(timer);
    draw(null);
  });
  window.addEventListener('pageshow', event => {
    if (event.persisted && started && stopped) start();
  });
  return {refresh, show(snapshot) { if (!stopped) { ++sequence; draw(snapshot); } }, start};
}
