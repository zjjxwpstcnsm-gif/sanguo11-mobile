package game.sanguo.mobile;

import game.sanguo.core.*;
import java.util.*;

/** CPU-only mesh preparation; no Android or renderer references. */
final class SceneMesh {
    private static final World.Terrain[] TERRAIN_TYPES=World.Terrain.values();
    SceneMesh distant;
    long fingerprint; int chunkQ,chunkR;
    float[] surfaceData; float[] uv; final float[] vertices; final int[] indices; final float x,z,radius;
    SceneMesh(List<Float> v,List<Integer> i,float x,float z,float radius){
        vertices=new float[v.size()];for(int n=0;n<v.size();n++)vertices[n]=v.get(n);
        indices=new int[i.size()];for(int n=0;n<i.size();n++)indices[n]=i.get(n);
        this.x=x;this.z=z;this.radius=radius;
    }
    SceneMesh(float[] vertices,int[] indices,float x,float z,float radius){
        this.vertices=vertices;this.indices=indices;this.x=x;this.z=z;this.radius=radius;
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
    static List<SceneMesh> ground(MapSceneSnapshot.Ground g){return ground(g,Collections.emptyList());}
    static List<SceneMesh> ground(MapSceneSnapshot.Ground g,List<SceneMesh> previous){
        List<SceneMesh> out=new ArrayList<>();Map<String,SceneMesh> cached=new HashMap<>();
        for(SceneMesh m:previous)cached.put(m.chunkQ+":"+m.chunkR,m);
        for(int r=0;r<g.height;r+=16)for(int q=0;q<g.width;q+=16){
            if(Thread.currentThread().isInterrupted())return Collections.emptyList();
            long fingerprint=1469598103934665603L ^ TerrainMaterialField.VERSION;
            fingerprint=(fingerprint^g.width)*1099511628211L;fingerprint=(fingerprint^g.height)*1099511628211L;
            fingerprint=(fingerprint^Float.floatToIntBits(g.grid.offset))*1099511628211L;fingerprint=(fingerprint^(g.grid.staggered?1:0))*1099511628211L;
            for(int rr=r-6;rr<Math.min(r+22,g.height);rr++)for(int qq=q-6;qq<Math.min(q+22,g.width);qq++){
                Hex h=new Hex(qq,rr);int value=g.valid(h)?g.terrain[rr*g.width+qq]+(g.bases.contains(h)?64:0):-1;
                fingerprint=(fingerprint^value)*1099511628211L;
                fingerprint=(fingerprint^Float.floatToIntBits(g.surface.overrides.getOrDefault(h,-1f)))*1099511628211L;
            }
            SceneMesh retained=cached.get(q+":"+r);if(retained!=null&&retained.fingerprint==fingerprint){out.add(retained);continue;}
            Builder b=new Builder();float minX=Float.MAX_VALUE,minZ=minX,maxX=-minX,maxZ=-minX;
            for(int rr=r;rr<Math.min(r+16,g.height);rr++)for(int qq=q;qq<Math.min(q+16,g.width);qq++){
                Hex h=new Hex(qq,rr);if(!g.valid(h))continue;float x=g.grid.x(h),z=g.grid.z(h);
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
                int color=terrain(g.terrain[rr*g.width+qq]),n=b.v.size()/7;boolean water=g.surface.water(h);
                b.vertex(x,g.surface.sample(x,z),z,g.surface.color(x,z,color,water));
                for(float[] edge:EDGE){float vx=x+edge[0],vz=z+edge[1];b.vertex(vx,g.surface.sample(vx,vz),vz,g.surface.color(vx,vz,color,water));}
                for(int j=0;j<8;j++)Collections.addAll(b.i,n,n+1+j,n+1+(j+1)%8);
            }
            if(!b.i.isEmpty()){
                SceneMesh m=b.mesh((minX+maxX)/2,(minZ+maxZ)/2,Math.max(maxX-minX,maxZ-minZ)/2+1);
                new TerrainMaterialField(g).attach(m);
                SceneMesh fine=detail(m,g);
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
        SceneMesh fine=b.mesh(coarse.x,coarse.z,coarse.radius);fine.distant=coarse;
        if(coarse.surfaceData!=null){
            fine.surfaceData=Arrays.copyOf(coarse.surfaceData,fine.vertices.length/7*8);
            int start=coarse.vertices.length/7;
            for(int t=0;t<coarse.indices.length;t+=3)for(int j=0;j<8;j++)
                fine.surfaceData[(start+t/3)*8+j]=(coarse.surfaceData[coarse.indices[t]*8+j]+coarse.surfaceData[coarse.indices[t+1]*8+j]+coarse.surfaceData[coarse.indices[t+2]*8+j])/3;
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
