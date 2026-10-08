#!/usr/bin/env python3
"""Readonly exact canonical-to-built369 A delivery plan; never apply parallel frozen inputs."""
from pathlib import Path
import json,difflib,subprocess
from read_session_state import read_session_state as read
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];D=Path(__file__).resolve().parent

def main():
 out=D/'SOURCE_DELIVERY383.json';assert not out.exists();build=read(D/'MUSIC_RESERVE_BUILD369.json');manifest=Path(build['candidateInputManifest']);assert sha(manifest)==build['candidateInputManifestSha256'];stage=manifest.parent/'source';owners={x['path']:x['owner'] for x in read(ROOT/'docs/handoff/20261006/parallel-repair/APP_OWNERSHIP.json')['files']};own=set(read(D/'OWNERSHIP.json')['paths']);rows=read(manifest);diffs=[];patch=[];b_guard=[]
 for row in rows:
  p=row['path']
  if not p.startswith(('app/src/main/java/','app/src/main/res/','app/src/main/assets/')):continue
  source=stage/p;assert sha(source)==row['sha256'];current=ROOT/p;before=sha(current) if current.exists() else None
  owner=owners.get(p,'A-assets' if p.startswith(('app/src/main/assets/','app/src/main/res/')) else 'A-registered' if p in own else 'UNKNOWN')
  if owner=='B':assert before==row['sha256'],p;b_guard.append(p)
  if before==row['sha256']:continue
  assert owner.startswith('A'),(p,owner);diffs.append({'path':p,'owner':owner,'beforeSha256':before,'afterSha256':row['sha256'],'bytes':row['bytes'],'builtSourcePath':str(source)})
  if p.endswith(('.java','.json')):patch.append(''.join(difflib.unified_diff(current.read_text().splitlines(True) if current.exists() else [],source.read_text().splitlines(True),fromfile='a/'+p,tofile='b/'+p)))
 assert len(diffs)==9,len(diffs);(D/'SOURCE_DELIVERY383.patch').write_text(''.join(patch));jni=[]
 for row in build['sixJniExact']:
  matches=[p for p in build['protectedCurrentSixUnchanged'] if p.endswith('/'+row['entry'].removeprefix('lib/'))];assert len(matches)==1,matches;p=matches[0];current=ROOT/p;candidate=stage/p;assert sha(candidate)==row['sha256'];jni.append({'path':p,'currentSha256':sha(current),'builtSha256':sha(candidate),'differs':sha(current)!=sha(candidate),'parallelApplyAllowed':False})
 protected={p:sha(ROOT/p) for p in build['protectedCurrentSixUnchanged']};assert protected==build['protectedCurrentSixUnchanged'];frozenCore=[]
 for row in rows:
  p=row['path']
  if p.startswith(('core/src/main/','game-api/src/main/','game-runtime/src/main/')):
   assert (ROOT/p).is_file() and sha(ROOT/p)==row['sha256'],p;frozenCore.append(p)
 cpaths=[]
 for row in rows:
  p=row['path']
  if p in own and p.startswith(('tools/media/','tools/content/')) and p.endswith('.c') and (ROOT/p).exists() and sha(ROOT/p)!=row['sha256']:cpaths.append({'path':p,'beforeSha256':sha(ROOT/p),'afterSha256':row['sha256'],'builtSourcePath':str(stage/p)})
 report={'commonBase':build['commonBase'],'canonicalGitHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'built369Apks':build['apks'],'builtFullInputManifestSha256':sha(manifest),'nineExactOwnedAppDeliveryDeltas':diffs,'textDiffPatchSha256':sha(D/'SOURCE_DELIVERY383.patch'),'all16BPagesUnchanged':b_guard,'coreApiRuntimeProductionPathsVerifiedUnchanged':len(frozenCore),'sixJniCanonicalProtected':True,'packagedSixJni':jni,'originalFourUnchangedAndAdditionalTwoNeedSerialIntegration':sum(x['differs'] for x in jni)==2,'originalNativeCompileSourceDeltas':cpaths,'appliedToCanonical':False,'scope':'Exact staged A app delivery plan only, not stability approval. Canonical already has palette/theme/base fixes;9 later app assets/renderer/portrait/audio/mesh paths differ from actual369. No B production copy/overwrite. Four original and existing2 JNI canonical remain protected;2 compiled replacements and original C recipe require final serial integration. Music379 fails, full16/cold380 pending; applying9 does not accept whole goal or resolve ARM/source14first/media/commands/finalB.','wholeGoalComplete':False};out.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'nineOwnedAppPaths':len(diffs),'unchangedBPages':len(b_guard),'unchangedCoreApiRuntimeProductionFiles':len(frozenCore),'serialOnlyJniDifferences':sum(x['differs'] for x in jni),'applied':False}))
if __name__=='__main__':main()
