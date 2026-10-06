#!/usr/bin/env python3
"""Actual original factory/handle-stop in an isolated initialized source effect manager."""
import pathlib,sys,json,struct
ROOT=pathlib.Path(__file__).resolve().parents[4];sys.path.insert(0,str(ROOT/'tools/content'))
from pc_resources import Archive,sha
from pc_effect_machine import SourceEffectMachine,VerifiedSourceExecutable
from inspect_pc_effect_draw_packets import DrawPacketObserver
from unicorn import UC_HOOK_CODE
PC=pathlib.Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版');exe=VerifiedSourceExecutable((PC/'san11pk.exe').read_bytes());a=Archive(PC/'Media/san11pkres.bin');raw=a.read(138);a.close()
assert sha(raw)=='7cc91cc42aeb548bcc85d21663251391ed5e78495129d38f9f8d8f708fcc44d7'
m=SourceEffectMachine(exe);observer=None;result={'passed':False};log=[]
try:
 observer=DrawPacketObserver(m,.001,(0,0,0));m.load(raw,scene_camera_provider=observer.provider)
 manager=m.HEAP+0x8000;point=m.HEAP+0x7000
 assert m.call(0x413510,manager)==manager
 assert m.uint(manager+0x33c)==9 and m.uint(manager+0x344)==0xffffffff
 assert m.call(0x45a620,manager,0,4096,244)==1
 m.call(0x45a280,manager,13,m.instance);assert m.call(0x45a270,manager,13)==m.instance
 # Explicit post-resource-loader boundary: original414134 writes this ready
 # flag after loading. GPU archive/library bootstrap is outside this probe.
 m.u.mem_write(manager+0x314,struct.pack('<I',1))
 ruleBefore=bytes(m.u.mem_read(0x8a5d44,4));seenRule=[]
 def forbid(u,addr,size,user):
  if addr in [0x472150,0x4721d0]:seenRule.append(hex(addr));raise ValueError('Rule RNG called by source visual factory')
 hook=m.u.hook_add(UC_HOOK_CODE,forbid)
 handles=[]
 for position in [(0,0,0),(250,0,100)]:
  m.u.mem_write(point,struct.pack('<4f',*position,1));m.u.mem_write(manager+0x334,bytes(8))
  handle=m.call(0x414670,manager,13,point);opaque=bytes(m.u.mem_read(manager+0x334,8))
  #414670 returns the low opaque handle copied by original413770 callback.
  assert handle!=0;handles.append({'handle':hex(handle),'generation':struct.unpack('<I',opaque[4:])[0],'position':position,'flagsBefore':m.uint(handle+4)})
 for dt in [.0333333333,.1,.3,.5,1]:
  m.update(dt);packets=observer.draw();log.append({'inputDt':dt,'packetCount':len(packets),'root':m.snapshot()})
 stop=[]
 for entry in handles:
  pointer=int(entry['handle'],16);before=bytes(m.u.mem_read(pointer,8));m.call(0x413470,manager,pointer);after=bytes(m.u.mem_read(pointer,8));assert after[6]==before[6]&0xfb and after[:6]==before[:6] and after[7:]==before[7:];stop.append({'handle':entry['handle'],'before':before.hex(),'after':after.hex()})
 for dt in [.1,.3,.5,1,2,4]:
  m.update(dt);log.append({'afterStopInputDt':dt,'packetCount':len(observer.draw()),'root':m.snapshot()})
 assert not seenRule and bytes(m.u.mem_read(0x8a5d44,4))==ruleBefore
 result={'passed':True,'managerConstructor':'source413510; mode9/remap-1','initializationBoundary':'source45a620 and actual template13 registration; post-source414134 ready flag explicit; GPU/bootstrap not simulated','factory':'source414670→413d20→457880; callback413770','handles':handles,'stop':'source413470 clears flag0x04 in byte+6','stopBytes':stop,'frames':log,'ruleRngCalls':seenRule,'rulePercentStateUnchanged':True,'destroyedInstancesDrawZero':log[-1]['packetCount']==0,'normalAndroidAccepted':False,'completeGoal':False}
 m.u.hook_del(hook)
except Exception as e:
 result.update(reason=str(e),frames=log,trace=list(m.trace),completeGoal=False)
finally:
 if observer:observer.close()
 m.close()
(ROOT/'docs/handoff/20261006/session-a/FIRE_FACTORY_RUNTIME.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ['frames','trace']},ensure_ascii=False))
