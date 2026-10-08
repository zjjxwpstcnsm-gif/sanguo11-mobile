#!/usr/bin/env python3
"""Original configured full battles, then full natural-death campaign callback.
Declared units/menu options/fresh seeds; never replace a battle result or rule.
"""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_EIP,UC_X86_REG_ESP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output,ruler=False):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve receipt')
 print('Loading original source',flush=True);d,w,source,geo,_=prepare(installation)
 baseline=bytes(w.u.mem_read(0x7200000,0x300000));originalRng=bytes(w.u.mem_read(0x8a5d44,4));cases=[];stage='constructors'
 try:
  for f,p in [(0x415400,0x32602b0),(0x477810,0x6ee7888),(0x4f52d0,0x91ba558)]:w.call(f,receiver=p,count=50000000)
  assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and originalRng==bytes(w.u.mem_read(0x8a5d44,4))
  # Actual button handlers/export/transfer, declared UI storage. Default is
  # unknown; this is the explicit supported menu choice difficulty0/death2/life0.
  window=0x16000000;w.u.mem_map(window,0x40000);meta=window+0x20000
  metadata=json.loads(Path('docs/pc-data/scenario-metadata-audit.json').read_text())['sources'][0];assert metadata['sha256']==source['sourceSha256'];w.u.mem_write(meta+0x14,bytes.fromhex(metadata['decoded']['name_bytes'])+b'\0');w.u.mem_write(window+0x11c00,struct.pack('<i',1))
  for control in [280,284,309]:w.call(0x545350,control,receiver=window,count=50000000)
  w.call(0x55c9f0,window+0x214,receiver=0x94109a0,count=50000000);args=[0]*26;args[24]=meta;args[25]=0x9414964;w.call(0x4a4354,*args,receiver=0x799895c,stop=0x4a446b,count=50000000)
  assert [struct.unpack('<i',w.u.mem_read(w.root+o,4))[0]for o in [0x20,0x24,0x38]]==[0,2,0]
  contextRaw=Path('out/session-b/duel-unit-context-source0-v4.json').read_bytes();assert sha(contextRaw)=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38';context=json.loads(contextRaw);assert context['source']==source
  units=[];crew=[]
  if ruler:
   for side,n in enumerate([365,517]):
    ptr=w.call(0x490b00,n,receiver=w.root);assert w.call(0x488c00,receiver=ptr)==1
    context['cases'][side]['declaredCrew']=[dict(nativeId=n,pointer=ptr)]
  for row,case in zip(context['units'],context['cases']):
   p=row['pointer'];w.u.mem_write(p,bytes.fromhex(case['afterHex']));
   if ruler:w.u.mem_write(p+0xc,struct.pack('<3i',case['declaredCrew'][0]['nativeId'],-1,-1))
   w.u.mem_write(p+0x18,struct.pack('<H',5000));w.u.mem_write(p+0x1a,bytes([100]));w.u.mem_write(p+0x3c,struct.pack('<hh',80+row['index'],80));w.call(0x496250,997,receiver=p);w.call(0x496280,17000,receiver=p);assert w.call(0x47a630,p)==1;w.u.reg_write(UC_X86_REG_EAX,p);location=w.call(0x4a7530)
   for person in case['declaredCrew']:w.call(0x4a0cb0,person['pointer'],location,receiver=0x799895c,count=50000000)
   units.append(p);crew.extend(case['declaredCrew'])
  configured=bytes(w.u.mem_read(0x7200000,0x300000));inputPointer=d.fixture+0x1000
  selected=None
  for seed in range(64):
   stage='battle'+str(seed);w.u.mem_write(0x7200000,configured);w.call(0x50ddd0,receiver=inputPointer);assert w.call(0x589f70,inputPointer,*units,crew[0]['pointer'],crew[1 if ruler else 3]['pointer'],count=10000000)==1;w.call(0x50ddd0,receiver=0x8b3740);w.u.mem_write(0x8b3740+0xcc,struct.pack('<i',1));assert w.call(0x50de30,inputPointer,receiver=0x8b3740,count=10000000)==1;w.call(0x50ab90,receiver=d.fixture);w.u.mem_write(0x8a5d44,struct.pack('<I',seed));w.call(0x50c030,receiver=d.fixture,count=10000000)
   for frame in range(2200):
    if w.call(0x505e60,1,receiver=d.fixture,count=10000000)==1:break
   else:raise ValueError('Original battle failed to close')
   manager=bytes(w.u.mem_read(0x8b3740,0xd0));outcomes=list(struct.unpack_from('<6i',manager,0x64));summary=dict(seed=seed,frames=frame+1,winner=struct.unpack_from('<i',manager,0x54)[0],outcomes=outcomes,terminalRng=int.from_bytes(w.u.mem_read(0x8a5d44,4),'little'));cases.append(summary)
   # Retain every actual outcome, including deaths outside this first ordinary
   # officer branch; never relabel or force a chosen casualty.
   dead=[i for i,n in enumerate(outcomes)if n==2]
   if len(dead)==1 and bool(w.call(0x488c00,receiver=crew[(dead[0]//3) if ruler else dead[0]]['pointer']))==ruler:selected=dict(**summary,deadSlot=dead[0],deadNative=crew[(dead[0]//3) if ruler else dead[0]]['nativeId'],managerHex=manager.hex(),modelHex=bytes(w.u.mem_read(d.fixture,0x59c)).hex());break
   if seed%8==0:print('Actual original battle',summary,flush=True)
  if selected is None:raise ValueError('No ordinary native death in bounded64 actual battles; preserve results')
  from session_b_pc_readonly_render_abi import bind_readonly_render_abi
  environmentCalls=bind_readonly_render_abi(w)
  def people():return [dict(nativeId=n,hex=bytes(w.u.mem_read(w.root+0xc0bc+n*0x190,0x190)).hex())for n in range(1100)]
  selected['peopleBefore']=people();selected['unitsBefore']=[bytes(w.u.mem_read(p,0xf4)).hex()for p in units];before=bytes(w.u.mem_read(0x7200000,0x300000));entries=[];omitted=[]
  for address in [0x4acbe0,0x4a9120,0x4aa680,0x4b2820,0x4a5d90,0x4acae0]:
   def observe(u,ip,size,user):
    sp=u.reg_read(UC_X86_REG_ESP);entries.append(dict(address=hex(ip),receiver=hex(u.reg_read(UC_X86_REG_ECX)),stackWords=list(struct.unpack('<8I',u.mem_read(sp,32)))))
   w.u.hook_add(UC_HOOK_CODE,observe,begin=address,end=address)
  display=next(r for r in json.loads(Path('out/session-b/duel-campaign-presentation-source-v2.json').read_text())['functions']if r['address']=='0x588bb0');assert sha(bytes(w.u.mem_read(0x588bb0,0xb0)))==display['sha256']
  def valueDisplay(u,ip,size,user):
   sp=u.reg_read(UC_X86_REG_ESP);words=list(struct.unpack('<4I',u.mem_read(sp,16)));omitted.append(dict(address=hex(ip),arguments=words[1:],sha=display['sha256'],scope='examined void value display only'));u.reg_write(UC_X86_REG_ESP,sp+16);u.reg_write(UC_X86_REG_EIP,words[0])
  w.u.hook_add(UC_HOOK_CODE,valueDisplay,begin=0x588bb0,end=0x588bb0)
  output.with_suffix('.partial.json').write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,cases=cases,selected=selected),indent=2)+'\n');stage='full4d3340';print('Original natural-death callback',selected['deadNative'],'seed',selected['seed'],flush=True);w.call(0x4d3340,count=50000000)
  selected.update(peopleAfter=people(),unitsAfter=[bytes(w.u.mem_read(p,0xf4)).hex()for p in units],entries=entries,omittedValueDisplay=omitted,rngAfter=int.from_bytes(w.u.mem_read(0x8a5d44,4),'little'),changedBytes=sum(a!=b for a,b in zip(before,bytes(w.u.mem_read(0x7200000,0x300000)))))
  assert any(e['address']=='0x4acbe0'for e in entries)and not any(e['address']in ['0x4a9120','0x4b2820']for e in entries)
  w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,originalRng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and originalRng==bytes(w.u.mem_read(0x8a5d44,4))
  output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,geography=geo,contextSha=sha(contextRaw),natives=[p['nativeId']for p in crew],optionsDifficultyDeathLife=[0,2,0],cases=cases,selected=selected,wholeWorldAndRngRestored=True,declaredSoloRulers=ruler,readonlyEnvironmentCalls=environmentCalls,limits=['Original full configured model/terminal/campaign, generated outcome2 not edited','Source units/positions/resources/newgame choices/fresh seeds declared; no normal deployment/menu/human GUI proof','Only documented588bb0 void value display omitted; all death/cleanup/treasure callbacks retained','Original full global turn controller/APK/other sources remain separate'],completeGoal=False),indent=2)+'\n');print('PASS natural native death',sha(output.read_bytes()),flush=True)
 except Exception as e:
  output.with_suffix('.failure.json').write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,stage=stage,cases=cases,error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid),indent=2)+'\n');raise
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--ruler',action='store_true');a=p.parse_args();inspect(a.installation,a.output,a.ruler)
