#!/usr/bin/env python3
"""Original full47army AP replenishment driver; opening caller remains separate."""
import argparse,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from session_b_pc_geography_context import load_geography
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard

def inspect(installation,output,selected_force=None,source_index=0):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier evidence')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA;manifest=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();source=json.loads(manifest)['scenarios'][source_index];raw=(installation/source['sourcePath']).read_bytes();assert sha(raw)==source['sourceSha256']
 w=NativeDebateFlow(installation,exe).world;load_original_data(w.u);w.load((installation/'Media/scenario/Scenario.s11').read_bytes(),True);w.load(raw);w.call(0x73c500);w.call(0x73ca80)
 for f,v in zip([0x4826e0,0x482700,0x482720],source['date']):w.call(f,v,receiver=w.root)
 w.call(0x4827b0,0,receiver=w.root);geography=load_geography(w,installation);w.call(0x493400,receiver=w.root,count=50000000)
 selected=None
 if selected_force is not None:
  force=w.call(0x490aa0,selected_force,receiver=w.root)
  if not w.call(0x47a630,force):raise ValueError('Selected original force absent')
  before_player=struct.unpack('<i',w.u.mem_read(force+0x60,4))[0];w.call(0x481480,0,receiver=force)
  selected=dict(nativeForce=selected_force,playerSlot=0,beforePlayerSlot=before_player,afterPlayerSlot=struct.unpack('<i',w.u.mem_read(force+0x60,4))[0],originalSetter='0x481480',controllerPredicate=bool(w.call(0x480fa0,receiver=force)),scope='Explicit selected-player context, original picker not executed')
 scratch=0xc300000;w.u.mem_map(scratch,0x1000);rows=[]
 for native in range(47):
  p=w.call(0x490ad0,native,receiver=w.root);b=bytes(w.u.mem_read(p,0x50));valid=bool(w.call(0x47a630,p));row=dict(nativeId=native,valid=valid,rawBefore=b.hex(),apBefore=b[0x2c])
  if valid:
   w.call(0x47e9b0,scratch,receiver=p,count=50000000);counts=bytes(w.u.mem_read(scratch,64));leader=struct.unpack_from('<i',b,12)[0];person=w.call(0x490b00,leader&0xffffffff,receiver=w.root);table=struct.unpack_from('<I',b)[0];owner=w.call(struct.unpack('<I',w.u.mem_read(table+0x40,4))[0],receiver=p);force=w.call(0x490aa0,owner,receiver=w.root);adviserId=struct.unpack('<i',w.u.mem_read(force+8,4))[0]if force else -1;adviser=w.call(0x490b00,adviserId&0xffffffff,receiver=w.root)
   row.update(owner=owner,leaderNative=leader,adviserNative=adviserId,cities=struct.unpack_from('<h',counts,0)[0],officers=struct.unpack_from('<h',counts,2)[0],countsHex=counts.hex(),leaderValid=bool(w.call(0x47a630,person)),adviserValid=bool(w.call(0x47a630,adviser)))
   if row['leaderValid']:row.update(leaderLeadership=w.call(0x489070,receiver=person),leaderCharm=w.call(0x4890b0,receiver=person))
   if row['adviserValid']:row['adviserIntelligence']=w.call(0x489090,receiver=adviser)
  rows.append(row)
 before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));w.call(0x598880 if selected_force is None else 0x4bc910,receiver=0x9771590 if selected_force is None else 0x799895c,count=100000000);after=bytes(w.u.mem_read(0x7200000,0x300000));assert rng==bytes(w.u.mem_read(0x8a5d44,4))
 for row in rows:row['apAfter']=w.u.mem_read(w.call(0x490ad0,row['nativeId'],receiver=w.root)+0x2c,1)[0]
 report=dict(source=source,geographyContext=geography,exeSha=EXE_SHA,selectedPlayerContext=selected,driver='0x598880'if selected_force is None else '0x4bc910',perArmy='0x5986a0',countsGetter='0x47e9b0',rows=rows,nativeRngUnchanged=True,changedOffsets=[i for i,(a,b)in enumerate(zip(before,after))if a!=b],completeGoal=False,limits=['Full original driver on source/postload/geography; not complete normal opening/turn caller certificate','Source controller flags and all850 effective/native identities remain original; project670 coverage not assumed','No AP/rule/getter/RNG substitution; inherited platform scaffolding; no invented opening AP fixture'])
 output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps([(r['nativeId'],r['apBefore'],r['apAfter'])for r in rows if r['valid']]),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--source',type=int,choices=range(16),default=0);p.add_argument('--selected-force',type=int,choices=range(42));a=p.parse_args();inspect(a.installation,a.output,a.selected_force,a.source)
