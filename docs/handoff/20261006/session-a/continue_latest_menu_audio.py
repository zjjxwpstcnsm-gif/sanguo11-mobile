#!/usr/bin/env python3
"""Serialize current frozen menu PCM acceptance after the complete normal batch.

Waiting never touches the device. A failed/incomplete producer stops this job.
"""
import argparse
import hashlib
import json
import os
import pathlib
import subprocess
import sys
import time
import zipfile

from run_remaining_normal_media import ROOT, HELPER, accepted, sha


def command(pid):
    r=subprocess.run(['ps','-p',str(pid),'-o','command='],text=True,capture_output=True)
    return r.stdout.strip() if r.returncode==0 else None


def normal_batch(batch, apks, require_fast):
    report=json.loads((batch/'batch.json').read_text())
    assert report['apks']==apks
    if (require_fast and not report.get('fastPreviewAccepted')) or not report.get('remainingCallersAccepted') or report.get('error'):
        raise ValueError('Actual producer normal batch incomplete or failed')
    commands=report.get('normalCommandResults',[])
    assert {r['name'] for r in commands}=={'military-construction-repair','fire-extinguish-expiry','continuous-attack-capture'}
    assert len(commands)==3 and all(r['normalColdRestorationPassed'] for r in commands)
    for entry in commands:
        state=json.loads((pathlib.Path(entry['session'])/'session.json').read_text())
        assert state['stage']=='restored-verified' and state['passed'] and state['apks']==apks
        assert state['coldProcess']['passed'] and state['coldProcess']['differentPid']
        assert set(state['restoration'])=={'internal','external'}
        assert all(r['exactRegularFileSha'] for r in state['restoration'].values())
        assert state['workerObservation']['exitCode']==0 and state['videoObservation']['exitCode']==0
        assert state['videoObservation']['completedOriginalParts']>0 and not state['videoObservation']['captureLimitReachedBeforeRestoration']
    media=json.loads((pathlib.Path(report['remainingBatch'])/'batch.json').read_text())
    assert media['completeAll16NormalCallers'] and media['apks']==apks
    assert set(media['completedSources'])==set(range(16)) and set(media['sessions'])==set(map(str,range(16)))
    for source in range(16):
        actual,_=accepted(pathlib.Path(media['sessions'][str(source)]),source)
        assert actual['apks']==apks
    return pathlib.Path(media['sessions']['15'])


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--producer-pid',type=int,required=True)
    p.add_argument('--normal-batch',type=pathlib.Path,required=True)
    p.add_argument('--output',type=pathlib.Path,required=True)
    p.add_argument('--cohort-receipt',type=pathlib.Path,default=HELPER.with_name('FIRE_OVERRIDE_DEFAULT_BUILD90.json'))
    p.add_argument('--check-only',action='store_true')
    a=p.parse_args()
    batch=a.normal_batch.resolve();output=a.output.resolve()
    assert batch.is_relative_to(ROOT/'out/session-a') and output.is_relative_to(ROOT/'out/session-a')
    receipt=a.cohort_receipt.resolve()
    assert receipt.parent==HELPER.parent.resolve() and receipt.name in ('FIRE_OVERRIDE_DEFAULT_BUILD90.json','NORMAL_VIEW_OPTION_TEST_BUILD124.json','CURRENT_NORMAL_TARGET_BUILD139.json','NORMAL_PREPARATION_BUILD145.json','OVERLAY_ADMISSION_BUILD155.json','B_LEGACY39_COMBINED_TEST_BUILD168.json')
    build=json.loads(receipt.read_text())
    assert build['buildSuccessful'] and build['gameLargeHeap'] is True
    current124=receipt.name!='FIRE_OVERRIDE_DEFAULT_BUILD90.json'
    if not current124:assert build['sourceRevision']=='fd88f79a2cc7a5efcf5942b5d1b9891f7608ec77'
    apks={r['path']:r['sha256'] for r in build['apks']}
    assert all(sha(pathlib.Path(path))==digest for path,digest in apks.items())
    if a.check_only:
        normal_batch(batch,apks,require_fast=not current124)
        print('Actual complete normal batch gate passed; no device action')
        return
    assert a.producer_pid>0 and not output.exists()
    producer=command(a.producer_pid)
    expected_producer='run_picker_release_followup.py' if current124 else 'run_fast_preview_and_remaining_media.py'
    assert producer and expected_producer in producer
    assert str(a.normal_batch) in producer
    if current124:assert str(a.cohort_receipt) in producer
    output.mkdir(parents=True)
    state={'stage':'waiting-for-actual-normal-producer','hostPid':os.getpid(),'producerPid':a.producer_pid,'producerCommand':producer,
           'normalBatch':str(batch),'apks':apks,'cohortReceipt':str(receipt),'wholeTrackGate':.995,'deviceActionsStarted':False,
           'fastPreviewAcceptedForThisPair':False,
           'scope':'Exact frozen pair independent normal menu music/PCM/Save-RNG-StateToken/whole file restoration after all16 callers and normal fire/construction/attack. Current124 does not borrow116 fast preview score. Not mapBGM/voice/old-22 unique cause/ARM/final combined APK acceptance.',
           'wholeGoalComplete':False}
    def save():
        (output/'queue.json').write_text(json.dumps(state,indent=2)+'\n')
    save()
    try:
        deadline=time.monotonic()+48*3600
        while True:
            actual=command(a.producer_pid)
            if actual is None:break
            if actual!=producer:raise ValueError('Producer PID identity changed; no device action')
            if time.monotonic()>deadline:raise TimeoutError('Producer still live; queue timeout is not producer failure')
            time.sleep(10)
        previous=normal_batch(batch,apks,require_fast=not current124)
        reference=ROOT/'out/session-a/music-submission-appop-44/reference'
        manifest=json.loads((reference/'manifest.json').read_text())
        entry=next(r for r in manifest['entries'] if r['resourceId']==2238)
        assert sha(reference/'2238.wav')==entry['wavSha256']
        game=next(pathlib.Path(p) for p in apks if pathlib.Path(p).name=='app-debug.apk')
        test=next(pathlib.Path(p) for p in apks if pathlib.Path(p).name=='app-debug-androidTest.apk')
        sys.path.insert(0,str(ROOT/'tools/content'))
        from pc_resources import Archive
        pc=pathlib.Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版')
        archive=Archive(pc/'Media/san11pkres.bin')
        try:original=archive.read(2238)
        finally:archive.close()
        assert hashlib.sha256(original).hexdigest()==entry['originalKovsSha256']
        with zipfile.ZipFile(game) as z:
            assert hashlib.sha256(z.read('assets/'+entry['asset'])).hexdigest()==entry['oggSha256']
        case=output/'actual-normal-menu-capture'
        state.update(stage='normal-prerequisites-and-original-reference-verified',previous=str(previous),
                     originalResourceSha256=entry['originalKovsSha256'],referenceSha256=entry['wavSha256'])
        save()
        state.update(stage='actual-backup-verification',deviceActionsStarted=True);save()
        subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(case),'--previous',str(previous)],cwd=ROOT,check=True)
        state.update(stage='actual-normal-menu-capture');save()
        with (case/'driver.log').open('w') as log:
            subprocess.run([sys.executable,str(HELPER),'install-test','--output',str(case),
                '--apk',str(game),'--test-apk',str(test),'--reuse-installed','--observe-workers',
                '--runner','UiUxInstrumentation','--suite','audio','--menu-music','--audio-capture-rate','44100'],
                cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
        actual=json.loads((case/'session.json').read_text())
        assert actual['stage']=='restored-verified' and actual['passed'] and actual['apks']==apks
        assert set(actual['restoration'])=={'internal','external'}
        assert all(r['exactRegularFileSha'] for r in actual['restoration'].values())
        capture=actual['audioCapture']
        assert capture['stage']=='restored-verified' and all(r['exactRegularFileSha'] for r in capture['restoration'].values())
        music=next(r for r in capture['captures'] if r['run'].endswith('_menu'))
        assert not music['result']['failure'] and music['result']['frames']>0
        menu=json.loads((case/'evidence/menu-music.json').read_text())
        assert menu['normalMenuOnly'] and menu['noTestPlaybackDirective'] and menu['allSaveRngEqual'] and menu['allStateTokenEqual']
        assert menu['firstAcceptedBytes']==menu['firstDecodedBytes']==15618048
        assert menu['firstAcceptedPcmSha256']==menu['firstDecodedPcmSha256']
        waveform=case/'whole-music-check.json'
        with (case/'whole-music-check.log').open('w') as log:
            subprocess.run([sys.executable,str(ROOT/'tools/audio/check_pc_music_mix.py'),
                str(pathlib.Path(music['hostPath'])/'android-mix.wav'),str(reference/'2238.wav'),
                '--resource','2238','--manifest',str(reference/'manifest.json'),'--output',str(waveform)],
                cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
        result=json.loads(waveform.read_text());assert result['status']=='PASS' and result['correlation']>=.995
        state.update(stage='actual-menu-waveform-and-restoration-verified',case=str(case),waveform=result,
                     normalMenuFullSaveRngStateTokenPassed=True,menuColdProcessAccepted=False,
                     menuExitBoundary='Inherited Activity.finish release barrier; this audio case does not claim real Back/cold acceptance',
                     originalWindowsExactPcm=False,mapBgmVoiceOrArmAccepted=False)
        save()
    except BaseException as error:
        state.update(stage='stopped-retain-actual-evidence',error=repr(error),needsActualRestorationInspection=state['deviceActionsStarted'])
        save();raise


if __name__=='__main__':main()
