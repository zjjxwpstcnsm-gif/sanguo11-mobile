#!/usr/bin/env python3
"""Original helditem AIbonus explicit owner/type boundary fixtures.
Preserve original rule functions. Synthetic ownership is never an actualplacement.
"""
import argparse,itertools,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve prior receipt')
 d,w,src,geo,people=prepare(installation);actor=people[0]['pointer'];index=w.call(0x491310,actor,receiver=w.root);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));items={n:w.call(0x490b30,n,receiver=w.root)for n in range(100)};rows=[]
 fixtures=[[(n,kind)]for n,kind in itertools.product([0,1,2,3,4,11,12,13],range(5))]+[list(enumerate(kinds))for kinds in itertools.product(range(5),repeat=3)]+[[(11,4),(12,3),(13,2),(0,0),(1,1)]]
 for fixture in fixtures:
  w.u.mem_write(0x7200000,baseline)
  # Declared synthetic ownership/type fixture only. Avoid accidental existing
  # actor items; every original item constructor/validity remains untouched.
  for n,p in items.items():
   if w.call(0x47a630,p):w.u.mem_write(p+0x40,struct.pack('<i',-1))
  for n,kind in fixture:
   p=items[n];assert w.call(0x47a630,p)==1;w.u.mem_write(p+0x38,struct.pack('<i',kind));w.u.mem_write(p+0x40,struct.pack('<i',index))
  before=bytes(w.u.mem_read(0x7200000,0x300000));value=w.call(0x4faa60,actor,count=10000000);assert before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(heldNativeFixture=fixture,originalBonus=value,originalRuleWorldAndRngUnchanged=True))
 w.u.mem_write(0x7200000,baseline);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))
 report=dict(exeSha=EXE_SHA,source=src,geography=geo,actor=people[0],cases=rows,limits=['Nativeitemregistry+validity retained; owner/type values explicitfixtures, not sourceplacements oractualnormalitems','No effect/rule/RNG replacement; original4faa60 fullfunction runs','ActualWorldrestored afterfixtures; sourceinstallation readonly','Fullgear/use/skills/normalDuel/campaign/Save/APKremainrequired'],completeGoal=False)
 output.write_text(json.dumps(report,indent=2)+'\n');print('PASS originaltreasureAIbonus',len(rows),'SHA',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',required=True,type=Path);a=p.parse_args();inspect(a.installation,a.output)
