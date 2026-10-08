#!/usr/bin/env python3
"""Original opening primitives/fullphase1 with checked source getter bindings."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier receipt')
 raw=corpus.read_bytes();assert sha(raw)=='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426';r=json.loads(raw);d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people']
 facts=[]
 for p in people:
  ptr=p['pointer'];vtable=struct.unpack('<I',w.u.mem_read(ptr,4))[0];virtual=struct.unpack('<I',w.u.mem_read(vtable+0x48,4))[0]
  facts.append(dict(nativeId=p['nativeId'],warByInjury=[w.call(0x50c690,ptr,i,1,count=10000000)for i in range(4)],age=w.call(0x488a20,receiver=ptr),raw489080=w.call(0x489080,receiver=ptr)&255,virtual48=virtual,virtual48Value=bool(w.call(virtual,receiver=ptr))))
 relations=[[bool(w.call(0x48d720,q['nativeId'],receiver=p['pointer'])or w.call(0x48bc10,q['nativeId'],receiver=p['pointer'])or w.call(0x4887d0,q['nativeId'],receiver=p['pointer']))for q in people]for p in people]
 settingsValid=bool(w.call(0x47a630,0x7201958));difficulty=struct.unpack('<i',w.u.mem_read(0x7201978,4))[0];life=struct.unpack('<i',w.u.mem_read(0x7201990,4))[0];baseline=bytes(w.u.mem_read(0x7200000,0x300000));rows=[]
 for number,case in enumerate(r['cases']):
  for side in [-1,0,1]:
   for seed in [0,23,42,0xffffffff]:
    b=bytearray.fromhex(case['modelHex']);struct.pack_into('<3i',b,4,1,-1,0);struct.pack_into('<2i',b,0x268,side,-1);w.u.mem_write(d.fixture,bytes(b));w.u.mem_write(0x8a5d44,struct.pack('<I',seed))
    for step in range(10):
     before=bytes(w.u.mem_read(d.fixture,0x59c));rngBefore=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];value=w.call(0x505e60,1,receiver=d.fixture,count=10000000);after=bytes(w.u.mem_read(d.fixture,0x59c));rngAfter=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));rows.append(dict(operation='frame',beforeHex=before.hex(),afterHex=after.hex(),seed=rngBefore,nativeRng=rngAfter,result=value,worldUnchanged=True))
     if struct.unpack_from('<i',after,8)[0]in [2,12]:break
    else:raise ValueError('Opening phase did not leave')
  print('PASS original opening case',number,flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,facts=facts,relations=relations,settingsValid=settingsValid,difficulty=difficulty,life=life,cases=rows,limits=['Declared source teams/selectedside/seed, actual untouched phase1 callbacks and numeric effects','Original VM settings are not certified normal startup options','Sourceworld unchanged, no normal admission/player/campaign/Save/API/APK proof'],completeGoal=False),indent=2)+'\n');print('PASS original opening',len(rows),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
