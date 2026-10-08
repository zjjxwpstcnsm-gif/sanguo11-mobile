#!/usr/bin/env python3
"""Pinned source0 original4af7d0 special gate and4afd60 same-city decision.
No relationship/probability/hash/controller/RNG function replacement.
"""
import argparse,json,struct
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

 w.u.mem_map(0xc600000,0x1000);result=0xc600000
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));rows=[]
 actors=[116,163,198,251,365,377,466]
 for target_native in [222,235,348,449,532,658,116,163,365]:
  target=w.call(0x490b00,target_native,receiver=w.root)
  for actor_native in actors:
   actor=w.call(0x490b00,actor_native,receiver=w.root);w.u.mem_write(result,struct.pack('<i',-99))
   forced=w.call(0x4af7d0,target,actor,0,result,receiver=0x799895c,count=50000000)
   forced_result=struct.unpack('<i',w.u.mem_read(result,4))[0]
   date=w.call(0x5b9c00)
   decision=w.call(0x4afd60,target,actor,0,date,receiver=0x799895c,count=50000000)
   pure=baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
   if not pure:raise ValueError('Original same-city mode0 gate changed world/RNG')
   rows.append(dict(targetNative=target_native,actorNative=actor_native,forced=bool(forced),forcedResult=forced_result,decision=decision,dateArgument=date,wholeWorldAndRngUnchanged=pure))
 report=dict(source=src,exeSha=EXE_SHA,geography=geography,cases=rows,limits=['Exact original source objects and controller flags','Original5c5940 caller uses5b9c00 date hash and manager799895c; these are retained','No current relation projection or live menu/command admission inferred'])
 output.write_text(json.dumps(report,indent=2)+'\n');print('PASS original special gate',len(rows),'pairs;forced',sum(r['forced']for r in rows),'world/RNG pure')
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
