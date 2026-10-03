from pathlib import Path
import argparse,hashlib
p=argparse.ArgumentParser(description='Execute original42-city factory refresh with strict full-world/RNG observations; PC read only, no Wine')
p.add_argument('--output',type=Path,required=True)
output=p.parse_args().output.resolve()
installation=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve()
if output==installation or installation in output.parents:raise ValueError('Output must be outside PC installation')
output.mkdir(parents=True,exist_ok=False)
assert hashlib.sha256((installation/'san11pk.exe').read_bytes()).hexdigest()=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'

import sys,struct,json
sys.path.insert(0,str(Path('tools/content').resolve()))
import test_pc_city_action_costs as s
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP
s.CityActionCostsTest.setUpClass();t=s.CityActionCostsTest();d=t.d;u=d.u;building,city,district,facility=t.actors();facilities=[facility+n*0x38 for n in range(5)]
for f in facilities:u.reg_write(UC_X86_REG_ECX,f);t.call(0x4880a0)
for f,kind in zip(facilities,[33,34,35,36,37]):u.mem_write(f+8,struct.pack('<I',kind));u.mem_write(f+0x14,struct.pack('<I',1))
u.mem_write(city+0xe8,struct.pack('<I',5));u.mem_write(city+0xf0,b''.join(struct.pack('<II',0,f) for f in facilities));rows=[]
for ready in [0,1]:
 for ordinary in [-1,0,1,7]:
  for delayed in [-1,0,1,3]:
   for f in facilities:u.mem_write(f+0x14,struct.pack('<I',ready))
   u.mem_write(city+0xa8,struct.pack('<5b',ordinary,ordinary,ordinary,delayed,delayed));before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<I',d.stop));u.emu_start(0x598400,d.stop,count=1000000);assert u.reg_read(UC_X86_REG_EIP)==d.stop
   actual=list(struct.unpack('<5b',u.mem_read(city+0xa8,5)));assert actual==[ready,ready,ready,delayed,delayed],(ready,ordinary,delayed,actual)
   after=bytes(u.mem_read(0x7200000,0x300000));changed=[0x7200000+i for i,(a,b) in enumerate(zip(before,after)) if a!=b];allowed={d.root+0x1d8+n*0x248+offset for n in range(42) for offset in range(0xa8,0xab)};assert set(changed)<=allowed,[(hex(a)) for a in changed if a not in allowed];assert rng==bytes(u.mem_read(0x8a5d44,4));rows.append(dict(ready=ready,ordinary_before=ordinary,delayed_before=delayed,counters_after=actual,changed_addresses=list(map(hex,changed))))
(output/'results.json').write_text(json.dumps(dict(entry='598400',rows=rows,rng_unchanged=True,whole3MiB_changes_only_ordinary_counters=True,delayed_counters_not_refreshed=True,scope='Complete original42-city refresh function; actual containing whole-turn dispatch still separately required'),indent=2)+'\n');print('PASS',len(rows),'complete original global refresh observations; ordinary reset, delayed preserved, entire3MiB/RNG strict')
