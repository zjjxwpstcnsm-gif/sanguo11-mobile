#!/usr/bin/env python3
"""Execute original cultivation entry/validators/registration through AP debit.

Stop before the uninitialized frontend refresh in5b9387. This is not a complete
PC command return, UI test, official opening, or Android base-policy migration.
No game instruction, rule callback or validator return is replaced.
"""
import argparse,gzip,hashlib,json,struct,sys
from collections import deque
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--output',type=Path,required=True)
a=p.parse_args();out=a.output.resolve()
source=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve()
if out==source or source in out.parents:raise ValueError('PC installation is read only')
out.mkdir(parents=True,exist_ok=False)
sys.path.insert(0,str(Path(__file__).resolve().parent))
import test_pc_city_action_costs as support
from pc_original_pe_data import load_original_data
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP
support.CityActionCostsTest.setUpClass();t,d=support.CityActionCostsTest(),support.CityActionCostsTest.d;u=d.u
mappings=load_original_data(u)
raw=(source/'san11pk.exe').read_bytes();shared=(source/'Media/scenario/Scenario.s11').read_bytes()
exe_sha=hashlib.sha256(raw).hexdigest();shared_sha=hashlib.sha256(shared).hexdigest()
assert exe_sha=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
assert shared_sha=='dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f'
world_base=0x7200000;world_bytes=0x300000
building,city,district,_=t.actors();force=d.root+0x7af8;officer=d.root+0xc0bc
word=lambda addr,value:u.mem_write(addr,struct.pack('<I',value&0xffffffff))
read=lambda addr:struct.unpack('<I',u.mem_read(addr,4))[0]
u.mem_write(force,bytes(0x12c));u.reg_write(UC_X86_REG_ECX,force);t.call(0x481830)
word(force+4,0);word(force+0x60,-1)
for index in range(1100):
 u.reg_write(UC_X86_REG_ECX,d.root+0xc0bc+index*0x190);t.call(0x489f10)
for offset,value in [(0x94,0),(0x98,0),(0x9c,0),(0xa0,0),(0xa4,-1),(0x60,-1),(0x15c,0)]:word(officer+offset,value)
u.mem_write(officer+0xc8,bytes([50]*5));u.mem_write(officer+0xd0,struct.pack('<5i',*([-1]*5)))
word(officer+0xe8,-1);u.reg_write(UC_X86_REG_ECX,officer);t.call(0x48a2d0)
for address,task in [(0x8ba130,41),(0x8ba154,42),(0x8ba16c,43)]:word(address,task)
rows=[r for r in d.records if r['kind']=='table_86dd8'];assert [r['native_index'] for r in rows]==list(range(98))
for row in rows:
 u.reg_write(UC_X86_REG_ECX,row['actor_address']);t.call(0x494f50,row['native_index'])
baseline=bytes(u.mem_read(world_base,world_bytes));request=d.stack+0x400
trace=deque(maxlen=48)
def tracing(machine,address,size,user):
 if 0x400000<=address<0x740000:trace.append(hex(address))
u.hook_add(UC_HOOK_CODE,tracing)
# Observation only, at the instruction after the original AP primitive returns.
stop_hook=u.hook_add(UC_HOOK_CODE,lambda m,ad,z,q:m.emu_stop(),begin=0x5b9387,end=0x5b9387)
def call(entry,*args,receiver=None):
 if receiver is not None:u.reg_write(UC_X86_REG_ECX,receiver)
 u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<'+'I'*(len(args)+1),d.stop,*args))
 u.emu_start(entry,d.stop,count=2000000)
 endpoint=u.reg_read(UC_X86_REG_EIP)
 assert endpoint in [d.stop,0x5b9387],hex(endpoint)
 return endpoint,u.reg_read(UC_X86_REG_EAX)
