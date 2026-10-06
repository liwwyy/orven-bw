#!/usr/bin/env python3
"""Fit aggregate click profiles from local logs; raw events are never embedded in models.

python3 scripts/fit_click_profiles.py --output .reference/click-profile-report.json \
    --model-output src/main/resources/assets/orvenbw/click-profile-model.json
"""
import argparse
import bisect
from collections import Counter, defaultdict
import hashlib
import json
import math
from pathlib import Path
import random
import statistics as stats

ROOT = Path(__file__).resolve().parents[1]
PROBABILITIES = [0, .05, .1, .25, .5, .75, .9, .95, 1]


def quantile(values, p):
    values = sorted(values)
    if not values:
        raise ValueError('Cannot fit an empty distribution')
    index = (len(values) - 1) * p
    lo = int(index)
    return values[lo] + (values[min(lo + 1, len(values) - 1)] - values[lo]) * (index - lo)


def distribution(values, fallback):
    return [round(quantile(values or fallback, p), 9) for p in PROBABILITIES]


def draw(rng, values):
    u = rng.random()
    i = min(len(PROBABILITIES) - 2, bisect.bisect_right(PROBABILITIES, u) - 1)
    f = (u - PROBABILITIES[i]) / (PROBABILITIES[i + 1] - PROBABILITIES[i])
    return values[i] + f * (values[i + 1] - values[i])


def weights(counts, prior=0.5):
    values = [x + prior for x in counts]
    total = sum(values)
    return [x / total for x in values] if total else [1 / len(values)] * len(values)


def choose(rng, values):
    value = rng.random() * sum(values)
    for i, weight in enumerate(values):
        value -= weight
        if weight > 0 and value <= 0:
            return i
    return len(values) - 1


def session_metadata(path):
    sessions = {}
    for line in path.read_text().splitlines():
        try:
            row = json.loads(line)
            sid = row['session']
            info = sessions.setdefault(sid, dict(session=sid, timestamp_ms=row.get('timestamp_ms', 0),
                                                  mod_version='unknown', physical_presses=0))
            if row.get('event') == 'session_start':
                info.update(timestamp_ms=row.get('timestamp_ms', info['timestamp_ms']),
                            mod_version=row.get('mod_version', 'unknown'))
            if (row.get('source'), row.get('method'), row.get('action')) == ('physical', 'mouse', 'press') and row.get('side') in ('left', 'right'):
                info['physical_presses'] += 1
        except (ValueError, KeyError, TypeError):
            continue
    return sorted(sessions.values(), key=lambda r: (r['timestamp_ms'], r['session']))


def load(path, sessions=None, clock='native', source='physical'):
    if clock == 'native' and source != 'physical':
        raise ValueError('Native mouse time is only available for physical input')
    groups = defaultdict(list)
    malformed = missing = resets = 0
    for line in path.read_text().splitlines():
        try:
            row = json.loads(line)
            if sessions is not None and row.get('session') not in sessions:
                continue
            physical = (row.get('source'), row.get('method'), row.get('action')) == ('physical', 'mouse', 'press')
            artificial = row.get('source') == 'artificial' and row.get('action') == 'queue' and row.get('method') in ('spam_click', 'mouse_hold_click', 'cps_boost')
            if not ((source in ('physical', 'combined') and physical) or (source in ('artificial', 'combined') and artificial)):
                continue
            if row.get('side') not in ('left', 'right'):
                continue
            if clock == 'planned':
                raw = row.get('intended_elapsed_ns_text')
            else:
                key = 'native_event_ns' if clock == 'native' else 'monotonic_ns'
                raw = row.get(key + '_text', row.get(key))
            if raw is None:
                missing += 1
                continue
            groups[(row['session'], row['side'])].append(int(raw))
        except (ValueError, KeyError, TypeError):
            malformed += 1
    sides = {'left': [], 'right': []}
    bout_sessions = {'left': [], 'right': []}
    counts = Counter()
    for (sid, side), times in groups.items():
        counts[side] += len(times)
        # Observed synthetic and physical timestamps can legitimately coincide.
        if clock != 'native': times.sort()
        bout = []
        def finish():
            if len(bout) >= 10 and bout[-1] - bout[0] >= 2_000_000_000:
                sides[side].append([(x - bout[0]) / 1e9 for x in bout])
                bout_sessions[side].append(sid)
        for t in times:
            if bout and (t < bout[-1] or clock == 'native' and t == bout[-1] or t - bout[-1] > 750_000_000):
                resets += t <= bout[-1]
                finish(); bout = []
            bout.append(t)
        if bout: finish()
    return sides, dict(physical_presses=dict(counts) if source == 'physical' else {}, click_counts=dict(counts),
                      malformed=malformed, missing_native=missing if clock == 'native' else 0, missing_timestamps=missing,
                      clock_resets=resets, bout_sessions=bout_sessions, clock=clock, source=source,
                      sha256=hashlib.sha256(path.read_bytes()).hexdigest())


