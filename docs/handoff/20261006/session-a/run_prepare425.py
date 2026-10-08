#!/usr/bin/env python3
"""Actual source0 first/cancel/second preview diagnostic with same ce83; original functional120s unchanged."""
from pathlib import Path
import json,subprocess,sys,time
from read_session_state import read_session_state as read
from stage_serial401 import ROOT,D,sha
from run_music_reserve378 import live_case
OUT=ROOT/'out/session-a/prepare425';RECEIPT=D/'PREPARE425_LAUNCH.json';HELPER=D/'device_session.py'
def main():
 assert not OUT.exists() and not RECEIPT.exists();r={'stage':'waiting-new-combined-build-terminal','previousScoresTransferred':False,'wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(r,indent=2)+'\n')
 save()
 try:
  while not (D/'PREPARE_BUILD424.json').exists():time.sleep(5)
  b=read(D/'PREPARE_BUILD424.json');assert b['buildSuccessful'] and b['buildExit']==0 and b['fullSourceShaUnchangedAfterBuild']
  previous=ROOT/'out/session-a/serial-matrix419';state=read(previous/'session.json');assert state['stage']=='restored-verified' and not state['passed'] and all(x['exactRegularFileSha'] for x in state['restoration'].values()) and not live_case(previous)
  apks={x['path']:x['sha256'] for x in b['apks']}
  for p,h in apks.items():assert sha(Path(p))==h
  manifest=Path(b['candidateInputManifest']);assert sha(manifest)==b['candidateInputManifestSha256'];source=Path(b['sourcePath'])
  for row in read(manifest):assert sha(source/row['path'])==row['sha256'],row['path']
  r.update(stage='fresh-complete-backup',apks=apks);save()
  with (ROOT/'out/session-a/prepare425-backup.log').open('w') as f:backup=subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(OUT),'--previous',str(previous)],stdout=f,stderr=subprocess.STDOUT,cwd=ROOT)
  assert backup.returncode==0
  game=next(p for p in apks if Path(p).name=='app-debug.apk');test=next(p for p in apks if Path(p).name=='app-debug-androidTest.apk')
  command=[sys.executable,str(HELPER),'install-test','--output',str(OUT),'--apk',game,'--test-apk',test,'--observe-workers','--runner','SessionAScenarioPrepareInstrumentation']
  r.update(stage='actual-source0-two-previews-read-worker-observation',command=command);save()
  with (OUT/'driver.log').open('w') as f:result=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT,cwd=ROOT)
  final=read(OUT/'session.json');assert final['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in final['restoration'].values()) and not live_case(OUT)
  r.update(stage='restored-verified',functionalPreviewPassed=bool(final.get('passed')),helperExit=result.returncode,restoration=final['restoration'],scope='Only actual source0 two preview read-worker/scene-cpu250ms checkpoint observation, full Save/RNG/Token pure and complete original restore.600s cap is diagnosis, original120s host/renderer remains same. No normal16/cold/ARM/native events scores transferred.');save()
 except BaseException as e:r.update(stage='stopped-preserve-real-evidence',error=str(e));save();raise
if __name__=='__main__':main()
