#!/usr/bin/env python3
"""Install the frozen production fix only after the active old fire owner exits."""
import argparse
import json
import os
import pathlib
import subprocess
import sys
import time
from run_picker_release_followup import command, restored
from run_remaining_normal_media import ROOT, HELPER, sha


def main():
    p=argparse.ArgumentParser();p.add_argument('--producer-pid',type=int,required=True)
    previous_arg=p.add_mutually_exclusive_group(required=True)
    previous_arg.add_argument('--previous-fire',type=pathlib.Path)
    previous_arg.add_argument('--previous-case',type=pathlib.Path)
    p.add_argument('--cohort-receipt',default='OVERLAY_ADMISSION_BUILD155.json',choices=['OVERLAY_ADMISSION_BUILD155.json','EMPTY_PRESENTATION_BUILD165.json','B_LEGACY39_COMBINED_TEST_BUILD168.json'])
    p.add_argument('--output',type=pathlib.Path,required=True)
    a=p.parse_args();previous=(a.previous_case or a.previous_fire).resolve();out=a.output.resolve()
    assert previous.is_relative_to(ROOT/'out/session-a') and out.is_relative_to(ROOT/'out/session-a') and not out.exists()
    producer=command(a.producer_pid)
    assert producer and 'device_session.py install-test' in producer and str(previous) in producer
    current=json.loads((previous/'session.json').read_text());old_apks=current['apks']
    assert current['root']==str(ROOT) and current['serial']=='emulator-5554'
    b=json.loads(HELPER.with_name(a.cohort_receipt).read_text())
    assert b['buildSuccessful'] and b['gameLargeHeap'] is True
    apks={r['path']:r['sha256'] for r in b['apks']}
    assert all(sha(pathlib.Path(p))==h for p,h in apks.items())
    out.mkdir(parents=True);report=dict(hostPid=os.getpid(),stage='waiting-for-old-fire-owner',producerPid=a.producer_pid,
        producerCommand=producer,previousFire=str(previous),oldApks=old_apks,newApks=apks,deviceActionsStarted=False,
        cohortReceipt=a.cohort_receipt,scope='New independently built production overlay fix. No old acceptance transferred; first fresh Source14 military normal/cold/fullrestore verifies affected UI and latency. Whole matrices follow separately.',wholeGoalComplete=False)
    def save():(out/'queue.json').write_text(json.dumps(report,indent=2)+'\n')
    save()
    try:
        deadline=time.monotonic()+8*3600
        while True:
            actual=command(a.producer_pid)
            if actual is None:break
            assert actual==producer,'Producer PID reused; no device action'
            if time.monotonic()>deadline:raise TimeoutError('Live observation timeout, not producer terminal proof')
            time.sleep(10)
        old=json.loads((previous/'session.json').read_text())
        assert old['root']==str(ROOT) and old['serial']=='emulator-5554' and old['apks']==old_apks
        assert old['stage']=='restored-verified' and set(old['restoration'])=={'internal','external'}
        assert all(r['exactRegularFileSha'] for r in old['restoration'].values())
        assert old['videoObservation']['exitCode']==old['workerObservation']['exitCode']==0
        assert old['videoObservation']['completedOriginalParts']>0 and not old['videoObservation']['captureLimitReachedBeforeRestoration']
        report.update(previousNormalAccepted=bool(old.get('passed')),previousColdAccepted=bool(old.get('coldProcess',{}).get('passed')))
        # A failed prior production test may be the defect this new build fixes.
        # It is retained as failed, but complete restored data permits a new run.
        assert not subprocess.check_output(['git','diff','--name-only',b['sourceRevision'],'--','app/src','app/build.gradle','core','game-api','game-runtime'],cwd=ROOT).strip(),'Source/WIP changed; fresh APK required'
        case=out/'new-military';report.update(stage='fresh-backup-verification',activeSession=str(case));save()
        subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(case),'--previous',str(previous)],cwd=ROOT,check=True)
        game=next(p for p in apks if pathlib.Path(p).name=='app-debug.apk');test=next(p for p in apks if pathlib.Path(p).name=='app-debug-androidTest.apk')
        report.update(stage='new-overlay-normal-military',deviceActionsStarted=True);save()
        with (case/'driver.log').open('w') as log:
            subprocess.run([sys.executable,str(HELPER),'install-test','--output',str(case),'--apk',game,'--test-apk',test,
                '--observe-workers','--runner','SessionAScenePresentationInstrumentation','--fresh-process-reopen'],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
        restored(case,apks)
        report.update(stage='new-overlay-military-normal-cold-fullrestore-complete',militaryAccepted=True);save()
    except BaseException as error:
        report.update(stage='stopped-retain-actual-evidence',error=repr(error),needsActualRestorationInspection=report['deviceActionsStarted']);save();raise


if __name__=='__main__':main()
