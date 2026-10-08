#!/usr/bin/env python3
"""Original589f70 current scene selection, actual units and declared map objects."""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EIP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve receipt')
 raw=Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes();assert sha(raw)=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38';context=json.loads(raw);d,w,source,geo,_=prepare(installation)
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));grid_base=0x6fb0e68;grid_before=bytes(w.u.mem_read(grid_base,40000*20));rng=bytes(w.u.mem_read(0x8a5d44,4));units=[]
 for unit,case in zip(context['units'],context['cases']):
  p=unit['pointer'];w.u.mem_write(p,bytes.fromhex(case['afterHex']));w.u.mem_write(p+0x3c,struct.pack('<hh',80+unit['index'],80));w.u.reg_write(UC_X86_REG_EAX,p);location=w.call(0x4a7530)
  for person in case['declaredCrew']:w.call(0x4a0cb0,person['pointer'],location,receiver=0x799895c,count=10000000)
  units.append(p)
 facility=w.call(0x490d00,100,receiver=w.root);registry=0x46483b4+100*128;registry_before=bytes(w.u.mem_read(registry,4));w.u.mem_write(registry,struct.pack('<I',facility));pointer=d.fixture+0x7400;rows=[]
 # Clear only declared local geometry/object cells, retaining exact source copy.
 for x in range(77,86):
  for y in range(75,86):
   grid=grid_base+(x*200+y)*20;b=bytearray(w.u.mem_read(grid,20));struct.pack_into('<I',b,0,struct.unpack_from('<I',b)[0]&~3);struct.pack_into('<I',b,4,struct.unpack_from('<I',b,4)[0]&~31);w.u.mem_write(grid,bytes(b))
 local=bytes(w.u.mem_read(grid_base,40000*20))
 for kind in [-1,0,1,2,3,4,5,6,11]:
  for complete in [0,1]:
   for gap in range(4):
    for forest in [False,True]:
     w.u.mem_write(grid_base,local)
     if forest:
      grid=grid_base+(81*200+80)*20;word=struct.unpack('<I',w.u.mem_read(grid+4,4))[0];w.u.mem_write(grid+4,struct.pack('<I',(word&~31)|5))
     if kind>=0:
      w.u.mem_write(facility+8,struct.pack('<3i',kind,3,800));w.u.mem_write(facility+0x14,struct.pack('<i',complete));assert w.call(0x47a630,facility)==1
      grid=grid_base+((81+gap)*200+80+gap//2)*20;b=bytearray(w.u.mem_read(grid,20));struct.pack_into('<I',b,0,(struct.unpack_from('<I',b)[0]&~3)|2);struct.pack_into('<H',b,8,100);w.u.mem_write(grid,bytes(b));assert w.call(0x483b20,receiver=grid)==facility
     before=bytes(w.u.mem_read(0x7200000,0x300000));w.call(0x50ddd0,receiver=pointer)
     try:result=w.call(0x589f70,pointer,*units,context['cases'][0]['declaredCrew'][0]['pointer'],context['cases'][1]['declaredCrew'][0]['pointer'],count=10000000)
     except Exception as error:output.with_suffix('.failure.json').write_text(json.dumps(dict(error=repr(error),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid,kind=kind,complete=complete,gap=gap,forest=forest),indent=2)+'\n');raise
     assert result==1 and before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(kind=kind,complete=complete,gap=gap,forest=forest,scene=struct.unpack('<i',w.u.mem_read(pointer+0x44,4))[0]))
 w.u.mem_write(registry,registry_before);w.u.mem_write(grid_base,grid_before);w.u.mem_write(0x7200000,baseline);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,rows=rows,fullSourceWorldGridAndRngRestored=True,limits=['Original full589f70 retains geometry scans and untouched scene predicates','Declared original unit registry/crew and local facility/forest cells, not normal deployment/menu/APK'],completeGoal=False),indent=2)+'\n');print('PASS original scene',len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
