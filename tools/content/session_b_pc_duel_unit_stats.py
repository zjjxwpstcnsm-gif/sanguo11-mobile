#!/usr/bin/env python3
"""Full original496570 cache calculation; observers never replace rules."""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EBP,UC_X86_REG_ESI,UC_X86_REG_ESP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output,table,elite=False):
 output_guard(installation,output);output_guard(installation,table)
 if output.exists()or table.exists():raise ValueError('Preserve earlier receipt')
 raw=Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes();assert sha(raw)=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38'
 context=json.loads(raw);d,w,source,geo,_=prepare(installation);baseline=bytes(w.u.mem_read(w.root,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));crew=d.fixture+0x7000;dest=d.fixture+0x7100
 constants={hex(a):bytes(w.u.mem_read(a,4)).hex()for a in [0x79cc88,0x77aa88,0x74e718,0x795ae0]};observed=[]
 def observe(u,ip,size,user):
  sp=u.reg_read(UC_X86_REG_ESP);p=u.reg_read(UC_X86_REG_EBP);template=u.reg_read(UC_X86_REG_ESI)
  observed.append(dict(stack=list(struct.unpack('<32I',u.mem_read(sp,128))),partialHex=bytes(u.mem_read(p,36)).hex(),templateHex=bytes(u.mem_read(template,0x60)).hex()))
 w.u.hook_add(UC_HOOK_CODE,observe,begin=0x4969aa,end=0x4969aa)
 rows=[]
 for side,case in enumerate(context['cases']):
  pointers=[p['pointer']for p in case['declaredCrew']];
  if elite:
   vt=struct.unpack('<I',w.u.mem_read(pointers[0],4))[0];owner=w.call(struct.unpack('<I',w.u.mem_read(vt+0x40,4))[0],receiver=pointers[0]);force=w.call(0x490aa0,owner,receiver=w.root);bits=struct.unpack('<Q',w.u.mem_read(force+0x58,8))[0];w.u.mem_write(force+0x58,struct.pack('<Q',bits|sum(1<<t for t in [3,7,11,15])))
  w.u.mem_write(crew,struct.pack('<3I',*pointers))
  for weapon in [0,1,2,3,4]:
   for category in [0,1]:
    for status in [0,1]:
     for troops in [0,1000,5000]:
      cold=bytes(w.u.mem_read(w.root,0x300000));w.call(0x496570,dest,crew,weapon,troops,category,status,0,1,count=10000000);before=bytes(w.u.mem_read(w.root,0x300000));cache_changes=[dict(offset=i,before=a,after=b)for i,(a,b)in enumerate(zip(cold,before))if a!=b];observed.clear();w.call(0x496570,dest,crew,weapon,troops,category,status,0,1,count=10000000);out=bytes(w.u.mem_read(dest,36));assert before==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4)),('replay changed',side,weapon,category,status,troops);assert len(observed)==1,('observation count',len(observed),side,weapon,category,status,troops)
      obs=observed[0];s=obs['stack'];partial=bytes.fromhex(obs['partialHex']);values=[partial[0x18],partial[0x19],s[4],s[5],s[8],s[7],s[9],out[0x1d],out[0x1e]]
      rows.append(dict(side=side,weapon=weapon,category=category,status=status,troops=troops,originalOutputHex=out.hex(),coldCacheChanges=cache_changes,observed=obs,values=values))
 w.u.mem_write(w.root,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 output.write_text(json.dumps(dict(declaredEliteBits=elite,exeSha=EXE_SHA,source=source,geography=geo,contextSha=sha(raw),constants=constants,rows=rows,afterCacheWarmWorldPureAndRngRestored=True,limits=['Direct original496570 eight numeric arguments with declared current source crew/equipment/category/status/troops','Template and coefficients observed from original instructions; no replacement of getters or arithmetic','Normal map terrain A8, current crew relationship binding and ordinary APK remain required'],completeGoal=False),indent=2)+'\n');table.write_text('# original496570 receipt '+sha(output.read_bytes())+'\n'+''.join('\t'.join(map(str,r['values']))+'\n'for r in rows));print('PASS original unit stats',len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--table',type=Path,required=True);p.add_argument('--elite',action='store_true');a=p.parse_args();inspect(a.installation,a.output,a.table,a.elite)
