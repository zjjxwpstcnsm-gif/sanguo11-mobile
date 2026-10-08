#!/usr/bin/env python3
"""Actual fresh combined ce83 and268 source14 normal fire lifecycle/save/cold; no earlier A-only fire scores."""
from pathlib import Path
import json,subprocess,sys,time
from read_session_state import read_session_state as read
from stage_serial401 import ROOT,D,sha
from run_music_reserve378 import live_case
OUT=ROOT/'out/session-a/combined-fire428';RECEIPT=D/'COMBINED_FIRE428_LAUNCH.json';HELPER=D/'device_session.py'
def main():
 assert not OUT.exists() and not RECEIPT.exists();r={'stage':'waiting-new-combined-build-terminal','previousScoresTransferred':False,'wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(r,indent=2)+'\n')
 save()
 try:
  while not (D/'SERIAL_TEST_BUILD416.json').exists():time.sleep(5)
  b=read(D/'SERIAL_TEST_BUILD416.json');assert b['buildSuccessful'] and b['buildExit']==0 and b['fullSourceShaUnchangedAfterBuild']
  previous=ROOT/'out/session-a/prepare425';state=read(previous/'session.json');assert state['stage']=='restored-verified' and not state['passed'] and all(x['exactRegularFileSha'] for x in state['restoration'].values()) and not live_case(previous)
  apks={x['path']:x['sha256'] for x in b['apks']}
  for p,h in apks.items():assert sha(Path(p))==h
  manifest=Path(b['candidateInputManifest']);assert sha(manifest)==b['candidateInputManifestSha256'];source=Path(b['sourcePath'])
  for row in read(manifest):assert sha(source/row['path'])==row['sha256'],row['path']
  r.update(stage='fresh-complete-backup',apks=apks);save()
  with (ROOT/'out/session-a/combined-fire428-backup.log').open('w') as f:backup=subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(OUT),'--previous',str(previous)],stdout=f,stderr=subprocess.STDOUT,cwd=ROOT)
  assert backup.returncode==0
  game=next(p for p in apks if Path(p).name=='app-debug.apk');test=next(p for p in apks if Path(p).name=='app-debug-androidTest.apk')
  command=[sys.executable,str(HELPER),'install-test','--output',str(OUT),'--apk',game,'--test-apk',test,'--observe-workers','--runner','SessionAFireFlowInstrumentation','--fresh-process-reopen','--pause-fire']
  r.update(stage='actual-combined-source14-normal-fire-and-cold',command=command);save()
  with (OUT/'driver.log').open('w') as f:result=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT,cwd=ROOT)
  final=read(OUT/'session.json');assert final['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in final['restoration'].values()) and not live_case(OUT)
  r.update(stage='restored-verified',normalColdFirePassed=bool(final.get('passed')),helperExit=result.returncode,restoration=final['restoration'],scope='Only new combined ce83/268 normal source14 two deploy/player fire/Home/lowquality/extinguish/reignite/fullturn expiry/burning save/read/newprocess/allRNG. Known first-source0 strict preparationFAIL remains separate, not overall stability or all16/ARM/facility/native contest/media acceptance.');save()
 except BaseException as e:r.update(stage='stopped-preserve-real-evidence',error=str(e));save();raise
if __name__=='__main__':main()
