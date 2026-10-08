#!/usr/bin/env python3
"""Source0 original search-discovery follow-up with explicit AP fixtures.
No claim that loaded AP0 or selected fixture AP is the actual opening budget.
Original actor/controller flags and rule/RNG functions are retained.
"""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EIP,UC_X86_REG_ESP,UC_X86_REG_EAX
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
 site=w.call(0x490d00,8,receiver=w.root);target=w.call(0x490b00,222,receiver=w.root);army=w.call(0x490ad0,0,receiver=w.root);baseline=bytes(w.u.mem_read(0x7200000,0x300000));cases=[]
 def fields(person):
  b=bytes(w.u.mem_read(person,0x190));return dict(nativeId=w.call(0x4883c0,receiver=person),status=struct.unpack_from('<i',b,0xa0)[0],home=struct.unpack_from('<i',b,0x98)[0],action=bool(struct.unpack_from("<I",b,0x124)[0]&1),raw148=b[0x148],merit=struct.unpack_from('<H',b,0xae)[0],experience=list(struct.unpack_from('<5H',b,0x12a)),loyalty=b[0xac])
 def facts(actor):
  before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));stocks=[dict(nativeId=n,gold=w.call(0x4c69a0,w.call(0x490d00,n,receiver=w.root),17),food=w.call(0x4c69a0,w.call(0x490d00,n,receiver=w.root),18))for n in range(87)]
  result=dict(actor=fields(actor),target=fields(target),armyAP=w.u.mem_read(army+0x2c,1)[0],stocks=stocks,techniquePoints=w.call(0x4c4260,w.call(0x490aa0,2,receiver=w.root),16),nativeRng=struct.unpack('<I',rng)[0])
  assert before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));return result
 def write(partial=False):
  dest=output.with_suffix('.partial.json')if partial else output;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(json.dumps(dict(source=src,exeSha=EXE_SHA,geographyContext=geography,cases=cases,completeGoal=False,limits=['Source actors/source ownership and controller flags, no fabricated current player','AP set with original47e3e0 only as declared isolated input fixture; opening budget unknown','Full original search5d3ac0 admission then5d56a0 discovery/recruitment follow-up; stops before UI if reached','No original probability/roll/RNG/stock/AP/merit/TP/action rule substituted','Normal player GUI/menu, optional prompt/diplomacy/concession separate']),indent=2)+'\n')
 for native,ap in [(116,0),(116,19),(116,20),(116,40),(466,20),(163,20)]:
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,struct.pack('<I',23));actor=w.call(0x490b00,native,receiver=w.root);w.call(0x47e3e0,ap,receiver=army);row=dict(actorNative=native,inputAP=ap,before=facts(actor),trace=[]);cases.append(row)
  boundary={0x4f5450,0x4f54a0,0x520680};tracked=boundary|{0x5d3ac0,0x5d56a0,0x5d5220,0x5d3c90,0x5b9340,0x4a1820,0x4b6580,0x4a5600,0x5d1f30}
  def trace(u,ip,size,data):
   if ip not in tracked:return
   sp=u.reg_read(UC_X86_REG_ESP);row['trace'].append(dict(ip=hex(ip),stack=bytes(u.mem_read(sp,32)).hex()))
   if ip in boundary:row['uiBoundary']=hex(ip);u.emu_stop()
  token=w.u.hook_add(UC_HOOK_CODE,trace)
  try:
   row['admitted']=bool(w.call(0x5d3ac0,site,count=50000000));row['afterAdmission']=facts(actor)
   if row['admitted']:w.call(0x5d56a0,target,actor,site,count=50000000);row['commandComplete']=True
  except Exception as error:
   if 'uiBoundary'not in row:row.update(error=repr(error),nativeIp=hex(w.u.reg_read(UC_X86_REG_EIP)),invalidMemory=w.invalid);write(True);raise
  finally:w.u.hook_del(token)
  row['after']=facts(actor);write(True);print(json.dumps({k:v for k,v in row.items()if k not in ['before','after','afterAdmission','trace']}),flush=True)
 write()
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
