#!/usr/bin/env python3
"""Freeze real two-pass preparation strict failures, allocator samples and restore."""
from pathlib import Path
import json,time,csv,re
from read_session_state import read_session_state
from run_cache_regression304 import live_case
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent;CASE=ROOT/'out/session-a/repeat-prepare-installed331';OUTPUT=DOC/'REPEAT_PREPARE332.json'
def main():
 assert not OUTPUT.exists();print('Waiting actual331 owners/observers/wholeSHA terminal',flush=True)
 while True:
  launch=read_session_state(DOC/'REPEAT_PREPARE331_LAUNCH.json')
  if launch['stage']=='stopped-preserve-actual-state':raise ValueError('331 gate failed, no invented evidence')
  if (CASE/'session.json').exists():
   state=read_session_state(CASE/'session.json')
   if state['stage']=='restored-verified' and not live_case(CASE):break
  time.sleep(5)
 build=read_session_state(DOC/'REPEAT_TEST_BUILD330.json');apks={x['path']:x['sha256'] for x in build['apks']};assert state['apks']==apks
 assert {k:v['files'] for k,v in state['restoration'].items()}=={'internal':9,'external':3797} and all(x['exactRegularFileSha'] for x in state['restoration'].values());assert all(state[k]['exitCode']==0 for k in ['videoObservation','workerObservation'])
 for p,h in apks.items():assert sha(Path(p))==h
 summary=read_session_state(CASE/'evidence/repeat-summary.json');observations=[json.loads(x) for x in (CASE/'evidence/repeat-observations.jsonl').read_text().splitlines()];assert observations
 if 'passes' in summary:
  for p in summary['passes']:
   assert p['functionalHostLimitMillis']==p['functionalRendererAfterHostLimitMillis']==120000
   expected=p['firstFocusedPreviewMillis']>=0 and p['firstFocusedPreviewMillis']<=120000 and p['firstVerified3DMillis']>=p['firstFocusedPreviewMillis'] and p['firstVerified3DMillis']-p['firstFocusedPreviewMillis']<=120000
   assert p['functionalAccepted']==expected and p['fullSaveRngTokenPure']
  assert state['passed']==summary['functionalBothAccepted']
 samples=list(csv.DictReader((CASE/'evidence/allocator-samples.csv').open()));assert samples
 valid=[x for x in samples if x['consistent']=='1' and int(x['javaUsed'])>=0];assert valid
 for x in valid:assert int(x['javaUsed'])==int(x['javaTotal'])-int(x['freeBytes']) and int(x['javaTotal'])==int(x['totalAfter']) and int(x['javaLimit'])==536870912
 peak=max(valid,key=lambda x:int(x['javaUsed']));native=max(samples,key=lambda x:int(x['nativeAllocated']));video=read_session_state(Path(state['videoObservation']['path']));assert sha(Path(state['videoObservation']['path']))==state['videoObservation']['finalIndexSha256'] and video['apks']==apks
 for part in video['parts']:assert sha(Path(part['path']))==part['sha256']==part['deviceSha256']
 raw=(CASE/'logcat.txt').read_text(errors='replace');pids=re.findall(r'ActivityManager: Start proc (\d+):game\.sanguo\.mobile\.dev/[^\s]+ for added application game\.sanguo\.mobile\.dev(?:\n|$)',raw);assert pids
 errors=[x for x in raw.splitlines() if any(re.search(r'\s'+p+r'\s+\d+.*(?:OutOfMemoryError|FATAL EXCEPTION|Original map effects stopped)',x) for p in pids)]
 report={'apks':apks,'game326Unchanged':True,'case':str(CASE),'strictTwoPreparationPassesAccepted':state['passed'],'summary':summary,'actualObservationRows':observations,'allocatorSamples':len(samples),'consistentAllocatorSamples':len(valid),'inconsistentRawSamplesRetained':len(samples)-len(valid),'sampledJavaPeakRow':peak,'independentSampledNativeAllocatorPeakRow':native,'sampledPeaksNotInstantaneousOrGpu':True,'noForcedGcOrHeapDump':True,'actualTargetErrorLines':errors,'restoration':state['restoration'],'videoObservation':state['videoObservation'],'workerObservation':state['workerObservation'],'sessionSha256':sha(CASE/'session.json'),'evidenceFiles':[{'path':str(p.relative_to(CASE)),'sha256':sha(p),'bytes':p.stat().st_size} for p in sorted((CASE/'evidence').rglob('*')) if p.is_file()],'scope':'Two actual Source14 normal menu choose/preview/cancel and repeat, strict120s host and120s render-after-host.600s observations never treated as gate relaxation.250ms sampler/stack/video overhead explicit, instantaneous/GPU/native child allocator unknown. NewWorld/zoom/fire/cold/all16/voice/ARM/finalB not accepted from diagnostic.','rootCauseProven':False,'wholeGoalComplete':False}
 OUTPUT.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'strictTwoPreparationPassesAccepted':state['passed'],'summary':summary,'sampledJavaPeakRow':peak,'independentSampledNativeAllocatorPeakRow':native}),flush=True)
if __name__=='__main__':main()
