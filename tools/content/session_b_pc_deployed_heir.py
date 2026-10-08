#!/usr/bin/env python3
"""Complete original coronation of a deployed heir; declared original unit.
No abilities, identity, raw loyalty, task, battle outcome or RNG overrides.
"""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EIP,UC_X86_REG_ESP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve receipt')
 print('Loading original source',flush=True);d,w,source,geo,_=prepare(installation)
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));rows=[];stage='constructors';calls=[]
 def snapshot(force,unit):
  return dict(people=[dict(nativeId=n,hex=bytes(w.u.mem_read(w.root+0xc0bc+n*0x190,0x190)).hex())for n in range(1100)],armies=[dict(nativeId=n,hex=bytes(w.u.mem_read(w.call(0x490ad0,n,receiver=w.root),0x50)).hex())for n in range(47)],sites=[dict(nativeId=n,governorRaw=w.call(0x4c69a0,w.call(0x490d00,n,receiver=w.root),14))for n in range(87)],forceHex=bytes(w.u.mem_read(force,0x12c)).hex(),unitHex=bytes(w.u.mem_read(unit,0xf4)).hex())
 try:
  w.call(0x415400,receiver=0x32602b0,count=50000000);w.call(0x477810,receiver=0x6ee7888,count=50000000);w.call(0x4f52d0,receiver=0x91ba558,count=50000000)
  assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
  def trace(u,ip,size,user):
   sp=u.reg_read(UC_X86_REG_ESP);calls.append(dict(address=hex(ip),words=list(struct.unpack('<5I',u.mem_read(sp,20)))))
  for ip in [0x4b21b0,0x495340,0x4bbaa0]:w.u.hook_add(UC_HOOK_CODE,trace,begin=ip,end=ip)
  for crew,troops,energy in [([98,432],5000,100),([432,98],5000,100),([635,98,432],5000,100),([98,635,432],5000,100),([98],20000,120),([432,98],20000,120),([635,432,98],20000,120)]:
   w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);calls=[]
   force=w.call(0x490aa0,4,receiver=w.root);old=w.call(0x490b00,614,receiver=w.root);heir=w.call(0x490b00,98,receiver=w.root)
   unit=w.root+0x169730;w.u.mem_write(unit+8,struct.pack('<i',0));w.u.mem_write(unit+0xc,struct.pack('<3i',*(crew+[-1]*(3-len(crew)))));w.u.mem_write(unit+0x18,struct.pack('<H',troops));w.u.mem_write(unit+0x1a,bytes([energy]));w.u.mem_write(unit+0x3c,struct.pack('<hh',80,80));w.call(0x496250,997,receiver=unit);w.call(0x496280,17000,receiver=unit)
   assert w.call(0x47a630,unit)==1;w.u.reg_write(UC_X86_REG_EAX,unit);loc=w.call(0x4a7530)
   for native in crew:w.call(0x4a0cb0,w.call(0x490b00,native,receiver=w.root),loc,receiver=0x799895c,count=50000000)
   assert w.call(0x47a630,heir)==1 and w.call(0x489220,receiver=heir)==0 and w.call(0x488c00,receiver=old)==1
   before=snapshot(force,unit);worldBefore=bytes(w.u.mem_read(0x7200000,0x300000));calls=[];stage='crown '+str(crew);print(stage,flush=True)
   w.call(0x4b78f0,force,heir,old,receiver=0x799895c,count=50000000)
   after=snapshot(force,unit);worldAfter=bytes(w.u.mem_read(0x7200000,0x300000));assert rng==bytes(w.u.mem_read(0x8a5d44,4))
   rows.append(dict(crewBefore=crew,declaredTroops=troops,declaredEnergy=energy,before=before,after=after,calls=calls,changedBytes=sum(a!=b for a,b in zip(worldBefore,worldAfter)),rngPure=True));print('Full crown finished',rows[-1]['changedBytes'],flush=True)
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
  output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,heirNative=98,departingNative=614,rows=rows,wholeWorldAndRngRestored=True,limits=['Complete original4b78f0 with declared deployed source0 heir98; full constructors and seven declared crew/resource cases','Not a normal deployment, AI selection, death callback, menu or Android acceptance','No omitted numeric or presentation callbacks and no source stats or RNG substitution']),indent=2)+'\n');print('PASS original deployed heir',sha(output.read_bytes()),flush=True)
 except Exception as e:
  output.with_suffix('.failure.json').write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,stage=stage,rows=rows,calls=calls,error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid),indent=2)+'\n');raise
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
