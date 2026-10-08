#!/usr/bin/env python3
"""Untouched4afd60 modes1/2 with source captive/commander and raw loyalty."""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EAX
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output,table):
 output_guard(installation,output);output_guard(installation,table)
 if output.exists()or table.exists():raise ValueError('Preserve receipt')
 d,w,source,geo,_=prepare(installation);target=w.call(0x490b00,558,receiver=w.root);actor=w.call(0x490b00,365,receiver=w.root);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));rows=[];observed=[];probe=d.fixture+0x7400
 def observe(u,ip,size,user):
  sp=u.reg_read(UC_X86_REG_ESP);observed.append(dict(address=hex(ip),eax=u.reg_read(UC_X86_REG_EAX),stack=list(struct.unpack('<12I',u.mem_read(sp,48)))))
 for address in [0x5ba410,0x4afeb3]:w.u.hook_add(UC_HOOK_CODE,observe,begin=address,end=address)
 actor_ruler=w.call(0x489d40,receiver=actor);old_ruler=w.call(0x489d40,receiver=target)
 inputs=dict(honor=struct.unpack('<i',w.u.mem_read(target+0xf0,4))[0],actorCharm=w.call(0x4890b0,receiver=actor)&255,oldRulerGap=w.call(0x489f80,old_ruler,receiver=target)&255,newRulerGap=w.call(0x489f80,actor_ruler,receiver=target)&255,captiveBonus=15 if w.call(0x488c70,receiver=target) else 0,familyPenalty=15 if w.call(0x48bdf0,old_ruler,receiver=target) else 0,likedPenalty=15 if w.call(0x488910,old_ruler,receiver=target) else 0,dislikedBonus=15 if w.call(0x4889e0,old_ruler,receiver=target) else 0)
 for loyalty in [0,20,50,80,100,120,150,255]:
  w.u.mem_write(target+0xac,bytes([loyalty]));display=w.call(0x4c8720,target,23,count=10000000);fixture=bytes(w.u.mem_read(0x7200000,0x300000))
  for mode in [1,2]:
   probability=w.call(0x5c4f80,target,actor,mode,0,count=10000000);w.u.mem_write(probe,struct.pack('<i',-99));forced=w.call(0x4af7d0,target,actor,mode,probe,receiver=0x799895c,count=10000000);forced_result=struct.unpack('<i',w.u.mem_read(probe,4))[0]
   for seed in range(32):
    w.u.mem_write(0x8a5d44,struct.pack('<I',seed));observed.clear();decision=w.call(0x4afd60,target,actor,mode,0,receiver=0x799895c,count=10000000);after=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];assert fixture==bytes(w.u.mem_read(0x7200000,0x300000));chance=next((r['eax']for r in observed if r['address']=='0x4afeb3'),-1);adjust=next((r['stack'][1:9]for r in observed if r['address']=='0x5ba410'),None);rows.append(dict(loyalty=loyalty,display=display,mode=mode,seed=seed,probability=probability,forced=bool(forced),forcedResult=forced_result,chance=chance,decision=decision,rngAfter=after,adjustmentArguments=adjust))
 w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,target=558,actor=365,inputs=inputs,rows=rows,fullWorldPureAndRngRestored=True,limits=['Original source serving target/commander, explicit raw loyalty0..255 and seeds0..31; source status3 retained','Untouched forced gate/5c4f80/honor scaling/percentage RNG modes1/2; not original human selection or ordinary deployed battle'],completeGoal=False),indent=2)+'\n');table.write_text('# original4afd60 captive receipt '+sha(output.read_bytes())+'\n'+''.join('\t'.join(map(str,[r['loyalty'],r['mode'],r['seed'],r['probability'],int(r['forced']),r['forcedResult'],r['chance'],r['decision'],r['rngAfter']]))+'\n'for r in rows));print('PASS original captive recruitment',len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--table',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output,a.table)
