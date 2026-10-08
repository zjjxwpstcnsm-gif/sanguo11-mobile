#!/usr/bin/env python3
from pathlib import Path
import json
from read_session_state import read_session_state
from run_cache_regression304 import live_case
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent;CASE=ROOT/'out/session-a/gpu-menu338/large-menu-pcm'
def main():
 out=DOC/'CAPTURE_ID_FAILURE339.json';assert not out.exists();r=read_session_state(CASE/'session.json');assert r['stage']=='restored-verified' and not live_case(CASE);assert all(x['exactRegularFileSha'] for x in r['restoration'].values());c=r['audioCapture'];assert c['stage']=='restored-verified' and c['captures']==[] and all(x['exactRegularFileSha'] for x in c['restoration'].values());assert sha(Path(c['previousApkPath']))==c['previousApkSha256'];raw=(CASE/'driver.log').read_text();assert 'ValueError: Capture identity/rate' in raw and not (CASE/'instrumentation.txt').exists()
 report={'case':str(CASE),'actualDriverLog':raw,'normalGameInstrumentationStarted':False,'pcmCapturesStarted':False,'runId':r['runId'],'baseLength':len(r['runId']),'captureInitLength':len(r['runId']+'_init44100'),'allowedCaptureLength':64,'apks':r['apks'],'restoration':r['restoration'],'testTreesRestoration':c['restoration'],'previousTestApkRestoredSha256':c['previousApkSha256'],'recordPermissionBefore':c['grantedBefore'],'recordPermissionAfterRestored':True,'appOpBefore':c['modeBefore'],'appOpAfter':c['appOpAfter'],'workerObservation':r['workerObservation'],'sessionSha256':sha(CASE/'session.json'),'scope':'Actual tool identity precondition rejection before recording/game instrumentation; not Android audio-22/product playback failure or wholetrack acceptance. Complete game/test files/APK/permission/appop restoration retained.','wholeGoalComplete':False}
 out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'baseLength':report['baseLength'],'captureInitLength':report['captureInitLength'],'pcmCapturesStarted':False}))
if __name__=='__main__':main()
