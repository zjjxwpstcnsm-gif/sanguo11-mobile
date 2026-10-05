from pathlib import Path
import argparse
parser=argparse.ArgumentParser(description='Execute bounded original production code; PC read only; no Wine or full admission claim')
parser.add_argument('--output',required=True,type=Path)
output=parser.parse_args().output.resolve()
installation=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve()
if output==installation or installation in output.parents:raise ValueError('Output must be outside PC installation')
output.mkdir(parents=True,exist_ok=False)

import sys,struct,json
sys.path.insert(0,str(Path('tools/content').resolve()))
import test_pc_city_action_costs as s
from unicorn.x86_const import UC_X86_REG_ECX
s.CityActionCostsTest.setUpClass();t=s.CityActionCostsTest();d=t.d;u=d.u;t.actors();officers=[d.root+0xc0bc+n*0x190 for n in range(3)];array=d.stream+0xc00
for o in officers:
 u.reg_write(UC_X86_REG_ECX,o);t.call(0x489f10)
 for offset,value in [(0x9c,0),(0xa0,0),(0xa4,-1),(0x60,-1),(0xe8,-1)]:u.mem_write(o+offset,struct.pack('<i',value))
 u.mem_write(o+0xd0,struct.pack('<5i',*([-1]*5)));u.mem_write(o+0x12a,b'\0'*10)
rows=[]
for values in [(1,),(50,),(80,),(100,),(80,50),(80,50,20),(100,100,100)]:
 active=officers[:len(values)];u.mem_write(array,struct.pack('<3I',*(active+[0]*(3-len(active)))))
 for injury in [0,1,2,3]:
  for o,value in zip(active,values):u.mem_write(o+0xc8,bytes([value]*5));u.mem_write(o+0x15c,struct.pack('<i',injury));u.reg_write(UC_X86_REG_ECX,o);t.call(0x48a2d0)
  current=[bytes(u.mem_read(o+0x172,1))[0] for o in active]
  for skill in [-1,82,83]:
   for o in officers:u.mem_write(o+0xe8,struct.pack('<i',-1))
   u.mem_write(active[-1]+0xe8,struct.pack('<i',skill))
   for item in range(5,12):
    before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));actual=t.call(0x5c6ca0,array,item);expected=9 if item==9 else 10-max(24,max(current)+sum(current)-200)//24
    if item<=8 and skill==82 or item>=10 and skill==83:expected//=2
    assert actual==expected,(values,current,item,skill,actual,expected);assert before==bytes(u.mem_read(0x7200000,0x300000)) and rng==bytes(u.mem_read(0x8a5d44,4));rows.append(dict(base=list(values),current=current,injury=injury,item=item,skill=skill,holder=len(active)-1,native_counter=actual))
(output/'native-results.json').write_text(json.dumps(dict(scope='588 original helper calls complete3MiB/RNG, no native ship facility45; sourcecounter not yet full completed-game duration',function='5c6ca0',formula='Item9 keepszero current aggregate; otherwise10-floor(max(24,max current INT+sum current INT-200)/24);skill82 for5..8 or83 for10..11 halves',rows=rows,actual_scheduling_pending=True),indent=2)+'\n');print('PASS',len(rows),'original delay counter helpers, complete3MiB/RNG; actual tick/completion pending')

(output/'pc-delayed-counter-native.tsv').write_text('current\titem\tskill\tcounter\n'+''.join(','.join(map(str,r['current']))+'\t'+str(r['item'])+'\t'+str(r['skill'])+'\t'+str(r['native_counter'])+'\n' for r in rows))
