#!/usr/bin/env python3
"""Original58ad60 player candidate estimate with full current source unit caches."""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EDI,UC_X86_REG_ESI,UC_X86_REG_ESP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output,table):
 output_guard(installation,output);output_guard(installation,table)
 if output.exists()or table.exists():raise ValueError('Preserve earlier receipt')
 raw=Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes();assert sha(raw)=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38';context=json.loads(raw);d,w,source,geo,_=prepare(installation);baseline=bytes(w.u.mem_read(w.root,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));units=[]
 for unit,case in zip(context['units'],context['cases']):
  p=unit['pointer'];w.u.mem_write(p,bytes.fromhex(case['afterHex']));w.call(0x4962b0,0,0,5000,receiver=p);w.u.mem_write(p+0x18,struct.pack('<H',5000));w.u.mem_write(p+0x3c,struct.pack('<hh',80+unit['index'],80));w.u.mem_write(p+0x24,bytes(4));w.u.reg_write(UC_X86_REG_EAX,p);location=w.call(0x4a7530)
  for person in case['declaredCrew']:
   w.call(0x4a0cb0,person['pointer'],location,receiver=0x799895c,count=10000000)
   for injury in range(4):w.call(0x50c690,person['pointer'],injury,1,count=10000000)
  w.call(0x496f40,receiver=p,count=10000000);units.append(p)
 currentUnits=[bytes(w.u.mem_read(p,0xf4)).hex()for p in units];point=d.fixture+0x7900;rows=[];stages=[]
 for address in [0x58a9d9,0x58aa38,0x58ab97,0x58adce]:
  def observe(u,ip,size,user):
   sp=u.reg_read(UC_X86_REG_ESP);stages.append(dict(address=hex(ip),eax=u.reg_read(UC_X86_REG_EAX),edi=u.reg_read(UC_X86_REG_EDI),esi=u.reg_read(UC_X86_REG_ESI),stack=list(struct.unpack('<20I',u.mem_read(sp,80)))))
  w.u.hook_add(UC_HOOK_CODE,observe,begin=address,end=address)
 for side,case in enumerate(context['cases']):
  own,enemy=units[side],units[1-side];w.u.mem_write(point,bytes(w.u.mem_read(enemy+0x3c,4)))
  for actor in case['declaredCrew']:
   counters=[w.call(0x58a5e0,actor['pointer'],other['pointer'],own,enemy,count=10000000)for other in context['cases'][1-side]['declaredCrew']]
   for seed in [0,23,0xffffffff]:
    w.u.mem_write(0x8a5d44,struct.pack('<I',seed));before=bytes(w.u.mem_read(w.root,0x300000));stages.clear();value=w.call(0x58ad60,actor['pointer'],own,enemy,point,count=10000000);after=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];assert before==bytes(w.u.mem_read(w.root,0x300000));rows.append(dict(side=side,native=actor['nativeId'],seed=seed,value=value,rngAfter=after,counters=counters,stages=list(stages)))
 w.u.mem_write(w.root,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 output.write_text(json.dumps(dict(explicitEquipment0=True,currentUnits=currentUnits,exeSha=EXE_SHA,source=source,geography=geo,contextSha=sha(raw),rows=rows,wholeWorldPureAndRngRestored=True,limits=['Declared linked original units5000troops/source current crews/caches at80,80/81,80','Original full58ad60/counter/response/gear/relations/RNG preserved','Not original player GUI/deployment or Android ordinary campaign'],completeGoal=False),indent=2)+'\n');table.write_text('# original58ad60 receipt '+sha(output.read_bytes())+'\n'+''.join('\t'.join(map(str,[r['side'],r['native'],r['seed'],r['value'],r['rngAfter'],*r['counters']]))+'\n'for r in rows));print('PASS original human selection',len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--table',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output,a.table)
