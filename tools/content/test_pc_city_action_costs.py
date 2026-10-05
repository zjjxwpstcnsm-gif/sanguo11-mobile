"""Bounded original city action AP debit; no Wine, fake validators or PC writes."""
import struct,unittest
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EBX,UC_X86_REG_EBP,UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP,UC_X86_REG_ESI,UC_X86_REG_EDI
from inspect_pc_scenario_tail import NativeTailDecoder

class CityActionCostsTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  root=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版')
  cls.d=NativeTailDecoder((root/'san11pk.exe').read_bytes());cls.d.decode_tail((root/'Media/scenario/Scenario.s11').read_bytes(),True)
 def call(self,address,*args):
  d=self.d;d.u.reg_write(UC_X86_REG_ESP,d.stack);d.u.mem_write(d.stack,struct.pack('<'+'I'*(len(args)+1),d.stop,*args));d.u.emu_start(address,d.stop,count=10000)
  self.assertEqual(d.stop,d.u.reg_read(UC_X86_REG_EIP));return d.u.reg_read(UC_X86_REG_EAX)
 def actors(self):
  d=self.d;u=d.u;building=d.root+0x89730;city=d.root+0x1d8;district=d.root+0xb20c;facility=building+87*0x38
  for p in (building,facility):u.reg_write(UC_X86_REG_ECX,p);self.call(0x4880a0)
  for p,n in [(building+8,0),(city+0x38,0),(district+4,0),(facility+8,41),(facility+0x14,1)]:u.mem_write(p,struct.pack('<I',n))
  u.mem_write(city+0xf4,b'\0'*(30*8))
  for p in (building,city,district,facility):self.assertEqual(1,self.call(0x47a630,p))
  return building,city,district,facility
 def test_named_command_table(self):
  u=self.d.u
  for index,name in [(0,'開發'),(1,'徵兵'),(2,'生產'),(3,'巡察'),(4,'商人'),(6,'訓練'),(11,'探索人材'),(13,'褒賞')]:
   # Execute original bounds/lookup, stop before its presentation string allocation.
   u.reg_write(UC_X86_REG_EAX,index);u.emu_start(0x48f1a0,0x48f1b0,count=10)
   pointer=u.reg_read(UC_X86_REG_EAX);self.assertEqual(name,bytes(u.mem_read(pointer,32)).split(b'\0',1)[0].decode('big5'))
 def test_real_recruit_patrol_debit(self):
  d=self.d;u=d.u;building,city,district,facility=self.actors();command=d.stream+0xc00;frame=d.stream+0xd00
  u.mem_write(command,struct.pack('<I',building));u.mem_write(frame+8,struct.pack('<I',command))
  for entry,code in [(0x5c3c97,1),(0x5cbf86,3)]:
   for ap in [0,19,20,21,60,255]:
    u.mem_write(district+0x2c,bytes([ap]));before=bytearray(u.mem_read(0x7200000,0x300000));u.reg_write(UC_X86_REG_EBP,frame);u.reg_write(UC_X86_REG_ESP,d.stack)
    u.emu_start(entry,0x5b9387,count=10000);self.assertEqual(0x5b9387,u.reg_read(UC_X86_REG_EIP))
    before[district+0x2c-0x7200000]=max(0,ap-20);self.assertEqual(bytes(before),bytes(u.mem_read(0x7200000,0x300000)),(code,ap))
 def test_real_production_and_merchant_debit(self):
  d=self.d;u=d.u;building,city,district,facility=self.actors();command=d.stream+0xc00;frame=d.stream+0xd00
  u.mem_write(command,struct.pack('<I',building));u.mem_write(frame+8,struct.pack('<I',command))
  for entry,raw in [(0x5c67a4,'8b75088b0e6a146a0251b990157709e8882bffff'),(0x5c7119,'8b0f6a146a0251b990157709e81622ffff'),(0x5cad0f,'8b066a146a0450b990157709e820e6feff')]:
   self.assertEqual(raw,bytes(u.mem_read(entry,len(raw)//2)).hex())
   for ap in [0,9,10,19,20,21,60,255]:
    u.mem_write(district+0x2c,bytes([ap]));before=bytearray(u.mem_read(0x7200000,0x300000))
    u.reg_write(UC_X86_REG_EBP,frame);u.reg_write(UC_X86_REG_ESI,command);u.reg_write(UC_X86_REG_EDI,command);u.reg_write(UC_X86_REG_ESP,d.stack)
    u.emu_start(entry,0x5b9387,count=10000);self.assertEqual(0x5b9387,u.reg_read(UC_X86_REG_EIP))
    before[district+0x2c-0x7200000]=max(0,ap-20);self.assertEqual(bytes(before),bytes(u.mem_read(0x7200000,0x300000)),(entry,ap))
 def test_merchant_original_ap_admission_branch(self):
  d=self.d;u=d.u;building,city,district,_=self.actors()
  self.assertEqual('807f2c140f82e5000000',bytes(u.mem_read(0x5caae6,10)).hex())
  stop=u.hook_add(UC_HOOK_CODE,lambda machine,address,size,user:machine.emu_stop(),begin=0x5cabd5,end=0x5cabd5)
  try:
   for ap in [0,9,10,19,20,21,60,255]:
    u.mem_write(district+0x2c,bytes([ap]));before=bytes(u.mem_read(0x7200000,0x300000));u.reg_write(UC_X86_REG_ESI,building);u.reg_write(UC_X86_REG_EBX,city);u.reg_write(UC_X86_REG_EBP,0);u.reg_write(UC_X86_REG_ESP,d.stack)
    u.emu_start(0x5caac0,0x5caaf0,count=10000);self.assertEqual(0x5caaf0 if ap>=20 else 0x5cabd5,u.reg_read(UC_X86_REG_EIP));self.assertEqual(before,bytes(u.mem_read(0x7200000,0x300000)))
  finally:u.hook_del(stop)
 def test_real_training_facility_discount(self):
  d=self.d;u=d.u;building,city,district,facility=self.actors();command=d.stream+0xc00;u.mem_write(command,struct.pack('<I',building))
  for present,field14 in [(False,0),(True,0),(True,1)]:
   u.mem_write(city+0xf4,struct.pack('<I',facility if present else 0));u.mem_write(facility+0x14,struct.pack('<I',field14))
   self.assertEqual(int(present and field14),self.call(0x49dab0,city,41))
   for ap in [0,9,10,19,20,21,60,255]:
    u.mem_write(district+0x2c,bytes([ap]));before=bytearray(u.mem_read(0x7200000,0x300000));u.reg_write(UC_X86_REG_EBX,command);u.reg_write(UC_X86_REG_ESP,d.stack)
    u.emu_start(0x5c43ca,0x5b9387,count=10000);self.assertEqual(0x5b9387,u.reg_read(UC_X86_REG_EIP))
    cost=10 if present and field14 else 20;before[district+0x2c-0x7200000]=max(0,ap-cost)
    self.assertEqual(bytes(before),bytes(u.mem_read(0x7200000,0x300000)),(present,field14,ap))

if __name__=='__main__':unittest.main()
