#!/usr/bin/env python3
"""Prove the actual CI npm-install command cannot run a dependency lifecycle script."""
import io
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import tarfile
import tempfile
import unittest


class FrontendInstallPolicyTest(unittest.TestCase):
    def test_ci_installs_dependencies_without_running_their_lifecycle_scripts(self):
        workflow = (Path(__file__).resolve().parents[1] / '.github/workflows/pr-review.yml').read_text()
        commands = re.findall(r'^\s*- run: (npm ci[^\n]*)$', workflow, re.MULTILINE)
        self.assertEqual(1, len(commands), 'Find the real frontend CI install command')
        command = shlex.split(commands[0])
        self.assertEqual(['npm', 'ci'], command[:2])
        with tempfile.TemporaryDirectory(prefix='frontend-install-policy-') as directory:
            root = Path(directory)
            marker = root / 'lifecycle-ran'
            package = {
                'name': 'lifecycle-fixture', 'version': '1.0.0',
                'scripts': {'postinstall': 'node -e "require(\'node:fs\').writeFileSync(process.env.INSTALL_POLICY_MARKER, \'executed\')"'}
            }
            content = json.dumps(package).encode()
            with tarfile.open(root / 'fixture.tgz', 'w:gz') as archive:
                entry = tarfile.TarInfo('package/package.json')
                entry.size = len(content)
                archive.addfile(entry, io.BytesIO(content))
            (root / 'package.json').write_text(json.dumps({
                'name': 'install-policy-test', 'version': '1.0.0', 'private': True,
                'dependencies': {'lifecycle-fixture': 'file:./fixture.tgz'}
            }))
            environment = {**os.environ, 'INSTALL_POLICY_MARKER': str(marker)}
            def run(arguments):
                result = subprocess.run(arguments, cwd=root, env=environment, text=True,
                                        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=30)
                self.assertEqual(0, result.returncode, result.stdout)
            run(['npm', 'install', '--package-lock-only', '--ignore-scripts', '--no-audit', '--no-fund'])
            run(command + ['--no-audit', '--no-fund'])
            self.assertTrue((root / 'node_modules/lifecycle-fixture/package.json').is_file())
            self.assertFalse(marker.exists(), 'CI executed the dependency postinstall script')


if __name__ == '__main__':
    unittest.main()
