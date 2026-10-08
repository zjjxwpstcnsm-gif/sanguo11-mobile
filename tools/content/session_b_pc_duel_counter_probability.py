#!/usr/bin/env python3
"""Untouched58a5e0 counter chance, no rule/getter/RNG replacement."""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output,table):
 output_guard(installation,output);output_guard(installation,table)
 if output.exists()or table.exists():raise ValueError('Preserve earlier receipt')
 raw=Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes();assert sha(raw)=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38';context=json.loads(raw);d,w,source,geo,_=prepare(installation);baseline=bytes(w.u.mem_read(w.root,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));units=[]
 for unit,case in zip(context['units'],context['cases']):
  p=unit['pointer'];w.u.mem_write(p,bytes.fromhex(case['afterHex']));units.append(p)
 setup=bytes(w.u.mem_read(w.root,0x300000));rows=[]
 for own_native,other_native in [(116,558),(432,660),(660,432),(116,432),(116,660)]:
  for own_troops,other_troops in [(5000,5000),(1000,7000),(1000,6999),(2000,6000),(0,6000),(10000,5000)]:
   for health in [69,70,100]:
    for personality in [2,3]:
     w.u.mem_write(w.root,setup);own=w.call(0x490b00,own_native,receiver=w.root);other=w.call(0x490b00,other_native,receiver=w.root)
     for p,n,t in zip(units,[own_native,other_native],[own_troops,other_troops]):w.u.mem_write(p+12,struct.pack('<3i',n,-1,-1));w.u.mem_write(p+0x18,struct.pack('<H',t))
     w.u.mem_write(other+0x128,bytes([health]));w.u.mem_write(other+0xfc,struct.pack('<i',personality))
     facts=dict(native=other_native,health=health,personality=personality,war=w.call(0x489080,receiver=other)&255,intelligence=w.call(0x489090,receiver=other)&255,treasure=w.call(0x4faa60,other),ruler=bool(w.call(0x488c00,receiver=other)))
     own_strength=w.call(0x58a200,units[0],own,count=10000000);other_strength=w.call(0x58a200,units[1],other,count=10000000);before=bytes(w.u.mem_read(w.root,0x300000));value=w.call(0x58a5e0,own,other,*units,count=10000000);assert before==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));values=[own_native,own_troops,other_troops,own_strength,other_strength,facts['native'],facts['health'],facts['war'],facts['personality'],facts['treasure'],int(facts['ruler']),facts['intelligence'],value]
     rows.append(dict(values=values,currentOpponent=facts))
 w.u.mem_write(w.root,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,contextSha=sha(raw),rows=rows,wholeWorldAndRngPureAndRestored=True,limits=['Declared crews/health/personality/troops on real source people; source registry does not imply playable activation','Original58a5e0 and all native getters unchanged','Normal player picker/fees/campaign/Save/APK remain separate'],completeGoal=False),indent=2)+'\n');table.write_text('# original58a5e0 receipt '+sha(output.read_bytes())+'\n'+''.join('\t'.join(map(str,r['values']))+'\n'for r in rows));print('PASS original counter probability',len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--table',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output,a.table)
