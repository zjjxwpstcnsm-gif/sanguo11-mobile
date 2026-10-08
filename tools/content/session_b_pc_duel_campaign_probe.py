#!/usr/bin/env python3
"""Untouched original campaign callback after full original configured duel."""
import argparse,json,struct
from collections import deque
from unicorn import UC_HOOK_CODE
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EIP,UC_X86_REG_ESP,UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_EDX
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,context,output,numeric_tail=False,seed=23,entries=False,link_members=False,solo_right=False,display_context=False,ruler_right=False,disposition_choice=-1,return_phases=0,ordered_return=False,camera=False,dialog=False,ruler_native=517,initial_slot=0,human_force=None,human_choice=None,callback_count=10000000,human_ui_fixture=False,left_head_slot=0,held_items=False):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve earlier receipt')
 raw=context.read_bytes();assert sha(raw)in ['acbd258e635d491bf3d1c471fd0d83edbaf30746f6935d0800d9a918aab3bca1','49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38'];r=json.loads(raw);print('Loading original source and geography',flush=True);d,w,src,geo,people=prepare(installation);print('Original source loaded',flush=True);assert src==r['source'];baseline=bytes(w.u.mem_read(0x7200000,0x300000));baseline_rng=bytes(w.u.mem_read(0x8a5d44,4));args=d.fixture+0x1000;units=[];crew=[]
 if left_head_slot:
  old=list(r['cases'][0]['declaredCrew']);r['cases'][0]['declaredCrew']=[old[left_head_slot]]+[person for i,person in enumerate(old)if i!=left_head_slot]
 if ruler_right:
  original=r['cases'][1]['declaredCrew'];assert [p['nativeId']for p in original]==[558,14,517]
  r['cases'][1]['declaredCrew']=[original[2],original[1],original[0]]
  if ruler_native!=517:
   if not solo_right:raise ValueError('Alternate actual source ruler requires declared solo unit')
   ptr=w.call(0x490b00,ruler_native,receiver=w.root);b=bytes(w.u.mem_read(ptr,0x190));vt=struct.unpack_from('<I',b)[0];owner=w.call(struct.unpack('<I',w.u.mem_read(vt+0x40,4))[0],receiver=ptr);person=dict(nativeId=ruler_native,pointer=ptr,owner=owner,status=struct.unpack_from('<i',b,0xa0)[0],runtimeRecordSha=sha(b),health=b[0x128],war=w.call(0x489080,receiver=ptr)&255);r['cases'][1]['declaredCrew'][0]=person
 for u,c in zip(r['units'],r['cases']):
  ptr=u['pointer'];w.u.mem_write(ptr,bytes.fromhex(c['afterHex']));w.u.mem_write(ptr+0x18,struct.pack('<H',5000));w.u.mem_write(ptr+0x1a,bytes([100]));w.u.mem_write(ptr+0x3c,struct.pack('<hh',80+u['index'],80));units.append(ptr);crew.extend(c['declaredCrew']);assert w.call(0x47a630,ptr)==1
  if (ruler_right and u['index']==1)or(left_head_slot and u['index']==0):w.u.mem_write(ptr+0xc,struct.pack('<3i',*[p['nativeId']for p in c['declaredCrew']]))
 if link_members:
  for side,unit in enumerate(units):
   w.u.reg_write(UC_X86_REG_EAX,unit);location=w.call(0x4a7530,count=10000000)
   for person in r['cases'][side]['declaredCrew']:
    w.call(0x4a0cb0,person['pointer'],location,receiver=0x799895c,count=10000000);assert w.call(0x489220,receiver=person['pointer'])==side
 if solo_right:
  w.u.mem_write(units[1]+0x10,struct.pack('<2i',-1,-1))
  for person in r['cases'][1]['declaredCrew'][1:]:
   home=struct.unpack('<i',w.u.mem_read(person['pointer']+0x98,4))[0];w.call(0x4a0cb0,person['pointer'],home,receiver=0x799895c,count=10000000);assert w.call(0x489220,receiver=person['pointer'])==0xffffffff
 if solo_right:
  for unit in units:w.call(0x496250,997,receiver=unit);w.call(0x496280,17000,receiver=unit)
 crewAuthority=[]
 for person in crew:
  ptr=person['pointer'];vtable=struct.unpack('<I',w.u.mem_read(ptr,4))[0];owner=w.call(struct.unpack('<I',w.u.mem_read(vtable+0x40,4))[0],receiver=ptr)
  crewAuthority.append(dict(nativeId=person['nativeId'],owner=owner,originalRuler=bool(w.call(0x488c00,receiver=ptr)),originalForceHuman=bool(w.call(0x47a690,receiver=ptr))))
 if ruler_right:assert next(p for p in crewAuthority if p['nativeId']==ruler_native)['originalRuler']
 actors=[r['cases'][s]['declaredCrew'][initial_slot]['pointer']for s in range(2)];w.call(0x50ddd0,receiver=args);assert w.call(0x589f70,args,*units,*actors,count=10000000)==1
 w.call(0x50ddd0,receiver=0x8b3740);w.u.mem_write(0x8b3740+0xcc,struct.pack('<I',1));assert w.call(0x50de30,args,receiver=0x8b3740,count=10000000)==1
 w.call(0x50ab90,receiver=d.fixture);w.u.mem_write(0x8a5d44,struct.pack('<I',seed));w.call(0x50c030,receiver=d.fixture,count=10000000);frames=0
 for frames in range(2200):
  result=w.call(0x505e60,1,receiver=d.fixture,count=10000000)
  if result==1:break
 else:raise ValueError('Original model did not reach terminal')
 if human_force is not None:
  # Declared postbattle control fixture changes only the original controller,
  # not the natural model/terminal outcome, HP, actor or native RNG.
  force=w.call(0x490aa0,human_force,receiver=w.root);w.call(0x481480,0,receiver=force)
 manager=bytes(w.u.mem_read(0x8b3740,0xd0));before=bytes(w.u.mem_read(0x7200000,0x300000));rngBefore=bytes(w.u.mem_read(0x8a5d44,4));facts=dict(declaredLeftHeadSlot=left_head_slot,declaredHumanForce=human_force,declaredHumanChoice=human_choice,declaredInitialSlotFixture=initial_slot,crewAuthority=crewAuthority,rulerRightCrewFixture=ruler_right,declaredRulerNative=ruler_native,soloRightFixture=solo_right,displayPointFixture=display_context,linkedOriginalMembership=link_members,seed=seed,numericTailOnly=numeric_tail,exeSha=EXE_SHA,source=src,geography=geo,contextSha=sha(raw),crew=crew,unitPointers=units,unitBefore=[bytes(w.u.mem_read(p,0xf4)).hex()for p in units],managerHex=manager.hex(),modelHex=bytes(w.u.mem_read(d.fixture,0x59c)).hex(),frames=frames+1,rngBefore=struct.unpack('<I',rngBefore)[0],completeGoal=False)
 def person_records():
  return [dict(nativeId=p['nativeId'],hex=bytes(w.u.mem_read(p['pointer'],0x190)).hex())for p in crew]
 def administrative_records():
  print('Original terminal reached; collecting registry snapshots',flush=True)
  assert w.call(0x490b00,0,receiver=w.root)==w.root+0xc0bc and w.call(0x490b00,1099,receiver=w.root)==w.root+0xc0bc+1099*0x190
  return dict(people=[dict(nativeId=i,pointer=w.root+0xc0bc+i*0x190)for i in range(1100)],armies=[dict(nativeId=i,pointer=w.call(0x490ad0,i,receiver=w.root))for i in range(47)],sites=[dict(nativeId=i,pointer=w.call(0x490d00,i,receiver=w.root))for i in range(87)])
 def administrative_bytes(rows):
  return {kind:[dict(nativeId=row['nativeId'],pointer=row['pointer'],hex=bytes(w.u.mem_read(row['pointer'],length)).hex())for row in entries]for kind,entries in rows.items()for length in [dict(people=0x190,armies=0x100,sites=0x100)[kind]]}
 # Held-owner declarations are only fixture inputs after an untouched natural
 # terminal. Resolve actual manager roster, never the initially declared order.
 item_rows=[]
 if held_items:
  native_by_pointer={p['pointer']:p['nativeId'] for p in crew}
  captured=[native_by_pointer[struct.unpack_from('<I',manager,4*i)[0]]for i in range(6)if struct.unpack_from('<i',manager,0x64+4*i)[0]==1]
  facts['naturalCapturedNatives']=captured
  output.with_suffix('.partial.json').write_text(json.dumps(facts,indent=2)+'\n')
  if not captured:raise ValueError('Natural battle has no captured recipient fixture target')
  target_native=captured[0]
  target_pointer=w.call(0x490b00,target_native,receiver=w.root)
  if w.call(0x488c00,receiver=target_pointer):raise ValueError('Only ordinary serving target callback supported by this fixture')
  for native_item in [0,30,42]:
   ptr=w.root+0x7777c+native_item*0x54
   w.u.mem_write(ptr+0x40,struct.pack('<i',target_native))
   item_rows.append(dict(nativeItemId=native_item,pointer=ptr,declaredHolderNative=target_native,beforeHex=bytes(w.u.mem_read(ptr,0x54)).hex()))
  facts['declaredHeldItems']=item_rows
 facts['personBefore']=person_records()
 if ruler_right or entries:
  administrative=administrative_records()
  if ruler_right:facts['administrativeBefore']=administrative_bytes(administrative)
 if ruler_right:
  facts['forcesBefore']=[dict(nativeId=i,pointer=w.call(0x490aa0,i,receiver=w.root))for i in range(47)]
  for row in facts['forcesBefore']:row['hex']=bytes(w.u.mem_read(row['pointer'],0x12c)).hex()
 output.with_suffix('.partial.json').write_text(json.dumps(facts,indent=2)+'\n');winner=struct.unpack_from('<i',manager,0x54)[0];function=0x4d3340 if 0<=winner<=1 else 0x4d3260
 omitted=[]
 if numeric_tail:
  # Examined void value-display wrapper only; never skip its numeric setter.
  evidence=json.loads(Path('out/session-b/duel-campaign-presentation-source-v2.json').read_text())
  f=next(v for v in evidence['functions']if v['address']=='0x588bb0')
  assert sha(bytes(w.u.mem_read(0x588bb0,0xb0)))==f['sha256']
  def presentation(u,address,size,user):
   sp=u.reg_read(UC_X86_REG_ESP);words=struct.unpack('<4I',bytes(u.mem_read(sp,16)));omitted.append(dict(address=hex(address),returnAddress=hex(words[0]),arguments=list(words[1:]),boundedSha=f['sha256'],scope='void value-display only; no native GUI/render/audio proof'));u.reg_write(UC_X86_REG_ESP,sp+16);u.reg_write(UC_X86_REG_EIP,words[0])
  display_hook=w.u.hook_add(UC_HOOK_CODE,presentation,begin=0x588bb0,end=0x588bb0)
 if camera:
  code=bytes(w.u.mem_read(0x415400,0xa4));exe=(installation/'san11pk.exe').read_bytes();assert code==exe[0x15400:0x154a4];w.call(0x415400,receiver=0x32602b0,count=50000000);facts['cameraProvider']=dict(function='0x415400',codeSha=sha(code),limit='Original constructor initialized; actual viewport not certified')
 if dialog:
  code=bytes(w.u.mem_read(0x73d5b0,10));assert code==bytes.fromhex('b958a51b09e8167ddbff');registry=bytes(w.u.mem_read(0x73bdd0,10));assert registry[:6]==bytes.fromhex('b98878ee06e8')and 0x73bdda+struct.unpack_from('<i',registry,6)[0]==0x477810;prior=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));w.call(0x477810,receiver=0x6ee7888,count=50000000);w.call(0x4f52d0,receiver=0x91ba558,count=50000000);assert prior==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));facts['dialogProvider']=dict(originalInitializer='0x73d5b0',constructor='0x4f52d0',registryInitializer='0x73bdd0',registryConstructor='0x477810',registryCodeSha=sha(registry),codeSha=sha(code),worldAndRngPure=True,limit='Original constructors only; no GUI/render acceptance')
 if display_context or (ordered_return and not camera):
  evidence=json.loads(Path('out/session-b/duel-solo-display-source-v2.json').read_text());pointSource=next(x for x in evidence['functions']if x['address']=='0x5887b0');assert sha(bytes(w.u.mem_read(0x5887b0,0x50)))==pointSource['sha256']
  def point(u,address,size,user):
   sp=u.reg_read(UC_X86_REG_ESP);ret,dest=struct.unpack('<2I',u.mem_read(sp,8));u.mem_write(dest,struct.pack('<hh',80,80));u.reg_write(UC_X86_REG_EAX,dest);u.reg_write(UC_X86_REG_ESP,sp+8);u.reg_write(UC_X86_REG_EIP,ret);omitted.append(dict(address=hex(address),boundedSha=pointSource['sha256'],scope='declared screen point fixture only; all original numeric deletion callbacks retained'))
  w.u.hook_add(UC_HOOK_CODE,point,begin=0x5887b0,end=0x5887b0)
 entryFacts=[];entryHooks=[]
 if human_choice is not None:
  if human_force is None or not entries or disposition_choice>=0:raise ValueError('Human menu requires original human force and entry observations; cannot overwrite AI decisions')
  facts['declaredHumanUiInputs']=[]
  if human_ui_fixture:
   facts['declaredHumanUiDescriptorFixture']=True
   def descriptor_ui(u,ip,size,user):
    sp=u.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',u.mem_read(sp,4))[0]
    # UI construction/caption only. Native4b0140/4b01b0 masks and the
    # untouched4afd60/4add50 selection result rules are never omitted.
    pop=12 if ip==0x68e610 else 4
    omitted.append(dict(address=hex(ip),scope='declared UI creation or caption omission; no original GUI acceptance',returnAddress=hex(ret)))
    u.reg_write(UC_X86_REG_EAX,1 if ip==0x68e610 else 0);u.reg_write(UC_X86_REG_ESP,sp+4+pop);u.reg_write(UC_X86_REG_EIP,ret)
   for ip in [0x68e610,0x572840]:w.u.hook_add(UC_HOOK_CODE,descriptor_ui,begin=ip,end=ip)

  def human_menu(u,ip,size,user):
   sp=u.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',u.mem_read(sp,4))[0]
   mask=struct.unpack('<I',u.mem_read(sp+0x1c,4))[0]
   if mask<1 or mask>15 or not mask&(1<<human_choice):raise ValueError('Original human menu mask rejects choice')
   facts['declaredHumanUiInputs'].append(dict(originalBoundary=hex(ip),legalMask=mask,declaredChoice=human_choice,returnAddress=hex(ret),scope='UI choice only; all original probabilities and mutations retained'))
   u.reg_write(UC_X86_REG_EAX,human_choice);u.reg_write(UC_X86_REG_ESP,sp+4);u.reg_write(UC_X86_REG_EIP,ret)
  w.u.hook_add(UC_HOOK_CODE,human_menu,begin=0x4d7d50,end=0x4d7d50)

 if disposition_choice>=0:
  if disposition_choice not in [0,1,2,3]or not entries:raise ValueError('Declared choice and original entry observation required')
  def select_disposition(u,ip,size,user):
   sp=u.reg_read(UC_X86_REG_ESP);collector=sp+0x1728;destination=struct.unpack('<I',u.mem_read(collector+12,4))[0];has_city=struct.unpack('<I',u.mem_read(destination+8,4))[0]!=0
   for side in range(2):
    for slot in range(3):
     if struct.unpack_from('<i',manager,0x64+12*side+4*slot)[0]!=1:continue
     actual_pointer=struct.unpack_from('<I',manager,4*(side*3+slot))[0];person=next(p for p in crew if p['pointer']==actual_pointer);native=person['nativeId'];ruler=next(p['originalRuler']for p in crewAuthority if p['nativeId']==native);mask=12 if ruler and has_city else 15
     if not mask&(1<<disposition_choice):raise ValueError('Original human initial mask rejects declared choice')
     if disposition_choice==0:
      # Selection is a declared input; original admission must be compared separately.
      facts['recruitmentAdmissionLimit']='Declared recruitment selection after original AI; original human4afd60 admission not executed by this input hook'
     u.mem_write(sp+0x1738+4*native,struct.pack('<i',disposition_choice))
   facts['declaredDispositionInput']=disposition_choice;facts['dispositionInputLimit']='Declared valid selection after original AI chooser, not execution of original human GUI/selection rules'
  w.u.hook_add(UC_HOOK_CODE,select_disposition,begin=0x4b29d9,end=0x4b29d9)
 if entries:
  def loyalty_return(u,ip,size,user):
   for row in reversed(entryFacts):
    if row['address']=='0x4a75a0'and 'resultRaw'not in row:
     ptr=next(x['pointer']for x in administrative['people']if x['nativeId']==row['personNative']);row['resultRaw']=bytes(u.mem_read(ptr+0xac,1))[0];break
  if ruler_right:entryHooks.append(w.u.hook_add(UC_HOOK_CODE,loyalty_return,begin=0x4b7c2c,end=0x4b7c2c))
  for address in [0x4b2820,0x4b0e20,0x4b1950,0x4a9120,0x4acbe0,0x4a92c0,0x4a93b0,0x4a8440,0x4b80a0,0x4b9080,0x4b78f0,0x4a75a0,0x4a2cb0,0x4bbaa0,0x4a5d90,0x4ae4a0,0x4b27f0,0x4b2380,0x4b03d0,0x4b29d9,0x4ad9a0,0x4afd60,0x4aed40]:
   def entry(u,ip,size,user):
    sp=u.reg_read(UC_X86_REG_ESP);row=dict(address=hex(ip),receiver=hex(u.reg_read(UC_X86_REG_ECX)),stackWords=list(struct.unpack('<9I',bytes(u.mem_read(sp,36)))))
    by_pointer={x['pointer']:x['nativeId'] for x in administrative['people']}
    if ip in [0x4b03d0,0x4b2380]:
     collector=u.reg_read(UC_X86_REG_ECX);actor=struct.unpack('<I',u.mem_read(collector+4,4))[0]
     row['actualAiActorPointer']=actor;row['actualAiActorNative']=by_pointer.get(actor)
    if ip in [0x4afd60,0x4aed40]:
     row['actualTargetNative']=by_pointer.get(row['stackWords'][1]);row['actualActorNative']=by_pointer.get(row['stackWords'][2])
    if ip==0x4ad9a0:
     context=u.reg_read(UC_X86_REG_ECX);side=row['stackWords'][1];obj=struct.unpack('<I',u.mem_read(context+4*side,4))[0]
     row['declaredContextSide']=side;row['actualContextObjectPointer']=obj;row['actualObjectUnitIndex']=next((i for i,p in enumerate(units)if p==obj),None)
    if ip==0x4a75a0:
     person,force,weighted=row['stackWords'][1:4];row['personNative']=next(x['nativeId']for x in administrative['people']if x['pointer']==person);row['personHex']=bytes(u.mem_read(person,0x190)).hex();row['forceHex']=bytes(u.mem_read(force,0x12c)).hex();row['weighted']=weighted
    if ip==0x4b1950:
     wrapper=row['stackWords'][2];row['placeWrapperHex']=bytes(u.mem_read(wrapper,32)).hex()
    if ip==0x4b29d9:row['actualDispositionByNative']=list(struct.unpack('<1100i',u.mem_read(sp+0x1738,4400)))
    entryFacts.append(row)
   entryHooks.append(w.u.hook_add(UC_HOOK_CODE,entry,begin=address,end=address))
 recent=deque(maxlen=150)
 def trace(u,address,size,user):
  recent.append(dict(ip=hex(address),esp=hex(u.reg_read(UC_X86_REG_ESP)),eax=hex(u.reg_read(UC_X86_REG_EAX)),ecx=hex(u.reg_read(UC_X86_REG_ECX)),edx=hex(u.reg_read(UC_X86_REG_EDX)),code=bytes(u.mem_read(address,size)).hex()))
 hook=None if entries else w.u.hook_add(UC_HOOK_CODE,trace)
 print('Original campaign callback entering',hex(function),flush=True)
 try:w.call(function,count=callback_count)
 except Exception as error:
  ip=w.u.reg_read(UC_X86_REG_EIP);sp=w.u.reg_read(UC_X86_REG_ESP);failure=dict(**facts,entryFacts=entryFacts,omittedPresentation=omitted,function=hex(function),error=repr(error),nativeIp=hex(ip),invalid=w.invalid,stackHex=bytes(w.u.mem_read(sp,96)).hex(),recentInstructions=list(recent),worldBeforeSha=sha(before),worldAfterSha=sha(bytes(w.u.mem_read(0x7200000,0x300000))))
  try:failure['instructionHex']=bytes(w.u.mem_read(ip,32)).hex()
  except Exception:pass
  output.with_suffix('.failure.json').write_text(json.dumps(failure,indent=2)+'\n');raise
 if hook is not None:w.u.hook_del(hook)
 after=bytes(w.u.mem_read(0x7200000,0x300000));facts.update(personAfter=person_records(),entryFacts=entryFacts,observerMode="targeted-entries"if entries else "every-instruction",omittedPresentation=omitted,function=hex(function),worldBeforeSha=sha(before),worldAfterSha=sha(after),changedBytes=[dict(offset=i,before=a,after=b)for i,(a,b)in enumerate(zip(before,after))if a!=b],unitAfter=[bytes(w.u.mem_read(p,0xf4)).hex()for p in units],rngAfter=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],limits=['UnitIDs/crew/troops/energy/adjacent coordinates explicit fixtures on original constructors','Original full battle/terminal/campaign numeric callback; declared presentation omission if numericTailOnly','Actual original deployment/menu/UI and Android ordinary flow remain required'])
 for row in item_rows:row['afterHex']=bytes(w.u.mem_read(row['pointer'],0x54)).hex()
 if ruler_right:facts['administrativeAfter']=administrative_bytes(administrative)
 if ruler_right:facts['forcesAfter']=[dict(nativeId=row['nativeId'],pointer=row['pointer'],hex=bytes(w.u.mem_read(row['pointer'],0x12c)).hex())for row in facts['forcesBefore']]
 if return_phases:
  if disposition_choice not in [0,2]:raise ValueError('Return phases require declared RECRUIT or RELEASE')
  phases=[]
  for turn in range(return_phases):
   previous=bytes(w.u.mem_read(0x7200000,0x300000))
   try:
    if ordered_return:w.call(0x598630,count=50000000);w.call(0x59a4b0,count=50000000)
    w.call(0x599cf0,count=50000000)
   except Exception as error:
    facts['returnPhaseFailure']=dict(turn=turn,error=repr(error),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid,people=person_records(),completed=phases)
    output.with_suffix('.failure.json').write_text(json.dumps(facts,indent=2)+'\n');raise
   current=bytes(w.u.mem_read(0x7200000,0x300000));phases.append(dict(turn=turn,people=person_records(),changedBytes=sum(a!=b for a,b in zip(previous,current)),worldSha=sha(current),nativeRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]))
  facts['returnPhases']=phases;facts['returnPhaseLimit']='Original598630 reset then59a4b0 duration/diplomacy then599cf0 personnel phase'if ordered_return else 'Untouched599cf0 complete personnel phase after full original battle/release; calendar/global turn lifecycle not invoked'
 w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,baseline_rng);assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and baseline_rng==bytes(w.u.mem_read(0x8a5d44,4));facts['wholeWorldAndRngRestored']=True;output.write_text(json.dumps(facts,indent=2)+'\n');print('PASS original duel campaign',hex(function),'changed',len(facts['changedBytes']),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('context',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--numeric-tail-only',action='store_true');p.add_argument('--seed',type=int,default=23);p.add_argument('--entries',action='store_true');p.add_argument('--solo-right',action='store_true');p.add_argument('--display-context',action='store_true');p.add_argument('--link-members',action='store_true');p.add_argument('--ruler-right',action='store_true');p.add_argument('--ordered-return',action='store_true');p.add_argument('--camera',action='store_true');p.add_argument('--dialog',action='store_true');p.add_argument('--left-head-slot',type=int,choices=[0,1,2],default=0);p.add_argument('--held-items',action='store_true');p.add_argument('--human-ui-descriptor-fixture',action='store_true');p.add_argument('--callback-count',type=int,choices=[10000000,50000000,100000000],default=10000000);p.add_argument('--human-force',type=int,choices=range(47));p.add_argument('--human-choice',type=int,choices=[0,1,2,3]);p.add_argument('--initial-slot',type=int,choices=[0,1,2],default=0);p.add_argument('--ruler-native',type=int,choices=range(670),default=517);p.add_argument('--return-phases',type=int,choices=range(9),default=0);p.add_argument('--disposition-choice',type=int,choices=[0,1,2,3],default=-1);a=p.parse_args();inspect(a.installation,a.context,a.output,a.numeric_tail_only,a.seed,a.entries,a.link_members,a.solo_right,a.display_context,a.ruler_right,a.disposition_choice,a.return_phases,a.ordered_return,a.camera,a.dialog,a.ruler_native,a.initial_slot,a.human_force,a.human_choice,a.callback_count,a.human_ui_descriptor_fixture,a.left_head_slot,a.held_items)
