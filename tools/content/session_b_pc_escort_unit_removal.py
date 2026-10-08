#!/usr/bin/env python3
"""Full original current unit destruction while holding another captive.
Declared original unit constructors/valid crown choice, no source stat edits.
"""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EIP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output,prisoner_native=355):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve receipt')
 print('Loading original source',flush=True);d,w,source,geo,_=prepare(installation);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));stage='constructors'
 def records():return [dict(nativeId=n,hex=bytes(w.u.mem_read(w.root+0xc0bc+n*0x190,0x190)).hex())for n in range(1100)]
 try:
  w.call(0x415400,receiver=0x32602b0,count=50000000);w.call(0x477810,receiver=0x6ee7888,count=50000000);w.call(0x4f52d0,receiver=0x91ba558,count=50000000);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
  force=w.call(0x490aa0,4,receiver=w.root);departed=w.call(0x490b00,614,receiver=w.root);ruler=w.call(0x490b00,635,receiver=w.root);victor=w.call(0x490b00,377,receiver=w.root);prisoner=w.call(0x490b00,prisoner_native,receiver=w.root);stage='crown';w.call(0x4b78f0,force,ruler,departed,receiver=0x799895c,count=50000000);w.call(0x4acae0,departed,receiver=0x799895c,count=50000000)
  units=[]
  for index,native in enumerate([377,635,prisoner_native]):
   ptr=w.root+0x169730+index*0xf4;w.u.mem_write(ptr+8,struct.pack('<i',0));w.u.mem_write(ptr+0xc,struct.pack('<3i',native,-1,-1));w.u.mem_write(ptr+0x18,struct.pack('<H',5000));w.u.mem_write(ptr+0x1a,bytes([100]));w.u.mem_write(ptr+0x3c,struct.pack('<hh',80+index,80));w.call(0x496250,997,receiver=ptr);w.call(0x496280,17000,receiver=ptr);assert w.call(0x47a630,ptr)==1;w.u.reg_write(UC_X86_REG_EAX,ptr);loc=w.call(0x4a7530);person=w.call(0x490b00,native,receiver=w.root);w.call(0x4a0cb0,person,loc,receiver=0x799895c,count=50000000);units.append(ptr)
  stage='capture355';w.call(0x4a93b0,prisoner,ruler,0,receiver=0x799895c,count=50000000);stage='remove355oldunit';w.call(0x4a5d90,prisoner,ruler,units[1],units[2],receiver=0x799895c,count=50000000)
  before=records();unitBefore=[bytes(w.u.mem_read(p,0xf4)).hex()for p in units];beforeWorld=bytes(w.u.mem_read(0x7200000,0x300000));print('Original captive established; removing escort unit',flush=True)
  stage='remove635escort';w.call(0x4a5d90,ruler,victor,units[0],units[1],receiver=0x799895c,count=50000000);after=records();unitAfter=[bytes(w.u.mem_read(p,0xf4)).hex()for p in units];afterWorld=bytes(w.u.mem_read(0x7200000,0x300000));rngAfter=bytes(w.u.mem_read(0x8a5d44,4));w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,prisonerNative=prisoner_native,before=before,after=after,unitBefore=unitBefore,unitAfter=unitAfter,changedBytes=sum(a!=b for a,b in zip(beforeWorld,afterWorld)),rngBefore=int.from_bytes(rng,'little'),rngAfter=int.from_bytes(rngAfter,'little'),wholeWorldAndRngRestored=True,limits=['Full original capture355/4a5d90 removal then current ruler635 unit4a5d90 deletion, actual original constructors','Declared valid crown614->635 and original array0/1/2 formations/coordinates/resources, not ordinary deployment/battle/GUI','Ruler is not executed in this receipt; escort relocation is isolated prerequisite of full death settlement','No numeric/presentation function omissions or source stat/result replacements']),indent=2)+'\n');print('PASS original escorted unit removal',sha(output.read_bytes()),flush=True)
 except Exception as e:output.with_suffix('.failure.json').write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,stage=stage,error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid),indent=2)+'\n');raise
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--prisoner-native',type=int,choices=[355,558],default=355);a=p.parse_args();inspect(a.installation,a.output,a.prisoner_native)
