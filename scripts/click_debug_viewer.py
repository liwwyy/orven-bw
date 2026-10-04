#!/usr/bin/env python3
"""Serve the orven-bw click log viewer locally using only the Python standard library."""
import argparse
from collections import deque
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import math
from pathlib import Path
import threading
from urllib.parse import urlsplit

DEFAULT_DIRECTORY = Path('/home/user/.local/share/Polyfrost/OneClient/clusters/1.8.9 OC/config/orven-bw')
ASSETS = Path(__file__).resolve().parent / 'click_debug'


class LogCache:
    def __init__(self, directory=DEFAULT_DIRECTORY, log=None, limit=20000):
        self.directory, self.explicit, self.limit = Path(directory), Path(log) if log else None, limit
        self.lock = threading.Lock()
        self.reset()

    def reset(self):
        self.rows = deque(maxlen=self.limit)
        self.identity, self.prefix, self.position = None, b'', 0
        self.seen = self.malformed = 0

    def locate(self):
        if self.explicit:
            return self.explicit
        canonical = self.directory / 'click-debug.jsonl'
        if canonical.exists():
            return canonical
        candidates = list(self.directory.glob('*.jsonl')) + list(self.directory.glob('*.log'))
        return max(candidates, key=lambda p: p.stat().st_mtime_ns) if candidates else canonical

    def snapshot(self):
        with self.lock:
            path = self.locate()
            error = None
            try:
                stat = path.stat()
                with path.open('rb') as file:
                    prefix = file.read(256)
                    identity = (str(path), stat.st_dev, stat.st_ino)
                    replaced = self.prefix and not prefix.startswith(self.prefix)
                    if identity != self.identity or stat.st_size < self.position or replaced:
                        self.reset()
                    self.identity, self.prefix = identity, prefix
                    file.seek(self.position)
                    while file.tell() < stat.st_size:
                        offset = file.tell()
                        raw = file.readline()
                        if not raw.endswith(b'\n'):
                            file.seek(offset)  # The active writer may still be completing this line.
                            break
                        try:
                            row = json.loads(raw)
                            if not isinstance(row, dict):
                                raise ValueError('Expected an object')
                            if row.get('event') == 'input':
                                if not isinstance(row.get('timestamp_ms'), (int, float)) or not math.isfinite(row['timestamp_ms']) or not isinstance(row.get('session'), str):
                                    raise ValueError('Missing timestamp/session')
                                self.rows.append(row)
                                self.seen += 1
                        except (ValueError, UnicodeDecodeError):
                            self.malformed += 1
                    self.position = file.tell()
            except FileNotFoundError:
                self.reset()
            except OSError as exception:
                error = str(exception)
            return dict(events=list(self.rows), path=str(path), seen=self.seen,
                        retained=len(self.rows), malformed=self.malformed, error=error)


def handler_for(cache):
    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            route = urlsplit(self.path).path
            if route == '/api/events':
                body = json.dumps(cache.snapshot(), allow_nan=False).encode()
                content_type = 'application/json; charset=utf-8'
            elif route in ('/', '/viewer.js'):
                body = (ASSETS / ('index.html' if route == '/' else 'viewer.js')).read_bytes()
                content_type = 'text/html; charset=utf-8' if route == '/' else 'text/javascript; charset=utf-8'
            else:
                self.send_error(404)
                return
            self.send_response(200)
            self.send_header('Content-Type', content_type)
            self.send_header('Content-Length', str(len(body)))
            self.send_header('Cache-Control', 'no-store')
            self.send_header('X-Content-Type-Options', 'nosniff')
            self.end_headers()
            self.wfile.write(body)

        def log_message(self, *_):
            pass
    return Handler


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--directory', type=Path, default=DEFAULT_DIRECTORY)
    parser.add_argument('--log', type=Path, help='Read this file instead of discovering the default log')
    parser.add_argument('--host', default='127.0.0.1')
    parser.add_argument('--port', type=int, default=8765)
    parser.add_argument('--max-events', type=int, default=20000, help='Number of recent records retained in the viewer')
    args = parser.parse_args()
    if args.max_events < 1:
        parser.error('--max-events must be positive')
    cache = LogCache(args.directory, args.log, args.max_events)
    with ThreadingHTTPServer((args.host, args.port), handler_for(cache)) as server:
        print(f'Click viewer: http://{args.host}:{server.server_port}', flush=True)
        print(f'Log: {cache.locate()} (read only; appends refresh automatically)', flush=True)
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass


if __name__ == '__main__':
    main()
