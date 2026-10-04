#!/usr/bin/env python3
"""Actual cavalry blocked target survival decision; stop before presentation."""
import argparse,gzip,hashlib,json,struct,sys
from pathlib import Path

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args()
 source=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve();out=a.output.resolve()
 if out==source or source in out.parents:raise ValueError('Read-only PC source')
 out.mkdir(parents=True,exist_ok=False);sys.path.insert(0,str(Path(__file__).resolve().parent))
 from test_pc_city_action_costs import CityActionCostsTest
 from pc_original_pe_data import load_original_data
 from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_EIP,UC_X86_REG_EAX
 CityActionCostsTest.setUpClass();t=CityActionCostsTest();d=t.d;u=d.u;mappings=load_original_data(u)
 word=lambda at,v:u.mem_write(at,struct.pack('<I',v&0xffffffff))
 read=lambda at:struct.unpack('<I',u.mem_read(at,4))[0]
 pack=lambda x,y:((y&65535)<<16)|(x&65535)
 unit=d.root+0x169730;officer=d.root+0xc0bc;request=d.stack+0x600;frame=d.stack-0x400
 u.reg_write(UC_X86_REG_ECX,officer);t.call(0x489f10);word(officer+0xa0,0)
 u.reg_write(UC_X86_REG_ECX,unit);t.call(0x496d90);word(unit+8,1);word(unit+0xc,0)
 assert t.call(0x47a630,unit)==1
 target=pack(100,100);planned=pack(99,100);cell=0x6fb0e68+(100*200+100)*20;word(cell,1);u.mem_write(cell+8,struct.pack('<H',0))
 rows=[]
 for troops in [1,100,5000,60000]:
  for damage in sorted({0,max(0,troops-1),troops,troops+1,100000}):
   for blocked in [0,1]:
    u.mem_write(unit+0x18,struct.pack('<H',troops));u.mem_write(request,bytes(0x80));word(request+0xc,damage);word(request+0x5c,target);word(frame+0x28,blocked);word(frame+0x10,planned)
    world=bytes(u.mem_read(0x7200000,0x300000));grid=bytes(u.mem_read(0x6fb0000,0x100000));rng=bytes(u.mem_read(0x8a5d44,4));req=bytes(u.mem_read(request,0x80))
    u.reg_write(UC_X86_REG_ESI,request);u.reg_write(UC_X86_REG_ESP,frame);u.emu_start(0x5957e2,0x59584a,count=10000);assert u.reg_read(UC_X86_REG_EIP)==0x59584a
    expected=target if blocked and troops<=damage else planned
    assert u.reg_read(UC_X86_REG_EAX)==expected and read(frame+0x10)==expected
    assert world==bytes(u.mem_read(0x7200000,0x300000)) and grid==bytes(u.mem_read(0x6fb0000,0x100000)) and rng==bytes(u.mem_read(0x8a5d44,4)) and req==bytes(u.mem_read(request,0x80))
    rows.append(dict(troops=troops,main_damage=damage,blocked=bool(blocked),actor_planned_coordinate=hex(expected),target_original_coordinate=hex(target),lethal_follow_override=bool(blocked and troops<=damage),world_grid_rng_request_unchanged=True))
 raw=(source/'san11pk.exe').read_bytes();shared=(source/'Media/scenario/Scenario.s11').read_bytes()
 assert hashlib.sha256(raw).hexdigest()=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
 assert hashlib.sha256(shared).hexdigest()=='dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f'
 report=dict(executable_sha256=hashlib.sha256(raw).hexdigest(),shared_sha256=hashlib.sha256(shared).hexdigest(),original_data_mappings=mappings,rows=rows,
  functions=[dict(start=hex(low),end=hex(high),bytes_hex=raw[low-0x400000:high-0x400000].hex()) for low,high in [(0x5957e2,0x59584a),(0x490e70,0x490e94)]],
  limits=['Blocked local and main damage are explicit inputs, not complete prior landing-loop or damage-generator execution','Original target-unit lookup/getter execute; planned coordinate is a stack local, not authority position commit','Stop before59584a visibility/presentation/result application/duel/counter/death; no whole tactic claim','No rule RNG consumed by this decision; no Android behavior changes'])
 encoded=(json.dumps(report,indent=2)+'\n').encode();(out/'cavalry-blocked-survival-native.json').write_bytes(encoded)
 with (out/'cavalry-blocked-survival-native.json.gz').open('wb') as f:
  with gzip.GzipFile(filename='',mode='wb',fileobj=f,mtime=0) as z:z.write(encoded)
 print('PASS original blocked survival decision',len(rows),'cases; lethal target selects original target follow coordinate; world/grid/RNG unchanged; authority commit pending')

if __name__=='__main__':main()
