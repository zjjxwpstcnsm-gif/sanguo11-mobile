#!/usr/bin/env python3
"""Verify the original special-building predicate used after a cavalry landing rejection."""
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
from unicorn.x86_const import UC_X86_REG_ECX
support.CityActionCostsTest.setUpClass()
t,d=support.CityActionCostsTest(),support.CityActionCostsTest.d
u=d.u
mappings=load_original_data(u)
raw=(source/'san11pk.exe').read_bytes()
shared=(source/'Media/scenario/Scenario.s11').read_bytes()
assert hashlib.sha256(raw).hexdigest()=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
assert hashlib.sha256(shared).hexdigest()=='dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f'
catalog=json.loads((Path(__file__).resolve().parents[2]/'docs/pc-data/shared-rule-catalog.json').read_text())
assert catalog['source_executable_sha256']==hashlib.sha256(raw).hexdigest()
names={r['native_index']:r for r in catalog['records'] if r['table']=='table_79c54'}
records={r['native_index']:r for r in d.records if r['kind']=='table_79c54'}
assert sorted(names)==sorted(records)==list(range(64))
building=d.root+0x89730
u.reg_write(UC_X86_REG_ECX,building)
t.call(0x4880a0)
results=[]
for index in list(range(64))+[-1,64,999]:
    u.mem_write(building+8,struct.pack('<i',index))
    before=bytes(u.mem_read(0x7200000,0x300000))
    rng=bytes(u.mem_read(0x8a5d44,4))
    u.reg_write(UC_X86_REG_ECX,building)
    actual=t.call(0x487ab0)
    assert before==bytes(u.mem_read(0x7200000,0x300000))
    assert rng==bytes(u.mem_read(0x8a5d44,4))
    row=dict(native_index=index,predicate=actual)
    if index in records:
        r=records[index]
        assert names[index]['source_record_sha256']==r['sha256']
        assert hashlib.sha256(shared[r['offset']:r['offset']+r['bytes']]).hexdigest()==r['sha256']
        category=struct.unpack('<i',u.mem_read(d.root+0x79c54+index*0xd0+0xb4,4))[0]
        assert actual==int(index!=24 and category==3)
        row.update(name=names[index]['name'],category_plusb4=category,source_offset=r['offset'],
                   source_record_sha256=r['sha256'])
    else:assert actual==0
    results.append(row)
assert [r['native_index'] for r in results if r['predicate']]==[16,17,18,19,20,21,22,23,25]
report=dict(exe_sha256=hashlib.sha256(raw).hexdigest(),shared_sha256=hashlib.sha256(shared).hexdigest(),
            rows=results,full_3mib_rng_unchanged=True,original_data_mappings=mappings,
            functions=[dict(start=hex(start),end=hex(end),bytes_hex=raw[start-0x400000:end-0x400000].hex(),
                            sha256=hashlib.sha256(raw[start-0x400000:end-0x400000]).hexdigest())
                       for start,end in [(0x487ab0,0x487aee),(0x595746,0x5957d1)]],
            limits=['Actual original64-template predicate plus3 bounds cases; special branch does not select city/gate/port',
                    'The cavalry caller/output flag, main/collision damage, duel and complete authoritative movement remain unverified',
                    'No Android combat numbers, old saves or PC resources changed'])
encoded=(json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode('utf-8')
(out/'native-building-predicate.json').write_bytes(encoded)
with (out/'native-building-predicate.json.gz').open('wb') as file:
    with gzip.GzipFile(filename='',mode='wb',fileobj=file,mtime=0) as packed:packed.write(encoded)
print('PASS original67 building predicate cases;9 positives; cities/gate/port excluded; world/RNG unchanged')
