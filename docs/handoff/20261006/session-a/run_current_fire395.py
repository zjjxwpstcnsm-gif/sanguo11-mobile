#!/usr/bin/env python3
"""Exact369 fresh normal player grid fire/expiry/save/cold after392 terminal restoration."""
from pathlib import Path
import json,subprocess,sys,time
from read_session_state import read_session_state as read
from run_remaining_normal_media import sha
from guard_music_reserve377 import guard_music_reserve
from run_music_reserve378 import live_case
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/current-fire395';PRE=ROOT/'out/session-a/matrix-redo392';HELPER=D/'device_session.py';RECEIPT=D/'CURRENT_FIRE395_LAUNCH.json'
def main():
 assert not OUT.exists() and not RECEIPT.exists();report={'stage':'waiting-exact392-normal-cold-allSHA-owner-exit','previousScoresTransferred':False,'wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(report,indent=2)+'\n')
 save()
 try:
  while not (D/'MATRIX_REDO393.json').exists():time.sleep(5)
  audit=read(D/'MATRIX_REDO393.json');assert audit['actualFull16AndFinalColdAccepted'],'392 matrix failed; no fire device action'
  prior=read(PRE/'session.json');assert prior['stage']=='restored-verified' and prior['passed'] and all(x['exactRegularFileSha'] for x in prior['restoration'].values()) and prior['videoObservation']['exitCode']==prior['workerObservation']['exitCode']==0
  while live_case(PRE):time.sleep(5)
  build=read(D/'MUSIC_RESERVE_BUILD369.json');expected=read(D/'INHERITANCE.json')['main'];guard_music_reserve(build['sourceRevision'],expected);apks={x['path']:x['sha256'] for x in build['apks']}
  for p,h in apks.items():assert sha(Path(p))==h
  report.update(stage='fresh-complete-backup',apks=apks);save()
  with (ROOT/'out/session-a/current-fire395-backup.log').open('w') as f:r=subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(OUT),'--previous',str(PRE)],cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
  assert r.returncode==0
  try:
   guard_music_reserve(build['sourceRevision'],expected)
   for p,h in apks.items():assert sha(Path(p))==h
  except BaseException:subprocess.run([sys.executable,str(HELPER),'restore','--output',str(OUT)],cwd=ROOT,check=True);raise
  game=next(p for p in apks if Path(p).name=='app-debug.apk');test=next(p for p in apks if Path(p).name=='app-debug-androidTest.apk');command=[sys.executable,str(HELPER),'install-test','--output',str(OUT),'--apk',game,'--test-apk',test,'--reuse-installed','--observe-workers','--runner','SessionAFireFlowInstrumentation','--fresh-process-reopen','--pause-fire'];report.update(stage='actual-normal-new369-player-fire-running',command=command);save()
  with (OUT/'driver.log').open('w') as f:r=subprocess.run(command,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
  final=read(OUT/'session.json');assert final['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in final['restoration'].values()) and final['videoObservation']['exitCode']==final['workerObservation']['exitCode']==0 and not live_case(OUT)
  report.update(stage='restored-verified',normalAndColdPassed=bool(final.get('passed')),helperExit=r.returncode,restoration=final['restoration'],scope='Current exact369 real normal Source14/deploy/cancel/player-fire/Home/pause/lowquality/extinguish/reignite/full-turn expiry/save/read/newPID; no fabricated burning snapshot or rule RNG. Not other fire chains/128 performance/all facilities/PCM/ARM/finalB, old299 scores not transferred.');save()
 except BaseException as e:report.update(stage='stopped-preserve-real-evidence',error=str(e));save();raise
if __name__=='__main__':main()
