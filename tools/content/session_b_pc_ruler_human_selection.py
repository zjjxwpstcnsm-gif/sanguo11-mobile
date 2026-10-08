#!/usr/bin/env python3
"""Original full human successor selector, declared valid UI choice boundary."""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EIP,UC_X86_REG_EAX,UC_X86_REG_EDI
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve receipt')
 print('Loading original source',flush=True);d,w,source,geo,_=prepare(installation);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));force=w.call(0x490aa0,28,receiver=w.root);ruler=w.call(0x490b00,403,receiver=w.root);w.call(0x481480,0,receiver=force);assert w.call(0x47a690,receiver=ruler)==1;before=bytes(w.u.mem_read(0x7200000,0x300000));rows=[];selected=440
 def input_choice(u,ip,size,user):
  sp=u.reg_read(UC_X86_REG_ESP);ret,descriptor=struct.unpack('<2I',u.mem_read(sp,8));f,vector=struct.unpack('<2I',u.mem_read(descriptor+4,8));assert f==force;cursor=struct.unpack('<I',u.mem_read(vector+4,4))[0];people=[]
  while cursor:
   person=struct.unpack('<I',u.mem_read(cursor+8,4))[0];native=(person-w.root-0xc0bc)//0x190;assert person==w.root+0xc0bc+native*0x190;people.append(native);cursor=struct.unpack('<I',u.mem_read(cursor,4))[0]
  assert selected in people and 403 not in people;rows.append(dict(originalUiBoundary=hex(ip),declaredChoice=selected,candidates=people,descriptorHex=bytes(u.mem_read(descriptor,12)).hex()));u.reg_write(UC_X86_REG_EAX,selected);u.reg_write(UC_X86_REG_ESP,sp+8);u.reg_write(UC_X86_REG_EIP,ret)
 w.u.hook_add(UC_HOOK_CODE,input_choice,begin=0x588d70,end=0x588d70);w.u.reg_write(UC_X86_REG_EDI,force)
 try:result=w.call(0x4b7e70,ruler,count=50000000)
 except Exception as e:output.with_suffix('.failure.json').write_text(json.dumps(dict(error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid,rows=rows),indent=2)+'\n');raise
 after=bytes(w.u.mem_read(0x7200000,0x300000));rngAfter=bytes(w.u.mem_read(0x8a5d44,4))
 if not(len(rows)==1 and result==w.root+0xc0bc+selected*0x190 and before==after and rng==rngAfter):
  output.with_suffix('.failure.json').write_text(json.dumps(dict(rows=rows,resultPointer=hex(result),expectedPointer=hex(w.root+0xc0bc+selected*0x190),changedBytes=[dict(offset=i,before=a,after=b)for i,(a,b)in enumerate(zip(before,after))if a!=b],rngBefore=int.from_bytes(rng,'little'),rngAfter=int.from_bytes(rngAfter,'little')),indent=2)+'\n');raise AssertionError('Original human selector state differs; preserve diagnostic')
 w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,forceNative=28,departedNative=403,declaredHumanControl=True,rows=rows,selectedNative=selected,queryWholeWorldAndRngPure=True,sourceWorldAndRngRestored=True,limits=['Full original4b7e70 roster/removal/sort/resolution, UI588d70 returns declared valid choice','Human force control flag declared fixture, no candidate/ability/ownership/result writes','Actual original GUI/coronation/death and Android ordinary menu remain separate']),indent=2)+'\n');print('PASS original human successor input',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
