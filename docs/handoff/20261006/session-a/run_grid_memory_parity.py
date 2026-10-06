#!/usr/bin/env python3
import pathlib,subprocess,json,os
ROOT=pathlib.Path(__file__).resolve().parents[4]; OUT=ROOT/'out/session-a/grid-memory-parity'; OUT.mkdir(parents=True,exist_ok=True)
COMMON=ROOT/'out/session-a/map-memory-fix'; BEFORE=OUT/'before'; BEFORE.mkdir(exist_ok=True)
def call(args,**kw):subprocess.run(args,cwd=ROOT,check=True,**kw)
# Compile current production without replacing earlier memory receipts.
COMMON=OUT/'common'; COMMON.mkdir(exist_ok=True)
names='TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay SceneFactsPresentation'.split()
sources=list((ROOT/'core/src/main/java').rglob('*.java'))+list((ROOT/'game-api/src/main/java').rglob('*.java'))+[ROOT/f'app/src/main/java/game/sanguo/mobile/{n}.java' for n in names]+[ROOT/'docs/handoff/20261006/session-a/MapMemoryProbe.java']
(OUT/'sources.txt').write_text('\n'.join(map(str,sources))+'\n')
call(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-d',str(COMMON),'@'+str(OUT/'sources.txt')])
(BEFORE/'SceneMesh.java').write_bytes(subprocess.check_output(['git','show','36a5e525:app/src/main/java/game/sanguo/mobile/SceneMesh.java'],cwd=ROOT))
call(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-cp',str(COMMON),'-d',str(BEFORE),str(BEFORE/'SceneMesh.java')])
call(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-cp',str(COMMON),'-d',str(OUT),str(ROOT/'docs/handoff/20261006/session-a/MeshParityProbe.java'),str(ROOT/'docs/handoff/20261006/session-a/GridMemoryParityProbe.java')])
rows={}; olderWindows={}
for name,first in [('before',BEFORE),('after',COMMON)]:
 with (OUT/(name+'.txt')).open('w') as log:
  call(['java','-Xmx700m','-cp',':'.join(map(str,[first,OUT,COMMON,ROOT/'core/src/main/resources'])),'game.sanguo.mobile.GridMemoryParityProbe'],stdout=log,stderr=subprocess.STDOUT)
 rows[name]=(OUT/(name+'.txt')).read_text().splitlines()
 with (OUT/(name+'-legacy-parity.txt')).open('w') as log:
  call(['java','-Xmx700m','-cp',':'.join(map(str,[first,OUT,COMMON,ROOT/'core/src/main/resources',ROOT/'core/src/test/resources'])),'game.sanguo.mobile.MeshParityProbe'],stdout=log,stderr=subprocess.STDOUT)
 olderWindows[name]=(OUT/(name+'-legacy-parity.txt')).read_text().splitlines()
assert [x for x in rows['before'] if x.startswith('PARITY')]==[x for x in rows['after'] if x.startswith('PARITY')]
assert len(olderWindows['before'])==16 and olderWindows['before']==olderWindows['after']
report={'passed':True,'beforeCommit':'36a5e525','scope':'desktop production 3 source windows x 4 spans plus 16 PC/legacy windows; exact expanded xyz/color/triangle order/UV, cache rebuild and complete Save/RNG; not Android/GPU/ARM acceptance','rows':rows,'legacyWindows':olderWindows,'ordinary384AndroidPassed':False}
(ROOT/'docs/handoff/20261006/session-a/GRID_MEMORY_PARITY.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({k:[x for x in v if x.startswith('GRID')] for k,v in rows.items()},indent=2))
