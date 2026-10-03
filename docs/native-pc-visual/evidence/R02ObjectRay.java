package game.sanguo.mobile;
/** Diagnostic against production triangle picking at arbitrary orbit and animated transforms. */
public class R02ObjectRay {
 public static void main(String[] args){
  SceneCamera c=new SceneCamera();c.width=1080;c.height=1920;c.span=5;
  int checks=0;
  for(float yaw:new float[]{15,37,83,127,230,359})for(float tilt:new float[]{40,55,70})for(float scale:new float[]{.7f,1,1.3f}){
   c.yaw=yaw;c.tilt=tilt;c.x=13;c.z=-7;float rotation=.9f,x=13,y=.8f,z=-7;
   SceneMesh m=SceneMesh.proxy(0,0xffffffff);float cos=(float)Math.cos(rotation)*scale,sin=(float)Math.sin(rotation)*scale;
   for(int i=0;i<m.indices.length;i+=3){float px=0,py=0,pz=0;
    for(int j=0;j<3;j++){int at=m.indices[i+j]*7;px+=m.vertices[at]/3;py+=m.vertices[at+1]/3;pz+=m.vertices[at+2]/3;}
    float wx=x+cos*px+sin*pz,wy=y+scale*py,wz=z-sin*px+cos*pz;
    float hit=ScenePicking.hit(c,m,x,y,z,rotation,scale,c.screenX(wx,wz),c.screenY(wx,wz,wy));
    if(!Float.isFinite(hit)||hit<wy-.001f)throw new AssertionError("visible/occluded transformed triangle ordering");checks++;
   }
  }
  System.out.println("PASS "+checks+" arbitrary-orbit animated-scale triangle samples; host only");
 }
}
