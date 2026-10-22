"""Acceptance for a disposable Compose stack; recreates containers, never volumes."""
import argparse
import csv
import io
import json
import re
import subprocess
import time
import urllib.error
import urllib.request


def request(base, path, method='GET', body=None, expected=200):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(base + path, data=data, method=method,
                                 headers={'Content-Type': 'application/json'})
    try:
        response = urllib.request.urlopen(req, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        content = response.read().decode()
        assert response.status == expected, (path, response.status, content)
        return content, response.headers


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project', required=True, help='Disposable test Compose project only')
    parser.add_argument('--base-url', default='http://127.0.0.1:8080')
    args = parser.parse_args()
    base = args.base_url.rstrip('/')

    def compose(*arguments):
        return subprocess.check_output(['docker', 'compose', '-p', args.project, *arguments], text=True).strip()

    def api(path, method='GET', body=None, expected=200):
        content, _ = request(base, path, method, body, expected)
        return json.loads(content) if content else None

    def probe(path):
        raw = compose('exec', '-T', 'session-manager-service', 'wget', '-q', '-O', '-',
                      'http://127.0.0.1:8080' + path)
        return json.loads(raw)

    def wait_recovery():
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            try:
                if probe('/actuator/health/readiness') == {'status': 'UP'}:
                    api('/api/v1/events')
                    return
            except (subprocess.CalledProcessError, OSError, AssertionError):
                time.sleep(2)
        raise AssertionError('API/proxy did not recover within 90 seconds')

    html, headers = request(base, '/events')
    assert '<div id="root">' in html
    for path in ['/events/arbitrary-id', '/events/arbitrary-id/groups']:
        assert request(base, path)[0] == html
    for name, value in [('X-Content-Type-Options', 'nosniff'), ('Referrer-Policy', 'no-referrer'),
                        ('Cache-Control', 'no-store')]:
        assert headers[name] == value
    assert "frame-ancestors 'none'" in headers['Content-Security-Policy']
    assets = re.findall(r'(?:src|href)="(/assets/[^\"]+)"', html)
    assert len(assets) >= 2
    for path in assets:
        content, asset_headers = request(base, path)
        assert content and 'text/html' not in asset_headers['Content-Type']
    request(base, '/assets/does-not-exist.js', expected=404)
    missing, missing_headers = request(base, '/api/v1/not-a-route', expected=404)
    assert '<div id="root">' not in missing
    assert missing_headers['X-Content-Type-Options'] == 'nosniff'
    assert probe('/actuator/health/readiness') == {'status': 'UP'}

    config = json.loads(compose('config', '--format', 'json'))
    services = config['services']
    ids = {name: compose('ps', '-q', name) for name in services}
    assert all(ids.values())
    containers = {name: json.loads(subprocess.check_output(['docker', 'inspect', cid], text=True))[0]
                  for name, cid in ids.items()}
    for name, container in containers.items():
        assert container['State']['Health']['Status'] == 'healthy', name
        if name != 'ui':
            assert not container['HostConfig']['PortBindings'], name
    ui_networks = set(containers['ui']['NetworkSettings']['Networks'])
    db_networks = set(containers['db']['NetworkSettings']['Networks'])
    assert not ui_networks & db_networks
    for name in ['ui', 'session-manager-service']:
        uid = compose('exec', '-T', name, 'id', '-u')
        assert uid != '0', (name, uid)
    compose('exec', '-T', 'ui', 'sh', '-c', 'set -e; ! command -v node; ! command -v npm')
    compose('exec', '-T', 'session-manager-service', 'sh', '-c',
            'set -e; java -version; ! command -v javac; ! command -v mvn; ! command -v node')
    image = json.loads(subprocess.check_output(['docker', 'image', 'inspect',
                       services['session-manager-service']['image']], text=True))[0]
    assert image['Size'] < 300_000_000, image['Size']

    event = api('/api/v1/events', 'POST', {'name': 'Container acceptance', 'config': {'capSize': 6}}, 201)
    path = '/api/v1/events/' + event['id']
    try:
        people = []
        for name in ['=SUM(1,2)', 'Second attendee']:
            people.append(api(path + '/participants', 'POST',
                              {'name': name, 'age': 18, 'gender': 'MALE'}, 201))
        proposal = api(path + '/assignment', 'POST', {'facilitators': [], 'facilitatorExceptions': []})
        assert proposal['result']['state'] == 'NON_FINAL_PROPOSAL'
        assert proposal['result']['blockers']
        staff = {'facilitators': [{'name': 'Sam', 'role': 'LEADER', 'gender': 'MALE',
                                  'qualifiedTiers': ['YOUTH']}], 'facilitatorExceptions': []}
        final = api(path + '/assignment', 'POST', staff)
        assert final['result']['state'] == 'FINAL'
        assert final['result']['totalAssignedParticipants'] == 2
        exported, csv_headers = request(base, path + '/assignment/export.csv')
        rows = list(csv.reader(io.StringIO(exported)))
        assert all(len(row) == 12 for row in rows)
        assert "'=SUM(1,2)" in [row[7] for row in rows]
        assert 'attachment;' in csv_headers['Content-Disposition']
        assert api(path + '/assignment') == final

        # Recreate every process while retaining the named database volume.
        compose('down')
        compose('up', '--detach', '--wait', '--wait-timeout', '150')
        wait_recovery()
        assert api(path)['name'] == 'Container acceptance'
        assert len(api(path + '/participants')) == 2
        assert api(path + '/assignment') == final

        # A database outage must affect readiness, while liveness stays available.
        compose('stop', 'db')
        try:
            deadline = time.monotonic() + 30
            while True:
                try:
                    probe('/actuator/health/readiness')
                except subprocess.CalledProcessError:
                    break
                assert time.monotonic() < deadline, 'Readiness stayed healthy without a database'
                time.sleep(1)
            assert probe('/actuator/health/liveness') == {'status': 'UP'}
        finally:
            compose('start', 'db')
        wait_recovery()
        assert api(path + '/assignment') == final

        # A second client invalidates stored output; recompute and verify new data.
        person = people[1]
        api(path + '/participants/' + person['id'], 'PUT',
            {'name': 'Remote correction', 'age': 19, 'gender': 'MALE'})
        api(path + '/assignment', expected=204)
        request(base, path + '/assignment/export.csv', expected=409)
        refreshed = api(path + '/assignment', 'POST', staff)
        assert refreshed['revision'] > final['revision']
        assert 'Remote correction' in request(base, path + '/assignment/export.csv')[0]
        api(path + '/participants/' + person['id'], 'DELETE', expected=204)
        assert len(api(path + '/participants')) == 1
    finally:
        api(path, 'DELETE', expected=204)
    print('Three-container acceptance passed: routes, headers, isolation, health, CRUD, assignments, CSV and persistence/recovery.')


if __name__ == '__main__':
    main()
