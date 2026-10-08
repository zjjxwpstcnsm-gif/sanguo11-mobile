#!/usr/bin/env python3
"""Read-only terminal Source0 evidence audit; no device/install/score transfer."""
from pathlib import Path
import json,subprocess,sys,time
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';CASE=ROOT/'out/session-a/registered-normal239-all16-unique268/source-00'
def active():
 for pid in [20713,20790,20791]:
  p=subprocess.run(['ps','-p',str(pid),'-o','command='],capture_output=True,text=True)
  if p.returncode==0 and str(CASE) in p.stdout:return True
 return False
def main():
 files=[DOC/n for n in ['REGISTERED_SOURCE00_ACCEPTANCE283.json','registered-source00-memory284.json','registered-source00-widget285.json']];assert not any(f.exists() for f in files);print('Waiting same actual Source0 data owner and observers; read-only audit',flush=True)
 while active():time.sleep(5)
 from run_remaining_normal_media import accepted
 accepted(CASE,0)
 subprocess.run([sys.executable,str(DOC/'audit_session_memory.py'),'--session',str(CASE),'--output',str(files[1])],cwd=ROOT,check=True)
 subprocess.run([sys.executable,str(DOC/'audit_actual_widget_readability.py'),'--session',str(CASE),'--output',str(files[2])],cwd=ROOT,check=True)
 subprocess.run([sys.executable,str(DOC/'audit_completed_normal_source.py'),'--session',str(CASE),'--source','0','--helper-pid','20713','--memory-audit',str(files[1]),'--widget-audit',str(files[2]),'--output',str(files[0])],cwd=ROOT,check=True)
 d=json.loads(files[0].read_text());assert d['cohortReceipt']=='SEARCH_OBSERVATION_TEST_BUILD239.json';print('Frozen old165/239 Source0 only, no269/280 score transfer',flush=True)
if __name__=='__main__':main()
