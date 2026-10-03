"""Original date advance and bounded87-site action reset in the same turn dispatcher."""
import struct,unittest
import test_pc_city_action_costs as support
from inspect_pc_scenario_domains import GROUPS
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP

class MerchantTurnTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  support.CityActionCostsTest.setUpClass();cls.t=support.CityActionCostsTest();cls.d=cls.t.d
 def test_dispatcher_calls_and_date_advance(self):
  t=self.t;d=self.d;u=d.u
  # These are real direct calls, not an invented schedule. Both are on the
  # ordinary580xxx turn path; the reset has no month/season condition.
  for address,target in [(0x58051e,0x4a1180),(0x5805a8,0x590c30),(0x5805d0,0x59c330),(0x59c423,0x598630),(0x598650,0x487860)]:
   code=bytes(u.mem_read(address,5));self.assertEqual(0xe8,code[0]);self.assertEqual(target,address+5+struct.unpack('<i',code[1:])[0])
  self.assertEqual(bytes.fromhex('e91a3effff'),bytes(u.mem_read(0x487911,5)))
  for month in range(1,13):
   for day in [1,11,21]:
    for turn in [0,1,2,35,36]:
     u.mem_write(d.root+8,struct.pack('<III',190,month,day));u.mem_write(d.root+0x5c,struct.pack('<I',turn))
     before=bytearray(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));t.call(0x4a1180)
     struct.pack_into('<I',before,d.root+0x5c-0x7200000,turn+1)
     self.assertEqual(bytes(before),bytes(u.mem_read(0x7200000,0x300000)));self.assertEqual(rng,bytes(u.mem_read(0x8a5d44,4)))
     elapsed=(month-1)*30+day-1+(turn+1)*10
     for getter,expected in [(0x4824b0,190+elapsed//360),(0x4824f0,elapsed%360//30+1),(0x482530,elapsed%30+1)]:
      u.reg_write(UC_X86_REG_ECX,d.root);self.assertEqual(expected,t.call(getter),(month,day,turn,hex(getter)))

 def test_original_all_site_reset_and_merchant_readmission(self):
  t=self.t;d=self.d;u=d.u
  sites=[]
  for kind,base,stride,count,_,_ in GROUPS:
   if kind not in ('city','gate','port'):continue
   for index in range(count):
    native=index+({'city':0,'gate':42,'port':52}[kind]);building=d.root+0x89730+native*0x38
    u.reg_write(UC_X86_REG_ECX,building);t.call(0x4880a0);u.mem_write(building+8,struct.pack('<I',{'city':0,'gate':1,'port':2}[kind]))
    sites.append((d.root+base+index*stride,0xa4 if kind=='city' else 0x68,kind))
  self.assertEqual(87,len(sites))
  self.assertEqual(bytes.fromhex('c781a400000000000000c3'),bytes(u.mem_read(0x47b730,11)))
  self.assertEqual(bytes.fromhex('c7416800000000c3'),bytes(u.mem_read(0x48da10,8)))
  for flag in [0,2,0x10,0xffffffff]:
   for site,offset,kind in sites:u.mem_write(site+offset,struct.pack('<I',flag))
   before=bytearray(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4))
   u.reg_write(UC_X86_REG_ESP,d.stack);u.emu_start(0x598630,0x59865b,count=100000)
   self.assertEqual(0x59865b,u.reg_read(UC_X86_REG_EIP))
   for site,offset,kind in sites:struct.pack_into('<I',before,site+offset-0x7200000,0)
   self.assertEqual(bytes(before),bytes(u.mem_read(0x7200000,0x300000)));self.assertEqual(rng,bytes(u.mem_read(0x8a5d44,4)))
   for site,offset,kind in sites:
    if kind=='city':u.reg_write(UC_X86_REG_ECX,site);self.assertEqual(0,t.call(0x481310))

if __name__=='__main__':unittest.main()
