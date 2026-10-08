#!/usr/bin/env python3
"""Full original544900 on original base-window tree, source7 flag retained.
No control virtual/predicate substitutions; GUI pixels/defaults unproven.
"""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EIP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve receipt')
 d,w,source,geo,_=prepare(installation,7);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));window=0x16000000;w.u.mem_map(window,0x40000);stage='base constructor';rows=[]
 try:
  w.call(0x5593e0,receiver=window,count=50000000)
  for life in [0,1,2]:
   for event in [0,1,2]:
    for offset in [0x21c,0x220,0x22c,0x230]:w.u.mem_write(window+offset,struct.pack('<i',life if offset==0x22c else event if offset==0x230 else 0))
    before=bytes(w.u.mem_read(window+0x214,56));stage='full constraint callback';w.call(0x544900,receiver=window,count=50000000);after=bytes(w.u.mem_read(window+0x214,56));values=[struct.unpack('<i',w.u.mem_read(window+off,4))[0]for off in [0x21c,0x220,0x22c,0x230]];assert values==[1,1,2,1 if event==0 else event];assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(lifeInput=life,eventInput=event,beforeHex=before.hex(),afterHex=after.hex(),result=values,wholeWorldAndRngPure=True));print('PASS full original constraint',life,event,values,flush=True)
  output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,rows=rows,limits=['Full544900 and original5593e0 base-window constructor, no omitted methods or control predicate substitutions','Empty original control tree: actual button disabling visual/real window children is not proven','Declaration of draft life/event values; not normal GUI/default/full startup/Android acceptance']),indent=2)+'\n');print('PASS original constraint callback',sha(output.read_bytes()),flush=True)
 except Exception as e:
  output.with_suffix('.failure.json').write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,stage=stage,error=repr(e),rows=rows,ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid),indent=2)+'\n');raise
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
