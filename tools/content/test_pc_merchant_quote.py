"""Bounded original5ca620 arithmetic. City7c update and full merchant admission remain unknown."""
import struct,unittest
import test_pc_city_action_costs as support
from inspect_pc_scenario_domains import GROUPS
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESI,UC_X86_REG_EDI,UC_X86_REG_EBX,UC_X86_REG_ESP,UC_X86_REG_EIP

class MerchantQuoteTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  support.CityActionCostsTest.setUpClass();cls.helper=support.CityActionCostsTest();cls.d=cls.helper.d
 def test_signed_quote_and_world_purity(self):
  t=self.helper;d=self.d;u=d.u;building,city,_,_=t.actors();officer=d.root+0xc0bc
  u.reg_write(UC_X86_REG_ECX,officer);t.call(0x489f10);u.mem_write(officer+0xa0,struct.pack('<I',0))
  self.assertEqual(bytes.fromhex('8a8173010000c3'),bytes(u.mem_read(0x4890a0,7)))
  count=0
  for ability173 in [0,1,50,80,100,255]:
   u.mem_write(officer+0x173,bytes([ability173]))
   for rate7c in [1,10,25,50,255]:
    u.mem_write(city+0x7c,bytes([rate7c]))
    for amount in [0,1,999,1000,20000,1000000,2147483647,-1,-999,-1000,-20000,-1000000,-2147483647]:
     before=bytes(u.mem_read(0x7200000,0x300000));actual=t.call(0x5ca620,building,officer,amount&0xffffffff);actual=actual if actual<2**31 else actual-2**32
     expected=amount*(450-ability173)*10//(rate7c*400) if amount>=0 else -(abs(amount)*3200//((450-ability173)*rate7c))
     expected=max(-2**31,min(2**31-1,expected));self.assertEqual(expected,actual,(ability173,rate7c,amount));self.assertEqual(before,bytes(u.mem_read(0x7200000,0x300000)));count+=1
  self.assertEqual(390,count)
  self.assertEqual(0,t.call(0x5ca620,building,0,1000))
  self.assertEqual(0,t.call(0x5ca620,0,officer,1000))

 def test_original_commit_resource_segment(self):
  # Start after actor action/experience/merit effects; this is deliberately
  # NOT a full-command admission test. Execute original flag/quote/resources/AP.
  t=self.helper;d=self.d;u=d.u;building,city,district,_=t.actors();officer=d.root+0xc0bc;command=d.stream+0xc00
  u.reg_write(UC_X86_REG_ECX,officer);t.call(0x489f10);u.mem_write(officer+0xa0,struct.pack('<I',0))
  u.mem_write(officer+0x173,bytes([80]));u.mem_write(city+0x7c,bytes([50]))
  for amount in [-20000,-1000,0,1000,20000]:
   for gold,food in [(50000,50000),(0,0),(99999,499999),(99999,999999),(100,100)]:
    for flag in [0,4,255]:
     u.mem_write(command,struct.pack('<III',building,officer,amount&0xffffffff))
     u.mem_write(city+0x44,struct.pack('<II',gold,food));u.mem_write(city+0xa4,bytes([flag]));u.mem_write(district+0x2c,bytes([60]))
     before=bytearray(u.mem_read(0x7200000,0x300000))
     quote=amount*370*10//20000 if amount>=0 else -(abs(amount)*3200//18500)
     u.reg_write(UC_X86_REG_ESI,command);u.reg_write(UC_X86_REG_EDI,officer);u.reg_write(UC_X86_REG_EBX,city);u.reg_write(UC_X86_REG_ESP,d.stack)
     u.emu_start(0x5cacd5,0x5cad20,count=10000);self.assertEqual(0x5cad20,u.reg_read(UC_X86_REG_EIP))
     offset=city-0x7200000
     struct.pack_into('<II',before,offset+0x44,max(0,min(100000,gold-quote)),max(0,min(1000000,food+amount)))
     before[offset+0xa4]=flag|2;before[district+0x2c-0x7200000]=40
     self.assertEqual(bytes(before),bytes(u.mem_read(0x7200000,0x300000)),(amount,gold,food,flag))

 def test_original_city_capacity_getters(self):
  t=self.helper;d=self.d;u=d.u
  _,base,stride,count,_,_=next(g for g in GROUPS if g[0]=='city')
  self.assertEqual((0x1d8,0x248,42),(base,stride,count))
  for index in range(count):
   building=d.root+0x89730+index*0x38;city=d.root+base+index*stride
   u.reg_write(UC_X86_REG_ECX,building);t.call(0x4880a0);u.mem_write(building+8,struct.pack('<I',0))
   self.assertEqual(1,t.call(0x47a630,city))
   for getter,expected in [(0x486d30,100000),(0x486ea0,1000000)]:
    before=bytes(u.mem_read(0x7200000,0x300000));u.reg_write(UC_X86_REG_ECX,building)
    self.assertEqual(expected,t.call(getter),(index,hex(getter)));self.assertEqual(before,bytes(u.mem_read(0x7200000,0x300000)))

 def test_original_merchant_used_flag_admission(self):
  t=self.helper;d=self.d;u=d.u;_,city,_,_=t.actors()
  # This is the real bit guard after AP admission, before resource checks.
  # The two stop boundaries do not replace the original getter/branch.
  boundary=u.hook_add(UC_HOOK_CODE,lambda machine,address,size,user:machine.emu_stop(),begin=0x5cab57,end=0x5cab57)
  try:
   for flag in range(256):
    u.mem_write(city+0xa4,bytes([flag]));before=bytes(u.mem_read(0x7200000,0x300000))
    u.reg_write(UC_X86_REG_EBX,city);u.reg_write(UC_X86_REG_ESP,d.stack)
    u.emu_start(0x5caaf0,0x5caafb,count=1000)
    self.assertEqual(0x5caafb if flag&2 else 0x5cab57,u.reg_read(UC_X86_REG_EIP),flag)
    self.assertEqual(before,bytes(u.mem_read(0x7200000,0x300000)))
  finally:u.hook_del(boundary)

if __name__=='__main__':unittest.main()
