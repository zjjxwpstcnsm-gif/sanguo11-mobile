"""Original gate/port capacity through district ownership and force technology bits."""
import hashlib,json,struct,unittest
from pathlib import Path
import test_pc_city_action_costs as support
from inspect_pc_scenario_domains import GROUPS
from unicorn.x86_const import UC_X86_REG_ECX

class SiteCapacityTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  support.CityActionCostsTest.setUpClass();cls.t=support.CityActionCostsTest();cls.d=cls.t.d
 def test_all_gates_ports_and_native_expansion(self):
  t=self.t;d=self.d;u=d.u;t.actors();force=d.root+0x7af8
  u.mem_write(force+4,struct.pack('<I',0));self.assertEqual(1,t.call(0x47a630,force))
  audit=json.loads((Path(__file__).resolve().parents[2]/'docs/pc-data/shared-rule-catalog.json').read_text())
  installation=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版')
  raw=(installation/audit['source']['path']).read_bytes();self.assertEqual(audit['source']['sha256'],hashlib.sha256(raw).hexdigest())
  tech=next(r for r in audit['records'] if r['kind']=='technology' and r['native_index']==33)
  self.assertEqual('擴展港關',tech['name']);start=tech['source_offset'];self.assertEqual(tech['source_record_sha256'],hashlib.sha256(raw[start:start+tech['source_record_bytes']]).hexdigest())
  self.assertEqual(bytes.fromhex(tech['name_raw_hex']),raw[start:start+8])
  cases=0
  for kind,base,stride,count,_,_ in GROUPS:
   if kind not in ('gate','port'):continue
   for index in range(count):
    site=d.root+base+stride*index;u.mem_write(site+0x20,struct.pack('<I',0))
    building=d.root+0x89730+(index+(42 if kind=='gate' else 52))*0x38
    u.reg_write(UC_X86_REG_ECX,building);t.call(0x4880a0);u.mem_write(building+8,struct.pack('<I',1 if kind=='gate' else 2))
    for bits in [0,1<<32,1<<33,1<<34,(1<<36)-1]:
     u.mem_write(force+0x58,struct.pack('<Q',bits));multiplier=4 if bits&(1<<33) else 1
     for getter,base_cap in [(0x486d30,10000),(0x486ea0,100000)]:
      before=bytes(u.mem_read(0x7200000,0x300000));u.reg_write(UC_X86_REG_ECX,building)
      self.assertEqual(base_cap*multiplier,t.call(getter),(kind,index,bits));self.assertEqual(before,bytes(u.mem_read(0x7200000,0x300000)));cases+=1
  self.assertEqual(450,cases)

if __name__=='__main__':unittest.main()
