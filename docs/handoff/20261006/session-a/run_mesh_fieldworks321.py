#!/usr/bin/env python3
"""Fresh same316 real menu/march/build/stop/repair/multiturn/read/cold visual check."""
from pathlib import Path
import json,subprocess,sys,time
from read_session_state import read_session_state
from run_cache_regression304 import live_case,completed
from run_remaining_normal_media import sha,guard_caller_source
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/mesh-fieldworks321';RECEIPT=DOC/'MESH_FIELDWORKS321_LAUNCH.json';HELPER=DOC/'device_session.py'
def main():
 assert not OUT.exists() and not RECEIPT.exists();report={'stage':'waiting-actual320-normal-fire-cold-fullSHA-owner-exit','previousScoresTransferred':False,'wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(report,indent=2)+'\n')
 save();print(report['stage'],flush=True)
 try:
  while True:
   prior=read_session_state(DOC/'MESH_NORMAL320_LAUNCH.json')
   if prior['stage']=='stopped-retain-actual-evidence':raise ValueError('Actual320 failed; do not skip gate or repeat it unchanged')
   if prior['stage']=='stated-normal-cold-cases-passed':break
   time.sleep(5)
  build=read_session_state(DOC/'MESH_ALLOCATION_BUILD316.json');apks={x['path']:x['sha256'] for x in build['apks']};previous=Path(prior['cases'][-1]['case']);completed(previous,apks,True)
  expected=read_session_state(DOC/'INHERITANCE.json')['main'];guard_caller_source(build['sourceRevision'],expected)
  for p,h in apks.items():assert sha(Path(p))==h
  report.update(stage='preparing-complete-backup',apks=apks);save()
  with (ROOT/'out/session-a/fieldworks321-backup.log').open('w') as f:r=subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(OUT),'--previous',str(previous)],cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
  assert r.returncode==0
  try:
   guard_caller_source(build['sourceRevision'],expected)
   for p,h in apks.items():assert sha(Path(p))==h
  except BaseException:subprocess.run([sys.executable,str(HELPER),'restore','--output',str(OUT)],check=True);raise
  game=next(p for p in apks if Path(p).name=='app-debug.apk');test=next(p for p in apks if Path(p).name=='app-debug-androidTest.apk');cmd=[sys.executable,str(HELPER),'install-test','--output',str(OUT),'--apk',game,'--test-apk',test,'--reuse-installed','--observe-workers','--runner','SessionAScenePresentationInstrumentation','--fresh-process-reopen'];report.update(stage='actual-normal-fieldworks-running',command=cmd);save()
  with (OUT/'driver.log').open('w') as f:r=subprocess.run(cmd,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
  state=completed(OUT,apks,True);assert r.returncode==0;report.update(stage='normal-cold-fullSHA-passed',normalAndColdPassed=True,scope='Only actual Source14 menu/deploy/march/build/stop/repair/completion/save/read/recreate/newPID and A fact/render/state checks of frozen normal runner. Not every facility/fire-chain/native rules/source/climate/model/audio/ARM or finalB.');save()
 except Exception as e:report.update(stage='stopped-retain-actual-evidence',error=str(e),cleanupNotInferred=True);save();raise
if __name__=='__main__':main()
