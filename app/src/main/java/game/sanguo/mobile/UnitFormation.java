package game.sanguo.mobile;

/** Eight bounded GPU draw instances; shared single-member pose geometry, no army-size mesh. */
final class UnitFormation {
    static final int CAPACITY=8;
    private static final float[][] FOOT={{0,0},{-.14f,-.12f},{.14f,-.12f},{-.28f,-.24f},{0,-.24f},{.28f,-.24f},{-.14f,-.36f},{.14f,-.36f}};
    private static final float[][] HORSE={{0,0},{-.26f,-.20f},{.26f,-.20f},{0,-.40f}};
    final float[][] placement=new float[CAPACITY][4],grade=new float[CAPACITY][2];
    int count=1;
    float rootY;
    private MapSceneSnapshot.Ground previousGround;
    private float previousX,previousZ,previousYaw,previousScale;
    private boolean previousNaval,previousSingle,previousCavalry;
    long updates;
    boolean sample(UnitVisual unit,int strength,boolean naval,int lod,MapSceneSnapshot.Ground ground,
                float x,float z,float yaw,float rootScale) {
        int nextCount=unit.representatives(naval,strength,lod);
        if(previousGround==ground&&previousX==x&&previousZ==z&&previousYaw==yaw&&previousScale==rootScale&&previousNaval==naval&&previousSingle==unit.singleModel(naval)&&previousCavalry==unit.cavalry()&&count==nextCount)return false;
        previousGround=ground;previousX=x;previousZ=z;previousYaw=yaw;previousScale=rootScale;previousNaval=naval;previousSingle=unit.singleModel(naval);previousCavalry=unit.cavalry();count=nextCount;updates++;
        rootY=naval?.02f:ground.surface.meshHeight(x,z)+.02f;
        float scale=Math.max(.001f,rootScale),c=(float)Math.cos(yaw),s=(float)Math.sin(yaw);
        boolean single=unit.singleModel(naval);
        float[][] slots=unit.cavalry()?HORSE:FOOT;
        for(int i=0;i<CAPACITY;i++) {
            float[] member=placement[i],slope=grade[i];
            if(i>=count){java.util.Arrays.fill(member,0);java.util.Arrays.fill(slope,0);continue;}
            // Prefix-stable layout: no survivor/leader re-centering at troop or LOD boundaries.
            float dx=single?0:slots[i][0],dz=single?0:slots[i][1];
            float wx=x+(c*dx+s*dz)*scale,wz=z+(-s*dx+c*dz)*scale;
            member[0]=dx;member[2]=dz;member[3]=single?1:.78f;
            member[1]=naval?0:(ground.surface.meshHeight(wx,wz)+.02f-rootY)/scale;
            if(naval){slope[0]=slope[1]=0;continue;}
            // A local contact plane follows each pair of feet / axle / hull support.
            // This is presentation height sampling, never a new terrain or movement rule.
            float e=.10f*scale;
            slope[0]=(ground.surface.meshHeight(wx+c*e,wz-s*e)-ground.surface.meshHeight(wx-c*e,wz+s*e))/(2*e);
            slope[1]=(ground.surface.meshHeight(wx+s*e,wz+c*e)-ground.surface.meshHeight(wx-s*e,wz-c*e))/(2*e);
        }
        return true;
    }
    float hit(SceneCamera camera,SceneMesh mesh,float x,float z,float yaw,float rootScale,float sx,float sy) {
        float best=Float.NEGATIVE_INFINITY;
        for(int i=0;i<count;i++) {
            float[] m=placement[i],g=grade[i];
            best=Math.max(best,ScenePicking.hit(camera,mesh,x,rootY,z,yaw,rootScale,sx,sy,m[0],m[1],m[2],m[3],g[0],g[1]));
        }
        return best;
    }
}
