#!/usr/bin/env python3
"""Execute original cultivation AP debit block, stopping before unrelated live UI callbacks."""
import argparse
import gzip
import hashlib
import json
import struct
import sys
from pathlib import Path

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--output',required=True,type=Path)
a=p.parse_args()
source=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve()
out=a.output.resolve()
if out==source or source in out.parents:raise ValueError('PC directory is read-only')
out.mkdir(parents=True,exist_ok=False)
sys.path.insert(0,str(Path(__file__).resolve().parent))
import test_pc_city_action_costs as support
from pc_original_pe_data import load_original_data
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_EIP

support.CityActionCostsTest.setUpClass()
t,d=support.CityActionCostsTest(),support.CityActionCostsTest.d
u=d.u
mappings=load_original_data(u)
raw=(source/'san11pk.exe').read_bytes()
assert hashlib.sha256(raw).hexdigest()=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
building,city,district,_=t.actors()
cost=struct.unpack('<i',u.mem_read(0x84ce0c,4))[0]
assert cost==20
baseline=bytes(u.mem_read(0x7200000,0x300000))
results=[]
for task in [41,42,43]:
    for ap in [0,19,20,21,60,255]:
        u.mem_write(0x7200000,baseline)
        u.mem_write(district+0x2c,bytes([ap]))
        expected=bytearray(u.mem_read(0x7200000,0x300000))
        rng=bytes(u.mem_read(0x8a5d44,4))
        # Match the original5b9340 frame after its receiver validity check and
        # saved ESI push. Arguments are original caller inputs, never rule returns.
        u.mem_write(d.stack,struct.pack('<4I',d.stop,building,task,cost))
        u.reg_write(UC_X86_REG_ESI,building)
        u.reg_write(UC_X86_REG_ESP,d.stack-4)
        u.emu_start(0x5b9352,0x5b9387,count=100000)
        assert u.reg_read(UC_X86_REG_EIP)==0x5b9387
        after=max(0,ap-cost)
        expected[district+0x2c-0x7200000]=after
        assert bytes(expected)==bytes(u.mem_read(0x7200000,0x300000))
        assert rng==bytes(u.mem_read(0x8a5d44,4))
        results.append(dict(task=task,before=ap,cost=cost,after=after,
                            admission_not_claimed=ap<cost))
assert len(results)==18
admission=[]
# Observation stop at the actual rejection entry; no game return or validator
# is replaced. Success stops at the original next branch boundary.
stop=u.hook_add(UC_HOOK_CODE,lambda machine,address,size,user:machine.emu_stop(),begin=0x5d9843,end=0x5d9843)
try:
    for ap in [0,9,19,20,21,60,255]:
        u.mem_write(0x7200000,baseline)
        u.mem_write(district+0x2c,bytes([ap]))
        before=bytes(u.mem_read(0x7200000,0x300000))
        rng=bytes(u.mem_read(0x8a5d44,4))
        u.reg_write(UC_X86_REG_ESI,district)
        u.reg_write(UC_X86_REG_ESP,d.stack)
        # Execute the real receiver validity call and TEST as well: the JE
        # after MOV cost consumes those flags, not flags from a prior VM call.
        u.emu_start(0x5d9678,0x5d969a,count=1000)
        end=u.reg_read(UC_X86_REG_EIP)
        assert end==(0x5d969a if ap>=cost else 0x5d9843)
        assert before==bytes(u.mem_read(0x7200000,0x300000))
        assert rng==bytes(u.mem_read(0x8a5d44,4))
        admission.append(dict(ap=ap,cost=cost,branch_allows=ap>=cost,stop_address=hex(end)))
finally:
    u.hook_del(stop)
report=dict(exe_sha256=hashlib.sha256(raw).hexdigest(),global_cost_address='0x84ce0c',cost=cost,
            block=['0x5b9352','0x5b9387'],cases=results,full_3mib_mutation_guard=True,rng_unchanged=True,
            stat_ap_admission=['0x5d9678','0x5d969a'],stat_ap_admission_cases=admission,
            original_data_mappings=mappings,
            original_functions=[dict(start=hex(start),end=hex(end),bytes_hex=raw[start-0x400000:end-0x400000].hex(),
                                     sha256=hashlib.sha256(raw[start-0x400000:end-0x400000]).hexdigest())
                                for start,end in [(0x5b9340,0x5b93b7),(0x5d98d0,0x5d9a88),
                                                 (0x5da6e0,0x5da8a1),(0x5daf10,0x5db0c8)]],
            limits=['AP debit primitive/caller cost20 and original STAT AP predicate verified; below20 debit cases remain non-admitted inputs',
                    'Other caller validators, gold fees, task registration list allocation and UI callbacks not executed here',
                    'No Android rules, saves, source resource or PC files changed'])
encoded=(json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode('utf-8')
(out/'native-training-ap.json').write_bytes(encoded)
with (out/'native-training-ap.json.gz').open('wb') as file:
    with gzip.GzipFile(filename='',mode='wb',fileobj=file,mtime=0) as packed:packed.write(encoded)
print('PASS original cultivation AP18 debit and7 actual STAT admission branches, cost20, world/RNG guards; whole start pending')
