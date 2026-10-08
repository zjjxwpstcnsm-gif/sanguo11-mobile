#!/usr/bin/env python3
"""Fresh repaired APK heap/normal-flow/menu PCM tests, always full restoration."""
from pathlib import Path
import json,subprocess,sys,time,shlex,zipfile,hashlib
from run_remaining_normal_media import sha
from guard_capture_queue365 import guard_capture_queue as guard_caller_source
from read_session_state import read_session_state
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';PREVIOUS=ROOT/'out/session-a/capture-queue366/large-menu-pcm';OUT=ROOT/'out/session-a/mixer-isolation375';RECEIPT=DOC/'MIXER_ISOLATION375_LAUNCH.json';HELPER=DOC/'device_session.py'
def live_case(case):
 ps=subprocess.check_output(['ps','-axo','pid=,command='],text=True)
 for line in ps.splitlines():
  parts=line.strip().split(None,1)
  if len(parts)!=2:continue
  args=shlex.split(parts[1])
  if str(HELPER) in args and str(case) in args:return True
  if any(str(DOC/n) in args for n in ['observe_flow_video.py','observe_worker_memory.py']) and str(case/'session.json') in args:return True
 return False
def completed(case,apks,cold=True):
 d=read_session_state(case/'session.json');assert d['root']==str(ROOT) and d['serial']=='emulator-5554' and d['stage']=='restored-verified' and d.get('passed') and d['apks']==apks
 assert set(d['restoration'])=={'internal','external'} and all(x['exactRegularFileSha'] for x in d['restoration'].values())
 if cold:assert d['coldProcess']['passed'] and d['coldProcess']['differentPid']
 if d.get('workerObservation'):assert d['workerObservation']['exitCode']==0
 if d.get('videoObservation'):assert d['videoObservation']['exitCode']==0
 assert not live_case(case);return d
