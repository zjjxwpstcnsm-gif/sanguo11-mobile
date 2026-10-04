#!/usr/bin/env python3
"""Bounded original signed durability equality -> state setter, before UI."""
import argparse,gzip,hashlib,json,struct,sys
from pathlib import Path

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args()
 source=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve();out=a.output.resolve()
 if out==source or source in out.parents:raise ValueError('PC source is read only')
 out.mkdir(parents=True,exist_ok=False);sys.path.insert(0,str(Path(__file__).resolve().parent))
 from test_pc_city_action_costs import CityActionCostsTest
 from pc_original_pe_data import load_original_data
 from unicorn import UC_HOOK_CODE
 from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_EBX,UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_EIP
 CityActionCostsTest.setUpClass();t=CityActionCostsTest();d=t.d;u=d.u;mappings=load_original_data(u);_,city,district,facility=t.actors()
 word=lambda at,v:u.mem_write(at,struct.pack('<I',v&0xffffffff))
 read=lambda at:struct.unpack('<I',u.mem_read(at,4))[0]
 base=0x7200000;size=0x300000;rows=[];setters=[]
 stop=u.hook_add(UC_HOOK_CODE,lambda m,at,z,q:m.emu_stop(),begin=0x5bbb9c,end=0x5bbb9c)
 templates=[r for r in d.records if r['kind']=='table_79c54']
 try:
  for record in templates[3:]:
   native=record['native_index'];actor=record['actor_address'];category=read(actor+0xb4);maximum=struct.unpack('<H',u.mem_read(actor+0xc2,2))[0]
   word(facility+8,native);u.reg_write(UC_X86_REG_ECX,facility)
   assert t.call(0x487150)&65535==maximum
   for durability in sorted({0,max(0,maximum-1),maximum,min(65535,maximum+1)}):
    word(facility+0x14,0);word(facility+0xc,7);u.mem_write(facility+0x10,struct.pack('<H',durability))
    before=bytes(u.mem_read(base,size));rng=bytes(u.mem_read(0x8a5d44,4));u.reg_write(UC_X86_REG_EBX,facility);u.reg_write(UC_X86_REG_ESP,d.stack)
    u.emu_start(0x5bb823,0x5bb851,count=10000);end=u.reg_read(UC_X86_REG_EIP)
    signed_durability=durability if durability<32768 else durability-65536
    completed=signed_durability==maximum
    if end!=(0x5bb851 if completed else 0x5bbb9c):
     (out/'failure.json').write_text(json.dumps(dict(native_index=native,category=category,maximum=maximum,durability=durability,endpoint=hex(end),field14=read(facility+0x14)),indent=2)+'\n')
     raise AssertionError((native,maximum,durability,hex(end),completed))
    expected=bytearray(before)
    if completed:
     struct.pack_into('<I',expected,facility+0x14-base,1)
     if category in [2,3] and native!=24:struct.pack_into('<I',expected,facility+0xc-base,0xffffffff)
    assert bytes(expected)==bytes(u.mem_read(base,size)) and rng==bytes(u.mem_read(0x8a5d44,4))
    rows.append(dict(native_index=native,category=category,maximum=maximum,durability_raw16=durability,durability_signed16=signed_durability,completed=completed,endpoint=hex(end),source_record_sha256=record['sha256'],exact_world_effect=True,rng_unchanged=True))
   for value in [0,1]:
    word(facility+0x14,1-value);word(facility+0xc,7);before=bytes(u.mem_read(base,size));rng=bytes(u.mem_read(0x8a5d44,4));u.reg_write(UC_X86_REG_ECX,facility);t.call(0x487ca0,value)
    expected=bytearray(before);struct.pack_into('<I',expected,facility+0x14-base,value)
    if category in [2,3] and native!=24:struct.pack_into('<I',expected,facility+0xc-base,0xffffffff)
    assert bytes(expected)==bytes(u.mem_read(base,size)) and rng==bytes(u.mem_read(0x8a5d44,4))
    setters.append(dict(native_index=native,category=category,value=value,exact_world_effect=True,rng_unchanged=True))
 finally:u.hook_del(stop)
 raw=(source/'san11pk.exe').read_bytes();shared=(source/'Media/scenario/Scenario.s11').read_bytes()
 assert hashlib.sha256(raw).hexdigest()=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
 assert hashlib.sha256(shared).hexdigest()=='dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f'
 report=dict(executable_sha256=hashlib.sha256(raw).hexdigest(),shared_sha256=hashlib.sha256(shared).hexdigest(),original_data_mappings=mappings,
  rows=rows,setter_rows=setters,functions=[dict(start=hex(low),end=hex(high),bytes_hex=raw[low-0x400000:high-0x400000].hex()) for low,high in [(0x5bb823,0x5bb851),(0x487150,0x48722a),(0x487ca0,0x487d0a)]],
  limits=['Entry5bb823 follows original progress/resource/presentation work, which is not executed here','Stop before59fdd0 notification on completion, or observe5bbb9c noncompletion endpoint; no complete construction return','Explicit fixture durability and state; not official opening, elapsed construction timing, workers or cancellation','Native0..2 city/gate/port max durability uses separate source actors and is excluded','No Android state migration or gameplay changes'])
 encoded=(json.dumps(report,indent=2)+'\n').encode();(out/'facility-completion-native.json').write_bytes(encoded)
 with (out/'facility-completion-native.json.gz').open('wb') as f:
  with gzip.GzipFile(filename='',mode='wb',fileobj=f,mtime=0) as z:z.write(encoded)
 print('PASS original completion',len(rows),'durability boundaries and',len(setters),'state setters; exact3MiB effects/RNG unchanged; pre/post boundary work pending')

if __name__=='__main__':main()
