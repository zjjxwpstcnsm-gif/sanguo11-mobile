#!/usr/bin/env python3
"""Untouched589f70 and50de30 input on original linked source crews."""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier receipt')
 raw=Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes();assert sha(raw)=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38';context=json.loads(raw);d,w,source,geo,_=prepare(installation);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));units=[]
 for unit,case in zip(context['units'],context['cases']):
  p=unit['pointer'];w.u.mem_write(p,bytes.fromhex(case['afterHex']));w.call(0x4962b0,0,0,5000,receiver=p);w.u.mem_write(p+0x18,struct.pack('<H',5000));w.u.mem_write(p+0x3c,struct.pack('<hh',80+unit['index'],80));w.u.reg_write(UC_X86_REG_EAX,p);location=w.call(0x4a7530)
  for person in case['declaredCrew']:w.call(0x4a0cb0,person['pointer'],location,receiver=0x799895c,count=10000000)
  units.append(p)
 declared=bytes(w.u.mem_read(0x7200000,0x300000));rows=[];pointer=d.fixture+0x7400
 for player_fixture in [False,True]:
  for low_health in [False,True]:
   w.u.mem_write(0x7200000,declared)
   if player_fixture:w.call(0x481480,0,receiver=w.call(0x490aa0,2,receiver=w.root))
   if low_health:w.u.mem_write(context['cases'][1]['declaredCrew'][1]['pointer']+0x128,bytes([49]))
   before=bytes(w.u.mem_read(0x7200000,0x300000))
   for own in context['cases'][0]['declaredCrew']:
    for other in context['cases'][1]['declaredCrew']:
     w.call(0x50ddd0,receiver=pointer);assert w.call(0x589f70,pointer,*units,own['pointer'],other['pointer'],count=10000000)==1;input_bytes=bytes(w.u.mem_read(pointer,0xcc));w.call(0x50ddd0,receiver=0x8b3740);w.u.mem_write(0x8b3740+0xcc,struct.pack('<I',1));assert w.call(0x50de30,pointer,receiver=0x8b3740,count=10000000)==1;manager=bytes(w.u.mem_read(0x8b3740,0xd0));native=[]
     for p in struct.unpack_from('<6I',manager):native.append(w.call(0x4883c0,receiver=p)if p else -1)
     w.call(0x50ab90,receiver=d.fixture);w.u.mem_write(0x8a5d44,struct.pack('<I',23));w.call(0x50c030,receiver=d.fixture,count=10000000);model=bytes(w.u.mem_read(d.fixture,0x59c));rng_after=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];w.u.mem_write(0x8a5d44,rng)
     rows.append(dict(playerSlotFixture=player_fixture,lowHealthFixture=low_health,own=own['nativeId'],other=other['nativeId'],inputHex=input_bytes.hex(),managerHex=manager.hex(),nativeCrew=native,contexts=list(struct.unpack_from('<2i',manager,0x28)),controllers=list(struct.unpack_from('<2i',manager,0x38)),modelHex=model.hex(),rngAfter=str(rng_after),scene=struct.unpack_from('<i',manager,0x44)[0]))
     assert before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 w.u.mem_write(0x7200000,baseline);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,contextSha=sha(raw),unitPointers=units,rows=rows,fullWorldAndRngPureAfterDeclaredInputs=True,limits=['Declared linked source0 units/controllers/nominees and optional native14 physicalHP49 fixture','Full original589f70/50de30/health/dislike/scene getters retained; not normal deployment/menu command'],completeGoal=False),indent=2)+'\n');print('PASS original field entry input',len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
