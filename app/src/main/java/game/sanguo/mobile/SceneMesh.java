package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** CPU-only mesh preparation; no Android or renderer references. */
final class SceneMesh {
    private static final World.Terrain[] TERRAIN_TYPES=World.Terrain.values();
    // Source quarter-cell topology repeats across every full16x16 chunk.
    // Share only byte-identical immutable index arrays, bounded to16 patterns.
    private static final List<int[]> PC_INDEX_PATTERNS=new ArrayList<>();
    private static synchronized int[] pcIndices(int[] land,int landCount,int[] water,int waterCount){
        // Compare bounded task scratch before allocating a repeated immutable
        // topology. Never publish scratch; a miss owns an exact used-range copy.
        int count=landCount+waterCount;
        for(int[] pattern:PC_INDEX_PATTERNS){
            if(pattern.length!=count)continue;
            int n=0;while(n<landCount&&pattern[n]==land[n])n++;
            if(n!=landCount)continue;
            int w=0;while(w<waterCount&&pattern[landCount+w]==water[w])w++;
            if(w==waterCount)return pattern;
        }
        int[] indices=Arrays.copyOf(land,count);
        if(waterCount!=0)System.arraycopy(water,0,indices,landCount,waterCount);
        if(PC_INDEX_PATTERNS.size()<16)PC_INDEX_PATTERNS.add(indices);
        return indices;
    }
    SceneMesh distant,grid,sourceWater;
    boolean vegetation,pcGround,pcWater,pcScenery,pcSite,pcFacility,pcCliffWall,pcWall,pcDam,pcUnit,pcFacilityRig;
    int pcUnitModel=-1,pcUnitOpaqueIndices;
    boolean gridDifficultMarch;
    boolean gridMatches(MapSceneSnapshot.Ground ground){return grid!=null&&gridDifficultMarch==ground.gridDifficultMarch;}
    int landscapeChunkSize=8;
    // Disjoint index ranges share the same surface and buffers, but never draw water as land.
    int landIndexCount=-1;
    long fingerprint; int chunkQ,chunkR,terrainLod;
    // The existing tangent quaternion already encodes authored normals. Rigid
    // animation can rotate that frame without retaining a second normal stream.
    boolean authoredTangentFrame;
    float[] tangents; float[] surfaceData; float[] uv; final float[] vertices; final int[] indices; final float x,z,radius,minY,maxY;
    SceneMesh(List<Float> v,List<Integer> i,float x,float z,float radius){
        vertices=new float[v.size()];for(int n=0;n<v.size();n++)vertices[n]=v.get(n);
        indices=new int[i.size()];for(int n=0;n<i.size();n++)indices[n]=i.get(n);
        float[] heights=heightBounds(vertices);minY=heights[0];maxY=heights[1];
        this.x=x;this.z=z;this.radius=radius;
    }
    SceneMesh(float[] vertices,int[] indices,float x,float z,float radius){
        this.vertices=vertices;this.indices=indices;this.x=x;this.z=z;this.radius=radius;
        float[] heights=heightBounds(vertices);minY=heights[0];maxY=heights[1];
    }
    /** Unique immutable array payload reachable from these mesh roots. Excludes
     * object overhead, worker scratch/mailbox, upload buffers and GPU storage. */
    static long payloadBytes(Collection<SceneMesh> roots){
        Set<SceneMesh> meshes=Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> arrays=Collections.newSetFromMap(new IdentityHashMap<>());
        long bytes=0;for(SceneMesh mesh:roots)bytes+=payloadBytes(mesh,meshes,arrays);return bytes;
    }
    private static long payloadBytes(SceneMesh mesh,Set<SceneMesh> meshes,Set<Object> arrays){
        if(mesh==null||!meshes.add(mesh))return 0;
        long bytes=arrays.add(mesh.vertices)?4L*mesh.vertices.length:0;
        if(arrays.add(mesh.indices))bytes+=4L*mesh.indices.length;
        for(float[] stream:new float[][]{mesh.surfaceData,mesh.tangents,mesh.uv})if(stream!=null&&arrays.add(stream))bytes+=4L*stream.length;
        return bytes+payloadBytes(mesh.grid,meshes,arrays)+payloadBytes(mesh.sourceWater,meshes,arrays)+payloadBytes(mesh.distant,meshes,arrays);
    }
    private static float[] heightBounds(float[] vertices){
        float low=Float.POSITIVE_INFINITY,high=Float.NEGATIVE_INFINITY;
        for(int i=1;i<vertices.length;i+=7){low=Math.min(low,vertices[i]);high=Math.max(high,vertices[i]);}
        return vertices.length==0?new float[]{0,0}:new float[]{low,high};
    }
    /** Area-weighted normals respect split face vertices; generated on cache misses only.
     * No normal-map sampling on object materials, so a normal-aligned frame is sufficient. */
    void generateTangents(){
        int count=vertices.length/7;float[] normals=new float[count*3];
        for(int t=0;t<indices.length;t+=3){
            int a=indices[t]*7,b=indices[t+1]*7,c=indices[t+2]*7;
            float ax=vertices[b]-vertices[a],ay=vertices[b+1]-vertices[a+1],az=vertices[b+2]-vertices[a+2];
            float bx=vertices[c]-vertices[a],by=vertices[c+1]-vertices[a+1],bz=vertices[c+2]-vertices[a+2];
            float nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx;
            for(int k=0;k<3;k++){int v=indices[t+k]*3;normals[v]+=nx;normals[v+1]+=ny;normals[v+2]+=nz;}
        }
        setNormals(normals);
    }
    void setNormals(float[] normals){
        int count=vertices.length/7;
        if(normals.length!=count*3)throw new IllegalArgumentException("normal count");
        tangents=new float[count*4];
        for(int i=0;i<count;i++){
            float x=normals[i*3],y=normals[i*3+1],z=normals[i*3+2];
            float len=(float)Math.sqrt(x*x+y*y+z*z);
            if(!Float.isFinite(len))throw new IllegalArgumentException("nonfinite normal");
            if(len<1e-8f){x=0;y=1;z=0;}else{x/=len;y/=len;z/=len;}
            // Stable shortest arc from +Z, including exactly backward-facing walls.
            float w=(float)Math.sqrt(Math.max(0,(1+z)*.5f));
            if(w<.0001f){tangents[i*4]=1; tangents[i*4+3]=.00001f;}
            else{tangents[i*4]=-y*.5f/w;tangents[i*4+1]=x*.5f/w;tangents[i*4+3]=w;}
            float qlen=0;for(int k=0;k<4;k++)qlen+=tangents[i*4+k]*tangents[i*4+k];
            qlen=(float)Math.sqrt(qlen);for(int k=0;k<4;k++)tangents[i*4+k]/=qlen;
        }
    }
    static final class Builder {
        final List<Float> v=new ArrayList<>();final List<Integer> i=new ArrayList<>();
        void vertex(float x,float y,float z,int color){Collections.addAll(v,x,y,z,((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1f);}
        void quad(float x,float y,float z,float dx,float dz,int color){int n=v.size()/7;vertex(x,y,z,color);vertex(x+dx,y,z,color);vertex(x+dx,y,z+dz,color);vertex(x,y,z+dz,color);Collections.addAll(i,n,n+1,n+2,n,n+2,n+3);}
        void face(float[] p,int color){int n=v.size()/7;for(int j=0;j<12;j+=3)vertex(p[j],p[j+1],p[j+2],color);Collections.addAll(i,n,n+1,n+2,n,n+2,n+3);}
        void box(float x,float z,float dx,float dz,float h,int color){
            quad(x,h,z,dx,dz,color);
            int shade=shade(color,.73f);
            face(new float[]{x,0,z,x+dx,0,z,x+dx,h,z,x,h,z},shade);
            face(new float[]{x,0,z+dz,x+dx,0,z+dz,x+dx,h,z+dz,x,h,z+dz},shade);
            shade=shade(color,.85f);
            face(new float[]{x,0,z,x,0,z+dz,x,h,z+dz,x,h,z},shade);
            face(new float[]{x+dx,0,z,x+dx,0,z+dz,x+dx,h,z+dz,x+dx,h,z},shade);
        }
        SceneMesh mesh(float x,float z,float radius){return new SceneMesh(v,i,x,z,radius);}
    }
    /** Fixed-capacity primitive scratch for one surface chunk (or the bounded backdrop).
     * The fan, winding, vertex order, color arithmetic and land/water partition are unchanged.
     * Avoid retaining millions of Float/Integer objects during a national first load. */
    private static final class SurfaceBuilder {
        final float[] vertices;
        final int[] land;
        int[] water;
        final float[] clippedPolygon=new float[12];
        // Exact PC positions share one vertex within a chunk. Fixed scratch is
        // reset with the task's builder; no boxed map or cross-window cache.
        long[] pcKeys;int[] pcSlots;
        // Grid ribbons are unlit position/pigment streams. Adjacent segments
        // share exactly equal endpoints; preserve raw float bits and colors.
        int[] ribbonSlots;
        int vertexCount,landCount,waterCount;
        SurfaceBuilder(int maxVertices,int maxIndices){vertices=new float[maxVertices*7];land=new int[maxIndices];}
        // mesh() owns copies of all used elements. One worker may therefore
        // reuse this bounded scratch without sharing published mesh arrays.
        void reset(){vertexCount=landCount=waterCount=0;if(pcSlots!=null)Arrays.fill(pcSlots,0);if(ribbonSlots!=null)Arrays.fill(ribbonSlots,0);}
        void shareRibbonVertices(){int capacity=1;while(capacity<vertices.length/7*2)capacity<<=1;ribbonSlots=new int[capacity];}
        int ribbonVertex(float x,float y,float z,int color){
            int xb=Float.floatToRawIntBits(x),yb=Float.floatToRawIntBits(y),zb=Float.floatToRawIntBits(z);
            int hash=((xb*31+yb)*31+zb)*31+color;
            int slot=(hash^(hash>>>16))&(ribbonSlots.length-1);
            float red=((color>>16)&255)/255f,green=((color>>8)&255)/255f,blue=(color&255)/255f;
            while(ribbonSlots[slot]!=0){
                int at=ribbonSlots[slot]-1,n=at*7;
                if(Float.floatToRawIntBits(vertices[n])==xb&&Float.floatToRawIntBits(vertices[n+1])==yb
                    &&Float.floatToRawIntBits(vertices[n+2])==zb&&vertices[n+3]==red&&vertices[n+4]==green&&vertices[n+5]==blue)return at;
                slot=(slot+1)&(ribbonSlots.length-1);
            }
            int at=vertexCount;vertex(x,y,z,color);ribbonSlots[slot]=at+1;return at;
        }
        int pcVertex(float x,float y,float z,int color){
            if(pcSlots==null){pcSlots=new int[16384];pcKeys=new long[16384];}
            long key=((long)Float.floatToRawIntBits(x)<<32)|(Float.floatToRawIntBits(z)&0xffffffffL);
            long mixed=(key^(key>>>33))*0xff51afd7ed558ccdL;
            mixed=(mixed^(mixed>>>33))*0xc4ceb9fe1a85ec53L;
            int slot=(int)(mixed^(mixed>>>33))&16383;
            while(pcSlots[slot]!=0){if(pcKeys[slot]==key)return pcSlots[slot]-1;slot=(slot+1)&16383;}
            int at=vertexCount;vertex(x,y,z,color);pcKeys[slot]=key;pcSlots[slot]=at+1;return at;
        }
        void vertex(float x,float y,float z,int color){
            int n=vertexCount++*7;vertices[n]=x;vertices[n+1]=y;vertices[n+2]=z;
            vertices[n+3]=((color>>16)&255)/255f;vertices[n+4]=((color>>8)&255)/255f;
            vertices[n+5]=(color&255)/255f;vertices[n+6]=1f;
        }
        void triangle(boolean wet,int a,int b,int c){
            if(wet){if(water==null)water=new int[land.length];water[waterCount++]=a;water[waterCount++]=b;water[waterCount++]=c;}
            else{land[landCount++]=a;land[landCount++]=b;land[landCount++]=c;}
        }
        void face(float ax,float ay,float az,float bx,float by,float bz,
                float cx,float cy,float cz,float dx,float dy,float dz,int color){
            if(ribbonSlots!=null){
                int a=ribbonVertex(ax,ay,az,color),b=ribbonVertex(bx,by,bz,color),c=ribbonVertex(cx,cy,cz,color),d=ribbonVertex(dx,dy,dz,color);
                triangle(false,a,b,c);triangle(false,a,c,d);return;
            }
            int n=vertexCount;vertex(ax,ay,az,color);vertex(bx,by,bz,color);
            vertex(cx,cy,cz,color);vertex(dx,dy,dz,color);
            triangle(false,n,n+1,n+2);triangle(false,n,n+2,n+3);
        }
        SceneMesh mesh(float x,float z,float radius){
            int[] indices;
            if(pcSlots!=null)indices=pcIndices(land,landCount,water,waterCount);
            else{
                indices=Arrays.copyOf(land,landCount+waterCount);
                if(waterCount!=0)System.arraycopy(water,0,indices,landCount,waterCount);
            }
            SceneMesh mesh=new SceneMesh(Arrays.copyOf(vertices,vertexCount*7),indices,x,z,radius);
            mesh.landIndexCount=landCount;return mesh;
        }
    }
    static int shade(int c,float f){return 0xff000000|((int)(((c>>16)&255)*f)<<16)|((int)(((c>>8)&255)*f)<<8)|(int)((c&255)*f);}
    static int terrain(int ordinal){
        switch(TERRAIN_TYPES[ordinal]){
            case WATER:case SEA:case SHALLOWS:case NON_NAVIGABLE_WATER:return 0xff496d78;
            case FOREST:return 0xff596c49;
            case MOUNTAIN:return 0xff77796d;
            case SAND:return 0xffad9e78;
            case DAM:return 0xff858276;
            case SWAMP:case POISON:return 0xff69755c;
            default:return 0xff889576;
        }
    }
    static final float[][] EDGE={{-.5f,-.5f},{0,-.5f},{.5f,-.5f},{.5f,0},{.5f,.5f},{0,.5f},{-.5f,.5f},{-.5f,0}};
    /** Coarse non-playable scenery beneath the authoritative surface; never a picking input.
     * Fills VOID perforations and the exterior margin without manufacturing playable tiles. */
    static SceneMesh backdrop(MapSceneSnapshot.Ground g){
        if(g.pcMap!=null)return null; // Source scenery is streamed with the same quarter-grid as playable terrain.
        float minX=Float.MAX_VALUE,minZ=minX,maxX=-minX,maxZ=-minX;
        for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++)if(g.valid(new Hex(q,r))){
            float x=g.grid.x(q,r),z=g.grid.z(q,r);minX=Math.min(minX,x);maxX=Math.max(maxX,x);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
        }
        if(minX==Float.MAX_VALUE)return null;
        minX=(float)Math.floor((minX-16)/8)*8;minZ=(float)Math.floor((minZ-16)/8)*8;maxX+=16;maxZ+=16;
        // Sample the shared field densely enough to avoid coarse background colour islands
        // showing through authoritative VOID cells. This never adds playable cells.
        final float step=2.25f;
        int columns=(int)Math.ceil((maxX-minX)/step),rows=(int)Math.ceil((maxZ-minZ)/step);
        SurfaceBuilder b=new SurfaceBuilder((columns+1)*(rows+1),columns*rows*6);
        // The regular background has no split normals: adjacent quads share payloads.
        for(int r=0;r<=rows;r++)for(int q=0;q<=columns;q++)
            b.vertex(minX+q*step,g.pcMap==null?-.04f:g.surface.pcLandHeight(minX+q*step,minZ+r*step)-.015f,minZ+r*step,0xffffffff);
        for(int r=0;r<rows;r++)for(int q=0;q<columns;q++){
            int n=r*(columns+1)+q,next=n+columns+1;
            b.triangle(false,n,next,next+1);b.triangle(false,n,next+1,n+1);
        }
        SceneMesh m=b.mesh((minX+maxX)/2,(minZ+maxZ)/2,Math.max(maxX-minX,maxZ-minZ)/2+8);m.landIndexCount=m.indices.length;m.pcGround=g.pcMap!=null;
        if(g.pcMap!=null)m.sourceWater=sourceWater(g,-1,-1,m);
        // Identical weights, lighting frame, macro tone and shore data to the foreground.
        new TerrainMaterialField(g).attachBackdrop(m);
        return m;
    }
    /** A view request retains only visible chunks plus a prefetch margin. All levels
     * preserve the canonical fan planes/edges, so arbitrary adjacent levels stitch exactly. */
    static final class TerrainWindow {
        final float x,z,ex,ez,span;
        TerrainWindow(float x,float z,float ex,float ez,float span){this.x=x;this.z=z;this.ex=ex+16;this.ez=ez+16;this.span=span;}
        boolean covers(float cx,float cz,float vx,float vz,float nextSpan){
            return Math.abs(cx-x)+vx<=ex-4&&Math.abs(cz-z)+vz<=ez-4&&level(nextSpan)==level(span)&&(nextSpan<48)==(span<48);
        }
        static int level(float span){return span<14?0:span<40?1:2;}
        int lod(float cx,float cz){return Math.min(2,Math.max(level(span),(int)(Math.max(Math.abs(cx-x),Math.abs(cz-z))/32)));}
        boolean contains(float cx,float cz,float radius){return Math.abs(cx-x)<=ex+radius&&Math.abs(cz-z)<=ez+radius;}
    }
    static List<SceneMesh> ground(MapSceneSnapshot.Ground g){return ground(g,Collections.emptyList());}
    static List<SceneMesh> ground(MapSceneSnapshot.Ground g,List<SceneMesh> previous){
        return ground(g,previous,null);
    }
    static final class BuildStats {
        long geometryNanos,heightNanos,blendNanos,shoreNanos;int sharedSamples,timedFieldSamples,builtChunks;Runnable progress;
        java.util.function.Consumer<List<SceneMesh>> firstLoadBatch;
        @Override public String toString(){return "geometryWallMs="+geometryNanos/1e6+" sampledNormalHeightWallMs="+heightNanos/1e6+" sampledBlendWallMs="+blendNanos/1e6+" sampledShoreWallMs="+shoreNanos/1e6+" reusedFieldSamples="+sharedSamples+" timedFieldSamples="+timedFieldSamples;}
    }
    /** A growing replacement window retains old CPU coverage until each key has
     * a new result. loadVisible separately retains its old GPU mesh until upload. */
    static List<SceneMesh> retainWindowCoverage(List<SceneMesh> previous,List<SceneMesh> partial){
        Map<Long,SceneMesh> merged=new LinkedHashMap<>();
        for(SceneMesh m:partial)merged.put(((long)m.chunkR<<32)|(m.chunkQ&0xffffffffL),m);
        for(SceneMesh m:previous)merged.putIfAbsent(((long)m.chunkR<<32)|(m.chunkQ&0xffffffffL),m);
        return Collections.unmodifiableList(new ArrayList<>(merged.values()));
    }
    static List<SceneMesh> windowCoverage(List<SceneMesh> meshes,TerrainWindow window){
        List<SceneMesh> result=new ArrayList<>();
        for(SceneMesh m:meshes)if(window.contains(m.x,m.z,m.radius))result.add(m);
        return Collections.unmodifiableList(result);
    }
    static List<SceneMesh> ground(MapSceneSnapshot.Ground g,List<SceneMesh> previous,TerrainWindow window){
        return ground(g,previous,window,null);
    }
    static List<SceneMesh> ground(MapSceneSnapshot.Ground g,List<SceneMesh> previous,TerrainWindow window,BuildStats stats){
        List<SceneMesh> out=new ArrayList<>();Map<String,SceneMesh> cached=new HashMap<>();
        for(SceneMesh m:previous)cached.put(m.chunkQ+":"+m.chunkR,m);
        List<int[]> requests=new ArrayList<>();
        int margin=g.pcMap==null?0:32,qLimit=g.width+margin,rLimit=g.height+margin;
        for(int r=-margin;r<rLimit;r+=16)for(int q=-margin;q<qLimit;q+=16)requests.add(new int[]{q,r});
        if(window!=null)requests.sort(Comparator.comparingDouble(a->Math.hypot(g.grid.x(a[0]+8,a[1]+8)-window.x,g.grid.z(a[0]+8,a[1]+8)-window.z)));
        SurfaceBuilder scratch=null,gridScratch=null;int[] pcCell=new int[25];
        for(int[] request:requests){
            int q=request[0],r=request[1];float cx=g.grid.x(q+8,r+8),cz=g.grid.z(q+8,r+8);
            if(window!=null&&!window.contains(cx,cz,13))continue;
            int lod=window==null?1:window.lod(cx,cz);
            if(Thread.currentThread().isInterrupted())return Collections.emptyList();
            long fingerprint=1469598103934665603L ^ 0x3D20261003L ^ TerrainMaterialField.VERSION ^ TerrainSurface.METADATA_VERSION ^ WaterVisualField.VERSION;
            fingerprint=(fingerprint^(window==null?g.mapSeed:g.mapIdentity))*1099511628211L;
            fingerprint=(fingerprint^(g.pcMap==null?0:64))*1099511628211L;
            fingerprint=(fingerprint^lod^(window!=null&&window.span<48?0x12100:0))*1099511628211L;
            // Grid-bearing chunks may not retain another force/research state's lines.
            // Overview/no-grid geometry stays reusable across research and force changes.
            if(window!=null&&window.span<48)fingerprint=(fingerprint^(g.gridDifficultMarch?1:0))*1099511628211L;
            fingerprint=(fingerprint^g.width)*1099511628211L;fingerprint=(fingerprint^g.height)*1099511628211L;
            fingerprint=(fingerprint^Float.floatToIntBits(g.grid.offset))*1099511628211L;fingerprint=(fingerprint^(g.grid.staggered?1:0))*1099511628211L;
            for(int rr=r-8;rr<Math.min(r+24,g.height);rr++)for(int qq=q-8;qq<Math.min(q+24,g.width);qq++){
                Hex h=new Hex(qq,rr);int value=g.valid(h)?g.terrain[rr*g.width+qq]+(g.bases.contains(h)?64:0):-1;
                fingerprint=(fingerprint^value)*1099511628211L;
                Float override=g.surface.overrides.get(h);
                fingerprint=(fingerprint^Float.floatToIntBits(override==null?-1f:override))*1099511628211L;
            }
            SceneMesh retained=cached.get(q+":"+r);if(retained!=null&&retained.fingerprint==fingerprint){out.add(retained);continue;}
            long geometryStarted=stats==null?0:System.nanoTime();
            // Maximum capacity for one 16x16 chunk. The previous per-chunk
            // allocation discarded up to 1.5 MiB of scratch on each iteration.
            // Keep one task-local buffer; only immutable used ranges escape.
            if(scratch==null){int cells=g.pcMap==null?Math.min(16,g.width)*Math.min(16,g.height):16*16;scratch=new SurfaceBuilder(cells*(g.pcMap==null?9:25),cells*(g.pcMap==null?24:96));}
            else scratch.reset();
            SurfaceBuilder b=scratch;float minX=Float.MAX_VALUE,minZ=minX,maxX=-minX,maxZ=-minX;
            for(int rr=r;rr<Math.min(r+16,rLimit);rr++)for(int qq=q;qq<Math.min(q+16,qLimit);qq++){
                Hex h=new Hex(qq,rr);if(g.pcMap==null&&!g.valid(h))continue;float x=g.grid.x(h),z=g.grid.z(h);
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
                int color=g.pcMap==null?terrain(g.terrain[rr*g.width+qq]):0xff999999,n=b.vertexCount;boolean water=g.surface.water(h);
                if(g.pcMap!=null){
                    for(int iz=0;iz<=4;iz++)for(int ix=0;ix<=4;ix++){
                        float vx=x-.5f+ix*.25f,vz=z-.5f+iz*.25f;
                        pcCell[iz*5+ix]=b.pcVertex(vx,g.surface.pcLandHeight(vx,vz),vz,0xff999999);
                    }
                    for(int iz=0;iz<4;iz++)for(int ix=0;ix<4;ix++){
                        int a=iz*5+ix;b.triangle(false,pcCell[a],pcCell[a+5],pcCell[a+6]);b.triangle(false,pcCell[a],pcCell[a+6],pcCell[a+1]);
                    }
                    continue;
                }
                b.vertex(x,g.surface.sample(x,z),z,g.surface.color(x,z,color,water));
                for(float[] edge:EDGE){float vx=x+edge[0],vz=z+edge[1];b.vertex(vx,g.surface.sample(vx,vz),vz,g.surface.color(vx,vz,color,water));}
                // +Y facing winding: lit double-sided shading otherwise flips the valid +Y tangent normal.
                for(int j=0;j<8;j++)b.triangle(water,n,n+1+(j+1)%8,n+1+j);
            }
            int landCount=b.landCount;
            if(landCount+b.waterCount>0){
                SceneMesh m=b.mesh((minX+maxX)/2,(minZ+maxZ)/2,Math.max(maxX-minX,maxZ-minZ)/2+1);
                m.landIndexCount=landCount;
                m.pcGround=g.pcMap!=null;
                if(stats!=null)stats.geometryNanos+=System.nanoTime()-geometryStarted;
                new TerrainMaterialField(g).attach(m,stats);
                SceneMesh fine=g.pcMap!=null||lod==2?m:detail(m,g);
                fine.pcGround=g.pcMap!=null;
                if(lod==0&&g.pcMap==null)fine=detail(fine,g);
                // Transport the entire LOD family only after canonical height/material
                // sampling. Water/land and all shared attributes follow one rounded shore.
                g.shoreline.transport(fine);
                fine.terrainLod=lod;
                // Production holds only the requested precision, not a nationwide LOD pyramid.
                if(window!=null)fine.distant=null;
                fine.chunkQ=q;fine.chunkR=r;fine.fingerprint=fingerprint;
                if(g.pcMap!=null)fine.sourceWater=sourceWater(g,q,r,fine);
                // One immutable GPU batch per streamed ground chunk. No UI-thread
                // terrain sampling/raycast per grid cell during camera gestures.
                if(window!=null&&window.span<48){if(gridScratch==null)gridScratch=gridScratch(g);fine.grid=grid(g,q,r,fine,lod,gridScratch);fine.gridDifficultMarch=g.gridDifficultMarch;}
                out.add(fine);
                if(stats!=null&&stats.firstLoadBatch!=null&&out.size()%8==0)
                    stats.firstLoadBatch.accept(Collections.unmodifiableList(new ArrayList<>(out)));
                if(stats!=null&&++stats.builtChunks%32==0&&stats.progress!=null)stats.progress.run();
            }
        }
        return Collections.unmodifiableList(out);
    }
    /** Native422980 full coarse quads, alpha mask TL/TR/BL/BR, source8x8 UV
     * animation. One owner chunk per quad center, independent of hex parity.
     * UV1 carries immutable source phase/period, COLOR retains source alpha. */
    private static SceneMesh sourceWater(MapSceneSnapshot.Ground g,int q,int r,SceneMesh owner){
        float originX=PcMap.ORIGIN*.25f+g.sourceOriginX,originZ=PcMap.ORIGIN*.25f+g.sourceOriginY;
        int left=Math.max(0,(int)Math.floor(owner.x-owner.radius+originX)-1),right=Math.min(255,(int)Math.ceil(owner.x+owner.radius+originX)+1);
        int top=Math.max(0,(int)Math.floor(owner.z-owner.radius+originZ)-1),bottom=Math.min(255,(int)Math.ceil(owner.z+owner.radius+originZ)+1);
        if(left>right||top>bottom)return null;
        // Keep original x/y admission order. A bounded packed-cell scratch
        // avoids boxing60 floats and6 indices per admitted original quad.
        int[] quads=new int[(right-left+1)*(bottom-top+1)];int count=0;
        for(int x=left;x<=right;x++)for(int y=top;y<=bottom;y++){
            if(g.pcMap.coarseWaterByte(x,y)==0)continue;
            float wx=x-originX,wz=y-originZ;Hex cell=g.grid.cell(wx+.5f,wz+.5f);
            if(q<0){if(g.valid(cell))continue;}
            else if(cell.q<q||cell.q>=q+16||cell.r<r||cell.r>=r+16)continue;
            quads[count++]=(x<<8)|y;
        }
        if(count==0)return null;
        float[] vertices=new float[count*28],stream=new float[count*32];int[] indices=new int[count*6];
        for(int quad=0;quad<count;quad++){
            int x=quads[quad]>>>8,y=quads[quad]&255;
            float wx=x-originX,wz=y-originZ;
            int first=quad*4,mask=g.pcMap.coarseWaterMask(x,y),sheet=g.pcMap.coarseWaterSheet(x,y);
            float level=g.pcMap.coarseWaterPlane(x,y);
            for(int corner=0;corner<4;corner++){
                int dx=corner&1,dz=corner>>1;
                int v=(first+corner)*7,s=(first+corner)*8;
                vertices[v]=wx+dx;vertices[v+1]=level;vertices[v+2]=wz+dz;
                vertices[v+3]=vertices[v+4]=vertices[v+5]=1f;vertices[v+6]=(mask&(1<<corner))!=0?1f:32/255f;
                stream[s]=dx+2f*sheet;stream[s+1]=(float)dz;stream[s+5]=1f;
                stream[s+6]=(float)g.pcMap.coarseWaterPhase(x,y);stream[s+7]=(float)g.pcMap.coarseWaterPeriod(x,y);
            }
            int i=quad*6;indices[i]=first;indices[i+1]=first+1;indices[i+2]=first+2;
            indices[i+3]=first+2;indices[i+4]=first+1;indices[i+5]=first+3;
        }
        SceneMesh result=new SceneMesh(vertices,indices,owner.x,owner.z,owner.radius+1);
        result.pcWater=true;result.chunkQ=q;result.chunkR=r;
        result.surfaceData=stream;
        return result;
    }
    /** Clip the original land triangles against their authored face water plane.
     * Gameplay tile categories never decide the visual coast silhouette. */
    private static void pcWaterTriangle(SurfaceBuilder b,int a,int c,int d,float level){
        float[] polygon=b.clippedPolygon;int count=0;
        for(int i=0;i<3;i++){
            int p=(i==0?a:i==1?c:d)*7,n=(i==0?c:i==1?d:a)*7;
            float py=b.vertices[p+1],ny=b.vertices[n+1];boolean pin=py<level,nin=ny<level;
            if(pin){polygon[count*3]=b.vertices[p];polygon[count*3+1]=level;polygon[count++*3+2]=b.vertices[p+2];}
            if(pin!=nin){
                double t=((double)level-py)/((double)ny-py);
                float x=(float)(b.vertices[p]+((double)b.vertices[n]-b.vertices[p])*t);
                float z=(float)(b.vertices[p+2]+((double)b.vertices[n+2]-b.vertices[p+2])*t);
                // Quantize the common edge parameter using its coarser
                // coordinate. Rounding X/Z independently can move a diagonal
                // intersection across the shared terrain triangle edge.
                double dx=(double)b.vertices[n]-b.vertices[p],dz=(double)b.vertices[n+2]-b.vertices[p+2];
                boolean xAxis=dx!=0&&(dz==0||Math.ulp(x)>=Math.ulp(z));
                float coordinate=xAxis?x:z;double start=xAxis?b.vertices[p]:b.vertices[p+2],delta=xAxis?dx:dz;
                int wet=pin?p:n;
                for(int round=0;round<2;round++){
                    double quantized=((double)coordinate-start)/delta;
                    if(py+((double)ny-py)*quantized<=level)break;
                    coordinate=Math.nextAfter(coordinate,b.vertices[wet+(xAxis?0:2)]);
                }
                double quantized=((double)coordinate-start)/delta;
                x=xAxis?coordinate:(float)(b.vertices[p]+dx*quantized);
                z=xAxis?(float)(b.vertices[p+2]+dz*quantized):coordinate;
                polygon[count*3]=x;polygon[count*3+1]=level;polygon[count++*3+2]=z;
            }
        }
        if(count<3)return;int start=b.vertexCount;
        for(int i=0;i<count;i++)b.vertex(polygon[i*3],level,polygon[i*3+2],0xff496d78);
        for(int i=1;i<count-1;i++)b.triangle(true,start,start+i,start+i+1);
    }
    private static SurfaceBuilder gridScratch(MapSceneSnapshot.Ground g){
        int faces=16*16*8*(g.pcMap==null?1:2)*2;
        SurfaceBuilder builder=new SurfaceBuilder(faces*4,faces*6);builder.shareRibbonVertices();return builder;
    }
    static SceneMesh grid(MapSceneSnapshot.Ground g,int q,int r,SceneMesh bounds,int lod){
        return grid(g,q,r,bounds,lod,gridScratch(g));
    }
    /** Same ordered ribbons; task-local scratch owns no published arrays. */
    private static SceneMesh grid(MapSceneSnapshot.Ground g,int q,int r,SceneMesh bounds,int lod,SurfaceBuilder b){
        b.reset();
        float width=lod==0?.012f:lod==1?.023f:.038f;
        for(int rr=Math.max(0,r);rr<Math.min(r+16,g.height);rr++)for(int qq=Math.max(0,q);qq<Math.min(q+16,g.width);qq++){
            Hex h=new Hex(qq,rr);if(!g.gridCell(h,false))continue;
            float x=g.grid.x(h),z=g.grid.z(h),cy=g.surface.at(h);
            for(int e=0;e<8;e++){
                float[] a=EDGE[e],d=EDGE[(e+1)%8];
                float ax=x+a[0],az=z+a[1],dx=x+d[0],dz=z+d[1];
                float ay=g.surface.sample(ax,az),dy=g.surface.sample(dx,dz);
                // Each half-stroke stays inside its eligible cell. Shared edges
                // join without drawing over a mountain/restricted cell interior.
                int segments=g.pcMap==null?1:2;
                for(int part=0;part<segments;part++){
                    float t0=part/(float)segments,t1=(part+1)/(float)segments;
                    float ex=ax+(dx-ax)*t0,ez=az+(dz-az)*t0,fx=ax+(dx-ax)*t1,fz=az+(dz-az)*t1;
                    float ey=g.surface.sample(ex,ez),fy=g.surface.sample(fx,fz);
                    gridRibbon(b,g,x,z,cy,ex,ez,ey,fx,fz,fy,width,.018f,0xff243b38);
                    gridRibbon(b,g,x,z,cy,ex,ez,ey,fx,fz,fy,width*.42f,.022f,0xffd9dfbe);
                }
            }
        }
        SceneMesh result=b.mesh(bounds.x,bounds.z,bounds.radius);result.landIndexCount=-1;
        return result;
    }
    private static void gridRibbon(SurfaceBuilder b,MapSceneSnapshot.Ground g,float x,float z,float y,
            float ax,float az,float ay,float dx,float dz,float dy,float inset,float lift,int color){
        // Exact barycentric inset within the same visible fan planes, including
        // rounded shoreline transport. Width is an offline LOD choice, not a rule.
        float[] a=g.shoreline.project(ax,az),d=g.shoreline.project(dx,dz);
        float t=inset*2;
        b.face(a[0],ay+lift,a[1],d[0],dy+lift,d[1],
            d[0]+(x-d[0])*t,dy+(y-dy)*t+lift,d[1]+(z-d[1])*t,
            a[0]+(x-a[0])*t,ay+(y-ay)*t+lift,a[1]+(z-a[1])*t,color);
    }
    /** Interior subdivision adds close-range material detail; boundary geometry is identical at both LODs. */
    static SceneMesh detail(SceneMesh coarse,MapSceneSnapshot.Ground g){
        Builder b=new Builder();for(float f:coarse.vertices)b.v.add(f);
        for(int t=0;t<coarse.indices.length;t+=3){
            int a=coarse.indices[t],c=coarse.indices[t+1],d=coarse.indices[t+2],n=b.v.size()/7;
            float x=(coarse.vertices[a*7]+coarse.vertices[c*7]+coarse.vertices[d*7])/3,
                y=(coarse.vertices[a*7+1]+coarse.vertices[c*7+1]+coarse.vertices[d*7+1])/3,
                z=(coarse.vertices[a*7+2]+coarse.vertices[c*7+2]+coarse.vertices[d*7+2])/3;
            b.vertex(x,y,z,0xffffffff);
            for(int j=3;j<7;j++)b.v.set(n*7+j,(coarse.vertices[a*7+j]+coarse.vertices[c*7+j]+coarse.vertices[d*7+j])/3);
            Collections.addAll(b.i,a,c,n,c,d,n,d,a,n);
        }
        SceneMesh fine=b.mesh(coarse.x,coarse.z,coarse.radius);fine.distant=coarse;fine.landIndexCount=coarse.landIndexCount<0?-1:coarse.landIndexCount*3;
        if(coarse.surfaceData!=null){
            fine.surfaceData=Arrays.copyOf(coarse.surfaceData,fine.vertices.length/7*8);
            int start=coarse.vertices.length/7;
            for(int t=0;t<coarse.indices.length;t+=3)for(int j=0;j<8;j++)
                fine.surfaceData[(start+t/3)*8+j]=(coarse.surfaceData[coarse.indices[t]*8+j]+coarse.surfaceData[coarse.indices[t+1]*8+j]+coarse.surfaceData[coarse.indices[t+2]*8+j])/3;
        }
        // R05: resample only newly introduced interior vertices. Shared edge samples
        // remain byte-identical across chunks/LODs; geometry and ray picking do not move.
        // Interpolating the old field here added triangles but no shoreline detail.
        if(fine.surfaceData!=null){
            WaterVisualField field=g.waterField();
            int start=coarse.vertices.length/7;
            for(int t=0;t<coarse.indices.length;t+=3){
                int i=start+t/3;float x=fine.surfaceData[i*8],z=-fine.surfaceData[i*8+1];
                fine.surfaceData[i*8+6]=field.distance(x,z);
                if(t>=coarse.landIndexCount)fine.surfaceData[i*8+7]=field.flowAngle(x,z);
            }
        }
        return fine;
    }
    /** Explicit temporary silhouettes: walled city, pier, gate, standard, farm, tower. */
    static SceneMesh proxy(int kind,int color){
        Builder b=new Builder();
        switch(kind){
            case 0:
                b.box(-.9f,-.8f,1.8f,1.6f,.12f,0xff8e8975);
                b.box(-.9f,-.8f,1.8f,.12f,.35f,color);b.box(-.9f,.68f,1.8f,.12f,.35f,color);
                b.box(-.9f,-.8f,.12f,1.6f,.35f,color);b.box(.78f,-.8f,.12f,1.6f,.35f,color);
                b.box(-.35f,-.3f,.7f,.6f,.65f,0xff776857);
                for(float x:new float[]{-.9f,.68f})for(float z:new float[]{-.8f,.58f})b.box(x,z,.22f,.22f,.55f,color);break;
            case 1:
                b.box(-.4f,-.35f,.65f,.6f,.32f,color);b.box(-.1f,.25f,.16f,.65f,.10f,0xffb1a085);break;
            case 2:
                b.box(-.45f,-.2f,.25f,.4f,.6f,color);b.box(.2f,-.2f,.25f,.4f,.6f,color);b.quad(-.45f,.6f,-.2f,.9f,.4f,color);break;
            case 3:
                b.box(-.025f,-.025f,.05f,.05f,.9f,0xffd8c8a5);
                b.face(new float[]{0,.9f,0,.4f,.9f,0,.4f,.55f,0,0,.55f,0},color);break;
            case 4:
                b.box(-.3f,-.3f,.6f,.6f,.15f,color);for(int j=0;j<3;j++)b.box(-.25f+j*.18f,-.25f,.08f,.5f,.2f,0xff687e45);break;
            default:b.box(-.22f,-.22f,.44f,.44f,.65f,color);b.box(-.32f,-.32f,.64f,.64f,.12f,0xff807765);
        }
        return b.mesh(0,0,2);
    }
}
