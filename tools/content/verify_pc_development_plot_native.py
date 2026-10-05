#!/usr/bin/env python3
"""Original development-plot predicate; controlled grid, not whole construction."""
import argparse,gzip,hashlib,json,struct,sys
from pathlib import Path

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args()
 source=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve();out=a.output.resolve()
 if out==source or source in out.parents:raise ValueError('Read-only PC installation')
 out.mkdir(parents=True,exist_ok=False);sys.path.insert(0,str(Path(__file__).resolve().parent))
 from test_pc_city_action_costs import CityActionCostsTest
 from pc_original_pe_data import load_original_data
 from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_EAX,UC_X86_REG_ESP,UC_X86_REG_EIP
 CityActionCostsTest.setUpClass();t=CityActionCostsTest();d=t.d;u=d.u;mappings=load_original_data(u)
 building,city,district,_=t.actors();pack=lambda x,y:((y&65535)<<16)|(x&65535)
 word=lambda at,v:u.mem_write(at,struct.pack('<I',v&0xffffffff))
 cell=lambda x,y:0x6fb0e68+(x*200+y)*20
 u.mem_write(building+0x1e,struct.pack('<hh',100,100))
 territories=list(u.mem_read(0x79c2b0,87));rows=[]
 def checked(x,y,region,origin,developable,occupancy):
  word(cell(100,100)+4,origin<<5)
  valid=0<=x<200 and 0<=y<200
  if valid:
   word(cell(x,y),occupancy);word(cell(x,y)+4,(region<<5)|(0x40000 if developable else 0))
  world=bytes(u.mem_read(0x7200000,0x300000));grid=bytes(u.mem_read(0x6fb0000,0x100000));rng=bytes(u.mem_read(0x8a5d44,4))
  actual=t.call(0x5bbbf0,building,pack(x,y))
  expected=int(valid and developable and not occupancy and territories[region]==territories[origin])
  assert actual==expected,(x,y,region,origin,developable,occupancy,actual,expected)
  assert world==bytes(u.mem_read(0x7200000,0x300000)) and grid==bytes(u.mem_read(0x6fb0000,0x100000)) and rng==bytes(u.mem_read(0x8a5d44,4))
  rows.append(dict(x=x,y=y,target_region=region,origin_region=origin,development_bit=bool(developable),occupancy_low2=occupancy,allowed=bool(actual),world_grid_rng_unchanged=True))
 # All87 original region mappings against a city and its subordinate region.
 for origin in [0,5,44]:
  for region in range(87):
   for developable in [0,1]:
    for occupancy in range(4):checked(101,100,region,origin,developable,occupancy)
 for x,y in [(-1,100),(200,100),(100,-1),(100,200),(0,0),(199,199)]:
  for developable in [0,1]:
   for occupancy in range(4):checked(x,y,0,0,developable,occupancy)
 assert len(rows)==2136
 # Enclosing development-type/plot validator with its real native registry.
 command=d.stack+0x600;category_rows=[]
 word(cell(100,100)+4,0);word(cell(101,100),0);word(cell(101,100)+4,0x40000)
 templates=[r for r in d.records if r['kind']=='table_79c54'];assert len(templates)==64
 for record in templates:
  native=record['native_index'];category=struct.unpack('<I',u.mem_read(record['actor_address']+0xb4,4))[0]
  word(command,building);word(command+0x10,native);word(command+0x14,pack(101,100))
  world=bytes(u.mem_read(0x7200000,0x300000));grid=bytes(u.mem_read(0x6fb0000,0x100000));rng=bytes(u.mem_read(0x8a5d44,4))
  u.reg_write(UC_X86_REG_ECX,command);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<I',d.stop));u.emu_start(0x5bc000,d.stop,count=10000)
  assert u.reg_read(UC_X86_REG_EIP)==d.stop
  actual=u.reg_read(UC_X86_REG_EAX);assert actual==int(category==4),(native,category,actual)
  assert world==bytes(u.mem_read(0x7200000,0x300000)) and grid==bytes(u.mem_read(0x6fb0000,0x100000)) and rng==bytes(u.mem_read(0x8a5d44,4))
  category_rows.append(dict(native_index=native,category=category,source_record_sha256=record['sha256'],allowed=bool(actual),world_grid_rng_unchanged=True))
 raw=(source/'san11pk.exe').read_bytes();shared=(source/'Media/scenario/Scenario.s11').read_bytes()
 assert hashlib.sha256(raw).hexdigest()=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
 assert hashlib.sha256(shared).hexdigest()=='dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f'
 report=dict(executable_sha256=hashlib.sha256(raw).hexdigest(),shared_sha256=hashlib.sha256(shared).hexdigest(),original_data_mappings=mappings,
  function=dict(entry='5bbbf0',end='5bbca9',bytes_hex=raw[0x1bbbf0:0x1bbca9].hex()),region_map=dict(address='79c2b0',bytes_hex=bytes(territories).hex()),
  rows=rows,category_validator=dict(entry='5bc000',end='5bc066',bytes_hex=raw[0x1bc000:0x1bc066].hex(),rows=category_rows),limits=['Controlled grid development bit and occupancy, not original opening or placement registry','Enclosing5bc000 validcity/category/plot only; complete construction command adds more conditions','No gold/AP/crew/full admission/progress/field14 transition or Android integration claim','No external active MOD callback verified; PC installation read only'])
 encoded=(json.dumps(report,indent=2)+'\n').encode();(out/'development-plot-native.json').write_bytes(encoded)
 with (out/'development-plot-native.json.gz').open('wb') as f:
  with gzip.GzipFile(filename='',mode='wb',fileobj=f,mtime=0) as z:z.write(encoded)
 print('PASS original development plot2136 and source category validator64; bounds/development bit/occupancy/87 source territory mappings;3MiB/grid/RNG read only')

if __name__=='__main__':main()
