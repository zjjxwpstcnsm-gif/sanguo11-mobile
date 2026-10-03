#!/usr/bin/env python3
"""Actual camera/picking against known world points on fixture triangles.

Tests projection, reciprocal-depth interpolation and touch anchoring. Fixture
triangles are numerical tests, not game artwork or PC-reference evidence.
"""
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[1]
out=ROOT/'app/build/pc-perspective-interaction';out.mkdir(parents=True,exist_ok=True)
stub=out/'MapSceneSnapshot.java';stub.write_text('''package game.sanguo.mobile;
final class MapSceneSnapshot {static final class Ground {Object pcMap;float minX,minZ,maxX,maxZ;}}
final class TerrainSurface {static final float MAX_HEIGHT=2.6f;}
final class SceneMesh {float[] vertices;int[] indices;SceneMesh(float[] v,int[] i){vertices=v;indices=i;}}
''')
pc=out/'PcMap.java';pc.write_text('''package game.sanguo.core;
public final class PcMap {public static final float SCALE=.025f,MAX_HEIGHT=255*SCALE;}
''')
test=out/'PerspectiveInteraction.java';test.write_text('''package game.sanguo.mobile;
public final class PerspectiveInteraction {
 static int checks;static void near(double a,double b,double tolerance){checks++;if(!Double.isFinite(a)||Math.abs(a-b)>tolerance)throw new AssertionError("interaction "+checks+" "+a+" != "+b);}
 public static void main(String[] args){
  float[] vertices={-1,0,-1,0,0,0,0, 1,2,-1,0,0,0,0, 0,.5f,1,0,0,0,0};
  SceneMesh mesh=new SceneMesh(vertices,new int[]{0,1,2});
  for(boolean perspective:new boolean[]{false,true})for(int facing:new int[]{-1,1})for(float tilt:new float[]{40,55,70})
  for(float yaw:new float[]{0,35,90,180})for(float span:new float[]{3,8,35}){
   SceneCamera c=new SceneCamera();c.perspective=perspective;c.heightLimit=6.375f;c.width=1080;c.height=1232;c.span=span;c.tilt=tilt;c.yaw=yaw;c.facing=facing;c.x=43;c.z=51;
   // Known barycentric world point with grade, member offset, yaw and scale.
   float angle=.7f,scale=1.2f,ox=.3f,oy=.5f,oz=.2f,member=.8f,gx=.2f,gz=-.1f;
   double[] world=new double[3];double[] weights={.2,.3,.5};
   for(int i=0;i<3;i++){double lx=vertices[i*7],ly=vertices[i*7+1],lz=vertices[i*7+2];
    world[0]+=weights[i]*(43+scale*Math.cos(angle)*(ox+member*lx)+scale*Math.sin(angle)*(oz+member*lz));
    world[1]+=weights[i]*(1.5+scale*(oy+member*(ly+gx*lx+gz*lz)));
    world[2]+=weights[i]*(51-scale*Math.sin(angle)*(ox+member*lx)+scale*Math.cos(angle)*(oz+member*lz));
   }
   float sx=c.screenX((float)world[0],(float)world[2],(float)world[1]),sy=c.screenY((float)world[0],(float)world[2],(float)world[1]);
   float height=ScenePicking.hit(c,mesh,43,1.5f,51,angle,scale,sx,sy,ox,oy,oz,member,gx,gz);
   // The query pixels above are float32. Compare to the actual pixel ray's
   // intersection, not the world point before projection/pixel rounding.
   // Independent ray-plane reference; original assertion tolerances retained.
   double[][] tri=new double[3][3];float rc=(float)Math.cos(angle)*scale,rs=(float)Math.sin(angle)*scale;
   for(int i=0;i<3;i++){float lx=vertices[i*7],ly=vertices[i*7+1],lz=vertices[i*7+2];tri[i][0]=43+rc*(ox+member*lx)+rs*(oz+member*lz);tri[i][1]=1.5f+scale*(oy+member*(ly+gx*lx+gz*lz));tri[i][2]=51-rs*(ox+member*lx)+rc*(oz+member*lz);}
   if(perspective){
    double d=c.eyeDistance(),u=(sx-c.width*.5)/c.pixels()/d,v=-(sy-c.height*.5)/c.pixels()/d;
    double[] eye={c.x+c.backX()*d*c.cos(),.8+d*c.sin(),c.z+c.rightX()*d*c.cos()};
    double[] ray={c.rightX()*u-c.backX()*c.sin()*v-c.backX()*c.cos(),c.cos()*v-c.sin(),-c.backX()*u-c.rightX()*c.sin()*v-c.rightX()*c.cos()};
    double[] ab=new double[3],ac=new double[3];for(int a=0;a<3;a++){ab[a]=tri[1][a]-tri[0][a];ac[a]=tri[2][a]-tri[0][a];}
    double[] n={ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]};
    double numerator=0,denominator=0;for(int a=0;a<3;a++){numerator+=n[a]*(tri[0][a]-eye[a]);denominator+=n[a]*ray[a];}
    for(int a=0;a<3;a++)world[a]=eye[a]+numerator/denominator*ray[a];
   }
   near(height,world[1],.0002);near(c.worldX(sx,sy,height),world[0],.0001);near(c.worldZ(sx,sy,height),world[2],.0001);
   c.zoom(1.5f,sx,sy,height);near(c.screenX((float)world[0],(float)world[2],height),sx,.004);near(c.screenY((float)world[0],(float)world[2],height),sy,.004);
   c.orbit(10,5,sx,sy,height);near(c.screenX((float)world[0],(float)world[2],height),sx,.004);near(c.screenY((float)world[0],(float)world[2],height),sy,.004);
   if(perspective){c.pan(17,-11,sx+17,sy-11,height);near(c.screenX((float)world[0],(float)world[2],height),sx+17,.004);near(c.screenY((float)world[0],(float)world[2],height),sy-11,.004);}
  }
  System.out.println("PASS actual camera/picking "+checks+" world triangle, perspective-correct height, zoom/orbit/pan anchors; HOST ONLY, actual GPU/touch gameplay validation still required");
 }
}
''')
subprocess.run(['javac','--release','17','-d',str(out),str(stub),str(pc),str(test),str(ROOT/'app/src/main/java/game/sanguo/mobile/SceneCamera.java'),str(ROOT/'app/src/main/java/game/sanguo/mobile/ScenePicking.java')],check=True)
subprocess.run(['java','-cp',str(out),'game.sanguo.mobile.PerspectiveInteraction'],check=True)