def main():
 assert not OUT.exists() and not RECEIPT.exists();print('Waiting actual332 full restore and335 source exporter exit; independent normal menu only',flush=True)
 report={'stage':'waiting-actual332-fullSHA-and335-owner-exit','cases':[],'source14FailuresRemainUnresolved':True,'ordinaryHeapAccepted':False,'all16NormalCallersAccepted':False,'armAccepted':False,'wholeGoalComplete':False}
 def save():RECEIPT.write_text(json.dumps(report,indent=2)+'\n')
 save()
 try:
  while not (DOC/'CAPTURE_QUEUE_BUILD363.json').exists():time.sleep(5)
  while not (DOC/'GPU_SOURCE335.json').exists():time.sleep(5)
  while any('docs/handoff/20261006/session-a/export_gpu_source335.py' in shlex.split(line.strip().split(None,1)[1]) or str(DOC/'export_gpu_source335.py') in shlex.split(line.strip().split(None,1)[1]) for line in subprocess.check_output(['ps','-axo','pid=,command='],text=True).splitlines() if len(line.strip().split(None,1))==2):time.sleep(5)
  diagnostic=read_session_state(DOC/'REPEAT_PREPARE332.json');assert all(x['exactRegularFileSha'] for x in diagnostic['restoration'].values()) and not live_case(PREVIOUS)
  repair=read_session_state(DOC/'CAPTURE_RESTORE348.json');assert repair['gameEachOriginalShaReadBack']
  previous_state=read_session_state(PREVIOUS/'session.json');assert previous_state['stage']=='restored-verified';assert read_session_state(DOC/'CAPTURE_QUEUE367.json')['gameUiNormalPassed'];assert previous_state['workerObservation']['exitCode']==0
  if previous_state.get('exactDefaultVideoRequested') or previous_state.get('videoObservation'):assert previous_state['videoObservation']['exitCode']==0
  else:assert previous_state.get('exactDefaultVideoRequested') is False
  large=read_session_state(DOC/'CAPTURE_QUEUE_BUILD363.json');assert large['buildSuccessful'];expected_main=read_session_state(DOC/'INHERITANCE.json')['main'];OUT.mkdir(parents=True);previous=PREVIOUS
  specs=[('large-menu-pcm',large,'UiUxInstrumentation',['--suite','audio','--menu-music','--audio-capture-rate','44100','--menu-capture-seconds','180','--mixer-observation','off'],False,False)]
  for name,build,runner,arguments,reuse,cold in specs:
   guard_caller_source(build['sourceRevision'],expected_main);apks={r['path']:r['sha256'] for r in build['apks']}
   for path,digest in apks.items():assert sha(Path(path))==digest
   case=OUT/name;report['stage']='preparing-backup-'+name;report['activeCase']=str(case);save()
   with (OUT/(name+'-backup.log')).open('w') as f:r=subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(case),'--previous',str(previous)],cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
   assert r.returncode==0
   try:
    guard_caller_source(build['sourceRevision'],expected_main)
    for path,digest in apks.items():assert sha(Path(path))==digest
   except BaseException:subprocess.run([sys.executable,str(HELPER),'restore','--output',str(case)],cwd=ROOT,check=True);raise
   game=next(p for p in apks if Path(p).name=='app-debug.apk');test=next(p for p in apks if Path(p).name=='app-debug-androidTest.apk');command=[sys.executable,str(HELPER),'install-test','--output',str(case),'--apk',game,'--test-apk',test,*(['--reuse-installed'] if reuse else []),'--test-only-update','--observe-workers','--runner',runner,*arguments];report['stage']='actual-running-'+name;report['command']=command;save()
   with (case/'driver.log').open('w') as f:r=subprocess.run(command,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
   actual=completed(case,apks,cold);assert r.returncode==0;entry={'name':name,'case':str(case),'apks':apks,'normalPassed':True,'differentPidColdPassed':cold,'everyOriginalFileShaRestored':True,'sourceSnapshotOrArithmeticInjected':False};report['cases'].append(entry);save()
   if name=='large-menu-pcm':
    capture=actual['audioCapture'];assert capture['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in capture['restoration'].values());music=next(x for x in capture['captures'] if x['run'].endswith('_menu'));assert not music['result']['failure'] and music['result']['frames']>0;menu=json.loads((case/'evidence/menu-music.json').read_text());assert menu['normalMenuOnly'] and menu['noTestPlaybackDirective'] and menu['allSaveRngEqual'] and menu['allStateTokenEqual'];assert menu['firstAcceptedBytes']==menu['firstDecodedBytes']==15618048 and menu['firstAcceptedPcmSha256']==menu['firstDecodedPcmSha256'];reference=ROOT/'out/session-a/music-submission-appop-44/reference';refmeta=json.loads((reference/'manifest.json').read_text());ref=next(x for x in refmeta['entries'] if x['resourceId']==2238);assert sha(reference/'2238.wav')==ref['wavSha256']
    sys.path.insert(0,str(ROOT/'tools/content'))
    from pc_resources import Archive
    original=Archive(Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版/Media/san11pkres.bin'))
    try:raw=original.read(2238)
    finally:original.file.close()
    assert hashlib.sha256(raw).hexdigest()==ref['originalKovsSha256']
    with zipfile.ZipFile(game) as z:assert hashlib.sha256(z.read('assets/'+ref['asset'])).hexdigest()==ref['oggSha256']
    python=Path('/Users/paopao/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3');waveform=case/'whole-music-check.json'
    with (case/'whole-music-check.log').open('w') as f:r=subprocess.run([str(python),str(ROOT/'tools/audio/check_pc_music_mix.py'),str(Path(music['hostPath'])/'android-mix.wav'),str(reference/'2238.wav'),'--resource','2238','--manifest',str(reference/'manifest.json'),'--output',str(waveform)],cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
    assert r.returncode==0;result=json.loads(waveform.read_text());assert result['status']=='PASS' and result['correlation']>=.995;entry['freshWholeTrackCorrelation']=result['correlation'];entry['scope']='Current large APK normal menu whole decoded-resource waveform; not Windows exactPCM/mapBGM/voice/ARM or old-22 unique cause';save()
   previous=case
  report['stage']='specified-regressions-terminal-passed';report['testedCohort']='CAPTURE_QUEUE_BUILD363.json';report['testApkRestoredToPrevious330']=True;report['scope']='Only fresh actual normal menu and whole original2238 waveform/0.995/fullSaveRNGToken/userFiles capture restore. Independent from failed327 ordinary source14 and failed332 first prepare; no override of those failures, no mapBGM/voice/fullmedia/all16/ordinary384/newWorld/cold/ARM/finalB acceptance.';save()
 except Exception as error:report['stage']='stopped-retain-actual-evidence';report['error']=str(error);report['cleanupNotInferred']=True;save();raise
if __name__=='__main__':main()
