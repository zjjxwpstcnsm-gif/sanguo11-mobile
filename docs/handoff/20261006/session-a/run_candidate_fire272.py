#!/usr/bin/env python3
"""Serial actual normal fire installation after current caller owner restoration."""
from pathlib import Path
import json,subprocess,sys,time,hashlib,shlex
from run_remaining_normal_media import accepted,guard_caller_source
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';PREVIOUS=ROOT/'out/session-a/registered-normal239-all16-unique268/source-00';OUT=ROOT/'out/session-a/map-fire-upload-installed272';RECEIPT=DOC/'CANDIDATE_FIRE272_LAUNCH.json'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 assert not RECEIPT.exists() and not OUT.exists();build=json.loads((DOC/'MAP_FIRE_UPLOAD_BUILD269.json').read_text());assert build['buildSuccessful'] and build['fixedPackagedInputsExact']==168 and len(build['variantSourceDiffersFromCanonical'])==4;inherit=json.loads((DOC/'INHERITANCE.json').read_text());expected_main=inherit['main'];guard_caller_source(build['sourceRevision'],expected_main)
 for row in build['apks']:assert sha(Path(row['path']))==row['sha256']
 report={'previousNormalSource':str(PREVIOUS),'newCandidate':'MAP_FIRE_UPLOAD_BUILD269.json','apks':build['apks'],'stage':'waiting-current-source0-normal-cold-fullSHA-owner-exits','previousOwnerPids':[20713,20790,20791],'oldScoresTransferred':False,'wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(report,indent=2)+'\n')
 save();print(report['stage'],flush=True)
 try:
  while True:
   state=json.loads((PREVIOUS/'session.json').read_text());live=[]
   for pid in report['previousOwnerPids']:
    p=subprocess.run(['ps','-p',str(pid),'-o','command='],capture_output=True,text=True)
    if p.returncode==0 and any(str(PREVIOUS) in a for a in shlex.split(p.stdout.strip())):live.append(pid)
   if not live:
    accepted(PREVIOUS,0);assert all(state[k]['exitCode']==0 for k in ['videoObservation','workerObservation']);break
   time.sleep(5)
  guard_caller_source(build['sourceRevision'],expected_main)
  for row in build['apks']:assert sha(Path(row['path']))==row['sha256']
  report['stage']='preparing-complete-backup';save();helper=DOC/'device_session.py'
  with (PREVIOUS.parent/'candidate-fire272-backup.log').open('w') as f:r=subprocess.run([sys.executable,str(helper),'reuse-backup','--output',str(OUT),'--previous',str(PREVIOUS)],cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
  assert r.returncode==0
  try:
   guard_caller_source(build['sourceRevision'],expected_main)
   for row in build['apks']:assert sha(Path(row['path']))==row['sha256']
  except BaseException:subprocess.run([sys.executable,str(helper),'restore','--output',str(OUT)],cwd=ROOT,check=True);raise
  game=next(x['path'] for x in build['apks'] if Path(x['path']).name=='app-debug.apk');test=next(x['path'] for x in build['apks'] if Path(x['path']).name=='app-debug-androidTest.apk');command=[sys.executable,str(helper),'install-test','--output',str(OUT),'--apk',game,'--test-apk',test,'--observe-workers','--runner','SessionAFireFlowInstrumentation','--fresh-process-reopen','--pause-fire'];report['stage']='new-apk-normal-fire-running';report['command']=command;save()
  with (PREVIOUS.parent/'candidate-fire272-install.log').open('w') as f:r=subprocess.run(command,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
  final=json.loads((OUT/'session.json').read_text());assert final['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in final['restoration'].values());report['stage']='restored-verified';report['normalAndColdPassed']=final.get('passed',False);report['helperExit']=r.returncode;save();raise SystemExit(r.returncode)
 except Exception as error:report['stage']='stopped-preserve-inspect-actual-state';report['error']=str(error);report['cleanupNotInferred']=True;save();raise
if __name__=='__main__':main()
