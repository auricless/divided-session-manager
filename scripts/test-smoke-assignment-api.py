"""Exercise the smoke script against a real HTTP server and startup connection reset."""

import json
from pathlib import Path
import socket
import struct
import subprocess
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


REPO_ROOT = Path(__file__).resolve().parents[1]


class SmokeScriptTest(unittest.TestCase):
    def test_retries_a_startup_connection_reset_before_validating_assignment(self):
        requests = []
        response = json.dumps({
            "state": "FINAL",
            "totalGroups": 1,
            "totalAssignedParticipants": 2,
            "groups": [{"leader": {"name": "Sam"}}],
            "blockers": [],
        }).encode()

        class Handler(BaseHTTPRequestHandler):
            def do_POST(self):
                body = self.rfile.read(int(self.headers["Content-Length"]))
                requests.append((self.path, json.loads(body)))
                if len(requests) == 1:
                    self.close_connection = True
                    self.connection.setsockopt(
                        socket.SOL_SOCKET, socket.SO_LINGER, struct.pack("ii", 1, 0)
                    )
                    self.connection.close()
                    return
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(response)))
                self.end_headers()
                self.wfile.write(response)

            def log_message(self, *_args):
                pass

        with ThreadingHTTPServer(("127.0.0.1", 0), Handler) as server:
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            try:
                result = subprocess.run(
                    ["sh", "scripts/smoke-assignment-api.sh",
                     f"http://127.0.0.1:{server.server_port}"],
                    cwd=REPO_ROOT, capture_output=True, text=True, timeout=15,
                )
            finally:
                server.shutdown()
                thread.join()

        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual(2, len(requests))
        self.assertEqual("/api/v1/sessions/assign", requests[1][0])
        self.assertEqual(requests[0], requests[1], "The retried request must preserve its body")
        self.assertIn("Assignment smoke passed", result.stdout)


if __name__ == "__main__":
    unittest.main()
