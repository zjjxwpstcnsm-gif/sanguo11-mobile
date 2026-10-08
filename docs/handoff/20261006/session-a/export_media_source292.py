#!/usr/bin/env python3
"""Full exact media candidate plus current tracked tools/documents; no device data."""
from pathlib import Path
import json,hashlib,tarfile,subprocess,time
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/media-source292'
def sha(p):
 d=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1048576),b''):d.update(b)
 return d.hexdigest()
def main():
 assert not OUT.exists();print('Waiting actual290 successful receipt before source export',flush=True)
 while not (DOC/'MEDIA_CANDIDATE_BUILD290.json').exists():time.sleep(5)
 build=json.loads((DOC/'MEDIA_CANDIDATE_BUILD290.json').read_text());assert build['buildSuccessful'];inputs=Path(build['candidateInputManifest']);assert sha(inputs)==build['candidateInputManifestSha256'];stage=inputs.parent/'source'
 current=subprocess.check_output(['git','ls-files','-z'],cwd=ROOT).decode().split('\0');files={p:(ROOT/p,sha(ROOT/p)) for p in current if p and (ROOT/p).is_file()}
 # Candidate production/tests/resources/JNI must override the canonical files;
 # current documentation and reproducible tools override only older documents.
 for row in json.loads(inputs.read_text()):
  if row['path'].startswith('docs/') and row['path'] in files:continue
  files[row['path']]=(stage/row['path'],row['sha256'])
 for name in ['docs/handoff/20261006/session-a/MEDIA_CANDIDATE_BUILD290.json','docs/handoff/20261006/session-a/PORTRAIT_DRAW_CHECK289.json','docs/handoff/20261006/session-a/A_MEDIA_DELTA282.json']:
  files[name]=(ROOT/name,sha(ROOT/name))
 for path,digest in build['protectedCurrentSixUnchanged'].items():assert sha(ROOT/path)==digest
 OUT.mkdir(parents=True);archive=OUT/'sanguo11-mobile-media-candidate-source.tar.gz';manifest=[]
 with tarfile.open(archive,'w:gz') as t:
  for name,(path,digest) in sorted(files.items()):
   assert sha(path)==digest,name;t.add(path,arcname=name);manifest.append({'path':name,'bytes':path.stat().st_size,'sha256':digest})
 with tarfile.open(archive) as t:
  members={m.name:m for m in t if m.isfile()};assert len(members)==len(manifest)
  for row in manifest:assert hashlib.sha256(t.extractfile(members[row['path']]).read()).hexdigest()==row['sha256']
 index=OUT/'source-files.json';index.write_text(json.dumps(manifest,indent=2)+'\n')
 report={'commonBase':build['commonBase'],'canonicalBaseRevision':build['canonicalBaseRevision'],'currentDeliveryRevision':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'archivePath':str(archive),'archiveBytes':archive.stat().st_size,'archiveSha256':sha(archive),'files':len(manifest),'everyArchivedFileShaVerified':True,'fileManifest':str(index),'fileManifestSha256':sha(index),'exactCandidateApks':build['apks'],'changesFrom269':build['changesFrom269'],'portableNativeSourceIncluded':True,'completeOriginalFourAndCandidateTwoJniIncluded':True,'containsSdkOrBuildCaches':False,'containsDeviceBackups':False,'scope':'All complete290 inherited candidate inputs (including original168/untracked media/4original+2candidate JNI) plus current tracked source/tools/documents; exact variant production/test overrides canonical paths. No B Native WIP or user device data. Source archive BuildConfig fallback outside Git can change APK digest; no bit-repro APK claim. Pending actual290 callers, voices/fullscreen/map music/miss producer/finalB/ARM stay pending.','wholeGoalComplete':False}
 (DOC/'MEDIA_SOURCE292.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:report[k] for k in ['archivePath','archiveSha256','files','everyArchivedFileShaVerified']}),flush=True)
if __name__=='__main__':main()
