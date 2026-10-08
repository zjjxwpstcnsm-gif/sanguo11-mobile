#!/usr/bin/env python3
"""Actual new369 full16/cold/SHA/video scoped audit, no old scores."""
from pathlib import Path
import json,time
from read_session_state import read_session_state as read
from run_remaining_normal_media import sha
from run_music_reserve378 import live_case
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent;CASE=ROOT/'out/session-a/reserve-map380'
def main():
 out=D/'RESERVE_MAP381.json';assert not out.exists()
 while True:
  launch=read(D/'RESERVE_MAP380_LAUNCH.json')
  if launch['stage'] in ['actual-terminal-full-sha-verified','stopped-preserve-real-evidence']:
   assert (CASE/'session.json').exists(),'Stopped before actual device case; no invented acceptance'
   state=read(CASE/'session.json')
   if state['stage']=='restored-verified' and not live_case(CASE):break
  time.sleep(5)
 assert all(x['exactRegularFileSha'] for x in state['restoration'].values());assert state['workerObservation']['exitCode']==0;video=state.get('videoObservation');assert video and video['exitCode']==0
 evidence=CASE/'evidence';normalFiles=[str(p.relative_to(evidence)) for p in sorted(evidence.glob('source-*.sg11'))];accepted=bool(state.get('passed')) and bool(state.get('coldProcess',{}).get('passed')) and state['coldProcess']['differentPid'];assert not accepted or len(normalFiles)==16
 report={'actualNew369Apks':state['apks'],'actualFull16AndFinalColdAccepted':accepted,'actualSavedNormalSourceFiles':normalFiles,'gameRestoration':state['restoration'],'actualColdProcess':state.get('coldProcess'),'wholeMusicGateKeptSeparate':read(D/'MUSIC_RESERVE379.json')['wholeNormalMenuTrackAccepted'],'workerObservation':state['workerObservation'],'videoObservation':video,'evidenceFiles':[{'path':str(p.relative_to(CASE)),'sha256':sha(p),'bytes':p.stat().st_size} for p in sorted(evidence.rglob('*')) if p.is_file()],'originalFunctionalThresholdsUnchanged':True,'scope':'Exact new369 actual full16 normal source preview/cancel/newgame/zoom/pan/Home/direction/manualsave/load plus final-source fresh PID if passed. Not each-source670caller/voice/fullscreen/allfactions/ordinary384/commands/multiturn/new fire/complete memory or GPU/ARM/finalB. Source14-first failures remain, matrix warmed sequentially does not override them.','wholeGoalComplete':False};out.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'actualFull16AndFinalColdAccepted':accepted,'actualNormalSourceSaves':len(normalFiles)}))
if __name__=='__main__':main()
