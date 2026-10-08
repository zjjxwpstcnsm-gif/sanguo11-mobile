#!/usr/bin/env python3
"""Fresh media cohort all16 normal callers after exact prior regression closes."""
from pathlib import Path
import json,subprocess,sys,time
from read_session_state import read_session_state
from run_remaining_normal_media import accepted,guard_caller_source,sha
from run_candidate_regression287 import completed,live_case
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/cache-all-callers305';RECEIPT=DOC/'CACHE_ALL_CALLERS305_LAUNCH.json';HELPER=DOC/'device_session.py'
def main():
 assert not OUT.exists() and not RECEIPT.exists();OUT.mkdir(parents=True)
 report={'stage':'waiting-new296-build-and-actual304-full-regressions-terminal','completedSources':[],'cases':{},'all16NormalCallersAccepted':False,'normalMiss58ProducerBound':False,'wholeGoalComplete':False,'armAccepted':False}
 def save():RECEIPT.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
 save();print(report['stage'],flush=True)
 try:
  while True:
   if not (DOC/'CACHE_REGRESSION304_LAUNCH.json').exists():time.sleep(5);continue
   previous=read_session_state(DOC/'CACHE_REGRESSION304_LAUNCH.json')
   if previous['stage']=='stopped-retain-actual-evidence':raise ValueError('Actual304 failed; no media installation')
   if previous['stage']=='specified-regressions-terminal-passed' and (DOC/'FIRE_CACHE_APK296.json').exists():break
   time.sleep(5)
  build=read_session_state(DOC/'FIRE_CACHE_APK296.json');assert build['buildSuccessful'];revision=build['canonicalBaseRevision'];expected_main=read_session_state(DOC/'INHERITANCE.json')['main'];apks={r['path']:r['sha256'] for r in build['apks']};prior=ROOT/'out/session-a/cache-regression304/return-large-source14';parent=read_session_state(DOC/'FIRE_CACHE_APK296.json');completed(prior,{r['path']:r['sha256'] for r in parent['apks']})
  report.update(apks=apks,canonicalBaseRevision=revision,cohortReceipt='FIRE_CACHE_APK296.json');save()
  for source in range(16):
   case=OUT/('source-'+str(source).zfill(2));guard_caller_source(revision,expected_main)
   for p,h in apks.items():assert sha(Path(p))==h
   report.update(stage='preparing-complete-backup',activeSource=source,actualCase=str(case));save()
   with (OUT/('source-'+str(source).zfill(2)+'-backup.log')).open('w') as f:r=subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(case),'--previous',str(prior)],cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
   assert r.returncode==0
   try:
    guard_caller_source(revision,expected_main)
    for p,h in apks.items():assert sha(Path(p))==h
   except BaseException:subprocess.run([sys.executable,str(HELPER),'restore','--output',str(case)],cwd=ROOT,check=True);raise
   game=next(p for p in apks if Path(p).name=='app-debug.apk');test=next(p for p in apks if Path(p).name=='app-debug-androidTest.apk')
   command=[sys.executable,str(HELPER),'install-test','--output',str(case),'--apk',game,'--test-apk',test,*(['--reuse-installed'] if source else []),'--observe-workers','--runner','SessionAMapRepairInstrumentation','--suite','mediaAll16','--begin',str(source),'--end',str(source+1),'--fresh-process-reopen'];report.update(stage='actual-normal-all-person-caller-running',command=command);save()
   with (case/'driver.log').open('w') as f:r=subprocess.run(command,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
   actual,proof=accepted(case,source);completed(case,apks);assert r.returncode==0 and not live_case(case)
   assert proof['expectedOfficers']==670 and len(proof['rows'])==1340
   assert all(x['attachedDrawableFullRectangleSameAs'] and 0<x['drawnWidth']<=1024 and 0<x['drawnHeight']<=1024 for x in proof['rows'])
   report['completedSources'].append(source);report['cases'][str(source)]={'actualCase':str(case),'actualOfficers':670,'actualRosterDetailCallers':1340,'attachedDrawableFullRectangleSameAs':True,'normalColdFullShaRestoration':True,'proofSha256':sha(case/'evidence'/('source-'+str(source)+'-portrait-callers.json'))};save();prior=case
  report.update(stage='all16-normal-roster-detail-rectangle-terminal-passed',all16NormalCallersAccepted=True,scope='Exact new296 fullall16 x670person/1340 actual roster-detail caller and Drawable Canvas dimensions/current-year original PNG/identity checks plus normal save/newPID/current3D/full original SHA restoration. Not Windows framebuffer/PC small-family/allage boundaries/fullscreen/voice/mapBGM/miss58 real producer/finalB/ARM acceptance. Whole goal remains open.');save()
 except Exception as e:report.update(stage='stopped-retain-actual-evidence',error=str(e),cleanupNotInferred=True);save();raise
if __name__=='__main__':main()
