#!/usr/bin/env python3
import pathlib,subprocess,json
ROOT=pathlib.Path(__file__).resolve().parents[4];OUT=ROOT/'out/session-a/mesh-parity';OUT.mkdir(parents=True,exist_ok=True)
COMMON=ROOT/'out/session-a/map-memory-fix';BASE=OUT/'baseline';BASE.mkdir(exist_ok=True)
for name in ['SceneMesh','TerrainMaterialField']:
 source=BASE/(name+'.java');source.write_bytes(subprocess.check_output(['git','show','0e7b9bc2:app/src/main/java/game/sanguo/mobile/'+name+'.java'],cwd=ROOT))
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-cp',str(COMMON),'-d',str(BASE),str(BASE/'SceneMesh.java'),str(BASE/'TerrainMaterialField.java')],check=True)
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-cp',str(COMMON),'-d',str(OUT),str(ROOT/'docs/handoff/20261006/session-a/MeshParityProbe.java')],check=True)
results={}
for stage,first in [('baseline',BASE),('current',COMMON)]:
 with (OUT/(stage+'.txt')).open('w') as f:
  subprocess.run(['java','-Xmx700m','-cp',':'.join(map(str,[first,OUT,COMMON,ROOT/'core/src/main/resources',ROOT/'core/src/test/resources'])),'game.sanguo.mobile.MeshParityProbe'],stdout=f,stderr=subprocess.STDOUT,check=True)
 results[stage]=(OUT/(stage+'.txt')).read_text().splitlines()
assert len(results['current'])==16,results
assert results['baseline']==results['current'],results
report={'passed':True,'windows':16,'scope':'desktop real production terrain; original indexed xyz/UV0/triangle order and full source water/grid exact; legacy all attributes exact; cache/rebuild/saveRNG invariance','unusedPcCarriersRemoved':'legacy vertex pigment/tangent/shore/flow unused by original unlit source shaders','androidGpuPixelsValidated':False,'results':results}
(ROOT/'docs/handoff/20261006/session-a/MESH_PARITY.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False))
