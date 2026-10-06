#!/usr/bin/env python3
"""Original top SEARCH5d5970 source0 exact ordinary argument tuple.
Uses original selected-player setter/AP driver, no fee/probability/RNG/result replacement.
UI boundaries are observed only; they are never answered by a fake return.
"""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EIP,UC_X86_REG_ESP,UC_X86_REG_EAX
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from session_b_pc_geography_context import load_geography
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard

def inspect(installation,output,selected_force=2,city_native=8,actors=None):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier evidence')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
 manifest=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();src=json.loads(manifest)['scenarios'][0];shared=(installation/'Media/scenario/Scenario.s11').read_bytes();raw=(installation/src['sourcePath']).read_bytes();assert sha(raw)==src['sourceSha256']
 w=NativeDebateFlow(installation,exe).world;load_original_data(w.u);w.load(shared,True);w.load(raw);w.call(0x73c500);w.call(0x73ca80);w.call(0x49b490,receiver=0x767cab8)
 for f,v in zip([0x4826e0,0x482700,0x482720],src['date']):w.call(f,v,receiver=w.root)
 w.call(0x4827b0,0,receiver=w.root);geography=load_geography(w,installation);w.call(0x493400,receiver=w.root,count=50000000)

 force=w.call(0x490aa0,selected_force,receiver=w.root);w.call(0x481480,0,receiver=force);w.call(0x4bc910,receiver=0x799895c,count=100000000)
 site=w.call(0x490d00,city_native,receiver=w.root);city=w.call(0x490a10,city_native,receiver=w.root);army_native=w.call(0x4c0c30,city,5);army=w.call(0x490ad0,army_native,receiver=w.root);assert w.call(0x47a630,site)and w.u.mem_read(army+0x2c,1)[0]>=20
 arguments=0xc700000;w.u.mem_map(arguments,0x1000);baseline=bytes(w.u.mem_read(0x7200000,0x300000));cases=[]
 def fields(native):
  person=w.call(0x490b00,native,receiver=w.root);b=bytes(w.u.mem_read(person,0x190));return dict(nativeId=native,status=struct.unpack_from('<i',b,0xa0)[0],home=struct.unpack_from('<i',b,0x98)[0],action=bool(struct.unpack_from('<I',b,0x124)[0]&1),merit=struct.unpack_from('<H',b,0xae)[0],experience=list(struct.unpack_from('<5H',b,0x12a)),loyalty=b[0xac])
 def facts(native):
  before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));result=dict(actor=fields(native),targets=[fields(n)for n in [222,235,348,449,532,590,658]],armyAP=w.u.mem_read(army+0x2c,1)[0],gold=w.call(0x4c69a0,site,17),techniquePoints=w.call(0x4c4260,force,16),nativeRng=struct.unpack('<I',rng)[0]);assert before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));return result
 def save(partial=False):
  report=dict(source=src,exeSha=EXE_SHA,geography=geography,originalSelectedForceSetter='481480',originalOpeningApDriver='4bc910',selectedForce=selected_force,cityNative=city_native,armyNative=army_native,initialArmyAP=cases[0]['before']['armyAP'] if cases else None,cases=cases,limits=['Actual source actor/site/status/identity, explicit original selected force context; originalPCpicker/events unknown','Source0 ordinary topSEARCH5d5970, argumenttuple(site,actor), no rule/getter/probability/RNG/stock/result substituted','UI/presentation calls observed/stopped, no synthesized choice','Raw action/merit/XP/nativehealth/loyalty remain separate source fields; fullJava/menu/APKstillrequired'])
  output.with_suffix('.partial.json').write_text(json.dumps(report,indent=2)+'\n')if partial else output.write_text(json.dumps(report,indent=2)+'\n')
 for native in (actors or [116,163,195,198,251,355,365,377,466]):
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,struct.pack('<I',23));actor=w.call(0x490b00,native,receiver=w.root);w.u.mem_write(arguments,struct.pack('<2I',site,actor));row=dict(actorNative=native,before=facts(native),trace=[]);cases.append(row)
  boundaries={0x520680,0x4f5450,0x4f54a0,0x63b350,0x4d04f0,0x5d3f80,0x5d4160};tracked=boundaries|{0x5d5970,0x5d3ac0,0x5b82e0,0x5d1ea0,0x5d1df0,0x5b8140,0x5d5450,0x5d56a0,0x5d5220,0x5d3c90,0x5d3d40,0x5b9340,0x4a1820,0x4b6580,0x4a5600,0x4721d0}
  def trace(u,ip,size,data):
   if ip not in tracked:return
   sp=u.reg_read(UC_X86_REG_ESP);event=dict(ip=hex(ip),stack=bytes(u.mem_read(sp,32)).hex(),eax=u.reg_read(UC_X86_REG_EAX));row['trace'].append(event)
   if ip in boundaries:row['uiBoundary']=hex(ip);u.emu_stop()
  hook=w.u.hook_add(UC_HOOK_CODE,trace)
  try:row['return']=w.call(0x5d5970,arguments,count=100000000);row['commandComplete']=True
  except Exception as e:
   if'uiBoundary'not in row:row.update(error=repr(e),nativeIp=hex(w.u.reg_read(UC_X86_REG_EIP)),invalidMemory=w.invalid);save(True);raise
  finally:w.u.hook_del(hook)
  row['after']=facts(native);save(True);print(json.dumps({k:v for k,v in row.items()if k not in ['before','after','trace']}),flush=True)
 save()
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--force',type=int,default=2);p.add_argument('--city',type=int,default=8);p.add_argument('--actors',type=int,nargs='+');a=p.parse_args();inspect(a.installation,a.output,a.force,a.city,a.actors)
