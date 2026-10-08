#!/usr/bin/env python3
"""Independent normal-heap build of exact269 candidate sources/resources/JNI."""
from pathlib import Path
import json,hashlib,subprocess,os,shutil,time,ctypes,zipfile
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/ordinary-candidate-build280'
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for chunk in iter(lambda:f.read(1048576),b''):h.update(chunk)
 return h.hexdigest()
def main():
 assert not OUT.exists();free=shutil.disk_usage(ROOT).free;assert free>3*1024**3;head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip();base=json.loads((DOC/'MAP_FIRE_UPLOAD_BUILD269.json').read_text());manifest=Path(base['candidateInputManifest']);assert sha(manifest)==base['candidateInputManifestSha256'];inputs=json.loads(manifest.read_text());original=manifest.parent/'source';OUT.mkdir(parents=True);stage=OUT/'source';stage.mkdir();lib=ctypes.CDLL('/usr/lib/libSystem.B.dylib',use_errno=True);clone=lib.clonefile;clone.argtypes=[ctypes.c_char_p,ctypes.c_char_p,ctypes.c_int];clone.restype=ctypes.c_int
 for row in inputs:
  source=original/row['path'];assert sha(source)==row['sha256'];dest=stage/row['path'];dest.parent.mkdir(parents=True,exist_ok=True)
  if clone(os.fsencode(source),os.fsencode(dest),0):shutil.copy2(source,dest)
  assert sha(dest)==row['sha256'] and dest.stat().st_ino!=source.stat().st_ino
 home=OUT/'gradle-home';subprocess.run(['/bin/cp','-c','-R',str(manifest.parent/'gradle-home'),str(home)],check=True);env=os.environ.copy();env['JAVA_HOME']='/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home';env['GRADLE_USER_HOME']=str(home);env['ANDROID_HOME']='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk';gradles=list(Path('/Users/paopao/.gradle/wrapper/dists/gradle-8.13-bin').glob('*/gradle-8.13/bin/gradle'));assert len(gradles)==1;command=[str(gradles[0]),'--offline','--no-daemon','-PgameLargeHeap=false',':app:assembleDebug',':app:assembleDebugAndroidTest'];begin=time.monotonic()
 with (OUT/'build.log').open('w') as f:r=subprocess.run(command,cwd=stage,env=env,stdout=f,stderr=subprocess.STDOUT)
 assert r.returncode==0,(OUT/'build.log').read_text()[-6000:];records=[]
 for source in [stage/'app/build/outputs/apk/debug/app-debug.apk',stage/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
  target=OUT/source.name;shutil.copy2(source,target);records.append({'path':str(target),'bytes':target.stat().st_size,'sha256':sha(target)})
 sdk=Path(env['ANDROID_HOME']);aapt=sdk/'build-tools/35.0.0/aapt';signer=sdk/'build-tools/35.0.0/apksigner';text=subprocess.check_output([str(aapt),'dump','xmltree',str(OUT/'app-debug.apk'),'AndroidManifest.xml'],text=True);(OUT/'game-manifest.txt').write_text(text);heap=[line for line in text.splitlines() if 'android:largeHeap' in line];assert len(heap)==1 and '(type 0x12)0x0' in heap[0];testmanifest=subprocess.check_output([str(aapt),'dump','xmltree',str(OUT/'app-debug-androidTest.apk'),'AndroidManifest.xml'],text=True);assert 'SessionAMapRepairInstrumentation' in testmanifest and 'SessionAFireFlowInstrumentation' in testmanifest
 certs=[]
 for row in records:
  apk=Path(row['path']);text=subprocess.check_output([str(signer),'verify','--print-certs',str(apk)],env=env,text=True);(OUT/(apk.name+'.signature.txt')).write_text(text);certs.append(next(line for line in text.splitlines() if line.startswith('Signer #1 certificate SHA-256 digest:')))
 assert certs[0]==certs[1];pins=json.loads((stage/'tools/content/map-release-manifest.json').read_text())['files'];assert len(pins)==168
 with zipfile.ZipFile(OUT/'app-debug.apk') as z,zipfile.ZipFile(base['apks'][0]['path']) as old:
  for row in pins:assert hashlib.sha256(z.read(row['apk_path'])).hexdigest()==row['sha256']
  for row in base['sixJniExact']:assert hashlib.sha256(z.read(row['entry'])).hexdigest()==row['sha256']
  assets={n:hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if n.startswith('assets/')};original_assets={n:hashlib.sha256(old.read(n)).hexdigest() for n in old.namelist() if n.startswith('assets/')};assert assets==original_assets
 with zipfile.ZipFile(OUT/'app-debug-androidTest.apk') as z:assert len([n for n in z.namelist() if n.endswith('.sg11') and hashlib.sha256(z.read(n)).hexdigest()==base['genuine39FixtureSha256']])==1
 for row in inputs:assert sha(stage/row['path'])==row['sha256']
 for name,h in base['protectedCurrentSixUnchanged'].items():assert sha(ROOT/name)==h
 assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()==head
 report={'buildSuccessful':True,'sourceRevision':head,'variantBase':'MAP_FIRE_UPLOAD_BUILD269.json','candidateBaseRevision':base['canonicalBaseRevision'],'sameCandidateInputManifest':str(manifest),'sameCandidateInputManifestSha256':sha(manifest),'copiedEveryInputFileExact':len(inputs),'apks':records,'gameLargeHeap':False,'mergedManifestLargeHeapFalse':True,'allPackagedAssetsByteExact269':len(assets),'fixedPackagedInputsExact':168,'sixJniExact':base['sixJniExact'],'protectedCurrentSixUnchanged':base['protectedCurrentSixUnchanged'],'genuine39FixtureSha256':base['genuine39FixtureSha256'],'fixturePackagedByteExact':True,'independentSourceBuildOutGradleCache':True,'buildCommand':command,'wallSeconds':time.monotonic()-begin,'buildLogSha256':sha(OUT/'build.log'),'buildRevisionLabelMayDifferFrom269':True,'actualInstalled':False,'scope':'Independent ordinary-heap variant of same actual269 production source/resource/native files. Manifest largeHeap=false and BuildConfig parent revision label may differ. No old180 or new large-heap scores transferred. Current canonical source/current sixJNI unchanged; B WIP excluded. Fresh full backup/install/all16 previews/zoom/normal commands/cold/read/fullSHA and Java-native-GPU/ARM evidence required.','wholeGoalComplete':False};(DOC/'ORDINARY_CANDIDATE_BUILD280.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:report[k] for k in ['apks','copiedEveryInputFileExact','allPackagedAssetsByteExact269','wallSeconds','gameLargeHeap','actualInstalled']}))
if __name__=='__main__':main()
