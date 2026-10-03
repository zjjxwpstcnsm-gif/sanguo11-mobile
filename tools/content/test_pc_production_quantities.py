"""Original quantity helper observations, not full command or Android parity.
Uses the installed verified PC EXE read-only and the existing native tail decoder.
Native item/skill names, facilities/difficulty and task timing remain separate.
"""
import struct, unittest
import test_pc_city_action_costs as support
from unicorn.x86_const import UC_X86_REG_ECX

class ProductionQuantityTest(unittest.TestCase):
 def test_single_officer(self):
  support.CityActionCostsTest.setUpClass();t=support.CityActionCostsTest();d=t.d;u=d.u;building,city,_,_=t.actors();officer=d.root+0xc0bc;u.reg_write(UC_X86_REG_ECX,officer);t.call(0x489f10)
  for offset,value in [(0x9c,0),(0xa0,0),(0xa4,-1),(0x60,-1),(0x15c,0)]:u.mem_write(officer+offset,struct.pack('<i',value))
  u.mem_write(officer+0xd0,struct.pack('<5i',*([-1]*5)));u.mem_write(officer+0x12a,b'\0'*10);array=d.stream+0xc00;u.mem_write(array,struct.pack('<3I',officer,0,0));rows=[]
  for ability in (1,50,80,99,100):
   u.mem_write(officer+0xc8,bytes([ability]*5));u.reg_write(UC_X86_REG_ECX,officer);t.call(0x48a2d0)
   for item in range(12):
    before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));value=t.call(0x5c63d0,city,array,item);assert before==bytes(u.mem_read(0x7200000,0x300000));assert rng==bytes(u.mem_read(0x8a5d44,4));rows.append(dict(current=ability,item=item,quantity=value))
  print('PASS '+self._testMethodName+' observations='+str(len(rows))+' original helper, complete3MiB/RNG; full command and Android integration pending')
 def test_three_officers_and_numeric_skills(self):
  support.CityActionCostsTest.setUpClass();t=support.CityActionCostsTest();d=t.d;u=d.u;building,city,_,_=t.actors();officers=[d.root+0xc0bc+n*0x190 for n in range(3)];array=d.stream+0xc00;rows=[]
  for o in officers:
   u.reg_write(UC_X86_REG_ECX,o);t.call(0x489f10)
   for offset,value in [(0x9c,0),(0xa0,0),(0xa4,-1),(0x60,-1),(0x15c,0),(0xe8,-1)]:u.mem_write(o+offset,struct.pack('<i',value))
   u.mem_write(o+0xd0,struct.pack('<5i',*([-1]*5)));u.mem_write(o+0x12a,b'\0'*10)
  for values in [(80,),(80,50),(80,50,20),(1,1,1),(100,100,100),(99,98,1)]:
   active=officers[:len(values)];u.mem_write(array,struct.pack('<3I',*(active+[0]*(3-len(active)))))
   for o,v in zip(active,values):u.mem_write(o+0xc8,bytes([v]*5));u.reg_write(UC_X86_REG_ECX,o);t.call(0x48a2d0)
   for skill in (-1,80,81):
    for holder in range(len(active)):
     for o in officers:u.mem_write(o+0xe8,struct.pack('<i',-1))
     u.mem_write(active[holder]+0xe8,struct.pack('<i',skill))
     for item in range(12):
      before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));actual=t.call(0x5c63d0,city,array,item)
      expected=(max(values)+sum(values)+200)*5 if item<=4 else 1
      if item in (1,2,3) and skill==80 or item==4 and skill==81:expected*=2
      assert actual==expected,(values,skill,holder,item,actual,expected);assert before==bytes(u.mem_read(0x7200000,0x300000));assert rng==bytes(u.mem_read(0x8a5d44,4));rows.append(dict(current=list(values),skill=skill,holder=holder,item=item,quantity=actual))
  print('PASS '+self._testMethodName+' observations='+str(len(rows))+' original helper, complete3MiB/RNG; full command and Android integration pending')
 def test_distinct_current_stat_and_injury(self):
  support.CityActionCostsTest.setUpClass();t=support.CityActionCostsTest();d=t.d;u=d.u;_,city,_,_=t.actors();o=d.root+0xc0bc;u.reg_write(UC_X86_REG_ECX,o);t.call(0x489f10)
  for offset,value in [(0x9c,0),(0xa0,0),(0xa4,-1),(0x60,-1),(0xe8,-1)]:u.mem_write(o+offset,struct.pack('<i',value))
  u.mem_write(o+0xd0,struct.pack('<5i',*([-1]*5)));array=d.stream+0xc00;u.mem_write(array,struct.pack('<3I',o,0,0));rows=[]
  for base in [(11,22,33,44,55),(99,22,33,44,55),(11,99,33,44,55),(11,22,33,99,55),(11,22,33,44,99),(11,22,1,44,55),(11,22,100,44,55)]:
   for injury in (-1,0,1,2,3):
    u.mem_write(o+0xc8,bytes(base));u.mem_write(o+0x12a,b'\0'*10);u.mem_write(o+0x15c,struct.pack('<i',injury));u.reg_write(UC_X86_REG_ECX,o);t.call(0x48a2d0);current=bytes(u.mem_read(o+0x170,5));before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));amount=t.call(0x5c63d0,city,array,1);assert amount==(2*current[2]+200)*5,(base,injury,current,amount);assert before==bytes(u.mem_read(0x7200000,0x300000));assert rng==bytes(u.mem_read(0x8a5d44,4));rows.append(dict(base=list(base),injury=injury,current=list(current),quantity=amount))
  print('PASS '+self._testMethodName+' observations='+str(len(rows))+' original helper, complete3MiB/RNG; full command and Android integration pending')

if __name__=='__main__':unittest.main()
