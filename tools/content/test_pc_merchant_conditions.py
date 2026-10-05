"""Tie original localized property names to the city's actual price condition bits."""
import struct,unittest
import test_pc_city_action_costs as support
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EDI,UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_EIP

class MerchantConditionsTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  support.CityActionCostsTest.setUpClass();cls.t=support.CityActionCostsTest();cls.d=cls.t.d
 def test_original_named_property_set_get(self):
  d=self.d;u=d.u;city=d.root+0x1d8
  for index,name in enumerate(['瘟疫','蝗災','豐收']):
   property_id=0x9a+index
   before=bytes(u.mem_read(0x7200000,0x300000));u.reg_write(UC_X86_REG_ESI,property_id);u.reg_write(UC_X86_REG_ESP,d.stack)
   u.emu_start(0x4c09ef,0x4c0a1f,count=1000);self.assertEqual(0x4c0a1f,u.reg_read(UC_X86_REG_EIP))
   pointer=u.reg_read(UC_X86_REG_ESI);self.assertEqual(name,bytes(u.mem_read(pointer,32)).split(b'\0')[0].decode('big5'));self.assertEqual(before,bytes(u.mem_read(0x7200000,0x300000)))
   for flags in [0,1,2,3,4,7,0x80000000,0xffffffff]:
    for value in [0,1]:
     u.mem_write(city+0x9c,struct.pack('<I',flags));before=bytearray(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4))
     u.reg_write(UC_X86_REG_EAX,property_id);u.reg_write(UC_X86_REG_ESI,city);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack+0x14,struct.pack('<I',value))
     u.emu_start(0x4a3abd,0x4a3add,count=1000);self.assertEqual(0x4a3add,u.reg_read(UC_X86_REG_EIP))
     expected=flags|(1<<index) if value else flags&~(1<<index);struct.pack_into('<I',before,city+0x9c-0x7200000,expected)
     self.assertEqual(bytes(before),bytes(u.mem_read(0x7200000,0x300000)))
     u.reg_write(UC_X86_REG_EAX,property_id);u.reg_write(UC_X86_REG_ESI,city);u.reg_write(UC_X86_REG_ESP,d.stack)
     u.emu_start(0x4c0cd3,0x4c0cf0,count=1000);self.assertEqual(0x4c0cf0,u.reg_read(UC_X86_REG_EIP));self.assertEqual(value,u.reg_read(UC_X86_REG_EDI))
     self.assertEqual(bytes(before),bytes(u.mem_read(0x7200000,0x300000)));self.assertEqual(rng,bytes(u.mem_read(0x8a5d44,4)))

if __name__=='__main__':unittest.main()
