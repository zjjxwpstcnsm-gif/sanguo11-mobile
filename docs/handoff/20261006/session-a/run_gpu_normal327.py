#!/usr/bin/env python3
"""New326 actual normal repeated source14/new/zoom/save/read/cold then real fire; no old scores."""
from pathlib import Path
import json,subprocess,sys,time
from read_session_state import read_session_state
from run_cache_regression304 import live_case,completed
from run_remaining_normal_media import sha,guard_caller_source
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/gpu-normal327';PREVIOUS=ROOT/'out/session-a/mesh-normal320/source14';RECEIPT=DOC/'GPU_NORMAL327_LAUNCH.json';HELPER=DOC/'device_session.py'
def main():
 assert not OUT.exists() and not RECEIPT.exists();report={'stage':'waiting-actual326-build-owner-and-failed320-fullSHA-exit','cases':[],'oldScoresTransferred':False,'all16NormalMediaAccepted':False,'armAccepted':False,'wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(report,indent=2)+'\n')
 save();print(report['stage'],flush=True)
 try:
  while not (DOC/'GPU_INDEX_BUILD326.json').exists():time.sleep(5)
  # Receipt is written before the build process exits; do not overlap new
  # native-frame acceptance with the owning compiler/hash process.
  from run_prepare_diagnostic311 import compiling as old_compiling
  import shlex
  while any('docs/handoff/20261006/session-a/build_gpu_indices326.py' in shlex.split(line.strip().split(None,1)[1]) or str(DOC/'build_gpu_indices326.py') in shlex.split(line.strip().split(None,1)[1]) for line in subprocess.check_output(['ps','-axo','pid=,command='],text=True).splitlines() if len(line.strip().split(None,1))==2):time.sleep(5)
  previous=read_session_state(PREVIOUS/'session.json');assert previous['stage']=='restored-verified' and not previous['passed'] and all(x['exactRegularFileSha'] for x in previous['restoration'].values()) and not live_case(PREVIOUS);assert all(previous[k]['exitCode']==0 for k in ['videoObservation','workerObservation'])
  build=read_session_state(DOC/'GPU_INDEX_BUILD326.json');assert build['buildSuccessful'];apks={x['path']:x['sha256'] for x in build['apks']}
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
   game=next(p for p in apks if Path(p).name=='app-debug.apk');test=next(p for p in apks if Path(p).name=='app-debug-androidTest.apk');command=[sys.executable,str(HELPER),'install-test','--output',str(case),'--apk',game,'--test-apk',test,*(['--reuse-installed'] if name!='source14' else []),'--observe-workers','--runner',runner,*args];report.update(stage='actual-running-'+name,command=command);save()
   with (case/'driver.log').open('w') as f:r=subprocess.run(command,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
   state=completed(case,apks,cold=True);assert r.returncode==0
   report['cases'].append({'name':name,'case':str(case),'apks':apks,'normalAndDifferentPidColdPassed':True,'originalEveryFileShaRestored':True});save();previous=case
  report.update(stage='stated-normal-cold-cases-passed',scope='Exact326 source14 actual menu/new/zoom/pan/Home/orientation/save/load/newPID and realfire lifecycle separate cases; not all16 sources/callers/ordinaryheap/fullmedia/ARM/finalB or unique OOM root.');save()
 except Exception as e:report.update(stage='stopped-retain-actual-evidence',error=str(e),cleanupNotInferred=True);save();raise
if __name__=='__main__':main()
