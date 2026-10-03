package game.sanguo.mobile;

import game.sanguo.core.Hex;

/** Canonical shore attributes, transported with the rounded presentation mesh.
 * The gameplay mask and every cell centre remain unchanged. */
final class WaterVisualField {
    static final int VERSION=3;
    static final float BAND=1.5f;
    private final MapSceneSnapshot.Ground ground;
    private final Samples flows,distances;
    WaterVisualField(MapSceneSnapshot.Ground ground){this.ground=ground;flows=ground.pcMap==null?null:new Samples();distances=ground.pcMap==null?null:new Samples();}
    /** Bounded memo of exact immutable-field results across neighboring chunks and
     * camera windows. Float-bit keys preserve signed zero. No quantization, terrain
     * simplification, shader change or unbounded boxed coordinate map. */
    private static final class Samples {
        private final long[] keys=new long[65536];
        private final float[] values=new float[65536];
        private final boolean[] occupied=new boolean[65536];
        private static long key(float x,float z){return ((long)Float.floatToRawIntBits(x)<<32)|(Float.floatToRawIntBits(z)&0xffffffffL);}
        private static int slot(long key){long mixed=(key^(key>>>33))*0xff51afd7ed558ccdL;mixed=(mixed^(mixed>>>33))*0xc4ceb9fe1a85ec53L;return (int)(mixed^(mixed>>>33))&65535;}
        synchronized float get(float x,float z){long key=key(x,z);int slot=slot(key);return occupied[slot]&&keys[slot]==key?values[slot]:Float.NaN;}
        synchronized void put(float x,float z,float value){long key=key(x,z);int slot=slot(key);keys[slot]=key;values[slot]=value;occupied[slot]=true;}
    }
    /** Principal local water extent, biased toward a fixed downstream reference.
     * Positive definite tensor keeps the direction in one hemisphere; no per-cell rotations.
     * Radius 2.4 plus the shore band's 1.5 fits inside the chunk's eight-cell halo. */
    float flowAngle(float x,float z){
        if(flows!=null){float cached=flows.get(x,z);if(!Float.isNaN(cached))return cached;}
        Hex center=ground.grid.cell(x,z);float xx=.2f,zz=.2f,xz=0;
        for(int r=center.r-4;r<=center.r+4;r++)for(int q=center.q-6;q<=center.q+6;q++){
            if(!ground.surface.water(q,r))continue;
            float dx=ground.grid.x(q,r)-x,dz=-(ground.grid.z(q,r)-z),d2=(dx*dx+dz*dz)/(2.4f*2.4f);
            if(d2>=1)continue;float k=(1-d2)*(1-d2)*(1-d2);
            xx+=dx*dx*k;zz+=dz*dz*k;xz+=dx*dz*k;
        }
        float result=(float)Math.atan2(.8f*xz+.6f*zz,.8f*xx+.6f*xz);
        if(flows!=null)flows.put(x,z,result);return result;
    }
    float distance(float x,float z){
        if(distances!=null){float cached=distances.get(x,z);if(!Float.isNaN(cached))return cached;}
        Hex center=ground.grid.cell(x,z);boolean wet=ground.surface.water(center);
        float best=BAND;
        for(int r=center.r-3;r<=center.r+3;r++)for(int q=center.q-5;q<=center.q+5;q++){
            if(ground.surface.water(q,r)==wet)continue;
            float dx=Math.max(0,Math.abs(x-ground.grid.x(q,r))-.5f);
            float dz=Math.max(0,Math.abs(z-ground.grid.z(q,r))-.5f);
            best=Math.min(best,(float)Math.sqrt(dx*dx+dz*dz));
        }
        float result=wet?best:-best;
        if(distances!=null)distances.put(x,z,result);return result;
    }