def best(bouts):
    return sorted(bouts, key=lambda b: (len(b) - 1) / b[-1], reverse=True)[:math.ceil(.6 * len(bouts))]


def rolling(bout):
    return [(i / 10, bisect.bisect_left(bout, i / 10 + 1) - bisect.bisect_left(bout, i / 10))
            for i in range(int((bout[-1] - 1) * 10) + 1)]


def state_for(rate, side):
    return 0 if rate <= (8 if side == 'left' else 4) else 2 if rate >= (18 if side == 'left' else 11) else 1


def category(interval):
    return 0 if interval < .045 else 1 if interval < .150 else 2


def fit(bouts, side):
    if not bouts:
        raise ValueError(f'No eligible {side} bouts')
    rates = [[] for _ in range(3)]
    durations = [[] for _ in range(3)]
    transitions = [[0] * 3 for _ in range(3)]
    initial = [0] * 3
    interval_values = [[[] for _ in range(3)] for _ in range(3)]
    category_counts = [[0] * 3 for _ in range(3)]
    category_transitions = [[[0] * 3 for _ in range(3)] for _ in range(3)]
    startup_weights = [0] * 3
    startup_ratios = [[[] for _ in range(6)] for _ in range(3)]
    startup_rates = [[], [], []]
    startup_trajectories = [[], [], []]
    all_intervals = []
    for b in bouts:
        windows = rolling(b)
        states = [state_for(c, side) for _, c in windows]
        initial[states[0]] += 1
        start = 0
        for i, (_, c) in enumerate(windows):
            rates[states[i]].append(c)
            if i and states[i] != states[i - 1]:
                durations[states[i - 1]].append((i - start) / 10)
                transitions[states[i - 1]][states[i]] += 1
                start = i
        durations[states[-1]].append((len(states) - start) / 10)
        previous_category = None
        for a, t in zip(b, b[1:]):
            dt = t - a
            all_intervals.append(dt)
            window_index = min(len(states) - 1, max(0, round((a - .5) * 10)))
            state = states[window_index]
            cat = category(dt)
            interval_values[state][cat].append(dt)
            category_counts[state][cat] += 1
            if previous_category is not None:
                category_transitions[state][previous_category][cat] += 1
            previous_category = cat
        if b[-1] >= 3:
            baseline = sum(1 <= t < 3 for t in b) / 2
            if baseline > 0:
                ratio = sum(t < 1 for t in b) / baseline
                mode = 0 if ratio < .9 else 2 if ratio > 1.1 else 1
                startup_weights[mode] += 1
                startup_rates[mode].append(baseline)
                trajectory = []
                for k in range(6):
                    clicks = sum(k / 4 <= t < (k + 1) / 4 for t in b) - (1 if k == 0 else 0)
                    value = max(.25, 4 * clicks / baseline)
                    startup_ratios[mode][k].append(value); trajectory.append(value)
                startup_trajectories[mode].append(dict(rate=baseline, ratios=trajectory))
    med = quantile([c for b in bouts for _, c in rolling(b)], .5)
    models = []
    pooled_cats = [[dt for dt in all_intervals if category(dt) == cat] for cat in range(3)]
    for state in range(3):
        all_state_intervals = [dt for ds in interval_values[state] for dt in ds]
        mean_interval = stats.mean(all_state_intervals or all_intervals)
        state_rate = 1 / mean_interval
        # Use empirical state mean so emission rhythm and tempo do not independently double-vary CPS.
        models.append(dict(rate=state_rate,
                           durations=distribution(durations[state], [.3, .6, 1.2]),
                           transitions=weights([x if i != state else 0 for i, x in enumerate(transitions[state])], 0),
                           categories=weights(category_counts[state], 0),
                           category_transitions=[weights(row, .25) for row in category_transitions[state]],
                           intervals=[distribution(interval_values[state][cat], pooled_cats[cat] or [mean_interval])
                                      for cat in range(3)]))
        if sum(models[-1]['transitions']) == 0:
            models[-1]['transitions'] = [0, 1, 0] if state != 1 else [.5, 0, .5]
    return dict(median_cps=med, initial=weights(initial), states=models,
                startup_weights=weights(startup_weights, 0),
                startup_rates=[distribution(values, [med]) for values in startup_rates],
                startup_ratios=[[distribution(ds, [1]) for ds in mode] for mode in startup_ratios],
                startup_trajectories=startup_trajectories, rare_peak=side == 'left')


