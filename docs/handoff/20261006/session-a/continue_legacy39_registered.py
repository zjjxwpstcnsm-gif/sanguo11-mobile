#!/usr/bin/env python3
"""Serialize genuine completed B39 continuation after the current A data owner.

No old acceptance transfer. No direct world mutation, fabricated save or B WIP.
"""
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
    p=argparse.ArgumentParser()
    p.add_argument('--producer-pid',type=int,required=True)
    p.add_argument('--previous-case',type=pathlib.Path,required=True)
    p.add_argument('--output',type=pathlib.Path,required=True)
    a=p.parse_args();previous=a.previous_case.resolve();out=a.output.resolve()
    assert previous.is_relative_to(ROOT/'out/session-a') and out.is_relative_to(ROOT/'out/session-a') and not out.exists()
    producer=command(a.producer_pid)
    assert producer and 'device_session.py install-test' in producer and str(previous) in producer
    old=json.loads((previous/'session.json').read_text())
    assert old['root']==str(ROOT) and old['serial']=='emulator-5554'
    receipt=HELPER.with_name('LEGACY39_REGISTERED_TEST_BUILD176.json')
    build=json.loads(receipt.read_text());assert build['buildSuccessful'] and build['gameLargeHeap'] and build['legacy39RunnerManifestRegistered']
    apks={row['path']:row['sha256'] for row in build['apks']}
    assert all(sha(pathlib.Path(path))==digest for path,digest in apks.items())
    out.mkdir(parents=True)
    report=dict(hostPid=os.getpid(),stage='waiting-for-current-data-owner',producerPid=a.producer_pid,producerCommand=producer,
        previousCase=str(previous),previousApks=old['apks'],apks=apks,cohortReceipt=str(receipt),deviceActionsStarted=False,
        scope='Exact completed genuine39 fixture, ordinary menu load/adoption cancel/human/native terminal/three turns/save-read and independent cold replay on current A production. No original diplomacy/concession/ARM claim.',wholeGoalComplete=False)
    def save():(out/'queue.json').write_text(json.dumps(report,indent=2)+'\n')
    save()
    try:
        deadline=time.monotonic()+8*3600
        while True:
            actual=command(a.producer_pid)
            if actual is None:break
            assert actual==producer,'PID reused; no device action'
            if time.monotonic()>deadline:raise TimeoutError('Live owner observation timeout, not terminal proof')
            time.sleep(10)
        prior=json.loads((previous/'session.json').read_text())
        assert prior['root']==str(ROOT) and prior['serial']=='emulator-5554' and prior['apks']==old['apks']
        assert prior['stage']=='restored-verified' and set(prior['restoration'])=={'internal','external'}
        assert all(row['exactRegularFileSha'] for row in prior['restoration'].values())
        assert prior['videoObservation']['exitCode']==prior['workerObservation']['exitCode']==0
        assert prior['videoObservation']['completedOriginalParts']>0 and not prior['videoObservation']['captureLimitReachedBeforeRestoration']
        report.update(previousNormalAccepted=bool(prior.get('passed')),previousColdAccepted=bool(prior.get('coldProcess',{}).get('passed')))
        assert subprocess.check_output(['git','-C','/Users/paopao/.codex/worktrees/scenario-main-closeout/sanguo11-mobile','rev-parse','main'],text=True).strip()=='ef413be3653820dd6449ba7f02aa60bed5b26ef5','New main requires complete inheritance'
        assert not subprocess.check_output(['git','diff','--name-only',build['sourceRevision'],'--','app/src','app/build.gradle','core','game-api','game-runtime'],cwd=ROOT).strip(),'Source/WIP changed; new build required'
        case=out/'genuine39';report.update(stage='fresh-full-backup-verification',activeSession=str(case));save()
        subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(case),'--previous',str(previous)],cwd=ROOT,check=True)
        game=next(path for path in apks if pathlib.Path(path).name=='app-debug.apk')
        test=next(path for path in apks if pathlib.Path(path).name=='app-debug-androidTest.apk')
        report.update(stage='actual-genuine39-normal-cold',deviceActionsStarted=True);save()
        with (case/'driver.log').open('w') as log:
            subprocess.run([sys.executable,str(HELPER),'install-test','--output',str(case),'--apk',game,'--test-apk',test,
                '--test-only-update','--observe-workers','--runner','SessionBLegacy39Instrumentation','--fresh-process-reopen'],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
        state=restored(case,apks)
        assert state['actualRunnerRegistered'] and state['exactDefaultVideoRequested']
        assert sha(case/'evidence/actual-mid.sg11')==state['coldProcess']['expectedStartupSaveSha256']
        assert sha(case/'evidence/finished.sg11')==sha(case/'cold-evidence/finished.sg11')
        report.update(stage='genuine39-normal-cold-fullrestore-accepted',genuine39Accepted=True);save()
    except BaseException as error:
        report.update(stage='stopped-retain-actual-evidence',error=repr(error),needsActualRestorationInspection=report['deviceActionsStarted']);save();raise


if __name__=='__main__':main()
