#!/usr/bin/env python3
"""Serialize frozen ordinary/default heap stress after current normal/audio work.

An audio waveform failure is retained, never transferred as a success; memory
work can proceed only after its complete data/preferences restoration is proven.
"""
import argparse
import json
import os
import pathlib
import subprocess
import sys
import time
from continue_latest_menu_audio import command, normal_batch
from run_remaining_normal_media import ROOT, HELPER, sha


def guarded_restoration(case, apks, require_pass):
    s=json.loads((case/'session.json').read_text())
    assert s['root']==str(ROOT) and s['serial']=='emulator-5554' and s['apks']==apks
    assert s['stage']=='restored-verified'
    assert set(s['restoration'])=={'internal','external'}
    assert all(r['exactRegularFileSha'] for r in s['restoration'].values())
    assert s['workerObservation']['exitCode']==0
    if require_pass:
        assert s['passed'] and s['coldProcess']['passed'] and s['coldProcess']['differentPid']
    if s.get('videoObservation'):
        assert s['videoObservation']['exitCode']==0
        assert s['videoObservation']['completedOriginalParts']>0
        assert not s['videoObservation']['captureLimitReachedBeforeRestoration']
    if s.get('audioCapture'):
        a=s['audioCapture'];assert a['stage']=='restored-verified'
        assert set(a['restoration'])=={'test-internal','test-external'}
        assert all(r['exactRegularFileSha'] for r in a['restoration'].values())
    assert not s.get('audioCaptureRestoreError') and not s.get('systemAnimationRestoreError')
    return s


def frozen(name):
    receipt=HELPER.with_name(name);b=json.loads(receipt.read_text())
    assert b['buildSuccessful']
    apks={r['path']:r['sha256'] for r in b['apks']}
    assert all(sha(pathlib.Path(p))==h for p,h in apks.items())
    return b,apks


