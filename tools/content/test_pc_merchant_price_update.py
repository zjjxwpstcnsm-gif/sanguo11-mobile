"""Execute original price update/RNG, not replacement callbacks or a hooked quote."""
import struct,unittest
import test_pc_city_action_costs as support
from unicorn.x86_const import UC_X86_REG_ECX

class MerchantPriceUpdateTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  support.CityActionCostsTest.setUpClass();cls.t=support.CityActionCostsTest();cls.d=cls.t.d
 def advance(self,state):return (state*0x6c078965+0x3039)&0xffffffff
 def test_original_rng_edges(self):
  t=self.t;u=self.d.u
  for seed in [0,1,42,0x7fffffff,0xffffffff]:
   for bound in [-1,0,1,2,3,100,65535,65536,65537,2147483647]:
    u.mem_write(0x8a5d44,struct.pack('<I',seed));actual=t.call(0x472150,bound&0xffffffff)&0xffff
    after=self.advance(seed) if bound>=2 and bound&0xffff else seed
    expected=(after>>16)%(bound&0xffff) if after!=seed else 0
    self.assertEqual((expected,after),(actual,struct.unpack('<I',u.mem_read(0x8a5d44,4))[0]))
   for chance in [-1,0,1,40,50,100,101]:
    u.mem_write(0x8a5d44,struct.pack('<I',seed));actual=t.call(0x4721d0,chance&0xffffffff)
    after=self.advance(seed) if chance>0 else seed
    self.assertEqual((int(chance>0 and (after>>16)%100<chance),after),(actual,struct.unpack('<I',u.mem_read(0x8a5d44,4))[0]))
 def update(self,month,initial,rate,flags,seed,expected,expected_state):
  t=self.t;d=self.d;u=d.u;city=d.root+0x1d8
  u.mem_write(city+0x7c,bytes([rate]));u.mem_write(city+0x9c,struct.pack('<I',flags));u.mem_write(0x8a5d44,struct.pack('<I',seed))
  before=bytearray(u.mem_read(0x7200000,0x300000));u.reg_write(UC_X86_REG_ECX,0x799895c);t.call(0x4b3c60,city,month,initial)
  before[city+0x7c-0x7200000]=expected
  self.assertEqual(bytes(before),bytes(u.mem_read(0x7200000,0x300000)),(month,initial,rate,flags,seed))
  self.assertEqual(expected_state,struct.unpack('<I',u.mem_read(0x8a5d44,4))[0])
 def test_initial_month_table_and_invalid_months(self):
  table=[(50,10,2),(40,10,3),(40,10,3),(40,10,2),(40,10,2),(30,10,3),(60,10,2),(50,10,3),(50,10,3),(50,10,3),(50,10,2),(50,10,2)]
  self.assertEqual(tuple(x for row in table for x in row),struct.unpack('<36i',self.d.u.mem_read(0x7e84b8,144)))
  for month in range(14):
   for seed in range(16):
    if 1<=month<=12:
     base,step,bound=table[month-1];after=self.advance(seed);rate=base+step*((after>>16)%bound)
    else:after=seed;rate=50
    self.update(month,1,50,0xffffffff,seed,rate,after)
 def test_monthly_branches_and_exact_draw_consumption(self):
  observed=set()
  for rate in [0,29,30,40,49,50,51,60,70,71,255]:
   for flags in [0,1,2,3,4,5,8,0xffffffff]:
    for seed in range(32):
     state=self.advance(seed);value=state>>16;draws=1
     if flags&3:result=30+10*(value%2)
     elif flags&4:result=60+10*(value%2)
     elif rate==30:result=30 if value%100<50 else 40
     elif rate==70:result=60 if value%100<50 else 70
     elif rate<=40 or rate>=60:
      direction=10 if rate<=40 else -10
      if value%100<40:result=rate+direction
      else:
       state=self.advance(state);draws=2;result=rate-direction if (state>>16)%100<40 else rate
     else:result=rate+10*(value%3)-10
     result=max(30,min(70,result));observed.add(draws)
     self.update(1,0,rate,flags,seed,result,state)
  self.assertEqual({1,2},observed)

if __name__=='__main__':unittest.main()
