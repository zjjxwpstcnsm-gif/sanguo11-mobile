#!/usr/bin/env python3
"""Actually install changed cache candidate only after build owner fully exits."""
from pathlib import Path
import json,subprocess,sys,time,shlex
from read_session_state import read_session_state
from run_remaining_normal_media import sha,guard_caller_source
from run_candidate_regression287 import live_case
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
PREVIOUS=ROOT/'out/session-a/map-fire-upload-installed286';OUT=ROOT/'out/session-a/fire-cache-installed297';RECEIPT=DOC/'FIRE_CACHE_INSTALL297_LAUNCH.json';HELPER=DOC/'device_session.py'
def own_build_active():
 for line in subprocess.check_output(['ps','-axo','pid=,command='],text=True).splitlines():
  parts=line.strip().split(None,1)
  if len(parts)!=2:continue
  args=shlex.split(parts[1])
  if any(str(DOC/n) in args or 'docs/handoff/20261006/session-a/'+n in args for n in ['build_fire_cache_apk296.py','build_fire_cache_candidate295.py','probe_fire_cache294.py','export_media_source292.py']):return True
 return False
def main():
 assert not OUT.exists() and not RECEIPT.exists();report={'stage':'waiting-actual296-build-and-own-build-owner-exit','previousFailedCase':str(PREVIOUS),'changedCandidate':'FIRE_CACHE_APK296.json','previousScoresTransferred':False,'watchdogRootCauseProven':False,'wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(report,indent=2)+'\n')
 save();print(report['stage'],flush=True)
 try:
  while not (DOC/'FIRE_CACHE_APK296.json').exists() or own_build_active():time.sleep(5)
  build=read_session_state(DOC/'FIRE_CACHE_APK296.json');assert build['buildSuccessful'];prior=read_session_state(PREVIOUS/'session.json');assert prior['stage']=='restored-verified' and not prior['passed'] and all(x['exactRegularFileSha'] for x in prior['restoration'].values()) and not live_case(PREVIOUS)
  assert {k:v['files'] for k,v in prior['restoration'].items()}=={'internal':9,'external':3797}
  assert all(prior[k]['exitCode']==0 for k in ['videoObservation','workerObservation'])
  apks={r['path']:r['sha256'] for r in build['apks']};expected=read_session_state(DOC/'INHERITANCE.json')['main'];guard_caller_source(build['sourceRevision'],expected)
  for p,h in apks.items():assert sha(Path(p))==h
  report.update(stage='preparing-complete-backup',apks=apks);save()
  log=ROOT/'out/session-a/fire-cache-install297-backup.log'
  with log.open('w') as f:r=subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(OUT),'--previous',str(PREVIOUS)],cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
  assert r.returncode==0
  try:
   guard_caller_source(build['sourceRevision'],expected)
   for p,h in apks.items():assert sha(Path(p))==h
  except BaseException:subprocess.run([sys.executable,str(HELPER),'restore','--output',str(OUT)],cwd=ROOT,check=True);raise
  game=next(p for p in apks if Path(p).name=='app-debug.apk');test=next(p for p in apks if Path(p).name=='app-debug-androidTest.apk');command=[sys.executable,str(HELPER),'install-test','--output',str(OUT),'--apk',game,'--test-apk',test,'--observe-workers','--runner','SessionAFireFlowInstrumentation','--fresh-process-reopen','--pause-fire'];report.update(stage='changed-new-apk-actual-normal-fire-running',command=command,ownBuildActiveAtLaunch=own_build_active());assert not report['ownBuildActiveAtLaunch'];save()
  with (OUT/'driver.log').open('w') as f:r=subprocess.run(command,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
  final=read_session_state(OUT/'session.json');assert final['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in final['restoration'].values());report.update(stage='restored-verified',normalAndColdPassed=final.get('passed',False),helperExit=r.returncode);save();raise SystemExit(r.returncode)
 except Exception as e:report.update(stage='stopped-preserve-inspect-actual-state',error=str(e),cleanupNotInferred=True);save();raise
if __name__=='__main__':main()
