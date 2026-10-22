import {api, element, createAssignmentView} from './assignment-view.js';
'use strict';

const tiers = ['TEENS', 'YOUTH', 'YOUNG_ADULT'];
const feedback = document.querySelector('#feedback');
const main = document.querySelector('[data-event-id]');
const eventId = main?.dataset.eventId;
const eventPath = eventId ? `/api/v1/events/${encodeURIComponent(eventId)}` : null;
const assignmentView = main ? createAssignmentView(eventPath, document.querySelector('#assignment'), {
  onEvent(event) { document.querySelector('#event-heading').textContent = event.name; document.title = `${event.name} · Session Manager`; }
}) : null;

function report(message, error = false) {
  feedback.textContent = message;
  feedback.classList.toggle('error', error);
}

function config(form) {
  const fields = form.elements;
  return {
    capSize: Number(fields.namedItem('capSize').value),
    targetSize: Number(fields.namedItem('targetSize').value),
    genderMode: fields.namedItem('genderMode').value,
    activeTiers: [...form.querySelectorAll('[name="activeTiers"]:checked')].map(input => input.value),
    lifeStageSubgroupingEnabled: fields.namedItem('lifeStageSubgroupingEnabled').checked,
    ageTierBoundaries: Object.fromEntries(tiers.map(tier => [tier, {
      minAge: Number(fields.namedItem(`${tier}.minAge`).value),
      maxAge: fields.namedItem(`${tier}.maxAge`).value === '' ? 2147483647 : Number(fields.namedItem(`${tier}.maxAge`).value)
    }]))
  };
}

function onSubmit(selector, action) {
  const form = document.querySelector(selector);
  if (!form) return;
  form.addEventListener('submit', async event => {
    event.preventDefault();
    const buttons = [...form.querySelectorAll('[type="submit"]')];
    buttons.forEach(button => { button.disabled = true; });
    try { await action(form); } catch (error) { report(error.message, true); }
    finally { buttons.forEach(button => { button.disabled = false; }); }
  });
}

function actionButton(label, action) {
  const button = element('button', label, 'secondary');
  button.type = 'button';
  button.addEventListener('click', () => { action().catch(error => report(error.message, true)); });
  return button;
}

async function refreshParticipants() {
  const participants = await api(`${eventPath}/participants`);
  const rows = document.querySelector('#participants');
  rows.replaceChildren();
  document.querySelector('#participant-count').textContent = `(${participants.length})`;
  for (const participant of participants) {
    const row = element('tr');
    for (const value of [participant.name, participant.age, participant.gender,
      [participant.ageTier || 'Excluded', participant.lifeStage].filter(Boolean).join(' / ')]) {
      row.append(element('td', String(value)));
    }
    const actions = element('td');
    actions.append(actionButton('Edit', async () => {
      const form = document.querySelector('#participant-form');
      for (const field of ['name', 'age', 'gender', 'lifeStage']) form.elements.namedItem(field).value = participant[field] ?? '';
      form.elements.namedItem('participantId').value = participant.id;
      form.elements.namedItem('name').focus();
    }));
    actions.append(actionButton('Remove', async () => {
      if (!confirm(`Remove ${participant.name} from this event?`)) return;
      await api(`${eventPath}/participants/${encodeURIComponent(participant.id)}`, 'DELETE');
      await refreshParticipants();
      await refreshAssignment();
      report('Participant removed. Generate groups again when ready.');
    }));
    row.append(actions);
    rows.append(row);
  }
}

function refreshAssignment() { return assignmentView.refresh(); }
function renderAssignment(snapshot) { assignmentView.show(snapshot); }

onSubmit('#create-event-form', async form => {
  const event = await api('/api/v1/events', 'POST', {name: form.elements.namedItem('name').value, config: config(form)});
  window.location.assign(`/events/${encodeURIComponent(event.id)}`);
});

if (eventId) {
  onSubmit('#event-form', async form => {
    const event = await api(eventPath, 'PUT', {name: form.elements.namedItem('name').value, config: config(form)});
    document.querySelector('#event-heading').textContent = event.name;
    document.title = `${event.name} · Session Manager`;
    await refreshParticipants();
    await refreshAssignment();
    report('Settings saved. The current assignment below reflects whether regrouping is needed.');
  });
  onSubmit('#participant-form', async form => {
    const fields = form.elements;
    const participantId = fields.namedItem('participantId').value;
    await api(`${eventPath}/participants${participantId ? '/' + encodeURIComponent(participantId) : ''}`, participantId ? 'PUT' : 'POST', {
      name: fields.namedItem('name').value,
      age: Number(fields.namedItem('age').value),
      gender: fields.namedItem('gender').value,
      lifeStage: fields.namedItem('lifeStage').value || null
    });
    form.reset();
    fields.namedItem('participantId').value = '';
    await refreshParticipants();
    await refreshAssignment();
    report('Participant saved. Generate groups again when ready.');
  });
  document.querySelector('#cancel-edit').addEventListener('click', () => {
    const form = document.querySelector('#participant-form');
    form.reset();
    form.elements.namedItem('participantId').value = '';
  });
  document.querySelector('#delete-event').addEventListener('click', async () => {
    if (!confirm('Delete this event, all its participants and its current assignment?')) return;
    try { await api(eventPath, 'DELETE'); window.location.assign('/events'); }
    catch (error) { report(error.message, true); }
  });
  document.querySelector('#add-facilitator').addEventListener('click', () => {
    const row = document.querySelector('#facilitator-template').content.cloneNode(true);
    row.querySelector('.remove-facilitator').addEventListener('click', event => event.target.closest('fieldset').remove());
    document.querySelector('#facilitators').append(row);
  });
  onSubmit('#assignment-form', async form => {
    const rows = [...form.querySelectorAll('.facilitator')];
    const facilitators = rows.map(row => ({
      name: row.querySelector('[name="facilitatorName"]').value,
      role: row.querySelector('[name="role"]').value,
      gender: row.querySelector('[name="facilitatorGender"]').value,
      qualifiedTiers: [...row.querySelectorAll('[name="qualifiedTiers"]:checked')].map(input => input.value)
    }));
    const facilitatorExceptions = rows.flatMap((row, facilitatorIndex) => {
      const allowAgeTierMismatch = row.querySelector('[name="allowAgeTierMismatch"]').checked;
      const allowGenderMismatch = row.querySelector('[name="allowGenderMismatch"]').checked;
      return allowAgeTierMismatch || allowGenderMismatch ? [{facilitatorIndex, allowAgeTierMismatch, allowGenderMismatch}] : [];
    });
    const result = await api(`${eventPath}/assignment`, 'POST', {facilitators, facilitatorExceptions});
    renderAssignment(result);
    form.querySelectorAll('[name="allowAgeTierMismatch"], [name="allowGenderMismatch"]').forEach(input => { input.checked = false; });
    report('Groups computed. Explicit exceptions have been cleared for the next run.');
  });
  refreshParticipants().catch(error => report(error.message, true));
  assignmentView.start();
}
