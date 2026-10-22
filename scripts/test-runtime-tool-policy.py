"""An unexpected tool at any position must fail the actual container policy."""
import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('stack', Path(__file__).with_name('smoke-container-stack.py'))
stack = importlib.util.module_from_spec(spec)
spec.loader.exec_module(stack)


class RuntimeToolPolicy(unittest.TestCase):
    def test_rejects_each_forbidden_tool_even_when_later_tools_are_absent(self):
        for tools in [('node', 'npm'), ('javac', 'mvn', 'node')]:
            for forbidden in tools:
                with self.subTest(forbidden=forbidden), tempfile.TemporaryDirectory() as directory:
                    executable = Path(directory, forbidden)
                    executable.write_text('#!/bin/sh\nexit 0\n')
                    executable.chmod(0o755)
                    result = subprocess.run(['/bin/sh', '-c', stack.RUNTIME_TOOL_CHECK, 'policy', *tools],
                                            env={**os.environ, 'PATH': directory}, capture_output=True)
                    self.assertNotEqual(result.returncode, 0, f'{forbidden} was silently accepted')

    def test_accepts_a_runtime_without_any_forbidden_tools(self):
        with tempfile.TemporaryDirectory() as directory:
            result = subprocess.run(['/bin/sh', '-c', stack.RUNTIME_TOOL_CHECK, 'policy', 'javac', 'mvn', 'node', 'npm'],
                                    env={**os.environ, 'PATH': directory}, capture_output=True)
            self.assertEqual(result.returncode, 0)


if __name__ == '__main__':
    unittest.main()
