package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Opaque merged vegetation: one renderable per visible 8x8 chunk, never one per tree. */
final class Vegetation {
    static final int CHUNK=8,SEED=0x3111203, PLACEMENT_VERSION=3, ASSET_VERSION=129;
    static boolean forest(MapSceneSnapshot.Ground ground,Hex h){
        return ground.valid(h)&&ground.terrain[h.r*ground.width+h.q]==World.Terrain.FOREST.ordinal();
    }
    static Set<Hex> exclusions(MapSceneSnapshot snapshot){
        Set<Hex> result=new HashSet<>(snapshot.ground.bases);
        // Armies move through the existing forest. Only permanent footprints clear
        // vegetation; a unit revision must not erase/rebuild its cell and neighbours.
        for(MapSceneSnapshot.Item item:snapshot.items)if(item.facility!=null)result.add(item.hex);
        return result;
    }
    static List<SceneMesh> build(MapSceneSnapshot.Ground ground,Set<Hex> excluded,List<SceneMesh> previous,SceneMesh tree,SceneMesh farTree){
        return build(ground,excluded,previous,tree,farTree,tree,farTree);
    }
    static List<SceneMesh> build(MapSceneSnapshot.Ground ground,Set<Hex> excluded,List<SceneMesh> previous,SceneMesh tree,SceneMesh farTree,SceneMesh upland,SceneMesh farUpland){
        Map<String,SceneMesh> cache=new HashMap<>();for(SceneMesh m:previous)cache.put(m.chunkQ+":"+m.chunkR,m);
        List<SceneMesh> result=new ArrayList<>();
        for(int r=0;r<ground.height;r+=CHUNK)for(int q=0;q<ground.width;q+=CHUNK){
            if(Thread.currentThread().isInterrupted())return Collections.emptyList();
            long fingerprint=SEED^ground.mapSeed^ground.width*31L^ground.height;List<Hex> cells=new ArrayList<>();
            // Height sampling has a bounded neighborhood; include it in invalidation.
            for(int rr=r-8;rr<r+CHUNK+8;rr++)for(int qq=q-8;qq<q+CHUNK+8;qq++){
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
            SceneMesh near=merge(ground,cells,tree,upland),far=merge(ground,cells,farTree,farUpland);
            near.chunkQ=q;near.chunkR=r;near.fingerprint=fingerprint;near.distant=far;result.add(near);
        }
        return Collections.unmodifiableList(result);
    }
    /** Production streaming path. The legacy offline overload above retains its versioned
     * export contract; live edits use map identity and bounded content fingerprints. */
    static List<SceneMesh> buildWindow(MapSceneSnapshot.Ground g,Set<Hex> excluded,
            List<SceneMesh> previous,FieldAssets assets,SceneMesh.TerrainWindow window)throws Exception {
        // At national scale retain every far triangle, but batch 16x16 cells.
        // Near/mid streaming retains its existing 8x8 granularity and edit halo.
        int chunk=window.span>=40?16:CHUNK;float radius=chunk*.75f+1;
        SceneMesh[][] models=new SceneMesh[5][2];
        String[] names={"tree","tree-upland","shrub","rock-ledge","rock-talus"};
        for(int i=0;i<names.length;i++)for(int lod=window.span<14?0:1;lod<2;lod++)models[i][lod]=assets.mesh(names[i]+"-lod"+lod);
        Map<Long,SceneMesh> cache=new HashMap<>();for(SceneMesh m:previous)cache.put(key(m.chunkQ,m.chunkR),m);
        List<SceneMesh> result=new ArrayList<>();
        for(int r=0;r<g.height;r+=chunk)for(int q=0;q<g.width;q+=chunk){
            if(Thread.currentThread().isInterrupted())return Collections.emptyList();
            float cx=g.grid.x(q+chunk/2,r+chunk/2),cz=g.grid.z(q+chunk/2,r+chunk/2);
            if(!window.contains(cx,cz,radius))continue;
            boolean detailed=window.span<14&&Math.hypot(cx-window.x,cz-window.z)<20;
            long hash=SEED^PLACEMENT_VERSION^LandscapeProfile.VERSION^g.mapIdentity^g.width*31L^g.height^(detailed?0x808:0)^((long)chunk<<48)^((long)ASSET_VERSION<<32);
            hash=(hash^Float.floatToIntBits(g.grid.offset))*1099511628211L;hash^=g.grid.staggered?1:0;
            hash=(hash^g.sourceMapWidth)*1099511628211L;
            hash=(hash^(g.originalNational?1:0))*1099511628211L;
            hash=(hash^g.sourceOriginX)*1099511628211L;hash=(hash^g.sourceOriginY)*1099511628211L;
            for(int rr=r-8;rr<r+chunk+8;rr++)for(int qq=q-8;qq<q+chunk+8;qq++){
                Hex h=new Hex(qq,rr);int t=g.valid(h)?g.terrain[rr*g.width+qq]:-1;
                hash=(hash^(t+1+(excluded.contains(h)?64:0)+(g.bases.contains(h)?128:0)))*1099511628211L;
                hash=(hash^Float.floatToIntBits(g.surface.overrides.getOrDefault(h,-1f)))*1099511628211L;
            }
            SceneMesh prior=cache.get(key(q,r));if(prior!=null&&prior.fingerprint==hash){result.add(prior);continue;}
            Batch near=new Batch(g),far=new Batch(g);
            for(int rr=r;rr<Math.min(r+chunk,g.height);rr++)for(int qq=q;qq<Math.min(q+chunk,g.width);qq++){
                Hex h=new Hex(qq,rr);if(!g.valid(h))continue;
                int cascade=cascadeRegion(g,h);
                if(cascade>=0){
                    String id=cascade==3?"fall-hukou":cascade==1?"fall-wide":"fall-narrow";
                    if(detailed)near.cascade(h,excluded,assets.mesh(id+"-lod0"),cascade==3?1.15f:2.1f);
                    far.cascade(h,excluded,assets.mesh(id+"-lod1"),cascade==3?1.15f:2.1f);
                }
                if(wallRegion(g,h)){
                    for(Hex n:h.neighbors())if(wallRegion(g,n)&&g.source(n).x==g.source(h).x+1){
                        if(detailed)near.wall(h,n,excluded,assets.mesh("wall-earth-lod0"));
                        far.wall(h,n,excluded,assets.mesh("wall-earth-lod1"));
                    }
                    if(g.source(h).x%4==2){
                        if(detailed)near.landmark(h,excluded,assets.mesh("beacon-han-lod0"));
                        far.landmark(h,excluded,assets.mesh("beacon-han-lod1"));
                    }
                }
                if(taihuShore(g,h)&&!excluded.contains(h)){
                    String shore=(mix(h.q*73856093^h.r*19349663^SEED)&3)==0?"shore-rock":"shore-reeds";
                    if(detailed)near.shore(h,excluded,assets.mesh(shore+"-lod0"));
                    far.shore(h,excluded,assets.mesh(shore+"-lod1"));
                }
                // Only existing adjacent logical road cells connect. Each undirected edge
                // belongs to its lower cell ID, including edges that cross a chunk boundary.
                if(path(g,h))for(Hex n:h.neighbors())if(g.valid(n)&&path(g,n)&&key(h.q,h.r)<key(n.q,n.r)){
                    if(detailed)near.road(h,n);far.road(h,n);
                }
                if(path(g,h)){if(detailed)near.junction(h);far.junction(h);}
                for(Placement a:placements(g,excluded,h)){
                    String cliff=a.family>=3?cliffRegion(g,h):null;
                    if(detailed)near.append(cliff==null?models[a.family][a.detail?0:1]:assets.mesh(cliff+"-lod"+(a.detail?0:1)),a);
                    far.append(cliff==null?models[a.family][1]:assets.mesh(cliff+"-lod1"),a);
                }
            }
            SceneMesh distant=far.mesh(cx,cz,radius);distant.chunkQ=q;distant.chunkR=r;distant.landscapeChunkSize=chunk;SceneMesh m=detailed?near.mesh(cx,cz,radius):distant;m.distant=distant;m.chunkQ=q;m.chunkR=r;m.landscapeChunkSize=chunk;m.fingerprint=hash;result.add(m);
        }
        return Collections.unmodifiableList(result);
    }
    static boolean replacementPending(SceneMesh old,Set<SceneMesh> wanted,Set<SceneMesh> resident){
        for(SceneMesh next:wanted){
            boolean overlap=next.chunkQ<old.chunkQ+old.landscapeChunkSize&&old.chunkQ<next.chunkQ+next.landscapeChunkSize
                &&next.chunkR<old.chunkR+old.landscapeChunkSize&&old.chunkR<next.chunkR+next.landscapeChunkSize;
            if(overlap&&!resident.contains(next))return true;
        }
        return false;
    }
    private static long key(int q,int r){return ((long)r<<32)|(q&0xffffffffL);}
    // Presentation anchors selected on this project's recovered source map.
    // Historical PC waterfall exact cells are still REFERENCE_MISSING. Crops
    // use the same source mapping; unrelated/custom maps never inherit them.
    static int cascadeRegion(MapSceneSnapshot.Ground g,Hex h){
        if(!g.originalNational
            ||!g.valid(h)||g.terrain[h.r*g.width+h.q]!=World.Terrain.NON_NAVIGABLE_WATER.ordinal())return -1;
        game.sanguo.core.map.SourceGridCoord s=g.source(h);
        for(var entry:LandscapeLandmarks.ALL)if(entry.cascade()>=0&&s.x==entry.x()&&s.y==entry.y())return entry.cascade();
        return -1;
    }
    static boolean national(MapSceneSnapshot.Ground g){return g.originalNational;}
    /** Bounded northern ridge interpretation; no new logical wall or collision. */
    static boolean wallRegion(MapSceneSnapshot.Ground g,Hex h){
        if(!national(g)||!g.valid(h)||g.terrain[h.r*g.width+h.q]!=World.Terrain.MOUNTAIN.ordinal())return false;
        var s=g.source(h);return s.y==15&&s.x>=86&&s.x<=99;
    }
    static String cliffRegion(MapSceneSnapshot.Ground g,Hex h){
        if(!national(g))return null;var s=g.source(h);
        if(s.x>=68&&s.x<=86&&s.y>=25&&s.y<=60)return "cliff-sandstone";
        if(s.x>=136&&s.x<=158&&s.y>=48&&s.y<=78)return "cliff-granite";
        if(s.x>=12&&s.x<=52&&s.y>=155&&s.y<=195)return "cliff-karst";
        return null;
    }
    /** The compact existing blocked lake west of Wu; no new water/shore cells.
     * The exact PC-game lake label/anchor remains a documented interpretation. */
    static boolean taihuRegion(MapSceneSnapshot.Ground g,Hex h){
        if(!national(g)||!g.valid(h))return false;
        var s=g.source(h);return s.x>=171&&s.x<=179&&s.y>=103&&s.y<=111;
    }
    static boolean taihuShore(MapSceneSnapshot.Ground g,Hex h){
        if(!taihuRegion(g,h)||g.terrain[h.r*g.width+h.q]!=World.Terrain.NON_NAVIGABLE_WATER.ordinal())return false;
        for(Hex n:h.neighbors())if(g.valid(n)&&!g.surface.water(n)&&!path(g,n))return true;
        return false;
    }
    static boolean path(MapSceneSnapshot.Ground g,Hex h){
        if(!g.valid(h))return false;int t=g.terrain[h.r*g.width+h.q];
        return t==World.Terrain.ROAD.ordinal()||t==World.Terrain.PLANK_ROAD.ordinal()||t==World.Terrain.MOUNTAIN_PATH.ordinal();
    }
    /** A movement requirement is not a timber material. Classify each endpoint
     * from its actual terrain; never propagate plank surfacing into a rough path. */
    static boolean timber(MapSceneSnapshot.Ground g,Hex h){
        return g.valid(h)&&g.terrain[h.r*g.width+h.q]==World.Terrain.PLANK_ROAD.ordinal();
    }
    static final class Placement {
        final float x,z,scale,angle;final int family;final boolean detail;
        Placement(float x,float z,float scale,float angle,int family,boolean detail){this.x=x;this.z=z;this.scale=scale;this.angle=angle;this.family=family;this.detail=detail;}
    }
    /** Opaque silhouettes keep alpha-test/mip overdraw out of the foliage path. */
    static List<Placement> placements(MapSceneSnapshot.Ground g,Set<Hex> excluded,Hex h){
        List<Placement> out=new ArrayList<>();if(!g.valid(h))return out;
        int type=g.terrain[h.r*g.width+h.q];boolean rock=type==World.Terrain.MOUNTAIN.ordinal();
        if(!rock&&type!=World.Terrain.FOREST.ordinal()&&type!=World.Terrain.PLAIN.ordinal())return out;
        float hx=g.grid.x(h),hz=g.grid.z(h);
        boolean forest=type==World.Terrain.FOREST.ordinal();
        for(int i=0;i<(rock?1:forest?6:3);i++){
            int hash=mix(Float.floatToIntBits(hx)*73856093^Float.floatToIntBits(hz)*19349663^SEED^g.mapIdentity^i*83492791);
            float x=hx+(((hash>>>2)&1023)/1023f-.5f)*.70f,z=hz+(((hash>>>12)&1023)/1023f-.5f)*.70f;
            // Candidate must belong to this cell: no duplicate ownership at block/hex seams.
            if(!h.equals(g.grid.cell(x,z))||!clear(g,excluded,h,x,z))continue;
            float cluster=.58f+.24f*(float)(Math.sin(x*.71+1.7)*Math.cos(z*.57-2));
            // Preserve every previous tree/shrub. Extra forest understory uses reduced
            // meshes even up close, increasing canopy coverage without multiplying LOD0 cost.
            float chance=rock?.24f:i>=3?.76f+.18f*cluster:density(g,x,z)*cluster;
            if((mix(hash^0x55ab)&65535)/65535f>chance)continue;
            float regional=.5f+.35f*(float)(Math.sin(x*.055)*Math.cos(z*.047));
            int family=rock?3+((hash>>>21)&1):i==2?2:((hash>>>22)&255)/255f<regional?0:1;
            out.add(new Placement(x,z,LandscapeProfile.sceneryScale(family,.65f+((hash>>>4)&255)/255f*.30f),(hash&65535)/65535f*6.283185f,family,i<3));
        }
        return out;
    }
    static boolean clear(MapSceneSnapshot.Ground g,Set<Hex> excluded,Hex h,float x,float z){
        // One-cell safety apron protects fixed structures, roads and port approaches.
        for(int r=h.r-2;r<=h.r+2;r++)for(int q=h.q-2;q<=h.q+2;q++){
            Hex n=new Hex(q,r);float dx=g.grid.x(n)-x,dz=g.grid.z(n)-z;
            if(dx*dx+dz*dz>2.1f)continue;
            if(!g.valid(n)||excluded.contains(n)||g.bases.contains(n)||path(g,n)||g.surface.water(n))return false;
        }
        return true;
    }
    private static final class Batch {
        final MapSceneSnapshot.Ground g;
        float[] v=new float[4096],uv=new float[1024];int[] indices=new int[2048];
        int floats,texels,indexCount;
        // Primitive growing buffers retain the exact vertex/triangle order. Only
        // one bounded chunk is built at a time; no Float/Integer per mesh element.
        void reserve(int vertices,int texture,int triangles){
            if(floats+vertices>v.length)v=Arrays.copyOf(v,Math.max(floats+vertices,v.length*2));
            if(texels+texture>uv.length)uv=Arrays.copyOf(uv,Math.max(texels+texture,uv.length*2));
            if(indexCount+triangles>indices.length)indices=Arrays.copyOf(indices,Math.max(indexCount+triangles,indices.length*2));
        }
        void triangle(int a,int b,int c){reserve(0,0,3);indices[indexCount++]=a;indices[indexCount++]=b;indices[indexCount++]=c;}
        void quad(int o){triangle(o,o+2,o+1);triangle(o,o+3,o+2);}
        Batch(MapSceneSnapshot.Ground g){this.g=g;}
        boolean safeLandmark(float x,float z,Set<Hex> excluded){
            Hex h=g.grid.cell(x,z);return g.valid(h)&&!excluded.contains(h)&&!g.bases.contains(h)
                &&g.terrain[h.r*g.width+h.q]==World.Terrain.MOUNTAIN.ordinal();
        }
        void landmark(Hex h,Set<Hex> excluded,SceneMesh model){
            float x=g.grid.x(h),z=g.grid.z(h);
            if(!safeLandmark(x,z,excluded))return;
            for(int i=0;i<model.vertices.length;i+=7)if(!safeLandmark(x+model.vertices[i],z+model.vertices[i+2],excluded))return;
            append(model,new Placement(x,z,1,0,3,true));
        }
        void shore(Hex h,Set<Hex> excluded,SceneMesh model){
            float x=g.grid.x(h),z=g.grid.z(h);Hex bank=null;
            for(Hex n:h.neighbors())if(g.valid(n)&&!g.surface.water(n)&&!path(g,n)&&!excluded.contains(n)&&!g.bases.contains(n)){bank=n;break;}
            if(bank==null)return;
            // A low fringe on the blocked wet side, with open central water retained.
            float dx=g.grid.x(bank)-x,dz=g.grid.z(bank)-z,len=(float)Math.hypot(dx,dz);
            x+=dx/len*.20f;z+=dz/len*.20f;
            for(int rr=h.r-2;rr<=h.r+2;rr++)for(int qq=h.q-2;qq<=h.q+2;qq++){
                Hex n=new Hex(qq,rr);float X=g.grid.x(n)-x,Z=g.grid.z(n)-z;
                if(X*X+Z*Z<2.1f&&(excluded.contains(n)||g.bases.contains(n)||path(g,n)))return;
            }
            float angle=(mix(h.q*73856093^h.r*19349663^SEED)&65535)/65535f*6.283185f,cs=(float)Math.cos(angle),sn=(float)Math.sin(angle);
            // Sample each full triangle so a reed/stone never covers a navigable
            // channel, land-cell centre, port or facility, even at a bank corner.
            for(int i=0;i<model.indices.length;i+=3)for(int a=0;a<=4;a++)for(int b=0;b<=4-a;b++){
                float u=a/4f,t=b/4f,w=1-u-t;int A=model.indices[i]*7,B=model.indices[i+1]*7,C=model.indices[i+2]*7;
                float X=model.vertices[A]*u+model.vertices[B]*t+model.vertices[C]*w,Z=model.vertices[A+2]*u+model.vertices[B+2]*t+model.vertices[C+2]*w;
                Hex at=g.grid.cell(x+(X*cs+Z*sn)*.70f,z+(-X*sn+Z*cs)*.70f);
                if(!taihuRegion(g,at)||g.terrain[at.r*g.width+at.q]!=World.Terrain.NON_NAVIGABLE_WATER.ordinal()||excluded.contains(at)||g.bases.contains(at))return;
            }
            append(model,new Placement(x,z,.70f,angle,3,true));
        }
        void wall(Hex h,Hex n,Set<Hex> excluded,SceneMesh model){
            float x=g.grid.x(h),z=g.grid.z(h),dx=g.grid.x(n)-x,dz=g.grid.z(n)-z,len=(float)Math.hypot(dx,dz);
            float sx=dz/len,sz=-dx/len;
            // Reject the whole wall if any sampled face footprint leaves blocked mountains.
            for(int i=0;i<model.indices.length;i+=3)for(int a=0;a<=4;a++)for(int b=0;b<=4-a;b++){
                float u=a/4f,t=b/4f,w=1-u-t;int A=model.indices[i]*7,B=model.indices[i+1]*7,C=model.indices[i+2]*7;
                float vx=model.vertices[A]*u+model.vertices[B]*t+model.vertices[C]*w;
                float vz=model.vertices[A+2]*u+model.vertices[B+2]*t+model.vertices[C+2]*w;
                if(!safeLandmark(x+dx*vz+sx*vx,z+dz*vz+sz*vx,excluded))return;
            }
            int offset=floats/7;reserve(model.vertices.length,model.uv.length,model.indices.length);
            for(int i=0;i<model.vertices.length;i+=7){
                float px=x+dx*model.vertices[i+2]+sx*model.vertices[i],pz=z+dz*model.vertices[i+2]+sz*model.vertices[i];
                v[floats++]=px;v[floats++]=g.surface.meshHeight(px,pz)-.015f+model.vertices[i+1];v[floats++]=pz;
                for(int k=3;k<7;k++)v[floats++]=model.vertices[i+k];
            }
            for(int i:model.indices)indices[indexCount++]=offset+i;
            System.arraycopy(model.uv,0,uv,texels,model.uv.length);texels+=model.uv.length;
        }
        void vertex(float x,float y,float z,int mat,float u,float t){
            reserve(7,2,0);v[floats++]=x;v[floats++]=y;v[floats++]=z;
            for(int i=0;i<4;i++)v[floats++]=1f;
            uv[texels++]=(mat+.03f+.94f*u)/8;uv[texels++]=.03f+.94f*t;
        }
        void append(SceneMesh model,Placement a){
            int offset=floats/7;float cs=(float)Math.cos(a.angle),sn=(float)Math.sin(a.angle);
            float base=g.surface.meshHeight(a.x,a.z);
            reserve(model.vertices.length,model.uv.length,model.indices.length);
            for(int i=0;i<model.vertices.length;i+=7){
                float x=(model.vertices[i]*cs+model.vertices[i+2]*sn)*a.scale+a.x;
                float z=(-model.vertices[i]*sn+model.vertices[i+2]*cs)*a.scale+a.z;
                // Rock foot conforms per vertex, embedding the bottom below the canonical surface.
                float y=a.family>=3?g.surface.meshHeight(x,z)-.035f:base-.015f;
                v[floats++]=x;v[floats++]=model.vertices[i+1]*a.scale+y;v[floats++]=z;
                for(int k=3;k<7;k++)v[floats++]=model.vertices[i+k];
            }
            for(int i:model.indices)indices[indexCount++]=offset+i;System.arraycopy(model.uv,0,uv,texels,model.uv.length);texels+=model.uv.length;
        }
        void cascade(Hex water,Set<Hex> excluded,SceneMesh model,float reach){
            if(excluded.contains(water)||g.bases.contains(water))return;
            float bx=g.grid.x(water),bz=g.grid.z(water),tx=0,tz=0,top=0,across=1;
            // Find an entirely non-enterable uphill corridor. No river, height
            // override, road, unit or site cell is added or changed.
            for(int attempt=0;attempt<3&&top==0;attempt++){
            float corridor=reach*(attempt==0?1:attempt==1?.75f:.5f);
            for(Hex n:water.neighbors())if(g.valid(n)&&g.terrain[n.r*g.width+n.q]==World.Terrain.MOUNTAIN.ordinal()){
                float dx=g.grid.x(n)-bx,dz=g.grid.z(n)-bz;
                float x=bx+dx*corridor,z=bz+dz*corridor;
                boolean safe=true;
                for(int k=1;k<=8;k++){
                    Hex h=g.grid.cell(bx+dx*corridor*k/8,bz+dz*corridor*k/8);
                    if(!g.valid(h)||excluded.contains(h)||g.bases.contains(h)
                        ||!h.equals(water)&&g.terrain[h.r*g.width+h.q]!=World.Terrain.MOUNTAIN.ordinal()){safe=false;break;}
                }
                if(!safe)continue;float width=1;
                while(width>=.69f&&!cascadeFits(model,water,excluded,x,z,bx,bz,width))width-=.15f;
                if(width<.69f)continue;float y=g.surface.meshHeight(x,z);
                if(y>=.22f&&y>top){top=y;tx=x;tz=z;across=width;}
            }
            }
            if(top<.22f)return;
            float dx=bx-tx,dz=bz-tz,len=(float)Math.hypot(dx,dz),sx=-dz/len,sz=dx/len;
            // The authored rock shelf supports a raised headwater and steep drop.
            // Do not flatten the normalized fall against a gradual mountain slope.
            float base=g.surface.meshHeight(bx,bz)+.03f;
            float bedHigh=top;
            for(int i=0;i<model.vertices.length;i+=7){
                float x=tx+dx*model.vertices[i+2]+sx*model.vertices[i]*across;
                float z=tz+dz*model.vertices[i+2]+sz*model.vertices[i]*across;
                bedHigh=Math.max(bedHigh,g.surface.meshHeight(x,z));
            }
            float rise=Math.max(.78f,bedHigh-base+.45f);
            int offset=floats/7;reserve(model.vertices.length,model.uv.length,model.indices.length);
            for(int i=0;i<model.vertices.length;i+=7){
                float x=tx+dx*model.vertices[i+2]+sx*model.vertices[i]*across;
                float z=tz+dz*model.vertices[i+2]+sz*model.vertices[i]*across;
                float y=Math.max(g.surface.meshHeight(x,z)+.03f,base+rise*model.vertices[i+1]);
                v[floats++]=x;v[floats++]=y;v[floats++]=z;
                for(int k=3;k<7;k++)v[floats++]=model.vertices[i+k];
            }
            // Vertex grounding alone can leave a ribbon triangle crossing a
            // terrain fan ridge between its corners. Lift shared vertices by
            // each face's sampled deficit before emitting the existing mesh.
            float[] lift=new float[model.vertices.length/7];
            for(int i=0;i<model.indices.length;i+=3){
                int a=offset+model.indices[i],b=offset+model.indices[i+1],c=offset+model.indices[i+2];float needed=0;
                for(int p=0;p<=6;p++)for(int q=0;q<=6-p;q++){
                    float u=p/6f,t=q/6f,w=1-u-t;
                    float x=v[a*7]*u+v[b*7]*t+v[c*7]*w,z=v[a*7+2]*u+v[b*7+2]*t+v[c*7+2]*w;
                    float y=v[a*7+1]*u+v[b*7+1]*t+v[c*7+1]*w;
                    needed=Math.max(needed,g.surface.meshHeight(x,z)+.03f-y);
                }
                for(int k=0;k<3;k++){int vertex=model.indices[i+k];lift[vertex]=Math.max(lift[vertex],needed);}
            }
            // Strict GLB baking expands each triangle corner. Equal source
            // positions therefore have different indices: weld the *correction*
            // across those corners before applying it, or neighbouring faces
            // lift by different amounts and tear apart above a terrain ridge.
            record Corner(int x,int y,int z) {}
            Map<Corner,Float> sharedLift=new HashMap<>();
            for(int i=0;i<lift.length;i++){
                int k=(offset+i)*7;Corner key=new Corner(Float.floatToIntBits(v[k]),Float.floatToIntBits(v[k+1]),Float.floatToIntBits(v[k+2]));
                sharedLift.merge(key,lift[i],Math::max);
            }
            for(int i=0;i<lift.length;i++){
                int k=(offset+i)*7;Corner key=new Corner(Float.floatToIntBits(v[k]),Float.floatToIntBits(v[k+1]),Float.floatToIntBits(v[k+2]));
                v[k+1]+=sharedLift.get(key);
            }
            for(int i:model.indices)indices[indexCount++]=offset+i;
            System.arraycopy(model.uv,0,uv,texels,model.uv.length);texels+=model.uv.length;
        }
        boolean cascadeFits(SceneMesh model,Hex water,Set<Hex> excluded,float tx,float tz,float bx,float bz,float across){
            float dx=bx-tx,dz=bz-tz,len=(float)Math.hypot(dx,dz),sx=-dz/len,sz=dx/len;
            // The full ribbon, not just its centerline, must fit the blocked corridor.
            for(int i=0;i<model.indices.length;i+=3)for(int a=0;a<=6;a++)for(int b=0;b<=6-a;b++){
                float u=a/6f,t=b/6f,w=1-u-t;int A=model.indices[i]*7,B=model.indices[i+1]*7,C=model.indices[i+2]*7;
                float vx=model.vertices[A]*u+model.vertices[B]*t+model.vertices[C]*w;
                float vz=model.vertices[A+2]*u+model.vertices[B+2]*t+model.vertices[C+2]*w;
                Hex h=g.grid.cell(tx+dx*vz+sx*vx*across,tz+dz*vz+sz*vx*across);
                if(!g.valid(h)||excluded.contains(h)||g.bases.contains(h)
                    ||!h.equals(water)&&g.terrain[h.r*g.width+h.q]!=World.Terrain.MOUNTAIN.ordinal())return false;
            }
            return true;
        }
        boolean connection(Hex h,Hex n){
            return path(g,n)&&!(g.terrain[h.r*g.width+h.q]==World.Terrain.ROAD.ordinal()
                &&g.terrain[n.r*g.width+n.q]==World.Terrain.ROAD.ordinal());
        }
        void pathVertex(float x,float z,boolean timber,float u,float t){
            vertex(x,g.surface.meshHeight(x,z)+LandscapeProfile.SURFACE_LIFT,z,timber?2:7,u,t);
            // Temper orange timber/soil to the shared earth palette, without another light source.
            int o=floats-7;v[o+3]=.76f;v[o+4]=.82f;v[o+5]=.74f;
        }
        void road(Hex h,Hex n){
            if(!connection(h,n))return;
            float x=g.grid.x(h),z=g.grid.z(h),dx=g.grid.x(n)-x,dz=g.grid.z(n)-z;
            float len=(float)Math.hypot(dx,dz);
            float widthH=LandscapeProfile.pathWidth(timber(g,h)),widthN=LandscapeProfile.pathWidth(timber(g,n));
            float trim=Math.min(.30f,LandscapeProfile.JUNCTION_TRIM/len);
            // Trim each edge at its node. The single node patch below fills every socket;
            // edge ribbons no longer overlap and z-fight at Y/T/cross intersections.
            for(int k=0;k<12;k++){
                float t=trim+(1-2*trim)*k/12f,T=trim+(1-2*trim)*(k+1)/12f;int o=floats/7;
                // The middle seam is shared by both halves. Width interpolates between
                // the node sockets; material changes exactly at the cell boundary.
                float w=widthH+(widthN-widthH)*(t-trim)/(1-2*trim),W=widthH+(widthN-widthH)*(T-trim)/(1-2*trim);
                float nx=-dz/len*w,nz=dx/len*w,NX=-dz/len*W,NZ=dx/len*W;
                boolean timber=timber(g,k<6?h:n);
                float[][] points={{x+dx*t-nx,z+dz*t-nz},{x+dx*T-NX,z+dz*T-NZ},{x+dx*T+NX,z+dz*T+NZ},{x+dx*t+nx,z+dz*t+nz}};
                for(int j=0;j<4;j++)pathVertex(points[j][0],points[j][1],timber,j<2?0:1,j==0||j==3?t:T);
                quad(o);
            }
        }
        void junction(Hex h){
            List<float[]> boundary=new ArrayList<>();float x=g.grid.x(h),z=g.grid.z(h);boolean timber=timber(g,h);
            List<float[]> sockets=new ArrayList<>();
            for(Hex n:h.neighbors())if(g.valid(n)&&connection(h,n)){
                float dx=g.grid.x(n)-x,dz=g.grid.z(n)-z,len=(float)Math.hypot(dx,dz);
                float w=LandscapeProfile.pathWidth(timber);
                float trim=Math.min(len*.30f,LandscapeProfile.JUNCTION_TRIM),ux=dx/len,uz=dz/len;
                sockets.add(new float[]{ux,uz,trim,w});
                boundary.add(new float[]{x+ux*trim-uz*w,z+uz*trim+ux*w});
                boundary.add(new float[]{x+ux*trim+uz*w,z+uz*trim-ux*w});
            }
            if(sockets.isEmpty())return;
            // Rounded exposed ends/gaps, omitting arc samples inside a socket's mouth.
            float radius=LandscapeProfile.pathWidth(timber);
            for(int i=0;i<24;i++){
                double angle=i*Math.PI/12;float dx=radius*(float)Math.cos(angle),dz=radius*(float)Math.sin(angle);boolean mouth=false;
                for(float[] a:sockets)if(dx*a[0]+dz*a[1]>0&&Math.abs(-dx*a[1]+dz*a[0])<a[3]){mouth=true;break;}
                if(!mouth)boundary.add(new float[]{x+dx,z+dz});
            }
            boundary.sort(Comparator.comparingDouble(a->Math.atan2(a[1]-z,a[0]-x)));
            // A star-shaped patch, with radial subdivision sampled from canonical terrain.
            // Adjacent branch meshes share the same socket endpoints at all LODs.
            for(int i=0;i<boundary.size();i++){
                float[] a=boundary.get(i),b=boundary.get((i+1)%boundary.size());
                for(int ring=0;ring<3;ring++){
                    float t=ring/3f,T=(ring+1)/3f;int o=floats/7;
                    pathVertex(x+(a[0]-x)*t,z+(a[1]-z)*t,timber,.5f,.5f);
                    pathVertex(x+(a[0]-x)*T,z+(a[1]-z)*T,timber,0,1);
                    pathVertex(x+(b[0]-x)*T,z+(b[1]-z)*T,timber,1,1);
                    if(ring==0)triangle(o,o+2,o+1);
                    else {pathVertex(x+(b[0]-x)*t,z+(b[1]-z)*t,timber,.5f,0);quad(o);}
                }
            }
        }
        SceneMesh mesh(float x,float z,float radius){SceneMesh m=new SceneMesh(Arrays.copyOf(v,floats),Arrays.copyOf(indices,indexCount),x,z,radius);m.vegetation=true;m.uv=Arrays.copyOf(uv,texels);m.generateTangents();return m;}
    }
    static boolean eligible(MapSceneSnapshot.Ground g,Set<Hex> excluded,Hex h){
        if(!g.valid(h)||excluded.contains(h))return false;
        int own=g.terrain[h.r*g.width+h.q];
        if(own!=World.Terrain.FOREST.ordinal()&&own!=World.Terrain.PLAIN.ordinal())return false;
        if(density(g,g.grid.x(h),g.grid.z(h))<.08f)return false;
        for(Hex n:h.neighbors()){
            if(!g.valid(n)||excluded.contains(n))return false;
            int t=g.terrain[n.r*g.width+n.q];
            if(t==World.Terrain.ROAD.ordinal()||t==World.Terrain.PLANK_ROAD.ordinal()||t==World.Terrain.MOUNTAIN_PATH.ordinal()||g.surface.water(n))return false;
        }
        return true;
    }
    private static int mix(int h){h^=h>>>16;h*=0x7feb352d;h^=h>>>15;h*=0x846ca68b;return h^(h>>>16);}
    static float density(MapSceneSnapshot.Ground g,float x,float z){
        Hex center=g.grid.cell(x,z);float forest=0,total=0;
        for(int r=center.r-2;r<=center.r+2;r++)for(int q=center.q-3;q<=center.q+3;q++){
            if(q<0||r<0||q>=g.width||r>=g.height)continue;
            float dx=x-g.grid.x(q,r),dz=z-g.grid.z(q,r),d=(dx*dx+dz*dz)/2.56f;
            if(d>=1)continue;float w=(1-d)*(1-d);total+=w;
            if(g.terrain[r*g.width+q]==World.Terrain.FOREST.ordinal())forest+=w;
        }
        return total==0?0:forest/total;
    }
    private static SceneMesh merge(MapSceneSnapshot.Ground ground,List<Hex> cells,SceneMesh broadleaf,SceneMesh upland){
        List<Float> vertices=new ArrayList<>();List<Integer> indices=new ArrayList<>();List<Float> tex=new ArrayList<>();
        float minX=Float.MAX_VALUE,minZ=minX,maxX=-minX,maxZ=-minX;
        for(Hex h:cells){
            // Global coordinate hash; stable phase across chunks, LODs and camera changes.
            for(int candidate=0;candidate<2;candidate++){
            int hash=(Float.floatToIntBits(ground.grid.x(h))*73856093)^(Float.floatToIntBits(ground.grid.z(h))*19349663)^SEED^ground.mapSeed^(candidate*83492791);
            hash=mix(hash);
            float x=ground.grid.x(h)+(((hash>>>2)&1023)/1023f-.5f)*.82f;
            float z=ground.grid.z(h)+(((hash>>>12)&1023)/1023f-.5f)*.82f;
            if((mix(hash^0x55ab)&65535)/65535f>density(ground,x,z)*.70f)continue;
            float scale=.65f+((hash>>>4)&255)/255f*.30f,y=ground.surface.meshHeight(x,z);
            float angle=(hash&65535)/65535f*6.283185f,cs=(float)Math.cos(angle),sn=(float)Math.sin(angle);
            float region=.5f+.35f*(float)(Math.sin(x*.13)*Math.cos(z*.11));
            SceneMesh tree=((hash>>>22)&255)/255f<region?broadleaf:upland;
            int offset=vertices.size()/7;
            for(int v=0;v<tree.vertices.length;v+=7){vertices.add((tree.vertices[v]*cs+tree.vertices[v+2]*sn)*scale+x);vertices.add(tree.vertices[v+1]*scale+y);vertices.add((-tree.vertices[v]*sn+tree.vertices[v+2]*cs)*scale+z);for(int k=3;k<7;k++)vertices.add(tree.vertices[v+k]);}
            for(int i:tree.indices)indices.add(offset+i);for(float uv:tree.uv)tex.add(uv);
            minX=Math.min(minX,x);minZ=Math.min(minZ,z);maxX=Math.max(maxX,x);maxZ=Math.max(maxZ,z);
            }
        }
        if(vertices.isEmpty()){minX=ground.grid.x(cells.get(0));maxX=minX;minZ=ground.grid.z(cells.get(0));maxZ=minZ;}
        SceneMesh mesh=new SceneMesh(vertices,indices,(minX+maxX)/2,(minZ+maxZ)/2,Math.max(maxX-minX,maxZ-minZ)/2+1);
        mesh.vegetation=true;mesh.uv=new float[tex.size()];for(int i=0;i<tex.size();i++)mesh.uv[i]=tex.get(i);mesh.generateTangents();return mesh;
    }
}
