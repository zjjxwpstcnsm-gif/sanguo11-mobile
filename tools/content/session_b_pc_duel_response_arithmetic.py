#!/usr/bin/env python3
"""Untouched58a8a0 arithmetic corpus: actual getters, explicit boundary inputs."""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EBX,UC_X86_REG_ECX,UC_X86_REG_EDI,UC_X86_REG_ESI,UC_X86_REG_ESP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output,table):
 output_guard(installation,output);output_guard(installation,table)
 if output.exists()or table.exists():raise ValueError('Preserve earlier receipt')
 raw=Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes();assert sha(raw)=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38'
 context=json.loads(raw);d,w,source,geo,_=prepare(installation);baseline=bytes(w.u.mem_read(w.root,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));units=[]
 for unit,case in zip(context['units'],context['cases']):
  p=unit['pointer'];w.u.mem_write(p,bytes.fromhex(case['afterHex']));w.u.mem_write(p+0x3c,struct.pack('<hh',80+unit['index'],80));w.u.mem_write(p+0x24,bytes(4));w.u.reg_write(UC_X86_REG_EAX,p);location=w.call(0x4a7530)
  for person in case['declaredCrew']:w.call(0x4a0cb0,person['pointer'],location,receiver=0x799895c,count=10000000)
  w.call(0x496f40,receiver=p,count=10000000);units.append(p)
 setup=bytes(w.u.mem_read(w.root,0x300000));point=d.fixture+0x7900;w.u.mem_write(point,bytes(w.u.mem_read(units[1]+0x3c,4)));stages=[]
 for address in [0x58aa38,0x58ab47,0x58abc6,0x58ac09]:
  def observe(u,ip,size,user):
   sp=u.reg_read(UC_X86_REG_ESP);stages.append(dict(address=hex(ip),stack=list(struct.unpack('<20I',u.mem_read(sp,80))),edi=u.reg_read(UC_X86_REG_EDI),ebx=u.reg_read(UC_X86_REG_EBX),eax=u.reg_read(UC_X86_REG_EAX)))
  w.u.hook_add(UC_HOOK_CODE,observe,begin=address,end=address)
 rows=[];signed=lambda x:x if x<0x80000000 else x-0x100000000
 for left_native,right_native in [(116,558),(432,660),(660,432),(116,660),(116,432)]:
  for personality in range(4):
   for war in [70,110]:
    for status in [0,1]:
     w.u.mem_write(w.root,setup);left=w.call(0x490b00,left_native,receiver=w.root);right=w.call(0x490b00,right_native,receiver=w.root)
     w.u.mem_write(units[0]+12,struct.pack('<3i',left_native,-1,-1));w.u.mem_write(units[1]+12,struct.pack('<3i',right_native,-1,-1));w.u.mem_write(units[1]+0x24,struct.pack('<i',status));w.u.mem_write(right+0xfc,struct.pack('<i',personality));w.u.mem_write(left+0x171,bytes([war]));w.u.mem_write(left+0x128,bytes([100]));w.u.mem_write(right+0x128,bytes([100]))
     before=bytes(w.u.mem_read(w.root,0x300000));w.u.mem_write(0x8a5d44,struct.pack('<I',23));stages.clear();result=w.call(0x58a8a0,left,*units,point,count=10000000);assert before==bytes(w.u.mem_read(w.root,0x300000))
     a=next(s for s in stages if s['address']=='0x58aa38');b=next((s for s in stages if s['address']=='0x58ab47'),None);e=next((s for s in stages if s['address']=='0x58ac09'),None)
     values=[struct.unpack('<H',w.u.mem_read(units[0]+0x18,2))[0],struct.unpack('<H',w.u.mem_read(units[1]+0x18,2))[0],signed(a['stack'][8]),a['stack'][6],a['stack'][10],a['stack'][9],a['edi'],left_native,right_native,personality,0 if b is None else b['stack'][10]-1,0 if b is None else b['edi'],status==1,0 if b is None else signed(b['stack'][7]),0 if e is None else e['eax']!=0,signed(result),0]
     rows.append(dict(values=values,early=b is None,rngAfter=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],stages=list(stages)))
 w.u.mem_write(w.root,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,contextSha=sha(raw),rows=rows,wholeWorldAndRngRestored=True,limits=['Declared crew/current WAR/personality/status boundary inputs, no original rule/getter/RNG substitution','Early exit does not call opening estimator; native actor activation/deployment and ordinary APK remain separate'],completeGoal=False),indent=2)+'\n');table.write_text('# original58a8a0 source '+sha(output.read_bytes())+'\n'+''.join('\t'.join(str(int(v))for v in r['values'])+'\n'for r in rows));print('PASS untouched response arithmetic',len(rows),'early',sum(r['early']for r in rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--table',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output,a.table)
