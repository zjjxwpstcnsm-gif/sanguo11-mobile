#!/usr/bin/env python3
"""Original current sworn groups after a declared valid source0 coronation.
No raw loyalty, relationship, ability, owner, RNG or battle outcome substitution.
"""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EIP,UC_X86_REG_ESP,UC_X86_REG_ESI
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve receipt')
 print('Loading original source',flush=True);d,w,source,geo,_=prepare(installation);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));rows=[];calls=[];active=False
 w.call(0x415400,receiver=0x32602b0,count=50000000);w.call(0x477810,receiver=0x6ee7888,count=50000000);w.call(0x4f52d0,receiver=0x91ba558,count=50000000)
 assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 def records():return [dict(nativeId=n,hex=bytes(w.u.mem_read(w.root+0xc0bc+n*0x190,0x190)).hex())for n in range(1100)]
 def trace(u,ip,size,user):
  if not active:return
  if ip==0x4a75a0:
   sp=u.reg_read(UC_X86_REG_ESP);ret,person,ruler,weighted=struct.unpack('<4I',u.mem_read(sp,16));assert person>=w.root+0xc0bc and (person-w.root-0xc0bc)%0x190==0
   calls.append(dict(address=hex(ip),personNative=(person-w.root-0xc0bc)//0x190,weighted=weighted,returnAddress=hex(ret),oldRaw=bytes(u.mem_read(person+0xac,1))[0]))
  else:
   person=u.reg_read(UC_X86_REG_ESI);native=(person-w.root-0xc0bc)//0x190;entry=next(c for c in reversed(calls)if c['personNative']==native);entry['computedRaw']=bytes(u.mem_read(person+0xac,1))[0]
 for ip in [0x4a75a0,0x4ab966]:w.u.hook_add(UC_HOOK_CODE,trace,begin=ip,end=ip)
 for heirNative in [635,98,432]:
  for target in [n for n in [98,432,635]if n!=heirNative]:
   w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);force=w.call(0x490aa0,4,receiver=w.root);departed=w.call(0x490b00,614,receiver=w.root);heir=w.call(0x490b00,heirNative,receiver=w.root)
   assert w.call(0x488c00,receiver=departed)==1 and w.call(0x47a630,heir)==1 and w.call(0x489220,receiver=heir)==0xffffffff and struct.unpack('<i',w.u.mem_read(heir+0x94,4))[0]==2
   print('Original coronation614 ->',heirNative,'then death',target,flush=True)
   try:
    w.call(0x4b78f0,force,heir,departed,receiver=0x799895c,count=50000000);w.call(0x4acae0,departed,receiver=0x799895c,count=50000000);assert w.call(0x488c00,receiver=heir)==1
    before=records();worldBefore=bytes(w.u.mem_read(0x7200000,0x300000));calls=[];active=True;person=w.call(0x490b00,target,receiver=w.root);w.call(0x4acae0,person,receiver=0x799895c,count=50000000);active=False
   except Exception as e:output.with_suffix('.failure.json').write_text(json.dumps(dict(rows=rows,calls=calls,heirNative=heirNative,target=target,error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid),indent=2)+'\n');raise
   assert rng==bytes(w.u.mem_read(0x8a5d44,4));after=records();rows.append(dict(heirNative=heirNative,target=target,before=before,after=after,calls=calls,changedBytes=sum(a!=b for a,b in zip(worldBefore,bytes(w.u.mem_read(0x7200000,0x300000)))),rngPure=True));print('Full original group loyalty calls',calls,flush=True)
 w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,rows=rows,wholeWorldAndRngRestored=True,limits=['Full original4b78f0 valid source0 force4 heir635/98/432 as explicit choice; departing614 original death then nonruler group death','Full4acae0/4ab9a0/4ab770 no callback omitted and no raw numeric substitution','Original declared valid coronation input is not AI selection/human GUI/battle/Android acceptance']),indent=2)+'\n');print('PASS original ruler sworn death',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
