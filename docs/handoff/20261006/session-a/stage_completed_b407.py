#!/usr/bin/env python3
"""Apply only frozen completed B27 two-path increment over exact staged B26."""
from pathlib import Path,PurePosixPath
import json,tarfile,hashlib
from stage_serial401 import sha,ROOT,D,OUT,B
def main():
 assert not (D/'COMPLETED_B408.json').exists()
 new=B.parent/'native-opening-combined61-frozen-r27';r=json.loads((new/'frozen-report.json').read_text());assert r['pairRevision']==27 and r['actualSavedOfficerContentNormalColdAndUserRestorationPassed']
 assert sha(Path(r['sourceArchive']['path']))==r['sourceArchive']['sha256']
 oldguard=json.loads((B/'source-guard.json').read_text());guard=json.loads((new/'source-guard.json').read_text());allowed={'app/src/main/java/game/sanguo/mobile/ContentUi.java','app/src/main/java/game/sanguo/mobile/CurrentOfficerContent.java'}
 assert {p for p,h in guard.items() if oldguard.get(p)!=h}==allowed;assert set(oldguard)<=set(guard)
 records=json.loads((new/'build-inputs.json').read_text())['files'];manifest={x['path']:x for x in records};assert len(manifest)==r['sourceInputs']
 stage=OUT/'source';changes=[];seen=set()
 with tarfile.open(r['sourceArchive']['path']) as t:
  for m in t:
   p=m.name;assert m.isfile() and not m.islnk() and not m.issym() and not PurePosixPath(p).is_absolute() and '..' not in PurePosixPath(p).parts and p not in seen
   seen.add(p);raw=t.extractfile(m).read();assert len(raw)==manifest[p]['bytes'] and hashlib.sha256(raw).hexdigest()==manifest[p]['sha256'],p
   if p in guard:assert hashlib.sha256(raw).hexdigest()==guard[p],p
   if p in allowed:
    dst=stage/p;before=sha(dst) if dst.exists() else None;assert before==oldguard.get(p);dst.write_bytes(raw);dst.chmod(m.mode);changes.append({'path':p,'beforeSha256':before,'afterSha256':sha(dst),'mode':m.mode,'owner':'B'})
   elif p.startswith(('app/src/androidTest/java/game/sanguo/mobile/SessionBOfficerContent','docs/handoff/20261006/session-b/','tools/content/session_b_verify_officer_content','tools/content/session_b_freeze_officer_content')):
    dst=stage/p;dst.parent.mkdir(parents=True,exist_ok=True);dst.write_bytes(raw);dst.chmod(m.mode)
  assert seen==set(manifest)
 (D/'COMPLETED_B408.json').write_text(json.dumps({'bCompletedCommit':'cdd7f949','sourceArchive':r['sourceArchive'],'everyArchivedFileVerified':len(seen),'changedProductionPaths':changes,'allOtherBProductionExact26':True,'APathsUnchanged':True,'originalFourAndCandidateTwoJniUnchanged':True,'dirtyBRead':False,'scope':'Exact completed B27 original current roster UI only, no worktree WIP, no later core/API/runtime or A/shared path copied. Actual final combined build/install and all scopes must rerun.','wholeGoalComplete':False},indent=2)+'\n');print(json.dumps({'verified':len(seen),'productionPaths':len(changes)}))
if __name__=='__main__':main()
