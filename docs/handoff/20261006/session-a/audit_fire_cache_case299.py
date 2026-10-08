#!/usr/bin/env python3
"""Wait actual297/298 complete then freeze exact pass/fail without borrowed scores."""
from pathlib import Path
import json,time,re
from read_session_state import read_session_state
from run_remaining_normal_media import sha
from run_candidate_regression287 import live_case
from audit_session_memory import meminfo_samples
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
CASE=ROOT/'out/session-a/fire-cache-installed297';OUTPUT=DOC/'FIRE_CACHE_ACCEPTANCE299.json'
def main():
 assert not OUTPUT.exists();print('Waiting same actual297 owner/worker and separate298 recorder terminal',flush=True)
 while True:
  state=read_session_state(CASE/'session.json');video=read_session_state(DOC/'FIRE_CACHE_VIDEO298_LAUNCH.json')
  if state['stage']=='restored-verified' and video['stage']=='terminal' and not live_case(CASE):break
  time.sleep(5)
 build=read_session_state(DOC/'FIRE_CACHE_APK296.json');apks={r['path']:r['sha256'] for r in build['apks']};assert state['apks']==video['apks']==apks
 assert {k:v['files'] for k,v in state['restoration'].items()}=={'internal':9,'external':3797} and all(x['exactRegularFileSha'] for x in state['restoration'].values())
 assert state['firePauseSystemAnimationRestored'] and state['workerObservation']['exitCode']==0
 assert video['observerExit']==0 and video['startupOmitted']
 for p,h in apks.items():assert sha(Path(p))==h
 index=Path(video['indexPath']);assert sha(index)==video['indexSha256'];record=read_session_state(index)
 assert record['apks']==apks and record['parts'] and not record['captureLimitReachedBeforeRestoration']
 for part in record['parts']:assert sha(Path(part['path']))==part['sha256']==part['deviceSha256'] and 'error' not in part
 progress=(CASE/'evidence/progress.txt').read_text();diagnosis=(CASE/'evidence/diagnosis.txt').read_text();raw=(CASE/'logcat.txt').read_text(errors='replace')
 passcase=bool(state.get('normalPassed') and state.get('passed') and state.get('coldProcess',{}).get('passed') and state.get('coldProcess',{}).get('differentPid'))
 labels=['01-player-fire','02-low-quality-fire','03-player-extinguished','04-real-reignite','05-real-expired','06-real-burning-readback'];checks={k:'PASS current native source fire target lifecycle '+k in progress for k in labels}
 if passcase:assert all(checks.values()) and 'PASS normal burning load exact full Save/RNG' in progress and 'PASS real B full-turn expiry removes burning' in progress
 checkpoints=[{'javaHeapUsedBytes':int(a),'javaHeapLimitBytes':int(b),'nativeHeapAllocatedBytes':int(c)} for a,b,c in re.findall(r'javaHeapUsedBytes=(\d+) javaHeapLimitBytes=(\d+) nativeHeapAllocatedBytes=(\d+)',diagnosis)]
 assert checkpoints and all(0<=x['javaHeapUsedBytes']<=x['javaHeapLimitBytes']==536870912 for x in checkpoints)
 pidset=set(re.findall(r'ActivityManager: Start proc (\d+):game\.sanguo\.mobile\.dev/[^\s]+ for added application game\.sanguo\.mobile\.dev(?:\n|$)',raw));assert pidset
 timeline=CASE/'meminfo-timeline.txt';samples=meminfo_samples(timeline.read_text(errors='replace'),pidset);assert samples
 failures=[line for line in raw.splitlines() if 'Original map effects stopped' in line or 'Native5s watchdog' in line or re.search(r'\s(?:'+ '|'.join(pidset)+r')\s+\d+.*OutOfMemoryError',line)]
 if passcase:assert not failures
 files=[{'path':str(p.relative_to(CASE)),'sha256':sha(p),'bytes':p.stat().st_size} for folder in ['evidence','cold-evidence'] for p in sorted((CASE/folder).rglob('*')) if p.is_file()]
 report={'actualCase':str(CASE),'actualSessionSha256':sha(CASE/'session.json'),'apks':apks,'normalColdFullRestorationPassed':passcase,'normalOutput':state.get('testOutput'),'coldProcess':state.get('coldProcess'),'actualLifecycleChecks':checks,'restoration':state['restoration'],'systemAnimationPreferenceRestored':True,'workerObservation':state['workerObservation'],'videoSidecar':video,'runtimeDiagnosticCheckpointCount':len(checkpoints),'largestObservedRuntimeCheckpoint':max(checkpoints,key=lambda x:x['javaHeapUsedBytes']),'javaScope':'Sparse actual MapHost Runtime checkpoints, not continuous allocator peak; no memory.csv fabrication.','processPssSampleCount':len(samples),'independentProcessPssPeak':max(samples,key=lambda x:x['pssKiB']),'meminfoTimelineSha256':sha(timeline),'actualErrorLogLines':failures,'evidenceFiles':files,'watchdogRootCauseProven':False,'memoryBudgetClosed':False,'gpuPeakBytes':None,'all16NormalCallersAccepted':False,'normalMiss58ProducerBound':False,'armAccepted':False,'wholeGoalComplete':False,'scope':'Only exact newly installed296/cache295 normal Source14 fire lifecycle. Independent current-case outputs; prior293 failure retained, no skipped controller error/recovery-page acceptance. Video298 starts later, omits startup and adds encoder pressure; own build exited before launch. All rules Save/RNG/StateToken pure checks and full original restoration required; no original PC/art/wholemedia/finalB/ARM or universal watchdog-cause conclusion.'}
 OUTPUT.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:report[k] for k in ['normalColdFullRestorationPassed','actualLifecycleChecks','runtimeDiagnosticCheckpointCount','wholeGoalComplete']}),flush=True)
if __name__=='__main__':main()
