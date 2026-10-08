#!/usr/bin/env python3
"""Original source0 domestic20-kind menu gate; platform expansion settings remain original/unknown."""
import argparse,json
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from session_b_pc_geography_context import load_geography
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier evidence')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
 manifest=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();src=json.loads(manifest)['scenarios'][0];shared=(installation/'Media/scenario/Scenario.s11').read_bytes();raw=(installation/src['sourcePath']).read_bytes();assert sha(raw)==src['sourceSha256']
 w=NativeDebateFlow(installation,exe).world;load_original_data(w.u);w.load(shared,True);w.load(raw);w.call(0x73c500);w.call(0x73ca80);w.call(0x49b490,receiver=0x767cab8)
 for f,v in zip([0x4826e0,0x482700,0x482720],src['date']):w.call(f,v,receiver=w.root)
 w.call(0x4827b0,0,receiver=w.root);geography=load_geography(w,installation);w.call(0x493400,receiver=w.root,count=50000000)

 before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));rows=[]
 for city in range(42):
  building=w.root+0x89730+city*0x38
  if not w.call(0x47a630,building):raise ValueError('Original city building absent')
  for native in list(range(31,40))+[30]+list(range(40,50)):
   gate=w.call(0x5bb4e0,building,native,count=50000000)
   exists=w.call(0x5bb3d0,building,native,count=50000000)
   pure=before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
   if not pure:raise ValueError('Original menu query changed world/RNG')
   rows.append(dict(cityNative=city,facilityNative=native,gate=gate,existing=exists,wholeWorldAndRngUnchanged=True))
 report=dict(source=src,exeSha=EXE_SHA,geography=geography,cases=rows,limits=['Original source ownership/geography/template setting; external Windows Expansion settings unknown','Pure native gate only; actual player menu/three workers/progress/completion/lifecycle separate','No field/frame/RNG/rule/getter substitution'])
 output.write_text(json.dumps(report,indent=2)+'\n');print('PASS original domestic menu gate',len(rows),'city-kind pairs;all world/RNG pure')
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
