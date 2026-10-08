#!/usr/bin/env python3
"""Original59fe00 visibility plus58b90b refusal loss instruction boundary.
These declared grids and unit troop inputs are not an original normal menu.
"""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EDI,UC_X86_REG_ESI
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve source receipt')
 d,w,source,geo,_=prepare(installation)
 corpus=Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes();assert sha(corpus)=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38'
 c=json.loads(corpus);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4))
 gridBefore=bytes(w.u.mem_read(0x44e5000,0xa1000));grid=0x44e5878;point=d.fixture+0x8000;visibility=[]
 values=[-32768,-8193,-8192,-29,-28,-1,0,1,199,227,228,8191,8192,32767]
 for x in values:
  for y in values:
   for marked in [0,1,255]:
    w.u.mem_write(0x44e5000,bytes(0xa1000));w.u.mem_write(point,struct.pack('<hh',x,y))
    # All coarse cells same declared value: native bounds/overflow remain native.
    if marked:
     b=bytearray(256*256*10);b[::10]=bytes([marked])*(256*256);w.u.mem_write(grid,bytes(b))
    result=w.call(0x59fe00,point,1);assert rng==bytes(w.u.mem_read(0x8a5d44,4));visibility.append(dict(x=x,y=y,marked=marked,result=result))
 # Actual valid source-unit vtable troop getter, executing unmodified loss block.
 unit=c['units'][1]['pointer'];w.u.mem_write(unit,bytes.fromhex(c['cases'][1]['afterHex']));rows=[]
 for troops in [0,1,19,20,999,5000,5999,6000,6500,6980,7000,10000,65535]:
  for seed in [0,1,2,23,0xffffffff]:
   w.u.mem_write(unit+0x18,struct.pack('<H',troops));before=bytes(w.u.mem_read(0x7200000,0x300000));w.u.mem_write(0x8a5d44,struct.pack('<I',seed));w.u.reg_write(UC_X86_REG_EDI,unit);w.call(0x58b90b,stop=0x58b942)
   loss=-struct.unpack('<i',struct.pack('<I',w.u.reg_read(UC_X86_REG_ESI)))[0];after=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];assert before==bytes(w.u.mem_read(0x7200000,0x300000));rows.append(dict(troops=troops,seed=seed,loss=loss,rngAfter=after))
 w.u.mem_write(0x44e5000,gridBefore);w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,visibility=visibility,refusal=rows,sourceWorldAndRngRestored=True,limits=['Visibility is renderer417120 coarse byte6; declared camera grid, not terrain/water/fire','Loss block58b90b..58b942 only; full handler applies it only non-tactic refusal','No normal menu/action/AP proof or Android integration'],completeGoal=False),indent=2)+'\n');print('PASS original challenge boundaries',len(visibility),len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
