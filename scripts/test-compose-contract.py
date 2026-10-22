"""Verify the resolved deployment contract without needing a Docker daemon."""
import json
import os
import subprocess
import unittest


def configuration(*files):
    command = ['docker', 'compose']
    for path in files:
        command += ['-f', path]
    return json.loads(subprocess.check_output(
        command + ['config', '--format', 'json'],
        env={**os.environ, 'POSTGRES_PASSWORD': 'contract-only', 'UI_PORT': '18088'},
        text=True,
    ))


class DeploymentContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.config = configuration('docker-compose.yml')
        cls.services = cls.config['services']

    def test_only_ui_publishes_a_configurable_loopback_port(self):
        self.assertEqual(set(self.services), {'ui', 'session-manager-service', 'db'})
        published = [(name, port) for name, service in self.services.items()
                     for port in service.get('ports', [])]
        self.assertEqual(len(published), 1)
        name, port = published[0]
        self.assertEqual(name, 'ui')
        self.assertEqual((port['host_ip'], port['published'], port['target']),
                         ('127.0.0.1', '18088', 8080))

    def test_ui_cannot_share_a_database_network(self):
        networks = {name: set(service['networks']) for name, service in self.services.items()}
        self.assertTrue(networks['ui'] & networks['session-manager-service'])
        database = networks['db'] & networks['session-manager-service']
        self.assertTrue(database)
        self.assertFalse(networks['ui'] & networks['db'])
        self.assertTrue(all(self.config['networks'][name].get('internal') for name in database))

    def test_startup_waits_for_database_and_api_health_and_retains_data(self):
        api = self.services['session-manager-service']
        self.assertEqual(api['depends_on']['db']['condition'], 'service_healthy')
        self.assertEqual(self.services['ui']['depends_on']['session-manager-service']['condition'],
                         'service_healthy')
        self.assertIn('/actuator/health/readiness', ' '.join(api['healthcheck']['test']))
        self.assertTrue(any(volume['type'] == 'volume' and volume['source'] == 'postgres-data'
                            for volume in self.services['db']['volumes']))

    def test_database_password_is_required(self):
        environment = {key: value for key, value in os.environ.items() if key != 'POSTGRES_PASSWORD'}
        result = subprocess.run(['docker', 'compose', 'config', '--quiet'],
                                env=environment, capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('POSTGRES_PASSWORD', result.stderr)


if __name__ == '__main__':
    unittest.main()
