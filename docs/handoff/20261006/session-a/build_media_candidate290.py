#!/usr/bin/env python3
"""Independent full candidate269 plus exact five owned media paths and draw check."""
from pathlib import Path
import json,hashlib,subprocess,os,shutil,time,tarfile,zipfile,ctypes
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/media-candidate-build290'
def sha(p):
 d=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1048576),b''):d.update(b)
 return d.hexdigest()
def main():
 assert not OUT.exists();free=shutil.disk_usage(ROOT).free;assert free>4*1024**3
 parent=json.loads((DOC/'MAP_FIRE_UPLOAD_BUILD269.json').read_text());delta=json.loads((DOC/'A_MEDIA_DELTA282.json').read_text());test=json.loads((DOC/'PORTRAIT_DRAW_CHECK289.json').read_text())
 base=ROOT/'out/session-a/map-fire-upload-build269/source';manifest_path=Path(parent['candidateInputManifest']);assert sha(manifest_path)==parent['candidateInputManifestSha256'];manifest=json.loads(manifest_path.read_text())
 protected=parent['protectedCurrentSixUnchanged']
 for p,h in protected.items():assert sha(ROOT/p)==h
 OUT.mkdir(parents=True);stage=OUT/'source';stage.mkdir();libc=ctypes.CDLL('/usr/lib/libSystem.B.dylib',use_errno=True);clone=libc.clonefile;clone.argtypes=[ctypes.c_char_p,ctypes.c_char_p,ctypes.c_int];clone.restype=ctypes.c_int
 for row in manifest:
  origin=base/row['path'];assert origin.is_file() and sha(origin)==row['sha256'];target=stage/row['path'];target.parent.mkdir(parents=True,exist_ok=True)
  if clone(os.fsencode(origin),os.fsencode(target),0):shutil.copy2(origin,target)
  assert sha(target)==row['sha256'] and target.stat().st_ino!=origin.stat().st_ino
 changes=[];archive=Path(delta['archivePath']);assert sha(archive)==delta['archiveSha256']
 with tarfile.open(archive) as t:
  for row in delta['paths']:
   target=stage/row['path'];assert (sha(target)==row['beforeSha256']) if row['beforeSha256'] else not target.exists()
   raw=t.extractfile(row['path']).read();assert hashlib.sha256(raw).hexdigest()==row['afterSha256'];target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(raw);changes.append(row)
 target=stage/test['path'];assert sha(target)==test['beforeSha256'];raw=Path(test['stagedPath']).read_bytes();assert hashlib.sha256(raw).hexdigest()==test['afterSha256'];target.write_bytes(raw);changes.append({k:test[k] for k in ['path','beforeSha256','afterSha256']})
 inputs=[{'path':str(p.relative_to(stage)),'bytes':p.stat().st_size,'sha256':sha(p)} for p in sorted(stage.rglob('*')) if p.is_file()];assert len(inputs)==len(manifest)+1
 (OUT/'candidate-inputs.json').write_text(json.dumps(inputs,indent=2)+'\n')
 for row in manifest:assert sha(base/row['path'])==row['sha256']
 home=OUT/'gradle-home';subprocess.run(['/bin/cp','-c','-R',str(ROOT/'out/session-a/gradle-home'),str(home)],check=True)
 env=os.environ.copy();env.update(JAVA_HOME='/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home',GRADLE_USER_HOME=str(home),ANDROID_HOME='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk')
 dist=list(Path('/Users/paopao/.gradle/wrapper/dists/gradle-8.13-bin').glob('*/gradle-8.13/bin/gradle'));assert len(dist)==1
 command=[str(dist[0]),'--offline','--no-daemon',':app:assembleDebug',':app:assembleDebugAndroidTest'];began=time.monotonic()
 with (OUT/'build.log').open('w') as f:result=subprocess.run(command,cwd=stage,env=env,stdout=f,stderr=subprocess.STDOUT)
 assert result.returncode==0,(OUT/'build.log').read_text()[-6000:]
 records=[]
 for source in [stage/'app/build/outputs/apk/debug/app-debug.apk',stage/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
  target=OUT/source.name;shutil.copy2(source,target);records.append({'path':str(target),'sha256':sha(target),'bytes':target.stat().st_size})
 pins=json.loads((stage/'tools/content/map-release-manifest.json').read_text())['files'];assert len(pins)==168
 with zipfile.ZipFile(OUT/'app-debug.apk') as z,zipfile.ZipFile(ROOT/'out/session-a/map-fire-upload-build269/app-debug.apk') as old:
  for row in pins:assert hashlib.sha256(z.read(row['apk_path'])).hexdigest()==row['sha256']
  for row in parent['sixJniExact']:assert hashlib.sha256(z.read(row['entry'])).hexdigest()==row['sha256']
  changed_assets={'assets/'+r['path'].split('app/src/main/assets/',1)[1] for r in delta['paths'] if r['path'].startswith('app/src/main/assets/')}
  assets=[n for n in z.namelist() if n.startswith('assets/') and not n.endswith('/')]
  for n in assets:
   if n not in changed_assets:assert z.read(n)==old.read(n),n
  for r in delta['paths']:
   if r['path'].startswith('app/src/main/assets/'):assert hashlib.sha256(z.read('assets/'+r['path'].split('app/src/main/assets/',1)[1])).hexdigest()==r['afterSha256']
 with zipfile.ZipFile(OUT/'app-debug-androidTest.apk') as z:assert len([n for n in z.namelist() if n.endswith('.sg11') and hashlib.sha256(z.read(n)).hexdigest()==parent['genuine39FixtureSha256']])==1
 sdk=Path(env['ANDROID_HOME']);certs=[]
 for row in records:
  apk=Path(row['path']);xml=subprocess.check_output([str(sdk/'build-tools/35.0.0/aapt'),'dump','xmltree',str(apk),'AndroidManifest.xml'],text=True);(OUT/(apk.name+'.manifest.txt')).write_text(xml)
  if apk.name=='app-debug.apk':assert 'android:largeHeap' in xml and '(type 0x12)0xffffffff' in xml
  else:assert 'SessionAMapRepairInstrumentation' in xml and 'SessionAFireFlowInstrumentation' in xml
  cert=subprocess.check_output([str(sdk/'build-tools/35.0.0/apksigner'),'verify','--print-certs',str(apk)],text=True,env=env);(OUT/(apk.name+'.signature.txt')).write_text(cert);certs.append(next(x for x in cert.splitlines() if x.startswith('Signer #1 certificate SHA-256 digest:')))
 assert certs[0]==certs[1]
 for p,h in protected.items():assert sha(ROOT/p)==h
 for row in manifest:assert sha(base/row['path'])==row['sha256']
 report={'buildSuccessful':True,'canonicalBaseRevision':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'commonBase':parent['commonBase'],'parentCandidate':'MAP_FIRE_UPLOAD_BUILD269.json','completeInheritedFiles':len(manifest),'completeCandidateFiles':len(inputs),'changesFrom269':changes,'apks':records,'candidateInputManifest':str(OUT/'candidate-inputs.json'),'candidateInputManifestSha256':sha(OUT/'candidate-inputs.json'),'fixedPackagedInputsExact':168,'sixJniExact':parent['sixJniExact'],'protectedCurrentSixUnchanged':protected,'independentSourceBuildOutGradleCache':True,'wallSeconds':time.monotonic()-began,'buildLogSha256':sha(OUT/'build.log'),'freeBytesBefore':free,'freeBytesAfter':shutil.disk_usage(ROOT).free,'actualInstalled':False,'scope':'Full269 inherited production/resources/native plus exact five A media282 paths and actual attached Drawable Canvas check289. New original58 remains unbound to normal miss producer; no B WIP/Main224/test250/core/API/Bridge/Unity/4JNI changes. Build only; no269 scores or PC small-family/fullscreen/voice/ARM acceptance transferred. Actual independent installation/full normal callers/SaveRNGToken/restoration required.','wholeGoalComplete':False}
 (DOC/'MEDIA_CANDIDATE_BUILD290.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:report[k] for k in ['buildSuccessful','apks','completeCandidateFiles','actualInstalled']}),flush=True)
if __name__=='__main__':main()
