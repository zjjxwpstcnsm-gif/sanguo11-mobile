#!/usr/bin/env python3
"""Execute archived v147 label occlusion against current exact presentation cache."""
from pathlib import Path
import subprocess

ROOT=Path(__file__).resolve().parents[1]
text=(ROOT/'app/src/test/fixtures/native-v147/labelVisible.java.txt').read_text()
baseline=text.replace('private boolean labelVisible','private static boolean baseline')
harness='''package game.sanguo.mobile;
import game.sanguo.core.*;import java.util.*;
public final class PcLabelVisibilityHarness {
 static SceneCamera camera;static MapSceneSnapshot snapshot;static int checks;
 static class Motion {float x,z;}
 static class Proxy {Motion motion=new Motion();float y;}
 static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
 static void compare(SceneLabelVisibility cache,Proxy object,boolean invalidated){
  long before=cache.computations;
  boolean expected=baseline(object);
  boolean actual=cache.visible(camera,snapshot.ground.surface,object.motion.x,object.motion.z,object.y+1);
  check(expected==actual,"archived actual terrain occlusion result changed");
  check(cache.computations==before+(invalidated?1:0),"exact-key recompute/retention boundary");
 }
 public static void main(String[] args)throws Exception {
  World source=ScenarioCatalog.load("heroes-250",0);byte[] authority=SaveCodec.encode(source);
  for(boolean legacy:new boolean[]{false,true}){
   World world=SaveCodec.decode(authority);if(legacy)world.mapRevision=63;
   MapSceneSnapshot.Ground ground=new MapSceneSnapshot.Ground(world);
   snapshot=new MapSceneSnapshot(ground,world,null,-1);
   camera=new SceneCamera();camera.width=1080;camera.height=1232;camera.span=50;camera.yaw=35;
   for(World.City city:world.cities){
    Proxy object=new Proxy();object.motion.x=ground.grid.x(city.hex);object.motion.z=ground.grid.z(city.hex);object.y=ground.surface.at(city.hex);
    camera.x=object.motion.x-4;camera.z=object.motion.z+4;
    SceneLabelVisibility cache=new SceneLabelVisibility();compare(cache,object,true);
    for(int i=0;i<8;i++)compare(cache,object,false);
    for(int field=0;field<8;field++){
     switch(field){case0:camera.x=Math.nextUp(camera.x);break;case1:camera.z=Math.nextUp(camera.z);break;case2:camera.span=Math.nextUp(camera.span);break;case3:camera.yaw=Math.nextUp(camera.yaw);break;case4:camera.tilt=Math.nextUp(camera.tilt);break;case5:camera.width++;break;case6:camera.height++;break;default:camera.facing=-camera.facing;}
     compare(cache,object,true);compare(cache,object,false);
    }
    object.motion.x=Math.nextUp(object.motion.x);compare(cache,object,true);
    object.motion.z=Math.nextUp(object.motion.z);compare(cache,object,true);
    object.y+=.01f;compare(cache,object,true);
    // A new immutable terrain can change occlusion without moving the camera.
    MapSceneSnapshot.Ground replacement=new MapSceneSnapshot.Ground(world);
    snapshot=new MapSceneSnapshot(replacement,world,null,-1);compare(cache,object,true);
    snapshot=new MapSceneSnapshot(ground,world,null,-1);compare(cache,object,true);
   }
  }
  check(Arrays.equals(authority,SaveCodec.encode(source)),"occlusion cache leaves full authority/RNG/save unchanged");
  System.out.println("PASS archived-source label occlusion "+checks+" checks; all87sites PC/legacy, every exact camera/position field, immutable surface identity; HOST ONLY");
 }
 __BASELINE__
}'''.replace('__BASELINE__',baseline).replace('case0','case 0').replace('case1','case 1').replace('case2','case 2').replace('case3','case 3').replace('case4','case 4').replace('case5','case 5').replace('case6','case 6')
work=ROOT/'app/build/pc-label-visibility-check';work.mkdir(parents=True,exist_ok=True)
java=work/'PcLabelVisibilityHarness.java';java.write_text(harness)
names='TileGeometry GridWorldTransform SceneCamera ScenePicking SceneMesh UnitVisual UnitMotion CombatVisual SiteVisual TerrainSurface WaterVisualField TerrainMaterialField MapSceneSnapshot FactionColors SiegeOverlay SceneVisibilityStamp SceneLabelVisibility'.split()
sources=list((ROOT/'core/src/main/java').rglob('*.java'))+list((ROOT/'game-api/src/main/java').rglob('*.java'))+[ROOT/f'app/src/main/java/game/sanguo/mobile/{name}.java'for name in names]+[java]
argfile=work/'sources.txt';argfile.write_text('\n'.join(str(p)for p in sources)+'\n')
subprocess.run(['javac','--release','17','-encoding','UTF-8','-d',str(work),'@'+str(argfile)],check=True)
subprocess.run(['java','-Xmx1200m','-cp',str(work)+':'+str(ROOT/'core/src/main/resources'),'game.sanguo.mobile.PcLabelVisibilityHarness'],check=True)
