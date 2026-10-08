#!/usr/bin/env python3
"""Full original coronation with a declared valid current district heir.

The heir is an explicit input, not the AI selector result or a death fixture.
No source officer, city, army, loyalty, AP or RNG value is overwritten.
"""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EIP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve original receipt')
 print('Loading original source',flush=True);d,w,source,geo,_=prepare(installation)
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));force=w.call(0x490aa0,28,receiver=w.root);dead=w.call(0x490b00,403,receiver=w.root);heir=w.call(0x490b00,440,receiver=w.root)
 assert w.call(0x488c00,receiver=dead)==1 and struct.unpack('<i',w.u.mem_read(heir+0x94,4))[0]==6
 # Actual original constructors, isolated from rule World and native RNG.
 w.call(0x415400,receiver=0x32602b0,count=50000000);w.call(0x477810,receiver=0x6ee7888,count=50000000);w.call(0x4f52d0,receiver=0x91ba558,count=50000000)
 assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 people=[dict(nativeId=i,pointer=w.root+0xc0bc+i*0x190)for i in range(1100)];assert people[-1]['pointer']==w.call(0x490b00,1099,receiver=w.root)
 armies=[dict(nativeId=i,pointer=w.call(0x490ad0,i,receiver=w.root))for i in range(47)];sites=[dict(nativeId=i,pointer=w.root+(0x1d8+i*0x248 if i<42 else 0x61a8+(i-42)*0x90 if i<52 else 0x6748+(i-52)*0x90))for i in range(87)]
 def snapshot():
  rows={kind:[dict(**row,hex=bytes(w.u.mem_read(row['pointer'],length)).hex())for row in entries]for kind,entries,length in [('people',people,0x190),('armies',armies,0x50),('sites',sites,0x248)]}
  for row in rows['sites']:
   ptr=w.call(0x490d00,row['nativeId'],receiver=w.root);row['propertyWrapperPointer']=ptr;vt=struct.unpack('<I',w.u.mem_read(ptr,4))[0];getter=struct.unpack('<I',w.u.mem_read(vt+0x44,4))[0];row['armyRaw']=w.call(getter,receiver=ptr);row['governorRaw']=w.call(0x4c69a0,ptr,14)
  rows['forceHex']=bytes(w.u.mem_read(force,0x12c)).hex();return rows
 before=snapshot();calls=[]
 def entry(u,address,size,user):
  sp=u.reg_read(UC_X86_REG_ESP);calls.append(dict(address=hex(address),stackWords=list(struct.unpack('<5I',u.mem_read(sp,20)))))
 for address in [0x4bd3b0,0x4a0940,0x4b3a20,0x4a75a0]:w.u.hook_add(UC_HOOK_CODE,entry,begin=address,end=address)
 print('Original full cross-army coronation entering',flush=True)
 try:w.call(0x4b78f0,force,heir,dead,receiver=0x799895c,count=50000000)
 except Exception as e:
  output.with_suffix('.failure.json').write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,before=before,calls=calls,error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid),indent=2)+'\n');raise
 after=snapshot();world=bytes(w.u.mem_read(0x7200000,0x300000));rngAfter=bytes(w.u.mem_read(0x8a5d44,4));w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,declaredHeirNative=440,departingRulerNative=403,before=before,after=after,calls=calls,changedBytes=sum(a!=b for a,b in zip(baseline,world)),worldBeforeSha=sha(baseline),worldAfterSha=sha(world),rngBefore=int.from_bytes(rng,'little'),rngAfter=int.from_bytes(rngAfter,'little'),wholeWorldAndRngRestored=True,limits=['Full original4b78f0 with actual valid force28 district440 as explicit heir input','Not AI selected heir435, original human selection GUI, death settlement, deployment or APK acceptance','No numeric or presentation callback omission']),indent=2)+'\n');print('PASS original cross-army coronation',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
