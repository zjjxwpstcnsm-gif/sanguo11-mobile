#!/usr/bin/env python3
"""Read-only exact current297 video sidecar; retain startup omission explicitly."""
from pathlib import Path
import json,subprocess,sys
from read_session_state import read_session_state
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
CASE=ROOT/'out/session-a/fire-cache-installed297';RECEIPT=DOC/'FIRE_CACHE_VIDEO298_LAUNCH.json'
def main():
 assert not RECEIPT.exists() and not (CASE/'video298').exists();state=read_session_state(CASE/'session.json');assert state['stage']=='installed-verified' and state['actualRunnerRegistered'] and not state['exactDefaultVideoRequested']
 build=read_session_state(DOC/'FIRE_CACHE_APK296.json');apks={r['path']:r['sha256'] for r in build['apks']};assert state['apks']==apks
 for p,h in apks.items():assert sha(Path(p))==h
 owner=read_session_state(Path('/tmp/sanguo11-emulator-5554-session-a.lock/owner.json'));assert owner['root']==str(ROOT) and owner['output']==str(CASE)
 active=subprocess.check_output(['ps','-p','49583','-o','command='],text=True);assert str(CASE) in active and 'install-test' in active
 observer=DOC/'observe_flow_video.py';command=[sys.executable,str(observer),'--session',str(CASE/'session.json'),'--output',str(CASE/'video298'),'--max-parts','80','--remove-verified-device-parts'];report={'stage':'actual-running','apks':apks,'actualSession':str(CASE),'command':command,'startupOmitted':True,'reason':'New296 receipt absent from old helper video whitelist; current live test retained without restart. Exact-cohort read-only sidecar starts later and records its actual start time.','scope':'Own fresh UUID /data/local/tmp only; app Save/library/preferences untouched. Capture encoder perturbs performance; no full startup/originalPC/audio/ARM acceptance. Current session.json is never rewritten by this sidecar. Subsequent install must wait this actual observer exit and original every-file SHA restoration.','wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(report,indent=2)+'\n')
 with (CASE/'video298-driver.log').open('w') as f:
  p=subprocess.Popen(command,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT);report['hostPid']=p.pid;save();print('actual sidecar hostPid='+str(p.pid),flush=True);code=p.wait()
 report.update(stage='terminal',observerExit=code);index=CASE/'video298/video.json'
 if code==0:
  video=read_session_state(index);assert video['apks']==apks and video['parts'] and not video['captureLimitReachedBeforeRestoration']
  for part in video['parts']:assert sha(Path(part['path']))==part['sha256']==part['deviceSha256'] and 'error' not in part
  report.update(indexPath=str(index),indexSha256=sha(index),actualParts=len(video['parts']),endedStage=video['endedStage'])
 save();raise SystemExit(code)
if __name__=='__main__':main()
