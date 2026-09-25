package game.sanguo.mobile;

import game.sanguo.core.*;

/** Compact global material field; immutable snapshot input, no gameplay RNG or camera state. */
final class TerrainMaterialField {
    static final int VERSION=4;
    static final float RADIUS=3.2f;
    private static final World.Terrain[] TYPES=World.Terrain.values();
    final MapSceneSnapshot.Ground ground;
    final WaterVisualField waterField;
    TerrainMaterialField(MapSceneSnapshot.Ground ground){this.ground=ground;this.waterField=new WaterVisualField(ground);}
    float[] sample(float x,float z){return sample(x,z,0);}
    float[] sample(float x,float z,float slope){
        float[] w=new float[4];
        if(!Float.isFinite(x)||!Float.isFinite(z))return new float[]{0,1,0,0};
        Hex center=ground.grid.cell(x,z);
        for(int r=center.r-5;r<=center.r+5;r++)for(int q=center.q-5;q<=center.q+5;q++){
            if(q<0||r<0||q>=ground.width||r>=ground.height||TYPES[ground.terrain[r*ground.width+q]]==World.Terrain.VOID||ground.surface.water(q,r))continue;
            float dx=x-ground.grid.x(q,r),dz=z-ground.grid.z(q,r),d2=(dx*dx+dz*dz)/(RADIUS*RADIUS);
            if(d2>=1)continue;
            float distance=(float)Math.sqrt(d2),t=1-distance;
            float k=t*t*t*t*(1+4*distance); // C2 support shared in world space, not cell-owned tint.
            if(ground.isBase(q,r)){w[1]+=k;continue;}
            switch(TYPES[ground.terrain[r*ground.width+q]]){
                case SAND:w[2]+=k;break;
                case MOUNTAIN:case DAM:w[3]+=.9f*k;w[1]+=.1f*k;break;
                case ROAD:w[0]+=.82f*k;w[1]+=.18f*k;break;
                case MOUNTAIN_PATH:case PLANK_ROAD:w[1]+=.8f*k;w[3]+=.2f*k;break;
                case FOREST:case SWAMP:case POISON:w[0]+=.65f*k;w[1]+=.35f*k;break;
                default:w[0]+=.9f*k;w[1]+=.1f*k;
            }
        }
        normalize(w);
        // Slope exposes existing rock/soil, without exporting mountain colors into desert interiors.
        float safeSlope=Float.isFinite(slope)?Math.max(0,slope):0;
        float exposed=Math.min(.55f,Math.max(0,safeSlope-.12f))*(1-w[2]);
        float transfer=w[0]*exposed;w[0]-=transfer;w[1]+=transfer*.4f;w[3]+=transfer*.6f;
        // Keep site/mountain-path centres recognisable. Ordinary ROAD retains its
        // existing grass/soil biome treatment: centre stamps make dense road regions polka-dotted. These local masks are NOT widened with
        // the biome filter, and never modify the independent hard land/water footprint.
        float constraint=0;
        for(int r=center.r-1;r<=center.r+1;r++)for(int q=center.q-1;q<=center.q+1;q++){
            Hex h=new Hex(q,r);if(!ground.valid(h)||ground.surface.water(h))continue;
            World.Terrain type=TYPES[ground.terrain[r*ground.width+q]];
            boolean site=ground.bases.contains(h);
            if(!site&&type!=World.Terrain.MOUNTAIN_PATH&&type!=World.Terrain.PLANK_ROAD)continue;
            float dx=x-ground.grid.x(h),dz=z-ground.grid.z(h);
            float radius=site?.8f:.55f;
            float t=Math.max(0,1-(dx*dx+dz*dz)/(radius*radius));
            constraint=Math.max(constraint,t*t*(3-2*t)*(site?.90f:.72f));
        }
        for(int j=0;j<4;j++)w[j]*=1-constraint;
        w[1]+=constraint;
        normalize(w);
        return w;
    }
    static void normalize(float[] w){
        float sum=0;for(int i=0;i<4;i++){w[i]=Float.isFinite(w[i])?Math.max(0,w[i]):0;sum+=w[i];}
        if(!Float.isFinite(sum)||sum<1e-8f){w[0]=w[2]=w[3]=0;w[1]=1;return;}
        for(int i=0;i<4;i++)w[i]/=sum;
    }
    /** Separate terrain-only stream: canonical UV, tangent quaternion, water mask and macro tone. */
    void attach(SceneMesh mesh){attach(mesh,null);}
    void attach(SceneMesh mesh,SceneMesh.BuildStats stats){
        int count=mesh.vertices.length/7;mesh.surfaceData=new float[count*8];
        // Chunk-local memo only: shared fan vertices have identical field inputs.
        // Preserve separate wet/dry flow channels at the same shore coordinate.
        long[] sampleKeys=new long[4096];int[] sampleVertices=new int[4096];
        boolean[] wet=new boolean[count];
        for(int i=0;i<count;i++){
            int v=i*7,s=i*8;float x=mesh.vertices[v],z=mesh.vertices[v+2];
            boolean water=mesh.vertices[v+5]>mesh.vertices[v+3];wet[i]=water;
            long key=((long)Float.floatToIntBits(x)<<32)|(Float.floatToIntBits(z)&0xffffffffL);
            long mixed=(key^(key>>>33))*0xff51afd7ed558ccdL;
            mixed=(mixed^(mixed>>>33))*0xc4ceb9fe1a85ec53L;
            int slot=(int)(mixed^(mixed>>>33))&4095;
            int encoded=sampleVertices[slot];
            if(encoded!=0&&sampleKeys[slot]==key){
                int prior=encoded-1;
                if(stats!=null)stats.sharedSamples++;
                System.arraycopy(mesh.vertices,prior*7+3,mesh.vertices,v+3,4);
                System.arraycopy(mesh.surfaceData,prior*8,mesh.surfaceData,s,8);
                if(wet[prior]!=water)mesh.surfaceData[s+7]=water?waterField.flowAngle(x,z):.96f+.04f*(float)(Math.sin(x*.19)*Math.cos(z*.17));
                continue;
            }
            sampleKeys[slot]=key;sampleVertices[slot]=i+1;


            mesh.surfaceData[s]=x;mesh.surfaceData[s+1]=-z;
            // Sample component clocks, rather than issuing several clock calls per vertex.
            boolean timed=stats!=null&&(i&255)==0;
            if(timed)stats.timedFieldSamples++;
            long sectionStarted=timed?System.nanoTime():0;
            float dx=(ground.surface.sample(x+.25f,z)-ground.surface.sample(x-.25f,z))*2;
            float dz=(ground.surface.sample(x,z+.25f)-ground.surface.sample(x,z-.25f))*2;
            if(timed){stats.heightNanos+=System.nanoTime()-sectionStarted;sectionStarted=System.nanoTime();}
            float[] w=sample(x,z,(float)Math.sqrt(dx*dx+dz*dz));System.arraycopy(w,0,mesh.vertices,v+3,4);
            if(timed)stats.blendNanos+=System.nanoTime()-sectionStarted;
            // Rotation from +Z to the global surface normal. Positive quaternion handedness.
            float len=(float)Math.sqrt(dx*dx+1+dz*dz),nx=-dx/len,ny=1/len,nz=-dz/len;
            float qw=(float)Math.sqrt((1+nz)*.5f),scale=.5f/qw;
            mesh.surfaceData[s+2]=-ny*scale;mesh.surfaceData[s+3]=nx*scale;
            mesh.surfaceData[s+4]=0;mesh.surfaceData[s+5]=qw;
            // Signed visual shore distance, NOT gameplay water depth. Shared by both batches.
            sectionStarted=timed?System.nanoTime():0;
            mesh.surfaceData[s+6]=waterField.distance(x,z);
            mesh.surfaceData[s+7]=water?waterField.flowAngle(x,z):.96f+.04f*(float)(Math.sin(x*.19)*Math.cos(z*.17));
            if(timed)stats.shoreNanos+=System.nanoTime()-sectionStarted;
        }
    }
}
