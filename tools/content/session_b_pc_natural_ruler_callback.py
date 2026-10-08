#!/usr/bin/env python3
"""Full original natural ruler callback, AI or declared valid human heir.
Original death model/manager retained; no stat/roster/death outcome replacement.
"""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EIP,UC_X86_REG_ESP
from session_b_pc_duel_crew import prepare
from session_b_pc_readonly_render_abi import bind_readonly_render_abi
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve receipt')
 raw=Path('out/session-b/natural-ruler-battle-source0-v1.json').read_bytes();assert sha(raw)=='12e70314e57c8e4c2fe6a7afada1a8e4e62e6bd971c58237f0b9bd12334ddddb';receipt=json.loads(raw);endpoint=receipt['selected'];assert endpoint['seed']==1 and endpoint['deadNative']==517 and endpoint['outcomes']==[0,0,0,2,0,0]
 d,w,source,geo,_=prepare(installation);assert source==receipt['source'];baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));cases=[];stage='constructors'
 try:
  for f,p in [(0x415400,0x32602b0),(0x477810,0x6ee7888),(0x4f52d0,0x91ba558)]:w.call(f,receiver=p,count=50000000)
  environment=bind_readonly_render_abi(w);units=[w.root+0x169730,w.root+0x169730+0xf4]
  for native,ptr,data in zip([365,517],units,endpoint['unitsBefore']):
   w.u.mem_write(ptr,bytes.fromhex(data));w.u.reg_write(UC_X86_REG_EAX,ptr);location=w.call(0x4a7530);w.call(0x4a0cb0,w.call(0x490b00,native,receiver=w.root),location,receiver=0x799895c,count=50000000)
  assert all(bytes(w.u.mem_read(w.root+0xc0bc+p['nativeId']*0x190,0x190)).hex()==p['hex']for p in endpoint['peopleBefore']);configured=bytes(w.u.mem_read(0x7200000,0x300000));Path('out/session-b/natural-ruler-callback-before-v1.bin').write_bytes(configured)
  activeChoice=None;choices=[];calls=[]
  def inputChoice(u,ip,size,user):
   if activeChoice is None:raise ValueError('Unexpected human input in actual AI force')
   sp=u.reg_read(UC_X86_REG_ESP);ret,descriptor=struct.unpack('<2I',u.mem_read(sp,8));force,vector=struct.unpack('<2I',u.mem_read(descriptor+4,8));assert force==expectedForce
   cursor=struct.unpack('<I',u.mem_read(vector+4,4))[0];natives=[]
   while cursor:
    ptr=struct.unpack('<I',u.mem_read(cursor+8,4))[0];natives.append((ptr-w.root-0xc0bc)//0x190);cursor=struct.unpack('<I',u.mem_read(cursor,4))[0]
   assert activeChoice in natives and 517 not in natives;choices.append(dict(candidates=natives,declaredChoice=activeChoice));u.reg_write(UC_X86_REG_EAX,activeChoice);u.reg_write(UC_X86_REG_ESP,sp+8);u.reg_write(UC_X86_REG_EIP,ret)
  # This is the known human UI boundary; all candidate generation, sorting,
  # resolution and subsequent coronation/death procedures remain original.
  w.u.hook_add(UC_HOOK_CODE,inputChoice,begin=0x588d70,end=0x588d70)
  def observe(u,ip,size,user):
   sp=u.reg_read(UC_X86_REG_ESP);calls.append(dict(address=hex(ip),stackWords=list(struct.unpack('<7I',u.mem_read(sp,28)))))
  for f in [0x4acbe0,0x4b9080,0x4b78f0,0x4acae0,0x4aa680]:w.u.hook_add(UC_HOOK_CODE,observe,begin=f,end=f)
  display=next(p for p in json.loads(Path('out/session-b/duel-campaign-presentation-source-v2.json').read_text())['functions']if p['address']=='0x588bb0');assert sha(bytes(w.u.mem_read(0x588bb0,0xb0)))==display['sha256']
  def valueDisplay(u,ip,size,user):
   sp=u.reg_read(UC_X86_REG_ESP);ret,*args=struct.unpack('<4I',u.mem_read(sp,16));u.reg_write(UC_X86_REG_ESP,sp+16);u.reg_write(UC_X86_REG_EIP,ret)
  w.u.hook_add(UC_HOOK_CODE,valueDisplay,begin=0x588bb0,end=0x588bb0)
  force=w.call(0x490aa0,3,receiver=w.root);expectedForce=force
  for choice,name in [(None,'ai'),(14,'human')]:
   stage='full4d3340 '+name;w.u.mem_write(0x7200000,configured);w.u.mem_write(d.fixture,bytes.fromhex(endpoint['modelHex']));w.u.mem_write(0x8b3740,bytes.fromhex(endpoint['managerHex']));w.u.mem_write(0x8a5d44,struct.pack('<I',endpoint['terminalRng']));activeChoice=choice;choices.clear();calls.clear()
   if choice is not None:w.call(0x481480,0,receiver=force);assert w.call(0x47a690,receiver=w.call(0x490b00,517,receiver=w.root))==1
   w.call(0x4d3340,count=50000000);world=bytes(w.u.mem_read(0x7200000,0x300000));file=Path('out/session-b/natural-ruler-callback-'+name+'-v1.bin');file.write_bytes(world);people=[dict(nativeId=n,hex=bytes(w.u.mem_read(w.root+0xc0bc+n*0x190,0x190)).hex())for n in [365,517,109,14,558]];cases.append(dict(declaredHumanChoice=choice,choiceRows=list(choices),calls=list(calls),worldPath=str(file),worldSha=sha(world),people=people,unitAfter=[bytes(w.u.mem_read(p,0xf4)).hex()for p in units],rngAfter=int.from_bytes(w.u.mem_read(0x8a5d44,4),'little')));print('PASS original natural ruler',name,flush=True)
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
  output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,originalNaturalReceiptSha=sha(raw),cases=cases,readonlyEnvironmentCalls=environment,wholeWorldAndRngRestored=True,limits=['Actual198-frame naturally generated king517 outcome2/model/manager retained','Original AI force3 case then declared force3 human flag after battle plus validUI588d70 choice14; not original human combat/menu proof','Full original4d3340/4acbe0/roster/selector/coronation/death; knownvoid588bb0 display only omitted','Original Windows registry/CPU unknown readonly environment; full VM snapshots preserve all rule bytes']),indent=2)+'\n');print('PASS natural ruler callbacks',sha(output.read_bytes()),flush=True)
 except Exception as e:output.with_suffix('.failure.json').write_text(json.dumps(dict(stage=stage,cases=cases,error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid),indent=2)+'\n');raise
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
