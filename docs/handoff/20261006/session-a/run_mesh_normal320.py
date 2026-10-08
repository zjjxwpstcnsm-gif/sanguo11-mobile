#!/usr/bin/env python3
"""New316 normal source14/new/zoom/save/read/cold then real fire; no old scores."""
from pathlib import Path
import json,subprocess,sys,time
from read_session_state import read_session_state
from run_cache_regression304 import live_case,completed
from run_remaining_normal_media import sha,guard_caller_source
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/mesh-normal320';PREVIOUS=ROOT/'out/session-a/mesh-prepare-installed317';RECEIPT=DOC/'MESH_NORMAL320_LAUNCH.json';HELPER=DOC/'device_session.py'
def main():
 assert not OUT.exists() and not RECEIPT.exists();report={'stage':'waiting-actual317-318-fullSHA-owners-exit','cases':[],'oldScoresTransferred':False,'all16NormalMediaAccepted':False,'armAccepted':False,'wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(report,indent=2)+'\n')
 save();print(report['stage'],flush=True)
 try:
  while not (DOC/'MESH_PREPARE318.json').exists():
   launch=read_session_state(DOC/'MESH_PREPARE317_LAUNCH.json')
   if launch['stage']=='stopped-preserve-actual-state':raise ValueError('317 gate failed; no new device action')
   time.sleep(5)
  a=read_session_state(DOC/'MESH_PREPARE318.json');assert a['functional120sPreviewAccepted'];assert not live_case(PREVIOUS)
  build=read_session_state(DOC/'MESH_ALLOCATION_BUILD316.json');assert build['buildSuccessful'];apks={x['path']:x['sha256'] for x in build['apks']};completed(PREVIOUS,apks,cold=False)
  expected=read_session_state(DOC/'INHERITANCE.json')['main'];OUT.mkdir(parents=True);previous=PREVIOUS
  for name,runner,args in [('source14','SessionAMapRepairInstrumentation',['--suite','all','--begin','14','--end','15','--fresh-process-reopen']),('real-fire','SessionAFireFlowInstrumentation',['--fresh-process-reopen'])]:
   guard_caller_source(build['sourceRevision'],expected)
   for p,h in apks.items():assert sha(Path(p))==h
   case=OUT/name;report.update(stage='preparing-complete-backup-'+name,activeCase=str(case));save()
   with (OUT/(name+'-backup.log')).open('w') as f:r=subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(case),'--previous',str(previous)],cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
   assert r.returncode==0
   try:
    guard_caller_source(build['sourceRevision'],expected)
    for p,h in apks.items():assert sha(Path(p))==h
   except BaseException:subprocess.run([sys.executable,str(HELPER),'restore','--output',str(case)],check=True);raise
   game=next(p for p in apks if Path(p).name=='app-debug.apk');test=next(p for p in apks if Path(p).name=='app-debug-androidTest.apk');command=[sys.executable,str(HELPER),'install-test','--output',str(case),'--apk',game,'--test-apk',test,'--reuse-installed','--observe-workers','--runner',runner,*args];report.update(stage='actual-running-'+name,command=command);save()
   with (case/'driver.log').open('w') as f:r=subprocess.run(command,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
   state=completed(case,apks,cold=True);assert r.returncode==0
   report['cases'].append({'name':name,'case':str(case),'apks':apks,'normalAndDifferentPidColdPassed':True,'originalEveryFileShaRestored':True});save();previous=case
  report.update(stage='stated-normal-cold-cases-passed',scope='Exact316 source14 actual menu/new/zoom/pan/Home/orientation/save/load/newPID and realfire lifecycle separate cases; not all16 sources/callers/ordinaryheap/fullmedia/ARM/finalB or unique OOM root.');save()
 except Exception as e:report.update(stage='stopped-retain-actual-evidence',error=str(e),cleanupNotInferred=True);save();raise
if __name__=='__main__':main()
