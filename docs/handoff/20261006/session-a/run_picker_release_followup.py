#!/usr/bin/env python3
"""Strict current116 commands and all16 callers after actual fast16 closure.

Wait on the known live helper; no old90 caller acceptance is transferred.
"""
import argparse
import json
import os
import pathlib
import subprocess
import sys
import time
from run_remaining_normal_media import ROOT, HELPER, accepted, sha


def command(pid):
    r=subprocess.run(['ps','-p',str(pid),'-o','command='],text=True,capture_output=True)
    return r.stdout.strip() if r.returncode==0 else None


def restored(case,apks):
    s=json.loads((case/'session.json').read_text())
    assert s['root']==str(ROOT) and s['serial']=='emulator-5554' and s['apks']==apks
    assert s['stage']=='restored-verified' and s['passed'] and s['coldProcess']['passed'] and s['coldProcess']['differentPid']
    assert set(s['restoration'])=={'internal','external'}
    assert all(row['exactRegularFileSha'] for row in s['restoration'].values())
    assert s['workerObservation']['exitCode']==0 and s['videoObservation']['exitCode']==0
    assert s['videoObservation']['completedOriginalParts']>0 and not s['videoObservation']['captureLimitReachedBeforeRestoration']
    return s


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--producer-pid',type=int,required=True)
    initial=p.add_mutually_exclusive_group(required=True)
    initial.add_argument('--previous-fast',type=pathlib.Path)
    initial.add_argument('--previous-military',type=pathlib.Path)
    initial.add_argument('--previous-attack',type=pathlib.Path)
    p.add_argument('--cohort-receipt',type=pathlib.Path,default=HELPER.with_name('PICKER_RELEASE_BUILD116.json'))
    p.add_argument('--output',type=pathlib.Path,required=True)
    a=p.parse_args();initial_case=a.previous_fast or a.previous_military or a.previous_attack;previous=initial_case.resolve();out=a.output.resolve()
    assert previous.is_relative_to(ROOT/'out/session-a') and out.is_relative_to(ROOT/'out/session-a') and not out.exists()
    receipt=a.cohort_receipt.resolve()
    assert receipt.parent==HELPER.parent.resolve() and receipt.name in ('PICKER_RELEASE_BUILD116.json','NORMAL_VIEW_OPTION_TEST_BUILD124.json','CURRENT_NORMAL_TARGET_BUILD139.json','NORMAL_PREPARATION_BUILD145.json','OVERLAY_ADMISSION_BUILD155.json','B_LEGACY39_COMBINED_TEST_BUILD168.json')
    b=json.loads(receipt.read_text())
    assert b['buildSuccessful'] and b['gameLargeHeap'] is True
    apks={row['path']:row['sha256'] for row in b['apks']};assert all(sha(pathlib.Path(p))==h for p,h in apks.items())
    producer=command(a.producer_pid);assert producer and 'device_session.py install-test' in producer and str(initial_case) in producer
    lock=json.loads(pathlib.Path('/tmp/sanguo11-emulator-5554-session-a.lock/owner.json').read_text())
    # reuse-backup owns the same case lock, then a separate install-test
    # process continues it. The saved backup PID is not its live test PID.
    assert lock['root']==str(ROOT) and lock['output']==str(previous)
    live=json.loads((previous/'session.json').read_text())
    assert live['root']==str(ROOT) and live['serial']=='emulator-5554' and live['apks']==apks
    assert live['stage'] in ('installed-verified','cold-process-running','restored-verified')
    out.mkdir(parents=True)
    report=dict(hostPid=os.getpid(),stage='waiting-for-current-attack' if a.previous_attack else 'waiting-for-current-military' if a.previous_military else 'waiting-for-current116-fast16',producerPid=a.producer_pid,producerCommand=producer,backupLockOwnerPid=lock['pid'],
        apks=apks,initialSession=str(previous),cohortReceipt=str(receipt),fastPreviewAccepted=False,remainingCallersAccepted=False,
        normalCommandResults=[],scope='Exact independent frozen game/test pair, own normal/cold/fullrestore then all three commands and16 normal real callers. Starting military or attack is accepted only for this exact pair. Prior116/124 scores remain their tested pairs only; no PCcrop/voice/ARM/final integration claim.',wholeGoalComplete=False)
    def save():
        (out/'batch.json').write_text(json.dumps(report,indent=2)+'\n')
    save()
    try:
        deadline=time.monotonic()+8*3600
        while True:
            current=command(a.producer_pid)
            if current is None:break
            assert current==producer,'Producer PID reused; do not touch device'
            if time.monotonic()>deadline:raise TimeoutError('Live producer observation timed out, not terminal proof')
            time.sleep(10)
        restored(previous,apks)
        if a.previous_fast:
            proof=json.loads((previous/'evidence/fast-preview-cancellation.json').read_text())
            rows=proof['rows'];assert proof['complete'] and len(rows)==32
            assert {(r['sourceIndex'],r['cycle']) for r in rows}=={(s,c) for s in range(16) for c in range(2)}
            assert all(r['closedWorkersAndOwners'] and r['completeSaveRngStateTokenPure'] for r in rows)
            assert any(r['actualPendingCancellation'] for r in rows)
            report.update(fastPreviewAccepted=True,fastSession=str(previous),stage='current116-fast16-accepted')
        else:
            name='continuous-attack-capture' if a.previous_attack else 'military-construction-repair'
            report['normalCommandResults'].append(dict(name=name,session=str(previous),normalColdRestorationPassed=True))
            report.update(stage='current-attack-accepted' if a.previous_attack else 'current-military-accepted')
        save()
        game=next(p for p in apks if pathlib.Path(p).name=='app-debug.apk');test=next(p for p in apks if pathlib.Path(p).name=='app-debug-androidTest.apk')
        def run_case(name,runner,extra):
            case=out/name;report.update(stage=name,activeStage=name,activeSession=str(case));save()
            subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(case),'--previous',str(previous)],cwd=ROOT,check=True)
            with (case/'driver.log').open('w') as log:
                subprocess.run([sys.executable,str(HELPER),'install-test','--output',str(case),'--apk',game,'--test-apk',test,
                    '--reuse-installed','--observe-workers','--runner',runner,'--fresh-process-reopen',*extra],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
            restored(case,apks);return case
        for name,runner,extra in [('military-construction-repair','SessionAScenePresentationInstrumentation',[]),
            ('fire-extinguish-expiry','SessionAFireFlowInstrumentation',['--pause-fire']),
            ('continuous-attack-capture','SessionAAttackTaskInstrumentation',[])]:
            if a.previous_military and name=='military-construction-repair':continue
            if a.previous_attack and name=='continuous-attack-capture':continue
            previous=run_case(name,runner,extra)
            report['normalCommandResults'].append(dict(name=name,session=str(previous),normalColdRestorationPassed=True));save()
        previous=run_case('source11-normal-callers','SessionAMapRepairInstrumentation',['--suite','mediaAll16','--begin','11','--end','12'])
        state,_=accepted(previous,11);assert state['apks']==apks
        report.update(source11AcceptedSession=str(previous),stage='remaining-normal-callers');save()
        remaining=out/'remaining-normal-callers'
        subprocess.run([sys.executable,str(HELPER.with_name('run_remaining_normal_media.py')),'--previous-source11',str(previous),'--output',str(remaining)],cwd=ROOT,check=True)
        media=json.loads((remaining/'batch.json').read_text());assert media['completeAll16NormalCallers'] and media['apks']==apks
        report.update(stage='current116-normal-commands-and-all16-callers-complete',remainingCallersAccepted=True,remainingBatch=str(remaining));save()
    except BaseException as error:
        report.update(stage='stopped-retain-actual-evidence',error=repr(error),needsActualRestorationInspection=True);save();raise


if __name__=='__main__':main()
