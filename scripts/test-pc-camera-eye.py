#!/usr/bin/env python3
"""Check production orthographic eye against visible terrain-prism corners.

Host projection/clipping boundary only; stub ground bounds are input fixtures,
not a Filament driver, PC lens reference, or image acceptance.
"""
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[1]
out=ROOT/'app/build/pc-camera-eye-check';out.mkdir(parents=True,exist_ok=True)
ground=out/'MapSceneSnapshot.java';ground.write_text('''package game.sanguo.mobile;
final class MapSceneSnapshot {static final class Ground {Object pcMap;float minX,minZ,maxX,maxZ;}}
final class TerrainSurface {static final float MAX_HEIGHT=2.6f;}
''')
pc=out/'PcMap.java';pc.write_text('''package game.sanguo.core;
public final class PcMap {public static final float SCALE=.025f,MAX_HEIGHT=255*SCALE;}
''')
test=out/'PcCameraEyeTest.java';test.write_text('''package game.sanguo.mobile;
public final class PcCameraEyeTest {
 static int checks;static void check(boolean condition){checks++;if(!condition)throw new AssertionError("camera eye prism "+checks);}
 public static void main(String[] args){
  SceneCamera c=new SceneCamera();c.width=1080;c.height=1232;c.x=43;c.z=51;
  for(boolean perspective:new boolean[]{false,true})for(float height:new float[]{2.6f,game.sanguo.core.PcMap.MAX_HEIGHT})for(float span:new float[]{3,8,17,35,160})
  for(float tilt:new float[]{40,55,70})for(float yaw:new float[]{0,35,90,180,265,359})for(int facing:new int[]{-1,1}){
   c.perspective=perspective;c.heightLimit=height;c.span=span;c.tilt=tilt;c.yaw=yaw;c.facing=facing;
   for(float sx:new float[]{0,c.width})for(float sy:new float[]{0,c.height})for(float y:new float[]{0,height}){
    float x=c.worldX(sx,sy,y),z=c.worldZ(sx,sy,y);
    check(Math.abs(c.screenX(x,z,y)-sx)<.003);check(Math.abs(c.screenY(x,z,y)-sy)<.003);
    double backwards=((x-c.x)*c.backX()+(z-c.z)*c.rightX())*c.cos()+(y-(perspective?.8:0))*c.sin();
    double depth=c.eyeDistance()-backwards;
    if(!perspective)check(depth>=16-.0001);check(depth>c.nearPlane()&&depth<c.farPlane());
   }
  }
  System.out.println("PASS production SceneCamera eye "+checks+" checks: visible terrain prism stays in near/far, PC perspective/legacy ortho plane round-trips; HOST ONLY, PC lens/objects/FX raster still pending");
 }
}
''')
subprocess.run(['javac','--release','17','-d',str(out),str(ground),str(pc),str(test),str(ROOT/'app/src/main/java/game/sanguo/mobile/SceneCamera.java')],check=True)
subprocess.run(['java','-cp',str(out),'game.sanguo.mobile.PcCameraEyeTest'],check=True)
