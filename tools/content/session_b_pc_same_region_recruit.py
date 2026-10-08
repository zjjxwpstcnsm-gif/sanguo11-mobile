#!/usr/bin/env python3
"""Untouched4a8440 allegiance callback with declared origin equal new home.
Not an original admission/probability replacement or actual menu acceptance.
"""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EIP,UC_X86_REG_ESP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
p=argparse.ArgumentParser();p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--held-items',action='store_true');a=p.parse_args();output_guard(a.installation,a.output)
if a.output.exists()or a.output.with_suffix('.failure.json').exists():raise ValueError('Preserve receipt')
print('Loading original source',flush=True);d,w,source,geo,_=prepare(a.installation);baseline=bytes(w.u.mem_read(w.root,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));target=w.call(0x490b00,558,receiver=w.root);army=w.call(0x490ad0,0,receiver=w.root);ruler=w.call(0x490b00,365,receiver=w.root);home=struct.unpack('<i',w.u.mem_read(ruler+0x98,4))[0];assert home==8;city=w.call(0x490d00,home,receiver=w.root);rows=[];omitted=[]
for f,receiver in [(0x415400,0x32602b0),(0x477810,0x6ee7888),(0x4f52d0,0x91ba558)]:w.call(f,receiver=receiver,count=50000000)
assert baseline==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
# Original callback derives origin from the target's actual deployed unit,
# not merely from its supplied site argument. Declare and link that unit.
context=json.loads(Path('out/session-b/duel-unit-context-source0-v4.json').read_text());unit=context['units'][1]['pointer'];w.u.mem_write(unit,bytes.fromhex(context['cases'][1]['afterHex']))
terrain=bytes(w.u.mem_read(0x6fb0e68,40000*20));parents=bytes(w.u.mem_read(0x79c2b0,87));cell=next(i for i in range(40000)if (struct.unpack_from('<I',terrain,20*i+4)[0]>>5)&127<87 and parents[(struct.unpack_from('<I',terrain,20*i+4)[0]>>5)&127]==home);point=(cell//200,cell%200)
w.u.mem_write(unit+0x3c,struct.pack('<hh',*point));w.u.reg_write(UC_X86_REG_EAX,unit);place=w.call(0x4a7530)
for native in [558,14,517]:w.call(0x4a0cb0,w.call(0x490b00,native,receiver=w.root),place,receiver=0x799895c,count=50000000)
assert w.call(0x489220,receiver=target)==1
assert w.call(0x47a9b0,receiver=target)==home, 'Original region getter must agree with declared home'
item_rows=[];resolved_actor=ruler;winner=None
if a.held_items:
 winner=context['units'][0]['pointer'];w.u.mem_write(winner,bytes.fromhex(context['cases'][0]['afterHex']));w.u.mem_write(winner+0xc,struct.pack('<3i',116,365,466));w.u.mem_write(winner+0x3c,struct.pack('<hh',*point))
 w.u.reg_write(UC_X86_REG_EAX,winner);winner_place=w.call(0x4a7530)
 for native in [116,365,466]:w.call(0x4a0cb0,w.call(0x490b00,native,receiver=w.root),winner_place,receiver=0x799895c,count=50000000)
 wrapper=d.fixture+0x3000;w.u.mem_write(wrapper,struct.pack('<2I',unit,winner));resolved_actor=w.call(0x4ad960,1,receiver=wrapper);assert resolved_actor==ruler
 for item_native in [0,30,41,42]:
  ptr=w.root+0x7777c+item_native*0x54;w.u.mem_write(ptr+0x40,struct.pack('<i',558));item_rows.append(dict(nativeId=item_native,pointer=ptr,beforeHex=bytes(w.u.mem_read(ptr,0x54)).hex()))
def observe(u,ip,size,user):
 sp=u.reg_read(UC_X86_REG_ESP);rows.append(dict(address=hex(ip),arguments=list(struct.unpack('<5I',u.mem_read(sp,20)))))
for ip in [0x4a75a0,0x4bbaa0,0x4a0cb0,0x4a32f0,0x4a92c0,0x484de0]:w.u.hook_add(UC_HOOK_CODE,observe,begin=ip,end=ip)
# This callback includes value displays; omit only the same established void
# display wrapper588bb0, leaving all original setters/admission untouched.
def display(u,ip,size,user):
 sp=u.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',u.mem_read(sp,4))[0];omitted.append(dict(address=hex(ip),scope='void value display only'));u.reg_write(UC_X86_REG_ESP,sp+16);u.reg_write(UC_X86_REG_EIP,ret)
w.u.hook_add(UC_HOOK_CODE,display,begin=0x588bb0,end=0x588bb0)
locationTrace=[]
def location_observe(u,ip,size,user):
 locationTrace.append(dict(address=hex(ip),current=struct.unpack('<i',u.mem_read(target+0x9c,4))[0],home=struct.unpack('<i',u.mem_read(target+0x98,4))[0],eax=u.reg_read(UC_X86_REG_EAX),unitPoint=list(struct.unpack('<hh',u.mem_read(unit+0x3c,4)))))
for ip in [0x4a855f,0x4a857f,0x4a79c8,0x4a79cd,0x4a85ca,0x4a8646]:w.u.hook_add(UC_HOOK_CODE,location_observe,begin=ip,end=ip)
before=bytes(w.u.mem_read(target,0x190));facts=dict(exeSha=EXE_SHA,source=source,geography=geo,targetNative=558,newArmyNative=0,newHomeNative=home,declaredOriginNative=home,declaredUnitNative=1,declaredPoint=point,linkedOriginalUnit=True,locationTrace=locationTrace,beforeHex=before.hex(),calls=rows,omittedPresentation=omitted,completeGoal=False,declaredHeldItems=item_rows,declaredWinnerHeadNative=116 if a.held_items else None,resolvedOriginalActorNative=365 if a.held_items else None)
try:
 if a.held_items:w.call(0x4a92c0,target,resolved_actor,receiver=0x799895c,count=50000000)
 for row in item_rows:row['afterOriginalItemTransferHex']=bytes(w.u.mem_read(row['pointer'],0x54)).hex()
 w.call(0x4a8440,target,army,city,1,receiver=0x799895c,count=50000000);after=bytes(w.u.mem_read(target,0x190));facts.update(afterHex=after.hex(),rngAfter=int.from_bytes(w.u.mem_read(0x8a5d44,4),'little'))
 for i in range(2):
  for f in [0x598630,0x59a4b0,0x599cf0]:w.call(f,count=50000000)
  facts.setdefault('personnelPhases',[]).append(dict(index=i,personHex=bytes(w.u.mem_read(target,0x190)).hex(),rng=int.from_bytes(w.u.mem_read(0x8a5d44,4),'little')))
except Exception as e:
 facts.update(error=repr(e),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),invalid=w.invalid,partialPersonHex=bytes(w.u.mem_read(target,0x190)).hex());a.output.with_suffix('.failure.json').write_text(json.dumps(facts,indent=2)+'\n');raise
finally:w.u.mem_write(w.root,baseline);w.u.mem_write(0x8a5d44,rng)
assert baseline==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));facts.update(wholeWorldAndRngRestored=True,limits=['Declared callback target/army/city and new origin equal city home; no result/HP/loyalty/owner/task write','Original allegiance callback and2 personnel phases; original probability/menu/whole calendar and actual APK remain separate']);a.output.write_text(json.dumps(facts,indent=2)+'\n');print('PASS original same-region recruit',sha(a.output.read_bytes()))
