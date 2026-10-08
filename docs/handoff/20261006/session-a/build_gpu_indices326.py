#!/usr/bin/env python3
"""Independent full310 source with one exact A mesh314 change; no B WIP."""
from pathlib import Path
import json,subprocess,shutil,os,ctypes,time,zipfile,hashlib
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/gpu-index-build326'
def main():
 assert not OUT.exists() and json.loads((DOC/'GPU_INDEX_PROBE325.json').read_text())['hostChecksPassed']
 free=shutil.disk_usage(ROOT).free;assert free>4*1024**3
 parent=json.loads((DOC/'MESH_ALLOCATION_BUILD316.json').read_text());product=json.loads((DOC/'FIRE_CACHE_APK296.json').read_text());delta=json.loads((DOC/'GPU_INDEX_DELTA324.json').read_text());inputs=Path(parent['candidateInputManifest']);assert sha(inputs)==parent['candidateInputManifestSha256'];manifest=json.loads(inputs.read_text());base=inputs.parent/'source';protected=product['protectedCurrentSixUnchanged'];head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
 for p,h in protected.items():assert sha(ROOT/p)==h
 OUT.mkdir(parents=True);stage=OUT/'source';stage.mkdir();libc=ctypes.CDLL('/usr/lib/libSystem.B.dylib',use_errno=True);clone=libc.clonefile;clone.argtypes=[ctypes.c_char_p,ctypes.c_char_p,ctypes.c_int];clone.restype=ctypes.c_int
 for row in manifest:
  origin=base/row['path'];assert sha(origin)==row['sha256'];target=stage/row['path'];target.parent.mkdir(parents=True,exist_ok=True)
  if clone(os.fsencode(origin),os.fsencode(target),0):shutil.copy2(origin,target)
  assert sha(target)==row['sha256'] and target.stat().st_ino!=origin.stat().st_ino
 for row in delta['paths']:
  target=stage/row['path'];assert (sha(target)==row['beforeSha256']) if row['beforeSha256'] else not target.exists();origin=Path(row['stagedPath']);assert sha(origin)==row['afterSha256'];target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(origin.read_bytes())
 records=[{'path':str(p.relative_to(stage)),'sha256':sha(p),'bytes':p.stat().st_size} for p in sorted(stage.rglob('*')) if p.is_file()];assert len(records)==len(manifest)+1;(OUT/'candidate-inputs.json').write_text(json.dumps(records,indent=2)+'\n')
 home=OUT/'gradle-home';subprocess.run(['/bin/cp','-c','-R',str(ROOT/'out/session-a/gradle-home'),str(home)],check=True);env=os.environ.copy();env.update(JAVA_HOME='/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home',GRADLE_USER_HOME=str(home),ANDROID_HOME='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk');dist=list(Path('/Users/paopao/.gradle/wrapper/dists/gradle-8.13-bin').glob('*/gradle-8.13/bin/gradle'));assert len(dist)==1;began=time.monotonic()
 with (OUT/'build.log').open('w') as f:r=subprocess.run([str(dist[0]),'--offline','--no-daemon',':app:assembleDebug',':app:assembleDebugAndroidTest'],cwd=stage,env=env,stdout=f,stderr=subprocess.STDOUT)
 assert r.returncode==0,(OUT/'build.log').read_text()[-6000:]
 apks=[]
 for source in [stage/'app/build/outputs/apk/debug/app-debug.apk',stage/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
  target=OUT/source.name;shutil.copy2(source,target);apks.append({'path':str(target),'sha256':sha(target),'bytes':target.stat().st_size})
 with zipfile.ZipFile(OUT/'app-debug.apk') as z,zipfile.ZipFile(ROOT/'out/session-a/fire-cache-apk296/app-debug.apk') as old:
  assets=[n for n in old.namelist() if n.startswith('assets/') and not n.endswith('/')];assert set(assets)=={n for n in z.namelist() if n.startswith('assets/') and not n.endswith('/')}
  for n in assets:assert z.read(n)==old.read(n),n
  for row in product['sixJniExact']:assert hashlib.sha256(z.read(row['entry'])).hexdigest()==row['sha256']
  pins=json.loads((stage/'tools/content/map-release-manifest.json').read_text())['files'];assert len(pins)==168
  for row in pins:assert hashlib.sha256(z.read(row['apk_path'])).hexdigest()==row['sha256']
 fixture=parent['genuine39FixtureSha256'] if 'genuine39FixtureSha256' in parent else json.loads((DOC/'SEARCH_OBSERVATION_TEST_BUILD239.json').read_text())['genuine39FixtureSha256']
 with zipfile.ZipFile(OUT/'app-debug-androidTest.apk') as z:assert len([n for n in z.namelist() if n.endswith('.sg11') and hashlib.sha256(z.read(n)).hexdigest()==fixture])==1
 sdk=Path(env['ANDROID_HOME']);certs=[]
 for row in apks:
  apk=Path(row['path']);xml=subprocess.check_output([str(sdk/'build-tools/35.0.0/aapt'),'dump','xmltree',str(apk),'AndroidManifest.xml'],text=True);(OUT/(apk.name+'.manifest.txt')).write_text(xml)
  if apk.name=='app-debug.apk':assert 'android:largeHeap' in xml and '(type 0x12)0xffffffff' in xml
  else:assert 'SessionAScenarioPrepareInstrumentation' in xml and 'SessionAFireFlowInstrumentation' in xml
  cert=subprocess.check_output([str(sdk/'build-tools/35.0.0/apksigner'),'verify','--print-certs',str(apk)],text=True,env=env);certs.append(next(x for x in cert.splitlines() if x.startswith('Signer #1 certificate SHA-256 digest:')))
 assert certs[0]==certs[1]
 for p,h in protected.items():assert sha(ROOT/p)==h
 for row in manifest:assert sha(base/row['path'])==row['sha256']
 report={'buildSuccessful':True,'sourceRevision':head,'commonBase':product['commonBase'],'parentCandidate':'MESH_ALLOCATION_BUILD316.json','completeInheritedFiles':len(manifest),'completeCandidateFiles':len(records),'changesFrom316':delta['paths'],'apks':apks,'candidateInputManifest':str(OUT/'candidate-inputs.json'),'candidateInputManifestSha256':sha(OUT/'candidate-inputs.json'),'fixedPackagedInputsExact':168,'allPackagedAssetsExact296':len(assets),'sixJniExact':product['sixJniExact'],'protectedCurrentSixUnchanged':protected,'gameLargeHeap':True,'independentSourceBuildOutGradleCache':True,'wallSeconds':time.monotonic()-began,'buildLogSha256':sha(OUT/'build.log'),'freeBytesBefore':free,'freeBytesAfter':shutil.disk_usage(ROOT).free,'actualInstalled':False,'scope':'Full316 complete inputs/resources/tests plus A per-engine immutable index registry324 and renderer. Same original streams/encoder/budgets;325 host lifetime/encoding and Android compilation passed. Product/API/Bridge/Unity/original4/allassets unchanged otherwise; no B Native WIP. New actual normalSource14/install/save restore evidence required; no prior scores transferred.','wholeGoalComplete':False}
 (DOC/'GPU_INDEX_BUILD326.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'apks':apks,'buildSuccessful':True,'actualInstalled':False}),flush=True)
if __name__=='__main__':main()
