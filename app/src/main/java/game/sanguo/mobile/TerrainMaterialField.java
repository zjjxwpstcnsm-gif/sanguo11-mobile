package game.sanguo.mobile;

import game.sanguo.core.*;

/** Compact global material field; immutable snapshot input, no gameplay RNG or camera state. */
final class TerrainMaterialField {
    static final int VERSION=1;
    static final float RADIUS=1.65f;
    private static final World.Terrain[] TYPES=World.Terrain.values();
    final MapSceneSnapshot.Ground ground;
    TerrainMaterialField(MapSceneSnapshot.Ground ground){this.ground=ground;}
    float[] sample(float x,float z){return sample(x,z,0);}
    float[] sample(float x,float z,float slope){
        float[] w=new float[4];
        if(!Float.isFinite(x)||!Float.isFinite(z))return new float[]{0,1,0,0};
        Hex center=ground.grid.cell(x,z);
        for(int r=center.r-3;r<=center.r+3;r++)for(int q=center.q-4;q<=center.q+4;q++){
            Hex h=new Hex(q,r);if(!ground.valid(h)||ground.surface.water(h))continue;
            float dx=x-ground.grid.x(h),dz=z-ground.grid.z(h),d2=(dx*dx+dz*dz)/(RADIUS*RADIUS);
            if(d2>=1)continue;
            float k=(1-d2)*(1-d2)*(1-d2);
            if(ground.bases.contains(h)){w[1]+=k;continue;}
            switch(TYPES[ground.terrain[r*ground.width+q]]){
                case SAND:w[2]+=k;break;
                case MOUNTAIN:case DAM:w[3]+=.9f*k;w[1]+=.1f*k;break;
                case ROAD:case MOUNTAIN_PATH:case PLANK_ROAD:w[1]+=.8f*k;w[3]+=.2f*k;break;
                case FOREST:case SWAMP:case POISON:w[0]+=.65f*k;w[1]+=.35f*k;break;
                default:w[0]+=.9f*k;w[1]+=.1f*k;
            }
        }
        float sum=w[0]+w[1]+w[2]+w[3];if(sum<1e-8f)return new float[]{0,1,0,0};
        for(int j=0;j<4;j++)w[j]/=sum;
        // Slope exposes existing rock/soil, without exporting mountain colors into desert interiors.
        float exposed=Math.min(.55f,Math.max(0,slope-.12f))*(1-w[2]);
        float transfer=w[0]*exposed;w[0]-=transfer;w[1]+=transfer*.4f;w[3]+=transfer*.6f;
        return w;
    }
    /** Separate terrain-only stream: canonical UV, tangent quaternion, water mask and macro tone. */
    void attach(SceneMesh mesh){
        int count=mesh.vertices.length/7;mesh.surfaceData=new float[count*8];
        for(int i=0;i<count;i++){
            int v=i*7,s=i*8;float x=mesh.vertices[v],z=mesh.vertices[v+2];
            boolean water=mesh.vertices[v+5]>mesh.vertices[v+3];

            mesh.surfaceData[s]=x;mesh.surfaceData[s+1]=-z;
            float dx=(ground.surface.sample(x+.25f,z)-ground.surface.sample(x-.25f,z))*2;
            float dz=(ground.surface.sample(x,z+.25f)-ground.surface.sample(x,z-.25f))*2;
            float[] w=sample(x,z,(float)Math.sqrt(dx*dx+dz*dz));System.arraycopy(w,0,mesh.vertices,v+3,4);
            // Rotation from +Z to the global surface normal. Positive quaternion handedness.
            float len=(float)Math.sqrt(dx*dx+1+dz*dz),nx=-dx/len,ny=1/len,nz=-dz/len;
            float qw=(float)Math.sqrt((1+nz)*.5f),scale=.5f/qw;
            mesh.surfaceData[s+2]=-ny*scale;mesh.surfaceData[s+3]=nx*scale;
            mesh.surfaceData[s+4]=0;mesh.surfaceData[s+5]=qw;
            // Water keeps the authoritative cell mask; water shading is replaced in S11.
            mesh.surfaceData[s+6]=water?1:0;
            mesh.surfaceData[s+7]=.96f+.04f*(float)(Math.sin(x*.19)*Math.cos(z*.17));
        }
    }
}
