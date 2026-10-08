#!/usr/bin/env python3
"""Finish interrupted optional test capture restore; keep original errors/raw PCM."""
from pathlib import Path
import json,os,subprocess
import device_session as ds
import audio_capture_support as capture
from read_session_state import read_session_state
from run_cache_regression304 import live_case
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent;CASE=ROOT/'out/session-a/gpu-menu346/large-menu-pcm';OUTPUT=DOC/'CAPTURE_RESTORE348.json'
def alive(pid):
 try:os.kill(pid,0);return True
 except ProcessLookupError:return False
def main():
 assert not OUTPUT.exists();r=read_session_state(CASE/'session.json');assert r['stage']=='restored-verified' and r.get('audioCaptureRestoreError');assert not live_case(CASE)
 owner=read_session_state(ds.LOCK/'owner.json');assert owner['root']==str(ROOT) and owner['output']==str(CASE) and not alive(owner['pid']);assert 'restore incomplete' in owner['purpose']
 before=ds.digest(CASE/'session.json');originalError=r['audioCaptureRestoreError'];owner['pid']=os.getpid();owner['purpose']='A same-case optional capture restore repair; original failed evidence retained';(ds.LOCK/'owner.json').write_text(json.dumps(owner))
 capture.stop_and_restore(CASE,r)
 for kind,record in r['trees'].items():assert ds.device_manifest(ds.TREES[kind])=={k:v['sha256'] for k,v in record['files'].items()}
 c=r['audioCapture'];assert c['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in c['restoration'].values());assert c['previousApkSha256']==ds.digest(Path(c['previousApkPath']))
 report={'case':str(CASE),'originalSessionSha256':before,'repairedSessionSha256':ds.digest(CASE/'session.json'),'originalCaptureRestoreErrorPreserved':originalError,'gameFileCounts':{k:len(v['files']) for k,v in r['trees'].items()},'gameEachOriginalShaReadBack':True,'testRestoration':c['restoration'],'testApkRestoredSha256':c['previousApkSha256'],'recordPermissionRestored':c['grantedBefore'],'appOpBefore':c['modeBefore'],'appOpAfter':c['appOpAfter'],'salvagedActualCaptureResults':[{'run':x['run'],'hostPath':x.get('hostPath'),'result':x.get('result')} for x in c['captures']],'ordinaryWholeMusicAccepted':False,'scope':'Repair only same failed346 optional capture rollback after actual old owner exit; original game/test files/APK/permission/appop verified. Raw interrupted capture saved before generated test evidence removal; original restore error retained, no invented menu metadata or acceptance.','wholeGoalComplete':False}
 OUTPUT.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');ds.LOCK.joinpath('owner.json').unlink();ds.LOCK.rmdir();print(json.dumps({'gameEachOriginalShaReadBack':True,'testApkRestoredSha256':c['previousApkSha256'],'ordinaryWholeMusicAccepted':False}),flush=True)
if __name__=='__main__':main()
