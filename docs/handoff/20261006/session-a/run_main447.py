#!/usr/bin/env python3
"""Actual main05236 independent APK normal source options/save/read/newprocess and original file rollback."""
from pathlib import Path
import json,subprocess,sys,time
from read_session_state import read_session_state as read
from stage_serial401 import ROOT,D,sha
from run_music_reserve378 import live_case
OUT=ROOT/'out/session-a/main447';RECEIPT=D/'MAIN447_LAUNCH.json';HELPER=D/'device_session.py'
def main():
 assert not OUT.exists() and not RECEIPT.exists();r={'stage':'waiting-new-combined-build-terminal','previousScoresTransferred':False,'wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(r,indent=2)+'\n')
 save()
 try:
  while not (D/'MAIN_BUILD446.json').exists():time.sleep(5)
  b=read(D/'MAIN_BUILD446.json');assert b['buildSuccessful'] and b['buildExit']==0 and b['sourceInputsExactFinalCandidate']
  previous=ROOT/'out/session-a/closeout438';state=read(previous/'session.json');assert state['stage']=='restored-verified' and state['passed'] and all(x['exactRegularFileSha'] for x in state['restoration'].values()) and not live_case(previous)
  apks={x['path']:x['sha256'] for x in b['apks']}
  for p,h in apks.items():assert sha(Path(p))==h
  assert b['sourceInputsExactFinalCandidate'] and b['mainWorkingTreeClean']
  assert b['sourceRevision']=='05236ca41c7795c9fc3030b9c274b7a8a25be262'
  r.update(stage='fresh-complete-backup',apks=apks);save()
  with (ROOT/'out/session-a/main447-backup.log').open('w') as f:backup=subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(OUT),'--previous',str(previous)],stdout=f,stderr=subprocess.STDOUT,cwd=ROOT)
  assert backup.returncode==0
  game=next(p for p in apks if Path(p).name=='app-debug.apk');test=next(p for p in apks if Path(p).name=='app-debug-androidTest.apk')
  command=[sys.executable,str(HELPER),'install-test','--output',str(OUT),'--apk',game,'--test-apk',test,'--observe-workers','--runner','SessionANativeDuelOpeningInstrumentation','--fresh-process-reopen']
  r.update(stage='actual-new-combined-install-and-normal-options',command=command);save()
  with (OUT/'driver.log').open('w') as f:result=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT,cwd=ROOT)
  final=read(OUT/'session.json');assert final['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in final['restoration'].values()) and not live_case(OUT)
  r.update(stage='restored-verified',normalColdPassed=bool(final.get('passed')),helperExit=result.returncode,restoration=final['restoration'],scope='New main05236 actual APK mandatory explicit normal PC settings source0/source14/cancel/fixed/accept/save/manualread/newprocess/allRNG. No prior16/fire/native-duel/media/ARM scores transferred.');save()
 except BaseException as e:r.update(stage='stopped-preserve-real-evidence',error=str(e));save();raise
if __name__=='__main__':main()
