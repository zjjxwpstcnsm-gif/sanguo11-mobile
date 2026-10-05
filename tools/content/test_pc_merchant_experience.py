"""Original experience->ability refresh->quote->resources/AP ordering.

Bounded after actor action marking and before presentation. Full ordinary command
admission, age curves, ranks, spouse bonuses and mentoring remain separate.
"""
import struct, unittest
import test_pc_city_action_costs as support
from unicorn.x86_const import UC_X86_REG_EBX,UC_X86_REG_ECX,UC_X86_REG_EDI,UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_EIP

class MerchantExperienceTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  support.CityActionCostsTest.setUpClass();cls.t=support.CityActionCostsTest();cls.d=cls.t.d

 def test_named_injury_and_current_ability_cache(self):
  t=self.t;d=self.d;u=d.u;officer=d.root+0xc0bc
  u.reg_write(UC_X86_REG_ECX,officer);t.call(0x489f10)
  u.mem_write(officer+0xa0,struct.pack('<I',0));u.mem_write(officer+0xa4,struct.pack('<i',-1));u.mem_write(officer+0x60,struct.pack('<i',-1))
  u.mem_write(officer+0xd0,struct.pack('<5i',*([-1]*5)))
  t.call(0x73ca80)
  for prop,name,target in [(15,'配偶',0x4a3c13),(21,'官職',0x4a3c69),(33,'政治經驗',0x4a3d45),(57,'傷病',0x4a3d6b)]:
   u.reg_write(UC_X86_REG_ESI,prop);u.reg_write(UC_X86_REG_ESP,d.stack);u.emu_start(0x4c86eb,0x4c86f6,count=100)
   self.assertEqual(name,bytes(u.mem_read(u.reg_read(UC_X86_REG_ECX),32)).split(b'\0')[0].decode('big5'))
   index=bytes(u.mem_read(0x4a41f4+prop-3,1))[0];self.assertEqual(target,struct.unpack('<I',u.mem_read(0x4a412c+4*index,4))[0])
  for base in (1,50,80,99,100):
   for xp in (0,95,100,3000):
    for injury in (-1,0,1,2,3):
     u.mem_write(officer+0xc8,bytes([base]*5));u.mem_write(officer+0x12a,struct.pack('<5H',*([xp]*5)));u.mem_write(officer+0x15c,struct.pack('<I',0))
     u.reg_write(UC_X86_REG_ECX,officer);t.call(0x48a2d0)
     before=bytearray(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));full=min(100,base+xp//100)
     u.reg_write(UC_X86_REG_ESI,officer);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack+0x14,struct.pack('<i',injury))
     u.emu_start(0x4a3d6b,0x4a3d77,count=10000);self.assertEqual(0x4a3d77,u.reg_read(UC_X86_REG_EIP))
     offset=officer-0x7200000;struct.pack_into('<i',before,offset+0x15c,injury)
     scale=100 if injury<1 else (80,50,30)[injury-1]
     before[offset+0x170:offset+0x175]=bytes([max(1,full*scale//100)]*4+[full])
     self.assertEqual(bytes(before),bytes(u.mem_read(0x7200000,0x300000)))
     self.assertEqual(bytes([full]*5),bytes(u.mem_read(officer+0x175,5)));self.assertEqual(rng,bytes(u.mem_read(0x8a5d44,4)))
  for invalid in (-2147483648,-2,4,2147483647):
   before=bytes(u.mem_read(0x7200000,0x300000));u.reg_write(UC_X86_REG_ECX,officer);t.call(0x48a8e0,invalid&0xffffffff)
   self.assertEqual(before,bytes(u.mem_read(0x7200000,0x300000)))

 def test_original_growth_changes_actual_quote_before_commit(self):
  t=self.t;d=self.d;u=d.u;building,city,district,_=t.actors();officer=d.root+0xc0bc;command=d.stream+0xc00
  u.reg_write(UC_X86_REG_ECX,officer);t.call(0x489f10)
  u.mem_write(officer+0xa0,struct.pack('<I',0));u.mem_write(officer+0xa4,struct.pack('<i',-1));u.mem_write(officer+0x60,struct.pack('<i',-1))
  u.mem_write(officer+0xd0,struct.pack('<5i',*([-1]*5)));u.mem_write(officer+0x15c,struct.pack('<I',0))
  self.assertEqual(1,t.call(0x47a630,officer));self.assertEqual(0,t.call(0x4a54a0,officer))
  # The original officer table identifies base-stat property28 as politics.
  t.call(0x73ca80);u.reg_write(UC_X86_REG_ESI,28);u.reg_write(UC_X86_REG_ESP,d.stack)
  u.emu_start(0x4c86eb,0x4c86f6,count=100)
  self.assertEqual('政治',bytes(u.mem_read(u.reg_read(UC_X86_REG_ECX),32)).split(b'\0')[0].decode('big5'))
  changed=0;count=0
  for base in (1,50,80,99,100):
   for experience in (0,94,95,99,100,2995,2999,3000):
    for food in (-20000,-1000,1000,20000):
     u.mem_write(officer+0xc8,bytes([50,50,50,base,50]));u.mem_write(officer+0x12a,b'\0'*10)
     u.mem_write(officer+0x130,struct.pack('<H',experience));u.mem_write(officer+0xae,struct.pack('<H',100))
     u.mem_write(city+0x44,struct.pack('<II',50000,100000));u.mem_write(city+0x7c,b'\x32');u.mem_write(city+0xa4,b'\x01');u.mem_write(district+0x2c,b'\x3c')
     u.mem_write(command,struct.pack('<III',building,officer,food&0xffffffff));u.reg_write(UC_X86_REG_ECX,officer);t.call(0x48a2d0)
     prior=min(100,base+experience//100);self.assertEqual(prior,bytes(u.mem_read(officer+0x173,1))[0])
     quoted=t.call(0x5ca620,building,officer,food&0xffffffff);quoted=quoted if quoted<2**31 else quoted-2**32
     before=bytearray(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4))
     u.reg_write(UC_X86_REG_ESI,command);u.reg_write(UC_X86_REG_EDI,officer);u.reg_write(UC_X86_REG_EBX,city);u.reg_write(UC_X86_REG_ESP,d.stack)
     u.emu_start(0x5cacb7,0x5cad20,count=50000);self.assertEqual(0x5cad20,u.reg_read(UC_X86_REG_EIP))
     xp=min(3000,experience+5);politics=min(100,base+xp//100)
     actual_quote=food*(450-politics)*10//20000 if food>0 else -(abs(food)*3200//((450-politics)*50))
     offset=officer-0x7200000;struct.pack_into('<H',before,offset+0x130,xp);struct.pack_into('<H',before,offset+0xae,150)
     before[offset+0x173]=politics;before[offset+0x178]=politics
     offset=city-0x7200000;struct.pack_into('<II',before,offset+0x44,50000-actual_quote,100000+food);before[offset+0xa4]=3;before[district+0x2c-0x7200000]=40
     actual=bytes(u.mem_read(0x7200000,0x300000))
     if bytes(before)!=actual:
      diff=[(hex(0x7200000+i),a,b) for i,(a,b) in enumerate(zip(before,actual)) if a!=b]
      self.fail(str((base,experience,food,diff[:30])))
     self.assertEqual(rng,bytes(u.mem_read(0x8a5d44,4)));changed+=quoted!=actual_quote;count+=1
  self.assertEqual(160,count);self.assertGreater(changed,0)
  print('original merchant ordered experience/quote cases=',count,'changed quotes=',changed)

if __name__=='__main__':unittest.main()
