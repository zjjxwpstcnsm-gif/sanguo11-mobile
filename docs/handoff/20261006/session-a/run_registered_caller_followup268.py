#!/usr/bin/env python3
"""Wait for actual Source4 full restore/owner exits before exact-cohort callers."""
from pathlib import Path
import json,subprocess,sys,time
from run_remaining_normal_media import accepted,frozen_caller_cohort,guard_caller_source
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';CASE=ROOT/'out/session-a/search-observation242/source04-diagnostic';OUT=ROOT/'out/session-a/registered-normal239-all16-unique268';RECEIPT=DOC/'REGISTERED_NORMAL239_QUEUE268.json'
def main():
 assert not RECEIPT.exists() and not OUT.exists()
 build=json.loads((DOC/'SEARCH_OBSERVATION_TEST_BUILD239.json').read_text());artifacts={x['path']:x['sha256'] for x in build['apks']};receipt,revision=frozen_caller_cohort(artifacts);expected_main=json.loads((DOC/'INHERITANCE.json').read_text())['main'];guard_caller_source(revision,expected_main)
 state=json.loads((CASE/'session.json').read_text());assert state['apks']==artifacts and state['serial']=='emulator-5554' and state['runId']=='session_a_source04_diagnostic'
 report={'anchor':str(CASE),'initialSource':4,'cohort':receipt,'apks':artifacts,'stage':'waiting-actual-source4-cold-full-restoration-owner-exits','completedSources':[],'old176ScoresTransferred':False,'sourceRevision':revision,'expectedMain':expected_main,'wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(report,indent=2)+'\n')
 save();print(report['stage'],flush=True)
 try:
  while True:
   state=json.loads((CASE/'session.json').read_text());assert state['apks']==artifacts
   owners=[5024,5061,5062]
   live=[]
   for pid in owners:
    p=subprocess.run(['ps','-p',str(pid),'-o','command='],capture_output=True,text=True)
    if p.returncode==0 and str(CASE) in p.stdout:live.append(pid)
   if state['stage']=='restored-verified' and not live:
    accepted(CASE,4);assert all(state[k]['exitCode']==0 for k in ['videoObservation','workerObservation']);break
   time.sleep(5)
  guard_caller_source(revision,expected_main);report['stage']='remaining15-running';report['completedSources']=[4];report['anchorOwnerPidsTerminated']=owners;save()
  command=[sys.executable,str(DOC/'run_remaining_normal_media.py'),'--previous-completed',str(CASE),'--initial-source','4','--output',str(OUT)];report['command']=command;save()
  with (CASE.parent/'caller-followup268.log').open('w') as f:r=subprocess.run(command,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
  report['driverExit']=r.returncode;report['stage']='remaining-driver-terminal';report['batchPath']=str(OUT/'batch.json')
  if (OUT/'batch.json').exists():batch=json.loads((OUT/'batch.json').read_text());report['completedSources']=batch['completedSources'];report['completeAll16NormalCallers']=batch['completeAll16NormalCallers']
  save();raise SystemExit(r.returncode)
 except Exception as error:
  report['stage']='stopped-before-followup-or-driver-failed';report['error']=str(error);report['deviceCleanupNotInferred']=True;save();raise
if __name__=='__main__':main()
