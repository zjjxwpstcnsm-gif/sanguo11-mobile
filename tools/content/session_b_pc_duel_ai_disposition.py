#!/usr/bin/env python3
"""Untouched4aed40 execution probability and4adb30 prior predicate.
Original source target/actor plus declared mutable AI inputs; no rule/RNG replacement.
"""
import argparse,json,struct,itertools
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EAX,UC_X86_REG_ECX
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output,table):
 output_guard(installation,output);output_guard(installation,table)
 if output.exists()or table.exists():raise ValueError('Preserve receipt')
 d,w,source,geo,_=prepare(installation);target=w.call(0x490b00,558,receiver=w.root);actor=w.call(0x490b00,365,receiver=w.root);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));observed=[];rows=[]
 def see(u,ip,size,user):
  sp=u.reg_read(UC_X86_REG_ESP);observed.append(dict(address=hex(ip),argument=struct.unpack('<I',u.mem_read(sp+4,4))[0],eax=u.reg_read(UC_X86_REG_EAX)))
 for ip in [0x472150,0x4721d0]:w.u.hook_add(UC_HOOK_CODE,see,begin=ip,end=ip)
 for ruler,option,personality,ability,merit,relation in itertools.product([False,True],[-1,0,1,2],[0,1,3,4],[[0]*5,[60,80,100,90,70]],[0,20000,60000],['different-sworn','equal-invalid','spouse','liked']):
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(target+0xa0,struct.pack('<i',0 if ruler else 3));w.u.mem_write(target+0x64,struct.pack('<i',-1));w.u.mem_write(actor+0x64,struct.pack('<i',-1 if relation=='equal-invalid' else 365));w.u.mem_write(actor+0x60,struct.pack('<i',558 if relation=='spouse' else -1));w.u.mem_write(actor+0x6c,struct.pack('<5i',*([558 if relation=='liked' else -1]+[-1]*4)));w.u.mem_write(actor+0xf8,struct.pack('<i',personality));w.u.mem_write(target+0x170,bytes(ability));w.u.mem_write(target+0xae,struct.pack('<H',merit));w.u.mem_write(w.root+0x24,struct.pack('<i',option));before=bytes(w.u.mem_read(0x7200000,0x300000));honor=struct.unpack('<i',w.u.mem_read(actor+0xf0,4))[0];ambitions=[struct.unpack('<i',w.u.mem_read(p+0xf4,4))[0]for p in [target,actor]]
  for seed in [0,4,24]:
   observed.clear();w.u.mem_write(0x8a5d44,struct.pack('<I',seed));actual=w.call(0x4aed40,target,actor,count=10000000);after=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];assert before==bytes(w.u.mem_read(0x7200000,0x300000));rows.append(dict(ruler=ruler,option=option,personality=personality,abilities=ability,merit=merit,relation=relation,honor=honor,targetAmbition=ambitions[0],actorAmbition=ambitions[1],seed=seed,decision=actual,rngAfter=after,calls=observed.copy()))
 w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,rows=rows,fullWorldPureAndRngRestored=True,limits=['Original4aed40 only, explicit source target/actor/ruler/personality/current-byte170/merit/relationship/option inputs','Raw equal-1 sworn values retained; original AI treats equality as no execution, without valid-group restriction','Original settingROOT+24 independent from ageROOT+20 and difficultyROOT+38','No ordinary command/full AI container, costs, menu or APK claimed'],completeGoal=False),indent=2)+'\n');table.write_text('# original4aed40 receipt '+sha(output.read_bytes())+'\n'+''.join('\t'.join(map(str,[int(r['ruler']),r['option'],r['personality'],*r['abilities'],r['merit'],r['relation'],r['honor'],r['targetAmbition'],r['actorAmbition'],r['seed'],r['decision'],r['rngAfter'],next((c['argument']for c in r['calls']if c['address']=='0x4721d0'),-1)]))+'\n'for r in rows));print('PASS original AI execution',len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--table',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output,a.table)