results=[]
# All98 real rows, plus low-gold/AP boundaries in each of the three categories.
cases=[(r['native_index'],60,5000) for r in rows]
for index in [0,15,27]:cases.extend([(index,20,0),(index,20,1),(index,19,5000)])
try:
 for index,ap,gold in cases:
  u.mem_write(world_base,baseline);row=rows[index];actor=row['actor_address'];category=read(actor+0x48)
  assert category in [0,1,2];task=[41,42,43][category];entry=[0x5d98d0,0x5da6e0,0x5daf10][category]
  # Learned bits are explicit input; check through the actual source getter.
  if index<48:
   word(force+0xb0+4*(index//32),1<<(index%32))
  else:
   word(force+0xe8,index);word(force+0x110,1)
  assert call(0x481970,index,receiver=force)[1]==1
  if category==1:
   aptitude=call(0x494ff0,receiver=actor)[1];rank=call(0x495010,receiver=actor)[1];word(officer+0xb0+4*aptitude,rank-1)
  u.mem_write(district+0x2c,bytes([ap]));word(city+0x44,gold)
  u.mem_write(request,struct.pack('<3I',building,officer,index))
  before=bytes(u.mem_read(world_base,world_bytes));rng=bytes(u.mem_read(0x8a5d44,4));trace.clear()
  try:end,returned=call(entry,request)
  except Exception as error:
   (out/'failure.json').write_text(json.dumps(dict(index=index,category=category,ap=ap,gold=gold,eip=hex(u.reg_read(UC_X86_REG_EIP)),trace=list(trace),error=str(error)),indent=2)+'\n');raise
  after=bytes(u.mem_read(world_base,world_bytes));assert rng==bytes(u.mem_read(0x8a5d44,4))
  allowed=ap>=20
  if not allowed:
   assert end==d.stop and returned==0 and after==before
  else:
   assert end==0x5b9387
   assert u.mem_read(officer+0x158,1)==b'\x03'
   assert bytes(u.mem_read(officer+0x13c,24))==struct.pack('<6i',task,index,0,0,0,0)
   assert read(officer+0x124)&1==1
   assert u.mem_read(district+0x2c,1)==bytes([ap-20])
   assert read(city+0x44)==gold
   # Actor base, XP, aptitude, skill and all city resources stay unchanged.
   expected=bytearray(before);offset=officer-world_base
   struct.pack_into('<6i',expected,offset+0x13c,task,index,0,0,0,0)
   expected[offset+0x158]=3;struct.pack_into('<I',expected,offset+0x124,struct.unpack_from('<I',before,offset+0x124)[0]|1)
   expected[district+0x2c-world_base]=ap-20
   # Original482f80 maintains the task roster, and489b40 signals its cache.
   # Registry metadata is not interpreted as a resource or saved game rule.
   for low,high in [(d.root+0x184,d.root+0x1b8),(d.root+0x1cc,d.root+0x1d0)]:
    expected[low-world_base:high-world_base]=after[low-world_base:high-world_base]
   assert bytes(expected)==after,'Unexpected world mutation outside exact actor/AP and declared registry metadata'
   assert read(d.root+0x190)==1,'One registered actor, not a duplicate'
   check_before=bytes(u.mem_read(world_base,world_bytes));assert call(0x482af0,officer,0,receiver=d.root+0x184)[1]!=0
   assert check_before==bytes(u.mem_read(world_base,world_bytes)),'Actual roster query must be read only'
  metadata=[]
  for low,high in [(d.root+0x184,d.root+0x1b8),(d.root+0x1cc,d.root+0x1d0)]:
   metadata.append(dict(root_offset=hex(low-d.root),bytes=high-low,before_hex=before[low-world_base:high-world_base].hex(),after_hex=after[low-world_base:high-world_base].hex()))
  results.append(dict(native_index=index,source_record_sha256=row['sha256'],category=category,task=task,entry=hex(entry),before_ap=ap,after_ap=ap-20 if allowed else ap,gold_before=gold,gold_after=read(city+0x44),allowed=allowed,stop_address=hex(end),task_countdown=3 if allowed else 0,registered_once=allowed,base_xp_skill_aptitude_unchanged=True,rng_unchanged=True,declared_registry_metadata=metadata))
finally:u.hook_del(stop_hook)
assert len(results)==107 and sum(r['allowed'] for r in results)==104
report=dict(exe_sha256=exe_sha,shared_sha256=shared_sha,rows=results,original_data_mappings=mappings,
            original_functions=[dict(start=hex(start),end=hex(end),bytes_hex=raw[start-0x400000:end-0x400000].hex(),sha256=hashlib.sha256(raw[start-0x400000:end-0x400000]).hexdigest()) for start,end in [(0x5d98d0,0x5d9a88),(0x5da6e0,0x5da8a1),(0x5daf10,0x5db0c8),(0x4a7410,0x4a7476),(0x482f80,0x482fbe)]],
            explicit_inputs=dict(nonplayer_controller=-1,district=0,city=0,officer_home98=0,officer_location9c=0,learned='one tested node via original force bit/hidden slot0',hidden_random_selection_verified=False),
            guard='Complete3MiB must match exact actor task/action/countdown/AP effects; only declared task roster/cache metadata copied for guard, independently count/query checked',
            host_imports='Existing VM permission pointer probes and isolated critical sections; initial original serializer source bytes',
            limits=['Original full entry and all preceding validators execute, but stop in5b9340 before frontend57e5d0; no complete function return/PC UI claim','Gold unchanged through this boundary; post-boundary UI/history573500 and any later behavior remain unexecuted','Registry metadata184..1b8 and1cc..1d0 is declared bookkeeping, not fully byte-predicted','All source rows admitted under explicit learned/selected inputs; original research tree/hidden random setup not proved','No Android cultivation base policy, old saves or PC files modified'])
encoded=(json.dumps(report,indent=2)+'\n').encode();(out/'native-start-core.json').write_bytes(encoded)
with (out/'native-start-core.json.gz').open('wb') as file:
 with gzip.GzipFile(filename='',mode='wb',fileobj=file,mtime=0) as packed:packed.write(encoded)
print('PASS original cultivation start-core107:104 admitted across98 IDs,3 AP rejects;3-turn task, one roster entry, AP20, gold/base/XP unchanged; frontend/full return pending')
