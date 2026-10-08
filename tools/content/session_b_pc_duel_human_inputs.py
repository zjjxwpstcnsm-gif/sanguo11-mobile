#!/usr/bin/env python3
"""Actual original pose/swap input getters; declared UI storage, no PC GUI claim."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier receipt')
 raw=corpus.read_bytes();assert sha(raw)=='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426';r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people'];view=0xc600000;w.u.mem_map(view,0x5000);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rows=[]
 for number,case in enumerate(r['cases']):
  for stance in [-1,0,1,2,3,4]:
   for swap in [-1,0,1,2]:
    b=bytearray.fromhex(case['modelHex']);struct.pack_into('<I',b,0x228,view);struct.pack_into('<I',b,0x22c,view);w.u.mem_write(d.fixture,bytes(b));w.u.mem_write(0x8a5d44,struct.pack('<I',23))
    for side in range(2):w.u.mem_write(view+0x558+side*0x448,struct.pack('<i',stance));w.u.mem_write(view+0x1f78+side*4,struct.pack('<i',swap))
    before=bytes(b);w.call(0x507510,1,receiver=d.fixture,count=10000000);w.call(0x5076d0,receiver=d.fixture,count=10000000);after=bytes(w.u.mem_read(d.fixture,0x59c));rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));rows.append(dict(stance=stance,swap=swap,seed=23,nativeRng=rng,beforeHex=before.hex(),afterHex=after.hex(),worldUnchanged=True))
  print('PASS original human inputs',number,flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,cases=rows,limits=['Actual original getters510e40/510e60 and dispatch507510(1)/5076d0 unchanged','UI component storage explicit; no PC GUI constructor or normal human screen proof','Source crew controllers preserved; AI fallback consumes original RNG','Full campaign/Save/API/normalAPK remain incomplete'],completeGoal=False),indent=2)+'\n');print('PASS original human inputs',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
