#!/usr/bin/env python3
"""Actual320 failed second-preview state, complete restoration and raw evidence."""
from pathlib import Path
import json,re
from read_session_state import read_session_state
from run_cache_regression304 import live_case
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent;CASE=ROOT/'out/session-a/mesh-normal320/source14'
def main():
 out=DOC/'MESH_REPEAT_FAILURE323.json';assert not out.exists();state=read_session_state(CASE/'session.json');assert state['stage']=='restored-verified' and not state['passed'] and not live_case(CASE)
 assert {k:v['files'] for k,v in state['restoration'].items()}=={'internal':9,'external':3797} and all(x['exactRegularFileSha'] for x in state['restoration'].values());assert all(state[k]['exitCode']==0 for k in ['videoObservation','workerObservation'])
 build=read_session_state(DOC/'MESH_ALLOCATION_BUILD316.json');apks={r['path']:r['sha256'] for r in build['apks']};assert state['apks']==apks
 for p,h in apks.items():assert sha(Path(p))==h
 result=(CASE/'evidence/result.txt').read_text();assert 'FAIL actual3D verified output and completed terrain' in result and 'dismissed preview releases engine, detachedWorld and ground' in result
 raw=(CASE/'logcat.txt').read_text(errors='replace');pid=state.get('normalProcess',{}).get('pid') or '11174';lines=raw.splitlines();errors=[x for x in lines if re.search(r'\s'+pid+r'\s+\d+.*(?:OutOfMemoryError|FATAL EXCEPTION|Original map effects stopped)',x)]
 video=read_session_state(Path(state['videoObservation']['path']));assert sha(Path(state['videoObservation']['path']))==state['videoObservation']['finalIndexSha256'];assert video['apks']==apks
 for part in video['parts']:assert sha(Path(part['path']))==part['sha256']==part['deviceSha256']
 report={'case':str(CASE),'apks':apks,'normalPassed':False,'coldExecuted':False,'failure':'second actual Scen014 preview ready120s; first submitted renderer125507ms after creation. PNG looks rendered by delayed failure capture; does not override120s readiness failure.','actualResult':result,'actualGroundFirstSubmissionRows':[x for x in lines if re.search(r'\s'+pid+r'\s+\d+',x) and ('Ground CPU ready' in x or 'Field CPU ready' in x or 'First submission (not visibility proof)' in x)],'actualTargetErrorLines':errors,'rootCauseProven':False,'originalUserOOMStackKnown':False,'restoration':state['restoration'],'videoObservation':state['videoObservation'],'workerObservation':state['workerObservation'],'everyVideoPartDeviceHostShaVerified':True,'firstPreviewReleaseVerified':True,'allLater320Fire321FieldworksStoppedBeforeAction':True,'sessionSha256':sha(CASE/'session.json'),'evidenceFiles':[{'path':str(p.relative_to(CASE)),'sha256':sha(p),'bytes':p.stat().st_size} for p in sorted((CASE/'evidence').rglob('*')) if p.is_file()],'scope':'Exact new316 repeated normal source14 preview/cancel/reselect FAIL. Full original9+3797SHA restored; observed Java/native checkpoint is not whole allocator/GPU peak. No unmodified retry, no timeout relaxation, no fire/cold/all16/ARM scores inferred.','wholeGoalComplete':False}
 out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'normalPassed':False,'actualTargetErrorLines':errors,'restoredFiles':{k:v['files'] for k,v in state['restoration'].items()}}))
if __name__=='__main__':main()
