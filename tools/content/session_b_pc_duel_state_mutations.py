#!/usr/bin/env python3
"""Original duel mutators/reset on actual initialized/source-backed model snapshots.
No gameplay/AI/RNG substitution. Explicit setter boundary inputs remain fixtures.
"""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
CORPUS_SHA='2bb4e40c692c1f0532fe31ebde2364d13c690c96aaf966f8f12248f9c368d426'

def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve prior native receipt')
 raw=corpus.read_bytes();assert sha(raw)==CORPUS_SHA;r=json.loads(raw);assert r['exeSha']==EXE_SHA
 d,w,src,geo,people=prepare(installation);assert src==r['source']and people==r['people'];baseline=bytes(w.u.mem_read(0x7200000,0x300000));seed=bytes(w.u.mem_read(0x8a5d44,4));rows=[]
 for case_number,case in enumerate(r['cases']):
  template=bytes.fromhex(case['modelHex']);assert len(template)==0x59c
  for side in range(2):
   for index in range(3):
    for function,name,values in [(0x50c320,'health',[-1,0,1,30,100,101]),(0x50c490,'injury',[-1,0,1,2,3,4]),(0x50c200,'stance',[-1,0,1,2,3,4]),(0x50c3c0,'spirit',[-1,0,99,100,200,300,301])]:
     for value in values:
      w.u.mem_write(d.fixture,template);w.call(function,index,value&0xffffffff,receiver=d.fixture+0x24+side*0xec,count=10000000);after=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and seed==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(case=case_number,operation=name,function=hex(function),side=side,index=index,value=value,beforeHex=template.hex(),afterHex=after.hex(),wholeWorldAndRngUnchanged=True))
  samples=[case['modelHex']]+[case['trace'][n]['modelHex']for n in sorted(set([0,len(case['trace'])//2,len(case['trace'])-1]))]
  for snapshot in samples:
   w.u.mem_write(d.fixture,bytes.fromhex(snapshot));assert struct.unpack('<I',w.u.mem_read(d.fixture+0x224,4))[0]==0;w.call(0x507900,receiver=d.fixture,count=10000000);after=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and seed==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(case=case_number,operation='resetQueues',function='0x507900',side=-1,index=-1,value=0,beforeHex=snapshot,afterHex=after.hex(),wholeWorldAndRngUnchanged=True))
  for snapshot in samples:
   for timers in [None,(1,2,1,0,1,2),(0,0,0,1,1,1),(4,5,6,3,4,5),(-1,-1,-1,-1,-1,-1)]:
    before=bytearray.fromhex(snapshot)
    if timers is not None:
     for side in range(2):
      for field,value in zip([0xcc,0xd0,0xd4,0xdc,0xe0,0xe4],timers):struct.pack_into('<i',before,0x24+side*0xec+field,value)
    w.u.mem_write(d.fixture,bytes(before));w.call(0x5077f0,receiver=d.fixture,count=10000000);after=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and seed==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(case=case_number,operation='tickTimers',function='0x5077f0',side=-1,index=-1,value=0,beforeHex=before.hex(),afterHex=after.hex(),wholeWorldAndRngUnchanged=True,timerFixture=timers))
  print('PASS native mutators/reset case',case_number,'rows',len(rows),flush=True)
 report=dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,corpusSha=CORPUS_SHA,cases=rows,limits=['Setter arguments explicit boundary fixtures; no claim illegal negativeHP/energy are normalcommands','Stance/energy setters cover allthree source-valid teammates, HP/injuryselected fighter','Original queue reset fullstate recorded; graphics pointerNULL, actualGUIeffectsnotcertified','Initializer/AI/frame/campaign/full normalAPK still required'],completeGoal=False)
 output.write_text(json.dumps(report,indent=2)+'\n');print('PASS actual native state mutators',len(rows),'SHA',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',required=True,type=Path);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
