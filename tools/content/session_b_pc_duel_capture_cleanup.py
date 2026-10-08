#!/usr/bin/env python3
"""Actual4a93b0/4a5d90 cleanup on declared linked original unit inputs."""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EIP,UC_X86_REG_ESP,UC_X86_REG_ECX
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output,multicrew_only=False,victor_slot=0,display_context=False,cargo_fixture=False):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve receipt')
 r=json.loads(Path('out/session-b/duel-unit-context-source0-v4.json').read_text());d,w,src,geo,unused=prepare(installation);assert src==r['source'];baseline=bytes(w.u.mem_read(w.root,0x300000));initialRng=bytes(w.u.mem_read(0x8a5d44,4));rows=[];item=w.root+0x7777c+30*0x54;displayCalls=[]
 if display_context:
  evidence=json.loads(Path('out/session-b/duel-solo-display-source-v2.json').read_text());f=next(x for x in evidence['functions']if x['address']=='0x5887b0');assert sha(bytes(w.u.mem_read(0x5887b0,0x50)))==f['sha256']
  # Examined screen-position getter only. No deletion/value/RNG callback is
  # omitted:4bccd0,4b0560 and588cb0 still execute all original instructions.
  def point(u,address,size,user):
   sp=u.reg_read(UC_X86_REG_ESP);ret,dest=struct.unpack('<2I',bytes(u.mem_read(sp,8)));u.mem_write(dest,struct.pack('<hh',80,80));u.reg_write(UC_X86_REG_EAX,dest);u.reg_write(UC_X86_REG_ESP,sp+8);u.reg_write(UC_X86_REG_EIP,ret);displayCalls.append(dict(address=hex(address),returnAddress=hex(ret),output=hex(dest),declaredDisplayPoint=[80,80],boundedSha=f['sha256']))
  w.u.hook_add(UC_HOOK_CODE,point,begin=0x5887b0,end=0x5887b0)
 for rank in [80,54,0]:
  for held in [False,True]:
   for solo in ([False]if multicrew_only else [False,True]):
    w.u.mem_write(w.root,baseline);units=[]
    for u,c in zip(r['units'],r['cases']):
     ptr=u['pointer'];w.u.mem_write(ptr,bytes.fromhex(c['afterHex']));w.u.mem_write(ptr+0x18,struct.pack('<H',5000));w.u.mem_write(ptr+0x1a,bytes([100]));w.u.mem_write(ptr+0x3c,struct.pack('<hh',80+u['index'],80));units.append(ptr);w.u.reg_write(UC_X86_REG_EAX,ptr);location=w.call(0x4a7530)
     for person in c['declaredCrew']:w.call(0x4a0cb0,person['pointer'],location,receiver=0x799895c,count=10000000)
    if cargo_fixture:
     # Declared raw resource inputs, labelled by exact offsets until getter
     # meanings are established; no source balance or production stock rewrite.
     w.u.mem_write(units[0]+0x1c,struct.pack('<2I',731,12000));w.u.mem_write(units[1]+0x1c,struct.pack('<2I',997,17000))
    target=r['cases'][1]['declaredCrew'][0]['pointer'];victor=r['cases'][0]['declaredCrew'][victor_slot]['pointer'];w.call(0x48a750,rank,receiver=target)
    if held:w.u.mem_write(item+0x40,struct.pack('<i',558)) # declared original item owner input, no getter/result replacement
    if solo:
     w.u.mem_write(units[1]+0x10,struct.pack('<2i',-1,-1))
     # A genuine one-person formation must not leave original person current9c
     # links claiming that removed deputies still belong to this unit.
     for person in r['cases'][1]['declaredCrew'][1:]:
      home=struct.unpack('<i',w.u.mem_read(person['pointer']+0x98,4))[0];w.call(0x4a0cb0,person['pointer'],home,receiver=0x799895c,count=10000000);assert w.call(0x489220,receiver=person['pointer'])==0xffffffff
    w.u.mem_write(0x8a5d44,struct.pack('<I',23));before=bytes(w.u.mem_read(w.root,0x300000));personBefore=bytes(w.u.mem_read(target,400));itemBefore=bytes(w.u.mem_read(item,84));unitBefore=bytes(w.u.mem_read(units[1],244))
    stage='capture'
    try:
     w.call(0x4a93b0,target,victor,0,receiver=0x799895c,count=10000000);stage='remove';w.call(0x4a5d90,target,victor,*units,receiver=0x799895c,count=10000000)
    except Exception as error:
     ip=w.u.reg_read(UC_X86_REG_EIP);sp=w.u.reg_read(UC_X86_REG_ESP);f=dict(stage=stage,rankFixture=rank,heldBookFixture=held,soloFixture=solo,source=src,exeSha=EXE_SHA,error=repr(error),ip=hex(ip),ecx=hex(w.u.reg_read(UC_X86_REG_ECX)),stackHex=bytes(w.u.mem_read(sp,64)).hex(),priorRows=rows,personBeforeHex=personBefore.hex(),personAfterHex=bytes(w.u.mem_read(target,400)).hex(),wholeGoal=False)
     try:f['instructionHex']=bytes(w.u.mem_read(ip,48)).hex()
     except Exception:pass
     output.with_suffix('.failure.json').write_text(json.dumps(f,indent=2)+'\n');raise
    after=bytes(w.u.mem_read(w.root,0x300000));p=bytes(w.u.mem_read(target,400));it=bytes(w.u.mem_read(item,84));u=bytes(w.u.mem_read(units[1],244));rows.append(dict(rankFixture=rank,heldBookFixture=held,soloFixture=solo,winnerUnitAfterHex=bytes(w.u.mem_read(units[0],244)).hex(),personBeforeHex=personBefore.hex(),personAfterHex=p.hex(),itemBeforeHex=itemBefore.hex(),itemAfterHex=it.hex(),unitBeforeHex=unitBefore.hex(),unitAfterHex=u.hex(),rngAfter=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],changedBytes=[dict(offset=i,before=a,after=b)for i,(a,b)in enumerate(zip(before,after))if a!=b]))
   print('PASS original capture cleanup',rank,held,len(rows),flush=True)
 w.u.mem_write(w.root,baseline);w.u.mem_write(0x8a5d44,initialRng);assert baseline==bytes(w.u.mem_read(w.root,0x300000))and initialRng==bytes(w.u.mem_read(0x8a5d44,4));output.write_text(json.dumps(dict(cargoFixture=cargo_fixture,displayPointFixture=display_context,displayCalls=displayCalls,victorSlot=victor_slot,multicrewOnly=multicrew_only,exeSha=EXE_SHA,source=src,geography=geo,rows=rows,wholeWorldAndRngRestored=True,limits=['Rank/itemowner/solo crew explicit input fixtures, original capture/removal setters unchanged','No original acceptance,damage,RNG replacement; no normal deployment/menu/APK proof'],completeGoal=False),indent=2)+'\n');print('PASS original capture cleanup SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--multicrew-only',action='store_true');p.add_argument('--cargo-fixture',action='store_true');p.add_argument('--display-context',action='store_true');p.add_argument('--victor-slot',type=int,choices=[0,1,2],default=0);a=p.parse_args();inspect(a.installation,a.output,a.multicrew_only,a.victor_slot,a.display_context,a.cargo_fixture)
