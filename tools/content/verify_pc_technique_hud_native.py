#!/usr/bin/env python3
"""Original technique HUD scalar slices; UI calls are explicit stop boundaries."""
from pathlib import Path
import argparse,json,struct,sys
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve();source=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve()
if out==source or source in out.parents:raise ValueError('Read-only PC source')
out.mkdir(parents=True,exist_ok=False);sys.path.insert(0,str(Path(__file__).resolve().parent))
import test_pc_city_action_costs as s
from pc_original_pe_data import load_original_data
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_EIP
s.CityActionCostsTest.setUpClass();d=s.CityActionCostsTest.d;u=d.u;load_original_data(u);view=d.stream+0x800;force=d.root+0x7af8;rows=[];interpolations=[]
labels={hex(p):bytes(u.mem_read(p,80)).split(b'\0',1)[0].decode('big5') for p in [0x7e8ebc,0x7e9bb8,0x7eb9ac,0x7ec424,0x7ec240]}
assert all(labels[hex(p)]=='技巧Ｐ' for p in [0x7e8ebc,0x7e9bb8,0x7eb9ac,0x7ec424])
for old in [0,1,10000]:
 for new in [0,1,10000]:
  for animated in [False,True]:
   u.mem_write(view,b'\0'*0x200);u.mem_write(view+0x198,struct.pack('<i',old));u.mem_write(force+0xa2,struct.pack('<H',new));u.reg_write(UC_X86_REG_ESI,view);u.reg_write(UC_X86_REG_ECX,force);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<III',view,d.stop,int(animated)))
   before=bytearray(u.mem_read(view,0x200));world=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));u.emu_start(0x6317bf,d.stop,count=100)
   assert u.reg_read(UC_X86_REG_EIP)==d.stop
   if old!=new:
    for at,value in [(0x190,1),(0x194,0 if animated else 1650),(0x198,new),(0x19c,old)]:struct.pack_into('<i',before,at,value)
   assert bytes(before)==bytes(u.mem_read(view,0x200));assert world==bytes(u.mem_read(0x7200000,0x300000));assert rng==bytes(u.mem_read(0x8a5d44,4))
   rows.append(dict(before=old,after=new,animated=animated,changed=old!=new,start_counter=0 if animated else 1650))
for old,new in [(0,10000),(10000,0),(19,20),(20,19),(600,3000)]:
 for elapsed in [0,1,49,50,299,300,599]:
  u.mem_write(view+0x198,struct.pack('<ii',new,old));u.reg_write(UC_X86_REG_ESI,view);u.reg_write(UC_X86_REG_ECX,view);u.reg_write(UC_X86_REG_ESP,d.stack)
  from unicorn.x86_const import UC_X86_REG_EAX
  u.reg_write(UC_X86_REG_EAX,1650+elapsed);rng=bytes(u.mem_read(0x8a5d44,4));world=bytes(u.mem_read(0x7200000,0x300000));ui=bytes(u.mem_read(view,0x200))
  u.emu_start(0x6318c8,0x6318f8,count=100);actual=u.reg_read(UC_X86_REG_ECX);expected=(new*elapsed+old*(600-elapsed))//600
  assert actual==expected,(old,new,elapsed,actual,expected);assert world==bytes(u.mem_read(0x7200000,0x300000));assert ui==bytes(u.mem_read(view,0x200));assert rng==bytes(u.mem_read(0x8a5d44,4))
  interpolations.append(dict(before=old,after=new,elapsed=elapsed,displayed=actual))
(out/'native-results.json').write_text(json.dumps(dict(scope='original HUD changed-value cache and arithmetic fragments; constructed UI memory; no actual renderer/audio or full HUD admission',labels=labels,updates=rows,interpolations=interpolations,world_rng_unchanged=True,ui_call_boundaries=['631650','631750','4d0570'],sound_call_static=dict(address='6318b2',requested_id=33),timing_static=dict(sound_and_roll_start=1650,roll_end=2250,roll_span=600)),ensure_ascii=False,indent=2)+'\n');print('PASS original HUD',len(rows),'updates',len(interpolations),'scalar frames; rule world/RNG unchanged; actual UI/audio pending')