def correlation(bouts):
    a, b = [], []
    for ts in bouts:
        ds = [y - x for x, y in zip(ts, ts[1:])]
        a += ds[:-1]; b += ds[1:]
    if len(a) < 3:
        return 0
    ma, mb = stats.mean(a), stats.mean(b)
    denominator = math.sqrt(sum((x-ma)**2 for x in a) * sum((y-mb)**2 for y in b))
    return sum((x-ma)*(y-mb) for x,y in zip(a,b)) / denominator if denominator else 0


def startup_summary(bouts):
    counts=[0,0,0]
    for b in bouts:
        if b[-1]<3:continue
        baseline=sum(1<=t<3 for t in b)/2
        if baseline<=0:continue
        ratio=sum(t<1 for t in b)/baseline
        counts[0 if ratio<.9 else 2 if ratio>1.1 else 1]+=1
    total=sum(counts)
    intervals=[]
    for count in counts:
        if not total:
            intervals.append([0,1]);continue
        p=count/total;z=1.96;den=1+z*z/total
        center=(p+z*z/(2*total))/den
        width=z*math.sqrt(p*(1-p)/total+z*z/(4*total*total))/den
        intervals.append([center-width,center+width])
    return dict(counts=counts,shares=[n/total if total else 0 for n in counts],wilson_95=intervals)


def run_durations(bouts, side, burst):
    runs=[]
    for b in bouts:
        length=0
        for _,c in rolling(b)+[(None,None)]:
            selected=c is not None and state_for(c,side)==(2 if burst else 0)
            if selected:length+=1
            elif length:runs.append(length/10);length=0
    return [quantile(runs,p) for p in [.05,.5,.95]] if runs else []


def summary(bouts):
    ds = [y-x for b in bouts for x,y in zip(b,b[1:])]
    cps = [c for b in bouts for _,c in rolling(b)]
    return dict(bouts=len(bouts), presses=sum(map(len,bouts)), active_seconds=sum(b[-1] for b in bouts),
                cps_percentiles=[quantile(cps,p) for p in [.05,.25,.5,.75,.95]],
                interval_ms_percentiles=[1000*quantile(ds,p) for p in [.05,.25,.5,.75,.95]],
                interval_categories=[sum(category(d)==i for d in ds)/len(ds) for i in range(3)],
                interval_correlation=correlation(bouts), startup=startup_summary(bouts),
                sub_1ms_share=sum(d<.001 for d in ds)/len(ds),
                near_50ms_grid_share=sum(abs(d/.05-round(d/.05))<.08 for d in ds)/len(ds))


