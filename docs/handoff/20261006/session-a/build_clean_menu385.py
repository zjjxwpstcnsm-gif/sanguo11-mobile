#!/usr/bin/env python3
"""Independent new prepare diagnostic test APK; keep exact296 game immutable."""
from pathlib import Path
import json,subprocess,os,shutil,ctypes,time,zipfile,hashlib
from run_music_reserve378 import live_case
from read_session_state import read_session_state
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/clean-menu-build385'
def main():
 assert not OUT.exists();print('Wait exact380 full restore/owners and382 residual before any copy/build',flush=True)
 while not (DOC/'RESERVE_MAP381.json').exists() or not (DOC/'MIX_RESIDUAL382.json').exists():time.sleep(5)
 state=read_session_state(ROOT/'out/session-a/reserve-map380/session.json');assert state['stage']=='restored-verified' and all(x['exactRegularFileSha'] for x in state['restoration'].values())
 assert not live_case(ROOT/'out/session-a/reserve-map380');assert state['workerObservation']['exitCode']==state['videoObservation']['exitCode']==0
 failure=read_session_state(DOC/'MUSIC_RESERVE379.json');assert not failure['wholeNormalMenuTrackAccepted'];assert failure['gameRestoration']['external']['exactRegularFileSha']
 parent=read_session_state(DOC/'MUSIC_RESERVE_BUILD369.json');delta=read_session_state(DOC/'CLEAN_MENU384.json');inputs=Path(parent['candidateInputManifest']);assert sha(inputs)==parent['candidateInputManifestSha256'];manifest=read_session_state(inputs);base=inputs.parent/'source';head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip();assert shutil.disk_usage(ROOT).free>4*1024**3
 for n,h in read_session_state(DOC/'GPU_INDEX_BUILD326.json')['protectedCurrentSixUnchanged'].items():assert sha(ROOT/n)==h
 OUT.mkdir(parents=True);stage=OUT/'source';stage.mkdir();libc=ctypes.CDLL('/usr/lib/libSystem.B.dylib',use_errno=True);clone=libc.clonefile;clone.argtypes=[ctypes.c_char_p,ctypes.c_char_p,ctypes.c_int];clone.restype=ctypes.c_int
 for row in manifest:
  origin=base/row['path'];assert sha(origin)==row['sha256'];target=stage/row['path'];target.parent.mkdir(parents=True,exist_ok=True)
  if clone(os.fsencode(origin),os.fsencode(target),0):shutil.copy2(origin,target)
  assert sha(target)==row['sha256'] and target.stat().st_ino!=origin.stat().st_ino
 for row in delta['paths']:
  target=stage/row['path'];assert (sha(target)==row['beforeSha256']) if row['beforeSha256'] else not target.exists();origin=Path(row['stagedPath']);assert sha(origin)==row['afterSha256'];target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(origin.read_bytes())
 records=[{'path':str(p.relative_to(stage)),'bytes':p.stat().st_size,'sha256':sha(p)} for p in sorted(stage.rglob('*')) if p.is_file()];assert len(records)==len(manifest);(OUT/'candidate-inputs.json').write_text(json.dumps(records,indent=2)+'\n')
 home=OUT/'gradle-home';subprocess.run(['/bin/cp','-c','-R',str(ROOT/'out/session-a/gradle-home'),str(home)],check=True);env=os.environ.copy();env.update(JAVA_HOME='/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home',GRADLE_USER_HOME=str(home),ANDROID_HOME='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk');gradle=list(Path('/Users/paopao/.gradle/wrapper/dists/gradle-8.13-bin').glob('*/gradle-8.13/bin/gradle'));assert len(gradle)==1;command=[str(gradle[0]),'--offline','--no-daemon',':app:assembleDebugAndroidTest'];began=time.monotonic()
 with (OUT/'build.log').open('w') as f:r=subprocess.run(command,cwd=stage,env=env,stdout=f,stderr=subprocess.STDOUT)
 assert r.returncode==0,(OUT/'build.log').read_text()[-6500:]
 test=OUT/'app-debug-androidTest.apk';shutil.copy2(stage/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk',test);game=next(row for row in parent['apks'] if Path(row['path']).name=='app-debug.apk');assert sha(Path(game['path']))==game['sha256'];apks=[game,{'path':str(test),'bytes':test.stat().st_size,'sha256':sha(test)}]
 sdk=Path(env['ANDROID_HOME']);xml=subprocess.check_output([str(sdk/'build-tools/35.0.0/aapt'),'dump','xmltree',str(test),'AndroidManifest.xml'],text=True);assert 'SessionAScenarioPrepareInstrumentation' in xml;(OUT/'test-manifest.txt').write_text(xml);certs=[]
 for row in apks:
  text=subprocess.check_output([str(sdk/'build-tools/35.0.0/apksigner'),'verify','--print-certs',row['path']],text=True,env=env);certs.append(next(line for line in text.splitlines() if line.startswith('Signer #1 certificate SHA-256 digest:')))
 assert certs[0]==certs[1]
 fixture=read_session_state(DOC/'SEARCH_OBSERVATION_TEST_BUILD239.json')['genuine39FixtureSha256']
 with zipfile.ZipFile(test) as z:assert len([n for n in z.namelist() if n.endswith('.sg11') and hashlib.sha256(z.read(n)).hexdigest()==fixture])==1
 for row in manifest:assert sha(base/row['path'])==row['sha256']
 for n,h in read_session_state(DOC/'GPU_INDEX_BUILD326.json')['protectedCurrentSixUnchanged'].items():assert sha(ROOT/n)==h
 report={'buildSuccessful':True,'sourceRevision':head,'commonBase':parent['commonBase'],'completeInheritedFiles':len(manifest),'completeCandidateFiles':len(records),'changedOnlyDiagnosticTestPaths':delta['paths'],'apks':apks,'gameReusedExact369':True,'gameLargeHeap':True,'candidateInputManifest':str(OUT/'candidate-inputs.json'),'candidateInputManifestSha256':sha(OUT/'candidate-inputs.json'),'independentSourceBuildOutGradleCache':True,'wallSeconds':time.monotonic()-began,'buildLogSha256':sha(OUT/'build.log'),'functional120sThresholdRelaxed':False,'actualInstalled':False,'scope':'Fresh diagnostic test-only APK built against full326 source/resource/JNI inputs; exact previously installed369 game APK bytes reused, not rebuilt/relabeled. New test may have different BuildConfig revision label; product code unchanged. Only optional normal slider-before-music isolation384; original mixed-flow fixture retained; original game811 unchanged, source audio/strict0.995 untouched. Need actual normal full-track capture and complete game/test/APK/permission restoration. Fresh full user backup/test-only install/SHA/restoration still required.','wholeGoalComplete':False};(DOC/'CLEAN_MENU_BUILD385.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'apks':apks,'buildSuccessful':True,'actualInstalled':False}),flush=True)
if __name__=='__main__':main()
