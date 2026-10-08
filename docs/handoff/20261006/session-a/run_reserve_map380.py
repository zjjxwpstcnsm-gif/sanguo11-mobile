#!/usr/bin/env python3
"""Independent new369 full16 normal map matrix; music failure is retained separately."""
from pathlib import Path
import json,subprocess,sys,time
from read_session_state import read_session_state as read
from run_remaining_normal_media import sha
from guard_music_reserve377 import guard_music_reserve
from run_music_reserve378 import live_case
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/reserve-map380';PRE=ROOT/'out/session-a/music-reserve378/large-menu-pcm';HELPER=D/'device_session.py';RECEIPT=D/'RESERVE_MAP380_LAUNCH.json'
def main():
 assert not OUT.exists() and not RECEIPT.exists();report={'stage':'waiting-exact378-all-game-test-sha-owner-exit','normal16Accepted':False,'wholeMusicAccepted':False,'armAccepted':False,'wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(report,indent=2)+'\n')
 save()
 try:
  while not (D/'MUSIC_RESERVE379.json').exists():time.sleep(5)
  audit=read(D/'MUSIC_RESERVE379.json');state=read(PRE/'session.json');assert state['stage']=='restored-verified' and audit['gameUiNormalPassed'] and all(x['exactRegularFileSha'] for x in state['restoration'].values()) and state['audioCapture']['stage']=='restored-verified';assert state['workerObservation']['exitCode']==0
  while live_case(PRE):time.sleep(5)
  build=read(D/'MUSIC_RESERVE_BUILD369.json');expected_main=read(D/'INHERITANCE.json')['main'];guard_music_reserve(build['sourceRevision'],expected_main);apks={x['path']:x['sha256'] for x in build['apks']};report.update(stage='fresh-complete-backup',actualWholeMusicGateAccepted=audit['wholeNormalMenuTrackAccepted'],wholeMusicAccepted=audit['wholeNormalMenuTrackAccepted'],apks=apks);save()
  r=subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(OUT),'--previous',str(PRE)],cwd=ROOT,capture_output=True,text=True);assert r.returncode==0,r.stdout+r.stderr
  try:
   guard_music_reserve(build['sourceRevision'],expected_main)
   for path,h in apks.items():assert sha(Path(path))==h
  except BaseException:subprocess.run([sys.executable,str(HELPER),'restore','--output',str(OUT)],cwd=ROOT,check=True);raise
  game=next(p for p in apks if Path(p).name=='app-debug.apk');test=next(p for p in apks if Path(p).name=='app-debug-androidTest.apk');command=[sys.executable,str(HELPER),'install-test','--output',str(OUT),'--apk',game,'--test-apk',test,'--test-only-update','--observe-workers','--runner','SessionAMapRepairInstrumentation','--suite','all','--begin','0','--end','16','--fresh-process-reopen'];report.update(stage='actual-full16-normal-map-running',command=command);save()
  with (OUT/'driver.log').open('w') as f:r=subprocess.run(command,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
  state=read(OUT/'session.json');assert state['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in state['restoration'].values());assert state['workerObservation']['exitCode']==0 and state['videoObservation']['exitCode']==0 and not live_case(OUT);report.update(stage='actual-terminal-full-sha-verified',normal16Accepted=bool(state.get('passed')),coldPassed=bool(state.get('coldProcess',{}).get('passed')),restoration=state['restoration'],driverExitCode=r.returncode);save()
 except BaseException as e:report.update(stage='stopped-preserve-real-evidence',error=str(e));save();raise
if __name__=='__main__':main()
