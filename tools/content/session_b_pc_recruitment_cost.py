#!/usr/bin/env python3
"""Full original5d1f30 action-cost query. Does not certify opening AP or command admission."""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EIP,UC_X86_REG_ESP
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from session_b_pc_geography_context import load_geography
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard

def inspect(installation,output,index,native_site=None):
 output_guard(installation,output)
 if output.exists():raise ValueError("Preserve earlier evidence")
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
 manifest=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();source=json.loads(manifest)['scenarios'][index]
 shared=(installation/'Media/scenario/Scenario.s11').read_bytes();raw=(installation/source['sourcePath']).read_bytes();assert sha(raw)==source['sourceSha256']
 w=NativeDebateFlow(installation,exe).world;load_original_data(w.u);w.load(shared,True);w.load(raw);w.call(0x73c500);w.call(0x73c2b0)
 for function,value in zip([0x4826e0,0x482700,0x482720],source['date']):w.call(function,value,receiver=w.root)
 w.call(0x4827b0,0,receiver=w.root)
 geography=load_geography(w,installation)
 w.call(0x493400,receiver=w.root,count=50000000)
 rows=[]
 for native in (range(87) if native_site is None else [native_site]):
  site=w.call(0x490d00,native,receiver=w.root);assert w.call(0x47a630,site)
  before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));
  try:cost=w.call(0x5d1f30,site)
  except Exception as error:
   output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(dict(completeGoal=False,queryComplete=False,source=source,exeSha=EXE_SHA,sharedSha=sha(shared),function='0x5d1f30',nativeSiteId=native,sitePointer=hex(site),siteBytes=bytes(w.u.mem_read(site,0x80)).hex(),error=repr(error),nativeIp=hex(w.u.reg_read(UC_X86_REG_EIP)),nativeSp=hex(w.u.reg_read(UC_X86_REG_ESP)),invalidMemory=w.invalid,completedRows=rows,limits=['Failed complete cost query; no native result/caller/admission/AP budget certified']),indent=2)+'\n');raise

  assert before==bytes(w.u.mem_read(0x7200000,0x300000)) and rng==bytes(w.u.mem_read(0x8a5d44,4))
  vt=struct.unpack('<I',w.u.mem_read(site,4))[0];army=w.call(struct.unpack('<I',w.u.mem_read(vt+0x44,4))[0],receiver=site);army=army if army<0x80000000 else army-0x100000000
  rows.append(dict(nativeSiteId=native,nativeArmyId=army,cost=cost,fullOriginalWorldAndRngUnchanged=True));print(native,cost,flush=True)
 report=dict(geographyContext=geography,source=source,exeSha=EXE_SHA,sharedSha=sha(shared),sourceManifestSha=sha(manifest),function='0x5d1f30',functionCodeSha=sha(exe[0x1d1f30:0x1d1f7d]),rows=rows,completeGoal=False,limits=['Full original cost query only; original native42 modifier needs separately verified source meaning','Loaded AP0 is not opening budget; no normal admission, execute debit, caller/GUI or diplomacy certificate'])
 output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n')
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',required=True,type=Path);p.add_argument('--source',type=int,default=0);p.add_argument('--native-site',type=int,choices=range(87));a=p.parse_args();inspect(a.installation,a.output,a.source,a.native_site)
