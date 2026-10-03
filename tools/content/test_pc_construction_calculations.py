"""Original bounded facility arithmetic; semantics not promoted to runtime rules yet."""
import struct,unittest
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EIP,UC_X86_REG_EAX
from inspect_pc_scenario_tail import NativeTailDecoder

class ConstructionCalculationsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        install=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版')
        cls.d=NativeTailDecoder((install/'san11pk.exe').read_bytes())
        cls.report=cls.d.decode_tail((install/'Media/scenario/Scenario.s11').read_bytes(),True)
        cls.rows=[r for r in cls.report['records'] if r['kind']=='table_79c54']
    def call(self,address,*args):
        d=self.d;u=d.u;u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<'+'I'*(len(args)+1),d.stop,*args));u.emu_start(address,d.stop,count=10000)
        self.assertEqual(d.stop,u.reg_read(UC_X86_REG_EIP));return u.reg_read(UC_X86_REG_EAX)
    def crew(self,stats):
        d=self.d;array=d.stream+0x100;pointers=[]
        for i,stat in enumerate(stats):
            address=d.stream+0x200+i*0x200;d.u.mem_write(address+0x173,bytes([stat]));pointers.append(address)
        d.u.mem_write(array,struct.pack('<III',*(pointers+[0]*(3-len(pointers)))));return array
    def test_exact_original_arithmetic_and_pure_globals(self):
        u=self.d.u;before=bytes(u.mem_read(0x7200000,0x300000));count=0
        for row in self.rows:
            actor=bytes.fromhex(row['actor_hex']);field_c2,field_c4=struct.unpack_from('<HH',actor,0xc2);index=row['native_index']
            for stats in [[],[1],[80],[100],[80,40],[40,80],[100,80,60],[255,255,255]]:
                crew=self.crew(stats);power=max(max(stats,default=0)+sum(stats)//2,(field_c2-field_c2//4)//9)
                self.assertEqual(power,self.call(0x5bb1d0,crew,index),(index,stats))
                self.assertEqual(field_c4,self.call(0x5bb2b0,crew,index),(index,stats))
                self.assertEqual(0 if power==0 else int((field_c2*3//4-1)/power)+1,self.call(0x5bb2e0,crew,index),(index,stats))
                count+=1
        self.assertEqual(before,bytes(u.mem_read(0x7200000,0x300000)),'original arithmetic does not mutate global game state/RNG')
        self.assertEqual(512,count)
    def test_original_values_follow_source_field_mutation(self):
        d=self.d;row=self.rows[31];raw=bytearray(d.raw)
        read=next(r for r in row['reads'] if r['destination']=='actor+0xc4')
        struct.pack_into('<H',raw,read['offset'],1234);d.decode_tail(bytes(raw),True)
        self.assertEqual(1234,self.call(0x5bb2b0,self.crew([80]),31))
        self.assertEqual(struct.unpack_from('<H',bytes.fromhex(self.rows[32]['actor_hex']),0xc4)[0],self.call(0x5bb2b0,self.crew([80]),32))

    def test_original_build_debit_reaches_city_resource(self):
        from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_EBX
        d=self.d;u=d.u
        # Original building constructor, first registry entry (city0), no fake
        # virtual methods. Bounded execution excludes placement and worker setup.
        building=d.root+0x89730;city=d.root+0x1d8
        u.reg_write(UC_X86_REG_ECX,building);self.call(0x4880a0)
        u.mem_write(building+8,struct.pack('<I',0))
        command=d.stream+0xc00
        for index in [30,31,32,33,34,35,36,37,38,39,48]:
            cost=self.call(0x5bb2b0,self.crew([80]),index)
            for amount in [0,cost,10000]:
                u.mem_write(city+0x44,struct.pack('<I',amount))
                u.mem_write(command,struct.pack('<I',building));u.mem_write(command+0x10,struct.pack('<I',index))
                before=bytearray(u.mem_read(0x7200000,0x300000))
                u.reg_write(UC_X86_REG_EBX,command);u.reg_write(UC_X86_REG_ESP,d.stack)
                u.emu_start(0x5bc462,0x5bc49b,count=10000)
                self.assertEqual(0x5bc49b,u.reg_read(UC_X86_REG_EIP))
                struct.pack_into('<I',before,city+0x44-0x7200000,max(0,amount-cost))
                self.assertEqual(bytes(before),bytes(u.mem_read(0x7200000,0x300000)),(index,amount,cost))

    def test_original_construction_menu_uses_base_facility_ids(self):
        from unicorn.x86_const import UC_X86_REG_EDI
        d=self.d;u=d.u;u.reg_write(UC_X86_REG_ESP,d.stack)
        # Native menu initialization: table pointer and 20 rows. No UI calls
        # or permission hooks: stop before the first presentation string call.
        u.emu_start(0x600e15,0x600e25,count=20)
        pointer=struct.unpack('<I',u.mem_read(d.stack+0x18,4))[0]
        count=struct.unpack('<I',u.mem_read(d.stack+0x10,4))[0]
        self.assertEqual((0x8ba678,20),(pointer,count))
        expected=[31,32,33,34,35,36,37,38,39,30,40,41,42,43,44,45,46,47,48,49]
        before=bytes(u.mem_read(0x7200000,0x300000))
        for index,facility in enumerate(expected):
            # Original loop increment at6011bf is +0x14; each row's ID is
            # pointer-8. Execute the original registry lookup for every row.
            u.mem_write(d.stack+0x18,struct.pack('<I',pointer+index*0x14))
            u.reg_write(UC_X86_REG_ESP,d.stack)
            u.emu_start(0x600e25,0x600e37,count=1000)
            self.assertEqual(0x600e37,u.reg_read(UC_X86_REG_EIP))
            self.assertEqual(facility,u.reg_read(UC_X86_REG_EDI))
            self.assertEqual(d.root+0x79c54+facility*0xd0,u.reg_read(UC_X86_REG_EAX))
        self.assertEqual(before,bytes(u.mem_read(0x7200000,0x300000)))

    def test_original_construction_action_debit_reaches_district(self):
        from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_EBX
        d=self.d;u=d.u;building=d.root+0x89730;city=d.root+0x1d8;district=d.root+0xb20c
        u.reg_write(UC_X86_REG_ECX,building);self.call(0x4880a0)
        u.mem_write(building+8,struct.pack('<I',0));u.mem_write(city+0x38,struct.pack('<I',0))
        # Shared definitions leave district ownership=-1. Native47e070 requires
        # a live force index at+4; set force0 without replacing its validator.
        u.mem_write(district+4,struct.pack('<I',0))
        for actor in (building,city,district):self.assertEqual(1,self.call(0x47a630,actor))
        command=d.stream+0xc00;u.mem_write(command,struct.pack('<I',building))
        for amount in [0,19,20,21,60,255]:
            u.mem_write(district+0x2c,bytes([amount]));before=bytearray(u.mem_read(0x7200000,0x300000))
            u.reg_write(UC_X86_REG_EBX,command);u.reg_write(UC_X86_REG_ESP,d.stack)
            # Real construction caller pushes20; original5b9340 follows the
            # building's district and invokes4a1820. Stop before UI notification.
            u.emu_start(0x5bc4b5,0x5b9387,count=10000)
            self.assertEqual(0x5b9387,u.reg_read(UC_X86_REG_EIP))
            before[district+0x2c-0x7200000]=max(0,amount-20)
            self.assertEqual(bytes(before),bytes(u.mem_read(0x7200000,0x300000)),amount)

if __name__=='__main__':unittest.main()
