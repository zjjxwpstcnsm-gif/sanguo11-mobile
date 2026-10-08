#!/usr/bin/env python3
"""Read-only exact tested candidate to canonical source sync plan; no WIP reads."""
from pathlib import Path
import json,subprocess
from stage_serial401 import ROOT,D,sha
def main():
 b=json.loads((D/'REQUIRED_BUILD437.json').read_text());assert b['buildSuccessful'];index=Path(b['candidateInputManifest']);assert sha(index)==b['candidateInputManifestSha256'];source=Path(b['sourcePath']);rows=json.loads(index.read_text());changes=[]
 for row in rows:
  p=row['path']
  if p.startswith(('docs/handoff/20261006/session-a/','out/','build/','.gradle/')):continue
  src=source/p;assert sha(src)==row['sha256'];dst=ROOT/p;old=sha(dst) if dst.is_file() else None
  if old!=row['sha256']:changes.append({'path':p,'beforeSha256':old,'afterSha256':row['sha256'],'mode':row['mode'],'candidatePath':str(src)})
 # Explicitly resolve final serial native deltas: original4 remain pinned,
 # A new2 are independently built/accepted inputs, not B's older cache pair.
 jni=[]
 for row in rows:
  if row['path'].endswith('.so') and 'jniLibs/' in row['path']:
   dst=ROOT/row['path'];jni.append({'path':row['path'],'beforeSha256':sha(dst) if dst.is_file() else None,'afterSha256':row['sha256'],'mode':row['mode'],'candidatePath':str(source/row['path'])})
 assert len(jni)==6;assert all(r['beforeSha256']==r['afterSha256'] for r in jni if r['path'].startswith('out/pc-native-runtime/'))
 for p in ['build.gradle','settings.gradle','gradle.properties','app/src/main/java/game/sanguo/mobile/bridge/AndroidGameBridge.java']:
  if (ROOT/p).is_file() and (source/p).is_file():assert sha(ROOT/p)==sha(source/p),p
 target=Path('/Users/paopao/.codex/worktrees/ba8a/sanguo11-mobile');assert not subprocess.check_output(['git','status','--porcelain'],cwd=target)
 report={'requestedTargetResolved':'main','mmainExists':bool(subprocess.check_output(['git','branch','--list','mmain'],cwd=ROOT).strip()),'mainWorktree':str(target),'mainBefore':subprocess.check_output(['git','rev-parse','main'],cwd=ROOT,text=True).strip(),'completeInheritedBase':b['commonBase'],'candidateSource':str(source),'candidateInputManifestSha256':b['candidateInputManifestSha256'],'exactCandidateApks':b['apks'],'syncPaths':changes,'jni':jni,'rootSharedGradleBridgeUnchanged':True,'modifiedAnySource':False,'scope':'Human user authorized closeout and main sync. Exact final candidate files only, all remaining B dirty worktree excluded. Own current A docs retained. Original4 unchanged; new2 explicit final serial update. Apply after final actual438 normal/cold/entireSHA restore, commit on A branch then main fast-forward only if still clean/current. Source input parity and a main build still required; knownSource0 performance/ARM/media remain open, not silently certified. No push authorized by original constraint.','wholeGoalComplete':False};(D/'MAIN_SYNC_PLAN440.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'syncPaths':len(changes),'mainBefore':report['mainBefore'],'mainClean':True}))
if __name__=='__main__':main()
