import {createAssignmentView} from './assignment-view.js';
const eventId = document.querySelector('[data-event-id]').dataset.eventId;
const path = `/api/v1/events/${encodeURIComponent(eventId)}`;
const print = document.querySelector('#print-roster');
const csv = document.querySelector('#export-csv');
const view = createAssignmentView(path, document.querySelector('#assignment'), {
  onEvent(event) { document.querySelector('#event-heading').textContent = event.name; document.title = `${event.name} · Groups`; },
  onAvailable(snapshot) { print.disabled = !snapshot; csv.disabled = !snapshot; }
});
print.addEventListener('click', async () => { await view.refresh(); if (!print.disabled) window.print(); });
csv.addEventListener('click', async () => { await view.refresh(); if (!csv.disabled) window.location.assign(`${path}/assignment/export.csv`); });
view.start();
