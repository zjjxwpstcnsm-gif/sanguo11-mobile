#!/usr/bin/env python3
"""Original5a07d0 ordinary command completion at unchanged unit position.
Full function retained; no action/RNG/geometry function replacement.
"""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EIP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve receipt')
 raw=Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes();assert sha(raw)=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38';context=json.loads(raw);d,w,source,geo,_=prepare(installation);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));units=[]
 for unit,case in zip(context['units'],context['cases']):
  p=unit['pointer'];w.u.mem_write(p,bytes.fromhex(case['afterHex']));w.u.mem_write(p+0x3c,struct.pack('<hh',80+unit['index'],80));units.append(p)
 declared=bytes(w.u.mem_read(0x7200000,0x300000));rows=[]
 for ownFlag in [0,1,0x52,0xffffffff]:
  for otherFlag in [0,1,0x24]:
   w.u.mem_write(0x7200000,declared);w.u.mem_write(units[0]+0x40,struct.pack('<I',ownFlag));w.u.mem_write(units[1]+0x40,struct.pack('<I',otherFlag));before=bytes(w.u.mem_read(0x7200000,0x300000));point=struct.unpack('<I',w.u.mem_read(units[0]+0x3c,4))[0]
   try:result=w.call(0x5a07d0,context['units'][0]['index'],point,count=10000000)
   except Exception as error:output.with_suffix('.failure.json').write_text(json.dumps(dict(error=repr(error),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid,ownFlag=ownFlag,otherFlag=otherFlag),indent=2)+'\n');raise
   after=bytes(w.u.mem_read(0x7200000,0x300000));newOwn=struct.unpack('<I',w.u.mem_read(units[0]+0x40,4))[0];newOther=struct.unpack('<I',w.u.mem_read(units[1]+0x40,4))[0];assert newOwn==ownFlag|1 and newOther==otherFlag and rng==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(ownFlag=ownFlag,otherFlag=otherFlag,ownAfter=newOwn,otherAfter=newOther,result=result,changedWorldAddresses=[hex(0x7200000+i)for i,(a,b)in enumerate(zip(before,after))if a!=b]))
 w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,rows=rows,fullSourceWorldAndRngRestored=True,limits=['Full original5a07d0 with declared valid units and unchanged position endpoint','Normal pointer callback579c40 still needs actual challenge callback binding; no PC GUI or APK proof','Optional global8a5a70 command reset remains separately identified, not changed by probe'],completeGoal=False),indent=2)+'\n');print('PASS original ordinary action',len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
