import json
from pathlib import Path
import random
import tempfile
import unittest
from unittest.mock import patch
import contextlib
import io
from fit_click_profiles import load, best, fit, Session, simulate, summary, session_metadata, validate, main


class ProfileFitTest(unittest.TestCase):
    def test_native_integer_subtraction_and_resets_exclude_other_sources(self):
        with tempfile.TemporaryDirectory() as tmp:
            path=Path(tmp)/'click-debug.jsonl'
            base=9_007_199_254_740_999
            rows=[]
            for origin in [base,10]:
                for i in range(31):
                    rows.append(dict(session='one',source='physical',method='mouse',side='left',action='press',
                                     native_event_ns_text=str(origin+i*100_000_000)))
            rows += [dict(rows[0],source='artificial'),dict(rows[0],action='release'),dict(rows[0],side='mouse_4')]
            path.write_text('\n'.join(map(json.dumps,rows)))
            bouts,meta=load(path)
            self.assertEqual(2,len(bouts['left']))
            self.assertEqual(.1,bouts['left'][0][1])
            self.assertEqual(1,meta['clock_resets'])
            self.assertEqual(62,meta['physical_presses']['left'])

    def test_top_sixty_percent_preserves_whole_bouts_and_internal_dips(self):
        bouts=[[i/rate for i in range(41)] for rate in [5,6,7,8,9]]
        retained=best(bouts)
        self.assertEqual(3,len(retained))
        self.assertEqual([bouts[4],bouts[3],bouts[2]],retained)

    def test_model_contains_only_aggregates_and_both_sides_are_finite(self):
        model=json.loads((Path(__file__).resolve().parents[1]/'src/main/resources/assets/orvenbw/click-profile-model.json').read_text())
        text=json.dumps(model)
        for prohibited in ['session','timestamp_ms','native_event_ns','sequence']:
            self.assertNotIn('"'+prohibited+'"',text)
        for side in ['left','right']:
            for profile in [False,True]:
                session=Session(model['sides'][side],random.Random(5),profile)
                for i in range(100):
                    self.assertTrue(1<=session.target(i/10)<=22)
                    self.assertGreater(session.interval(i/10),0)

    def test_sparse_state_fitting_still_produces_usable_distributions(self):
        bouts=[[i/6 for i in range(31)] for _ in range(5)]
        model=fit(bouts,'right')
        result=simulate(model,[5]*20,22)
        self.assertTrue(result)
        self.assertTrue(4<=summary(result)['cps_percentiles'][2]<=8)

    def test_session_order_is_chronological_and_newest_is_not_merged(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'log.jsonl'
            rows=[]
            for sid,start,count in [('new',200,2),('old',100,31)]:
                rows.append(dict(event='session_start',session=sid,timestamp_ms=start,mod_version=sid))
                for i in range(count):
                    rows.append(dict(session=sid,source='physical',method='mouse',side='left',action='press',native_event_ns_text=str(i*100_000_000)))
            path.write_text('\n'.join(map(json.dumps,rows)))
            sessions=session_metadata(path)
            self.assertEqual(['old','new'],[r['session'] for r in sessions])
            self.assertEqual(2,sessions[-1]['physical_presses'])
            latest,meta=load(path,{'new'})
            self.assertFalse(latest['left']); self.assertEqual(2,meta['physical_presses']['left'])
            older,meta=load(path,{'old'})
            self.assertEqual(['old'],meta['bout_sessions']['left']); self.assertEqual(1,len(older['left']))

    def test_observed_clock_retains_batched_intervals_but_planned_time_is_not_fabricated(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'log.jsonl'
            rows=[dict(session='one',source='artificial',method='spam_click',side='left',action='queue',
                       monotonic_ns_text=str(9_007_199_254_740_999+(i//2)*100_000_000)) for i in range(62)]
            path.write_text('\n'.join(map(json.dumps,rows)))
            observed,meta=load(path,clock='observed',source='artificial')
            self.assertEqual(0,observed['left'][0][1]); self.assertEqual(.1,observed['left'][0][2])
            planned,meta=load(path,clock='planned',source='artificial')
            self.assertFalse(planned['left']); self.assertEqual(62,meta['missing_timestamps']); self.assertEqual(0,meta['missing_native'])
            with self.assertRaises(ValueError): load(path,source='artificial')

    def test_joint_startup_uses_one_complete_aggregate_trajectory(self):
        model=fit([[i/10 for i in range(41)] for _ in range(5)],'left')
        expected=dict(rate=12,ratios=[.4,.6,.8,1.0,1.2,1.4])
        model['startup_weights']=[1,0,0]; model['startup_trajectories']=[[expected],[],[]]
        session=Session(model,random.Random(4))
        self.assertEqual(12,session.start_rate); self.assertEqual(expected['ratios']+[1],session.startup)
        self.assertTrue(all(len(t['ratios'])==6 for mode in fit([[i/10 for i in range(41)]],'left')['startup_trajectories'] for t in mode))

    def test_validation_holds_out_complete_sessions(self):
        bouts=[[i/10 for i in range(41)] for _ in range(9)]
        result=validate(bouts,'left',['a']*3+['b']*3+['c']*3)
        self.assertEqual(['a','b','c'],[f['held_out_session'] for f in result['folds']])
        self.assertTrue(all(f['training_bouts']==4 and f['held_out_bouts']==2 for f in result['folds']))

    def test_insufficient_newest_cohort_never_overwrites_a_bundled_model(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            rows=[dict(event='session_start',session='new',timestamp_ms=200,mod_version='0.6.1')]
            rows.extend(dict(session='new',source='physical',method='mouse',side='left',action='press',native_event_ns_text=str(i*1_000_000_000)) for i in range(2))
            for name in ('wren','liwwyy'):
                (root/name).mkdir(); (root/name/'click-debug.jsonl').write_text('\n'.join(map(json.dumps,rows)))
            model=root/'model.json'; model.write_text('existing model')
            report=root/'report.json'
            with patch('sys.argv',['fit','--samples-directory',str(root),'--wren-cohort','newest','--output',str(report),'--model-output',str(model)]), contextlib.redirect_stdout(io.StringIO()): main()
            self.assertEqual('existing model',model.read_text())
            result=json.loads(report.read_text())
            self.assertFalse(result['candidate_accepted'])
            self.assertEqual(2,result['sessions'][0]['physical_presses'])
            self.assertTrue(result['cohorts']['newest']['sides']['left']['insufficient'])


if __name__=='__main__':
    unittest.main()
