"""Original merchant quantity capacity, including the sell-side retained food unit."""
import struct,unittest
import test_pc_city_action_costs as support
from unicorn.x86_const import UC_X86_REG_ECX

class MerchantMaximumTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  support.CityActionCostsTest.setUpClass();cls.t=support.CityActionCostsTest();cls.d=cls.t.d
 def test_original_capacity_arithmetic_and_purity(self):
  t=self.t;d=self.d;u=d.u;building,city,_,_=t.actors();officer=d.root+0xc0bc
  u.reg_write(UC_X86_REG_ECX,officer);t.call(0x489f10);u.mem_write(officer+0xa0,struct.pack('<I',0));count=0;large=False;nonthousand=False
  for ability in [0,80,100,255]:
   u.mem_write(officer+0x173,bytes([ability]));factor=450-ability
   for rate in [1,30,50,70,255]:
    u.mem_write(city+0x7c,bytes([rate]))
    for gold,food in [(0,0),(0,1),(1,1),(9999,999),(50000,100000),(99999,999999),(100000,1000000),(100000,999999)]:
     u.mem_write(city+0x44,struct.pack('<II',gold,food))
     for buy in [0,1]:
      before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4))
      actual=t.call(0x5ca770,building,officer,buy)
      expected=min(gold*rate*400//(factor*10),1000000-food) if buy else min((100000-gold)*factor*rate//3200,max(0,food-1))
      expected=max(0,min(2147483647,expected));self.assertEqual(expected,actual,(ability,rate,gold,food,buy))
      self.assertEqual(before,bytes(u.mem_read(0x7200000,0x300000)));self.assertEqual(rng,bytes(u.mem_read(0x8a5d44,4)));count+=1
      large|=actual>20000;nonthousand|=actual%1000!=0
  self.assertEqual(320,count);self.assertTrue(large);self.assertTrue(nonthousand)
  self.assertEqual(0,t.call(0x5ca770,0,officer,1));self.assertEqual(0,t.call(0x5ca770,building,0,1))

 def test_original_command_quantity_validator(self):
  t=self.t;d=self.d;u=d.u;building,city,_,_=t.actors();officer=d.root+0xc0bc;command=d.stream+0xc00
  u.reg_write(UC_X86_REG_ECX,officer);t.call(0x489f10);u.mem_write(officer+0xa0,struct.pack('<I',0))
  # This validator is one component of admission. UI capacity separately prevents
  # overselling/overspending; do not turn its narrow checks into full permission.
  for food in [0,1,100000,999999,1000000]:
   u.mem_write(city+0x48,struct.pack('<I',food))
   for amount in [-1000001,-20001,-1000,-999,-1,0,1,999,1000,20001,1000000,1000001]:
    u.mem_write(command,struct.pack('<III',building,officer,amount&0xffffffff));u.reg_write(UC_X86_REG_ECX,command)
    before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));actual=t.call(0x5ca450)
    self.assertEqual(int(amount!=0 and food+amount<=1000000),actual,(food,amount));self.assertEqual(before,bytes(u.mem_read(0x7200000,0x300000)));self.assertEqual(rng,bytes(u.mem_read(0x8a5d44,4)))

if __name__=='__main__':unittest.main()
