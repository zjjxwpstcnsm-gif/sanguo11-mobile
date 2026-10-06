#!/usr/bin/env python3
"""Original5d1df0/5d1ea0 discovery and5d16b0 score semantic receipts."""
import argparse,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from session_b_pc_geography_context import load_geography
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve prior evidence')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
 manifest=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();src=json.loads(manifest)['scenarios'][0];shared=(installation/'Media/scenario/Scenario.s11').read_bytes();raw=(installation/src['sourcePath']).read_bytes();assert sha(raw)==src['sourceSha256']
 w=NativeDebateFlow(installation,exe).world;load_original_data(w.u);w.load(shared,True);w.load(raw);w.call(0x73c500);w.call(0x73ca80);w.call(0x49b490,receiver=0x767cab8)
 for f,v in zip([0x4826e0,0x482700,0x482720],src['date']):w.call(f,v,receiver=w.root)
 w.call(0x4827b0,0,receiver=w.root);geography=load_geography(w,installation);w.call(0x493400,receiver=w.root,count=50000000)

 city=w.call(0x490a10,8,receiver=w.root);vector=0xc800000;w.u.mem_map(vector,0x1000);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rows=[]
 def contents():
  cursor=struct.unpack('<I',w.u.mem_read(vector+4,4))[0];ids=[];seen=set()
  while cursor:
   if cursor in seen:raise ValueError('Cycle')
   seen.add(cursor);p=struct.unpack('<I',w.u.mem_read(cursor+8,4))[0];ids.append((w.call(0x4883c0,receiver=p),p));cursor=struct.unpack('<I',w.u.mem_read(cursor,4))[0]
  return ids
 for native in [116,163,195,198,251,355,365,377,466]:
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,struct.pack('<I',23));actor=w.call(0x490b00,native,receiver=w.root);w.call(0x47c250,receiver=vector);chance=w.call(0x5d1df0,actor,city,vector,count=50000000);targets=contents();w.call(0x47c100,receiver=vector)
  row=dict(actorNative=native,politics=w.call(0x4890a0,receiver=actor),birthplace=struct.unpack('<i',w.u.mem_read(actor+0xe4,4))[0],cityBirthplaceRegion=struct.unpack('<i',w.u.mem_read(city+0x18,4))[0],eye=bool(w.call(0x4890f0,85,receiver=actor)),chance=chance,targets=[n for n,p in targets]);rows.append(row)
  w.call(0x47c250,receiver=vector);row['discovery']=bool(w.call(0x5d1ea0,actor,city,vector,count=50000000));row['chanceRng']=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];row['scoreInputs']=[]
  if row['discovery']:
   for n,p in contents():
    before=bytes(w.u.mem_read(0x8a5d44,4));score=w.call(0x5d16b0,p,actor,0,0,count=50000000);row['scoreInputs'].append(dict(targetNative=n,actorAge=w.call(0x488a20,receiver=actor),gap=w.call(0x489f80,n,receiver=actor),score=score,rngBefore=struct.unpack('<I',before)[0],rngAfter=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]))
  if baseline!=bytes(w.u.mem_read(0x7200000,0x300000)):raise ValueError('Discovery/score changedWorld')
  w.call(0x47c100,receiver=vector);print(json.dumps(row),flush=True)
 output.write_text(json.dumps(dict(source=src,exeSha=EXE_SHA,geography=geography,rows=rows,limits=['Only executed source discovery/score semantics, not normalcommand/result settlement','Original488a20 is actor age from original birth/date, not total ability; selection tie ordering separate','Native globalrandom before/after recorded; no random/getter/rule substitution']),indent=2)+'\n')
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
