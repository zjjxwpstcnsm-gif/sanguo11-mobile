package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** CPU-only mesh preparation; no Android or renderer references. */
final class SceneMesh {
    private static final World.Terrain[] TERRAIN_TYPES=World.Terrain.values();
    SceneMesh distant;
    boolean vegetation;
    // Disjoint index ranges share the same surface and buffers, but never draw water as land.
    int landIndexCount=-1;
    long fingerprint; int chunkQ,chunkR,terrainLod;
    float[] tangents; float[] surfaceData; float[] uv; final float[] vertices; final int[] indices; final float x,z,radius;
    SceneMesh(List<Float> v,List<Integer> i,float x,float z,float radius){
        vertices=new float[v.size()];for(int n=0;n<v.size();n++)vertices[n]=v.get(n);
        indices=new int[i.size()];for(int n=0;n<i.size();n++)indices[n]=i.get(n);
        this.x=x;this.z=z;this.radius=radius;
    }
    SceneMesh(float[] vertices,int[] indices,float x,float z,float radius){
        this.vertices=vertices;this.indices=indices;this.x=x;this.z=z;this.radius=radius;
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
        float minX=Float.MAX_VALUE,minZ=minX,maxX=-minX,maxZ=-minX;
        for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++)if(g.valid(new Hex(q,r))){
            float x=g.grid.x(q,r),z=g.grid.z(q,r);minX=Math.min(minX,x);maxX=Math.max(maxX,x);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
        }
        if(minX==Float.MAX_VALUE)return null;
        minX=(float)Math.floor((minX-16)/8)*8;minZ=(float)Math.floor((minZ-16)/8)*8;maxX+=16;maxZ+=16;
        Builder b=new Builder();
        // Sample the shared field densely enough to avoid coarse background colour islands
        // showing through authoritative VOID cells. This never adds playable cells.
        final float step=2.25f;
        int columns=(int)Math.ceil((maxX-minX)/step),rows=(int)Math.ceil((maxZ-minZ)/step);
        // The regular background has no split normals: adjacent quads share payloads.
        for(int r=0;r<=rows;r++)for(int q=0;q<=columns;q++)
            b.vertex(minX+q*step,-.04f,minZ+r*step,0xffffffff);
        for(int r=0;r<rows;r++)for(int q=0;q<columns;q++){
            int n=r*(columns+1)+q,next=n+columns+1;
            Collections.addAll(b.i,n,next,next+1,n,next+1,n+1);
        }
        SceneMesh m=b.mesh((minX+maxX)/2,(minZ+maxZ)/2,Math.max(maxX-minX,maxZ-minZ)/2+8);m.landIndexCount=m.indices.length;
        // Identical weights, lighting frame, macro tone and shore data to the foreground.
        new TerrainMaterialField(g).attach(m);
        return m;
    }
    /** A view request retains only visible chunks plus a prefetch margin. All levels
     * preserve the canonical fan planes/edges, so arbitrary adjacent levels stitch exactly. */
    static final class TerrainWindow {
        final float x,z,ex,ez,span;
        TerrainWindow(float x,float z,float ex,float ez,float span){this.x=x;this.z=z;this.ex=ex+16;this.ez=ez+16;this.span=span;}
        boolean covers(float cx,float cz,float vx,float vz,float nextSpan){
            return Math.abs(cx-x)+vx<=ex-4&&Math.abs(cz-z)+vz<=ez-4&&level(nextSpan)==level(span);
        }
        static int level(float span){return span<14?0:span<40?1:2;}
        int lod(float cx,float cz){return Math.min(2,Math.max(level(span),(int)(Math.max(Math.abs(cx-x),Math.abs(cz-z))/32)));}
        boolean contains(float cx,float cz,float radius){return Math.abs(cx-x)<=ex+radius&&Math.abs(cz-z)<=ez+radius;}
    }
    static List<SceneMesh> ground(MapSceneSnapshot.Ground g){return ground(g,Collections.emptyList());}
    static List<SceneMesh> ground(MapSceneSnapshot.Ground g,List<SceneMesh> previous){
        return ground(g,previous,null);
    }
    static List<SceneMesh> ground(MapSceneSnapshot.Ground g,List<SceneMesh> previous,TerrainWindow window){
        List<SceneMesh> out=new ArrayList<>();Map<String,SceneMesh> cached=new HashMap<>();
        for(SceneMesh m:previous)cached.put(m.chunkQ+":"+m.chunkR,m);
        List<int[]> requests=new ArrayList<>();
        for(int r=0;r<g.height;r+=16)for(int q=0;q<g.width;q+=16)requests.add(new int[]{q,r});
        if(window!=null)requests.sort(Comparator.comparingDouble(a->Math.hypot(g.grid.x(a[0]+8,a[1]+8)-window.x,g.grid.z(a[0]+8,a[1]+8)-window.z)));
        for(int[] request:requests){
            int q=request[0],r=request[1];float cx=g.grid.x(q+8,r+8),cz=g.grid.z(q+8,r+8);
            if(window!=null&&!window.contains(cx,cz,13))continue;
            int lod=window==null?1:window.lod(cx,cz);
            if(Thread.currentThread().isInterrupted())return Collections.emptyList();
            long fingerprint=1469598103934665603L ^ TerrainMaterialField.VERSION ^ TerrainSurface.METADATA_VERSION ^ WaterVisualField.VERSION;
            fingerprint=(fingerprint^(window==null?g.mapSeed:g.mapIdentity))*1099511628211L;
            fingerprint=(fingerprint^lod)*1099511628211L;
            fingerprint=(fingerprint^g.width)*1099511628211L;fingerprint=(fingerprint^g.height)*1099511628211L;
            fingerprint=(fingerprint^Float.floatToIntBits(g.grid.offset))*1099511628211L;fingerprint=(fingerprint^(g.grid.staggered?1:0))*1099511628211L;
            for(int rr=r-8;rr<Math.min(r+24,g.height);rr++)for(int qq=q-8;qq<Math.min(q+24,g.width);qq++){
                Hex h=new Hex(qq,rr);int value=g.valid(h)?g.terrain[rr*g.width+qq]+(g.bases.contains(h)?64:0):-1;
                fingerprint=(fingerprint^value)*1099511628211L;
                fingerprint=(fingerprint^Float.floatToIntBits(g.surface.overrides.getOrDefault(h,-1f)))*1099511628211L;
            }
            SceneMesh retained=cached.get(q+":"+r);if(retained!=null&&retained.fingerprint==fingerprint){out.add(retained);continue;}
            Builder b=new Builder();List<Integer> waterIndices=new ArrayList<>();float minX=Float.MAX_VALUE,minZ=minX,maxX=-minX,maxZ=-minX;
            for(int rr=r;rr<Math.min(r+16,g.height);rr++)for(int qq=q;qq<Math.min(q+16,g.width);qq++){
                Hex h=new Hex(qq,rr);if(!g.valid(h))continue;float x=g.grid.x(h),z=g.grid.z(h);
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
                int color=terrain(g.terrain[rr*g.width+qq]),n=b.v.size()/7;boolean water=g.surface.water(h);
                b.vertex(x,g.surface.sample(x,z),z,g.surface.color(x,z,color,water));
                for(float[] edge:EDGE){float vx=x+edge[0],vz=z+edge[1];b.vertex(vx,g.surface.sample(vx,vz),vz,g.surface.color(vx,vz,color,water));}
                // +Y facing winding: lit double-sided shading otherwise flips the valid +Y tangent normal.
                for(int j=0;j<8;j++)Collections.addAll(water?waterIndices:b.i,n,n+1+(j+1)%8,n+1+j);
            }
            int landCount=b.i.size();b.i.addAll(waterIndices);
            if(!b.i.isEmpty()){
                SceneMesh m=b.mesh((minX+maxX)/2,(minZ+maxZ)/2,Math.max(maxX-minX,maxZ-minZ)/2+1);
                m.landIndexCount=landCount;
                new TerrainMaterialField(g).attach(m);
                SceneMesh fine=lod==2?m:detail(m,g);
                if(lod==0)fine=detail(fine,g);
                fine.terrainLod=lod;
                // Production holds only the requested precision, not a nationwide LOD pyramid.
                if(window!=null)fine.distant=null;
                fine.chunkQ=q;fine.chunkR=r;fine.fingerprint=fingerprint;out.add(fine);
            }
        }
        return Collections.unmodifiableList(out);
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
            WaterVisualField field=new WaterVisualField(g);
            int start=coarse.vertices.length/7;
            for(int t=0;t<coarse.indices.length;t+=3){
                int i=start+t/3;float x=fine.vertices[i*7],z=fine.vertices[i*7+2];
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
