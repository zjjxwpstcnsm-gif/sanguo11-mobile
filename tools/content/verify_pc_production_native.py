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
from pc_original_pe_data import load_original_data
from unicorn import UC_HOOK_MEM_WRITE
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP
s.CityActionCostsTest.setUpClass();t=s.CityActionCostsTest();d=t.d;u=d.u;building,city,district,facility=t.actors();load_original_data(u)
force=d.root+0x7af8;u.reg_write(UC_X86_REG_ECX,force);t.call(0x481830);u.mem_write(force+4,struct.pack('<i',0));u.mem_write(force+0x60,struct.pack('<i',-1));u.mem_write(city+0x38,struct.pack('<i',0));officers=[d.root+0xc0bc+n*0x190 for n in range(3)];cmd=d.stream+0xc00
for o in officers:
 u.reg_write(UC_X86_REG_ECX,o);t.call(0x489f10)
 for offset,value in [(0x9c,0),(0xa0,0),(0xa4,-1),(0x60,-1),(0x15c,0),(0xe8,-1)]:u.mem_write(o+offset,struct.pack('<i',value))
 u.mem_write(o+0xd0,struct.pack('<5i',*([-1]*5)))
baseline=bytes(u.mem_read(0x7200000,0x300000));writes=[];capture=False
def observe(machine,access,address,size,value,user):
 if capture and not d.stack-0x10000<=address<d.stack+0x10000 and address!=0:writes.append((address,size))
u.hook_add(UC_HOOK_MEM_WRITE,observe);rows=[]
for values in [(80,),(80,50),(80,50,20)]:
 for skill in [-1,80,81]:
  for item in [1,2,3,4]:
   for tier in [1,2,3]:
    for xp in [0,99,2999]:
     for injury in [0,1,3]:
      u.mem_write(0x7200000,baseline);active=officers[:len(values)];u.mem_write(cmd,struct.pack('<5I',building,*(active+[0]*(3-len(active))),item));u.mem_write(city+0x44,struct.pack('<I',10000));u.mem_write(district+0x2c,b'\x3c');u.mem_write(city+0x86,b'\0'*6)
      u.mem_write(city+0xe8,struct.pack('<i',1));u.mem_write(city+0xf4,struct.pack('<I',facility));u.mem_write(facility+8,struct.pack('<i',([34,56,57] if item!=4 else [35,58,59])[tier-1]));u.mem_write(facility+0x14,struct.pack('<i',1));u.mem_write(city+(0xaa if item==4 else 0xa9),b'\1')
      for n,(o,value) in enumerate(zip(active,values)):
       u.mem_write(o+0xc8,bytes([value]*5));u.mem_write(o+0x12a,struct.pack('<5H',0,0,xp,0,0));u.mem_write(o+0xae,struct.pack('<H',59999));u.mem_write(o+0x15c,struct.pack('<i',injury));u.mem_write(o+0xe8,struct.pack('<i',skill if n==len(active)-1 else -1));u.reg_write(UC_X86_REG_ECX,o);t.call(0x48a2d0)
      before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));writes.clear();capture=True;u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<II',d.stop,cmd));u.emu_start(0x5c65b0,0x5c67b8,count=100000);capture=False
      assert u.reg_read(UC_X86_REG_EIP)==0x5c67b8;assert rng==bytes(u.mem_read(0x8a5d44,4));assert all(0x7200000<=a and a+n<=0x7500000 for a,n in writes),[(hex(a),n) for a,n in writes if not 0x7200000<=a<0x7500000]
      currents=[bytes(u.mem_read(o+0x172,1))[0] for o in active];scale=100 if injury==0 else 80 if injury==1 else 30;expected_currents=[max(1,min(100,value+min(3000,xp+2)//100)*scale//100) for value in values];assert currents==expected_currents
      quantity=(max(currents)+sum(currents)+200)*5
      if skill==80 and item<=3 or skill==81 and item==4:quantity*=2
      multiplier=struct.unpack('<f',struct.pack('<f',[1,1.2,1.5][tier-1]))[0];quantity=int(quantity*multiplier)
      u.reg_write(UC_X86_REG_ECX,city);stock=t.call(0x47b350,item);assert stock==quantity,(values,skill,item,tier,xp,injury,stock,quantity)
      assert struct.unpack('<I',u.mem_read(city+0x44,4))[0]==9300 and bytes(u.mem_read(district+0x2c,1))==b'\x28' and bytes(u.mem_read(city+(0xaa if item==4 else 0xa9),1))==b'\0'
      assert all(struct.unpack('<H',u.mem_read(o+0x12e,2))[0]==min(3000,xp+2) and struct.unpack('<H',u.mem_read(o+0xae,2))[0]==60000 for o in active)
      after=bytes(u.mem_read(0x7200000,0x300000));allowed=[(city,city+0x248),(district,district+0x50),(force,force+0x12c),(d.root+0x1cc,d.root+0x1d0)]+[(o,o+0x190) for o in active];changed=[i for i,(a,b) in enumerate(zip(before,after)) if a!=b];assert all(any(lo<=0x7200000+i<hi for lo,hi in allowed) for i in changed)
      if len(rows)%108==0:print('PROGRESS',len(rows),flush=True)
      rows.append(dict(base=list(values),skill_holder=len(active)-1,skill=skill,item=item,facility_tier=tier,xp_before=xp,injury=injury,current_after=currents,quantity=stock,gold_after=9300,ap_after=40,facility_remaining=0,all_changed_addresses=[hex(0x7200000+i) for i in changed]))
(output/'native-results.json').write_text(json.dumps(dict(scope='972 original immediate post-admission production slices through original XP,merit,acted,quantity,inventory,force,factory,gold and20AP; before presentation; original admission/UI/runtime startup not executed',entry='5c65b0',stop='5c67b8',rng_unchanged=True,all_writes_within_rule_domain=True,unknown_prefix_counter='root+1cc exact observed addresses retained; semantic identity pending',rows=rows),indent=2)+'\n');print('PASS',len(rows),'original post-admission rule slices; all writes in3MiB domain; XP/current/merit/stock/gold/AP/facility/RNG asserted; full admission still pending')

(output/'pc-production-native.tsv').write_text('base\tskill\titem\ttier\txp\tinjury\tquantity\tcurrent\n'+''.join(','.join(map(str,r['base']))+'\t'+str(r['skill'])+'\t'+str(r['item'])+'\t'+str(r['facility_tier'])+'\t'+str(r['xp_before'])+'\t'+str(r['injury'])+'\t'+str(r['quantity'])+'\t'+','.join(map(str,r['current_after']))+'\n' for r in rows))
