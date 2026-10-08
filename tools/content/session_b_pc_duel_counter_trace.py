#!/usr/bin/env python3
"""Unchanged original failing queue call stack and WAR arguments."""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_ECX,UC_X86_REG_EAX
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,frames,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier trace')
 raw=frames.read_bytes();assert sha(raw)=='a586d85804bae89f578610b6ffec16d33a674ddbf9e977226e7c74d1f325740f';line=[v for v in raw.decode().splitlines()if not v.startswith('#')][286].split('\t');before=bytes.fromhex(line[1]);expected=bytes.fromhex(line[2]);d,w,src,geo,people=prepare(installation)
 for p in people:
  for injury in range(4):w.call(0x50c690,p['pointer'],injury,1,count=10000000)
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));w.u.mem_write(d.fixture,before);w.u.mem_write(0x8a5d44,struct.pack('<I',int(line[4])));trace=[]
 def hook(u,address,size,data):
  sp=u.reg_read(UC_X86_REG_ESP)
  if address==0x506160:trace.append(dict(address=hex(address),receiver=hex(u.reg_read(UC_X86_REG_ECX)),stack=list(struct.unpack('<5I',u.mem_read(sp,20)))))
  elif address in [0x509b3e,0x509b4f,0x509b8f]:trace.append(dict(address=hex(address),eax=u.reg_read(UC_X86_REG_EAX),stack=list(struct.unpack('<8I',u.mem_read(sp,32)))))
 h=w.u.hook_add(UC_HOOK_CODE,hook,begin=0x506160,end=0x509b8f);value=w.call(0x505e60,1,receiver=d.fixture,count=10000000);w.u.hook_del(h);after=bytes(w.u.mem_read(d.fixture,0x59c));rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];assert after==expected and value==int(line[3])and rng==int(line[5])and baseline==bytes(w.u.mem_read(0x7200000,0x300000))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=src,people=people,geography=geo,frameFixtureSha=sha(raw),frameIndex=286,trace=trace,beforeHex=before.hex(),afterHex=after.hex(),nativeRng=rng,originalExpectedFrameWorldAndRngMatch=True,limits=['Hooks observe only, no rule or RNG replacement','One actual original whole-frame failure location, not ordinary campaign admission','Full engine/save/UI/APK incomplete'],completeGoal=False),indent=2)+'\n');print('PASS exact original counter stack',len(trace),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('frames',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.frames,a.output)
