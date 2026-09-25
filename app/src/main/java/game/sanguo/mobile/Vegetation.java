package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** Opaque merged vegetation: one renderable per visible 8x8 chunk, never one per tree. */
final class Vegetation {
    static final int CHUNK=8,SEED=0x3111203;
    static Set<Hex> exclusions(MapSceneSnapshot snapshot){
        Set<Hex> result=new HashSet<>(snapshot.ground.bases);
        for(MapSceneSnapshot.Item item:snapshot.items)if(item.facility!=null||item.unit!=null)result.add(item.hex);
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
        SceneMesh[][] models=new SceneMesh[4][2];
        String[] names={"tree","tree-upland","shrub","rock-strata"};
        for(int i=0;i<names.length;i++)for(int lod=window.span<14?0:1;lod<2;lod++)models[i][lod]=assets.mesh(names[i]+"-lod"+lod);
        Map<Long,SceneMesh> cache=new HashMap<>();for(SceneMesh m:previous)cache.put(key(m.chunkQ,m.chunkR),m);
        List<SceneMesh> result=new ArrayList<>();
        for(int r=0;r<g.height;r+=chunk)for(int q=0;q<g.width;q+=chunk){
            if(Thread.currentThread().isInterrupted())return Collections.emptyList();
            float cx=g.grid.x(q+chunk/2,r+chunk/2),cz=g.grid.z(q+chunk/2,r+chunk/2);
            if(!window.contains(cx,cz,radius))continue;
            boolean detailed=window.span<14&&Math.hypot(cx-window.x,cz-window.z)<20;
            long hash=SEED^LandscapeProfile.VERSION^g.mapIdentity^g.width*31L^g.height^(detailed?0x808:0)^((long)chunk<<48);
            hash=(hash^Float.floatToIntBits(g.grid.offset))*1099511628211L;hash^=g.grid.staggered?1:0;
            for(int rr=r-8;rr<r+chunk+8;rr++)for(int qq=q-8;qq<q+chunk+8;qq++){
                Hex h=new Hex(qq,rr);int t=g.valid(h)?g.terrain[rr*g.width+qq]:-1;
                hash=(hash^(t+1+(excluded.contains(h)?64:0)+(g.bases.contains(h)?128:0)))*1099511628211L;
                hash=(hash^Float.floatToIntBits(g.surface.overrides.getOrDefault(h,-1f)))*1099511628211L;
            }
            SceneMesh prior=cache.get(key(q,r));if(prior!=null&&prior.fingerprint==hash){result.add(prior);continue;}
            Batch near=new Batch(g),far=new Batch(g);
            for(int rr=r;rr<Math.min(r+chunk,g.height);rr++)for(int qq=q;qq<Math.min(q+chunk,g.width);qq++){
                Hex h=new Hex(qq,rr);if(!g.valid(h))continue;
                // Only existing adjacent logical road cells connect. Each undirected edge
                // belongs to its lower cell ID, including edges that cross a chunk boundary.
                if(path(g,h))for(Hex n:h.neighbors())if(g.valid(n)&&path(g,n)&&key(h.q,h.r)<key(n.q,n.r)){
                    if(detailed)near.road(h,n);far.road(h,n);
                }
                if(path(g,h)){if(detailed)near.junction(h);far.junction(h);}
                for(Placement a:placements(g,excluded,h)){
                    if(detailed)near.append(models[a.family][0],a);far.append(models[a.family][1],a);
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
    static boolean path(MapSceneSnapshot.Ground g,Hex h){
        if(!g.valid(h))return false;int t=g.terrain[h.r*g.width+h.q];
        return t==World.Terrain.ROAD.ordinal()||t==World.Terrain.PLANK_ROAD.ordinal()||t==World.Terrain.MOUNTAIN_PATH.ordinal();
    }
    static final class Placement {
        final float x,z,scale,angle;final int family;
        Placement(float x,float z,float scale,float angle,int family){this.x=x;this.z=z;this.scale=scale;this.angle=angle;this.family=family;}
    }
    /** Opaque silhouettes keep alpha-test/mip overdraw out of the foliage path. */
    static List<Placement> placements(MapSceneSnapshot.Ground g,Set<Hex> excluded,Hex h){
        List<Placement> out=new ArrayList<>();if(!g.valid(h))return out;
        int type=g.terrain[h.r*g.width+h.q];boolean rock=type==World.Terrain.MOUNTAIN.ordinal();
        if(!rock&&type!=World.Terrain.FOREST.ordinal()&&type!=World.Terrain.PLAIN.ordinal())return out;
        float hx=g.grid.x(h),hz=g.grid.z(h);
        for(int i=0;i<(rock?1:3);i++){
            int hash=mix(Float.floatToIntBits(hx)*73856093^Float.floatToIntBits(hz)*19349663^SEED^g.mapIdentity^i*83492791);
            float x=hx+(((hash>>>2)&1023)/1023f-.5f)*.70f,z=hz+(((hash>>>12)&1023)/1023f-.5f)*.70f;
            // Candidate must belong to this cell: no duplicate ownership at block/hex seams.
            if(!h.equals(g.grid.cell(x,z))||!clear(g,excluded,h,x,z))continue;
            float cluster=.58f+.24f*(float)(Math.sin(x*.71+1.7)*Math.cos(z*.57-2));
            float chance=rock?.24f:density(g,x,z)*cluster;
            if((mix(hash^0x55ab)&65535)/65535f>chance)continue;
            float regional=.5f+.35f*(float)(Math.sin(x*.055)*Math.cos(z*.047));
            int family=rock?3:i==2?2:((hash>>>22)&255)/255f<regional?0:1;
            out.add(new Placement(x,z,LandscapeProfile.sceneryScale(family,.65f+((hash>>>4)&255)/255f*.30f),(hash&65535)/65535f*6.283185f,family));
        }
        return out;
    }
    static boolean clear(MapSceneSnapshot.Ground g,Set<Hex> excluded,Hex h,float x,float z){
        // One-cell safety apron includes the full opaque crown, port approaches and unit anchors.
        for(int r=h.r-2;r<=h.r+2;r++)for(int q=h.q-2;q<=h.q+2;q++){
            Hex n=new Hex(q,r);float dx=g.grid.x(n)-x,dz=g.grid.z(n)-z;
            if(dx*dx+dz*dz>2.1f)continue;
            if(!g.valid(n)||excluded.contains(n)||g.bases.contains(n)||path(g,n)||g.surface.water(n))return false;
        }
        return true;
    }
    private static final class Batch {
        final MapSceneSnapshot.Ground g;final List<Float> v=new ArrayList<>(),uv=new ArrayList<>();final List<Integer> indices=new ArrayList<>();
        Batch(MapSceneSnapshot.Ground g){this.g=g;}
        void vertex(float x,float y,float z,int mat,float u,float t){Collections.addAll(v,x,y,z,1f,1f,1f,1f);Collections.addAll(uv,(mat+.03f+.94f*u)/8,.03f+.94f*t);}
        void append(SceneMesh model,Placement a){
            int offset=v.size()/7;float cs=(float)Math.cos(a.angle),sn=(float)Math.sin(a.angle);
            float base=g.surface.meshHeight(a.x,a.z);
            for(int i=0;i<model.vertices.length;i+=7){
                float x=(model.vertices[i]*cs+model.vertices[i+2]*sn)*a.scale+a.x;
                float z=(-model.vertices[i]*sn+model.vertices[i+2]*cs)*a.scale+a.z;
                // Rock foot conforms per vertex, embedding the bottom below the canonical surface.
                float y=a.family==3?g.surface.meshHeight(x,z)-.035f:base-.015f;
                Collections.addAll(v,x,model.vertices[i+1]*a.scale+y,z);
                for(int k=3;k<7;k++)v.add(model.vertices[i+k]);
            }
            for(int i:model.indices)indices.add(offset+i);for(float f:model.uv)uv.add(f);
        }
        boolean connection(Hex h,Hex n){
            return path(g,n)&&!(g.terrain[h.r*g.width+h.q]==World.Terrain.ROAD.ordinal()
                &&g.terrain[n.r*g.width+n.q]==World.Terrain.ROAD.ordinal());
        }
        boolean plank(Hex h,Hex n){return g.terrain[h.r*g.width+h.q]==World.Terrain.PLANK_ROAD.ordinal()||g.terrain[n.r*g.width+n.q]==World.Terrain.PLANK_ROAD.ordinal();}
        void pathVertex(float x,float z,boolean timber,float u,float t){
            vertex(x,g.surface.meshHeight(x,z)+LandscapeProfile.SURFACE_LIFT,z,timber?2:7,u,t);
            // Temper orange timber/soil to the shared earth palette, without another light source.
            int o=v.size()-7;v.set(o+3,.76f);v.set(o+4,.82f);v.set(o+5,.74f);
        }
        void road(Hex h,Hex n){
            if(!connection(h,n))return;
            float x=g.grid.x(h),z=g.grid.z(h),dx=g.grid.x(n)-x,dz=g.grid.z(n)-z;
            float len=(float)Math.hypot(dx,dz);boolean timber=plank(h,n);
            float width=LandscapeProfile.pathWidth(timber),nx=-dz/len*width,nz=dx/len*width;
            float trim=Math.min(.30f,LandscapeProfile.JUNCTION_TRIM/len);
            // Trim each edge at its node. The single node patch below fills every socket;
            // edge ribbons no longer overlap and z-fight at Y/T/cross intersections.
            for(int k=0;k<12;k++){
                float t=trim+(1-2*trim)*k/12f,T=trim+(1-2*trim)*(k+1)/12f;int o=v.size()/7;
                float[][] points={{x+dx*t-nx,z+dz*t-nz},{x+dx*T-nx,z+dz*T-nz},{x+dx*T+nx,z+dz*T+nz},{x+dx*t+nx,z+dz*t+nz}};
                for(int j=0;j<4;j++)pathVertex(points[j][0],points[j][1],timber,j<2?0:1,j==0||j==3?t:T);
                Collections.addAll(indices,o,o+2,o+1,o,o+3,o+2);
            }
        }
        void junction(Hex h){
            List<float[]> boundary=new ArrayList<>();float x=g.grid.x(h),z=g.grid.z(h);boolean timber=false;
            List<float[]> sockets=new ArrayList<>();
            for(Hex n:h.neighbors())if(g.valid(n)&&connection(h,n)){
                float dx=g.grid.x(n)-x,dz=g.grid.z(n)-z,len=(float)Math.hypot(dx,dz);
                boolean wood=plank(h,n);timber|=wood;float w=LandscapeProfile.pathWidth(wood);
                float trim=Math.min(len*.30f,LandscapeProfile.JUNCTION_TRIM),ux=dx/len,uz=dz/len;
                sockets.add(new float[]{ux,uz,trim,w});
                boundary.add(new float[]{x+ux*trim-uz*w,z+uz*trim+ux*w});
                boundary.add(new float[]{x+ux*trim+uz*w,z+uz*trim-ux*w});
            }
            if(sockets.isEmpty())return;
            // Rounded exposed ends/gaps, omitting arc samples inside a socket's mouth.
            float radius=LandscapeProfile.PATH_HALF_WIDTH;
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
                    float t=ring/3f,T=(ring+1)/3f;int o=v.size()/7;
                    pathVertex(x+(a[0]-x)*t,z+(a[1]-z)*t,timber,.5f,.5f);
                    pathVertex(x+(a[0]-x)*T,z+(a[1]-z)*T,timber,0,1);
                    pathVertex(x+(b[0]-x)*T,z+(b[1]-z)*T,timber,1,1);
                    if(ring==0)Collections.addAll(indices,o,o+2,o+1);
                    else {pathVertex(x+(b[0]-x)*t,z+(b[1]-z)*t,timber,.5f,0);Collections.addAll(indices,o,o+2,o+1,o,o+3,o+2);}
                }
            }
        }
        SceneMesh mesh(float x,float z,float radius){SceneMesh m=new SceneMesh(v,indices,x,z,radius);m.vegetation=true;m.uv=new float[uv.size()];for(int i=0;i<uv.size();i++)m.uv[i]=uv.get(i);m.generateTangents();return m;}
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
