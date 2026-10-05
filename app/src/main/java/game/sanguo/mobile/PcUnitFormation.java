package game.sanguo.mobile;

import game.sanguo.core.War;

/** Original 76-slot formation offsets, detached terrain contact and GPU instance inputs. */
final class PcUnitFormation {
    final float[] members=new float[76*4];
    private final float[][] layouts=new float[8][];
    int count=1;float rootY;long updates;
    private MapSceneSnapshot.Ground previous;
    private float previousX,previousZ,previousYaw;
    private int previousLayout=-1;
    private boolean previousNaval;
    PcUnitFormation(PcUnits assets){for(int i=0;i<8;i++)layouts[i]=assets.layout(i);}
    boolean sample(UnitVisual unit,int troops,boolean naval,MapSceneSnapshot.Ground ground,float x,float z,float yaw){
        int next=PcUnits.members(troops,unit.singleModel(naval)),layout=unit.status==War.Status.NORMAL?0:7;
        if(previous==ground&&previousX==x&&previousZ==z&&previousYaw==yaw&&previousLayout==layout&&previousNaval==naval&&count==next)return false;
        previous=ground;previousX=x;previousZ=z;previousYaw=yaw;previousLayout=layout;previousNaval=naval;count=next;
        updates++;
        // Boats share the authored face water plane. The old constant .02
        // submerged hulls when the original water-height field was restored.
        rootY=(naval?ground.surface.pcWaterHeight(x,z):ground.surface.meshHeight(x,z))+.02f;
        float c=(float)Math.cos(yaw),s=(float)Math.sin(yaw);
        java.util.Arrays.fill(members,0);
        for(int i=0;i<count;i++){
            float dx=next==1?0:layouts[layout][i*2]*.05f,dz=next==1?0:layouts[layout][i*2+1]*.05f;
            float wx=x+c*dx+s*dz,wz=z-s*dx+c*dz;
            members[i*4]=dx;members[i*4+1]=naval?0:ground.surface.meshHeight(wx,wz)+.02f-rootY;members[i*4+2]=dz;members[i*4+3]=1;
        }
        return true;
    }
    float hit(SceneCamera camera,SceneMesh mesh,float x,float z,float yaw,float sx,float sy){
        float best=Float.NEGATIVE_INFINITY;
        for(int i=0;i<count;i++)best=Math.max(best,ScenePicking.hit(camera,mesh,x,rootY,z,yaw,1,sx,sy,members[i*4],members[i*4+1],members[i*4+2],1,0,0));
        return best;
    }
}
