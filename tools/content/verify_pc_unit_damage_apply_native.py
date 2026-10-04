#!/usr/bin/env python3
"""Original result+c -> unit troop debit, before other damage/death/frontend work."""
import argparse,gzip,hashlib,json,struct,sys
from pathlib import Path

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args()
 source=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve();out=a.output.resolve()
 if out==source or source in out.parents:raise ValueError('PC source is read only')
 out.mkdir(parents=True,exist_ok=False);sys.path.insert(0,str(Path(__file__).resolve().parent))
 from test_pc_city_action_costs import CityActionCostsTest
 from pc_original_pe_data import load_original_data
 from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_EDI,UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_EIP,UC_X86_REG_EAX
 CityActionCostsTest.setUpClass();t=CityActionCostsTest();d=t.d;u=d.u;mappings=load_original_data(u)
 officer=d.root+0xc0bc;unit=d.root+0x169730;request=d.stack+0x600
 word=lambda at,v:u.mem_write(at,struct.pack('<I',v&0xffffffff))
 u.reg_write(UC_X86_REG_ECX,officer);t.call(0x489f10);word(officer+0xa0,0)
 u.reg_write(UC_X86_REG_ECX,unit);t.call(0x496d90);word(unit+8,1);word(unit+0xc,0)
 assert t.call(0x47a630,officer)==1 and t.call(0x47a630,unit)==1
 worldbase=0x7200000;worldbytes=0x300000;rows=[]
 for troops in [0,1,100,5000,60000]:
  for damage in [0,1,99,100,999,5000,60000,100000]:
   u.mem_write(unit+0x18,struct.pack('<H',troops));u.mem_write(request,bytes(0x80));word(request+0xc,damage)
   frame=d.stack-0x400
   before=bytes(u.mem_read(worldbase,worldbytes));rng=bytes(u.mem_read(0x8a5d44,4));grid=bytes(u.mem_read(0x6fb0000,0x100000));request_before=bytes(u.mem_read(request,0x80))
   u.reg_write(UC_X86_REG_EDI,request);u.reg_write(UC_X86_REG_ESI,unit);u.reg_write(UC_X86_REG_ESP,frame);u.emu_start(0x5b1c60,0x5b1c71,count=10000);assert u.reg_read(UC_X86_REG_EIP)==0x5b1c71
   after=max(0,troops-damage);expected=bytearray(before);struct.pack_into('<H',expected,unit+0x18-worldbase,after)
   delta=u.reg_read(UC_X86_REG_EAX);delta=delta if delta<0x80000000 else delta-0x100000000
   assert delta==after-troops and bytes(expected)==bytes(u.mem_read(worldbase,worldbytes))
   assert rng==bytes(u.mem_read(0x8a5d44,4)) and grid==bytes(u.mem_read(0x6fb0000,0x100000)) and request_before==bytes(u.mem_read(request,0x80))
   rows.append(dict(troops_before=troops,requested_damage=damage,troops_after=after,actual_delta=delta,exact_world_effect=True,grid_rng_request_unchanged=True))
 assert len(rows)==40
 raw=(source/'san11pk.exe').read_bytes();shared=(source/'Media/scenario/Scenario.s11').read_bytes()
 assert hashlib.sha256(raw).hexdigest()=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
 assert hashlib.sha256(shared).hexdigest()=='dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f'
 report=dict(executable_sha256=hashlib.sha256(raw).hexdigest(),shared_sha256=hashlib.sha256(shared).hexdigest(),original_data_mappings=mappings,rows=rows,
  functions=[dict(start=hex(low),end=hex(high),bytes_hex=raw[low-0x400000:high-0x400000].hex()) for low,high in [(0x5b1c60,0x5b1c71),(0x4ae4a0,0x4ae502),(0x4961f0,0x49624b)]],
  explicit_inputs=dict(unit_native_index=0,unit_type8=1,officer_native_index=0,officer_status_a0=0,maximum_troops=60000),
  limits=['Enters validated unit branch5b1c60; full5b1870 lookup/visibility/attacker and target setup not executed','Main result+c is explicit input; original damage generation/hit/critical/RNG and failure semantics unverified','Stop before morale/resource/cache/death/removal/presentation work; lethal troop0 remains allocated in this bounded phase','No Android formula or save changes'])
 encoded=(json.dumps(report,indent=2)+'\n').encode();(out/'unit-damage-apply-native.json').write_bytes(encoded)
 with (out/'unit-damage-apply-native.json.gz').open('wb') as f:
  with gzip.GzipFile(filename='',mode='wb',fileobj=f,mtime=0) as z:z.write(encoded)
 print('PASS original result troop application40; exact3MiB troop debit, coordinates/grid/RNG/request unchanged; full damage/death/command pending')

if __name__=='__main__':main()
