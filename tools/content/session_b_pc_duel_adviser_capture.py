#!/usr/bin/env python3
"""Original4a93b0 capture of actual source military adviser from the actual force getter, no replacement guesses."""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EIP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve receipt')
 raw=Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes();assert sha(raw)=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38';context=json.loads(raw);d,w,source,geo,_=prepare(installation);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));units=[]
 for unit,case in zip(context['units'],context['cases']):
  p=unit['pointer'];w.u.mem_write(p,bytes.fromhex(case['afterHex']));w.u.mem_write(p+0x18,struct.pack('<H',5000));w.u.mem_write(p+0x1a,bytes([100]));w.u.mem_write(p+0x3c,struct.pack('<hh',80+unit['index'],80));w.u.reg_write(UC_X86_REG_EAX,p);location=w.call(0x4a7530)
  for person in case['declaredCrew']:w.call(0x4a0cb0,person['pointer'],location,receiver=0x799895c,count=10000000)
  units.append(p)
 target=next(p['pointer']for p in context['cases'][0]['declaredCrew']if p['nativeId']==466);victor=context['cases'][1]['declaredCrew'][0]['pointer'];force=w.call(0x490aa0,2,receiver=w.root);beforeAdvisor=w.call(0x4c4260,force,4);assert beforeAdvisor==466,(beforeAdvisor,hex(force));force_before=bytes(w.u.mem_read(force,0x12c));before=bytes(w.u.mem_read(0x7200000,0x300000));person_before=bytes(w.u.mem_read(target,400));unit_before=bytes(w.u.mem_read(units[0],244))
 try:w.call(0x4a93b0,target,victor,0,receiver=0x799895c,count=10000000);w.call(0x4a5d90,target,victor,units[1],units[0],receiver=0x799895c,count=10000000)
 except Exception as error:output.with_suffix('.failure.json').write_text(json.dumps(dict(error=repr(error),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid,beforeAdvisor=beforeAdvisor,forceBeforeHex=force_before.hex()),indent=2)+'\n');raise
 afterAdvisor=w.call(0x4c4260,force,4);force_after=bytes(w.u.mem_read(force,0x12c));after=bytes(w.u.mem_read(0x7200000,0x300000));person_after=bytes(w.u.mem_read(target,400));unit_after=bytes(w.u.mem_read(units[0],244));assert afterAdvisor==0xffffffff
 result=dict(exeSha=EXE_SHA,source=source,geography=geo,targetNative=466,victorNative=558,originalAdvisorBefore=beforeAdvisor,originalAdvisorAfter=-1,forceBeforeHex=force_before.hex(),forceAfterHex=force_after.hex(),forceChangedOffsets=[hex(i)for i,(a,b)in enumerate(zip(force_before,force_after))if a!=b],personBeforeHex=person_before.hex(),personAfterHex=person_after.hex(),unitBeforeHex=unit_before.hex(),unitAfterHex=unit_after.hex(),rngBefore=struct.unpack('<I',rng)[0],rngAfter=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],changedWorldAddresses=[hex(0x7200000+i)for i,(a,b)in enumerate(zip(before,after))if a!=b],limits=['Actual source military adviser from the actual force getter captured by actual source558 in declared linked units','Both full capture/removal callbacks executed; no adviser election or game/RNG replacement','Not normal original menu or APK gameplay'],completeGoal=False)
 w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));result['fullSourceWorldAndRngRestored']=True;output.write_text(json.dumps(result,indent=2)+'\n');print('PASS original military adviser capture',sha(output.read_bytes()),result['forceChangedOffsets'],flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
