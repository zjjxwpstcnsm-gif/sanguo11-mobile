#!/usr/bin/env python3
"""Freeze actual menu capture/whole-track gate and every game/test file restore."""
from pathlib import Path
import json,time
from read_session_state import read_session_state
from run_cache_regression304 import live_case
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent;CASE=ROOT/'out/session-a/capture-queue366/large-menu-pcm';OUT=DOC/'CAPTURE_QUEUE367.json'
def main():
 assert not OUT.exists();print('Waiting actual366 terminal and complete game/test restoration/owner exit',flush=True)
 while True:
  launch=read_session_state(DOC/'CAPTURE_QUEUE366_LAUNCH.json')
  if launch['stage'] in ['specified-regressions-terminal-passed','stopped-retain-actual-evidence']:
   if not (CASE/'session.json').exists():raise ValueError('Gate stopped before case; no fabricated capture')
   state=read_session_state(CASE/'session.json')
   if state['stage']=='restored-verified' and not live_case(CASE):break
  time.sleep(5)
 build=read_session_state(DOC/'CAPTURE_QUEUE_BUILD363.json');previousBuild=read_session_state(DOC/'REPEAT_TEST_BUILD330.json');apks={x['path']:x['sha256'] for x in build['apks']};assert state['apks']==apks
 assert {k:v['files'] for k,v in state['restoration'].items()}=={'internal':9,'external':3797} and all(x['exactRegularFileSha'] for x in state['restoration'].values());assert state['workerObservation']['exitCode']==0
 c=state['audioCapture'];assert c['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in c['restoration'].values());assert c['modeBefore']=='default' and 'RECORD_AUDIO: default' in c['appOpAfter'];assert not c['grantedBefore'];assert sha(Path(c['previousApkPath']))==c['previousApkSha256']==next(x['sha256'] for x in previousBuild['apks'] if Path(x['path']).name=='app-debug-androidTest.apk')
 captures=c['captures'];track=next((x for x in captures if x['run'].endswith('_menu')),None);wavePath=CASE/'whole-music-check.json';wave=read_session_state(wavePath) if wavePath.exists() else None;
 if track:
  import wave as wav_module
  raw=Path(track['hostPath'])/'android-mix.wav'
  assert sha(raw)==track['result']['wavSha256']
  with wav_module.open(str(raw),'rb') as w:assert w.getnframes()==track['result']['frames'] and w.getnchannels()==2 and w.getsampwidth()==2 and w.getframerate()==track['rate']
  assert raw.stat().st_size==44+track['result']['bytes'] and track['result']['diskBufferBytes']==524288 and track['result']['actualRecorderBufferFrames']>0
 if track:assert track['result']['writerBytes']==track['result']['bytes'] and 0<track['result']['diskQueueHighWater']<=64 and track['result']['diskQueueSlots']==64 and track['result']['diskQueueBytes']==524288
 accepted=launch['stage']=='specified-regressions-terminal-passed';assert not accepted or (track and wave and wave['status']=='PASS' and wave['correlation']>=.995 and state.get('passed'))
 report={'apks':apks,'wholeNormalMenuTrackAccepted':accepted,'launchStage':launch['stage'],'gameUiNormalPassed':state.get('passed',False),'captures':captures,'waveformResult':wave,'actualFullWaveformFailureLog':(CASE/'whole-music-check.log').read_text() if (CASE/'whole-music-check.log').exists() else None,'waveformThresholdUnchanged':.995,'gameRestoration':state['restoration'],'testRestoration':c['restoration'],'recorderBufferRequestedFrames':track['rate']*2 if track else None,'recorderBufferActualFrames':track['result'].get('actualRecorderBufferFrames') if track else None,'requestedBufferIsNotAssumedEffective':True,'testApkRestoredSha256':c['previousApkSha256'],'recordPermissionRestoredFalse':True,'appOpRestoredDefault':True,'microphoneRestorationReceiptPresent':False,'workerObservation':state['workerObservation'],'sessionSha256':sha(CASE/'session.json'),'wholeWaveformEvidenceSha256':sha(wavePath) if wavePath.exists() else None,'evidenceFiles':[{'path':str(p.relative_to(CASE)),'sha256':sha(p),'bytes':p.stat().st_size} for p in sorted((CASE/'evidence').rglob('*')) if p.is_file()],'source14FailuresNotOverridden':True,'scope':'Current exact326+363 normal menu original2238 whole wave/continuity0.995 and pureSave/RNG/StateToken if passed; no test playback directive. Full game/test/APK/audio permission/appop/mic restoration required. Not Windows exactPCM/mapBGM/voice/miss/other original event sounds/all16/normalSource14/newWorld/cold/native rules/GPU/ARM/finalB. Old-22 unique cause unresolved.','wholeGoalComplete':False}
 OUT.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'wholeNormalMenuTrackAccepted':accepted,'waveformResult':wave}),flush=True)
if __name__=='__main__':main()