    /** Two bounded half-edge relaxation steps on the shared shore graph. No new
     * triangles, flood fill or per-LOD silhouettes. Junctions and VOID contacts are
     * pinned; a centre never moves. Even the tightest turn moves at most 3/16 per
     * axis, leaving single-cell rivers, islands and port centres intact.
     * Offsets are exact multiples of 1/32, cached lazily on CPU mesh workers.
     * The cache publishes atomically because picking can read it on the UI thread. */
    static final class Shoreline {
        private final MapSceneSnapshot.Ground g;
        private final int minX,minZ,width,height;
        private final java.util.concurrent.atomic.AtomicIntegerArray offsets;
        Shoreline(MapSceneSnapshot.Ground ground){
            g=ground;minX=(int)Math.floor(g.minX*2)-2;minZ=(int)Math.floor(g.minZ*2)-2;
            width=(int)Math.ceil(g.maxX*2)-minX+3;height=(int)Math.ceil(g.maxZ*2)-minZ+3;
            long count=(long)width*height;
            offsets=new java.util.concurrent.atomic.AtomicIntegerArray(count>0&&count<=1048576?(int)count:0);
        }
        // Quadrants NW, NE, SE, SW in world X/Z; 16 marks contact with non-playable exterior.
        private int mask(float x,float z){
            int mask=0;
            for(int i=0;i<4;i++){
                Hex h=g.grid.cell(x+(i==1||i==2?.015625f:-.015625f),z+(i>=2?.015625f:-.015625f));
                if(!g.valid(h))return 16;
                int t=g.terrain[h.r*g.width+h.q];
                if(t==game.sanguo.core.World.Terrain.WATER.ordinal()||t==game.sanguo.core.World.Terrain.SEA.ordinal()
                    ||t==game.sanguo.core.World.Terrain.SHALLOWS.ordinal()||t==game.sanguo.core.World.Terrain.NON_NAVIGABLE_WATER.ordinal())mask|=1<<i;
            }
            return mask;
        }
        private static int edges(int m){
            if(m==16)return 0;
            return (((m>>>1)^(m>>>2))&1) | ((((m>>>3)^m)&1)<<1)
                | (((m^(m>>>1))&1)<<2) | ((((m>>>2)^(m>>>3))&1)<<3);
        }
        // First-pass displacement in units of 1/32: east, west, north, south.
        private static int firstX(int e){return Integer.bitCount(e)==2?4*((e&1)-((e>>>1)&1)):0;}
        private static int firstZ(int e){return Integer.bitCount(e)==2?4*(((e>>>3)&1)-((e>>>2)&1)):0;}
        private int offset(float x,float z){
            int q=Math.round(x*2)-minX,r=Math.round(z*2)-minZ;
            int at=offsets.length()>0&&q>=0&&r>=0&&q<width&&r<height?r*width+q:-1;
            int old=at<0?0:offsets.get(at);if(old!=0)return old;
            int e=edges(mask(x,z)),dx=0,dz=0;
            if(Integer.bitCount(e)==2){
                dx=6*((e&1)-((e>>>1)&1));dz=6*(((e>>>3)&1)-((e>>>2)&1));
                for(int i=0;i<4;i++)if((e&(1<<i))!=0){
                    int near=edges(mask(x+(i==0?.5f:i==1?-.5f:0),z+(i==2?-.5f:i==3?.5f:0)));
                    dx+=firstX(near)/4;dz+=firstZ(near)/4;
                }
            }
            int encoded=0x10000|((dx+16)<<8)|(dz+16);
            if(at>=0)offsets.compareAndSet(at,0,encoded);return encoded;
        }
        float dx(float x,float z){if(g.pcMap!=null)return 0;return (((offset(x,z)>>>8)&255)-16)/32f;}
        float dz(float x,float z){if(g.pcMap!=null)return 0;return ((offset(x,z)&255)-16)/32f;}
        /** Transport all fine vertices through the same original fan, including
         * attributes sampled before transport. This keeps every LOD plane identical. */
        float[] project(float x,float z){
            Hex h=g.grid.cell(x,z);float cx=g.grid.x(h),cz=g.grid.z(h),rx=x-cx,rz=z-cz;
            float scale=2*Math.max(Math.abs(rx),Math.abs(rz));if(scale<.000001f)return new float[]{x,z};
            float bx=rx/scale,bz=rz/scale;int side=side(bx,bz);
            float[] a=SceneMesh.EDGE[side],b=SceneMesh.EDGE[(side+1)%8];
            float t=Math.abs(b[0]-a[0])>.1f?(bx-a[0])/(b[0]-a[0]):(bz-a[1])/(b[1]-a[1]);
            return new float[]{x+scale*((1-t)*dx(cx+a[0],cz+a[1])+t*dx(cx+b[0],cz+b[1])),
                z+scale*((1-t)*dz(cx+a[0],cz+a[1])+t*dz(cx+b[0],cz+b[1]))};
        }
        private static int side(float x,float z){return Math.abs(z)>=Math.abs(x)?(z<0?(x<0?0:1):(x>0?4:5)):(x>0?(z<0?2:3):(z>0?6:7));}
        static final class Point {
            final float x,z;final Hex cell;
            Point(float x,float z,Hex cell){this.x=x;this.z=z;this.cell=cell;}
        }
        private Point inverseIn(float x,float z,Hex h){
            if(!g.valid(h))return null;
            float cx=g.grid.x(h),cz=g.grid.z(h),rx=x-cx,rz=z-cz;
            if(rx==0&&rz==0)return new Point(x,z,h);
            for(int i=0;i<8;i++){
                float[] a=SceneMesh.EDGE[i],b=SceneMesh.EDGE[(i+1)%8];
                float ax=a[0]+dx(cx+a[0],cz+a[1]),az=a[1]+dz(cx+a[0],cz+a[1]);
                float bx=b[0]+dx(cx+b[0],cz+b[1]),bz=b[1]+dz(cx+b[0],cz+b[1]);
                float det=ax*bz-bx*az,u=(rx*bz-rz*bx)/det,v=(ax*rz-az*rx)/det;
                if(u>=-.00005f&&v>=-.00005f&&u+v<=1.00005f)return new Point(cx+u*a[0]+v*b[0],cz+u*a[1]+v*b[1],h);
            }
            return null;
        }
        Point inverse(float x,float z){
            Hex h=g.grid.cell(x,z);float cx=g.grid.x(h),cz=g.grid.z(h);boolean changed=false;
            for(float[] edge:SceneMesh.EDGE)if(dx(cx+edge[0],cz+edge[1])!=0||dz(cx+edge[0],cz+edge[1])!=0){changed=true;break;}
            if(!changed)return new Point(x,z,h);
            Point point=inverseIn(x,z,h);if(point!=null)return point;
            for(int r=h.r-1;r<=h.r+1;r++)for(int q=h.q-2;q<=h.q+2;q++){
                if(q==h.q&&r==h.r)continue;point=inverseIn(x,z,new Hex(q,r));if(point!=null)return point;
            }
            return new Point(x,z,h);
        }
        void transport(SceneMesh mesh){
            if(g.pcMap!=null){
                // Source shoreline displacement is identically zero. Keep even
                // signed-zero addition semantics without allocating a float[2]
                // per original vertex; topology and every GPU attribute stay exact.
                for(int i=0;i<mesh.vertices.length;i+=7){mesh.vertices[i]+=0f;mesh.vertices[i+2]+=0f;}
                if(mesh.distant!=null)transport(mesh.distant);return;
            }
            for(int i=0;i<mesh.vertices.length;i+=7){float[] p=project(mesh.vertices[i],mesh.vertices[i+2]);mesh.vertices[i]=p[0];mesh.vertices[i+2]=p[1];}
            if(mesh.distant!=null)transport(mesh.distant);
        }
    }
}
