#!/usr/bin/env python3
"""Finite existing Android codec diagnostic, full protected device userdata restore."""
from pathlib import Path
import json,subprocess,sys,hashlib,shlex
from read_session_state import read_session_state as read
from run_remaining_normal_media import sha
from run_capture_queue366 import live_case
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/android-music372';PRE=ROOT/'out/session-a/android-music370';HELPER=D/'device_session.py';ADB='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platform-tools/adb';P='game.sanguo.mobile.dev'
def adb(*args):return subprocess.check_output([ADB,'-s','emulator-5554',*args],timeout=180)
def main():
 assert not OUT.exists();previous=read(PRE/'session.json');assert previous['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in previous['restoration'].values());assert read(D/'CAPTURE_QUEUE367.json')['workerObservation']['exitCode']==0 and not live_case(PRE)
 receipt=D/'ANDROID_MUSIC_DECODE372.json';assert not receipt.exists();report={'stage':'fresh-backup','installedNewApk':False,'normalGameplayAcceptance':False,'wholeGoalComplete':False};receipt.write_text(json.dumps(report,indent=2)+'\n')
 r=subprocess.run([sys.executable,str(HELPER),'reuse-backup','--output',str(OUT),'--previous',str(PRE)],cwd=ROOT,capture_output=True,text=True);assert r.returncode==0,r.stdout+r.stderr
 try:
  build=read(D/'REPEAT_TEST_BUILD330.json');identities={}
  for row in build['apks']:
   package=P+('.test' if Path(row['path']).name.endswith('androidTest.apk') else '');remote=adb('shell','pm','path',package).decode().strip().removeprefix('package:');actual=adb('shell','sha256sum',remote).decode().split()[0];assert actual==row['sha256'];identities[package]=actual
  component=P+'.test/game.sanguo.mobile.MusicSourceInstrumentation';assert ('instrumentation:'+component+' (target='+P+')') in adb('shell','pm','list','instrumentation').decode().splitlines()
  # Existing fixture creates a detached test GameSession solely to compare Save/RNG.
  # It never installs it into the normal app. This is not normal menu/playback proof.
  result=adb('shell','am','instrument','-w','-e','resource','2238','-e','retainPcm','true',component);(OUT/'decoder-instrumentation.txt').write_bytes(result);assert b'MUSIC_SOURCE PASS' in result,result.decode()
  for name in ['music-source.json','music-source-2238.pcm']:
   (OUT/name).write_bytes(adb('exec-out','cat','/data/data/'+P+'/files/'+name))
  metadata=read(OUT/'music-source.json');row=metadata['tracks'][0];assert len(metadata['tracks'])==1 and row['resourceId']==2238;pcm=OUT/'music-source-2238.pcm';assert pcm.stat().st_size==row['bytes']==15618048 and sha(pcm)==row['actualPcmSha256']=='c90b72f072a61abbaf910aa2403a596c58590930d2ae43650d105743efecd66e';report.update(decoderAccepted=True,stage='decoder-measured-before-restore',actualInstalledApks=identities,actualCodecMetadata=metadata,pcmPath=str(pcm),pcmSha256=sha(pcm),scope='Actual existing Android decoder output2238. Detached fixture Save/RNG only, not normal UI/gameplay or audio waveform acceptance. Source conversion/codec/mixer separation; original0.995/raw capture untouched.')
 except BaseException as error:
  report['diagnosticFailure']=str(error);report['decoderAccepted']=False;raise
 finally:
  restored=subprocess.run([sys.executable,str(HELPER),'restore','--output',str(OUT)],cwd=ROOT,capture_output=True,text=True);(OUT/'restore.log').write_text(restored.stdout+restored.stderr);assert restored.returncode==0;state=read(OUT/'session.json');assert state['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in state['restoration'].values());report['restoration']=state['restoration'];report['stage']='restored-verified';receipt.write_text(json.dumps(report,indent=2)+'\n')
 print(json.dumps({'pcmSha256':report.get('pcmSha256'),'stage':report['stage']}))
if __name__=='__main__':main()
