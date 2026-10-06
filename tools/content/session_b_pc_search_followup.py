#!/usr/bin/env python3
"""Original discovery setter and resolver for actual source0 undiscovered658.
No input choices, probability, rule, RNG or result returns are substituted.
Stops at original presentation/input boundaries, not complete GUI acceptance.
"""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EAX,UC_X86_REG_EIP
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from session_b_pc_geography_context import load_geography
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard

def inspect(installation,output,force_native=2,city_native=8,target_native=658,actors=None):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier evidence')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
 src=json.loads((ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_text())['scenarios'][0]
 raw=(installation/src['sourcePath']).read_bytes();assert sha(raw)==src['sourceSha256']
 w=NativeDebateFlow(installation,exe).world;load_original_data(w.u);w.load((installation/'Media/scenario/Scenario.s11').read_bytes(),True);w.load(raw)
 w.call(0x73c500);w.call(0x73ca80);w.call(0x49b490,receiver=0x767cab8)
 for f,v in zip([0x4826e0,0x482700,0x482720],src['date']):w.call(f,v,receiver=w.root)
 w.call(0x4827b0,0,receiver=w.root);geography=load_geography(w,installation);w.call(0x493400,receiver=w.root,count=50000000)
 force=w.call(0x490aa0,force_native,receiver=w.root);w.call(0x481480,0,receiver=force);w.call(0x4bc910,receiver=0x799895c,count=100000000)
 target=w.call(0x490b00,target_native,receiver=w.root);before=bytes(w.u.mem_read(target,0x190));w.call(0x4a5b20,target,city_native,receiver=0x799895c);after=bytes(w.u.mem_read(target,0x190))
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));rows=[]
 for native in (actors or [116,163,195,198,251,355,365,377,466]):
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,struct.pack('<I',1730814600));actor=w.call(0x490b00,native,receiver=w.root)
  chance=w.call(0x5c51c0,target,actor,0,0);gap=w.call(0x489f80,target_native,receiver=actor);ruler=w.call(0x489d40,receiver=actor);ruler_gap=w.call(0x489f80,ruler,receiver=actor);charm=w.call(0x4890b0,receiver=actor)
  date=w.call(0x5b9c00);roll=w.call(0x5ba4c0,date,native,target_native,gap,0,0,0)
  if baseline!=bytes(w.u.mem_read(0x7200000,0x300000))or struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]!=1730814600:raise ValueError('Admission queries mutate state')
  row=dict(actorNative=native,targetNative=target_native,chance=chance,gap=gap,charm=charm,actorRulerGap=ruler_gap,date=date,roll=roll,debateThreshold=chance+charm-ruler_gap,trace=[],complete=False);rows.append(row)
  boundaries={0x4f5450,0x4f54a0,0x520680};tracked=boundaries|{0x5d3c90,0x5d3d40,0x5ba4c0,0x5ba410,0x5c51c0,0x5c4f80,0x5c512d,0x5c5169,0x5c519d}
  def trace(u,ip,size,data):
   if ip not in tracked:return
   sp=u.reg_read(UC_X86_REG_ESP);row['trace'].append(dict(ip=hex(ip),eax=u.reg_read(UC_X86_REG_EAX),stack=bytes(u.mem_read(sp,88)).hex()))
   if ip in boundaries:row['boundary']=hex(ip);u.emu_stop()
  token=w.u.hook_add(UC_HOOK_CODE,trace)
  try:w.call(0x5d5220,target,actor,count=50000000);row['complete']=True
  except Exception:
   if'boundary'not in row:raise
  finally:w.u.hook_del(token)
  row['rngAfter']=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];row['targetRawAfter']=bytes(w.u.mem_read(target,0x190)).hex();print(json.dumps({k:v for k,v in row.items()if k not in ['trace','targetRawAfter']}),flush=True)
 output.write_text(json.dumps(dict(source=src,exeSha=EXE_SHA,geography=geography,discoveryBefore=before.hex(),discoveryAfter=after.hex(),changedOffsets=[i for i,(a,b)in enumerate(zip(before,after))if a!=b],rows=rows,limits=['Only actual discovery setter/resolver, no upstream consent or complete original GUI','Post-discovery RNG1730814600 is exact selected-five seed23 trace fixture, not certified PC startup RNG','No choices or rule/getter/RNG/stock returns replaced']),indent=2)+'\n')
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--force',type=int,default=2);p.add_argument('--city',type=int,default=8);p.add_argument('--target',type=int,default=658);p.add_argument('--actors',type=int,nargs='+');a=p.parse_args();inspect(a.installation,a.output,a.force,a.city,a.target,a.actors)
