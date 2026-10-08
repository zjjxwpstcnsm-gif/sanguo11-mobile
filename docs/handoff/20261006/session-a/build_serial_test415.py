#!/usr/bin/env python3
"""Keep exact combined game410; correct only test query ownership thread."""
from pathlib import Path
import json,subprocess,sys,ctypes,os,shutil,time
from stage_serial401 import ROOT,D,sha
from read_session_state import read_session_state as read
from run_music_reserve378 import live_case
OUT=ROOT/'out/session-a/serial-test415'
def main():
 assert not OUT.exists();case=ROOT/'out/session-a/serial-opening412';state=read(case/'session.json');assert state['stage']=='restored-verified' and not live_case(case) and not state['passed'];assert all(x['exactRegularFileSha'] for x in state['restoration'].values())
 failure={'actualApks':state['apks'],'accepted':False,'coldAttempted':False,'actualFailure':(case/'instrumentation.txt').read_text(),'testThreadGuardViolated':True,'fullOriginalShaRestoration':state['restoration'],'rawSessionSha256':sha(case/'session.json'),'noRawVideoCaptured':state.get('videoObservation') is None,'scope':'Actual source0 settings cancel/final confirmation cancel and new explicit factory worked, then test queried serial API on instrumentation thread. Not counted as normal/cold accepted. Raw case, original test source and game/test APK retained.','wholeGoalComplete':False};(D/'SERIAL_OPENING_FAILURE418.json').write_text(json.dumps(failure,indent=2)+'\n')
 parent=read(D/'SERIAL_BUILD411.json');index=Path(parent['candidateInputManifest']);assert sha(index)==parent['candidateInputManifestSha256'];origin=Path(parent['sourcePath']);rows=read(index);OUT.mkdir();stage=OUT/'source';stage.mkdir()
 libc=ctypes.CDLL('/usr/lib/libSystem.B.dylib',use_errno=True);clone=libc.clonefile;clone.argtypes=[ctypes.c_char_p,ctypes.c_char_p,ctypes.c_int];clone.restype=ctypes.c_int
 for row in rows:
  src=origin/row['path'];assert sha(src)==row['sha256'];dst=stage/row['path'];dst.parent.mkdir(parents=True,exist_ok=True)
  if clone(os.fsencode(src),os.fsencode(dst),0):shutil.copy2(src,dst)
  assert sha(dst)==row['sha256'] and src.stat().st_ino!=dst.stat().st_ino
 path='app/src/androidTest/java/game/sanguo/mobile/SessionANativeDuelOpeningInstrumentation.java';p=stage/path;before=sha(p);s=p.read_text();old='''  GameApi api=((NativeGameHost)field(activity,"gameHost")).session();var facts=api.pcOpeningOptions();''';assert s.count(old)==1;s=s.replace(old,'''  final PcOpeningOptionsSnapshot[] projected={null};
  runOnMainSync(()->{try{GameApi api=((NativeGameHost)field(activity,"gameHost")).session();projected[0]=api.pcOpeningOptions();}catch(Exception error){throw new RuntimeException(error);}});
  var facts=projected[0];''');p.write_text(s);after=sha(p)
 manifest=[{'path':str(f.relative_to(stage)),'sha256':sha(f),'bytes':f.stat().st_size,'mode':f.stat().st_mode&0o777} for f in sorted(stage.rglob('*')) if f.is_file()];m=OUT/'candidate-inputs.json';m.write_text(json.dumps(manifest,indent=2)+'\n');assert len(manifest)==len(rows)
 home=OUT/'gradle-home';subprocess.run(['/bin/cp','-c','-R',str(ROOT/'out/session-a/gradle-home'),str(home)],check=True);env=os.environ.copy();env.update(JAVA_HOME='/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home',GRADLE_USER_HOME=str(home),ANDROID_HOME='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk');gradle=list(Path('/Users/paopao/.gradle/wrapper/dists/gradle-8.13-bin').glob('*/gradle-8.13/bin/gradle'));assert len(gradle)==1;start=time.monotonic()
 with (OUT/'build.log').open('w') as log:result=subprocess.run([str(gradle[0]),'--offline','--no-daemon',':app:assembleDebugAndroidTest'],cwd=stage,env=env,stdout=log,stderr=subprocess.STDOUT)
 assert result.returncode==0,(OUT/'build.log').read_text()[-5000:]
 game=next(r for r in parent['apks'] if Path(r['path']).name=='app-debug.apk');assert sha(Path(game['path']))==game['sha256'];test=OUT/'app-debug-androidTest.apk';shutil.copy2(stage/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk',test);apks=[game,{'path':str(test),'sha256':sha(test),'bytes':test.stat().st_size}]
 for row in rows:assert sha(origin/row['path'])==row['sha256']
 for row in manifest:assert sha(stage/row['path'])==row['sha256']
 report={'buildSuccessful':True,'gameLargeHeap':True,'commonBase':parent['commonBase'],'gameByteExact410':True,'apks':apks,'candidateInputManifest':str(m),'candidateInputManifestSha256':sha(m),'sourcePath':str(stage),'completeCandidateFiles':len(manifest),'changedOnlyTestPath':path,'beforeSha256':before,'afterSha256':after,'independentSourceBuildOutGradleCache':True,'wallSeconds':time.monotonic()-start,'buildLogSha256':sha(OUT/'build.log'),'fullSourceShaUnchangedAfterBuild':True,'buildExit':0,'actualInstalled':False,'scope':'Exact ce83 combined game retained. One test-only read marshaled to original serial UI/session owner; no production/World/RNG/DTO/rule or assertion change. Actual failed412 remains false. New normal/cold with full backup/install/SHA/restore required.','wholeGoalComplete':False};(D/'SERIAL_TEST_BUILD416.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'apks':apks,'gameExact410':True}),flush=True)
if __name__=='__main__':main()
