#!/usr/bin/env python3
"""Reproduce original cavalry dispatch bytes and source-name identity, read only.
Static call edges do not prove full hit/damage/collision semantics.
"""
import argparse,hashlib,json,struct
from pathlib import Path
from capstone import Cs,CS_ARCH_X86,CS_MODE_32

ROOT=Path(__file__).resolve().parents[2]
SOURCE=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版')
EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve()
if out==SOURCE.resolve() or SOURCE.resolve() in out.parents:raise ValueError('PC installation is read-only')
out.mkdir(parents=True,exist_ok=False)
raw=(SOURCE/'san11pk.exe').read_bytes();assert hashlib.sha256(raw).hexdigest()==EXE_SHA
shared=(SOURCE/'Media/scenario/Scenario.s11').read_bytes();catalog_raw=(ROOT/'docs/pc-data/shared-rule-catalog.json').read_bytes();catalog=json.loads(catalog_raw)
assert catalog['source_executable_sha256']==EXE_SHA and hashlib.sha256(shared).hexdigest()==catalog['source']['sha256']
table=struct.unpack_from('<19I',raw,0x587118-0x400000);assert [table[n] for n in [9,10,11]]==[0x58637b,0x586395,0x5863aa]
def direct_call(address,target):
    block=raw[address-0x400000:address-0x400000+5]
    assert block[0]==0xe8 and address+5+struct.unpack('<i',block[1:])[0]==target
    return dict(address=hex(address),target=hex(target),bytes=block.hex())
edges=[direct_call(x,y) for x,y in [(0x58638e,0x595b70),(0x5863a3,0x595bb0),(0x5863bd,0x595b90),(0x595b7c,0x595630),(0x595b9c,0x595630),(0x595724,0x594650)]]
assert raw[0x195b79:0x195b7b]==bytes.fromhex('6a01') and raw[0x195b99:0x195b9b]==bytes.fromhex('6a02')
names=[]
for n in [9,10,11]:
    row=next(x for x in catalog['records'] if x['kind']=='tactic' and x['native_index']==n)
    original=shared[row['source_offset']:row['source_offset']+row['source_record_bytes']]
    assert hashlib.sha256(original).hexdigest()==row['source_record_sha256']
    names.append({key:row[key] for key in ['native_index','name','name_raw_hex','source_offset','source_record_sha256','serializer']})
md=Cs(CS_ARCH_X86,CS_MODE_32);regions=[]
for start,end in [(0x58589c,0x5859d0),(0x5861e1,0x586470),(0x595b70,0x595bb0),(0x595630,0x595b70),(0x594650,0x5946e3),(0x67f750,0x67f75f)]:
    block=raw[start-0x400000:end-0x400000]
    regions.append(dict(start=hex(start),end=hex(end),bytes_hex=block.hex(),sha256=hashlib.sha256(block).hexdigest(),instructions=[dict(address=hex(i.address),bytes=i.bytes.hex(),instruction=i.mnemonic+' '+i.op_str) for i in md.disasm(block,start)]))
report=dict(exe_sha256=EXE_SHA,shared_sha256=hashlib.sha256(shared).hexdigest(),catalog_sha256=hashlib.sha256(catalog_raw).hexdigest(),native_names=names,
            dispatch_table=dict(address='587118',bytes_hex=raw[0x187118:0x187118+19*4].hex(),targets=[hex(x) for x in table]),direct_edges=edges,regions=regions,
            verified_static=['Native11 source name突進 routes5863aa ->595b90 ->595630 with immediate step count2',
                             'Native9突擊 uses same common path with step count1; native10突破 uses separate595bb0',
                             'Common landing loop calls original594650; its final predicate rejects all nonzero occupancy low2 bits'],
            limits=['Static source edges and bounded predicate only; no full PC boot, target admission, hit, damage, collision or failure equivalence',
                    'Active view-filter callback and source city-grid population remain unclosed; no Android rule values changed'])
(out/'source-dispatch.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print('Verified original cavalry source names, dispatch edges and byte hashes; full battle pending')
