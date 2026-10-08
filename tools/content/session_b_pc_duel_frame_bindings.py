#!/usr/bin/env python3
"""Actual source binding catalog for portable complete frame differential tests."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier binding receipt')
 d,w,src,geo,people=prepare(installation);actors=[];relations=[]
 for p in people:
  a=p['pointer'];b=bytes(w.u.mem_read(a,0x190));wars=[w.call(0x50c690,a,injury,1,count=10000000)for injury in range(4)]
  actors.append(dict(nativeId=p['nativeId'],pointer=a,personality=struct.unpack_from('<i',b,0xfc)[0],predicate488c00=w.call(0x488c00,receiver=a),treasureBonus=w.call(0x4faa60,a),warByInjury=wars,valid=bool(w.call(0x47a600,a))))
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4))
 for side in range(2):
  for ownSlot in range(3):
   own=people[side*3+ownSlot];a=own['pointer'];ob=bytes(w.u.mem_read(a,0x190));v=struct.unpack('<I',w.u.mem_read(a,4))[0];owner=w.call(struct.unpack('<I',w.u.mem_read(v+0x40,4))[0],receiver=a)
   for otherSlot in range(3):
    other=people[(1-side)*3+otherSlot]
    for slot in range(3):
     p=people[side*3+slot];candidate=p['pointer'];b=bytes(w.u.mem_read(candidate,0x190));v=struct.unpack('<I',w.u.mem_read(candidate,4))[0]
     values=[int(bool(w.call(0x47a600,candidate))),w.call(0x5079e0,candidate),int(bool(w.call(0x4887d0,own['nativeId'],receiver=candidate))),int(bool(w.call(0x488790,own['nativeId'],receiver=candidate))),int(bool(w.call(0x4889e0,other['nativeId'],receiver=candidate))),int(bool(w.call(0x4889e0,own['nativeId'],receiver=candidate))),int(bool(w.call(0x488910,own['nativeId'],receiver=candidate))),int(bool(w.call(0x48bb70,own['nativeId'],receiver=candidate))),int(bool(w.call(0x488c00,receiver=a))),int(bool(w.call(0x4123b0,owner))),int(owner==w.call(struct.unpack('<I',w.u.mem_read(v+0x40,4))[0],receiver=candidate)),b[0xac],int(b[0xe4:0xe8]==ob[0xe4:0xe8]),w.call(0x489f80,own['nativeId'],receiver=candidate)&255]
     relations.append(dict(side=side,ownSlot=ownSlot,otherSlot=otherSlot,slot=slot,values=values))
 assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,actors=actors,relations=relations,worldAndRngPureAfterCacheWarm=True,limits=['Original source actors/relations getter facts, no activation or normal unit admission claim','Source current ability cache warmed before purity fence','No model/frame result answers included as rule implementation','Full production binding/campaign/save/player APK remain incomplete'],completeGoal=False),indent=2)+'\n');print('PASS original frame bindings',len(actors),len(relations),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