class Session:
    """Reference implementation mirrored by ClickProfileSession; deadlines stay continuous."""
    def __init__(self, model, rng, performative=False, multiplier=1, ceiling=22, rare=True):
        self.model, self.rng = model, rng
        self.performative, self.multiplier, self.ceiling = performative, multiplier, ceiling
        self.state = choose(rng, model['initial']); self.cat = -1
        self.transition_at=0; self.transition_from=model['states'][self.state]['rate']
        self.until = draw(rng, model['states'][self.state]['durations'])
        mode = choose(rng, model['startup_weights'])
        trajectories = model.get('startup_trajectories', [[], [], []])[mode]
        if trajectories:
            trajectory = trajectories[int(rng.random()*len(trajectories))]
            self.startup = [max(.25, min(2, value)) for value in trajectory['ratios']] + [1]
            self.start_rate = trajectory['rate']
        else:
            self.startup = [max(.25, min(2, draw(rng,q))) for q in model['startup_ratios'][mode]] + [1]
            self.start_rate = draw(rng, model['startup_rates'][mode])
        self.rare = rare and model['rare_peak'] and not performative
        self.peak_at = 30 - 90*math.log(max(1e-12, 1-rng.random()))
        self.peak_until = 0

    def target(self, t):
        while t >= self.until:
            self.transition_at=self.until; self.transition_from=self.model['states'][self.state]['rate']
            self.state = choose(self.rng, self.model['states'][self.state]['transitions'])
            self.until += max(.1, draw(self.rng, self.model['states'][self.state]['durations']))
        blend=max(0,min(1,(t-self.transition_at)/.1))
        rate=self.transition_from+(self.model['states'][self.state]['rate']-self.transition_from)*blend
        if t < 1.5:
            index = min(5, int(t*4)); f=t*4-index
            endpoint=rate if index==5 else self.start_rate*self.startup[index+1]
            rate=self.start_rate*self.startup[index]*(1-f)+endpoint*f
        if self.performative:
            rate = self.model['median_cps']+.35*(rate-self.model['median_cps'])
        if self.rare and t >= self.peak_at:
            self.peak_until=t+.2+self.rng.random()*.2
            self.peak_rate=21+self.rng.random()
            self.peak_at=self.peak_until+30-90*math.log(max(1e-12,1-self.rng.random()))
        if self.rare and t < self.peak_until:
            rate = self.peak_rate
        return max(1, min(self.ceiling,rate)*self.multiplier)

    def interval(self, t):
        rate = self.target(t)
        state = self.model['states'][self.state]
        self.cat = choose(self.rng, state['categories'] if self.cat < 0 else state['category_transitions'][self.cat])
        return max(.008, min(1.5, draw(self.rng, state['intervals'][self.cat])*state['rate']/rate))


def simulate(model, durations, seed, performative=False):
    rng=random.Random(seed); result=[]
    for duration in durations:
        session=Session(model,rng,performative,rare=False)
        b=[0.0]
        while True:
            t=b[-1]+session.interval(b[-1])
            if t>=duration:break
            b.append(t)
        if len(b)>2:result.append(b)
    return result


def simulate_queued(model, durations, seed, performative=False):
    """Mirror the 20-Hz deadline scheduler; return intended and actual queue traces separately."""
    rng=random.Random(seed); intended_bouts=[]; queued_bouts=[]
    for duration in durations:
        session=Session(model,rng,performative,rare=False)
        due=0.0; previous_rate=None; intended=[]; queued=[]
        for i in range(math.ceil(duration*20)):
            now=i/20; rate=session.target(now)
            if previous_rate is not None and due>now:
                due=now+(due-now)*previous_rate/rate
            previous_rate=rate
            count=0
            while now>=due-1e-12 and count<2:
                deadline=due; due+=session.interval(now)
                if sum(t>now-1 for t in queued)>=22:break
                intended.append(deadline);queued.append(now);count+=1
            if now>=due:due=now+session.interval(now)
        if len(intended)>2:
            intended_bouts.append(intended);queued_bouts.append(queued)
    return intended_bouts,queued_bouts


def validate(bouts, side, session_ids=None):
    reference, generated=[],[]
    folds=[]
    ids = sorted(set(session_ids or []))
    for fold, held_out in enumerate(ids or range(5)):
        if ids:
            train = best([b for b, sid in zip(bouts, session_ids) if sid != held_out])
            test = best([b for b, sid in zip(bouts, session_ids) if sid == held_out])
        else:
            train = [b for i, b in enumerate(bouts) if i % 5 != fold]
            test = [b for i, b in enumerate(bouts) if i % 5 == fold]
        if not train or not test: continue
        model = fit(train, side)
        sim = simulate(model, [b[-1] for b in test]*24, 1000+fold)
        reference += test; generated += sim
        folds.append(dict(fold=fold, held_out_session=held_out if ids else None,
                          training_bouts=len(train), held_out_bouts=len(test)))
    if not reference or not generated:
        return dict(folds=folds, acceptance=dict(sufficient_sessions=False))
    real, sim=summary(reference),summary(generated)
    errors=dict(cps_max_percentile_error=max(abs(a-b) for a,b in zip(real['cps_percentiles'],sim['cps_percentiles'])),
                category_max_share_error=max(abs(a-b) for a,b in zip(real['interval_categories'],sim['interval_categories'])),
                correlation_error=abs(real['interval_correlation']-sim['interval_correlation']))
    limits=dict(cps_max_percentile_error=2,category_max_share_error=.05,correlation_error=.15)
    startup_ok=all(lo<=p<=hi for p,(lo,hi) in zip(sim['startup']['shares'],real['startup']['wilson_95']))
    return dict(folds=folds,reference=real,generated=sim,errors=errors,
                burst_duration_seconds=dict(reference=run_durations(reference,side,True),generated=run_durations(generated,side,True)),
                slower_duration_seconds=dict(reference=run_durations(reference,side,False),generated=run_durations(generated,side,False)),
                acceptance={**{key: errors[key]<=limit for key,limit in limits.items()}, 'startup_within_sample_uncertainty': startup_ok})


