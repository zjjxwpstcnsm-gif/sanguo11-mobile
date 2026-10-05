import struct
import unittest
from pathlib import Path
from inspect_pc_scenario_tail import NativeTailDecoder, TABLES


class NativeTailTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.install=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版')
        cls.shared=(cls.install/'Media/scenario/Scenario.s11').read_bytes()
        cls.scenario=next(p for p in (cls.install/'Media/scenario').iterdir() if p.name.lower()=='scen000.s11').read_bytes()
        cls.decoder=NativeTailDecoder((cls.install/'san11pk.exe').read_bytes())
        cls.reference=cls.decoder.decode_tail(cls.shared,True)

    def test_original_dispatch_reaches_both_file_ends(self):
        shared=self.reference;scenario=self.decoder.decode_tail(self.scenario)
        self.assertEqual((2310,len(self.shared)),(shared['start'],shared['end']))
        self.assertEqual((162702,len(self.scenario)),(scenario['start'],scenario['end']))
        self.assertEqual(sum(t[2] for t in TABLES),len(shared['records']))
        self.assertEqual(883,sum(r['bytes']>0 for r in shared['records']))
        self.assertEqual(84,sum(r['bytes']>0 for r in scenario['records']))
        self.assertEqual({'table_7ecd8'},{r['kind'] for r in scenario['records'] if r['bytes']})
        self.assertEqual(self.reference,self.decoder.decode_tail(self.shared,True),'reused VM/TB cannot cross prefix stop or leak type22 data')

    def test_names_follow_raw_records_and_preserve_neighbors(self):
        rows=[r for r in self.reference['records'] if r['kind']=='table_84858']
        left,right=rows[:2];self.assertEqual(left['bytes'],right['bytes'])
        modified=bytearray(self.shared)
        for destination,source in ((left,right),(right,left)):
            modified[destination['offset']:destination['offset']+destination['bytes']]=bytes.fromhex(source['raw_hex'])
        actual=self.decoder.decode_tail(bytes(modified),True)
        for row,original in zip(actual['records'],self.reference['records']):
            expected=right if row['kind']=='table_84858' and row['native_index']==0 else left if row['kind']=='table_84858' and row['native_index']==1 else original
            self.assertEqual(expected['actor_hex'],row['actor_hex'],(row['kind'],row['native_index']))
        self.assertEqual(self.reference,self.decoder.decode_tail(self.shared,True))

    def test_original_tactic_cost_accessor_uses_source_field(self):
        from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP,UC_X86_REG_EAX
        d=self.decoder;d.decode_tail(self.shared,True);u=d.u
        before=bytes(u.mem_read(0x7200000,0x300000))
        values=[]
        for index in range(32):
            u.reg_write(UC_X86_REG_ECX,d.root+0x169730);u.reg_write(UC_X86_REG_ESP,d.stack)
            u.mem_write(d.stack,struct.pack('<II',d.stop,index));u.emu_start(0x4951a0,d.stop,count=1000)
            self.assertEqual(d.stop,u.reg_read(UC_X86_REG_EIP));values.append(u.reg_read(UC_X86_REG_EAX))
        self.assertEqual([15,20,25,15,20,30,10,15,25,15,20,25,10,10,10,10,10,10,15]+[0]*13,values)
        self.assertEqual(before,bytes(u.mem_read(0x7200000,0x300000)))
        row=next(r for r in self.reference['records'] if r['kind']=='table_84858' and r['native_index']==15)
        field=next(r for r in row['reads'] if r['destination']=='actor+0x30')
        altered=bytearray(self.shared);altered[field['offset']]=73;d.decode_tail(bytes(altered),True)
        u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<II',d.stop,15));u.emu_start(0x4951a0,d.stop,count=1000)
        self.assertEqual(73,u.reg_read(UC_X86_REG_EAX),'getter follows changed source field; no hardcoded return')

    def test_original_execution_block_debits_source_cost(self):
        from unicorn.x86_const import UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_EIP
        d=self.decoder;d.decode_tail(self.shared,True);u=d.u;unit=d.root+0x169730
        # Neutral isolated unit: no officer/force. Exercise the original bounded
        # debit block, not target validation, hit resolution or a complete battle.
        u.mem_write(unit+0xc,struct.pack('<i',-1))
        rows=[r for r in self.reference['records'] if r['kind']=='table_84858']
        costs=[15,20,25,15,20,30,10,15,25,15,20,25,10,10,10,10,10,10,15]+[0]*13
        for row,cost in zip(rows,costs):
            for energy in [100,cost,max(0,cost-1)]:
                u.mem_write(unit+0x1a,bytes([energy]))
                before=bytearray(u.mem_read(0x7200000,0x300000))
                u.reg_write(UC_X86_REG_ESI,unit);u.reg_write(UC_X86_REG_ESP,d.stack)
                u.mem_write(d.stack+0x10,struct.pack('<I',row['actor_address']))
                u.emu_start(0x58589c,0x5858b7,count=10000)
                self.assertEqual(0x5858b7,u.reg_read(UC_X86_REG_EIP))
                before[unit+0x1a-0x7200000]=max(0,energy-cost)
                self.assertEqual(bytes(before),bytes(u.mem_read(0x7200000,0x300000)),(row['native_index'],energy))


if __name__=='__main__':unittest.main()
