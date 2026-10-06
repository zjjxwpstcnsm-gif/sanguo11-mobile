#!/usr/bin/env python3
"""Original417770 in isolated source-derived terrain memory vs immutable Java reads.
Geometry oracle, not a World/snapshot/rule or normal Android acceptance.
"""
import pathlib,sys,struct,gzip,subprocess,hashlib,json
ROOT=pathlib.Path(__file__).resolve().parents[4];sys.path.insert(0,str(ROOT/'tools/content'))
from pc_effect_machine import SourceEffectMachine,VerifiedSourceExecutable
from pc_resources import Archive
from unicorn import UC_HOOK_CODE
PC=pathlib.Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版');exe=VerifiedSourceExecutable((PC/'san11pk.exe').read_bytes());a=Archive(PC/'Media/san11pkres.bin');shex=a.read(4791);mesh=a.read(4793);a.close()
assert shex[:8]==b'SHEX0008' and len(shex)==440008 and mesh[:8]==b'K3ST0006'
coarsePath=ROOT/'core/src/main/resources/maps/pc-water.bin.gz';coarse=gzip.decompress(coarsePath.read_bytes());assert coarse[:8]==b'PCWCO001' and len(coarse)==655368
out=ROOT/'out/session-a/fire-position';out.mkdir(exist_ok=True);java='/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home/bin/'
subprocess.run([java+'javac','-cp',str(ROOT/'core/build/classes/java/main'),'-d',str(out),str(ROOT/'app/src/main/java/game/sanguo/mobile/PcCellFirePosition.java'),str(ROOT/'docs/handoff/20261006/session-a/PcCellFirePositionProbe.java')],check=True)
subprocess.run([java+'java','-cp',str(out)+':'+str(ROOT/'core/build/classes/java/main')+':'+str(ROOT/'core/build/resources/main'),'game.sanguo.mobile.PcCellFirePositionProbe',str(out/'java.bin')],check=True)
expected=(out/'java.bin').read_bytes();assert len(expected)==640000
m=SourceEffectMachine(exe);terrain=0x50000000;cells=0x6fb0e6c;point=m.HEAP+0x8000;actual=bytearray();ruleCalls=[];counts={'water':0,'land':0};result={'passed':False}
try:
 m.u.mem_map(terrain,0x2000000);m.u.mem_map(0x6fb0000,0x100000)
 fine=bytearray(1025*1025*10)
 for i in range(1025*1025):fine[i*10]=mesh[8+i*8]
 m.u.mem_write(terrain+0x800000,bytes(fine));m.u.mem_write(terrain+0x128500a,coarse[8:])
 view=bytearray(200*200*20)
 for i in range(40000):view[i*20]=shex[8+i*11]&31
 m.u.mem_write(cells,bytes(view))
 def forbid(u,address,size,user):
  if address in [0x472150,0x4721d0]:ruleCalls.append(hex(address));raise ValueError('Rule RNG from position helper')
 hook=m.u.hook_add(UC_HOOK_CODE,forbid)
 for x in range(200):
  for y in range(200):
   m.call(0x417770,terrain,point,x,y);data=bytes(m.u.mem_read(point,16));index=(x*200+y)*16
   assert data==expected[index:index+16],(x,y,data.hex(),expected[index:index+16].hex())
   actual.extend(data);counts['water' if shex[8+(x*200+y)*11] in [7,8] else 'land']+=1
 m.u.hook_del(hook);(out/'original.bin').write_bytes(actual)
 result.update(passed=True,exactFloat32Vectors=40000,counts=counts,originalVectorsSha256=hashlib.sha256(actual).hexdigest(),ruleRngCalls=ruleCalls)
except Exception as error:result['error']=repr(error)
finally:m.close()
result.update(scope='original helper against source-derived height/cell view plus inherited original-coarse-water field; not normal Android/RNG/save/gameplay acceptance',sourceExeSha256=hashlib.sha256(exe.data).hexdigest(),shex4791Sha256=hashlib.sha256(shex).hexdigest(),mesh4793Sha256=hashlib.sha256(mesh).hexdigest(),inheritedCoarseWaterSha256=hashlib.sha256(coarsePath.read_bytes()).hexdigest(),coarseWaterProvenance='existing original415e20 initialization/import contract; readonly no reconstruction of governance/World',helperCodeSha256=hashlib.sha256(exe.data[0x17770:0x1786f]).hexdigest(),normalAndroid=False,ARM=False,goalComplete=False)
(ROOT/'docs/handoff/20261006/session-a/FIRE_POSITION_SOURCE.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
if not result['passed']:raise SystemExit(1)
