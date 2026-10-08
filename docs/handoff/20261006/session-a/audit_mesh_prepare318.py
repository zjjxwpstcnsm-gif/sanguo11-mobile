#!/usr/bin/env python3
"""Freeze terminal actual diagnostic outcome, strict latency and complete restore."""
from pathlib import Path
import json,time,re
from read_session_state import read_session_state
from run_candidate_regression287 import live_case
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
CASE=ROOT/'out/session-a/mesh-prepare-installed317';OUTPUT=DOC/'MESH_PREPARE318.json'
def main():
 assert not OUTPUT.exists();print('Waiting actual317 diagnostic and all owner/observers/fullSHA restoration',flush=True)
 while True:
  launch=read_session_state(DOC/'MESH_PREPARE317_LAUNCH.json')
  if launch['stage']=='stopped-preserve-actual-state':raise ValueError('Diagnostic gate failed; no invented observations')
  if (CASE/'session.json').exists():
   state=read_session_state(CASE/'session.json')
   if state['stage']=='restored-verified' and not live_case(CASE):break
  time.sleep(5)
 build=read_session_state(DOC/'MESH_ALLOCATION_BUILD316.json');apks={r['path']:r['sha256'] for r in build['apks']};assert state['apks']==apks and not state.get('coldProcess')
 assert {k:v['files'] for k,v in state['restoration'].items()}=={'internal':9,'external':3797} and all(x['exactRegularFileSha'] for x in state['restoration'].values())
 assert all(state[k]['exitCode']==0 for k in ['videoObservation','workerObservation'])
 for p,h in apks.items():assert sha(Path(p))==h
 summary=read_session_state(CASE/'evidence/prepare-summary.json');observations=[json.loads(x) for x in (CASE/'evidence/prepare-observations.jsonl').read_text().splitlines()];assert observations and all(row['javaLimitBytes']==536870912 for row in observations)
 complete=summary.get('observationComplete',False)
 if complete:
  assert summary['sourceIndex']==14 and summary['functionalHostLimitMillis']==120000 and summary['observationOnlyLimitMillis']==600000 and summary['fullSaveRngTokenPure']
  expected=summary['firstFocusedPreviewMillis']>=0 and summary['firstFocusedPreviewMillis']<=120000 and summary['firstVerified3DMillis']>=0
  assert summary['functionalSource14Accepted']==expected
  assert state['passed']==expected
 video=state['videoObservation'];index=Path(video['path']);assert sha(index)==video['finalIndexSha256'];record=read_session_state(index);assert record['apks']==apks and record['parts'] and not record['captureLimitReachedBeforeRestoration']
 for part in record['parts']:assert sha(Path(part['path']))==part['sha256']==part['deviceSha256']
 raw=(CASE/'logcat.txt').read_text(errors='replace');pidset=set(re.findall(r'ActivityManager: Start proc (\d+):game\.sanguo\.mobile\.dev/[^\s]+ for added application game\.sanguo\.mobile\.dev(?:\n|$)',raw))
 target_errors=[line for line in raw.splitlines() if any(re.search(r'\s'+pid+r'\s+\d+.*(?:OutOfMemoryError|FATAL EXCEPTION|Original map effects stopped)',line) for pid in pidset)]
 files=[{'path':str(p.relative_to(CASE)),'sha256':sha(p),'bytes':p.stat().st_size} for p in sorted((CASE/'evidence').rglob('*')) if p.is_file()]
 report={'actualCase':str(CASE),'sessionSha256':sha(CASE/'session.json'),'apks':apks,'newGame316Installed':True,'diagnosticObservationComplete':complete,'functional120sPreviewAccepted':state['passed'],'summary':summary,'observationRows':observations,'restoration':state['restoration'],'videoObservation':video,'workerObservation':state['workerObservation'],'actualErrorLogLines':target_errors,'evidenceFiles':files,'rootCauseProven':False,'allNormalSource14NewZoomColdPassed':False,'scope':'Actual one source14 normal selection and real UiReadTask/worker-stack/focused-preview/verified3D times; strict120s unchanged,600s observation only. No World/RNG injection or core changes; A allocation-only product change314, raw parity315. Full original9+3797 SHA restore and exact new game and test APKs verified. Includes encoder/stack/screenshot overhead; not blanket source14 normal new/zoom/cold/media/all16/ARM or unique OOM proof.','wholeGoalComplete':False}
 OUTPUT.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'functional120sPreviewAccepted':state['passed'],'summary':summary,'wholeGoalComplete':False}),flush=True)
if __name__=='__main__':main()
