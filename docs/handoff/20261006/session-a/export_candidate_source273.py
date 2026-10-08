#!/usr/bin/env python3
"""Exact candidate source/resources/JNI plus portable source recipe; no caches."""
from pathlib import Path
import json,hashlib,tarfile,subprocess
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/candidate-source273'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 assert not OUT.exists();OUT.mkdir(parents=True);build=json.loads((DOC/'MAP_FIRE_UPLOAD_BUILD269.json').read_text());base=Path(build['candidateInputManifest']);assert sha(base)==build['candidateInputManifestSha256'];stage=base.parent/'source';files={row['path']:(stage/row['path'],row['sha256']) for row in json.loads(base.read_text())}
 for n in ['tools/content/native/session_a_cell_fire_instruction_worker.c','docs/handoff/20261006/session-a/build_portable_fire270.py','docs/handoff/20261006/session-a/PORTABLE_FIRE_SOURCE270.json','docs/handoff/20261006/session-a/MAP_FIRE_UPLOAD_BUILD269.json','docs/handoff/20261006/session-a/OWNERSHIP.json']:files[n]=(ROOT/n,sha(ROOT/n))
 archive=OUT/'sanguo11-mobile-candidate-source.tar.gz';manifest=[]
 with tarfile.open(archive,'w:gz') as t:
  for name,(path,expected) in sorted(files.items()):
   assert sha(path)==expected,name;t.add(path,arcname=name);manifest.append({'path':name,'bytes':path.stat().st_size,'sha256':expected})
 with tarfile.open(archive) as t:
  members={m.name:m for m in t if m.isfile()};assert len(members)==len(manifest)
  for row in manifest:assert hashlib.sha256(t.extractfile(members[row['path']]).read()).hexdigest()==row['sha256'],row['path']
 index=OUT/'source-files.json';index.write_text(json.dumps(manifest,indent=2)+'\n');report={'commonBase':build['commonBase'],'canonicalBaseRevision':build['canonicalBaseRevision'],'currentDeliveryRevision':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'archivePath':str(archive),'archiveBytes':archive.stat().st_size,'archiveSha256':sha(archive),'files':len(manifest),'everyArchivedFileShaVerified':True,'fileManifest':str(index),'fileManifestSha256':sha(index),'variantProductionBeforeAfter':build['variantSourceDiffersFromCanonical'],'exactCandidateApks':build['apks'],'portableNativeSourceIncluded':True,'completeOriginalFourAndCandidateTwoJniIncluded':True,'containsSdkOrBuildCaches':False,'containsDeviceBackups':False,'currentProtectedSixUnchanged':build['protectedCurrentSixUnchanged'],'actualInstalled':False,'scope':'Full exact isolated269 candidate inputs with two changed A Java files and two new fire ABI binaries, plus tracked original native source/portable build recipe and delivery metadata. Original168 resource inputs preserved; B Native WIP excluded. Source archive BuildConfig revision fallback may yield a different APK digest when rebuilt outside Git; all source/member/resource/native hashes explicit. No device/ARM/final combined acceptance transferred.','wholeGoalComplete':False};(DOC/'CANDIDATE_SOURCE273.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
