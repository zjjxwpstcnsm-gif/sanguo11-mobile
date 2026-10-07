#!/usr/bin/env python3
"""Freeze observed owner-frame counters after normal/cold/full restoration.

Counter segments are observations; log messages do not provide a renderer ID
or an atomic frame / HWUI draw correspondence. Never turn them into causality.
"""
import argparse
import json
import pathlib
import re
from run_remaining_normal_media import ROOT, sha

COUNTERS=('beginAttempts','beginSkipped','gpuPreparationFrames','lifetimeSubmissions',
          'overlayDraws','overlayRejectedStaticSkipped','overlayCameraRedraws')


def parse(raw, allowed_pids):
    rows=[]
    for line in raw.splitlines():
        match=re.search(r'\s(\d+)\s+\1\s+I Sanguo3D\s*: snapshot=true',line)
        if not match or match[1] not in allowed_pids:continue
        session=re.search(r'\bsession=([^ :]+):(\d+):(\d+)',line)
        if not session:continue
        values={name:re.search(r'\b'+name+r'=(\d+)',line) for name in COUNTERS}
        if not all(values.values()):continue
        row=dict(pid=match[1],sessionId=session[1],generation=int(session[2]),revision=int(session[3]),
                 counters={name:int(value[1]) for name,value in values.items()},rawLine=line)
        c=row['counters'];assert c['beginSkipped']<=c['beginAttempts']
        assert c['lifetimeSubmissions']<=c['gpuPreparationFrames']<=c['beginAttempts']
        rows.append(row)
    groups=[];current={}
    for row in rows:
        key=row['pid'],row['sessionId'],row['generation'];prior=current.get(key)
        if prior is None or any(row['counters'][k]<prior['last']['counters'][k] for k in COUNTERS):
            group=dict(pid=key[0],sessionId=key[1],generation=key[2],first=row,last=row,samples=1)
            groups.append(group);current[key]=group
        else:prior['last']=row;prior['samples']+=1
    for group in groups:
        group['deltas']={k:group['last']['counters'][k]-group['first']['counters'][k] for k in COUNTERS}
        group['rendererIdentityProven']=False
    return rows,groups


def main():
    p=argparse.ArgumentParser();p.add_argument('--session',type=pathlib.Path,required=True)
    p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args()
    case=a.session.resolve();assert case.is_relative_to(ROOT/'out/session-a') and not a.output.exists()
    state=json.loads((case/'session.json').read_text())
    assert state['root']==str(ROOT) and state['serial']=='emulator-5554'
    assert state['stage']=='restored-verified' and state['passed'] and state['coldProcess']['passed'] and state['coldProcess']['differentPid']
    assert set(state['restoration'])=={'internal','external'} and all(r['exactRegularFileSha'] for r in state['restoration'].values())
    assert state['videoObservation']['exitCode']==state['workerObservation']['exitCode']==0
    assert all(sha(pathlib.Path(p))==h for p,h in state['apks'].items())
    allowed={state['coldProcess']['beforePid'],state['coldProcess']['afterPid']}
    rows,groups=parse((case/'logcat.txt').read_text(errors='replace'),allowed);assert rows
    assert any(row['counters']['overlayRejectedStaticSkipped']>0 for row in rows)
    turns=[line for line in (case/'logcat.txt').read_text(errors='replace').splitlines()
           if any(re.search(r'\s'+pid+r'\s+'+pid+r'\s+I Turn52\s*: TURN_END',line) for pid in allowed)]
    report=dict(actualCase=str(case),apks=state['apks'],observedCounterRows=len(rows),counterSegments=groups,
        rejectedOverlayRequestsActuallySkipped=True,normalColdAndEveryOriginalShaRestored=True,
        turnProfiles=turns,rawFiles={name:sha(case/name) for name in ('session.json','logcat.txt','evidence/result.txt')},
        scope='Same actual target PID/token-generation monotonic segments only, not unique renderer identity, synchronized Native/HWUI frames, controlled old/new A/B, Java allocation/GPU/unique long-turn root or ARM. Functional correctness still requires actual screenshots/gesture/state/effect evidence.',
        uniqueCauseProven=False,foregroundLatencyAccepted=False,memoryBudgetClosed=False,wholeGoalComplete=False)
    a.output.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({k:report[k] for k in ('observedCounterRows','rejectedOverlayRequestsActuallySkipped','foregroundLatencyAccepted')}))


if __name__=='__main__':main()
