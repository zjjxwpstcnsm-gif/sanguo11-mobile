#!/usr/bin/env python3
"""Full inherited isolated build candidate; no live source/cache/JNI overwrite."""
from pathlib import Path
import json,hashlib,subprocess,os,shutil,time,tarfile,zipfile,ctypes
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/map-fire-upload-build269'
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for block in iter(lambda:f.read(1048576),b''):h.update(block)
 return h.hexdigest()
def main():
 assert not OUT.exists();head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip();free=shutil.disk_usage(ROOT).free;assert free>3*1024**3,free
 delta=json.loads((DOC/'A_PRESENTATION_DELTA261.json').read_text());native=json.loads((DOC/'INSTRUCTION_FIRE_CANDIDATE260.json').read_text());protected=native['protectedSixUnchanged']
 for n,h in protected.items():assert sha(ROOT/n)==h
 sourceguard=json.loads((DOC/'SOURCE_GUARD.json').read_text());tracked=subprocess.check_output(['git','ls-files','-z'],cwd=ROOT).decode().split('\0');paths=set(p for p in tracked if p)|{x['path'] for x in sourceguard['files']}|set(protected)
 OUT.mkdir(parents=True);stage=OUT/'source';stage.mkdir();libc=ctypes.CDLL('/usr/lib/libSystem.B.dylib',use_errno=True);clone=libc.clonefile;clone.argtypes=[ctypes.c_char_p,ctypes.c_char_p,ctypes.c_int];clone.restype=ctypes.c_int;manifest=[]
 for name in sorted(paths):
  original=ROOT/name;assert original.is_file(),name;target=stage/name;target.parent.mkdir(parents=True,exist_ok=True);expected=sha(original)
  result=clone(os.fsencode(original),os.fsencode(target),0)
  if result:shutil.copy2(original,target)
  assert sha(target)==expected and target.stat().st_ino!=original.stat().st_ino,name
  manifest.append({'path':name,'bytes':target.stat().st_size,'sha256':expected})
 (OUT/'inherited-inputs.json').write_text(json.dumps(manifest,indent=2)+'\n')
 changes=[]
 archive=Path(delta['archivePath']);assert sha(archive)==delta['archiveSha256']
 with tarfile.open(archive) as t:
  for row in delta['paths']:
   target=stage/row['path'];assert sha(target)==row['closure227BeforeSha256'];raw=t.extractfile(row['path']).read();assert hashlib.sha256(raw).hexdigest()==row['afterSha256'];target.write_bytes(raw);changes.append({'path':row['path'],'beforeSha256':row['closure227BeforeSha256'],'afterSha256':row['afterSha256']})
 for row in native['outputs']:
  name='out/session-a/pc-fire-runtime/additional-jniLibs/'+row['abi']+'/libpc_effect_fire_worker.so';target=stage/name;before=sha(target);original=Path(row['path']);assert sha(original)==row['sha256'];target.write_bytes(original.read_bytes());changes.append({'path':name,'beforeSha256':before,'afterSha256':sha(target)})
 # The standalone copy intentionally uses parent git revision for BuildConfig;
 # exact modified input manifest and four recorded before/after paths identify
 # the candidate, rather than falsely labelling these bytes as canonical HEAD.
 assert len(changes)==4
 inputs=[]
 for row in manifest:
  target=stage/row['path'];inputs.append({'path':row['path'],'bytes':target.stat().st_size,'sha256':sha(target)})
 for row in manifest:assert sha(ROOT/row['path'])==row['sha256'],'Live inherited source changed during candidate preparation'
 (OUT/'candidate-inputs.json').write_text(json.dumps(inputs,indent=2)+'\n')
 home=OUT/'gradle-home';subprocess.run(['/bin/cp','-c','-R',str(ROOT/'out/session-a/gradle-home'),str(home)],check=True)
 env=os.environ.copy();env['JAVA_HOME']='/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home';env['GRADLE_USER_HOME']=str(home);env['ANDROID_HOME']='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk'
 # Resolve the already installed distribution, with no download fallback.
 dist=Path('/Users/paopao/.gradle/wrapper/dists/gradle-8.13-bin');candidates=list(dist.glob('*/gradle-8.13/bin/gradle'));assert len(candidates)==1;gradle=str(candidates[0]);command=[gradle,'--offline','--no-daemon',':app:assembleDebug',':app:assembleDebugAndroidTest'];began=time.monotonic()
 with (OUT/'build.log').open('w') as f:r=subprocess.run(command,cwd=stage,env=env,stdout=f,stderr=subprocess.STDOUT)
 assert r.returncode==0,(OUT/'build.log').read_text()[-6000:]
 records=[]
 for source in [stage/'app/build/outputs/apk/debug/app-debug.apk',stage/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
  target=OUT/source.name;shutil.copy2(source,target);records.append({'path':str(target),'sha256':sha(target),'bytes':target.stat().st_size})
 pins=json.loads((stage/'tools/content/map-release-manifest.json').read_text())['files'];assert len(pins)==168;game=OUT/'app-debug.apk';jni=[]
 with zipfile.ZipFile(game) as z:
  for row in pins:assert hashlib.sha256(z.read(row['apk_path'])).hexdigest()==row['sha256']
  for name,h in protected.items():
   entry='lib/'+'/'.join(Path(name).parts[-2:]);override=next((x for x in changes if x['path']==name),None);expected=override['afterSha256'] if override else h;assert hashlib.sha256(z.read(entry)).hexdigest()==expected;jni.append({'entry':entry,'sha256':expected,'protectedOriginal4':override is None and 'pc-native-runtime' in name})
 baseline=json.loads((DOC/'SEARCH_OBSERVATION_TEST_BUILD239.json').read_text())
 with zipfile.ZipFile(OUT/'app-debug-androidTest.apk') as z:assert len([n for n in z.namelist() if n.endswith('.sg11') and hashlib.sha256(z.read(n)).hexdigest()==baseline['genuine39FixtureSha256']])==1
 sdk=Path(env['ANDROID_HOME']);aapt=sdk/'build-tools/35.0.0/aapt';signer=sdk/'build-tools/35.0.0/apksigner';manifests=[];certs=[]
 for row in records:
  apk=Path(row['path']);text=subprocess.check_output([str(aapt),'dump','xmltree',str(apk),'AndroidManifest.xml'],text=True);(OUT/(apk.name+'.manifest.txt')).write_text(text);manifests.append(text);text=subprocess.check_output([str(signer),'verify','--print-certs',str(apk)],text=True,env=env);(OUT/(apk.name+'.signature.txt')).write_text(text);certs.append(next(x for x in text.splitlines() if x.startswith('Signer #1 certificate SHA-256 digest:')))
 assert certs[0]==certs[1] and 'SessionAMapRepairInstrumentation' in manifests[1] and 'SessionAFireFlowInstrumentation' in manifests[1];assert 'android:largeHeap' in manifests[0] and '(type 0x12)0xffffffff' in manifests[0]
 for name,h in protected.items():assert sha(ROOT/name)==h
 assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()==head
 report={'buildSuccessful':True,'sourceRevision':head,'canonicalBaseRevision':head,'commonBase':'0e7b9bc2df90249a50851baeda58c7d183ea6059','variantSourceDiffersFromCanonical':changes,'candidateInputManifest':str(OUT/'candidate-inputs.json'),'candidateInputManifestSha256':sha(OUT/'candidate-inputs.json'),'inheritedInputManifest':str(OUT/'inherited-inputs.json'),'inheritedInputManifestSha256':sha(OUT/'inherited-inputs.json'),'completeInheritedFiles':len(manifest),'apks':records,'gameLargeHeap':True,'sixJniExact':jni,'protectedCurrentSixUnchanged':protected,'fixedPackagedInputsExact':168,'genuine39FixtureSha256':baseline['genuine39FixtureSha256'],'fixturePackagedByteExact':True,'buildCommand':command,'wallSeconds':time.monotonic()-began,'buildLogSha256':sha(OUT/'build.log'),'independentSourceBuildOutGradleCache':True,'freeBytesBefore':free,'freeBytesAfter':shutil.disk_usage(ROOT).free,'actualInstalled':False,'scope':'Independent complete current inherited copy; only A Filament hint235/PcMapEffects pool258 and separate fire ABI260 changed. Current canonical production/test/cache and sixJNI unchanged, B Native WIP excluded, Main224 options and test250 not included. Build/package proof only, never existing165/239 scores transferred. Same BuildConfig parent revision plus exact variant input manifest identifies new bytes. Fresh normal paths/all16/ordinary heap/Java-native-GPU/pixels/ARM/final B serial integration required.','wholeGoalComplete':False};(DOC/'MAP_FIRE_UPLOAD_BUILD269.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:report[k] for k in ['apks','completeInheritedFiles','wallSeconds','actualInstalled']}))
if __name__=='__main__':main()
