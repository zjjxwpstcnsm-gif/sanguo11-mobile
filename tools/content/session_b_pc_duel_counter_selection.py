#!/usr/bin/env python3
"""Untouched58a7f0 highest counter selection plus original RNG."""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output,table):
 output_guard(installation,output);output_guard(installation,table)
 if output.exists()or table.exists():raise ValueError('Preserve receipt')
 raw=Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes();assert sha(raw)=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38';context=json.loads(raw);d,w,source,geo,_=prepare(installation);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));units=[];rows=[]
 for unit,case in zip(context['units'],context['cases']):
  p=unit['pointer'];w.u.mem_write(p,bytes.fromhex(case['afterHex']));w.call(0x4962b0,0,0,5000,receiver=p);w.u.mem_write(p+0x18,struct.pack('<H',5000));w.u.mem_write(p+0x3c,struct.pack('<hh',80+unit['index'],80));w.u.reg_write(UC_X86_REG_EAX,p);location=w.call(0x4a7530)
  for person in case['declaredCrew']:
   w.call(0x4a0cb0,person['pointer'],location,receiver=0x799895c,count=10000000)
   for injury in range(4):w.call(0x50c690,person['pointer'],injury,1,count=10000000)
  w.call(0x496f40,receiver=p,count=10000000);units.append(p)
 point=d.fixture+0x7800;flag=d.fixture+0x7900;opponent_rows=[];before=bytes(w.u.mem_read(0x7200000,0x300000))
 for side,case in enumerate(context['cases']):
  for actor in case['declaredCrew']:
   for seed in range(32):
    w.u.mem_write(0x8a5d44,struct.pack('<I',seed));selected=w.call(0x58a7f0,actor['pointer'],units[side],units[1-side],count=10000000);native=w.call(0x4883c0,receiver=selected)if selected else -1;after=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];assert before==bytes(w.u.mem_read(0x7200000,0x300000));rows.append([side,actor['nativeId'],seed,native,after])
    w.u.mem_write(0x8a5d44,struct.pack('<I',seed));w.u.mem_write(point,bytes(w.u.mem_read(units[1-side]+0x3c,4)));w.u.mem_write(flag,bytes(4));selected=w.call(0x58b400,units[1-side],units[side],0,0xffffffff,point,flag,0,actor['pointer'],count=10000000);native=w.call(0x4883c0,receiver=selected)if selected else -1;after=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];counter=struct.unpack('<i',w.u.mem_read(flag,4))[0];opponent_rows.append([side,actor['nativeId'],seed,native,after,counter]);assert before==bytes(w.u.mem_read(0x7200000,0x300000))
 w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,contextSha=sha(raw),rows=rows,opponentRows=opponent_rows,fullWorldPureAndRngRestored=True,limits=['Original full58a7f0 with declared source crews5000troops and seeds0..31; not human command UI'],completeGoal=False),indent=2)+'\n');table.write_text('# original58a7f0 SHA'+sha(output.read_bytes())+'\n'+''.join('\t'.join(map(str,r))+'\n'for r in rows)+'# original58b400 opponent\n'+''.join('O\t'+'\t'.join(map(str,r))+'\n'for r in opponent_rows));print('PASS original counter selection',len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--table',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output,a.table)
