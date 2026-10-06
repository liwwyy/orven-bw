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

DEFAULT_DIRECTORY = Path('/home/user/Projects/orven-bw/click_logs')
ASSETS = Path(__file__).resolve().parent / 'click_debug'


class LogCache:
    def __init__(self, directory=DEFAULT_DIRECTORY, log=None, limit=20000):
        self.directory, self.explicit, self.limit = Path(directory), Path(log) if log else None, limit
        self.lock = threading.Lock()
        self.reset()

    def reset(self):
        self.rows = deque(maxlen=self.limit)
        self.sessions = {}
        self.clocks = {}
        self.identity, self.prefix, self.position = None, b'', 0
        self.seen = self.malformed = 0

    def annotate(self, row):
        if row.get('source') == 'artificial' and row.get('action') == 'queue':
            try:
                elapsed = int(row['intended_elapsed_ns_text']) / 1e6
                lateness = int(row['dispatch_lateness_ns_text']) / 1e6
                if not math.isfinite(elapsed) or not math.isfinite(lateness) or lateness < 0:
                    raise ValueError('Invalid intended timestamp')
                row['_intended_elapsed_ms'] = elapsed
                row['_intended_epoch_ms'] = row['timestamp_ms'] - lateness
                row['_dispatch_lateness_ms'] = lateness
            except (ValueError, TypeError, KeyError, OverflowError):
                pass
        # Native clocks have no absolute origin. Anchor differences within one session.
        if row.get('event') != 'input' or row.get('source') != 'physical' or row.get('method') != 'mouse' or row.get('action') not in ('press', 'release'):
            return row
        session = row['session']
        state = self.clocks.setdefault(session, dict(segment=0, anchor=None, last=None))
        try:
            raw = row.get('native_event_ns_text', row.get('native_event_ns'))
            if isinstance(raw, bool) or not isinstance(raw, (str, int)):
                raise ValueError('No native timestamp')
            native = int(raw)
            if native < 0:
                raise ValueError('Negative native timestamp')
        except (ValueError, TypeError):
            state['segment'] += 1
            state['anchor'] = state['last'] = None
            row['_timing_fallback'] = True
            row['_native_segment'] = f"fallback-{state['segment']}"
            return row
        if state['last'] is not None and native < state['last']:
            state['segment'] += 1
            state['anchor'] = None
        if state['anchor'] is None:
            observed = row.get('elapsed_ns', 0)
            elapsed = observed / 1e6 if isinstance(observed, (int, float)) and math.isfinite(observed) else 0
            state['anchor'] = (native, elapsed, row['timestamp_ms'])
        base, elapsed, epoch = state['anchor']
        delta = (native - base) / 1e6  # Subtract Python integers before converting to milliseconds.
        row['_native_elapsed_ms'] = elapsed + delta
        row['_native_epoch_ms'] = epoch + delta
        row['_native_segment'] = state['segment']
        row['_timing_fallback'] = False
        state['last'] = native
        return row

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
                            if row.get('event') == 'session_start' and isinstance(row.get('session'), str):
                                previous = self.sessions.setdefault(row['session'], {})
                                previous.update(row)
                                previous.setdefault('click_count', 0)
                            if row.get('event') in ('input', 'action', 'packet'):
                                if not isinstance(row.get('timestamp_ms'), (int, float)) or not math.isfinite(row['timestamp_ms']) or not isinstance(row.get('session'), str):
                                    raise ValueError('Missing timestamp/session')
                                info = self.sessions.setdefault(row['session'], dict(timestamp_ms=row['timestamp_ms'], mod_version='unknown', click_count=0))
                                if row.get('event') == 'input' and (row.get('action') == 'press' or row.get('source') == 'artificial' and row.get('action') == 'queue'):
                                    info['click_count'] = info.get('click_count', 0) + 1
                                self.rows.append(self.annotate(row))
                                self.seen += 1
                        except (ValueError, UnicodeDecodeError):
                            self.malformed += 1
                    self.position = file.tell()
            except FileNotFoundError:
                self.reset()
            except OSError as exception:
                error = str(exception)
            return dict(events=list(self.rows), path=str(path), seen=self.seen,
                        retained=len(self.rows), malformed=self.malformed, error=error, sessions={sid:info.copy() for sid,info in self.sessions.items()},
                        session_order=sorted(self.sessions, key=lambda sid: (self.sessions[sid].get('timestamp_ms', 0), sid)))


def handler_for(cache, samples=None):
    samples = samples or {}
    pages = {f'/{name}': sample for name, sample in samples.items()}
    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            route = urlsplit(self.path).path
            page = route.rstrip('/')
            sample = pages.get(page.removesuffix('/api/events'))
            if route == '/api/events' or (sample is not None and page.endswith('/api/events')):
                body = json.dumps((sample if sample is not None else cache).snapshot(), allow_nan=False).encode()
                content_type = 'application/json; charset=utf-8'
            elif route in ('/', '/viewer.js') or page in pages:
                body = (ASSETS / ('viewer.js' if route == '/viewer.js' else 'index.html')).read_bytes()
                content_type = 'text/javascript; charset=utf-8' if route == '/viewer.js' else 'text/html; charset=utf-8'
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
    samples = {}
    if not args.log and args.directory.is_dir():
        for directory in sorted(args.directory.iterdir()):
            if directory.is_dir() and directory.name.replace('-', '').replace('_', '').isalnum():
                candidate = LogCache(directory, limit=args.max_events)
                if candidate.locate().is_file():
                    samples[directory.name] = candidate
    if samples:
        cache = next(iter(samples.values()))
    with ThreadingHTTPServer((args.host, args.port), handler_for(cache, samples)) as server:
        print(f'Click viewer: http://{args.host}:{server.server_port}', flush=True)
        print(f'Log: {cache.locate()} (read only; appends refresh automatically)', flush=True)
        for name, sample in samples.items():
            print(f'http://{args.host}:{server.server_port}/{name} → {sample.locate()}', flush=True)
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass


if __name__ == '__main__':
    main()
