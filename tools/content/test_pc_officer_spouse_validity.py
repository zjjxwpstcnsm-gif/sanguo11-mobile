#!/usr/bin/env python3
"""Execute the original spouse validity/current-ability chain without Wine or source writes."""
import argparse,hashlib,json,struct
from pathlib import Path
from unicorn import UC_HOOK_MEM_WRITE
from unicorn.x86_const import UC_X86_REG_ECX
from inspect_pc_scenario_tail import NativeTailDecoder
from test_pc_city_action_costs import CityActionCostsTest
ROOT=Path(__file__).resolve().parents[2]
def sha(b):return hashlib.sha256(b).hexdigest()
def run(installation,output):
 installation=installation.resolve()
 if installation==output.resolve() or installation in output.resolve().parents:raise ValueError('Read-only installation')
 raw=(installation/'san11pk.exe').read_bytes();assert sha(raw)=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
 shared=(installation/'Media/scenario/Scenario.s11').read_bytes();d=NativeTailDecoder(raw);d.decode_tail(shared,True);t=CityActionCostsTest();t.d=d;u=d.u;o=d.root+0xc0bc;spouse=o+0x190
 for p in (o,spouse):
  u.reg_write(UC_X86_REG_ECX,p);t.call(0x489f10);u.reg_write(UC_X86_REG_ECX,p);t.call(0x488470,0);u.mem_write(p+0xa0,struct.pack('<i',0))
 u.mem_write(o+0xc8,bytes([80]*5));u.mem_write(o+0xa4,struct.pack('<i',-1));u.mem_write(o+0x60,struct.pack('<i',1));u.mem_write(0x7201980,struct.pack('<i',1))
 def guard(machine,access,address,size,value,user):
  if address<d.stack-0x10000 or address+size>d.stack+0x10000:raise ValueError('Unexpected nonstack write')
 hook=u.hook_add(UC_HOOK_MEM_WRITE,guard);rows=[]
 try:
  for status in range(9):
   for override in (0,1):
    for own,other in ((0,0),(99,0),(0,99),(99,99)):
     u.mem_write(spouse+0xa0,struct.pack('<i',status));u.mem_write(spouse+0x17c,struct.pack('<i',override));u.mem_write(o+0xe8,struct.pack('<i',own));u.mem_write(spouse+0xe8,struct.pack('<i',other))
     before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));valid=t.call(0x47a630,spouse)
     assert valid==int(override or status not in (6,8))
     values=[]
     for stat in range(5):
      u.reg_write(UC_X86_REG_ECX,o);value=t.call(0x48a110,stat,0)&255;assert value==80+int(bool(valid) and (own==99 or other==99));values.append(value)
     assert before==bytes(u.mem_read(0x7200000,0x300000)) and rng==bytes(u.mem_read(0x8a5d44,4))
     rows.append(dict(spouse_status=status,override_17c=override,own_skill=own,spouse_skill=other,valid=valid,values=values))
 finally:u.hook_del(hook)
 labels=json.loads((ROOT/'docs/pc-data/scenario-placements-audit.json').read_text())['labels']['status']
 report=dict(schema=1,exe_sha256=sha(raw),shared_sha256=sha(shared),functions={hex(a):dict(size=n,sha256=sha(bytes(u.mem_read(a,n)))) for a,n in [(0x47a630,38),(0x488430,49),(0x48a110,240)]},labels=labels,rows=rows,current_ability_checks=len(rows)*5,full_three_mib_and_rng_unchanged=True,non_stack_write_guard=True,limits=['Native +17c override semantics outside this explicit numeric test remain unknown; project has no such override state','Unappeared6/dead8 mapping uses original status labels, not remembered tables','Scenario/MOD activation and all original status imports remain pending'])
 output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print('PASS original spouse validity cases='+str(len(rows))+' ability_checks='+str(len(rows)*5))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--installation',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();run(a.installation,a.output)
