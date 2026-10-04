#!/usr/bin/env python3
"""Original source coordinate conversion and city shape occupancy registration."""
import argparse,gzip,hashlib,json,struct,sys
from pathlib import Path

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args()
 source=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve();out=a.output.resolve()
 if out==source or source in out.parents:raise ValueError('Read-only PC installation')
 out.mkdir(parents=True,exist_ok=False);sys.path.insert(0,str(Path(__file__).resolve().parent))
 from test_pc_city_action_costs import CityActionCostsTest
 from pc_original_pe_data import load_original_data
 from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_EIP
 CityActionCostsTest.setUpClass();t=CityActionCostsTest();d=t.d;u=d.u;mappings=load_original_data(u);building,_,_,_=t.actors()
 pack=lambda x,y:((y&65535)<<16)|(x&65535)
 word=lambda at,v:u.mem_write(at,struct.pack('<I',v&0xffffffff))
 read=lambda at:struct.unpack('<I',u.mem_read(at,4))[0]
 cell=lambda x,y:0x6fb0e68+(x*200+y)*20
 gridbase=0x6fb0000;gridbytes=0x100000;worldbase=0x7200000;worldbytes=0x300000
 word(0x73f550c,-1)
 terrain_flags=[read(0x8a5df8+4*i) for i in range(32)];allowed_terrain=next(i for i,v in enumerate(terrain_flags) if v)
 original_grid=bytes(u.mem_read(gridbase,gridbytes));record=0x4648350;receiver=0x6ed6eb0;rows=[]
 source_shape=bytes(u.mem_read(0x77a940,112));directions=bytes(u.mem_read(0x79c310,48))
 # Source5a0160 computes pixel coordinates, then halves after adding1.
 # Record index0 / selector0 is explicit fixture input, not allocator execution.
 for x,y in [(99,99),(100,100),(100,99),(99,100),(1,1),(198,198)]:
  u.mem_write(gridbase,original_grid);word(building+8,0);u.mem_write(building+0x1e,struct.pack('<hh',x,y));frame=d.stack-0x400
  u.reg_write(UC_X86_REG_ESI,building);u.reg_write(UC_X86_REG_ESP,frame)
  world=bytes(u.mem_read(worldbase,worldbytes));rng=bytes(u.mem_read(0x8a5d44,4))
  u.emu_start(0x5a0169,0x5a01aa,count=1000);assert u.reg_read(UC_X86_REG_EIP)==0x5a01aa
  half=read(frame+8)
  assert struct.unpack('<hh',struct.pack('<I',half))==(2*x+56,2*y+56+(x&1))
  u.emu_start(0x5a01bc,0x5a01f0,count=1000);assert u.reg_read(UC_X86_REG_EIP)==0x5a01f0
  anchor=read(frame+0x14)
  assert struct.unpack('<hh',struct.pack('<I',anchor))==(2*x+57,2*y+57+(x&1))
  assert world==bytes(u.mem_read(worldbase,worldbytes)) and rng==bytes(u.mem_read(0x8a5d44,4))
  u.mem_write(record,bytes(128));u.mem_write(record+0x6e,struct.pack('<I',anchor));word(record+0x64,building)
  selector_flag=u.mem_read(0x79c258,1)[0]
  world=bytes(u.mem_read(worldbase,worldbytes));grid=bytes(u.mem_read(gridbase,gridbytes));rng=bytes(u.mem_read(0x8a5d44,4));scratch=bytes(u.mem_read(receiver+0x1205012,0x80000))
  u.reg_write(UC_X86_REG_ECX,receiver);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<II',d.stop,record));u.emu_start(0x416690,d.stop,count=1000000);assert u.reg_read(UC_X86_REG_EIP)==d.stop
  occupied=[]
  for q in range(x-2,x+3):
   for r in range(y-2,y+3):
    if u.mem_read(cell(q,r),1)[0]&3:occupied.append([q,r])
  output=d.stack+0x600;neighbors=[]
  for direction in range(6):
   t.call(0x483a50,output,pack(x,y),direction);neighbors.append(list(struct.unpack('<hh',u.mem_read(output,4))))
  expected=sorted([[x,y]]+neighbors);assert sorted(occupied)==expected,(x,y,occupied,expected)
  exact_grid=bytearray(grid)
  for q,r in expected:
   at=cell(q,r)-gridbase;value=struct.unpack_from('<I',grid,at)[0];struct.pack_into('<I',exact_grid,at,(value&0xfffffffe)|2);struct.pack_into('<H',exact_grid,at+8,0)
  # Original city selector also clears its radius flag. Fixture grid starts
  # with that flag zero, so this must not introduce any additional grid delta.
  assert bytes(exact_grid)==bytes(u.mem_read(gridbase,gridbytes))
  assert world==bytes(u.mem_read(worldbase,worldbytes)) and rng==bytes(u.mem_read(0x8a5d44,4))
  for q,r in expected:
   u.reg_write(UC_X86_REG_ECX,cell(q,r));assert t.call(0x483b20)==building
  landing=[]
  for q,r in expected:
   at=cell(q,r)+4;word(at,(read(at)&~31)|allowed_terrain)
   before=bytes(u.mem_read(worldbase,worldbytes));before_grid=bytes(u.mem_read(gridbase,gridbytes));before_rng=bytes(u.mem_read(0x8a5d44,4))
   assert t.call(0x594650,pack(q,r))==0
   assert before==bytes(u.mem_read(worldbase,worldbytes)) and before_grid==bytes(u.mem_read(gridbase,gridbytes)) and before_rng==bytes(u.mem_read(0x8a5d44,4))
   landing.append(dict(cell=[q,r],allowed=False))
  q,r=(x+2 if x+2<200 else x-2),y;at=cell(q,r)+4;word(at,(read(at)&~31)|allowed_terrain)
  before=bytes(u.mem_read(worldbase,worldbytes));before_grid=bytes(u.mem_read(gridbase,gridbytes));before_rng=bytes(u.mem_read(0x8a5d44,4))
  assert t.call(0x594650,pack(q,r))==1
  assert before==bytes(u.mem_read(worldbase,worldbytes)) and before_grid==bytes(u.mem_read(gridbase,gridbytes)) and before_rng==bytes(u.mem_read(0x8a5d44,4))
  landing.append(dict(cell=[q,r],allowed=True))
  rows.append(dict(center=[x,y],source_half_coordinate=hex(half),source_anchor_coordinate=hex(anchor),occupied=expected,actual_registry_lookup_all_seven=True,actual_cavalry_landing=landing,exact_grid_effect=True,world_rng_unchanged=True,render_scratch_changed=scratch!=bytes(u.mem_read(receiver+0x1205012,0x80000))))
 raw=(source/'san11pk.exe').read_bytes();shared=(source/'Media/scenario/Scenario.s11').read_bytes()
 assert hashlib.sha256(raw).hexdigest()=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
 assert hashlib.sha256(shared).hexdigest()=='dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f'
 report=dict(executable_sha256=hashlib.sha256(raw).hexdigest(),shared_sha256=hashlib.sha256(shared).hexdigest(),original_data_mappings=mappings,source_shape=dict(address='77a940',bytes_hex=source_shape.hex()),selector_radius_flag=selector_flag,directions=dict(address='79c310',bytes_hex=directions.hex()),rows=rows,
  functions=[dict(start=hex(low),end=hex(high),bytes_hex=raw[low-0x400000:high-0x400000].hex()) for low,high in [(0x5a0169,0x5a01aa),(0x5a01bc,0x5a01f0),(0x416690,0x416819),(0x4848f0,0x484952)]],
  landing_inputs=dict(inactive_view_filter=-1,allowed_terrain_index=allowed_terrain),limits=['Native city0 selector0 graphical record/index0 and rendering receiver are controlled fixtures; normal allocator417590 and frontend not executed','Source coordinate/anchor calculation and complete shape registration416690 execute; scratch subcell array writes are rendering bookkeeping','Cavalry landing594650 uses explicit inactive filter and valid terrain input; no complete PC opening, active filter initialization, full tactic damage or authority commit claim','World and RNG unchanged; registration exact grid write only seven occupancy/index entries'])
 encoded=(json.dumps(report,indent=2)+'\n').encode();(out/'city-footprint-native.json').write_bytes(encoded)
 with (out/'city-footprint-native.json.gz').open('wb') as f:
  with gzip.GzipFile(filename='',mode='wb',fileobj=f,mtime=0) as z:z.write(encoded)
 print('PASS original city shape registration6 centers; seven exact cells/source anchor/parities/bounds/actual registry lookups; world/RNG unchanged')

if __name__=='__main__':main()
