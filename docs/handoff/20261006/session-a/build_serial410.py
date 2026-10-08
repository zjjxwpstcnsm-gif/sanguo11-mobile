#!/usr/bin/env python3
"""New independently cached A397+B27+explicit A opening adapter combined APK."""
from pathlib import Path
import json,os,subprocess,shutil,time,zipfile,hashlib
from stage_serial401 import ROOT,D,OUT as STAGE,sha
OUT=ROOT/'out/session-a/serial-build410'
def main():
 assert not OUT.exists();assert shutil.disk_usage(ROOT).free>6*1024**3
 stage=STAGE/'source';a=json.loads((D/'MUSIC_RESERVE_BUILD369.json').read_text());adapter=json.loads((D/'OPENING_ADAPTER404.json').read_text());b=json.loads((D/'COMPLETED_B408.json').read_text())
 for row in adapter['paths']:assert sha(stage/row['path'])==row['afterSha256']
 for row in b['changedProductionPaths']:assert sha(stage/row['path'])==row['afterSha256']
 for p,h in a['protectedCurrentSixUnchanged'].items():assert sha(ROOT/p)==h
 original=json.loads((ROOT/'out/session-a/serial-stage401/candidate-inputs.json').read_text()); old={r['path']:r for r in original};records=[{'path':str(p.relative_to(stage)),'sha256':sha(p),'bytes':p.stat().st_size,'mode':p.stat().st_mode&0o777} for p in sorted(stage.rglob('*')) if p.is_file()]
 changes=[r for r in records if old.get(r['path'],{}).get('sha256')!=r['sha256']]
 OUT.mkdir();index=OUT/'candidate-inputs.json';index.write_text(json.dumps(records,indent=2)+'\n')
 home=OUT/'gradle-home';subprocess.run(['/bin/cp','-c','-R',str(ROOT/'out/session-a/gradle-home'),str(home)],check=True)
 env=os.environ.copy();env.update(JAVA_HOME='/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home',GRADLE_USER_HOME=str(home),ANDROID_HOME='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk')
 gradle=list(Path('/Users/paopao/.gradle/wrapper/dists/gradle-8.13-bin').glob('*/gradle-8.13/bin/gradle'));assert len(gradle)==1;start=time.monotonic()
 with (OUT/'build.log').open('w') as f:result=subprocess.run([str(gradle[0]),'--offline','--no-daemon',':app:assembleDebug',':app:assembleDebugAndroidTest'],cwd=stage,env=env,stdout=f,stderr=subprocess.STDOUT)
 report={'commonBase':'0e7b9bc2df90249a50851baeda58c7d183ea6059','completedBCommit':'cdd7f949','aCandidate':'CURRENT_SOURCE397.json','openingAdapter':'OPENING_ADAPTER404.json','completeCandidateFiles':len(records),'candidateInputManifest':str(index),'candidateInputManifestSha256':sha(index),'sourcePath':str(stage),'changesAfterSerial401':changes,'independentGradleCache':str(home),'wallSeconds':time.monotonic()-start,'buildExit':result.returncode,'buildLogSha256':sha(OUT/'build.log'),'buildSuccessful':result.returncode==0,'actualInstalled':False,'wholeGoalComplete':False}
 if result.returncode:
  (D/'SERIAL_BUILD411.json').write_text(json.dumps(report,indent=2)+'\n');print((OUT/'build.log').read_text()[-6500:]);raise RuntimeError('Actual combined build failed; source and raw errors preserved')
 apks=[]
 for src in [stage/'app/build/outputs/apk/debug/app-debug.apk',stage/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
  dst=OUT/src.name;shutil.copy2(src,dst);apks.append({'path':str(dst),'sha256':sha(dst),'bytes':dst.stat().st_size})
 with zipfile.ZipFile(OUT/'app-debug.apk') as z,zipfile.ZipFile(ROOT/'out/session-a/music-reserve-build369/app-debug.apk') as az:
  names={n for n in az.namelist() if n.startswith('assets/') and not n.endswith('/')};assert names=={n for n in z.namelist() if n.startswith('assets/') and not n.endswith('/')}
  for n in names:assert z.read(n)==az.read(n),n
  for row in a['sixJniExact']:assert hashlib.sha256(z.read(row['entry'])).hexdigest()==row['sha256']
  pins=json.loads((stage/'tools/content/map-release-manifest.json').read_text())['files'];assert len(pins)==168
  for row in pins:assert hashlib.sha256(z.read(row['apk_path'])).hexdigest()==row['sha256']
 sdk=Path(env['ANDROID_HOME']);certs=[]
 for row in apks:
  xml=subprocess.check_output([str(sdk/'build-tools/35.0.0/aapt'),'dump','xmltree',row['path'],'AndroidManifest.xml'],text=True);(OUT/(Path(row['path']).name+'.manifest.txt')).write_text(xml)
  if Path(row['path']).name=='app-debug.apk':assert 'android:largeHeap' in xml and '(type 0x12)0xffffffff' in xml
  else:assert 'SessionANativeDuelOpeningInstrumentation' in xml and 'SessionBOfficerContentInstrumentation' in xml
  cert=subprocess.check_output([str(sdk/'build-tools/35.0.0/apksigner'),'verify','--print-certs',row['path']],env=env,text=True);certs.append(next(s for s in cert.splitlines() if s.startswith('Signer #1 certificate SHA-256 digest:')))
 assert certs[0]==certs[1]
 for p,h in a['protectedCurrentSixUnchanged'].items():assert sha(ROOT/p)==h
 for row in records:assert sha(stage/row['path'])==row['sha256'],row['path']
 report.update(apks=apks,fixedPackaged168Exact=True,allAAssetsExact369=len(names),sixJniExact369=True,signatureSame=certs[0],fullSourceShaUnchangedAfterBuild=True,scope='New combined build only: full A397, immutable completed B26/27, two A opening paths and diagnostic tests. Not installed, no previous emulator/ARM/cold/fire/native-duel scores transferred. All required checks and real new APK user-data backup/install/restore remain necessary.')
 (D/'SERIAL_BUILD411.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'apks':apks,'buildSuccessful':True,'installed':False}),flush=True)
if __name__=='__main__':main()