def optional_summary(bouts, side=None):
    if not bouts: return dict(bouts=0, insufficient=True)
    result = summary(bouts)
    if side:
        result['burst_duration_seconds'] = run_durations(bouts, side, True)
        result['dip_duration_seconds'] = run_durations(bouts, side, False)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--samples-directory', type=Path, default=ROOT/'click_logs')
    parser.add_argument('--output', type=Path, default=ROOT/'.reference/click-profile-report.json')
    parser.add_argument('--model-output', type=Path)
    parser.add_argument('--wren-cohort', choices=['older', 'newest', 'all'], default='older')
    args = parser.parse_args()
    wp, lp = args.samples_directory/'wren/click-debug.jsonl', args.samples_directory/'liwwyy/click-debug.jsonl'
    sessions = session_metadata(wp)
    newest = {sessions[-1]['session']} if sessions else set()
    older = {r['session'] for r in sessions[:-1]}
    cohort = older if args.wren_cohort == 'older' else newest if args.wren_cohort == 'newest' else None
    wren, wm = load(wp, cohort)
    selected = {side:best(bouts) for side, bouts in wren.items()}
    validation = {side:validate(wren[side], side, wm['bout_sessions'][side]) for side in selected}
    model = dict(schema=2, probabilities=PROBABILITIES,
                 selection=dict(fraction=.6, gap_ms=750, min_seconds=2, min_presses=10),
                 sides={side:fit(bouts, side) for side, bouts in selected.items()}) if all(selected.values()) else None
    comparisons = {}
    for name, path, ids, clock, source in [('wren_native',wp,cohort,'native','physical'),
            ('wren_observed',wp,cohort,'observed','physical'), ('liwwyy_artificial_planned',lp,None,'planned','artificial'),
            ('liwwyy_artificial_observed',lp,None,'observed','artificial'), ('liwwyy_combined_observed',lp,None,'observed','combined')]:
        bouts, meta = load(path, ids, clock, source)
        comparisons[name] = dict(metadata=meta, sides={side:optional_summary(b, side) for side,b in bouts.items()})
    cohorts = {}
    for name, ids in [('older',older),('newest',newest)]:
        bouts,meta = load(wp, ids)
        cohorts[name] = dict(metadata=meta, sides={side:optional_summary(b, side) for side,b in bouts.items()})
    passed = model is not None and all(all(v['acceptance'].values()) for v in validation.values())
    report = dict(sessions=sessions, cohorts=cohorts, selected_cohort=args.wren_cohort,
                  selection={side:dict(eligible=len(wren[side]), selected=optional_summary(b, side)) for side,b in selected.items()},
                  comparisons=comparisons, validation=validation, candidate_accepted=passed,
                  limitations=['The newest session may be too small to fit.', 'Historical artificial recordings are from older mod versions.',
                               'Native and intended timing differ from observed tick dispatch.', '22 CPS remains an extrapolation.'])
    if model:
        report['queued_simulation'] = {side:dict(zip(('intended','actual_queue'),
            [summary(b) for b in simulate_queued(model['sides'][side],[b[-1] for b in selected[side]]*24,99)])) for side in selected}
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(report,indent=2)+'\n')
    if args.model_output and passed:
        args.model_output.parent.mkdir(parents=True,exist_ok=True)
        args.model_output.write_text(json.dumps(model,indent=2)+'\n')
    for side,result in validation.items(): print(side,result.get('errors'),result['acceptance'])
    print('Candidate accepted:',passed,'; report:',args.output)
    if args.model_output and not passed: print('Existing model retained: validation did not pass.')


if __name__=='__main__':
    main()
