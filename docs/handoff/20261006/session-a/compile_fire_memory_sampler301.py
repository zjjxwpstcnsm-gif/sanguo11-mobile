#!/usr/bin/env python3
"""Compile staged sampler after live297 and recorder exit; no new device action."""
from pathlib import Path
import json,subprocess,time
from read_session_state import read_session_state
from run_candidate_regression287 import live_case
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/fire-memory-compile301';CASE=ROOT/'out/session-a/fire-cache-installed297'
def main():
 assert not OUT.exists();print('Waiting actual297 restoration/owner/298 exit before compiler load',flush=True)
 while True:
  state=read_session_state(CASE/'session.json');video=read_session_state(DOC/'FIRE_CACHE_VIDEO298_LAUNCH.json')
  if state['stage']=='restored-verified' and video['stage']=='terminal' and not live_case(CASE):break
  time.sleep(5)
 assert all(x['exactRegularFileSha'] for x in state['restoration'].values())
 delta=read_session_state(DOC/'FIRE_MEMORY_SAMPLER300.json');base=ROOT/'out/session-a/fire-cache-apk296/source'
 for row in delta['paths']:
  assert sha(Path(row['stagedPath']))==row['afterSha256']
  if row['beforeSha256']:assert sha(base/row['path'])==row['beforeSha256']
  else:assert not (base/row['path']).exists()
 OUT.mkdir(parents=True);classes=OUT/'classes';classes.mkdir()
 cp=['/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platforms/android-35/android.jar',str(ROOT/'out/session-a/native-opening-stage224/compile/filament.jar'),str(base/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'),str(base/'app/build/intermediates/javac/debugAndroidTest/compileDebugAndroidTestJavaWithJavac/classes')]
 cp +=[str(base/n/'build/classes/java/main') for n in ['core','game-api','game-runtime']]
 assert all(Path(p).exists() for p in cp)
 command=['/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home/bin/java','-Xmx256m','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-cp',':'.join(cp),'-d',str(classes),*[r['stagedPath'] for r in delta['paths']]]
 with (OUT/'compile.log').open('w') as f:r=subprocess.run(command,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
 assert r.returncode==0,(OUT/'compile.log').read_text()
 report={'compiled':True,'deltaReceipt':'FIRE_MEMORY_SAMPLER300.json','deltaReceiptSha256':sha(DOC/'FIRE_MEMORY_SAMPLER300.json'),'command':command,'compileLogSha256':sha(OUT/'compile.log'),'actualCurrent297EndedBeforeCompile':True,'current297Passed':state['passed'],'canonicalOrInstalledApksChanged':False,'actualSamplerInstalled':False,'scope':'Compile only exact staged instrumentation sampler+fire integration against real296 frozen compiled classes; no actual Runtime sampling/CSV acceptance/new APK/install or rules/save/RNG change. Later independently packaged test cohort, full backup/install/normal source flow/newPID/restoration and parser of consistent sampled rows required.','wholeGoalComplete':False}
 (DOC/'FIRE_MEMORY_COMPILE301.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'compiled':True,'actualSamplerInstalled':False}),flush=True)
if __name__=='__main__':main()
