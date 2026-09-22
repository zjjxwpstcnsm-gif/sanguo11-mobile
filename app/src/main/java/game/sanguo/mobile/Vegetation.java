package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Opaque merged vegetation: one renderable per visible 8x8 chunk, never one per tree. */
final class Vegetation {
    static final int CHUNK=8,SEED=0x3110405;
    static Set<Hex> exclusions(MapSceneSnapshot snapshot){
        Set<Hex> result=new HashSet<>(snapshot.ground.bases);
        for(MapSceneSnapshot.Item item:snapshot.items)if(item.facility!=null)result.add(item.hex);
        return result;
    }
    static List<SceneMesh> build(MapSceneSnapshot.Ground ground,Set<Hex> excluded,List<SceneMesh> previous,SceneMesh tree,SceneMesh farTree){
        Map<String,SceneMesh> cache=new HashMap<>();for(SceneMesh m:previous)cache.put(m.chunkQ+":"+m.chunkR,m);
        List<SceneMesh> result=new ArrayList<>();
        for(int r=0;r<ground.height;r+=CHUNK)for(int q=0;q<ground.width;q+=CHUNK){
            if(Thread.currentThread().isInterrupted())return Collections.emptyList();
            long fingerprint=SEED^ground.mapSeed;List<Hex> cells=new ArrayList<>();
            // Height sampling has a bounded neighborhood; include it in invalidation.
            for(int rr=r-6;rr<r+CHUNK+6;rr++)for(int qq=q-6;qq<q+CHUNK+6;qq++){
                Hex h=new Hex(qq,rr);int t=ground.valid(h)?ground.terrain[rr*ground.width+qq]:-1;
                fingerprint=(fingerprint^(t+1+(excluded.contains(h)?64:0)))*1099511628211L;
                fingerprint=(fingerprint^Float.floatToIntBits(ground.surface.overrides.getOrDefault(h,-1f)))*1099511628211L;
            }
            fingerprint^=ground.grid.staggered?1:0;fingerprint^=Float.floatToIntBits(ground.grid.offset);
            SceneMesh prior=cache.get(q+":"+r);if(prior!=null&&prior.fingerprint==fingerprint){result.add(prior);continue;}
            for(int rr=r;rr<Math.min(r+CHUNK,ground.height);rr++)for(int qq=q;qq<Math.min(q+CHUNK,ground.width);qq++){
                Hex h=new Hex(qq,rr);if(eligible(ground,excluded,h))cells.add(h);
            }
            if(cells.isEmpty())continue;
            SceneMesh near=merge(ground,cells,tree,false),far=merge(ground,cells,farTree,true);
            near.chunkQ=q;near.chunkR=r;near.fingerprint=fingerprint;near.distant=far;result.add(near);
        }
        return Collections.unmodifiableList(result);
    }
    static boolean eligible(MapSceneSnapshot.Ground g,Set<Hex> excluded,Hex h){
        if(!g.valid(h)||excluded.contains(h)||g.terrain[h.r*g.width+h.q]!=World.Terrain.FOREST.ordinal())return false;
        for(Hex n:h.neighbors()){
            if(!g.valid(n)||excluded.contains(n))return false;
            int t=g.terrain[n.r*g.width+n.q];
            if(t==World.Terrain.ROAD.ordinal()||t==World.Terrain.PLANK_ROAD.ordinal()||t==World.Terrain.MOUNTAIN_PATH.ordinal()||g.surface.water(n))return false;
        }
        return true;
    }
    private static SceneMesh merge(MapSceneSnapshot.Ground ground,List<Hex> cells,SceneMesh tree,boolean far){
        List<Float> vertices=new ArrayList<>();List<Integer> indices=new ArrayList<>();List<Float> tex=new ArrayList<>();
        float minX=Float.MAX_VALUE,minZ=minX,maxX=-minX,maxZ=-minX;
        for(Hex h:cells){
            int hash=(h.q*73856093)^(h.r*19349663)^SEED^ground.mapSeed;hash^=hash>>>16;
            if(far&&(hash&1)!=0)continue;
            float x=ground.grid.x(h)+((hash&1)==0?-.24f:.24f),z=ground.grid.z(h)+((hash&2)==0?-.24f:.24f);
            float scale=.66f+((hash>>>4)&7)*.035f,y=ground.surface.meshHeight(x,z);
            int offset=vertices.size()/7;
            for(int v=0;v<tree.vertices.length;v+=7){vertices.add(tree.vertices[v]*scale+x);vertices.add(tree.vertices[v+1]*scale+y);vertices.add(tree.vertices[v+2]*scale+z);for(int k=3;k<7;k++)vertices.add(tree.vertices[v+k]);}
            for(int i:tree.indices)indices.add(offset+i);for(float uv:tree.uv)tex.add(uv);
            minX=Math.min(minX,x);minZ=Math.min(minZ,z);maxX=Math.max(maxX,x);maxZ=Math.max(maxZ,z);
        }
        if(vertices.isEmpty()){minX=ground.grid.x(cells.get(0));maxX=minX;minZ=ground.grid.z(cells.get(0));maxZ=minZ;}
        SceneMesh mesh=new SceneMesh(vertices,indices,(minX+maxX)/2,(minZ+maxZ)/2,Math.max(maxX-minX,maxZ-minZ)/2+1);
        mesh.uv=new float[tex.size()];for(int i=0;i<tex.size();i++)mesh.uv[i]=tex.get(i);return mesh;
    }
}
