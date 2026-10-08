#!/usr/bin/env python3
"""Original special effect, source trait protection and genuine numeric injury writes."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier original effect receipt')
 raw=corpus.read_bytes();assert sha(raw)=='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426';r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people'];wars=[];protection=[]
 for p in people:
  wars.append([w.call(0x50c690,p['pointer'],injury,1,count=10000000)for injury in range(4)]);protection.append(bool(w.call(0x4890f0,0x20,receiver=p['pointer'])))
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));difficultyValid=bool(w.call(0x47a630,0x7201958));difficulty=struct.unpack('<i',w.u.mem_read(0x7201978,4))[0];rows=[]
 for number,case in enumerate(r['cases']):
  for snapshot in [case['modelHex'],case['trace'][len(case['trace'])//2]['modelHex'],case['trace'][-1]['modelHex']]:
   for side in range(2):
    for move in [0,1,2,4,5,6,7]:
     for seed in [0,23,42,0xffffffff]:
      b=bytearray.fromhex(snapshot);active=struct.unpack_from('<i',b,0xe8+side*0xec)[0];struct.pack_into('<4i',b,0x578,side,active,move,0)
      for slot in range(3):struct.pack_into('<i',b,0x24+side*0xec+slot*64+8,300)
      # Declared player move/energy input, no native effect/probability/RNG patch.
      w.u.mem_write(d.fixture,bytes(b));w.u.mem_write(0x8a5d44,struct.pack('<I',seed));value=w.call(0x50a8a0,receiver=d.fixture,count=10000000);after=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));rows.append(dict(case=number,side=side,move=move,seed=seed,returnValue=value,nativeRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],beforeHex=b.hex(),afterHex=after.hex(),worldUnchanged=True))
  print('PASS original special effect case',number,flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,warByInjury=wars,sourceInjuryProtection=protection,difficultyValid=difficultyValid,difficulty=difficulty,cases=rows,limits=['Original50a8a0 full numeric effect and NULLpresentation writers executed unchanged','Declared move/300SP/seed inputs, not player GUI/admission/possession/activation evidence','Actual source4890f0 mask32 protection getter, semantic skill binding remains required','Retreat3/other startup settings/hero exceptions/normal campaign/Save/API/APK incomplete'],completeGoal=False),indent=2)+'\n');print('PASS original special effects',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
