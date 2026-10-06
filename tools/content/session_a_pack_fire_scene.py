#!/usr/bin/env python3
"""New source-fire scene input. Preserve the old scene and all frozen JNI inputs."""
import pathlib,sys,struct,hashlib,json
ROOT=pathlib.Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools/content'))
from pc_resources import Archive
PC=pathlib.Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版')
exe=(PC/'san11pk.exe').read_bytes()
assert len(exe)==159920128 and hashlib.sha256(exe).hexdigest()=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
archive=Archive(PC/'Media/san11pkres.bin')
def digest(raw):return hashlib.sha256(raw).hexdigest()
old=ROOT/'app/src/main/assets/3d/pc-effects/source-scene.bin';prior=old.read_bytes()
assert digest(prior)=='98d2626d2842f272474cc7a3c2437e29b946d3827c8b52089741155cbc79e4ae'
assert prior[:8]==b'PCFXSC01'
body_size=struct.unpack_from('<I',prior,16)[0];seff=prior[64+body_size:];assert len(seff)==2280
try:
 result=[];records=[]
 for repeat in range(2):
  body=bytearray();rows=[]
  for effect in [8,9,13,16,17,18,19,20,23]:
   resource=struct.unpack_from('<I',exe,0x37692c+effect*12+4)[0];raw=archive.read(resource)
   assert raw[:8]==b'KSEF0131'
   if effect==13:assert resource==138 and digest(raw)=='7cc91cc42aeb548bcc85d21663251391ed5e78495129d38f9f8d8f708fcc44d7'
   body.extend(struct.pack('<3I',effect,resource,len(raw))+bytes.fromhex(digest(raw))+raw);rows.append({'effect':effect,'resource':resource,'bytes':len(raw),'sha256':digest(raw)})
  data=b'PCFXSC02'+struct.pack('<4I',9,126,len(body),len(seff))+bytes.fromhex(digest(seff))+bytes(8)+body+seff
  result.append(data);records=rows
 assert result[0]==result[1]
 out=ROOT/'app/src/main/assets/3d/pc-effects/source-fire-scene.bin';out.write_bytes(result[0]);assert old.read_bytes()==prior
 report={'schema':2,'asset':str(out),'bytes':len(result[0]),'sha256':digest(result[0]),'templates':records,'placements':126,'oldSceneByteEqual':True,'originalSeffByteEqual':True,'twoConversionsByteEqual':True,'jni4Modified':False,'currentOldWorkerAcceptsV2':False,'normalOriginalFire13Accepted':False,'originalModPriorityKnown':False,'overallGoalComplete':False}
 (ROOT/'docs/handoff/20261006/session-a/FIRE_SCENE_CONTAINER.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='templates'}))
finally:archive.close()
