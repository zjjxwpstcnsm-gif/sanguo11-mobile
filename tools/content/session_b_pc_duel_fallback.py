#!/usr/bin/env python3
"""Actual original fallback decision; declared control inputs remain explicit."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,context,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier receipt')
 raw=context.read_bytes();assert sha(raw)=='acbd258e635d491bf3d1c471fd0d83edbaf30746f6935d0800d9a918aab3bca1';r=json.loads(raw);d,w,src,geo,unused=prepare(installation);assert src==r['source'];baseline=bytes(w.u.mem_read(w.root,0x300000));units=[]
 for u,c in zip(r['units'],r['cases']):
  p=u['pointer'];w.u.mem_write(p,bytes.fromhex(c['afterHex']));w.u.mem_write(p+0x18,struct.pack('<H',5000));w.u.mem_write(p+0x1a,bytes([100]));units.append(p)
 before=bytes(w.u.mem_read(w.root,0x300000));cases=[]
 for direction in range(2):
  for actor in r['cases'][direction]['declaredCrew']:
   individual=[]
   for seed in range(1,11):
    w.u.mem_write(0x8a5d44,struct.pack('<I',seed));picked=w.call(0x58aca0,actor['pointer'],units[direction],units[1-direction],0,0,count=10000000);cases.append(dict(direction=direction,actorNative=actor['nativeId'],seed=seed,pickedPointer=picked,rngAfter=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],thresholds=individual));assert before==bytes(w.u.mem_read(w.root,0x300000))
 w.u.mem_write(w.root,baseline);assert baseline==bytes(w.u.mem_read(w.root,0x300000));output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,contextSha=sha(raw),cases=cases,wholeWorldRestored=True,limits=['Declared valid original units/cohorts5000troops source records unchanged','Actual58aca0 fallback; explicit controlflag0/extra0, fullnormal UI/menu context still required','No rule/RNG replacement, normal command/APK incomplete'],completeGoal=False),indent=2)+'\n');print('PASS original fallback',len(cases),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('context',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.context,a.output)
