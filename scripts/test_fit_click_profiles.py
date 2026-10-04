import json
from pathlib import Path
import random
import tempfile
import unittest
from fit_click_profiles import load, best, fit, Session, simulate, summary


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


if __name__=='__main__':
    unittest.main()