def main():
    p=argparse.ArgumentParser();p.add_argument('--producer-pid',type=int,required=True)
    p.add_argument('--audio-queue',type=pathlib.Path,required=True)
    p.add_argument('--normal-batch',type=pathlib.Path,required=True)
    p.add_argument('--ordinary-receipt',default='CURRENT_NORMAL384_BUILD134.json',choices=['CURRENT_NORMAL384_BUILD134.json','NORMAL_PREPARATION_ORDINARY149.json'])
    p.add_argument('--default-receipt',default='NORMAL_VIEW_OPTION_TEST_BUILD124.json',choices=['NORMAL_VIEW_OPTION_TEST_BUILD124.json','NORMAL_PREPARATION_BUILD145.json'])
    p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args()
    queue=a.audio_queue.resolve();batch=a.normal_batch.resolve();out=a.output.resolve()
    assert all(x.is_relative_to(ROOT/'out/session-a') for x in (queue,batch,out)) and not out.exists()
    ordinary,ordinary_apks=frozen(a.ordinary_receipt);default,default_apks=frozen(a.default_receipt)
    assert ordinary['gameLargeHeap'] is False and default['gameLargeHeap'] is True
    producer=command(a.producer_pid)
    assert producer and 'continue_latest_menu_audio.py' in producer and str(a.audio_queue) in producer
    initial=json.loads((queue/'queue.json').read_text());assert initial['hostPid']==a.producer_pid and initial['apks']==default_apks
    protected=pathlib.Path('/Users/paopao/.codex/worktrees/scenario-main-closeout/sanguo11-mobile')
    expected_main=subprocess.check_output(['git','-C',str(protected),'rev-parse','main'],text=True).strip()
    expected_local=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    out.mkdir(parents=True)
    report=dict(hostPid=os.getpid(),stage='waiting-for-current-default-normal-audio',producerPid=a.producer_pid,
        producerCommand=producer,ordinaryApks=ordinary_apks,defaultApks=default_apks,mainAtQueueStart=expected_main,
        ordinaryReceipt=a.ordinary_receipt,defaultReceipt=a.default_receipt,
        sourceAtQueueStart=expected_local,audioAccepted=False,deviceActionsStarted=False,
        ordinaryFastAccepted=False,ordinaryAll244Accepted=False,defaultFastAccepted=False,defaultAll244Accepted=False,
        scope='Exact frozen independent ordinary and default fast32 plus all16/244, each normal/newPID/fullSHArestore. No forcedGC/ordinaryencoder, no previous81 or116 score transfer, no ARM/finalintegration claim.',wholeGoalComplete=False)
    def save():(out/'batch.json').write_text(json.dumps(report,indent=2)+'\n')
    save()
    try:
        deadline=time.monotonic()+72*3600
        while True:
            actual=command(a.producer_pid)
            if actual is None:break
            assert actual==producer,'Producer PID reused; no device action'
            if time.monotonic()>deadline:raise TimeoutError('Live producer timeout is not terminal proof')
            time.sleep(10)
        # All current rule/media paths must finish; a restored waveform mismatch
        # does not turn into accepted audio or prevent independent heap evidence.
        previous=normal_batch(batch,default_apks,require_fast=False)
        audio=json.loads((queue/'queue.json').read_text());assert audio['apks']==default_apks
        if audio['deviceActionsStarted']:
            case=queue/'actual-normal-menu-capture';guarded_restoration(case,default_apks,False);previous=case
        report.update(audioAccepted=audio['stage']=='actual-menu-waveform-and-restoration-verified',
            audioTerminalStage=audio['stage'],audioError=audio.get('error'))
        assert subprocess.check_output(['git','-C',str(protected),'rev-parse','main'],text=True).strip()==expected_main,'New main requires complete inheritance before installing this frozen queue'
        for prefix in ('app/src','app/build.gradle','core','game-api','game-runtime'):
            assert not subprocess.check_output(['git','diff','--name-only',ordinary['sourceRevision'],'HEAD','--',prefix],cwd=ROOT).strip(),'New source requires fresh cohort; no stale install'
        def run_case(name,apks,suite,reuse):
            nonlocal previous
            assert subprocess.check_output(['git','-C',str(protected),'rev-parse','main'],text=True).strip()==expected_main,'New main appeared; no stale install'
            assert not subprocess.check_output(['git','diff','--name-only',ordinary['sourceRevision'],'HEAD','--','app/src','app/build.gradle','core','game-api','game-runtime'],cwd=ROOT).strip(),'New source appeared; fresh build required'
            target=out/name;report.update(stage=name,activeSession=str(target));save()
            subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(target),'--previous',str(previous)],cwd=ROOT,check=True)
            report['deviceActionsStarted']=True;save()
            game=next(p for p in apks if pathlib.Path(p).name=='app-debug.apk')
            test=next(p for p in apks if pathlib.Path(p).name=='app-debug-androidTest.apk')
            with (target/'driver.log').open('w') as log:
                subprocess.run([sys.executable,str(HELPER),'install-test','--output',str(target),'--apk',game,'--test-apk',test,
                    *(['--reuse-installed'] if reuse else []),'--observe-workers','--runner','SessionAMapRepairInstrumentation',
                    '--suite',suite,'--begin','0','--end','16','--fresh-process-reopen'],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
            state=guarded_restoration(target,apks,True);previous=target;return state
        run_case('ordinary-fast32',ordinary_apks,'fastPreview16',False)
        proof=json.loads((previous/'evidence/fast-preview-cancellation.json').read_text())
        assert proof['complete'] and len(proof['rows'])==32
        assert {(r['sourceIndex'],r['cycle']) for r in proof['rows']}=={(s,c) for s in range(16) for c in range(2)}
        assert all(r['closedWorkersAndOwners'] and r['completeSaveRngStateTokenPure'] for r in proof['rows'])
        assert any(r['actualPendingCancellation'] for r in proof['rows'])
        report['ordinaryFastAccepted']=True;save()
        for name,apks,reuse in [('ordinary-all244',ordinary_apks,True),('default-all244',default_apks,True)]:
            if name=='default-all244':
                run_case('default-fast32',default_apks,'fastPreview16',False)
                proof=json.loads((previous/'evidence/fast-preview-cancellation.json').read_text())
                assert proof['complete'] and len(proof['rows'])==32
                assert {(r['sourceIndex'],r['cycle']) for r in proof['rows']}=={(s,c) for s in range(16) for c in range(2)}
                assert all(r['closedWorkersAndOwners'] and r['completeSaveRngStateTokenPure'] for r in proof['rows'])
                assert any(r['actualPendingCancellation'] for r in proof['rows'])
                report['defaultFastAccepted']=True;save()
            run_case(name,apks,'factions16',reuse);enabled=0
            for source in range(16):
                proof=json.loads((previous/'evidence'/('source-'+str(source)+'-faction-previews.json')).read_text())
                assert proof['sourceIndex']==source and proof['noAuthorityMutation']
                assert all(r['actual3DVerified'] for r in proof['slots'] if r['enabled'])
                enabled+=sum(r['enabled'] for r in proof['slots'])
            assert enabled==244
            report['ordinaryAll244Accepted' if name.startswith('ordinary') else 'defaultAll244Accepted']=True;save()
        report.update(stage='current-both-heap-stress-complete',finalDefaultSession=str(previous));save()
    except BaseException as error:
        report.update(stage='stopped-retain-actual-evidence',error=repr(error),needsActualRestorationInspection=report['deviceActionsStarted']);save();raise


if __name__=='__main__':main()
