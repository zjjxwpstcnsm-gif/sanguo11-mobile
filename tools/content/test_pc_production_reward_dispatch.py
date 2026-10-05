"""Original post-admission production dispatch and isolated completion-reward block.
No full admission/job completion or Android rule implementation is claimed.
The verified PC directory stays read-only; observations cover complete3MiB/RNG.
"""
import struct, unittest
import test_pc_city_action_costs as support
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_EAX,UC_X86_REG_EIP

class ProductionRewardDispatchTest(unittest.TestCase):
 def test_award4_merit100_block(self):
  support.CityActionCostsTest.setUpClass();t=support.CityActionCostsTest();d=t.d;u=d.u;t.actors();officer=d.root+0xc0bc;u.reg_write(UC_X86_REG_ECX,officer);t.call(0x489f10)
  for offset,value in [(0x9c,0),(0xa0,0),(0xa4,-1),(0x60,-1),(0x15c,0)]:u.mem_write(officer+offset,struct.pack('<i',value))
  u.mem_write(officer+0xd0,struct.pack('<5i',*([-1]*5)));start,end=0x5c5ed8,0x5c5ef6
  assert bytes(u.mem_read(start,end-start)).hex()=='6a016a046a0256b95c899907e8e711eeff6a6456b95c899907e85a0eeeff'
  checks=0
  for base in (1,50,80,99,100):
   for xp in (0,95,96,98,99,100,2995,2996,2999,3000):
    for injury in (-1,0,1,2,3):
     for merit in (0,59899,59900,59999,60000):
      u.mem_write(officer+0xc8,bytes([base]*5));v=[0]*5;v[2]=xp;u.mem_write(officer+0x12a,struct.pack('<5H',*v));u.mem_write(officer+0x15c,struct.pack('<i',injury));u.mem_write(officer+0xae,struct.pack('<H',merit));u.reg_write(UC_X86_REG_ECX,officer);t.call(0x48a2d0)
      before=bytearray(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));u.reg_write(UC_X86_REG_ESI,officer);u.reg_write(UC_X86_REG_ESP,d.stack);u.emu_start(start,end,count=50000);assert u.reg_read(UC_X86_REG_EIP)==end
      offset=officer-0x7200000;after=min(3000,xp+4);struct.pack_into('<H',before,offset+0x12e,after);struct.pack_into('<H',before,offset+0xae,min(60000,merit+100));full=min(100,base+after//100);scale=100 if injury<1 else (80,50,30)[injury-1];before[offset+0x172]=max(1,full*scale//100);before[offset+0x177]=full
      assert bytes(before)==bytes(u.mem_read(0x7200000,0x300000)),(base,xp,injury,merit);assert rng==bytes(u.mem_read(0x8a5d44,4));checks+=1
  print('PASS '+self._testMethodName+' observations='+str(checks)+ ' complete3MiB/RNG; full command timing and Android integration pending')
 def test_post_admission_dispatch(self):
  support.CityActionCostsTest.setUpClass();t=support.CityActionCostsTest();d=t.d;u=d.u;command=d.stream+0xc00;rows=[];entries={0x5c65b0:'immediate_body',0x5c6eb0:'delayed_body'}
  for item in [-1,*range(12),12,0x7fffffff]:
   u.mem_write(command,b'\0'*32);u.mem_write(command+0x10,struct.pack('<i',item));u.mem_write(d.stack,struct.pack('<3I',0,d.stop,command));u.reg_write(UC_X86_REG_ESP,d.stack);u.reg_write(UC_X86_REG_ESI,command);world=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));seen=[]
   def stop(uc,address,size,unused):
    if address in entries:seen.append(address);uc.emu_stop()
   hook=u.hook_add(UC_HOOK_CODE,stop)
   try:u.emu_start(0x5c74df,d.stop,count=100)
   finally:u.hook_del(hook)
   wanted=[0x5c65b0] if 0<=item<=4 else [0x5c6eb0] if 5<=item<=11 else []
   assert seen==wanted,(item,seen);assert world==bytes(u.mem_read(0x7200000,0x300000));assert rng==bytes(u.mem_read(0x8a5d44,4))
   if not wanted:assert u.reg_read(UC_X86_REG_EIP)==d.stop and u.reg_read(UC_X86_REG_EAX)==0
   rows.append(dict(item=item,branch=entries[seen[0]] if seen else 'invalid_returns0',target=hex(seen[0]) if seen else None))
  print('PASS '+self._testMethodName+' observations='+str(len(rows))+ ' complete3MiB/RNG; full command timing and Android integration pending')

if __name__=='__main__':unittest.main()
