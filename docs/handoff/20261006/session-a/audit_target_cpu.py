#!/usr/bin/env python3
"""Freeze actual CPU observations after their owner exits and full restore.

CPU percentages are snapshots, not simultaneous peaks or a unique cause.
"""
import argparse
import json
import pathlib
import re
import subprocess
from run_remaining_normal_media import ROOT, sha


def schedstat_groups(samples):
    """Keep same observed PID/TID monotonic segments, not a lifetime proof."""
    groups=[];current={};errors=[]
    for index,sample in enumerate(samples):
        for row in sample.get('strategySchedstat',[]):
            if 'unavailable' in row:
                errors.append(dict(sampleIndex=index,targetPid=sample.get('targetPid'),**row));continue
            if not sample.get('sameTargetPidCommandBeforeAfter'):
                errors.append(dict(sampleIndex=index,reason='outer target identity unavailable',**row));continue
            key=sample['targetPid'],row['tid']
            values=[row[k] for k in ('runtimeNanos','runqueueWaitNanos','timeslices')]
            assert all(isinstance(v,int) and v>=0 for v in values)
            observed=dict(sampleIndex=index,**row)
            prior=current.get(key)
            if prior is None or any(row[k]<prior['last'][k] for k in ('runtimeNanos','runqueueWaitNanos','timeslices')):
                group=dict(targetPid=key[0],tid=key[1],first=observed,last=observed,samples=1)
                groups.append(group);current[key]=group
            else:
                prior['last']=observed;prior['samples']+=1
    for group in groups:
        first,last=group['first'],group['last']
        group['hostIntervalSeconds']=last['hostUnixEnd']-first['hostUnixStart']
        group['delta']={k:last[k]-first[k] for k in ('runtimeNanos','runqueueWaitNanos','timeslices')}
        group['identityBoundary']='Observed same PID/TID/comm with monotonic counters; no proc starttime, full lifetime or no-TID-reuse proof'
    return groups,errors


def audit(case, observer_pid, output):
    case=case.resolve(); output=output.resolve()
    assert case.is_relative_to(ROOT/'out/session-a') and not output.exists()
    live=subprocess.run(['ps','-p',str(observer_pid),'-o','command='],capture_output=True,text=True)
    assert live.returncode!=0 and not live.stdout.strip(), 'Observer still live; no frozen log claim'
    state=json.loads((case/'session.json').read_text())
    assert state['root']==str(ROOT) and state['serial']=='emulator-5554'
    assert state['stage']=='restored-verified'
    assert set(state['restoration'])=={'internal','external'}
    assert all(row['exactRegularFileSha'] for row in state['restoration'].values())
    assert state['workerObservation']['exitCode']==state['videoObservation']['exitCode']==0
    assert all(sha(pathlib.Path(p))==h for p,h in state['apks'].items())
    raw=case/'target-threads126.jsonl'
    samples=[json.loads(line) for line in raw.read_text().splitlines()]; assert samples
    allowed={str(state['coldProcess'][key]) for key in ('beforePid','afterPid')} if state.get('coldProcess') else set()
    observations=[]; unavailable=[]
    for index,sample in enumerate(samples):
        assert sample['hostPid']==observer_pid and sample['apks']==state['apks']
        if 'unavailable' in sample:
            unavailable.append(dict(sampleIndex=index,reason=sample['unavailable']));continue
        assert sample['sameTargetPidCommandBeforeAfter'] and (not allowed or sample['targetPid'] in allowed)
        header=next(line for line in sample['topThreads'].splitlines() if line.strip().startswith('TID ')).split()
        cpu_index=header.index('S[%CPU]') if 'S[%CPU]' in header else header.index('[%CPU]')
        # Android top renders "S[%CPU]" as two values: state, then CPU.
        if 'S[%CPU]' in header:cpu_index+=1
        for line in sample['topThreads'].splitlines():
            fields=line.split()
            if not fields or not fields[0].isdigit() or len(fields)<=cpu_index+3:continue
            name=fields[cpu_index+3]
            if name not in ('strategy-turn','RenderThread','FEngine::loop','HeapTaskDaemon'):continue
            observations.append(dict(sampleIndex=index,hostUnixStart=sample['hostUnixStart'],
                hostUnixEnd=sample['hostUnixEnd'],targetPid=sample['targetPid'],tid=fields[0],
                name=name,cpuPercent=float(fields[cpu_index]),rawLine=line,
                adjacentProgressBefore=sample['progressBefore'].splitlines()[-1:],
                adjacentProgressAfter=sample['progressAfter'].splitlines()[-1:]))
    turns=[]
    for line in (case/'logcat.txt').read_text(errors='replace').splitlines():
        match=re.search(r'\s(\d+)\s+\1\s+I Turn52\s*: TURN_END totalMs=(\d+) activeMs=(\d+) computeMs=(\d+) saveMs=(\d+) factions=(\d+)',line)
        if match and (not allowed or match[1] in allowed):
            turns.append(dict(pid=match[1],totalMs=int(match[2]),activeMs=int(match[3]),
                computeMs=int(match[4]),saveMs=int(match[5]),factions=int(match[6]),rawLine=line))
    scheduling,scheduling_errors=schedstat_groups(samples)
    report=dict(actualCase=str(case),apks=state['apks'],samples=len(samples),observations=observations,
        strategySchedstatSegments=scheduling,strategySchedstatUnavailable=scheduling_errors,
        unavailableSamples=unavailable,completedTurns=turns,finalLogFrozen=True,observerHostPid=observer_pid,
        normalPassed=state.get('normalPassed'),coldPassed=state.get('coldProcess',{}).get('passed'),
        everyOriginalShaRestored=True,foreground120sStableAccepted=False,uniqueCauseProven=False,
        rawFiles={name:sha(case/name) for name in ('session.json','target-threads126.jsonl','logcat.txt')},
        scope='Sequential same-PID CPU snapshots and adjacent progress, not atomic foreground phase, allocation stack, causal counterfactual, CPU peak sums or ARM. Recording/readback may perturb scheduling. Historical Turn52 wall profile is not a CPU profiler.',wholeGoalComplete=False)
    output.write_text(json.dumps(report,indent=2)+'\n')
    return report


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--session',type=pathlib.Path,required=True)
    p.add_argument('--observer-pid',type=int,required=True);p.add_argument('--output',type=pathlib.Path,required=True)
    a=p.parse_args();r=audit(a.session,a.observer_pid,a.output)
    print(json.dumps({k:r[k] for k in ('samples','completedTurns','finalLogFrozen','uniqueCauseProven')}))
