#!/usr/bin/env python3
"""Execute original seven-cell city registration and camp spatial admission; PC read-only."""
import argparse,hashlib,json,struct
from pathlib import Path
from test_pc_city_action_costs import CityActionCostsTest
from pc_original_pe_data import load_original_data
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_EIP

def main(a):
 a.output.mkdir(parents=True,exist_ok=False)
 CityActionCostsTest.setUpClass();t=CityActionCostsTest();d=t.d;u=d.u;load_original_data(u);building,_,_,_=t.actors()
 read=lambda at:struct.unpack('<I',u.mem_read(at,4))[0]
 word=lambda at,v:u.mem_write(at,struct.pack('<I',v&0xffffffff))
 pack=lambda x,y:(y<<16)|x
 cell=lambda x,y:0x6fb0e68+(x*200+y)*20
 facility=d.root+0x79c54+3*0xd0
 category=read(facility+0xb4);mask=read(facility+0xbc);assert category==1
 terrain=next(i for i in range(32)if mask&(1<<i))
 gridbase=0x6fb0000;gridbytes=0x100000;root=0x7200000;rootbytes=0x300000
 original=bytes(u.mem_read(gridbase,gridbytes));rows=[]
 def axial(x,y):return (y-x//2,x)
 def distance(a,b):
  q,r=axial(*a);s,t=axial(*b);return max(abs(q-s),abs(r-t),abs(q+r-s-t))
 for x,y in [(99,99),(100,100),(100,99),(99,100),(4,4),(195,195)]:
  u.mem_write(gridbase,original)
  for q in range(max(0,x-5),min(200,x+6)):
   for r in range(max(0,y-5),min(200,y+6)):word(cell(q,r),0x300000);word(cell(q,r)+4,terrain)
  word(building+8,0);u.mem_write(building+0x1e,struct.pack('<hh',x,y));frame=d.stack-0x400
  u.reg_write(UC_X86_REG_ESI,building);u.reg_write(UC_X86_REG_ESP,frame)
  u.emu_start(0x5a0169,0x5a01aa,count=1000);assert u.reg_read(UC_X86_REG_EIP)==0x5a01aa
  u.emu_start(0x5a01bc,0x5a01f0,count=1000);assert u.reg_read(UC_X86_REG_EIP)==0x5a01f0
  anchor=read(frame+0x14);record=0x4648350;u.mem_write(record,bytes(128));word(record+0x6e,anchor);word(record+0x64,building)
  world=bytes(u.mem_read(root,rootbytes));rng=bytes(u.mem_read(0x8a5d44,4))
  receiver=0x6ed6eb0;u.reg_write(UC_X86_REG_ECX,receiver);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<II',d.stop,record));u.emu_start(0x416690,d.stop,count=1000000);assert u.reg_read(UC_X86_REG_EIP)==d.stop
  occupied=[]
  for q in range(x-2,x+3):
   for r in range(y-2,y+3):
    if read(cell(q,r))&3:occupied.append((q,r))
  assert len(occupied)==7
  checks=[]
  for q in range(x-4,x+5):
   for r in range(y-4,y+5):
    nearest=min(distance((q,r),o)for o in occupied)
    bit=bool(read(cell(q,r))&0x100000)
    expected=nearest>2
    # Within occupied cells admission additionally rejects actual occupancy.
    assert bit==expected,((x,y),(q,r),nearest,hex(read(cell(q,r))))
    state=bytes(u.mem_read(gridbase,gridbytes))
    u.reg_write(UC_X86_REG_ECX,0);allowed=t.call(0x5a4170,0,facility,pack(q,r))
    assert allowed==int(expected),(q,r,nearest,allowed)
    assert state==bytes(u.mem_read(gridbase,gridbytes))and world==bytes(u.mem_read(root,rootbytes))and rng==bytes(u.mem_read(0x8a5d44,4))
    others={}
    for native in [8,9,16,17,18,19,21,22]:
     definition=d.root+0x79c54+native*0xd0
     actual=t.call(0x5a4170,0,definition,pack(q,r))
     assert actual==int(nearest>0),(native,q,r,nearest,actual)
     assert state==bytes(u.mem_read(gridbase,gridbytes))and world==bytes(u.mem_read(root,rootbytes))and rng==bytes(u.mem_read(0x8a5d44,4))
     others[str(native)]=dict(category=read(definition+0xb4),spatialAdmission=bool(actual))
    checks.append(dict(target=[q,r],footprintDistance=nearest,originalMilitaryFlag=bit,originalCampSpatialAdmission=bool(allowed),wallsAndTraps=others))
  rows.append(dict(center=[x,y],occupied=occupied,checks=checks))
 raw=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版/san11pk.exe').read_bytes()
 report=dict(executableSha256=hashlib.sha256(raw).hexdigest(),originalCalls=['5a0169..5a01f0','416690','4848f0->4847a0','5a4170->5a3460'],campNativeId=3,category=category,terrainRaw=terrain,rows=rows,worldRngUnchanged=True,limits=['Uniform allowed-terrain/radius-bit map and selector0 building record are explicit spatial fixtures','Null unit input to real5a3460 bypasses no code but does not prove force/tech/gold/action legality','No PC full UI/menu or dynamic MOD patch claim; original city spatial guard only'])
 (a.output/'original-city-camp-spacing.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print('PASS original spacing6x81: camp distance<=2 rejected, walls/traps only occupied cells rejected; world/grid query/RNG unchanged')
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);main(p.parse_args())
