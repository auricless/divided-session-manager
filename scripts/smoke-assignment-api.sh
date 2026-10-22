#!/bin/sh
set -eu

# Run from the repository root after starting the service or Compose stack.
base_url=${1:-http://127.0.0.1:8080}
response_file=$(mktemp)
trap 'rm -f "$response_file"' EXIT HUP INT TERM

# The assignment endpoint is stateless, so replaying this sample POST is safe.
# Container startup can reset a connection before the HTTP listener is ready.
curl --fail-with-body --silent --show-error \
  --retry 30 --retry-all-errors --retry-delay 1 --retry-max-time 45 \
  --connect-timeout 2 --max-time 5 \
  "$base_url/api/v1/sessions/assign" \
  -H 'Content-Type: application/json' \
  --data-binary @session-manager-service/examples/assign-request.json \
  --output "$response_file"

python3 - "$response_file" <<'PY'
import json
import sys

with open(sys.argv[1]) as response:
    result = json.load(response)
assert result['state'] == 'FINAL', result
assert result['totalGroups'] == 1, result
assert result['totalAssignedParticipants'] == 2, result
assert result['groups'][0]['leader']['name'] == 'Sam', result
assert result['blockers'] == [], result
print('Assignment smoke passed: FINAL, one staffed group, two participants.')
PY
