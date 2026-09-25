package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Deterministic presentation-only field. Coordinates are projected world units, never axial indices. */
final class TerrainSurface {
    static final int METADATA_VERSION=3;
    private static final World.Terrain[] TYPES=World.Terrain.values();
    static final float MAX_HEIGHT=2.6f;
    static final float LANDFORM_RADIUS=3.4f;
    final MapSceneSnapshot.Ground ground;
    final Map<Hex,Float> overrides;
    private final float[] targets;private final byte[] constraints;
    // One bounded, immutable-ground cache shared by CPU workers and the UI. The old
    // 32768-entry map cleared itself during every national sweep, so both consumers
    // continually recomputed the same surface. Zero is the empty slot; bits+1 stores
    // all non-negative finite heights (including zero) without a second validity array.
    private static final int MAX_LATTICE_SAMPLES=262144; // <= 1 MiB per Ground
    private final java.util.concurrent.atomic.AtomicIntegerArray lattice;
    private final int latticeX,latticeZ,latticeWidth,latticeHeight;
    TerrainSurface(MapSceneSnapshot.Ground g){this(g,Collections.emptyMap());}
    TerrainSurface(MapSceneSnapshot.Ground g,Map<Hex,Float> values){
        ground=g;
        latticeX=(int)Math.floor(g.minX*2)-2;latticeZ=(int)Math.floor(g.minZ*2)-2;
        latticeWidth=(int)Math.ceil(g.maxX*2)-latticeX+3;
        latticeHeight=(int)Math.ceil(g.maxZ*2)-latticeZ+3;
        long samples=(long)latticeWidth*latticeHeight;
        lattice=new java.util.concurrent.atomic.AtomicIntegerArray(samples>0&&samples<=MAX_LATTICE_SAMPLES?(int)samples:0);
        Map<Hex,Float> copy=new HashMap<>();
        for(Map.Entry<Hex,Float> e:values.entrySet())if(g.valid(e.getKey())&&Float.isFinite(e.getValue()))copy.put(e.getKey(),Math.max(0,Math.min(MAX_HEIGHT,e.getValue())));
        overrides=Collections.unmodifiableMap(copy);
        targets=new float[g.width*g.height];constraints=new byte[targets.length];
        for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++){
            Hex h=new Hex(q,r);int at=r*g.width+q;targets[at]=target(h);
            if(!g.valid(h)||water(h)||g.bases.contains(h))constraints[at]=1;
            else {World.Terrain t=TYPES[g.terrain[at]];if(t==World.Terrain.ROAD||t==World.Terrain.MOUNTAIN_PATH||t==World.Terrain.PLANK_ROAD)constraints[at]=2;}
        }
    }
    boolean water(Hex h){return h!=null&&water(h.q,h.r);}
    boolean water(int q,int r){if(q<0||r<0||q>=ground.width||r>=ground.height)return false;switch(TYPES[ground.terrain[r*ground.width+q]]){
        case WATER:case SEA:case SHALLOWS:case NON_NAVIGABLE_WATER:return true;default:return false;}}
    float target(Hex h){
        if(!ground.valid(h)||water(h)||ground.bases.contains(h))return 0;
        Float override=overrides.get(h);if(override!=null)return override;
        switch(TYPES[ground.terrain[h.r*ground.width+h.q]]){
            case MOUNTAIN:
                float x=ground.grid.x(h),z=ground.grid.z(h);
                // Broad, world-anchored ridges; the existing mountain mask supplies the region.
                float ridge=1-Math.abs((float)Math.sin(x*.16+z*.11+.7*Math.sin(z*.08)));
                return 1.6f+ridge*.9f;
            case FOREST:return .22f;
            case SAND:return .12f;
            case MOUNTAIN_PATH:case PLANK_ROAD:case ROAD:return .03f;
            default:return .06f;
        }
    }
    float sample(float x,float z){
        int ix=Math.round(x*2),iz=Math.round(z*2);
        if(Math.abs(x*2-ix)>.00001f||Math.abs(z*2-iz)>.00001f)return compute(x,z);
        int q=ix-latticeX,r=iz-latticeZ;
        if(lattice.length()==0||q<0||r<0||q>=latticeWidth||r>=latticeHeight)return compute(x,z);
        int key=r*latticeWidth+q,encoded=lattice.get(key);
        if(encoded!=0)return Float.intBitsToFloat(encoded-1);
        float value=compute(x,z);lattice.compareAndSet(key,0,Float.floatToIntBits(value)+1);return value;
    }
    private float compute(float x,float z){
        Hex cell=ground.grid.cell(x,z);float sum=0,weight=0,limit=MAX_HEIGHT;
        // Compact support includes all staggered neighbors, including both sides of chunk edges.
        for(int r=cell.r-5;r<=cell.r+5;r++)for(int q=cell.q-7;q<=cell.q+7;q++){
            float dx=Math.abs(x-ground.grid.x(q,r)),dz=Math.abs(z-ground.grid.z(q,r));
            float distance=(float)Math.sqrt(dx*dx+dz*dz);
            int at=q>=0&&r>=0&&q<ground.width&&r<ground.height?r*ground.width+q:-1;
            if(distance<LANDFORM_RADIUS){float t=distance/LANDFORM_RADIUS,k=1-t;k=k*k*k*k*(1+4*t);sum+=(at<0?0:targets[at])*k;weight+=k;}
            int constraint=at<0?1:constraints[at];
            if(constraint!=0){float edge=Math.max(Math.max(dx-.5f,dz-.5f),0);float t=Math.min(1,edge/4.5f);
                limit=Math.min(limit,(constraint==1?0:.08f)+MAX_HEIGHT*t*t*(3-2*t));}
        }
        float value=weight==0?0:sum/weight;
        // Explicit height paint wins at its center, smoothly joining the regional field.
        Float painted=overrides.get(cell);
        if(painted!=null){float dx=x-ground.grid.x(cell),dz=z-ground.grid.z(cell);
            float k=Math.max(0,1-(float)Math.sqrt(dx*dx+dz*dz)/.5f);k=k*k*(3-2*k);value=value*(1-k)+painted*k;}
        return Math.min(limit,value);
    }
    float at(Hex h){return sample(ground.grid.x(h),ground.grid.z(h));}
    // Exact piecewise-linear height of the eight-triangle cell fan, shared with the mesh and ray picker.
    float meshHeight(float x,float z){
        Hex h=ground.grid.cell(x,z);float cx=ground.grid.x(h),cz=ground.grid.z(h),dx=x-cx,dz=z-cz;
        float scale=2*Math.max(Math.abs(dx),Math.abs(dz));if(scale<.00001f)return sample(cx,cz);
        float bx=dx/scale,bz=dz/scale;float[] a,b;
        int side;
        if(Math.abs(bz)>=Math.abs(bx)){side=bz<0?(bx<0?0:1):(bx>0?4:5);}else side=bx>0?(bz<0?2:3):(bz>0?6:7);
        a=SceneMesh.EDGE[side];b=SceneMesh.EDGE[(side+1)%8];
        float t=Math.abs(b[0]-a[0])>.1f?(bx-a[0])/(b[0]-a[0]):(bz-a[1])/(b[1]-a[1]);
        float boundary=sample(cx+a[0],cz+a[1])*(1-t)+sample(cx+b[0],cz+b[1])*t;
        return sample(cx,cz)*(1-scale)+boundary*scale;
    }
    Hex pick(SceneCamera camera,float sx,float sy){
        float y=rayHeight(camera,sx,sy);if(!Float.isFinite(y))return null;
        Hex h=ground.grid.cell(camera.worldX(sx,sy,y),camera.worldZ(sx,sy,y));return ground.valid(h)?h:null;
    }
    float rayHeight(SceneCamera camera,float sx,float sy){
        float x=camera.worldX(sx,sy,0),base=camera.worldZ(sx,sy,0);
        float cotX=(float)(camera.cos()/camera.sin()*camera.backX()),cotZ=(float)(camera.cos()/camera.sin()*camera.rightX());
        // Test the actual eight-triangle fan (both display LODs share its planes).
        // The bounded height interval crosses only a small rectangle of source cells.
        float endX=x+MAX_HEIGHT*cotX,endZ=base+MAX_HEIGHT*cotZ;
        int minQ=Integer.MAX_VALUE,minR=minQ,maxQ=Integer.MIN_VALUE,maxR=maxQ;
        for(float wx:new float[]{Math.min(x,endX)-1,Math.max(x,endX)+1})
            for(float wz:new float[]{Math.min(base,endZ)-1,Math.max(base,endZ)+1}){
                Hex h=ground.grid.cell(wx,wz);minQ=Math.min(minQ,h.q);maxQ=Math.max(maxQ,h.q);minR=Math.min(minR,h.r);maxR=Math.max(maxR,h.r);
            }
        float best=Float.NEGATIVE_INFINITY;
        for(int r=minR;r<=maxR;r++)for(int q=minQ;q<=maxQ;q++){
            Hex h=new Hex(q,r);if(!ground.valid(h))continue;
            float hx=ground.grid.x(h),hz=ground.grid.z(h),hy=sample(hx,hz);
            for(int i=0;i<8;i++){
                float[] a=SceneMesh.EDGE[i],b=SceneMesh.EDGE[(i+1)%8];
                float ay=sample(hx+a[0],hz+a[1])-hy,by=sample(hx+b[0],hz+b[1])-hy;
                float det=a[0]*b[1]-b[0]*a[1];
                float slopeX=(ay*b[1]-by*a[1])/det,slopeZ=(a[0]*by-b[0]*ay)/det;
                float denominator=1-slopeX*cotX-slopeZ*cotZ;
                if(Math.abs(denominator)<1e-7f)continue;
                float y=(hy+slopeX*(x-hx)+slopeZ*(base-hz))/denominator;
                if(y<-.0001f||y>MAX_HEIGHT+.0001f||y<best)continue;
                float dx=x+y*cotX-hx,dz=base+y*cotZ-hz;
                float u=(dx*b[1]-dz*b[0])/det,v=(a[0]*dz-a[1]*dx)/det;
                if(u>=-.00001f&&v>=-.00001f&&u+v<=1.00001f)best=Math.max(best,y);
            }
        }
        return Float.isFinite(best)?Math.max(0,best):Float.NaN;
    }
    int color(float x,float z,int base,boolean water){
        return water?SceneMesh.shade(base,.93f):base;
    }
}
