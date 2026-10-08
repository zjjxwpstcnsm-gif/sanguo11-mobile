#!/usr/bin/env python3
"""Readonly fixed B r16 evidence: inherited recovery latch, not a new OOM claim."""
from pathlib import Path
import json,tarfile,hashlib,xml.etree.ElementTree as ET,time
from read_session_state import read_session_state
from run_remaining_normal_media import sha
from run_candidate_regression287 import live_case
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent
B=Path('/Users/paopao/.codex/worktrees/f55b/sanguo11-mobile');CASE=ROOT/'out/session-a/fire-cache-installed297'
def members(path):
 result={};prefs=None
 with tarfile.open(path,'r:') as t:
  for m in t:
   if not m.isfile():continue
   name=m.name.removeprefix('./');raw=t.extractfile(m).read();result[name]=hashlib.sha256(raw).hexdigest()
   if name=='shared_prefs/map-renderer.xml':prefs=raw
 return result,prefs
def main():
 output=DOC/'B_R16_RECOVERY_PREFS302.json';assert not output.exists();print('Waiting current297/298 exit before independent artifact SHA workload',flush=True)
 while True:
  current=read_session_state(CASE/'session.json');video=read_session_state(DOC/'FIRE_CACHE_VIDEO298_LAUNCH.json')
  if current['stage']=='restored-verified' and video['stage']=='terminal' and not live_case(CASE):break
  time.sleep(5)
 acceptance=B/'docs/handoff/20261006/session-b/NATIVE_R16_AI_ACTOR_ACCEPTANCE.json';d=read_session_state(acceptance);assert d['candidateOnly'] and d['boundedBatchPassed'] and not d['completeGoal'];results=Path(d['actualResultsPath']);assert sha(results)==d['actualResultsSha256'];actual=read_session_state(results);assert actual['passed'] and actual['instrumentationPassed'] and actual['coldPassed'] and actual['terminalColdPassed']
 for p,h in actual['apks'].items():assert sha(Path(p))==h
 folder=results.parent;restored={};prefs=[]
 for group in ['internal','external']:
  before,pb=members(folder/(group+'-before.tar'));after,pa=members(folder/(group+'-restored.tar'));assert before==after
  restored[group]={'exactRegularPathSetAndSha':True,'files':len(before)}
  if group=='internal':
   _,post=members(folder/'internal-post-test.tar')
   for stage,raw in [('before',pb),('post-test',post),('restored',pa)]:
    assert raw is not None;root=ET.fromstring(raw);values={x.attrib['name']:x.attrib.get('value') for x in root};assert values['nativeFailure']==values['nativeSession']=='true';prefs.append({'stage':stage,'sha256':hashlib.sha256(raw).hexdigest(),'values':values})
 assert restored['internal']['files']==15 and restored['external']['files']==772
 frozen=read_session_state(Path(d['frozenReportPath']));guard_path=Path(frozen['sourceGuardPath']);assert sha(guard_path)==frozen['sourceGuardSha256'];guard=read_session_state(guard_path);name='app/src/main/java/game/sanguo/mobile/MapHost.java';assert guard[name]==sha(ROOT/name)
 source=(ROOT/name).read_text();assert 'if(safeMode&&!manual)' in source and 'retry.setOnClickListener(v->retry3D())' in source and 'putBoolean("nativeFailure",false).commit()' in source
 report={'sourceAcceptance':str(acceptance),'sourceAcceptanceSha256':sha(acceptance),'actualResultsSha256':sha(results),'apks':actual['apks'],'fullOriginalTarMemberPathAndShaRestored':restored,'mapRendererPreferences':prefs,'frozenMapHostSha256':guard[name],'beforeAlreadyHadFailureLatch':True,'newAllocationCrashOrOomProven':False,'normalRetry3DRequiredForPresentationAcceptance':True,'normalRetry3DActuallyAccepted':False,'sourceCodeObservation':'Same exact A MapHost bytes: prior nativeFailure sets process safeMode; automatic start is blocked with recovery page; real retry button enters manual=true; only verified candidate output clears persisted/process failure latch. Do not clear prefs/direct flags for acceptance.','scope':'Fixed B r16 has finite rule/input/save/15+772 restoration proof; its recovery page starts with inherited true prefs, not independent proof new B APK allocated/crashed/OOM. Normal user Retry3D/actual output/current Token/SaveRNG pure/arena/cold required; not accepted here. No B/source/5582/user data edits, no tool message or WIP merge. AI judgment actor remains distinct from speech/voice.','wholeGoalComplete':False}
 output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'beforeFailureLatch':True,'restored':restored,'normalRetry3DAccepted':False}),flush=True)
if __name__=='__main__':main()
