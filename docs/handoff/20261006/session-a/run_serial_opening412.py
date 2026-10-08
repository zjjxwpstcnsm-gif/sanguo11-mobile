#!/usr/bin/env python3
"""Actual new combined APK source-bound menu options flow and complete restore."""
from pathlib import Path
import json,subprocess,sys,time
from read_session_state import read_session_state as read
from stage_serial401 import ROOT,D,sha
from run_music_reserve378 import live_case
OUT=ROOT/'out/session-a/serial-opening412';RECEIPT=D/'SERIAL_OPENING412_LAUNCH.json';HELPER=D/'device_session.py'
def main():
 assert not OUT.exists() and not RECEIPT.exists();r={'stage':'waiting-new-combined-build-terminal','previousScoresTransferred':False,'wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(r,indent=2)+'\n')
 save()
 try:
  while not (D/'SERIAL_BUILD411.json').exists():time.sleep(5)
  b=read(D/'SERIAL_BUILD411.json');assert b['buildSuccessful'] and b['buildExit']==0 and b['fullSourceShaUnchangedAfterBuild']
  previous=ROOT/'out/session-a/current-fire395';state=read(previous/'session.json');assert state['stage']=='restored-verified' and state['passed'] and all(x['exactRegularFileSha'] for x in state['restoration'].values()) and not live_case(previous)
  apks={x['path']:x['sha256'] for x in b['apks']}
  for p,h in apks.items():assert sha(Path(p))==h
  manifest=Path(b['candidateInputManifest']);assert sha(manifest)==b['candidateInputManifestSha256'];source=Path(b['sourcePath'])
  for row in read(manifest):assert sha(source/row['path'])==row['sha256'],row['path']
  r.update(stage='fresh-complete-backup',apks=apks);save()
  with (ROOT/'out/session-a/serial-opening412-backup.log').open('w') as f:backup=subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(OUT),'--previous',str(previous)],stdout=f,stderr=subprocess.STDOUT,cwd=ROOT)
  assert backup.returncode==0
  game=next(p for p in apks if Path(p).name=='app-debug.apk');test=next(p for p in apks if Path(p).name=='app-debug-androidTest.apk')
  command=[sys.executable,str(HELPER),'install-test','--output',str(OUT),'--apk',game,'--test-apk',test,'--observe-workers','--runner','SessionANativeDuelOpeningInstrumentation','--fresh-process-reopen']
  r.update(stage='actual-new-combined-install-and-normal-options',command=command);save()
  with (OUT/'driver.log').open('w') as f:result=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT,cwd=ROOT)
  final=read(OUT/'session.json');assert final['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in final['restoration'].values()) and not live_case(OUT)
  r.update(stage='restored-verified',normalColdPassed=bool(final.get('passed')),helperExit=result.returncode,restoration=final['restoration'],scope='Fresh combined A397/B27/explicit source options normal menu source0/source14/cancel/fixed/accept/save/manualread/newprocess/allRNG. No prior16/fire/native-duel/media/ARM scores transferred.');save()
 except BaseException as e:r.update(stage='stopped-preserve-real-evidence',error=str(e));save();raise
if __name__=='__main__':main()
