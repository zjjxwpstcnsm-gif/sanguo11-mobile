#!/usr/bin/env python3
"""Accept only final actual438 required setting normal/cold plus all original SHA rollback."""
from pathlib import Path
import json
from stage_serial401 import ROOT,D,sha
from read_session_state import read_session_state as read
from run_music_reserve378 import live_case
def main():
 case=ROOT/'out/session-a/closeout438';r=read(case/'session.json');assert r['stage']=='restored-verified' and not live_case(case);assert all(x['exactRegularFileSha'] for x in r['restoration'].values())
 apks={x['path']:x['sha256'] for x in read(D/'REQUIRED_BUILD437.json')['apks']};assert r['apks']==apks;assert r['workerObservation']['exitCode']==r['videoObservation']['exitCode']==0
 video=read(case/'video/video.json');assert video['apks']==apks and video['parts'] and not video['captureLimitReachedBeforeRestoration']
 for p in video['parts']:assert p['sha256']==p['deviceSha256']==p['deviceSha256AfterPull']==sha(Path(p['path']))
 progress=(case/'evidence/progress.txt').read_text();checks={}
 for label in ['cancel settings 0','cancel settings 14','accept local settings draft 0','accept local settings draft 14','cancel final new game confirmation 0','cancel final new game confirmation 14']:
  checks[label]='PASS full Save/allRNG/Token pure '+label in progress
 checks['noLegacyFallback']='PASS normal PC new-game disabled until explicit settings' in progress
 checks['sourceConstraint']='PASS real fixed source constraint disables incompatible lifespan' in progress
 checks['noImplicitDefaults']='PASS no original GUI defaults silently selected' in progress
 checks['exactNormalSaveRead']='PASS normal option save full readback' in progress
 accepted=bool(r['passed'] and r['normalPassed'] and r['coldProcess']['passed'] and r['coldProcess']['differentPid']);assert accepted and all(checks.values())
 files=[]
 for folder in ['evidence','cold-evidence']:
  for p in sorted((case/folder).rglob('*')):
   if p.is_file():files.append({'path':str(p.relative_to(case)),'sha256':sha(p),'bytes':p.stat().st_size})
 assert all((case/'evidence'/('source-'+str(i)+'-explicit.sg11')).is_file() for i in [0,14])
 report={'actualRequiredOpeningApks':apks,'normalSource0Source14AndFinalColdAccepted':accepted,'actualChecks':checks,'actualColdProcess':r['coldProcess'],'completeOriginalRestoration':r['restoration'],'rawVideos':len(video['parts']),'videoIndexSha256':sha(case/'video/video.json'),'sessionSha256':sha(case/'session.json'),'evidenceFiles':files,'previous412FailureTransferred':False,'scope':'Actual menu explicit choice/cancel/source fixed constraint/Source0 and14 new/save/read/final Source14 newprocess completeSave/allRNG/Token. Actual option dialog screenshot viewed for text controls. Preview/map entire geometry/pixels, all16/factions/670callers/first no-session/old31-39/ordinary384/real native contests/fire/military/media/ARM/final whole checks remain separate. Source0 settings screenshot was captured while preview map loading; not certified original preview pixels. Old412 thread error and ce83 first-source0 performance419/425FAIL remain. This new358 candidate no implicit legacy fallback; B media/ARM/all16/native commands remain open.','wholeGoalComplete':False};(D/'CLOSEOUT444.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'normalAndColdAccepted':accepted,'rawVideos':len(video['parts']),'originalFiles':sum(x['files'] for x in r['restoration'].values())}))
if __name__=='__main__':main()
