#!/usr/bin/env python3
"""Original4b0e20 context ->4afed0 field battle detention, not a salary guess."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,corpus,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve receipt')
 r=json.loads(corpus.read_bytes());assert sha(corpus.read_bytes())=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38';d,w,source,geo,_=prepare(installation);assert source==r['source'];baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));units=[]
 for item,case in zip(r['units'],r['cases']):
  unit=item['pointer'];w.u.mem_write(unit,bytes.fromhex(case['afterHex']));w.u.mem_write(unit+0x18,struct.pack('<H',5000));units.append(unit)
  # Original raw location is88+unit registry index; source getter4a7530 uses EAX.
  from unicorn.x86_const import UC_X86_REG_EAX
  w.u.reg_write(UC_X86_REG_EAX,unit);location=w.call(0x4a7530)
  for p in case['declaredCrew']:w.call(0x4a0cb0,p['pointer'],location,receiver=0x799895c,count=10000000)
 ctx=d.fixture+0x6000
 try:w.call(0x4b0e20,units[1],units[0],0,receiver=ctx,count=10000000)
 except Exception as error:
  from unicorn.x86_const import UC_X86_REG_EIP,UC_X86_REG_ESP
  ip=w.u.reg_read(UC_X86_REG_EIP);output.with_suffix('.failure.json').write_text(json.dumps(dict(error=repr(error),ip=hex(ip),invalid=w.invalid,contextHex=bytes(w.u.mem_read(ctx,16)).hex(),instructionHex=bytes(w.u.mem_read(ip,32)).hex(),stackHex=bytes(w.u.mem_read(w.u.reg_read(UC_X86_REG_ESP),64)).hex()),indent=2)+'\n');raise
 captor=w.call(0x490b00,365,receiver=w.root);home=struct.unpack('<i',w.u.mem_read(captor+0x98,4))[0];city=w.call(0x490d00,home,receiver=w.root);home_people=[];salary=[]
 for native in range(1100):
  p=w.call(0x490b00,native,receiver=w.root)
  if not w.call(0x47a600,p)or struct.unpack('<i',w.u.mem_read(p+0x9c,4))[0]!=home:continue
  b=bytes(w.u.mem_read(p,0x190));home_people.append(dict(nativeId=native,status=struct.unpack_from('<i',b,0xa0)[0],office=struct.unpack_from('<i',b,0xa4)[0],normalMask15=bool(w.call(0x489fe0,15,receiver=p)),captiveMask32=bool(w.call(0x489fe0,32,receiver=p))))
 for n in range(81):
  p=w.call(0x490c10,n,receiver=w.root);salary.append(dict(nativeId=n,valid=bool(w.call(0x47a630,p)),byte35=bytes(w.u.mem_read(p+0x35,1))[0]))
 declared=bytes(w.u.mem_read(0x7200000,0x300000));rows=[];vector=d.fixture+0x6200
 for prisoners in [0,1,2,4]:
  w.u.mem_write(0x7200000,declared)
  for n in [222,235,348,449][:prisoners]:
   p=w.call(0x490b00,n,receiver=w.root);w.u.mem_write(p+0xa0,struct.pack('<i',5));w.call(0x4a0cb0,p,home,receiver=0x799895c,count=10000000)
  w.call(0x47c250,receiver=vector);w.call(0x4cf360,vector,city,15,receiver=0x7999808,count=10000000);rank_cost=w.call(0x49f2a0,vector,receiver=0x780b3cc,count=10000000);roster=[];cursor=struct.unpack('<I',w.u.mem_read(vector+4,4))[0]
  while cursor:
   p=struct.unpack('<I',w.u.mem_read(cursor+8,4))[0];b=bytes(w.u.mem_read(p,0x190));roster.append(dict(nativeId=w.call(0x491310,p,receiver=w.root),home=struct.unpack_from('<i',b,0x98)[0],current=struct.unpack_from('<i',b,0x9c)[0],status=struct.unpack_from('<i',b,0xa0)[0],office=struct.unpack_from('<i',b,0xa4)[0]));cursor=struct.unpack('<I',w.u.mem_read(cursor,4))[0]
  w.call(0x47c100,receiver=vector);w.call(0x47c250,receiver=vector);count=w.call(0x4cf2f0,vector,home,32,receiver=0x7999808,count=10000000);w.call(0x47c100,receiver=vector)
  for troops in [0,199,200,399,400,599,600,999,1000,2000,5000]:
   # Actual city+44 is returned by full486c80; do not confuse district+40.
   raw=w.call(0x491770,city,receiver=w.root);cp=w.root+0x1d8+raw*0x248;w.u.mem_write(cp+0x44,struct.pack('<i',troops));before=bytes(w.u.mem_read(0x7200000,0x300000));nativeTroops=w.call(0x486c80,receiver=city);assert nativeTroops==troops;actual=w.call(0x4afed0,receiver=ctx,count=10000000);assert before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));rows.append(dict(prisonerFixture=prisoners,home=home,troops=troops,normalRankByte35Sum=rank_cost,normalRoster=roster,captiveMask32Count=count,nativeTroops=nativeTroops,decision=actual))
 w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,contextHex=bytes(w.u.mem_read(ctx,12)).hex(),home=home,homePeople=home_people,rankByte35=salary,rows=rows,fullWorldAndRngRestored=True,limits=['Actual original field-unit context with explicit serving unit fixtures and captor original administrative home','Declared prisoner status/location and city troop fixtures; no ordinary defeat/deployment menus','Both-unit/city-defender context and no-owned-city release remain separate'],completeGoal=False),indent=2)+'\n');print('PASS original detention',len(rows),sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('corpus',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.corpus,a.output)
