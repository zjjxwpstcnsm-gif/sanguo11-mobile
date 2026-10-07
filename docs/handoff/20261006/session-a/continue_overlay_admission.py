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
    p.add_argument('--previous-fire',type=pathlib.Path,required=True);p.add_argument('--output',type=pathlib.Path,required=True)
    a=p.parse_args();previous=a.previous_fire.resolve();out=a.output.resolve()
    assert previous.is_relative_to(ROOT/'out/session-a') and out.is_relative_to(ROOT/'out/session-a') and not out.exists()
    producer=command(a.producer_pid)
    assert producer and 'device_session.py install-test' in producer and str(previous) in producer
    current=json.loads((previous/'session.json').read_text());old_apks=current['apks']
    assert current['root']==str(ROOT) and current['serial']=='emulator-5554'
    b=json.loads(HELPER.with_name('OVERLAY_ADMISSION_BUILD155.json').read_text())
    assert b['buildSuccessful'] and b['gameLargeHeap'] is True
    apks={r['path']:r['sha256'] for r in b['apks']}
    assert all(sha(pathlib.Path(p))==h for p,h in apks.items())
    out.mkdir(parents=True);report=dict(hostPid=os.getpid(),stage='waiting-for-old-fire-owner',producerPid=a.producer_pid,
        producerCommand=producer,previousFire=str(previous),oldApks=old_apks,newApks=apks,deviceActionsStarted=False,
        scope='New independently built production overlay fix. No old145/156 acceptance transferred; first actual fresh Source14 military normal/cold/fullrestore verifies affected UI and latency. Whole matrices follow separately.',wholeGoalComplete=False)
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
        restored(previous,old_apks)
        assert not subprocess.check_output(['git','diff','--name-only',b['sourceRevision'],'HEAD','--','app/src','app/build.gradle','core','game-api','game-runtime'],cwd=ROOT).strip(),'Source changed; fresh APK required'
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
