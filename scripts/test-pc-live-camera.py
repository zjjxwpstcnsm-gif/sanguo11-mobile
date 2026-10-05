#!/usr/bin/env python3
"""Production camera against actual original PC before/after zoom snapshots.

Reference matrices come from the running supplied EXE, not a formula duplicated
from SceneCamera. Numerical lens/picking calibration, separate from GPU/art.
"""
from pathlib import Path
import hashlib,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
fixture=ROOT/'app/src/test/fixtures/native-v153'
manifest=json.loads((fixture/'camera-manifest.json').read_text())
for row in manifest['records']:
    assert row['stable_before_after']
    assert hashlib.sha256((fixture/row['file']).read_bytes()).hexdigest()==row['sha256']
out=ROOT/'app/build/pc-live-camera';out.mkdir(parents=True,exist_ok=True)
(out/'MapSceneSnapshot.java').write_text('''package game.sanguo.mobile;
final class MapSceneSnapshot {static final class Ground {Object pcMap;float minX,minZ,maxX,maxZ;}}
final class TerrainSurface {static final float MAX_HEIGHT=2.6f;}
''')
(out/'PcMap.java').write_text('''package game.sanguo.core;
public final class PcMap {public static final float MAX_HEIGHT=6.375f;}
''')
(out/'LiveCameraCheck.java').write_text('''package game.sanguo.mobile;
import java.nio.*;import java.nio.file.*;
public final class LiveCameraCheck {
 static int checks;static double pixelError,matrixError;
 static void near(double a,double b,double epsilon,String reason){checks++;if(!Double.isFinite(a)||Math.abs(a-b)>epsilon)throw new AssertionError(reason+" "+a+" != "+b);}
 public static void main(String[] args)throws Exception{
  for(String file:args){ByteBuffer data=ByteBuffer.wrap(Files.readAllBytes(Path.of(file))).order(ByteOrder.LITTLE_ENDIAN);float[] f=new float[128];for(int i=0;i<128;i++)f[i]=data.getFloat();
   double dx=f[0]-f[4],dy=f[1]-f[5],dz=f[2]-f[6],distance=Math.sqrt(dx*dx+dy*dy+dz*dz);
   SceneCamera c=new SceneCamera();c.perspective=true;c.width=1650;c.height=1050;c.x=f[4]*.05f-28.5f;c.z=f[6]*.05f-28.5f;c.span=(float)(distance*.05*Math.tan(f[9]*.5));c.tilt=(float)Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));c.yaw=(float)Math.toDegrees(Math.atan2(dx,dz));
   near(c.eyeDistance(),distance*.05,.00001,"actual source distance");near(c.nearPlane(),f[10]*.05,.000001,"source near");near(c.farPlane(),f[11]*.05,.00002,"source far");near(c.targetHeight(),f[5]*.05,.000001,"source targetY");
   double d=c.eyeDistance(),r=c.rightX(),b=c.backX(),s=c.sin(),co=c.cos(),targetY=c.targetHeight();
   float[] view={(float)r,(float)(-b*s),(float)(b*co),0,0,(float)co,(float)s,0,(float)-b,(float)(-r*s),(float)(r*co),0,
    (float)(-c.x*r+c.z*b),(float)(c.x*b*s+c.z*r*s-targetY*co),(float)(-c.x*b*co-c.z*r*co-targetY*s-d),1};
   float[] mapped=PcEffectCoordinates.camera(view,c.projection(c.nearPlane(),c.farPlane()),0,0,1/c.farPlane());
   for(int i=0;i<16;i++){matrixError=Math.max(matrixError,Math.abs(mapped[35+i]-f[32+i]));near(mapped[35+i],f[32+i],.000001,"source projection");}
   for(float xoff:new float[]{-120,-60,0,60,120})for(float zoff:new float[]{-120,-60,0,60,120})for(float py:new float[]{0,16,32,64,127.5f}){
    float px=f[4]+xoff,pz=f[6]+zoff,wx=px*.05f-28.5f,wz=pz*.05f-28.5f,wy=py*.05f;
    double[] clip=new double[4],mappedClip=new double[4];for(int a=0;a<4;a++){clip[a]=px*f[48+a]+py*f[52+a]+pz*f[56+a]+f[60+a];mappedClip[a]=px*mapped[19+a]+py*mapped[23+a]+pz*mapped[27+a]+mapped[31+a];}
    double sx=(clip[0]/clip[3]+1)*825,sy=(1-clip[1]/clip[3])*525;
    pixelError=Math.max(pixelError,Math.max(Math.abs(sx-c.projectedX(wx,wz,wy)),Math.abs(sy-c.projectedY(wx,wz,wy))));
    near(c.projectedX(wx,wz,wy),sx,.02,"actual original X pixel");near(c.projectedY(wx,wz,wy),sy,.02,"actual original Y pixel");
    near(c.depth(wx,wz,wy)/c.farPlane(),clip[3],.000002,"actual source normalized depth");
    near((mappedClip[0]/mappedClip[3]+1)*825,sx,.02,"mapped source X pixel");near((1-mappedClip[1]/mappedClip[3])*525,sy,.02,"mapped source Y pixel");
   }
  }
  System.out.println("PASS actual PC camera "+args.length+" stable runtime snapshots, "+checks+" checks, maxPixelError="+pixelError+" maxProjectionError="+matrixError+"; original PC projection/view matrices, normal wheel zoom; GPU/art parity still pending");
 }
}
''')
java=['MapSceneSnapshot.java','PcMap.java','LiveCameraCheck.java']
production=['SceneCamera.java','PcEffectCoordinates.java']
subprocess.run(['javac','--release','17','-d',str(out),*[str(out/p)for p in java],*[str(ROOT/'app/src/main/java/game/sanguo/mobile'/p)for p in production]],check=True)
subprocess.run(['java','-cp',str(out),'game.sanguo.mobile.LiveCameraCheck',*[str(fixture/r['file'])for r in manifest['records']]],check=True)
