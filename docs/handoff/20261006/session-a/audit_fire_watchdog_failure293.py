#!/usr/bin/env python3
"""Freeze actual failed fire lifecycle and complete restoration; never accept it."""
from pathlib import Path
import json,re,subprocess
from run_remaining_normal_media import sha
from read_session_state import read_session_state
from run_candidate_regression287 import live_case
from audit_session_memory import meminfo_samples
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
CASE=ROOT/'out/session-a/map-fire-upload-installed286';OUTPUT=DOC/'FIRE_WATCHDOG_FAILURE293.json'
def main():
 assert not OUTPUT.exists();state=read_session_state(CASE/'session.json');assert state['stage']=='restored-verified' and not state['passed'] and not live_case(CASE)
 assert 'current native source fire target lifecycle 05-real-expired' in state['testOutput']
 assert not state.get('coldProcess');assert state['firePauseSystemAnimationRestored']
 assert {k:v['files'] for k,v in state['restoration'].items()}=={'internal':9,'external':3797}
 assert all(x['exactRegularFileSha'] for x in state['restoration'].values())
 assert all(state[k]['exitCode']==0 for k in ['videoObservation','workerObservation'])
 assert not Path('/tmp/sanguo11-emulator-5554-session-a.lock').exists()
 for p,h in state['apks'].items():assert sha(Path(p))==h
 raw=(CASE/'logcat.txt').read_text(errors='replace');needle='E Sanguo3D: Original map effects stopped';offset=raw.index(needle);window=raw[offset:offset+3600]
 assert 'Native5s watchdog deadline at32000100' in window and 'peak=278525' in window and 'mismatches=0' in window
 assert 'PC_CELL_INSTRUCTION_GUARD' not in window
 progress=(CASE/'evidence/progress.txt').read_text();assert 'PASS real B full-turn expiry removes burning' in progress
 diagnosis=(CASE/'evidence/diagnosis.txt').read_text();assert 'pc_map_fx_error=java.io.IOException:_PC_visual_child_EOF' in diagnosis
 checkpoints=[{'javaHeapUsedBytes':int(a),'javaHeapLimitBytes':int(b),'nativeHeapAllocatedBytes':int(c)} for a,b,c in re.findall(r'javaHeapUsedBytes=(\d+) javaHeapLimitBytes=(\d+) nativeHeapAllocatedBytes=(\d+)',diagnosis)]
 assert checkpoints and all(0<=x['javaHeapUsedBytes']<=x['javaHeapLimitBytes']==536870912 for x in checkpoints)
 samples=meminfo_samples((CASE/'meminfo-timeline.txt').read_text(errors='replace'),{'28727'})
 assert samples
 video_path=Path(state['videoObservation']['path']);video=read_session_state(video_path);assert sha(video_path)==state['videoObservation']['finalIndexSha256']
 for part in video['parts']:assert sha(Path(part['path']))==part['sha256']==part['deviceSha256']
 for name in ['CANDIDATE_REGRESSION287_LAUNCH.json','MEDIA_CALLERS291_LAUNCH.json']:
  gate=read_session_state(DOC/name);assert gate['stage']=='stopped-retain-actual-evidence'
 assert not list((ROOT/'out/session-a/candidate-regression287').glob('*/session.json')) and not list((ROOT/'out/session-a/media-callers291').glob('*/session.json'))
 files=[{'path':str(p.relative_to(CASE)),'sha256':sha(p),'bytes':p.stat().st_size} for p in sorted((CASE/'evidence').rglob('*')) if p.is_file()]
 report={'actualCase':str(CASE),'actualSessionSha256':sha(CASE/'session.json'),'apks':state['apks'],'passed':False,'normalFailure':state['testOutput'],'coldInvoked':False,'restoration':state['restoration'],'actualOwnersAndObserversExited':True,'videoObservation':state['videoObservation'],'workerObservation':state['workerObservation'],'systemAnimationPreferenceRestored':True,'failureLogWindow':window,'logcatSha256':sha(CASE/'logcat.txt'),'evidenceFiles':files,'runtimeDiagnosticCheckpointCount':len(checkpoints),'largestObservedRuntimeCheckpoint':max(checkpoints,key=lambda x:x['javaHeapUsedBytes']),'checkpointScope':'Sparse actual MapHost diagnostic Runtime readings; not continuous Java peak or memory.csv.','independentProcessPssSampleCount':len(samples),'independentProcessPssPeak':max(samples,key=lambda x:x['pssKiB']),'meminfoTimelineSha256':sha(CASE/'meminfo-timeline.txt'),'javaHeapOOMObservedInTargetLog':bool(re.search(r'^.*\s28727\s+\d+.*OutOfMemoryError.*$',raw,re.M)),'nativeFailure':'Native5s wall watchdog at adapter32000100; instruction peak278525 below5M, metadata mismatches0; Java EOF follows','rootCauseProven':False,'backgroundPauseCauseProven':False,'hostConcurrentBuildRecorded':True,'regression287AndMedia291InstalledAnyCase':False,'subsequent290And280AlsoUnaudited':'Both include same fire native candidate; no score transfer or bypass of failed fire gate.','scope':'Actual269 one-cell fire/Home/reduced-motion/quality/extinguish/reignite and two rule whole-turn expiry passed partially, source controller failed before expiry rendering/readback/cold. All user regular-file SHA restored. Native deadline failure is separate from original354832 Java OOM; actual host/guest scheduling/cache/lifecycle cause unresolved.','memoryBudgetClosed':False,'armAccepted':False,'wholeGoalComplete':False}
 OUTPUT.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:report[k] for k in ['passed','nativeFailure','runtimeDiagnosticCheckpointCount','regression287AndMedia291InstalledAnyCase','wholeGoalComplete']}),flush=True)
if __name__=='__main__':main()
