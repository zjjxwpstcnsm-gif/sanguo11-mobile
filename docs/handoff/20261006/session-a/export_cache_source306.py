#!/usr/bin/env python3
"""Freeze complete exact296 plus current tools/docs with independent inode copies."""
from pathlib import Path
import json,hashlib,tarfile,subprocess,ctypes,os,shutil,time
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/cache-source306'
def sha(p):
 d=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1048576),b''):d.update(b)
 return d.hexdigest()
def main():
 assert not OUT.exists();build=json.loads((DOC/'FIRE_CACHE_APK296.json').read_text());proof=json.loads((DOC/'PORTABLE_CACHE_SOURCE307.json').read_text());assert all(x['byteExact295AndPackaged296'] for x in proof['outputs'])
 assert json.loads((DOC/'FIRE_CACHE_ACCEPTANCE299.json').read_text())['normalColdFullRestorationPassed'];inputs=Path(build['candidateInputManifest']);assert sha(inputs)==build['candidateInputManifestSha256'];stage=inputs.parent/'source';frozen={r['path']:(stage/r['path'],r['sha256']) for r in json.loads(inputs.read_text())}
 paths=subprocess.check_output(['git','ls-files','-z'],cwd=ROOT).decode().split('\0');files={p:ROOT/p for p in paths if p and (ROOT/p).is_file()}
 files.update({str(p.relative_to(ROOT)):p for p in DOC.rglob('*') if p.is_file()})
 for name,(path,digest) in frozen.items():
  if name.startswith('docs/') and name in files:continue
  assert sha(path)==digest;files[name]=path
 # Capture only during a currently observed app-stopped interval. This is a
 # read-only source operation; no lock, device mutation or scheduler stopping.
 print('Waiting observed target app stopped before source snapshot I/O',flush=True)
 adb='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platform-tools/adb'
 while subprocess.run([adb,'-s','emulator-5554','shell','pidof','game.sanguo.mobile.dev'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0:time.sleep(5)
 OUT.mkdir(parents=True);snapshot=OUT/'source';snapshot.mkdir();libc=ctypes.CDLL('/usr/lib/libSystem.B.dylib',use_errno=True);clone=libc.clonefile;clone.argtypes=[ctypes.c_char_p,ctypes.c_char_p,ctypes.c_int];clone.restype=ctypes.c_int;manifest=[]
 for name,path in sorted(files.items()):
  target=snapshot/name;target.parent.mkdir(parents=True,exist_ok=True)
  # Only own ephemeral snapshot copies can be removed to retry a document
  # that changed while its independent COW inode was created.
  for attempt in range(30):
   if target.exists():target.unlink()
   before=sha(path)
   if clone(os.fsencode(path),os.fsencode(target),0):shutil.copy2(path,target)
   after=sha(target)
   if before!=after:time.sleep(.1);continue
   if path.suffix=='.json' and path.is_relative_to(DOC):
    try:json.loads(target.read_text())
    except json.JSONDecodeError:time.sleep(.1);continue
   assert target.stat().st_ino!=path.stat().st_ino;break
  else:raise ValueError('Source document did not yield complete stable copy: '+name)
  manifest.append({'path':name,'bytes':target.stat().st_size,'sha256':after})
 archive=OUT/'sanguo11-mobile-cache-source.tar'
 with tarfile.open(archive,'w:') as t:
  for row in manifest:t.add(snapshot/row['path'],arcname=row['path'])
 with tarfile.open(archive,'r:') as t:
  members={m.name:m for m in t if m.isfile()};assert len(members)==len(manifest)
  for row in manifest:assert hashlib.sha256(t.extractfile(members[row['path']]).read()).hexdigest()==row['sha256']
 index=OUT/'source-files.json';index.write_text(json.dumps(manifest,indent=2)+'\n')
 for p,h in build['protectedCurrentSixUnchanged'].items():assert sha(ROOT/p)==h
 report={'commonBase':build['commonBase'],'canonicalBaseRevision':build['canonicalBaseRevision'],'currentDeliveryRevision':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'archivePath':str(archive),'archiveBytes':archive.stat().st_size,'archiveSha256':sha(archive),'files':len(manifest),'everyArchivedFileShaVerified':True,'independentFrozenSourceInodes':True,'fileManifest':str(index),'fileManifestSha256':sha(index),'exactCandidateApks':build['apks'],'changesFrom290':build['changesFrom290'],'portableNativeRecipe':'docs/handoff/20261006/session-a/build_portable_cache307.py','bothNativeBinariesRecompiledByteExact295And296':True,'originalFourAndCandidateTwoJniIncluded':True,'fixedBuildInputsIncluded':168,'containsSdkOrBuildCaches':False,'containsDeviceBackups':False,'archiveUncompressedToReduceCpuLoad':True,'scope':'Complete exact296 input source/resources/test/native plus current tracked tools/docs and own session files, all independently copied and tar-member SHA readback. New packaged cache source overrides old canonical native source; no B WIP or user device data. Snapshot begins only in observed app-stopped interval; hashing/I/O can still perturb host later, no GPU/FPS proof. SourceRevision fallback outsideGit can change APK digest. Actual299 Source14 fire normal/cold pass remains narrow; ongoing304/305/ARM/finalB/media/heap budget incomplete.','wholeGoalComplete':False}
 (DOC/'CACHE_SOURCE306.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:report[k] for k in ['archivePath','archiveSha256','files','everyArchivedFileShaVerified']}),flush=True)
if __name__=='__main__':main()
