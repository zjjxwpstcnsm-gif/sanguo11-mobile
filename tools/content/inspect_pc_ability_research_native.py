#!/usr/bin/env python3
"""Decode the 98 original research rows and execute their original parameter getters.
No award, admission, training tick, UI callback or save migration is invented.
"""
import argparse,hashlib,json,struct,sys
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve()
source=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve()
if out==source or source in out.parents:raise ValueError('PC directory is read-only')
out.mkdir(parents=True,exist_ok=False);sys.path.insert(0,str(Path(__file__).resolve().parent))
import test_pc_city_action_costs as s
from pc_original_pe_data import load_original_data
from unicorn import UC_HOOK_MEM_WRITE
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_EAX
s.CityActionCostsTest.setUpClass();t=s.CityActionCostsTest();d=t.d;u=d.u;mappings=load_original_data(u)
raw=(source/'san11pk.exe').read_bytes();exe_sha=hashlib.sha256(raw).hexdigest();assert exe_sha=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
shared=(source/'Media/scenario/Scenario.s11').read_bytes()
assert hashlib.sha256(shared).hexdigest()=='dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f'
rows=[r for r in d.records if r['kind']=='table_86dd8'];assert [r['native_index'] for r in rows]==list(range(98))
writes=[];capture=False
def observe(machine,access,address,size,value,user):
    if capture and not d.stack-0x10000<=address<d.stack+0x10000:writes.append((address,size))
u.hook_add(UC_HOOK_MEM_WRITE,observe);results=[]
for row in rows:
    actor=row['actor_address'];index=row['native_index'];before=bytearray(u.mem_read(0x7200000,0x300000))
    u.reg_write(UC_X86_REG_ECX,actor);t.call(0x494f50,index)
    before[actor+0x68-0x7200000:actor+0x6c-0x7200000]=struct.pack('<I',index)
    assert bytes(before)==bytes(u.mem_read(0x7200000,0x300000))
    values={}
    for entry in [0x494f80,0x494fb0,0x494fd0,0x494ff0,0x495010]:
        before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));writes.clear();capture=True
        u.reg_write(UC_X86_REG_ECX,actor);u.reg_write(UC_X86_REG_EAX,0x5a5a5a00 if entry==0x494f80 else 0)
        value=t.call(entry);capture=False
        value=value&255 if entry==0x494f80 else value
        assert not writes and before==bytes(u.mem_read(0x7200000,0x300000)) and rng==bytes(u.mem_read(0x8a5d44,4))
        values[hex(entry)]=value
    b=bytes(u.mem_read(actor,0x6c));record=shared[row['offset']:row['offset']+row['bytes']];assert hashlib.sha256(record).hexdigest()==row['sha256']
    results.append(dict(native_index=index,name=b[4:13].split(b'\0',1)[0].decode('big5'),description=b[13:68].split(b'\0',1)[0].decode('big5'),
                        source_offset=row['offset'],source_record_sha256=row['sha256'],source_reads=row['reads'],actor_hex=b.hex(),
                        raw_fields={hex(offset):struct.unpack_from('<i',b,offset)[0] for offset in [0x44,0x48,0x4c,0x50,0x54,0x58,0x5c,0x64]},bytes60_61=list(b[0x60:0x62]),original_getters=values))
assert [r['original_getters']['0x494f80'] for r in results[:15]]==[5,5,3]*5
assert [r['original_getters']['0x494fd0'] for r in results[:15]]==[70,80,95]*5
functions=[]
for start,end in [(0x494e40,0x494e52),(0x494f50,0x494f5a),(0x494f60,0x494f78),(0x494f80,0x494fa0),(0x494fb0,0x494fc8),(0x494fd0,0x494fea),(0x494ff0,0x49500a),(0x495010,0x49502a),(0x495060,0x49510a)]:
    code=raw[start-0x400000:end-0x400000];functions.append(dict(start=hex(start),end=hex(end),bytes_hex=code.hex(),sha256=hashlib.sha256(code).hexdigest()))
report=dict(exe_sha256=exe_sha,shared_sha256=hashlib.sha256(shared).hexdigest(),source='Media/scenario/Scenario.s11',rows=results,original_functions=functions,
            getter_calls=490,full_world_rng_unchanged=True,nonstack_getter_writes=0,original_data_mappings=mappings,
            index_input='Explicit original494f50 setter receives serializer ordinal; complete postload registration not claimed',
            limits=['The+44 raw3/3/2 is an encoded value: actual494f80 AL returns5/5/3; do not import raw counts',
                    'Original getters establish bounded parameter values; actual training award/base write, caps, duration consumer, hidden selection RNG and effective MOD source remain unresolved',
                    'No Android catalog, save policy or numeric rule changed; descriptions are source evidence, not full execution'])
(out/'native-parameters.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print('PASS original98 research rows,490 getters, full world/RNG/no writes; training completion pending')
