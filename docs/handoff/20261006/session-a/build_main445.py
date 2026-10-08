#!/usr/bin/env python3
"""Independent final main build with original4 and exact tested A2 JNI inputs."""
from pathlib import Path
import json,os,subprocess,shutil,time,zipfile,hashlib
from stage_serial401 import ROOT,D,sha
OUT=ROOT/'out/session-a/main-build445'
def main():
 assert not OUT.exists();plan=json.loads((D/'MAIN_SYNC_PLAN440.json').read_text());main=Path(plan['mainWorktree']);head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=main,text=True).strip();assert head==subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip();assert not subprocess.check_output(['git','status','--porcelain'],cwd=main)
 candidate=json.loads((D/'REQUIRED_BUILD437.json').read_text());rows=json.loads(Path(candidate['candidateInputManifest']).read_text());checked=0
 for row in rows:
  if row['path'].startswith(('docs/handoff/20261006/session-a/','out/','build/','.gradle/')):continue
  assert sha(main/row['path'])==row['sha256'],row['path'];checked+=1
 native=[]
 for row in plan['jni']:
  src=ROOT/row['path'];dst=main/row['path'];assert sha(src)==row['afterSha256'];before=sha(dst) if dst.is_file() else None
  if before is not None:assert before==row['afterSha256'],('Preserve unexpected existing main JNI',row['path'])
  else:dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dst)
  assert sha(dst)==row['afterSha256'];native.append({'path':row['path'],'beforeSha256':before,'afterSha256':sha(dst),'copiedOnlyWhenMissing':before is None})
 assert shutil.disk_usage(ROOT).free>3*1024**3;OUT.mkdir();home=OUT/'gradle-home';subprocess.run(['/bin/cp','-c','-R',str(ROOT/'out/session-a/gradle-home'),str(home)],check=True)
 env=os.environ.copy();env.update(JAVA_HOME='/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home',GRADLE_USER_HOME=str(home),ANDROID_HOME='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk');gradle=list(Path('/Users/paopao/.gradle/wrapper/dists/gradle-8.13-bin').glob('*/gradle-8.13/bin/gradle'));assert len(gradle)==1;start=time.monotonic()
 with (OUT/'build.log').open('w') as log:result=subprocess.run([str(gradle[0]),'--offline','--no-daemon','--project-cache-dir',str(OUT/'project-cache'),':app:assembleDebug',':app:assembleDebugAndroidTest'],cwd=main,env=env,stdout=log,stderr=subprocess.STDOUT)
 report={'sourceRevision':head,'sourcePath':str(main),'canonicalCandidateInputsVerified':checked,'jniGuards':native,'buildExit':result.returncode,'wallSeconds':time.monotonic()-start,'buildLogSha256':sha(OUT/'build.log'),'buildSuccessful':result.returncode==0,'gameLargeHeap':True,'mainWorkingTreeClean':not bool(subprocess.check_output(['git','status','--porcelain'],cwd=main)),'independentGradleCache':str(home),'actualInstalled':False,'wholeGoalComplete':False}
 if result.returncode:(D/'MAIN_BUILD446.json').write_text(json.dumps(report,indent=2)+'\n');raise RuntimeError((OUT/'build.log').read_text()[-4000:])
 apks=[]
 for src in [main/'app/build/outputs/apk/debug/app-debug.apk',main/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
  dst=OUT/src.name;shutil.copy2(src,dst);apks.append({'path':str(dst),'sha256':sha(dst),'bytes':dst.stat().st_size})
 with zipfile.ZipFile(OUT/'app-debug.apk') as z:
  for row in json.loads((D/'MUSIC_RESERVE_BUILD369.json').read_text())['sixJniExact']:assert hashlib.sha256(z.read(row['entry'])).hexdigest()==row['sha256']
  pins=json.loads((main/'tools/content/map-release-manifest.json').read_text())['files'];assert len(pins)==168
  for row in pins:assert hashlib.sha256(z.read(row['apk_path'])).hexdigest()==row['sha256']
 for row in rows:
  if row['path'].startswith(('docs/handoff/20261006/session-a/','out/','build/','.gradle/')):continue
  assert sha(main/row['path'])==row['sha256']
 assert report['mainWorkingTreeClean'] and not subprocess.check_output(['git','status','--porcelain'],cwd=main)
 report.update(apks=apks,fixed168AndSixJniPackagedExact=True,sourceInputsExactFinalCandidate=True,scope='New actual local main05236 build, original4 unchanged and tested A2 added only when absent. Only final source metadata differs from358 candidate; new installed normal/cancel/save/cold/complete restoration must be measured, no old APK scores transferred. Known first-source0 readinessFAIL/media/ARM/all16/native command gaps retained.');(D/'MAIN_BUILD446.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'sourceRevision':head,'apks':apks,'installed':False}),flush=True)
if __name__=='__main__':main()
