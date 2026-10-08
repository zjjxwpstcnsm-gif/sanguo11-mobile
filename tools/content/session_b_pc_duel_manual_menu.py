#!/usr/bin/env python3
"""Original58af20 first human nominee menu through58b0f7, before graphical UI."""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EAX,UC_X86_REG_EIP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve receipt')
 raw=Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes();assert sha(raw)=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38';context=json.loads(raw);d,w,source,geo,_=prepare(installation);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));units=[]
 for unit,case in zip(context['units'],context['cases']):
  p=unit['pointer'];w.u.mem_write(p,bytes.fromhex(case['afterHex']));w.call(0x4962b0,0,0,5000,receiver=p);w.u.mem_write(p+0x18,struct.pack('<H',5000));w.u.mem_write(p+0x3c,struct.pack('<hh',80+unit['index'],80));w.u.mem_write(p+0x24,bytes(4));w.u.reg_write(UC_X86_REG_EAX,p);location=w.call(0x4a7530)
  for person in case['declaredCrew']:
   w.call(0x4a0cb0,person['pointer'],location,receiver=0x799895c,count=10000000)
   for injury in range(4):w.call(0x50c690,person['pointer'],injury,1,count=10000000)
  w.call(0x496f40,receiver=p,count=10000000);units.append(p)
 point=d.fixture+0x7900;w.u.mem_write(point,bytes(w.u.mem_read(units[1]+0x3c,4)));rows=[];calls=[]
 # Avoid nested VM execution inside hooks; source ID maps from known pointers.
 def enter(u,ip,size,user):
  sp=u.reg_read(UC_X86_REG_ESP);actor=struct.unpack('<I',u.mem_read(sp+4,4))[0];native=next(p['nativeId']for c in context['cases']for p in c['declaredCrew']if p['pointer']==actor);calls.append(dict(native=native,seed=struct.unpack('<I',u.mem_read(0x8a5d44,4))[0]))
 def returned(u,ip,size,user):calls[-1].update(chance=u.reg_read(UC_X86_REG_EAX),rngAfter=struct.unpack('<I',u.mem_read(0x8a5d44,4))[0])
 w.u.hook_add(UC_HOOK_CODE,enter,begin=0x58ad60,end=0x58ad60);w.u.hook_add(UC_HOOK_CODE,returned,begin=0x58b0dc,end=0x58b0dc)
 declared=bytes(w.u.mem_read(0x7200000,0x300000))
 for manual in [False,True]:
  for seed in [0,23,0xffffffff]:
   w.u.mem_write(0x7200000,declared)
   if manual:w.call(0x481480,0,receiver=w.call(0x490aa0,2,receiver=w.root))
   w.u.mem_write(0x8a5d44,struct.pack('<I',seed));calls.clear();before=bytes(w.u.mem_read(0x7200000,0x300000))
   try:w.call(0x58af20,units[0],1,0,units[1],point,stop=0x58b0f7,count=10000000)
   except Exception as error:output.with_suffix('.failure.json').write_text(json.dumps(dict(error=repr(error),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid,calls=calls,manual=manual,seed=seed),indent=2)+'\n');raise
   after=bytes(w.u.mem_read(0x7200000,0x300000));rows.append(dict(manualPlayerSlotFixture=manual,seed=seed,candidates=list(calls),rngAfter=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],changedWorldAddresses=[hex(0x7200000+i)for i,(a,b)in enumerate(zip(before,after))if a!=b]));assert len(calls)==3
 w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,rows=rows,fullSourceWorldAndRngRestored=True,limits=['Full original58af20 prefix, actual three roster entries/counter/response/RNG before graphical picker','Declared original units/caches/player slot; no original GUI or ordinary deployment/APK proof','Original candidate temporary display writes preserved in receipt; no replacement of actual response/RNG'],completeGoal=False),indent=2)+'\n');print('PASS original human nominee menu',len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
