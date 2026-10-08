#!/usr/bin/env python3
"""Freeze terminal exact new fire cohort; read-only, no absent heap invention."""
from pathlib import Path
import hashlib,json,re,subprocess,time
from read_session_state import read_session_state
from run_candidate_regression287 import completed,live_case
from audit_session_memory import meminfo_samples
ROOT=Path(__file__).resolve().parents[4]
DOC=Path(__file__).resolve().parent
CASE=ROOT/'out/session-a/map-fire-upload-installed286'
OUTPUT=DOC/'CANDIDATE_FIRE_ACCEPTANCE288.json'
def sha(p):
 d=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1048576),b''):d.update(b)
 return d.hexdigest()
def main():
 assert not OUTPUT.exists()
 print('Read-only waiting actual286 terminal restoration and owner exit',flush=True)
 while True:
  state=read_session_state(CASE/'session.json')
  if state['stage']=='restored-verified' and not live_case(CASE):break
  time.sleep(5)
 build=read_session_state(DOC/'MAP_FIRE_UPLOAD_BUILD269.json')
 apks={x['path']:x['sha256'] for x in build['apks']}
 state=completed(CASE,apks)
 assert state['normalPassed'] and state['firePauseSystemAnimationRestored']
 assert all(sha(Path(p))==h for p,h in apks.items())
 assert {k:v['files'] for k,v in state['restoration'].items()}=={'internal':9,'external':3797}
 normal=(CASE/'instrumentation.txt').read_text();cold=(CASE/'cold-instrumentation.txt').read_text()
 assert 'PASS SESSION A FIRE normal' in normal and 'FAIL' not in normal
 assert 'PASS SESSION A FIRE COLD' in cold and 'FAIL' not in cold
 progress=(CASE/'evidence/progress.txt').read_text()
 labels=['01-player-fire','02-low-quality-fire','03-player-extinguished','04-real-reignite','05-real-expired','06-real-burning-readback']
 for label in labels:assert 'PASS current native source fire target lifecycle '+label in progress
 assert 'PASS real B full-turn expiry removes burning' in progress
 assert 'PASS normal burning load exact full Save/RNG' in progress
 index=Path(state['videoObservation']['path']);video=read_session_state(index)
 assert sha(index)==state['videoObservation']['finalIndexSha256'] and video['apks']==apks
 assert video['parts'] and not video['captureLimitReachedBeforeRestoration']
 for row in video['parts']:assert sha(Path(row['path']))==row['sha256']==row['deviceSha256']
 process=state['coldProcess'];pids={str(process['beforePid']),str(process['afterPid'])}
 timeline=CASE/'meminfo-timeline.txt';samples=meminfo_samples(timeline.read_text(errors='replace'),pids)
 assert samples
 child_path=CASE/'native-workers.jsonl';children=[]
 for i,line in enumerate(child_path.read_text().splitlines()):
  row=json.loads(line);assert row['apks']==apks
  children.append({'sampleIndex':i,'time':row['time'],'sourceChildren':row['sourceChildren'],'batchPssKiB':sum(x['KiB']['Pss'] for x in row['sourceChildren'])})
 assert children
 evidence=[]
 for folder in ('evidence','cold-evidence'):
  for path in sorted((CASE/folder).rglob('*')):
   if path.is_file():evidence.append({'path':str(path.relative_to(CASE)),'sha256':sha(path),'bytes':path.stat().st_size})
 report={'actualCase':str(CASE),'actualSessionSha256':sha(CASE/'session.json'),'apks':apks,'sourceIndex':14,
  'normalColdFullRestorationPassed':True,'coldProcess':process,'restoration':state['restoration'],
  'firePauseSystemAnimationRestored':True,'videoObservation':state['videoObservation'],'workerObservation':state['workerObservation'],
  'evidenceFiles':evidence,'processPssSampleCount':len(samples),'independentProcessPssPeak':max(samples,key=lambda x:x['pssKiB']),
  'meminfoTimelineSha256':sha(timeline),'sourceChildSampleCount':len(children),'independentSourceChildPssPeak':max(children,key=lambda x:x['batchPssKiB']),
  'nativeWorkerTimelineSha256':sha(child_path),'javaAllocatorPeakBytes':None,'javaTelemetryReason':'Fire runner emits no memory.csv or Runtime allocator samples; Dalvik PSS is not Java used heap.',
  'gpuPeakBytes':None,'memoryBudgetClosed':False,'original354832AllocationStackKnown':False,
  'fullFireChainsFacilityArenaAccepted':False,'all16NormalCallersAccepted':False,'armAccepted':False,'wholeGoalComplete':False,
  'scope':'Exact independently installed269/API29/x86_64 Source14 actual normal deploy/cancel/fire/Home/reduced-motion/quality/extinguish/reignite/whole-turn expiry/save/load/newPID/native controller13 and full original SHA restoration. PSS samples independent; no sum, GPU, FPS, original PC pixel or ARM conclusion. Long whole-turn foreground latency remains open.'}
 OUTPUT.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({k:report[k] for k in ['normalColdFullRestorationPassed','processPssSampleCount','javaAllocatorPeakBytes','wholeGoalComplete']}),flush=True)
if __name__=='__main__':main()
