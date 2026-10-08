#!/usr/bin/env python3
"""Freeze exact395 actual source fire/cold/full original SHA/video outcomes."""
from pathlib import Path
import json,time,re
from read_session_state import read_session_state as read
from run_remaining_normal_media import sha
from run_music_reserve378 import live_case
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent;CASE=ROOT/'out/session-a/current-fire395'
def main():
 out=D/'CURRENT_FIRE396.json';assert not out.exists()
 while True:
  launch=read(D/'CURRENT_FIRE395_LAUNCH.json')
  if launch['stage'] in ['restored-verified','stopped-preserve-real-evidence']:
   assert (CASE/'session.json').exists(),'Stopped before device fire case; no invented lifecycle'
   state=read(CASE/'session.json')
   if state['stage']=='restored-verified' and not live_case(CASE):break
  time.sleep(5)
 build=read(D/'MUSIC_RESERVE_BUILD369.json');apks={x['path']:x['sha256'] for x in build['apks']};assert state['apks']==apks and all(x['exactRegularFileSha'] for x in state['restoration'].values());assert state['videoObservation']['exitCode']==state['workerObservation']['exitCode']==0
 video=read(CASE/'video/video.json');assert video['apks']==apks and video['parts'] and not video['captureLimitReachedBeforeRestoration']
 for part in video['parts']:assert sha(Path(part['path']))==part['sha256']==part['deviceSha256']==part['deviceSha256AfterPull']
 progress=(CASE/'evidence/progress.txt').read_text() if (CASE/'evidence/progress.txt').exists() else '';raw=(CASE/'logcat.txt').read_text(errors='replace');labels=['01-player-fire','02-low-quality-fire','03-player-extinguished','04-real-reignite','05-real-expired','06-real-burning-readback'];checks={k:'PASS current native source fire target lifecycle '+k in progress for k in labels}
 accepted=bool(state.get('normalPassed') and state.get('passed') and state.get('coldProcess',{}).get('passed') and state['coldProcess']['differentPid']);failures=[s for s in raw.splitlines() if 'Original map effects stopped' in s or 'Native5s watchdog' in s or 'OutOfMemoryError' in s]
 if accepted:assert all(checks.values()) and 'PASS normal burning load exact full Save/RNG' in progress and 'PASS real B full-turn expiry removes burning' in progress and not failures and state['firePauseSystemAnimationRestored']
 files=[{'path':str(p.relative_to(CASE)),'sha256':sha(p),'bytes':p.stat().st_size} for folder in ['evidence','cold-evidence','interrupted-evidence'] for p in sorted((CASE/folder).rglob('*')) if p.is_file()]
 report={'actual369Apks':apks,'normalColdCompleteOriginalRestorationAccepted':accepted,'sixActualLifecycleChecks':checks,'actualNormalOutput':state.get('testOutput'),'actualColdProcess':state.get('coldProcess'),'restoration':state['restoration'],'systemAnimationRestored':state.get('firePauseSystemAnimationRestored'),'rawEffectOomWatchdogLines':failures,'videoIndexSha256':sha(CASE/'video/video.json'),'rawVideoParts':len(video['parts']),'rawSessionSha256':sha(CASE/'session.json'),'evidenceFiles':files,'scope':'Only fresh exact369 normal player Source14 fire lifecycle/native13 and new PID/full Save/bothRNG/token purity if passed. Other fire chains/traps/ships/all facilities/128 capacity performance/map BGM/voice/miss58/default mixedPCM/ARM/finalB/instantaneous memory/GPU unknown. Old299 outcome never transferred, failure/recovery not accepted.','wholeGoalComplete':False};out.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'normalColdAccepted':accepted,'sixLifecycleChecks':checks}))
if __name__=='__main__':main()
