#!/usr/bin/env python3
"""Compose immutable completed A397 and B26; never read B dirty production."""
from pathlib import Path, PurePosixPath
import json,hashlib,tarfile,ctypes,os,shutil,subprocess
ROOT=Path(__file__).resolve().parents[4]; D=Path(__file__).resolve().parent
OUT=ROOT/'out/session-a/serial-stage401'
B=Path('/Users/paopao/.codex/worktrees/f55b/sanguo11-mobile/out/session-b/native-opening-combined61-frozen-r26-v2')
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for block in iter(lambda:f.read(1048576),b''):h.update(block)
 return h.hexdigest()
def main():
 assert not OUT.exists(); assert shutil.disk_usage(ROOT).free>6*1024**3
 assert json.loads((D/'CURRENT_FIRE396.json').read_text())['normalColdCompleteOriginalRestorationAccepted']
 a=json.loads((D/'CURRENT_SOURCE397.json').read_text());b=json.loads((B/'frozen-report.json').read_text())
 assert a['commonBase']==b['latestFullMain']=='0e7b9bc2df90249a50851baeda58c7d183ea6059'
 assert b['pairRevision']==26 and b['actualMilitaryNormalColdAndUserRestorationPassed']
 assert sha(Path(a['archivePath']))==a['archiveSha256']
 assert sha(Path(b['sourceArchive']['path']))==b['sourceArchive']['sha256']
 amap={r['path']:r for r in json.loads(Path(a['fileManifest']).read_text())}
 assert sha(Path(a['fileManifest']))==a['fileManifestSha256']
 frozen=json.loads((B/'build-inputs.json').read_text())['files']; bmap={r['path']:r for r in frozen}
 assert len(bmap)==b['sourceInputs']==len(frozen)
 guard=json.loads((B/'source-guard.json').read_text());assert len(guard)==b['productionInputs']
 owners={r['path']:r['owner'] for r in json.loads((ROOT/'docs/handoff/20261006/parallel-repair/APP_OWNERSHIP.json').read_text())['files']}
 def select(p):
  return p.startswith(('core/','game-api/','game-runtime/','data/content/')) or owners.get(p)=='B' or p.startswith(('app/src/androidTest/java/game/sanguo/mobile/SessionB','app/src/androidTest/java/game/sanguo/core/SessionB','app/src/androidTest/assets/session-b/','docs/handoff/20261006/session-b/','tools/content/session_b_'))
 stage=OUT/'source';stage.mkdir(parents=True)
 libc=ctypes.CDLL('/usr/lib/libSystem.B.dylib',use_errno=True);clone=libc.clonefile;clone.argtypes=[ctypes.c_char_p,ctypes.c_char_p,ctypes.c_int];clone.restype=ctypes.c_int
 origin=Path(a['fileManifest']).parent/'source'
 for p,row in sorted(amap.items()):
  src=origin/p;assert sha(src)==row['sha256'];dst=stage/p;dst.parent.mkdir(parents=True,exist_ok=True)
  if clone(os.fsencode(src),os.fsencode(dst),0):shutil.copy2(src,dst)
  assert sha(dst)==row['sha256'] and src.stat().st_ino!=dst.stat().st_ino
 changes=[]; retained=[]; archived=0
 with tarfile.open(b['sourceArchive']['path']) as t:
  seen=set()
  for m in t:
   assert m.isfile() and not m.issym() and not m.islnk(),m.name
   p=m.name; assert not PurePosixPath(p).is_absolute() and '..' not in PurePosixPath(p).parts and p not in seen
   seen.add(p);assert p in bmap,p;raw=t.extractfile(m).read();assert len(raw)==bmap[p]['bytes'] and hashlib.sha256(raw).hexdigest()==bmap[p]['sha256'],p;archived+=1
   if p in guard:assert hashlib.sha256(raw).hexdigest()==guard[p],p
   if select(p):
    before=amap.get(p,{}).get('sha256');dst=stage/p;dst.parent.mkdir(parents=True,exist_ok=True);dst.write_bytes(raw);dst.chmod(m.mode)
    if before!=bmap[p]['sha256']:changes.append({'path':p,'owner':'B','beforeSha256':before,'afterSha256':bmap[p]['sha256'],'mode':m.mode})
   elif p in guard and amap.get(p,{}).get('sha256')!=guard[p]:retained.append({'path':p,'owner':owners.get(p,'A-or-shared'),'aSha256':amap.get(p,{}).get('sha256'),'bFrozenSha256':guard[p]})
  assert seen==set(bmap)
 # Original shared serialization/Unity/root Gradle stay in A; B must match them.
 forbidden=['game-runtime/src/main/java/game/sanguo/runtime/AndroidGameBridge.java','build.gradle','settings.gradle','gradle.properties']
 for p in forbidden:
  if p in bmap and p in amap:assert bmap[p]['sha256']==amap[p]['sha256'],('Frozen shared conflict',p)
 for p in bmap:
  if p.startswith('unity/') and p in amap:assert bmap[p]['sha256']==amap[p]['sha256'],p
 pins=json.loads((stage/'tools/content/map-release-manifest.json').read_text())['files'];assert len(pins)==168
 protected=json.loads((D/'MUSIC_RESERVE_BUILD369.json').read_text())['protectedCurrentSixUnchanged']
 for p,h in protected.items():assert sha(ROOT/p)==h
 jni=[{'path':p,'candidateSha256':sha(stage/p),'aSha256':amap[p]['sha256'],'bFrozenSha256':bmap.get(p,{}).get('sha256')} for p in amap if p.endswith('.so') and ('jniLibs/' in p)]
 assert len(jni)==6 and all(r['candidateSha256']==r['aSha256'] for r in jni)
 rows=[{'path':str(p.relative_to(stage)),'sha256':sha(p),'bytes':p.stat().st_size,'mode':p.stat().st_mode&0o777} for p in sorted(stage.rglob('*')) if p.is_file()];index=OUT/'candidate-inputs.json';index.write_text(json.dumps(rows,indent=2)+'\n')
 report={'commonBase':a['commonBase'],'aDeliveryCommit':a['currentDeliveryRevision'],'bCompletedCommit':'664c00f415dab2f17c889d42d0a5670888061213','aArchiveSha256':a['archiveSha256'],'bArchiveSha256':b['sourceArchive']['sha256'],'bEveryArchiveMemberShaVerified':archived,'sourcePath':str(stage),'candidateInputManifest':str(index),'candidateInputManifestSha256':sha(index),'completeCandidateFiles':len(rows),'completedBChanges':changes,'aOwnedOrSharedDifferencesRetained':retained,'jni':jni,'fixedInputs':168,'protectedCanonicalSixUnchanged':True,'dirtyBFilesRead':False,'deviceMutated':False,'buildSuccessful':False,'scope':'Serial immutable completed B26 only into complete A397 candidate. B-owned sources copied unchanged with exact frozen SHA/mode; A renderer/media/assets/new cache worker retained. B later WIP excluded. No canonical B production edit. Shared original Bridge/Unity/Gradle conflicts are terminal. New candidate build/install/allSaveRNG/UI/ARM/media acceptance remains pending.','wholeGoalComplete':False}
 (D/'SERIAL_STAGE402.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'files':len(rows),'bArchivedVerified':archived,'changedBPaths':len(changes),'retainedA':len(retained),'sourcePath':str(stage)}),flush=True)
if __name__=='__main__':main()
