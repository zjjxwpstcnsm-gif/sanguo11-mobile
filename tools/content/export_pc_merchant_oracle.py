#!/usr/bin/env python3
"""Export actual x86 merchant results for independent Java differential tests.

No Python implementation supplies the expected arithmetic. All result columns
come from the pinned original functions, with full3MiB mutation checks.
"""
import argparse, csv, hashlib, io, json, struct
from pathlib import Path
from unicorn import UC_HOOK_MEM_WRITE
from unicorn.x86_const import UC_X86_REG_ECX
from inspect_pc_scenario_tail import NativeTailDecoder
from inspect_pc_effect_bindings import EXE_SHA
from test_pc_city_action_costs import CityActionCostsTest

def export(exe,shared,output,audit):
 raw=exe.read_bytes()
 if hashlib.sha256(raw).hexdigest()!=EXE_SHA: raise ValueError('Unverified EXE')
 for p in (output,audit):
  if p.resolve()==exe.parent.resolve() or exe.parent.resolve() in p.resolve().parents:
   raise ValueError('PC installation is read-only')
 t=CityActionCostsTest(); t.d=NativeTailDecoder(raw); d=t.d
 shared_bytes=shared.read_bytes(); d.decode_tail(shared_bytes,True); u=d.u
 building,city,_,_=t.actors(); officer=d.root+0xc0bc
 u.reg_write(UC_X86_REG_ECX,officer); t.call(0x489f10); u.mem_write(officer+0xa0,struct.pack('<I',0))
 rows=[]; writes=[0]
 hook=u.hook_add(UC_HOOK_MEM_WRITE,lambda *args:writes.__setitem__(0,writes[0]+1),begin=0x8a5d44,end=0x8a5d47)
 def invoke(address,args,seed,price=False):
  u.mem_write(0x8a5d44,struct.pack('<I',seed)); writes[0]=0
  before=bytearray(u.mem_read(0x7200000,0x300000)); result=t.call(address,*args)
  if price:
   result=bytes(u.mem_read(city+0x7c,1))[0]; before[city+0x7c-0x7200000]=result
  if bytes(before)!=bytes(u.mem_read(0x7200000,0x300000)): raise AssertionError('Unexpected world mutation')
  after=struct.unpack('<I',u.mem_read(0x8a5d44,4))[0]
  if not price and address not in (0x472150,0x4721d0) and (after!=seed or writes[0]):
   raise AssertionError('Unexpected RNG mutation')
  return result,after,writes[0]
 try:
  for politics in (0,1,50,80,100,255):
   u.mem_write(officer+0x173,bytes([politics]))
   for rate in (1,10,25,50,255):
    u.mem_write(city+0x7c,bytes([rate]))
    for amount in (0,1,999,1000,20000,1000000,2147483647,-1,-999,-1000,-20000,-1000000,-2147483647,-2147483648):
     value,state,draws=invoke(0x5ca620,(building,officer,amount&0xffffffff),42)
     rows.append(['quote',amount,politics,rate,0,42,value if value<2**31 else value-2**32,state,draws])
  for politics in (0,80,100,255):
   u.mem_write(officer+0x173,bytes([politics]))
   for rate in (1,30,50,70,255):
    u.mem_write(city+0x7c,bytes([rate]))
    for gold,food in ((0,0),(0,1),(1,1),(9999,999),(50000,100000),(99999,999999),(100000,1000000),(100000,999999)):
     u.mem_write(city+0x44,struct.pack('<II',gold,food))
     for buy in (0,1):
      value,state,draws=invoke(0x5ca770,(building,officer,buy),42)
      rows.append(['buymax' if buy else 'sellmax',gold,food,politics,rate,42,value,state,draws])
  for initial in (True,False):
   cases=((month,50,0xffffffff,seed) for month in range(14) for seed in range(16)) if initial else (
      (1,rate,flags,seed) for rate in (0,29,30,40,49,50,51,60,70,71,255)
      for flags in (0,1,2,3,4,5,8,0xffffffff) for seed in range(32))
   for month,rate,flags,seed in cases:
    u.mem_write(city+0x7c,bytes([rate])); u.mem_write(city+0x9c,struct.pack('<I',flags))
    u.reg_write(UC_X86_REG_ECX,0x799895c)
    value,state,draws=invoke(0x4b3c60,(city,month,int(initial)),seed,True)
    rows.append(['initial' if initial else 'monthly',month,rate,flags,0,seed,value,state,draws])
  for seed in (0,1,42,0x7fffffff,0xffffffff):
   for percent,values in ((False,(-1,0,1,2,3,100,65535,65536,65537,2147483647)),(True,(-1,0,1,40,50,100,101))):
    for n in values:
     value,state,draws=invoke(0x4721d0 if percent else 0x472150,(n&0xffffffff,),seed)
     rows.append(['percent' if percent else 'uniform',n,0,0,0,seed,value if percent else value&0xffff,state,draws])
 finally: u.hook_del(hook)
 buf=io.StringIO(); writer=csv.writer(buf,delimiter='\t',lineterminator='\n')
 writer.writerow(['kind','a','b','c','d','seed','result','state_after','draws']); writer.writerows(rows)
 data=buf.getvalue().encode(); output.write_bytes(data)
 counts={name:sum(row[0]==name for row in rows) for name in sorted({r[0] for r in rows})}
 report=dict(schema=1,source_path='san11pk.exe',executable_sha256=EXE_SHA,
   shared_path='Media/scenario/Scenario.s11',shared_sha256=hashlib.sha256(shared_bytes).hexdigest(),
   output_sha256=hashlib.sha256(data).hexdigest(),rows=len(rows),counts=counts,
   functions={'quote':'5ca620','quantity':'5ca770','price':'4b3c60','uniform':'472150','percent':'4721d0'},
   method='Original instructions without replaced arithmetic/RNG; exact3MiB mutation and actual RNG write-count checks',
   limits=['Price and quantity not yet wired into project gameplay','Full command admission and global settlement not executed by this fixture'])
 audit.write_text(json.dumps(report,indent=2)+'\n'); print(json.dumps(report))

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for name in ('exe','shared','output','audit'): p.add_argument('--'+name,type=Path,required=True)
 a=p.parse_args(); export(a.exe,a.shared,a.output,a.audit)
