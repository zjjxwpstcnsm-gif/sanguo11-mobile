#!/usr/bin/env python3
"""Actual successful7 and failed14, fully restored; no restarted scenario test."""
from pathlib import Path
import json
from read_session_state import read_session_state
from run_remaining_normal_media import sha
from run_candidate_regression287 import live_case,completed
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
def main():
 output=DOC/'PREVIEW_FAILURE308.json';assert not output.exists();launch=read_session_state(DOC/'CACHE_REGRESSION304_LAUNCH.json');assert launch['stage']=='stopped-retain-actual-evidence' and len(launch['cases'])==1
 build=read_session_state(DOC/'FIRE_CACHE_APK296.json');apks={x['path']:x['sha256'] for x in build['apks']};good=ROOT/'out/session-a/cache-regression304/large-source07';completed(good,apks);bad=ROOT/'out/session-a/cache-regression304/large-source14';state=read_session_state(bad/'session.json');assert state['stage']=='restored-verified' and not state['passed'] and not live_case(bad)
 assert all(x['exactRegularFileSha'] for x in state['restoration'].values()) and {k:v['files'] for k,v in state['restoration'].items()}=={'internal':9,'external':3797}
 assert all(state[k]['exitCode']==0 for k in ['videoObservation','workerObservation']);assert not state.get('coldProcess')
 result=bad/'evidence/result.txt';text=result.read_text();assert 'Normal widget unavailable' in text and 'host(SessionAMapRepairInstrumentation.java:' in text and '选择PC来源剧本 Media/scenario/Scen014.S11' in text
 raw=(bad/'logcat.txt').read_text(errors='replace');assert 'OutOfMemoryError' not in raw and 'Original map effects stopped' not in raw and 'Native5s watchdog' not in raw
 assert read_session_state(DOC/'CACHE_ALL_CALLERS305_LAUNCH.json')['stage']=='stopped-retain-actual-evidence'
 assert not list((ROOT/'out/session-a/cache-all-callers305').glob('*/session.json'))
 rows=[]
 for p in sorted((bad/'evidence').rglob('*')):
  if p.is_file():rows.append({'path':str(p.relative_to(bad)),'sha256':sha(p),'bytes':p.stat().st_size})
 report={'actualApks':apks,'source07':{'actualCase':str(good),'normalColdFullShaPassed':True,'memoryReportPath':str(good/'memory-evidence.json'),'memoryReportSha256':sha(good/'memory-evidence.json')},'source14':{'actualCase':str(bad),'passed':False,'coldInvoked':False,'sessionSha256':sha(bad/'session.json'),'restoration':state['restoration'],'failureResult':text,'evidenceFiles':rows,'logcatSha256':sha(bad/'logcat.txt')},'viewedFailureImage':'Still real pending dialog 正在读取剧本… with cancel; no faction preview yet. Native renderer initialization later visible in log. No independent allocating/worker stack captured.','rootCauseProven':False,'javaOOMObserved':False,'nativeWatchdogObserved':False,'allRemaining304CasesAccepted':False,'any305NormalCallerCaseStarted':False,'scope':'Actual new296 source7 normal menu/preview/new/zoom/pan/Home/orientation/save/exit/newPID plus full original9+3797 restoration passed. Source14 actual source click accepted then120s host await failed while preparation dialog remained; whole source14 and remaining normal/ordinary/audio/all16 callers not accepted. Must observe actual read worker/callback/loading and latency without relaxing functional threshold or changing rules; all artifacts retained.','wholeGoalComplete':False}
 output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'source07Passed':True,'source14Passed':False,'allOriginalFilesRestored':True,'rootCauseProven':False}),flush=True)
if __name__=='__main__':main()
