#!/usr/bin/env python3
"""Actual supplied resource138/controller/matrix/quad/material; isolated visual VM, no rule calls."""
import pathlib,sys,json,struct,hashlib
ROOT=pathlib.Path(__file__).resolve().parents[4];sys.path.insert(0,str(ROOT/'tools/content'))
from pc_resources import Archive,sha
from pc_effect_machine import SourceEffectMachine,VerifiedSourceExecutable
from inspect_pc_effect_draw_packets import DrawPacketObserver
from pc_effect_gpu_observer import FinalQuadObserver
from unicorn.x86_const import UC_X86_REG_EDI
PC=pathlib.Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版');exe=VerifiedSourceExecutable((PC/'san11pk.exe').read_bytes());a=Archive(PC/'Media/san11pkres.bin');effect=13;ptr,resource,flag=struct.unpack_from('<3I',exe.data,0x37692c+effect*12);filename=exe.data[ptr-0x400000:].split(b'\0',1)[0].decode('ascii');raw=a.read(resource);a.close();assert resource==138 and filename=='media/effect/effect013.efdx' and sha(raw)=='7cc91cc42aeb548bcc85d21663251391ed5e78495129d38f9f8d8f708fcc44d7'
results=[]
for repeat in range(2):
 machine=SourceEffectMachine(exe);observer=None;final=None
 try:
  observer=DrawPacketObserver(machine,.001,(0,0,0));machine.load(raw,scene_camera_provider=observer.provider)
  machine.u.reg_write(UC_X86_REG_EDI,machine.instance);first=machine.call(0x4133d0,0)
  machine.u.reg_write(UC_X86_REG_EDI,machine.instance);second=machine.call(0x413420,0)
  identity=struct.pack('<16f',1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1)
  if first==0 and second==0:machine.start(identity);mode='native457880 count1/matrix; original template13 factory helper conditions both false'
  else:raise ValueError('Unexamined factory special start mode '+str((first,second)))
  final=FinalQuadObserver(machine,observer.camera,materials=True);frames=[];dig=hashlib.sha256();modes=set();textures=set()
  for dt in [.0333333333,.1,.3,.5,1,2,4]:
   machine.update(dt);packets=observer.draw();quads=[]
   for packet in packets:
    q=final.evaluate(packet);modes.add((q['blend_operation'],q['blend_source'],q['blend_destination']));textures.add(q['texture_index']);quads.append(q);dig.update(bytes.fromhex(q['quad_vb_hex']));dig.update(bytes.fromhex(q['quad_matrix_hex']));dig.update(struct.pack('<4I',q['texture_index'],q['blend_operation'],q['blend_source'],q['blend_destination']))
   frames.append({'inputDt':dt,'quads':len(quads),'snapshot':machine.snapshot()})
  pausedBefore=machine.snapshot();p1=observer.draw();p2=observer.draw();assert [x['raw_hex'] for x in p1]==[x['raw_hex'] for x in p2],'paused source packet changed';pausedAfter=machine.snapshot()
  results.append({'startMode':mode,'frames':frames,'quadMaterialsSha256':dig.hexdigest(),'blendModes':sorted(modes),'textureIndices':sorted(textures),'pausedPacketsEqual':True,'pausedRootElapsedUnchanged':pausedBefore['root_elapsed']==pausedAfter['root_elapsed'],'diagnosticCamera':observer.inputs})
 finally:
  if final:final.close()
  if observer:observer.close()
  machine.close()
assert results[0]['quadMaterialsSha256']==results[1]['quadMaterialsSha256'],'source repeats differ'
report={'sourceExecutableSha256':sha(exe.data),'effectIndex':effect,'resourceId':resource,'sourceFile':filename,'sourceBytes':len(raw),'sourceSha256':sha(raw),'sourceTableFlagRaw':flag,'isolatedSourceRuns':results,'repeatByteEqual':True,'normalAndroidCellFireBound':False,'originalManagerHandleStopValidated':False,'limits':['explicit source diagnostic view/matrix, not PC captured camera/pixels','source particle VM only, no World/session/commands/rule RNG','grid-fire creation/deletion identified in source, immutable fireState integration and opaque handle stop remain pending'],'completeGoal':False}
(ROOT/'docs/handoff/20261006/session-a/FIRE_SOURCE_RUNTIME.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'template':effect,'resource':resource,'repeatByteEqual':True,'materialModes':results[0]['blendModes'],'textures':results[0]['textureIndices'],'frames':[x['quads'] for x in results[0]['frames']],'pauseEqual':results[0]['pausedPacketsEqual']}))
