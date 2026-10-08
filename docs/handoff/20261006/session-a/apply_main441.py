#!/usr/bin/env python3
"""Final serial import of completed candidate after actual normal/cold rollback."""
from pathlib import Path
import json,shutil,subprocess,os
from stage_serial401 import ROOT,D,sha
from read_session_state import read_session_state as read
from run_music_reserve378 import live_case
def main():
 assert not (D/'MAIN_SOURCE_APPLIED442.json').exists();case=ROOT/'out/session-a/closeout438';r=read(case/'session.json');assert r['stage']=='restored-verified' and r['passed'] and r['normalPassed'] and r['coldProcess']['passed'] and r['coldProcess']['differentPid'] and not live_case(case)
 assert all(x['exactRegularFileSha'] for x in r['restoration'].values());assert r['videoObservation']['exitCode']==r['workerObservation']['exitCode']==0
 plan=read(D/'MAIN_SYNC_PLAN440.json');assert not plan['mmainExists']
 assert subprocess.check_output(['git','rev-parse','main'],cwd=ROOT,text=True).strip()==plan['mainBefore'];assert not subprocess.check_output(['git','status','--porcelain'],cwd=plan['mainWorktree'])
 dirty=subprocess.check_output(['git','diff','--name-only'],cwd=ROOT,text=True).splitlines();assert all(p.startswith('docs/handoff/20261006/session-a/') for p in dirty),dirty
 for row in plan['syncPaths']+plan['jni']:
  src=Path(row['candidatePath']);dst=ROOT/row['path'];assert sha(src)==row['afterSha256'];assert (sha(dst) if dst.is_file() else None)==row['beforeSha256'],row['path']
 for row in plan['syncPaths']+plan['jni']:
  dst=ROOT/row['path'];src=Path(row['candidatePath'])
  if row['beforeSha256']!=row['afterSha256']:dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dst);dst.chmod(row['mode'])
  assert sha(dst)==row['afterSha256'] and (dst.stat().st_mode&0o777)==row['mode'],row['path']
 b=read(D/'REQUIRED_BUILD437.json');source=Path(b['sourcePath']);rows=read(Path(b['candidateInputManifest']));checked=0
 for row in rows:
  if row['path'].startswith(('docs/handoff/20261006/session-a/','out/','build/','.gradle/')):continue
  assert sha(ROOT/row['path'])==row['sha256'],row['path'];checked+=1
 report={'mainBefore':plan['mainBefore'],'mainNotYetAdvanced':True,'requestedLocalMainSyncAuthorized':True,'completedCandidateSource':str(source),'exactCandidateApks':b['apks'],'canonicalFilesShaVerified':checked,'importedChangedPaths':len(plan['syncPaths']),'finalSerialJni':plan['jni'],'sourceBeforeAfterManifest':'MAIN_SYNC_PLAN440.json','actualFinalNormalColdAccepted':True,'rawAcceptedSessionSha256':sha(case/'session.json'),'originalFilesRestored':r['restoration'],'noBDirtySourceImported':True,'protectedOldWorkspaceUntouched':True,'scope':'Only exact completed staged source delta, not entire B branch or another WIP. Original4 same bytes/modes; A new2 exact tested inputs. Current A reports retained. Ready for A commit, clean main fast-forward and separate main build/install. Source0 preparationFAIL/media/default mixed music/ARM remain unaccepted; no whole-goal completion inferred.','wholeGoalComplete':False};(D/'MAIN_SOURCE_APPLIED442.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'verifiedFiles':checked,'importedPaths':len(plan['syncPaths']),'mainAdvanced':False}))
if __name__=='__main__':main()
