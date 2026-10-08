#!/usr/bin/env python3
"""Original full capture of actual source district commander, observes4be2a0."""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EIP,UC_X86_REG_ESP,UC_X86_REG_ECX,UC_X86_REG_EAX
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output,linked=False,cityless=False,ap_fixture=None,nopeople=False,phases=0,camera=False,depart=None):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve original receipt')
 d,w,source,geography,_=prepare(installation);baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));armies=[]
 for native in range(47):
  p=w.call(0x490ad0,native,receiver=w.root);b=bytes(w.u.mem_read(p,0x50));leader=struct.unpack_from('<i',b,12)[0]
  if w.call(0x47a630,p) and 0<=leader<1100:
   person=w.call(0x490b00,leader,receiver=w.root);status=struct.unpack('<i',w.u.mem_read(person+0xa0,4))[0]
   if status==1:armies.append((native,p,leader,person,b))
 assert armies,'No actual source district commander';native,army,leader,target,armyBefore=armies[0];victor=w.call(0x490b00,558,receiver=w.root);assert w.call(0x4883c0,receiver=target)==leader and w.call(0x47a600,target)
 units=[]
 if linked:
  for side,commander in enumerate([leader,558]):
   ptr=w.root+0x169730+0xf4*side;people=[commander]
   armyId=struct.unpack('<i',w.u.mem_read(w.call(0x490b00,commander,receiver=w.root)+0x94,4))[0]
   for n in range(670):
    person=w.call(0x490b00,n,receiver=w.root)
    if n!=commander and w.call(0x47a630,person) and struct.unpack('<i',w.u.mem_read(person+0x94,4))[0]==armyId:people.append(n)
    if len(people)==3:break
   assert len(people)==3
   w.u.mem_write(ptr+8,struct.pack('<i',0));w.u.mem_write(ptr+12,struct.pack('<3i',*people));w.u.mem_write(ptr+0x18,struct.pack('<H',5000));w.u.mem_write(ptr+0x1a,bytes([100]));w.u.mem_write(ptr+0x3c,struct.pack('<hh',80+side,80))
   for n in people:
    person=w.call(0x490b00,n,receiver=w.root);w.u.reg_write(UC_X86_REG_EAX,ptr);location=w.call(0x4a7530);w.call(0x4a0cb0,person,location,receiver=0x799895c,count=10000000)
   units.append(ptr)
 def sites():
  rows=[]
  for site in range(87):
   pointer=w.call(0x490d00,site,receiver=w.root)
   if not w.call(0x47a630,pointer):continue
   table=struct.unpack('<I',w.u.mem_read(pointer,4))[0];armyGetter=struct.unpack('<I',w.u.mem_read(table+0x44,4))[0];currentArmy=w.call(armyGetter,receiver=pointer);governor=w.call(0x4c69a0,pointer,14)
   rows.append(dict(nativeSite=site,army=currentArmy if currentArmy<0x80000000 else currentArmy-0x100000000,governor=governor if governor<0x80000000 else governor-0x100000000))
  return rows
 cityChanges=[]
 if cityless:
  table=struct.unpack('<I',w.u.mem_read(target,4))[0];ownerGetter=struct.unpack('<I',w.u.mem_read(table+0x40,4))[0];owner=w.call(ownerGetter,receiver=target);force=w.call(0x490aa0,owner,receiver=w.root);primary=w.call(0x481240,1,receiver=force)
  for site in range(42):
   pointer=w.call(0x490a10,site,receiver=w.root)
   if not w.call(0x47a630,pointer):continue
   table=struct.unpack('<I',w.u.mem_read(pointer,4))[0];getter=struct.unpack('<I',w.u.mem_read(table+0x44,4))[0]
   if w.call(getter,receiver=pointer)==native:w.call(0x47cbd0,primary,receiver=pointer);cityChanges.append(dict(nativeCity=site,armyBefore=native,armyAfter=primary))
  assert cityChanges
 sitesBefore=sites();table=struct.unpack('<I',w.u.mem_read(target,4))[0];ownerGetter=struct.unpack('<I',w.u.mem_read(table+0x40,4))[0];oldOwner=w.call(ownerGetter,receiver=target);oldForce=w.call(0x490aa0,oldOwner,receiver=w.root);oldPrimary=w.call(0x481240,1,receiver=oldForce)
 noPeopleChanges=[]
 if nopeople:
  for n in range(1100):
   person=w.call(0x490b00,n,receiver=w.root)
   if n!=leader and w.call(0x47a630,person) and struct.unpack('<i',w.u.mem_read(person+0x94,4))[0]==native:w.call(0x4a32f0,person,oldPrimary,receiver=0x799895c,count=10000000);noPeopleChanges.append(n)
 if ap_fixture is not None:w.u.mem_write(army+0x2c,bytes([ap_fixture]))
 armyBefore=bytes(w.u.mem_read(army,0x50));primaryPointer=w.call(0x490ad0,oldPrimary,receiver=w.root);primaryBefore=bytes(w.u.mem_read(primaryPointer,0x50))
 def roster():
  rows=[]
  for n in range(1100):
   person=w.call(0x490b00,n,receiver=w.root);data=bytes(w.u.mem_read(person,0x190));personArmy=struct.unpack_from('<i',data,0x94)[0]
   if personArmy not in [native,oldPrimary]:continue
   rows.append(dict(nativeId=n,army=personArmy,status=struct.unpack_from('<i',data,0xa0)[0],home=struct.unpack_from('<i',data,0x98)[0],current=struct.unpack_from('<i',data,0x9c)[0],valid=bool(w.call(0x47a630,person))))
  return rows
 peopleBefore=roster();unitBefore=[bytes(w.u.mem_read(pointer,0xf4)).hex()for pointer in units]
 before=bytes(w.u.mem_read(target,0x190));calls=[]
 def observe(u,address,size,user):calls.append(dict(ecx=hex(u.reg_read(UC_X86_REG_ECX)),stackHex=bytes(u.mem_read(u.reg_read(UC_X86_REG_ESP),24)).hex()))
 hook=w.u.hook_add(UC_HOOK_CODE,observe,begin=0x4be2a0,end=0x4be2a0)
 try:
  w.call(0x4a93b0,target,victor,0,receiver=0x799895c,count=10000000)
  if linked:w.call(0x4a5d90,target,victor,units[1],units[0],receiver=0x799895c,count=10000000)
 except Exception as e:output.with_suffix('.failure.json').write_text(json.dumps(dict(error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid,armyNative=native,targetNative=leader,personBeforeHex=before.hex(),armyBeforeHex=armyBefore.hex(),successorCalls=calls),indent=2)+'\n');raise
 finally:w.u.hook_del(hook)
 sitesAfter=sites();after=bytes(w.u.mem_read(target,0x190));armyAfter=bytes(w.u.mem_read(army,0x50));worldAfter=bytes(w.u.mem_read(0x7200000,0x300000));rngAfter=bytes(w.u.mem_read(0x8a5d44,4));nextLeader=struct.unpack_from('<i',armyAfter,12)[0]
 result=dict(noPeopleFixture=nopeople,noPeopleChanges=noPeopleChanges,declaredOldApFixture=ap_fixture,primaryArmyBeforeHex=primaryBefore.hex(),primaryArmyAfterHex=bytes(w.u.mem_read(primaryPointer,0x50)).hex(),peopleBefore=peopleBefore,peopleAfter=roster(),unitsBeforeHex=unitBefore,unitsAfterHex=[bytes(w.u.mem_read(pointer,0xf4)).hex()for pointer in units],citylessFixture=cityless,cityChanges=cityChanges,sitesBefore=sitesBefore,sitesAfter=sitesAfter,oldOwner=oldOwner,oldPrimaryArmy=oldPrimary,linkedUnitFixture=linked,source=source,geography=geography,exeSha=EXE_SHA,armyNative=native,targetNative=leader,victorNative=558,personBeforeHex=before.hex(),personAfterHex=after.hex(),armyBeforeHex=armyBefore.hex(),armyAfterHex=armyAfter.hex(),successorNative=nextLeader,successorCalls=calls,rngBefore=struct.unpack('<I',rng)[0],rngAfter=struct.unpack('<I',rngAfter)[0],changedWorldAddresses=[hex(0x7200000+i)for i,(a,b)in enumerate(zip(baseline,worldAfter))if a!=b],limits=['Actual postload native district commander captured by actual source558 through full4a93b0','Declared linked original units/full capture+removal, not original deployment/menu/APK'if linked else'Resident capture fixture, not deployed/menu/APK','No original rule/election/getter/RNG substitution'],completeGoal=False)
 if phases:
  if camera:
   code=bytes(w.u.mem_read(0x415400,0xa4));exe=(installation/'san11pk.exe').read_bytes();assert code==exe[0x15400:0x154a4];beforeProvider=bytes(w.u.mem_read(0x32602b0,0x240));w.call(0x415400,receiver=0x32602b0,count=50000000);result['cameraProvider']=dict(function='0x415400',codeSha=sha(code),beforeHex=beforeProvider.hex(),afterHex=bytes(w.u.mem_read(0x32602b0,0x240)).hex(),limits='Original constructor initialized; actual viewport not certified')
  result['orderedPersonnelPhases']=[]
  for phase in range(phases):
   try:
    for currentFunction in [0x598630,0x59a4b0,0x599cf0]:w.call(currentFunction,count=50000000)
   except Exception as e:result['phaseFailure']=dict(function=hex(currentFunction),index=phase,error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid);output.with_suffix('.failure.json').write_text(json.dumps(result,indent=2)+'\n');raise
   result['orderedPersonnelPhases'].append(dict(index=phase,sites=sites(),people=roster(),armyHex=bytes(w.u.mem_read(army,0x50)).hex(),rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]))
  result['phaseLimits']='Full598630/59a4b0/599cf0 original personnel/duration/reset phases, not whole original player/AI/calendar turn'
 if depart is not None:
  assert linked
  actor=w.call(0x490b00,depart,receiver=w.root);actorBefore=bytes(w.u.mem_read(actor,0x190));beforeDeparture=sites()
  try:value=w.call(0x4a7990,actor,units[0],0xffffffff,0,receiver=0x799895c,count=10000000)
  except Exception as e:result['departureFailure']=dict(error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid,native=depart,beforeHex=actorBefore.hex());output.with_suffix('.failure.json').write_text(json.dumps(result,indent=2)+'\n');raise
  result['departure']=dict(native=depart,originalFunction='0x4a7990',originResult=value,beforeHex=actorBefore.hex(),afterHex=bytes(w.u.mem_read(actor,0x190)).hex(),sitesBefore=beforeDeparture,sitesAfter=sites(),rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],limit='Declared existing original unit destination, not original deployment/menu')
 w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000));result['fullSourceWorldAndRngRestored']=True;output.write_text(json.dumps(result,indent=2)+'\n');print('PASS original district capture',leader,'successor',nextLeader,'SHA',sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--linked',action='store_true');p.add_argument('--cityless',action='store_true');p.add_argument('--ap-fixture',type=int,choices=range(256));p.add_argument('--nopeople',action='store_true');p.add_argument('--phases',type=int,choices=range(4),default=0);p.add_argument('--camera',action='store_true');p.add_argument('--depart',type=int,choices=range(670));a=p.parse_args();inspect(a.installation,a.output,a.linked,a.cityless,a.ap_fixture,a.nopeople,a.phases,a.camera,a.depart)
