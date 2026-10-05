"""Original merchant merit call, named officer property, and exact mutation bounds."""
import struct, unittest
import test_pc_city_action_costs as support
from unicorn.x86_const import UC_X86_REG_ECX, UC_X86_REG_EDI, UC_X86_REG_ESI, UC_X86_REG_ESP, UC_X86_REG_EIP

class MerchantMeritTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  support.CityActionCostsTest.setUpClass(); cls.t=support.CityActionCostsTest(); cls.d=cls.t.d

 def test_named_property_and_original_commit_award(self):
  t=self.t; d=self.d; u=d.u; officer=d.root+0xc0bc
  u.reg_write(UC_X86_REG_ECX,officer); t.call(0x489f10)
  u.mem_write(officer+0xa0,struct.pack('<I',0)); self.assertEqual(1,t.call(0x47a630,officer))
  # Initialize the ORIGINAL officer descriptor table, then execute its name lookup.
  # The city/unit descriptor table at8a6d70 is a different property namespace.
  t.call(0x73ca80)
  u.reg_write(UC_X86_REG_ESI,24); u.reg_write(UC_X86_REG_ESP,d.stack)
  u.emu_start(0x4c86eb,0x4c86f6,count=100)
  self.assertEqual(0x4c86f6,u.reg_read(UC_X86_REG_EIP))
  self.assertEqual('功績',bytes(u.mem_read(u.reg_read(UC_X86_REG_ECX),32)).split(b'\0')[0].decode('big5'))
  # Property24 dispatches to the setter for officer+ae, not an inferred field name.
  index=bytes(u.mem_read(0x4a41f4+24-3,1))[0]
  self.assertEqual(0x4a3c9c,struct.unpack('<I',u.mem_read(0x4a412c+4*index,4))[0])
  self.assertEqual(bytes.fromhex('6a3257b95c899907e87bc0edff'),bytes(u.mem_read(0x5cacc8,13)))
  for merit in [0,1,49,50,100,59900,59949,59950,59951,59999,60000,65535]:
   u.mem_write(officer+0xae,struct.pack('<H',merit))
   before=bytearray(u.mem_read(0x7200000,0x300000)); rng=bytes(u.mem_read(0x8a5d44,4))
   u.reg_write(UC_X86_REG_EDI,officer); u.reg_write(UC_X86_REG_ESP,d.stack)
   u.emu_start(0x5cacc8,0x5cacd5,count=1000)
   self.assertEqual(0x5cacd5,u.reg_read(UC_X86_REG_EIP))
   struct.pack_into('<H',before,officer+0xae-0x7200000,min(60000,merit+50))
   self.assertEqual(bytes(before),bytes(u.mem_read(0x7200000,0x300000)))
   self.assertEqual(rng,bytes(u.mem_read(0x8a5d44,4)))

if __name__=='__main__': unittest.main()
