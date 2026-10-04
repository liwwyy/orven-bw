#!/usr/bin/env python3
import json
from pathlib import Path
import tempfile
import threading
import unittest
from urllib.request import urlopen
from http.server import ThreadingHTTPServer
from click_debug_viewer import LogCache, handler_for


def event(ms, session='one'):
    return dict(event='input', session=session, timestamp_ms=ms, elapsed_ns=ms*1000000,
                source='physical', method='mouse', side='left', action='press')


class ViewerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name)/'click-debug.jsonl'
        self.cache = LogCache(self.temp.name, limit=3)

    def append(self, row):
        with self.path.open('a') as file:
            file.write(json.dumps(row)+'\n')

    def test_missing_file_then_live_append_sessions_and_retention(self):
        self.assertEqual([], self.cache.snapshot()['events'])
        for i in range(4):
            self.append(event(i, 'two' if i>1 else 'one'))
        first = self.cache.snapshot()
        self.assertEqual(4, first['seen'])
        self.assertEqual([1,2,3], [r['timestamp_ms'] for r in first['events']])
        self.assertEqual(first['events'], self.cache.snapshot()['events'])
        self.append(event(4))
        self.assertEqual(5, self.cache.snapshot()['seen'])

    def test_partial_and_malformed_rows_and_clear_reset(self):
        self.path.write_text('{broken}\n'+json.dumps(event(1)))
        result = self.cache.snapshot()
        self.assertEqual(1, result['malformed']); self.assertEqual([], result['events'])
        with self.path.open('a') as file:
            file.write('\n')
        self.assertEqual(1, self.cache.snapshot()['seen'])
        self.path.write_text('')
        self.assertEqual([], self.cache.snapshot()['events'])
        self.append(event(2))
        self.assertEqual(1, self.cache.snapshot()['seen'])
        # Replacement can also be larger than the previous file; the prefix identifies it.
        self.path.write_text(json.dumps(event(99999, 'a_new_session'))+'\n')
        self.assertEqual(1, self.cache.snapshot()['seen'])
        self.assertEqual('a_new_session', self.cache.snapshot()['events'][0]['session'])

    def test_http_page_script_and_api_are_self_contained(self):
        self.append(event(123))
        other = self.path.parent / 'other.jsonl'
        other.write_text(json.dumps(event(456)) + '\n')
        second = LogCache(log=other)
        server = ThreadingHTTPServer(('127.0.0.1',0), handler_for(
            self.cache, {'liwwyy': self.cache, 'wren': second}))
        thread = threading.Thread(target=server.serve_forever, daemon=True); thread.start()
        self.addCleanup(server.server_close); self.addCleanup(server.shutdown)
        url = f'http://127.0.0.1:{server.server_port}'
        with urlopen(url+'/') as response:
            self.assertIn(b'/viewer.js', response.read())
        with urlopen(url+'/viewer.js') as response:
            self.assertIn(b'/api/events', response.read())
        with urlopen(url+'/api/events') as response:
            self.assertEqual(123, json.load(response)['events'][0]['timestamp_ms'])
        for name, expected in [('liwwyy', 123), ('wren', 456)]:
            for suffix in ['', '/']:
                with urlopen(url + '/' + name + suffix) as response:
                    self.assertIn(b'/viewer.js', response.read())
            with urlopen(url + '/' + name + '/api/events') as response:
                self.assertEqual(expected, json.load(response)['events'][0]['timestamp_ms'])


if __name__ == '__main__':
    unittest.main()
