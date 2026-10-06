#!/usr/bin/env python3
"""Full original source-backed debate manager/model initialization and side receipt.
Control flags are explicit input contexts; full PC player-menu/startup remains unknown.
"""
import argparse,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard
from unicorn.x86_const import UC_X86_REG_EIP

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier evidence')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
 manifest=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();source=json.loads(manifest)['scenarios'][0]
 shared=(installation/'Media/scenario/Scenario.s11').read_bytes();raw=(installation/source['sourcePath']).read_bytes();assert sha(raw)==source['sourceSha256']
 d=NativeDebateFlow(installation,exe);w=d.world;load_original_data(w.u);w.load(shared,True);w.load(raw);w.call(0x73c500);w.call(0x73c2b0);w.call(0x49b490,receiver=0x767cab8)
 for f,v in zip([0x4826e0,0x482700,0x482720],source['date']):w.call(f,v,receiver=w.root)
 w.call(0x4827b0,0,receiver=w.root);w.call(0x493400,receiver=w.root,count=50000000)
 actor=w.call(0x490b00,116,receiver=w.root);target=w.call(0x490b00,222,receiver=w.root)
 assert w.call(0x4883c0,receiver=actor)==116 and w.call(0x4883c0,receiver=target)==222
 cases=[];baseline=bytes(w.u.mem_read(0x7200000,0x300000));model=d.fixture
 def save(partial=False):
  report=dict(source=source,exeSha=EXE_SHA,sharedSha=sha(shared),manifestSha=sha(manifest),cases=cases,completeGoal=False,limits=['Original full manager/model initialization; no original rule getter or RNG substituted','Control flags explicit contexts; actual PC player/menu/controller and startup budget remain separate','Only source-backed116/222; book/relationship mutation and ordinary campaign callbacks still separate'])
  dest=output.with_suffix('.partial.json')if partial else output;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(json.dumps(report,indent=2)+'\n')
 for ah,th in [(0,0),(1,0),(0,1),(1,1)]:
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8b5214+0x28,struct.pack('<I',1));w.u.mem_write(0x8a5d44,struct.pack('<I',23))
  row=dict(actorHuman=ah,targetHuman=th,inputActorNative=116,inputTargetNative=222)
  cases.append(row)
  try:
   assert w.call(0x51dff0,actor,1000,ah,target,1000,th,0,receiver=0x8b5214)==1
   row['managerHex']=bytes(w.u.mem_read(0x8b5214,0x30)).hex();row['initialLeader']=w.call(0x65b740,receiver=0x8b5214)
   row['canonicalSides']=[w.call(0x4883c0,receiver=w.call(0x51dcb0,side,receiver=0x8b5214))for side in range(2)]
   row['canonicalHuman']=[w.call(0x51dcf0,side,receiver=0x8b5214)for side in range(2)]
   w.u.mem_write(model,bytes(0x1000));w.call(0x51fcf0,receiver=model);assert w.call(0x51fd10,receiver=model,count=50000000)==1
   state=bytes(w.u.mem_read(model,0x1b0));row['initialModelHex']=state.hex();row['modelLeader']=struct.unpack_from('<i',state,0x16c)[0];row['nativeRngAfterInitialization']=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
   row['speakerNative']=[w.call(0x4883c0,receiver=struct.unpack_from('<I',state,0x10+side*0xa0)[0])for side in range(2)]
   row['currentIntelligence']=[w.call(0x489090,receiver=p)for p in [actor,target]]
   row['effectiveTalkCards']=[list(struct.unpack_from('<5i',state,0x10+side*0xa0+0x34+0x4c))for side in range(2)]
   row['sourceWorldAfterInitSha']=sha(bytes(w.u.mem_read(0x7200000,0x300000)));row['complete']=True
  except Exception as error:
   row.update(complete=False,error=repr(error),nativeIp=hex(w.u.reg_read(UC_X86_REG_EIP)),invalidMemory=w.invalid);save(True);raise
  save(True);print(json.dumps({k:row[k]for k in ['actorHuman','targetHuman','initialLeader','canonicalSides','canonicalHuman','modelLeader','nativeRngAfterInitialization']}),flush=True)
 save()
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
