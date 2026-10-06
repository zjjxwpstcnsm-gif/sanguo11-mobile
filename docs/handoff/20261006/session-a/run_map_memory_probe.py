#!/usr/bin/env python3
import pathlib,subprocess,json,time,sys
ROOT=pathlib.Path(__file__).resolve().parents[4];stage=sys.argv[1] if len(sys.argv)>1 else 'baseline';assert stage in ['baseline','fix'];OUT=ROOT/('out/session-a/map-memory-'+stage);OUT.mkdir(parents=True,exist_ok=True)
names='TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay'.split()
sources=list((ROOT/'core/src/main/java').rglob('*.java'))+list((ROOT/'game-api/src/main/java').rglob('*.java'))
for name in names:
 source=ROOT/f'app/src/main/java/game/sanguo/mobile/{name}.java'
 if stage=='baseline':
  source=OUT/'source'/(name+'.java');source.parent.mkdir(parents=True,exist_ok=True)
  source.write_bytes(subprocess.check_output(['git','show','0e7b9bc2:app/src/main/java/game/sanguo/mobile/'+name+'.java'],cwd=ROOT))
 sources.append(source)
sources.append(ROOT/'docs/handoff/20261006/session-a/MapMemoryProbe.java')
(OUT/'sources.txt').write_text('\n'.join(map(str,sources))+'\n')
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-d',str(OUT),'@'+str(OUT/'sources.txt')],check=True)
started=time.monotonic()
with (OUT/'host-384m.txt').open('w') as log:
 r=subprocess.run(['java','-Xmx384m','-cp',str(OUT)+':'+str(ROOT/'core/src/main/resources'),'game.sanguo.mobile.MapMemoryProbe'],stdout=log,stderr=subprocess.STDOUT)
report={'scope':'actual production CPU terrain in desktop JVM384MiB; not Android reproduction, native/GPU unknown','exitCode':r.returncode,'seconds':time.monotonic()-started,'log':str(OUT/'host-384m.txt'),'output':(OUT/'host-384m.txt').read_text(),'completeGoal':False}
(ROOT/('docs/handoff/20261006/session-a/MEMORY_'+stage.upper()+'.json')).write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False))
