#!/usr/bin/env python3
"""Bind all actual fast preview owners to source/cycle and close logs.

Completed normal/cold/full restore required. Does not infer a unique OOM leak.
"""
import argparse
import json
import pathlib
import re
import statistics
from run_remaining_normal_media import ROOT, sha


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--session',type=pathlib.Path,required=True)
    p.add_argument('--output',type=pathlib.Path,required=True)
    a=p.parse_args();case=a.session.resolve();assert case.is_relative_to(ROOT/'out/session-a')
    assert not a.output.exists()
    state=json.loads((case/'session.json').read_text())
    assert state['root']==str(ROOT) and state['serial']=='emulator-5554'
    assert state['stage']=='restored-verified' and state['passed'] and state['normalPassed']
    assert state['coldProcess']['passed'] and state['coldProcess']['differentPid']
    assert set(state['restoration'])=={'internal','external'}
    assert all(row['exactRegularFileSha'] for row in state['restoration'].values())
    assert state['workerObservation']['exitCode']==state['videoObservation']['exitCode']==0
    assert all(sha(pathlib.Path(path))==digest for path,digest in state['apks'].items())
    proof=json.loads((case/'evidence/fast-preview-cancellation.json').read_text())
    assert proof['complete'] and len(proof['rows'])==32
    requested={(s,c) for s in range(16) for c in range(2)}
    assert {(r['sourceIndex'],r['cycle']) for r in proof['rows']}==requested
    actual=(case/'evidence/result.txt').read_text()
    identities=re.findall(r'ACTUAL fast preview source=(\d+) cycle=(\d+) mapIdentity=(\d+) rendererIdentity=(\d+) pending=(\d+) assets=(\d+)',actual)
    assert len(identities)==32
    normal_pid=str(state['coldProcess']['beforePid'])
    events={}
    for line in (case/'logcat.txt').read_text(errors='replace').splitlines():
        if not re.search(r'\s'+re.escape(normal_pid)+r'\s+'+re.escape(normal_pid)+r'\s+I PickerLifetime:',line):continue
        begin=re.search(r'close begin picker=(\d+) map=(\d+) reason=return native=true',line)
        end=re.search(r'close end picker=(\d+) map=(\d+) reason=return wallMs=(\d+) native=false',line)
        if begin:events.setdefault(begin.group(2),[]).append(dict(kind='begin',pickerIdentity=begin.group(1),rawLine=line))
        if end:events.setdefault(end.group(2),[]).append(dict(kind='end',pickerIdentity=end.group(1),wallMs=int(end.group(3)),rawLine=line))
    bindings=[]
    for source,cycle,map_id,renderer,pending,assets in identities:
        key=int(source),int(cycle)
        row=next(r for r in proof['rows'] if (r['sourceIndex'],r['cycle'])==key)
        assert row['closedWorkersAndOwners'] and row['completeSaveRngStateTokenPure']
        assert row['pendingAtObservation']==int(pending) and row['assetsAtObservation']==int(assets)
        ordered=events[map_id];assert [r['kind'] for r in ordered]==['begin','end']
        assert ordered[0]['pickerIdentity']==ordered[1]['pickerIdentity']
        bindings.append(dict(sourceIndex=key[0],cycle=key[1],mapIdentity=map_id,rendererIdentity=renderer,
            pendingAtObservation=int(pending),assetsAtObservation=int(assets),
            actualPendingCancellation=row['actualPendingCancellation'],
            sourceNativeCloseWallMs=ordered[1]['wallMs'],cancelToBothWorkersClosedMs=row['cancelToWorkersClosedMs'],
            actualOwnerCloseLogLines=ordered,fullSaveRngStateTokenPure=True,bothWorkersAndOwnersClosed=True))
    assert {(r['sourceIndex'],r['cycle']) for r in bindings}==requested
    times=[r['sourceNativeCloseWallMs'] for r in bindings]
    report=dict(actualCase=str(case),apks=state['apks'],normalColdFullRestorationPassed=True,
        normalPid=normal_pid,bindings=bindings,actualBound32ClosePairs=len(bindings),
        originalFailedSource3Cycle1NowClosed=True,
        nativeCloseWallMs=dict(min=min(times),max=max(times),median=statistics.median(times)),
        actualPendingCancellationPairs=sum(r['actualPendingCancellation'] for r in bindings),
        rawFiles={f:sha(case/f) for f in ('session.json','evidence/result.txt','evidence/fast-preview-cancellation.json','logcat.txt')},
        scope='Actual exact116 main-thread owner identities and Return close begin/end, full32 cancel/normal16/cold/every-file restore. Includes source3-cycle1 old114 trigger. Camera/output/art parity, unperturbed timing, allocation stack, unique userOOM cause and ARM are not inferred.',
        gpuBudgetAccepted=False,uniqueUserOomCauseProven=False,wholeGoalComplete=False)
    a.output.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({k:report[k] for k in ('actualBound32ClosePairs','actualPendingCancellationPairs','nativeCloseWallMs')}))


if __name__=='__main__':main()
