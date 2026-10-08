#!/usr/bin/env python3
"""Full original death cleanup for the actual source0 three-person sworn group."""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EIP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve receipt')
 print('Loading original source',flush=True);d,w,source,geo,_=prepare(installation);before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));rows=[]
 def people():
  return [dict(nativeId=n,hex=bytes(w.u.mem_read(w.root+0xc0bc+n*0x190,0x190)).hex())for n in range(1100)]
 for order in [[98],[432],[635],[98,432,635],[635,98,432]]:
  w.u.mem_write(0x7200000,before);w.u.mem_write(0x8a5d44,rng);steps=[]
  for target in order:
   previous=bytes(w.u.mem_read(0x7200000,0x300000));records=people();pointer=w.call(0x490b00,target,receiver=w.root)
   try:w.call(0x4acae0,pointer,receiver=0x799895c,count=50000000)
   except Exception as e:output.with_suffix('.failure.json').write_text(json.dumps(dict(rows=rows,order=order,target=target,error=repr(e),steps=steps,ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid),indent=2)+'\n');raise
   current=bytes(w.u.mem_read(0x7200000,0x300000));assert rng==bytes(w.u.mem_read(0x8a5d44,4));steps.append(dict(target=target,before=records,after=people(),changedBytes=sum(a!=b for a,b in zip(previous,current)),rngPure=True));print('Original full death',target,'group',[(n,struct.unpack('<i',w.u.mem_read(w.root+0xc0bc+n*0x190+0x64,4))[0])for n in [98,432,635]],flush=True)
  rows.append(dict(order=order,steps=steps))
 w.u.mem_write(0x7200000,before);w.u.mem_write(0x8a5d44,rng);assert before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,rows=rows,wholeWorldAndRngRestored=True,limits=['Full original4acae0 including4ab9a0 actual98/432/635 sourcegroup; no anchor/relations/outcome substitution','Standalone death cleanup after source loader; battle/coronation/deployment/GUI/APK separate']),indent=2)+'\n');print('PASS original full sworn cleanup',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
