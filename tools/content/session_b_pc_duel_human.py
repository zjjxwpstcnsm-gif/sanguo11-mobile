#!/usr/bin/env python3
"""Actual original human special getter/merge and legal source-fighter options.
UI storage/energy/round are declared fixtures; no original rule/RNG replacement.
"""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier evidence')
 d,w,src,geo,people=prepare(installation);p=d.fixture+0x1000;model=d.fixture;view=0xc600000;w.u.mem_map(view,0x1000)
 w.call(0x50ddd0,receiver=p);w.u.mem_write(p,struct.pack('<6I',*[x['pointer']for x in people]));w.u.mem_write(p+0x20,struct.pack('<8i',0,0,0,0,2,0,0,0));w.call(0x50ddd0,receiver=0x8b3740);w.u.mem_write(0x8b3740+0xcc,struct.pack('<I',1));assert w.call(0x50de30,p,receiver=0x8b3740,count=10000000)==1;w.call(0x50ab90,receiver=model);w.u.mem_write(0x8a5d44,struct.pack('<I',23));w.call(0x50c030,receiver=model,count=10000000)
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));template=bytes(w.u.mem_read(model,0x59c));initial_rng=bytes(w.u.mem_read(0x8a5d44,4));rows=[];legal=[]
 for side in range(2):
  for selection in range(-1,9):
   w.u.mem_write(model,template);w.u.mem_write(model+0x230,struct.pack('<I',view));w.u.mem_write(model+0x598,struct.pack('<i',side));w.u.mem_write(view+0x2b0+side*0x19c,struct.pack('<i',selection));before=bytes(w.u.mem_read(model,0x59c));actual=w.call(0x511140,side,receiver=view);actual=actual if actual<0x80000000 else actual-0x100000000;assert actual==selection
   accepted=w.call(0x507610,receiver=model);after=bytes(w.u.mem_read(model,0x59c));assert bool(accepted)==(0<=selection<8);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and initial_rng==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(side=side,selection=selection,originalGetter=actual,accepted=bool(accepted),beforeHex=before.hex(),afterHex=after.hex(),inputFields=list(struct.unpack_from('<4i',after,0x578)),wholeSourceWorldAndRngUnchanged=True))
 for side in range(2):
  for fighter in range(3):
   for spirit in [0,99,100,199,200,299,300]:
    for round_number in [0,14,15,50]:
     w.u.mem_write(model,template);w.u.mem_write(model+0x24+side*0xec+fighter*0x40+8,struct.pack('<i',spirit));w.u.mem_write(model+0x10,struct.pack('<i',round_number));before=bytes(w.u.mem_read(model,0x59c));options=[]
     for move in range(8):
      cost=w.call(0x506670,move,receiver=model);allowed=bool(w.call(0x507cf0,side,fighter,move,receiver=model,count=10000000));availability=w.call(0x506620,side,fighter,move,receiver=model);options.append(dict(nativeMove=move,cost=cost,available=bool(availability),allowed=allowed))
     assert before==bytes(w.u.mem_read(model,0x59c))and baseline==bytes(w.u.mem_read(0x7200000,0x300000))and initial_rng==bytes(w.u.mem_read(0x8a5d44,4));legal.append(dict(side=side,fighter=fighter,spiritFixture=spirit,roundFixture=round_number,options=options,completeModelWorldAndRngUnchanged=True))
 result=dict(source=src,exeSha=EXE_SHA,geography=geo,people=people,humanSelections=rows,legalSourceFixtures=legal,initialModelHex=template.hex(),initialNativeRng=struct.unpack('<I',initial_rng)[0],limits=['Original initialized source sixpeople; teams not normal deployed enemyunits','Human UI choice storage is explicit; actual511140 getter/full507610 remainder executes unchanged; no rule/AI/RNG substituted','Original507cf0/506620/506670 availability/energy/round pure boundaries; energy/round explicit fixture not gameplay progression','Normal human GUI/widget construction, all gear/state mutations/fullcampaign/API/Save/APK remain required'],completeGoal=False)
 output.write_text(json.dumps(result,indent=2)+'\n');print('PASS original human',len(rows),'legalfixtures',len(legal),'options',len(legal)*8,'SHA',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
