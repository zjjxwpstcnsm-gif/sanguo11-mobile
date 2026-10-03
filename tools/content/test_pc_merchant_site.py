"""Original merchant building-kind and city-index admission, before actor/resource UI."""
import struct,unittest
import test_pc_city_action_costs as support
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_EIP

class MerchantSiteTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  support.CityActionCostsTest.setUpClass();cls.t=support.CityActionCostsTest();cls.d=cls.t.d
 def test_original_building_gate(self):
  from unicorn.x86_const import UC_X86_REG_ECX
  t=self.t;d=self.d;u=d.u
  # Two rejection branches: non-city type before lookup, invalid city index after.
  stops=[u.hook_add(UC_HOOK_CODE,lambda machine,address,size,user:machine.emu_stop(),begin=a,end=a) for a in [0x5cabfe,0x5caa07]]
  try:
   for native in range(87):
    building=d.root+0x89730+native*0x38;u.reg_write(UC_X86_REG_ECX,building);t.call(0x4880a0)
    actual_kind=0 if native<42 else 1 if native<52 else 2
    for kind in range(3):
     u.mem_write(building+8,struct.pack('<I',kind));before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4))
     u.reg_write(UC_X86_REG_ESI,building);u.reg_write(UC_X86_REG_ESP,d.stack);u.emu_start(0x5ca985,0x5ca9c9,count=10000)
     expected=0x5cabfe if kind else 0x5ca9c9 if native<42 else 0x5caa07
     self.assertEqual(expected,u.reg_read(UC_X86_REG_EIP),(native,actual_kind,kind))
     self.assertEqual(before,bytes(u.mem_read(0x7200000,0x300000)));self.assertEqual(rng,bytes(u.mem_read(0x8a5d44,4)))
  finally:
   for hook in stops:u.hook_del(hook)

if __name__=='__main__':unittest.main()
