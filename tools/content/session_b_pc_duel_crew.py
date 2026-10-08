#!/usr/bin/env python3
"""Original six source-fighter input/model initialization with declared controller fixtures."""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EIP,UC_X86_REG_ESP
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from session_b_pc_geography_context import load_geography
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard

def prepare(installation,index=0):
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
 manifest=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();src=json.loads(manifest)['scenarios'][index];shared=(installation/'Media/scenario/Scenario.s11').read_bytes();raw=(installation/src['sourcePath']).read_bytes();assert sha(raw)==src['sourceSha256']
 d=NativeDebateFlow(installation,exe);w=d.world;load_original_data(w.u);w.load(shared,True);loaded=w.load(raw);w.call(0x73c500);w.call(0x73ca80)
 for f,v in zip([0x4826e0,0x482700,0x482720],src['date']):w.call(f,v,receiver=w.root)
 w.call(0x4827b0,0,receiver=w.root);geo=load_geography(w,installation);w.call(0x493400,receiver=w.root,count=50000000)
 # Actual source force2 officers and source unaffiliated opponents: declared
 # duel teams, not normal opposing deployed units or original event admission.
 natives=[116,163,195,222,658,590];people=[];records={r['native_index']:r for r in loaded['records']if r['kind']=='officer'}
 for n in natives:
  a=w.call(0x490b00,n,receiver=w.root);assert w.call(0x4883c0,receiver=a)==n and w.call(0x47a600,a)==1;b=bytes(w.u.mem_read(a,0x190));people.append(dict(nativeId=n,pointer=a,recordSha=records[n]['sha256'],health=b[0x128],injury=struct.unpack_from('<i',b,0x15c)[0],valid=True))
 return d,w,src,geo,people

def inspect(installation,output,frames=0,ai_gate=0):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier native receipt')
 d,w,src,geo,people=prepare(installation)
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));cases=[];pointer=d.fixture+0x1000
 for control in [(1,1),(0,1),(1,0),(0,0)]:
  for active_index in range(3):
   w.call(0x50ddd0,receiver=pointer);w.u.mem_write(pointer,struct.pack('<6I',*[p['pointer']for p in people]));w.u.mem_write(pointer+0x20,struct.pack('<8i',active_index,active_index,ai_gate,ai_gate,2,0,*control));w.call(0x50ddd0,receiver=0x8b3740);w.u.mem_write(0x8b3740+0xcc,struct.pack('<I',1));assert w.call(0x50de30,pointer,receiver=0x8b3740,count=10000000)==1
   manager=bytes(w.u.mem_read(0x8b3740,0xd0));w.call(0x50ab90,receiver=d.fixture);w.u.mem_write(0x8a5d44,struct.pack('<I',23));
   try:w.call(0x50c030,receiver=d.fixture,count=10000000)
   except Exception as error:
    sp=w.u.reg_read(UC_X86_REG_ESP);failure=dict(error=repr(error),nativeIp=hex(w.u.reg_read(UC_X86_REG_EIP)),invalidMemory=w.invalid,stackHex=bytes(w.u.mem_read(sp,64)).hex(),inputField28GateFixture=ai_gate,controllerFixture=control,activeIndexFixture=active_index,managerHex=manager.hex(),modelHex=bytes(w.u.mem_read(d.fixture,0x59c)).hex(),people=people,source=src,completeGoal=False)
    output.with_suffix('.failure.json').write_text(json.dumps(failure,indent=2)+'\n');raise
   model=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))
   sides=[]
   for side in range(2):
    at=0x24+side*0xec;crew=[]
    for slot in range(3):
     fighter=model[at+slot*0x40:at+(slot+1)*0x40];person=people[side*3+slot];assert struct.unpack_from('<I',fighter)[0]==person['pointer'];crew.append(dict(nativeId=person['nativeId'],fighterHex=fighter.hex()))
    sides.append(dict(side=side,crew=crew,sideHex=model[at:at+0xec].hex(),managerActiveIndex=w.call(0x50d400,side,receiver=0x8b3740),managerField28=w.call(0x50d430,side,receiver=0x8b3740),managerController=w.call(0x50d460,side,receiver=0x8b3740)))
   initial_rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];trace=[];terminal=False
   for frame in range(frames):
    result=w.call(0x505e60,1,receiver=d.fixture,count=10000000);state=bytes(w.u.mem_read(d.fixture,0x59c));header=list(struct.unpack_from('<5i',state,4));trace.append(dict(frame=frame,header=header,returnValue=result,nativeRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],modelHex=state.hex()))
    if header[0]==12 and result==1:terminal=True;break
   assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))
   cases.append(dict(inputField28GateFixture=ai_gate,controllerFixture=control,activeIndexFixture=active_index,sides=sides,managerHex=manager.hex(),modelHex=model.hex(),nativeRng=initial_rng,trace=trace,terminal=terminal,finalManagerHex=bytes(w.u.mem_read(0x8b3740,0xd0)).hex(),wholeSourceWorldUnchanged=True));print('PASS original crew initialized',control,active_index,'frames',len(trace),'terminal',terminal,flush=True);output.with_suffix('.partial.json').write_text(json.dumps(dict(source=src,people=people,cases=cases,geography=geo,exeSha=EXE_SHA,completeGoal=False),indent=2)+'\n')
 result=dict(source=src,exeSha=EXE_SHA,geography=geo,people=people,cases=cases,limits=['Teams/active-index/controllers explicit input fixtures; opponents are not normal deployed enemy units','Full original manager admission/model initializer retained; no damage/AI/RNG/health/source rule replacement','Controller flags and support bytes not yet labelled normal human operations','Normal event admission/human input/support arrival/swap/full terminal/campaign/Save/API/APK remain required'],completeGoal=False)
 output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(result,indent=2)+'\n');print('PASS original6actor',len(cases),'cases SHA',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--frames',type=int,choices=range(2001),default=0);p.add_argument('--input-field28',type=int,choices=[0,1],default=0);a=p.parse_args();inspect(a.installation,a.output,a.frames,a.input_field28)
