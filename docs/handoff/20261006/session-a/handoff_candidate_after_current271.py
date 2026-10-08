#!/usr/bin/env python3
"""Stop only future orchestration; existing exclusive data owner finishes intact."""
from pathlib import Path
import subprocess,json,os,signal,shlex,time
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';CASE=ROOT/'out/session-a/registered-normal239-all16-unique268/source-00'
def command(pid):
 p=subprocess.run(['ps','-p',str(pid),'-o','command='],capture_output=True,text=True);return shlex.split(p.stdout.strip()) if p.returncode==0 else []
def main():
 output=DOC/'CANDIDATE_DEVICE_HANDOFF271.json';assert not output.exists();parent=20672;helper=20713;observers=[20790,20791];args=command(parent);assert str(DOC/'run_remaining_normal_media.py') in args and str(CASE.parent) in args;assert str(DOC/'device_session.py') in command(helper) and str(CASE) in command(helper);state=json.loads((CASE/'session.json').read_text());assert state['stage']=='installed-verified' and not state.get('passed');assert all(str(CASE/'session.json') in command(pid) for pid in observers)
 report={'case':str(CASE),'oldCallerCohort':'SEARCH_OBSERVATION_TEST_BUILD239.json','nextCandidate':'MAP_FIRE_UPLOAD_BUILD269.json','scope':'Terminate only own parent future-source dispatcher20672, never data helper20713 or observers20790/20791, no process group signal. Current normal Source0/save/newPID/all-original SHA restoration finishes unchanged. Unstarted sources remain required on fresh candidate, no historical score transfer. New APK installation must wait actual restored-verified+passed+differentPID+observer0+data owner exit; goal stays active.','dispatcherPid':parent,'dataHelperPid':helper,'observerPids':observers,'stage':'validated-parent-only-handoff','wholeGoalComplete':False};output.write_text(json.dumps(report,indent=2)+'\n');os.kill(parent,signal.SIGTERM);time.sleep(.2);assert str(DOC/'device_session.py') in command(helper) and all(str(CASE/'session.json') in command(pid) for pid in observers);report['stage']='future-dispatcher-stopped-current-data-owner-verified-live';report['dataOwnerAndObserversNotSignalled']=True;output.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
