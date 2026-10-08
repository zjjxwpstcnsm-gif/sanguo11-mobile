#!/usr/bin/env python3
"""Complete original special damage with explicit equipment/buff/setting fixtures."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
CORPUS_SHA='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426'
def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier original damage receipt')
 raw=corpus.read_bytes();assert sha(raw)==CORPUS_SHA;r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people'];baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));originalDifficulty=struct.unpack('<i',w.u.mem_read(0x7201978,4))[0];difficultyValid=bool(w.call(0x47a630,0x7201958));rows=[]
 for number,case in enumerate(r['cases']):
  for snapshot in [case['modelHex'],case['trace'][len(case['trace'])//2]['modelHex'],case['trace'][-1]['modelHex']]:
   for side in range(2):
    for fixture in [None,'power_guard','gear4_defend','gear8_context10','context01']:
     b=bytearray.fromhex(snapshot);own=0x24+side*0xec;other=0x24+(1-side)*0xec;active=struct.unpack_from('<i',b,own+0xc4)[0];enemy=struct.unpack_from('<i',b,other+0xc4)[0];difficulty=originalDifficulty
     if fixture=='power_guard':struct.pack_into('<i',b,own+0xdc,-1);struct.pack_into('<i',b,other+0xe0,6)
     if fixture=='gear4_defend':struct.pack_into('<i',b,own+active*64+0x1c,4);struct.pack_into('<i',b,other+enemy*64+0x10,1)
     if fixture=='gear8_context10':struct.pack_into('<i',b,own+active*64+0x1c,8);struct.pack_into('<i',b,own+0xc8,7);struct.pack_into('<i',b,other+0xc8,8);difficulty=0
     if fixture=='context01':struct.pack_into('<i',b,own+0xc8,-1);struct.pack_into('<i',b,other+0xc8,0);difficulty=0
     w.u.mem_write(d.fixture,bytes(b));w.u.mem_write(0x7201978,struct.pack('<i',difficulty));expectedWorld=bytes(w.u.mem_read(0x7200000,0x300000));facts=[]
     for s in range(2):
      slot=struct.unpack_from('<i',b,0x24+s*0xec+0xc4)[0];actor=w.call(0x505f70,s,slot,receiver=d.fixture);facts.append(w.call(0x4883c0,receiver=actor))
     before=bytes(w.u.mem_read(d.fixture,0x59c))
     for move in range(8):
      w.u.mem_write(d.fixture+0x1000,struct.pack('<4i',side,active,move,0));value=w.call(0x5084a0,d.fixture+0x1000,receiver=d.fixture,count=10000000)
      assert before==bytes(w.u.mem_read(d.fixture,0x59c))and expectedWorld==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
      rows.append(dict(case=number,side=side,slot=active,move=move,fixture=fixture,difficulty=difficulty,difficultyValid=difficultyValid,activeNativeIds=facts,damage=value,modelHex=before.hex(),modelDeclaredWorldAndRngUnchanged=True))
     w.u.mem_write(0x7201978,struct.pack('<i',originalDifficulty));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))
  print('PASS original specialdamage case',number,flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,corpusSha=CORPUS_SHA,cases=rows,limits=['Actual source cached strength relation matrix retained, full initializer still to port','Explicit buffs/gear/defend/context/raw difficulty fixtures, not source placements or normal startup setting proof','Original model/world/RNG calculation unchanged and all declared world fixtures restored','Native special actor exceptions outside six sampled people not covered','Full battle/campaign/Save/API/UI/APK incomplete'],completeGoal=False),indent=2)+'\n');print('PASS original damage',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
