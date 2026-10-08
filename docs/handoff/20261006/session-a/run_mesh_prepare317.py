#!/usr/bin/env python3
"""Fresh complete316 normal source14 preparation; wait actual build exit/full restore."""
from pathlib import Path
import json,subprocess,sys,time,shlex
from read_session_state import read_session_state
from run_remaining_normal_media import sha,guard_caller_source
from run_candidate_regression287 import live_case
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/mesh-prepare-installed317';PREVIOUS=ROOT/'out/session-a/prepare-diagnostic-installed311';RECEIPT=DOC/'MESH_PREPARE317_LAUNCH.json';HELPER=DOC/'device_session.py'
def compiling():
 for line in subprocess.check_output(['ps','-axo','pid=,command='],text=True).splitlines():
  p=line.strip().split(None,1)
  if len(p)==2:
   args=shlex.split(p[1])
   if str(DOC/'build_mesh_allocation316.py') in args or 'docs/handoff/20261006/session-a/build_mesh_allocation316.py' in args:return True
 return False
def main():
 assert not OUT.exists() and not RECEIPT.exists();report={'stage':'waiting-actual316-build-owner-exit','previousFailedActualCase':str(PREVIOUS),'changedOnlyMeshAllocation':True,'functional120sLimitUnchanged':True,'previousScoresTransferred':False,'wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(report,indent=2)+'\n')
 save();print(report['stage'],flush=True)
 try:
  while not (DOC/'MESH_ALLOCATION_BUILD316.json').exists() or compiling():time.sleep(5)
  previous=read_session_state(PREVIOUS/'session.json');assert previous['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in previous['restoration'].values()) and not live_case(PREVIOUS);assert all(previous[k]['exitCode']==0 for k in ['videoObservation','workerObservation'])
  build=read_session_state(DOC/'MESH_ALLOCATION_BUILD316.json');assert build['buildSuccessful'];apks={r['path']:r['sha256'] for r in build['apks']};expected=read_session_state(DOC/'INHERITANCE.json')['main'];guard_caller_source(build['sourceRevision'],expected)
  for p,h in apks.items():assert sha(Path(p))==h
  report.update(stage='preparing-complete-backup',apks=apks);save()
  with (ROOT/'out/session-a/mesh317-backup.log').open('w') as f:r=subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(OUT),'--previous',str(PREVIOUS)],cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
  assert r.returncode==0
  try:
   guard_caller_source(build['sourceRevision'],expected)
   for p,h in apks.items():assert sha(Path(p))==h
  except BaseException:subprocess.run([sys.executable,str(HELPER),'restore','--output',str(OUT)],cwd=ROOT,check=True);raise
  game=next(p for p in apks if Path(p).name=='app-debug.apk');test=next(p for p in apks if Path(p).name=='app-debug-androidTest.apk');command=[sys.executable,str(HELPER),'install-test','--output',str(OUT),'--apk',game,'--test-apk',test,'--observe-workers','--runner','SessionAScenarioPrepareInstrumentation'];report.update(stage='actual-new316-source14-diagnostic-running',command=command);save()
  with (OUT/'driver.log').open('w') as f:r=subprocess.run(command,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
  final=read_session_state(OUT/'session.json');assert final['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in final['restoration'].values());report.update(stage='restored-verified',helperExit=r.returncode,functionalDiagnosticPassed=final.get('passed',False));save();raise SystemExit(r.returncode)
 except Exception as e:report.update(stage='stopped-preserve-actual-state',error=str(e),cleanupNotInferred=True);save();raise
if __name__=='__main__':main()
