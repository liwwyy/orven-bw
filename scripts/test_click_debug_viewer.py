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

    def test_native_batched_events_keep_exact_deltas_and_recording_identity(self):
        self.append(dict(event='session_start', session='one', build_flavor='recording', mod_version='0.2.1+mc1.8.9'))
        base = 1_800_000_000_000_123_457
        for delta in (0, 5_000_000, 22_000_000):
            row = event(100)
            row['native_event_ns_text'] = str(base + delta)
            row['native_event_ns'] = base + delta
            self.append(row)
        result = self.cache.snapshot()
        self.assertEqual('recording', result['sessions']['one']['build_flavor'])
        self.assertEqual([100, 105, 122], [r['_native_elapsed_ms'] for r in result['events']])
        self.assertEqual([100, 105, 122], [r['_native_epoch_ms'] for r in result['events']])
        self.assertEqual(1, len({r['_native_segment'] for r in result['events']}))

    def test_native_legacy_numbers_and_clock_resets_do_not_bridge_intervals(self):
        for native in (100_000_000, 105_000_000, 2_000_000):
            row = event(50)
            row['native_event_ns'] = native
            self.append(row)
        rows = self.cache.snapshot()['events']
        self.assertEqual([50, 55, 50], [r['_native_elapsed_ms'] for r in rows])
        self.assertNotEqual(rows[1]['_native_segment'], rows[2]['_native_segment'])
        self.append(event(60))
        fallback = self.cache.snapshot()['events'][-1]
        self.assertTrue(fallback['_timing_fallback'])
        row = event(70); row['native_event_ns'] = 10_000_000; self.append(row)
        fresh = self.cache.snapshot()['events'][-1]
        self.assertEqual(70, fresh['_native_elapsed_ms'])
        self.assertNotEqual(fallback['_native_segment'], fresh['_native_segment'])

    def test_native_clock_state_survives_retention_and_resets_on_clear(self):
        for i in range(6):
            row = event(100); row['native_event_ns_text'] = str(1_000_000 + i * 1_000_000)
            self.append(row)
        result = self.cache.snapshot()
        self.assertEqual([103,104,105], [r['_native_elapsed_ms'] for r in result['events']])
        self.path.write_text('')
        self.assertEqual({}, self.cache.snapshot()['sessions'])
        row = event(1, 'two'); row['native_event_ns_text'] = '1000000'; self.append(row)
        self.assertEqual(1, self.cache.snapshot()['events'][0]['_native_elapsed_ms'])

    def test_intended_deadlines_are_exact_and_never_native_mouse_events(self):
        row=event(100)
        row.update(source='artificial', method='spam_click', action='queue',
                   intended_elapsed_ns_text='75000123', dispatch_lateness_ns_text='25000000')
        self.append(row)
        result=self.cache.snapshot()['events'][0]
        self.assertAlmostEqual(75.000123,result['_intended_elapsed_ms'])
        self.assertEqual(25,result['_dispatch_lateness_ms'])
        self.assertNotIn('_native_elapsed_ms',result)
        row['dispatch_lateness_ns_text']='invalid'
        self.append(row)
        self.assertNotIn('_intended_elapsed_ms',self.cache.snapshot()['events'][-1])

    def test_action_packet_and_legacy_input_rows_share_one_viewer(self):
        self.append(event(10))
        self.append(dict(event='action', session='one', timestamp_ms=11, elapsed_ns=11_000_000,
                         source='physical', method='mouse', side='left', action='invoke',
                         origin_id=1, action_id=2, monotonic_ns_text='1234567890123456789'))
        self.append(dict(event='packet', session='one', timestamp_ms=12, elapsed_ns=12_000_000,
                         source='physical', method='mouse', side='left', action='send',
                         origin_id=1, action_id=2, packet_kind='entity_attack'))
        rows = self.cache.snapshot()['events']
        self.assertEqual(['input', 'action', 'packet'], [r['event'] for r in rows])
        self.assertNotIn('_native_elapsed_ms', rows[1])
        self.assertEqual(rows[1]['action_id'], rows[2]['action_id'])

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
            page = response.read()
            self.assertIn(b'/viewer.js', page)
            self.assertIn(b'value="action"', page)
            self.assertIn(b'value="packet"', page)
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

    def test_session_cohorts_follow_start_dates_and_counts_survive_retention(self):
        self.append(dict(event='session_start',session='new',timestamp_ms=200,mod_version='0.6.1'))
        self.append(event(201,'new')); self.append(event(202,'new'))
        self.append(dict(event='session_start',session='old',timestamp_ms=100,mod_version='0.2.1'))
        for i in range(5): self.append(event(100+i,'old'))
        result=self.cache.snapshot()
        self.assertEqual(['old','new'],result['session_order'])
        self.assertEqual(2,result['sessions']['new']['click_count'])
        self.assertEqual(5,result['sessions']['old']['click_count'])
        self.assertEqual('0.6.1',result['sessions']['new']['mod_version'])
        self.assertEqual(3,result['retained'])


if __name__ == '__main__':
    unittest.main()
