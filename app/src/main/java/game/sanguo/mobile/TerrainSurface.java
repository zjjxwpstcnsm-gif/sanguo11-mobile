package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Deterministic presentation-only field. Coordinates are projected world units, never axial indices. */
final class TerrainSurface {
    static final int METADATA_VERSION=2;
    private static final World.Terrain[] TYPES=World.Terrain.values();
    static final float MAX_HEIGHT=2.6f;
    static final float LANDFORM_RADIUS=3.4f;
    final MapSceneSnapshot.Ground ground;
    final Map<Hex,Float> overrides;
    private final float[] targets;private final byte[] constraints;
    private final java.util.concurrent.ConcurrentHashMap<Long,Float> lattice=new java.util.concurrent.ConcurrentHashMap<>();
    TerrainSurface(MapSceneSnapshot.Ground g){this(g,Collections.emptyMap());}
    TerrainSurface(MapSceneSnapshot.Ground g,Map<Hex,Float> values){
        ground=g;Map<Hex,Float> copy=new HashMap<>();
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
        long key=((long)ix<<32)^(iz&0xffffffffL);Float v=lattice.get(key);if(v!=null)return v;
        float value=compute(x,z);lattice.putIfAbsent(key,value);return value;
    }
    private float compute(float x,float z){
        Hex cell=ground.grid.cell(x,z);float sum=0,weight=0,limit=MAX_HEIGHT;
        // Compact support includes all staggered neighbors, including both sides of chunk edges.
        for(int r=cell.r-5;r<=cell.r+5;r++)for(int q=cell.q-7;q<=cell.q+7;q++){
            float dx=Math.abs(x-ground.grid.x(q,r)),dz=Math.abs(z-ground.grid.z(q,r));
            float distance=(float)Math.sqrt(dx*dx+dz*dz);
            int at=q>=0&&r>=0&&q<ground.width&&r<ground.height?r*ground.width+q:-1;
            if(distance<LANDFORM_RADIUS){float k=1-distance/LANDFORM_RADIUS;k=k*k*k;sum+=(at<0?0:targets[at])*k;weight+=k;}
            int constraint=at<0?1:constraints[at];
            if(constraint!=0){float edge=Math.max(Math.max(dx-.5f,dz-.5f),0);limit=Math.min(limit,(constraint==1?0:.08f)+edge*.6f);}
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
        Hex h=ground.grid.cell(camera.worldX(sx),camera.worldZ(sy)+y*(float)(camera.cos()/camera.sin())*camera.facing);return ground.valid(h)?h:null;
    }
    float rayHeight(SceneCamera camera,float sx,float sy){
        float x=camera.worldX(sx),base=camera.worldZ(sy),cot=(float)(camera.cos()/camera.sin())*camera.facing;
        // Bounded ray march independent of national triangle count; select first visible surface.
        float previous=MAX_HEIGHT,previousF=previous-meshHeight(x,base+previous*cot);
        for(int n=1;n<=104;n++){
            float y=MAX_HEIGHT*(1-n/104f),f=y-meshHeight(x,base+y*cot);
            if(f<=0&&previousF>=0){float lo=y,hi=previous;for(int j=0;j<14;j++){float mid=(lo+hi)*.5f;if(mid>meshHeight(x,base+mid*cot))hi=mid;else lo=mid;}
                return (lo+hi)*.5f;}
            previous=y;previousF=f;
        }return Float.NaN;
    }
    int color(float x,float z,int base,boolean water){
        return water?SceneMesh.shade(base,.93f):base;
    }
}
