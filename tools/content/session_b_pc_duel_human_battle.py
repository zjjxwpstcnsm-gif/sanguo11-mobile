#!/usr/bin/env python3
"""Original battle via actual human getters with declared UI storage only."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier receipt')
 d,w,src,geo,people=prepare(installation);view=0xc600000;w.u.mem_map(view,0x5000);baseline=bytes(w.u.mem_read(0x7200000,0x300000));cases=[];inputPointer=d.fixture+0x1000
 for pose in range(4):
  for preferred in [0,4,5,3]:
   w.call(0x50ddd0,receiver=inputPointer);w.u.mem_write(inputPointer,struct.pack('<6I',*[p['pointer']for p in people]));w.u.mem_write(inputPointer+0x20,struct.pack('<8i',0,0,0,0,2,0,0,1));w.call(0x50ddd0,receiver=0x8b3740);w.u.mem_write(0x8b3740+0xcc,struct.pack('<I',1));assert w.call(0x50de30,inputPointer,receiver=0x8b3740,count=10000000)==1;manager=bytes(w.u.mem_read(0x8b3740,0xd0));w.call(0x50ab90,receiver=d.fixture);w.u.mem_write(0x8a5d44,struct.pack('<I',23));w.call(0x50c030,receiver=d.fixture,count=10000000);initial=bytes(w.u.mem_read(d.fixture,0x59c));initialRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];trace=[];selected=-1;terminal=False
   for frame in range(2200):
    before=bytes(w.u.mem_read(d.fixture,0x59c));phase,nextPhase,sub=struct.unpack_from('<3i',before,4);effective=nextPhase if 0<=nextPhase<=12 else phase;effectiveSub=0 if 0<=nextPhase<=12 else sub;inputKind=None
    if effective==3 and effectiveSub in [0,1]:
     active=struct.unpack_from('<i',before,0xe8)[0];selected=preferred if w.call(0x507cf0,0,active,preferred,receiver=d.fixture,count=10000000)else -1
     if selected>=0:w.u.mem_write(view+0x2078,struct.pack('<I',1));w.u.mem_write(d.fixture+0x228,struct.pack('<I',view));inputKind='request'
    if effective==5 and effectiveSub==3:
     w.u.mem_write(view+0x558,struct.pack('<i',pose));w.u.mem_write(d.fixture+0x228,struct.pack('<I',view));w.u.mem_write(d.fixture+0x22c,struct.pack('<I',view));w.call(0x507510,1,receiver=d.fixture,count=10000000);w.u.mem_write(d.fixture+0x228,struct.pack('<I',0));w.u.mem_write(d.fixture+0x22c,struct.pack('<I',0));inputKind='pose'
    if effective==7 and effectiveSub==3 and struct.unpack_from('<i',before,0x598)[0]==0:
     w.u.mem_write(view+0x2b0,struct.pack('<i',selected));w.u.mem_write(d.fixture+0x230,struct.pack('<I',view));inputKind='special'
    value=w.call(0x505e60,1,receiver=d.fixture,count=10000000)
    for offset in [0x228,0x22c,0x230]:w.u.mem_write(d.fixture+offset,struct.pack('<I',0))
    after=bytes(w.u.mem_read(d.fixture,0x59c));assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));trace.append(dict(frame=frame,inputKind=inputKind,pose=pose,selected=selected,modelHex=after.hex(),nativeRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],returnValue=value))
    if value==1:terminal=True;break
   if not terminal:raise ValueError('Human battle did not close')
   cases.append(dict(pose=pose,preferredSpecial=preferred,managerHex=manager.hex(),modelHex=initial.hex(),nativeRng=initialRng,trace=trace,finalManagerHex=bytes(w.u.mem_read(0x8b3740,0xd0)).hex(),terminal=terminal,worldUnchanged=True));print('PASS original human battle',pose,preferred,len(trace),flush=True)
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,geography=geo,people=people,cases=cases,limits=['Complete original numeric rules and real UI getters execute unchanged','Input storage declared; native GUI not constructed, source teams not deployed enemy units','UI references attached only at examined getter branches and removed aftercall','Normal admission/campaign/Save/API/B UI/realAPK still pending'],completeGoal=False),indent=2)+'\n');print('PASS original human battles',len(cases),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
