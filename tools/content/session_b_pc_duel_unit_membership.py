#!/usr/bin/env python3
"""Original crew membership/location getters; declared units not real deployment."""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import sha,EXE_SHA,output_guard

def inspect(installation,output,immediate=False):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve receipt')
 raw=Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes();assert sha(raw)=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38';r=json.loads(raw);d,w,src,geo,unused=prepare(installation);assert src==r['source'];baseline=bytes(w.u.mem_read(w.root,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));rows=[]
 for unit,c in zip(r['units'],r['cases']):
  pointer=unit['pointer'];w.u.mem_write(pointer,bytes.fromhex(c['afterHex']));w.u.mem_write(pointer+0x3c,struct.pack('<hh',80+unit['index'],80));members=[]
  for p in c['declaredCrew']:
   person=p['pointer'];before=bytes(w.u.mem_read(person,0x190));getterBefore=w.call(0x489220,receiver=person);
   # Examined4a7990(person,location,-1,0) is the original relocation caller;
   # no raw current9c write or native membership getter result replacement.
   w.u.reg_write(UC_X86_REG_EAX,pointer);location=w.call(0x4a7530,count=10000000)
   result=w.call(0x4a0cb0,person,location,receiver=0x799895c,count=10000000)if immediate else w.call(0x4a7990,person,pointer,0xffffffff,0,receiver=0x799895c,count=10000000);after=bytes(w.u.mem_read(person,0x190));getterAfter=w.call(0x489220,receiver=person);members.append(dict(native=p['nativeId'],beforeLocation=struct.unpack_from('<i',before,0x9c)[0],beforeUnit=getterBefore,afterLocation=struct.unpack_from('<i',after,0x9c)[0],afterUnit=getterAfter,locationGetter=location,immediate=immediate,relocationResult=result,changed=[dict(offset=i,before=a,after=b)for i,(a,b)in enumerate(zip(before,after))if a!=b]))
  
  if immediate:assert all(p['afterUnit']==unit['index']and p['afterLocation']==p['locationGetter']for p in members)
  rows.append(dict(arrayIndex=unit['index'],pointer=pointer,members=members));print('PASS original membership getter unit',unit['index'],flush=True)
 after=bytes(w.u.mem_read(w.root,0x300000));rngAfter=bytes(w.u.mem_read(0x8a5d44,4));w.u.mem_write(w.root,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,rows=rows,worldBeforeSha=sha(baseline),worldAfterSha=sha(after),rngBefore=rng.hex(),rngAfter=rngAfter.hex(),wholeWorldAndRngRestored=True,completeGoal=False,limits=['Original constructors and relocation getter on declared units; not original deployment/menu proof','Earlier constructed crew arrays alone must not imply original person current-unit membership']),indent=2)+'\n');print('PASS original membership SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--immediate',action='store_true');a=p.parse_args();inspect(a.installation,a.output,a.immediate)
