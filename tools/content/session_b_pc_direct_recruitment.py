#!/usr/bin/env python3
"""Original direct5c5940: selected-force driver or explicit AP boundary fixtures.
Stops at UI, or explicitly omits examined presentation-only calls for numeric tail.
Neither mode claims complete PC GUI, events, audio or UI RNG acceptance.
"""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EIP,UC_X86_REG_ESP,UC_X86_REG_EAX
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from session_b_pc_geography_context import load_geography
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard

def inspect(installation,output,selected_force=None,presentation_fixture=False):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier evidence')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
 manifest=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();src=json.loads(manifest)['scenarios'][0];shared=(installation/'Media/scenario/Scenario.s11').read_bytes();raw=(installation/src['sourcePath']).read_bytes();assert sha(raw)==src['sourceSha256']
 w=NativeDebateFlow(installation,exe).world;load_original_data(w.u);w.load(shared,True);w.load(raw);w.call(0x73c500);w.call(0x73ca80);w.call(0x49b490,receiver=0x767cab8)
 for f,v in zip([0x4826e0,0x482700,0x482720],src['date']):w.call(f,v,receiver=w.root)
 w.call(0x4827b0,0,receiver=w.root);geography=load_geography(w,installation);w.call(0x493400,receiver=w.root,count=50000000)
 if selected_force is not None:
  force=w.call(0x490aa0,selected_force,receiver=w.root);w.call(0x481480,0,receiver=force);w.call(0x4bc910,receiver=0x799895c,count=100000000)
 w.u.mem_map(0xc500000,0x1000);arguments=0xc500000;site=w.call(0x490d00,8,receiver=w.root);target=w.call(0x490b00,222,receiver=w.root);army=w.call(0x490ad0,0,receiver=w.root);baseline=bytes(w.u.mem_read(0x7200000,0x300000));cases=[]
 def fields(person):
  b=bytes(w.u.mem_read(person,0x190));return dict(nativeId=w.call(0x4883c0,receiver=person),status=struct.unpack_from('<i',b,0xa0)[0],home=struct.unpack_from('<i',b,0x98)[0],action=bool(struct.unpack_from("<I",b,0x124)[0]&1),raw148=b[0x148],merit=struct.unpack_from('<H',b,0xae)[0],experience=list(struct.unpack_from('<5H',b,0x12a)),loyalty=b[0xac])
 def facts(actor):
  before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));stocks=[dict(nativeId=n,gold=w.call(0x4c69a0,w.call(0x490d00,n,receiver=w.root),17),food=w.call(0x4c69a0,w.call(0x490d00,n,receiver=w.root),18))for n in range(87)]
  result=dict(actor=fields(actor),target=fields(target),armyAP=w.u.mem_read(army+0x2c,1)[0],stocks=stocks,techniquePoints=w.call(0x4c4260,w.call(0x490aa0,2,receiver=w.root),16),nativeRng=struct.unpack('<I',rng)[0])
  assert before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));return result
 def write(partial=False):
  dest=output.with_suffix('.partial.json')if partial else output;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(json.dumps(dict(source=src,exeSha=EXE_SHA,geographyContext=geography,selectedForce=selected_force,apContext='original481480/full4bc910'if selected_force is not None else 'declared original-setter fixtures',presentationFixture=bool(presentation_fixture),cases=cases,completeGoal=False,limits=['Source actors/source ownership and controller flags; explicit original selectedforce setter when requested, not complete PCpicker','AP is full original driver result in selected context; fixture setter otherwise, never loadedAP0 as opening truth',('Original numeric fulltail with declared examined4f5450/63add0 presentation omissions; not PCGUI/UI RNG'if presentation_fixture else 'Full original direct5c56d0 admission then5c5940 dispatcher (site,target,actor); stops before UI if reached'),'No original probability/roll/RNG/stock/AP/merit/TP/action rule substituted','Normal player GUI/menu, optional prompt/diplomacy/concession separate']),indent=2)+'\n')
 initial_ap=w.u.mem_read(army+0x2c,1)[0]
 for native,ap in ([(n,initial_ap)for n in [116,163,195,198,251,355,365,377,466]]if selected_force is not None else [(116,0),(116,19),(116,20),(116,40),(466,20),(163,20)]):
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,struct.pack('<I',23));actor=w.call(0x490b00,native,receiver=w.root)
  if selected_force is None:w.call(0x47e3e0,ap,receiver=army)
  row=dict(actorNative=native,inputAP=ap,before=facts(actor),trace=[]);cases.append(row)
  boundary={0x4f5450,0x4f54a0,0x520680};tracked=boundary|{0x63add0,0x5c56d0,0x5c4650,0x5c5940,0x4afd60,0x5c4840,0x5c5220,0x5b9340,0x4a1820,0x4b6580,0x4a5600,0x5d1f30}
  def trace(u,ip,size,data):
   if ip not in tracked:return
   sp=u.reg_read(UC_X86_REG_ESP);row['trace'].append(dict(ip=hex(ip),stack=bytes(u.mem_read(sp,32)).hex()))
   if presentation_fixture and ip in [0x4f5450,0x63add0]:
    caller=struct.unpack('<I',u.mem_read(sp,4))[0]
    allowed={0x4f5450:{0x5c49a9,0x5c49e2,0x5c4c5c},0x63add0:{0x5c4a45,0x5c4abc,0x5c4ca8,0x5c4d04}}
    if caller not in allowed[ip]:raise ValueError('Unexamined presentation caller '+hex(caller))
    row.setdefault('omittedPresentationFixture',[]).append(dict(function=hex(ip),originalCaller=hex(caller),scope='presentation/notification only; ignored result; GUI/audio/event scheduling and UI RNG unproven'))
    u.reg_write(UC_X86_REG_ESP,sp+4+(16 if ip==0x63add0 else 0));u.reg_write(UC_X86_REG_EIP,caller);return
   if ip in boundary:row['uiBoundary']=hex(ip);u.emu_stop()
  token=w.u.hook_add(UC_HOOK_CODE,trace)
  try:
   row['admitted']=bool(w.call(0x5c56d0,site,count=50000000));row['afterAdmission']=facts(actor)
   if row['admitted']:
    w.u.mem_write(arguments,struct.pack('<3I',site,target,actor));row['commandReturn']=w.call(0x5c5940,arguments,count=50000000);row['commandComplete']=True
  except Exception as error:
   if 'uiBoundary'not in row:row.update(error=repr(error),nativeIp=hex(w.u.reg_read(UC_X86_REG_EIP)),invalidMemory=w.invalid);write(True);raise
  finally:w.u.hook_del(token)
  row['after']=facts(actor);write(True);print(json.dumps({k:v for k,v in row.items()if k not in ['before','after','afterAdmission','trace']}),flush=True)
 write()
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--selected-force',type=int);p.add_argument('--presentation-fixture',action='store_true');a=p.parse_args();inspect(a.installation,a.output,a.selected_force,a.presentation_fixture)
