package game.sanguo.mobile;

import game.sanguo.core.Hex;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Immutable source map walls. Never creates structures, blockers or save fields. */
final class PcCliffWalls {
    private record Placement(int slot,int rawX,int rawZ,float x,float z,float y,float yaw,int mask,int body) {}
    private record Part(SceneMesh mesh,float x,float z,float y,float yaw) {}
    private static final int[][] STEPS={{-4,-2},{0,-4},{4,-2},{-4,2},{0,4},{4,2}};
    private final Placement[] placements;
    private final PcFacilities library;
    PcCliffWalls(InputStream source,PcFacilities library)throws IOException {
        this.library=Objects.requireNonNull(library);byte[] bytes;
        try(InputStream raw=source;InputStream in=new GZIPInputStream(raw);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] block=new byte[4096];int n;
            while((n=in.read(block))!=-1){if(out.size()+n>4096)throw new IOException("PC wall budget");out.write(block,0,n);}bytes=out.toByteArray();
        }
        try{
            ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);byte[] header=new byte[8];b.get(header);
            if(!Arrays.equals(header,new byte[]{'P','C','W','A','L','L','0','2'})||b.getInt()!=161)throw new IOException("PC wall header");
            placements=new Placement[161];Set<Integer> slots=new HashSet<>();
            for(int i=0;i<placements.length;i++){
                int slot=b.getShort()&65535,x=b.getShort()&65535,z=b.getShort()&65535,y=b.get()&255;float yaw=b.getFloat();
                int mask=b.get()&255,body=b.getShort()&65535,q=(x*2-112)/4,r=(z*2-112-2*(q&1))/4;
                if(slot==65535||!slots.add(slot)||x>512||z>512||!Float.isFinite(yaw))throw new IOException("PC wall transform");
                if(mask>63||body!=(mask==0?114:(q&1)!=0||(r&1)!=0?120:118))throw new IOException("PC wall source connection variant");
                placements[i]=new Placement(slot,x,z,x*.5f-28.5f,z*.5f-28.5f,y*game.sanguo.core.PcMap.SCALE,yaw,mask,body);
            }
            if(b.hasRemaining())throw new IOException("PC wall trailing bytes");
        }catch(BufferUnderflowException|IllegalArgumentException e){throw new IOException("PC wall truncated",e);}
    }
    int placementCount(){return placements.length;}
    int connectionCount(){int count=0;for(Placement p:placements)count+=Integer.bitCount(p.mask&56);return count;}
    List<Hex> anchors(MapSceneSnapshot.Ground g){
        if(g.pcMap==null)return Collections.emptyList();List<Hex> anchors=new ArrayList<>();
        for(Placement p:placements){Hex h=g.grid.cell(p.x-g.sourceOriginX,p.z-g.sourceOriginY);if(g.valid(h))anchors.add(h);}
        return Collections.unmodifiableList(anchors);
    }
    List<SceneMesh> buildWindow(MapSceneSnapshot.Ground g,List<SceneMesh> previous,SceneMesh.TerrainWindow window,int month)throws InterruptedException {
        if(g.pcMap==null)return Collections.emptyList();int quarter=PcFacilities.quarter(month),size=window.span<14?8:16;
        Map<Long,SceneMesh> cache=new HashMap<>();for(SceneMesh m:previous)if(m.pcCliffWall)cache.put(key(m.chunkQ,m.chunkR),m);
        Map<Long,List<Placement>> groups=new TreeMap<>();
        for(Placement p:placements){
            Hex h=g.grid.cell(p.x-g.sourceOriginX,p.z-g.sourceOriginY);if(!g.valid(h))continue;
            int q=h.q/size*size,r=h.r/size*size;float x=g.grid.x(q+size/2,r+size/2),z=g.grid.z(q+size/2,r+size/2);
            if(window.contains(x,z,size))groups.computeIfAbsent(key(q,r),k->new ArrayList<>()).add(p);
        }
        List<SceneMesh> result=new ArrayList<>();Map<Integer,SceneMesh> canonical=new HashMap<>();
        for(var entry:groups.entrySet()){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException("PC walls superseded");
            int q=(int)(long)entry.getKey(),r=(int)(entry.getKey()>>32);float x=g.grid.x(q+size/2,r+size/2),z=g.grid.z(q+size/2,r+size/2);
            boolean near=window.span<14&&Math.hypot(x-window.x,z-window.z)<20;
            long fingerprint=0x504357414c4cL^quarter^((long)g.sourceOriginX<<32)^((long)g.sourceOriginY<<16)^((long)size<<48)^(near?0x800:0);
            for(Placement p:entry.getValue())fingerprint=(fingerprint^p.hashCode())*1099511628211L;
            SceneMesh old=cache.get(entry.getKey());if(old!=null&&old.fingerprint==fingerprint){result.add(old);continue;}
            SceneMesh far=merge(parts(g,entry.getValue(),quarter,1,canonical),x,z),mesh=far;
            if(near)mesh=merge(parts(g,entry.getValue(),quarter,0,canonical),x,z);
            mesh.distant=far;mesh.chunkQ=far.chunkQ=q;mesh.chunkR=far.chunkR=r;mesh.fingerprint=fingerprint;
            mesh.landscapeChunkSize=far.landscapeChunkSize=size;result.add(mesh);
        }
        return Collections.unmodifiableList(result);
    }
    private SceneMesh canonical(Map<Integer,SceneMesh> cache,int model,int quarter,int lod){
        return cache.computeIfAbsent(model+lod,k->library.meshIndex(14,0,model,quarter,lod,0));
    }
    private List<Part> parts(MapSceneSnapshot.Ground g,List<Placement> group,int quarter,int lod,Map<Integer,SceneMesh> cache){
        List<Part> result=new ArrayList<>();
        for(Placement p:group){
            float x=p.x-g.sourceOriginX,z=p.z-g.sourceOriginY;
            result.add(new Part(canonical(cache,p.body,quarter,lod),x,z,p.y,p.yaw));
            if((p.mask&56)==0)continue;
            SceneMesh segment=canonical(cache,114,quarter,lod);int ix=p.rawX*2,iz=p.rawZ*2;
            int start=g.pcMap.heightByte(ix,iz);
            for(int direction=3;direction<6;direction++)if((p.mask&(1<<direction))!=0){
                int dx=STEPS[direction][0],dz=STEPS[direction][1],end=g.pcMap.heightByte(ix+dx,iz+dz);
                result.add(new Part(PcWallGeometry.segment(segment,dx*.25f,dz*.25f,(end-start)*game.sanguo.core.PcMap.SCALE),x,z,start*game.sanguo.core.PcMap.SCALE,0));
            }
        }return result;
    }
    private static SceneMesh merge(List<Part> group,float x,float z){
        int total=0,ni=0;for(Part p:group){total+=p.mesh.vertices.length/7;ni+=p.mesh.indices.length;}
        float[] v=new float[total*7],uv=new float[total*2],tangent=new float[total*4];int[] indices=new int[ni];int vertex=0,index=0;float radius=0;
        for(Part p:group){
            SceneMesh source=p.mesh;int count=source.vertices.length/7;
            float cs=(float)Math.cos(p.yaw),sn=(float)Math.sin(p.yaw),s=(float)Math.sin(p.yaw*.5f),c=(float)Math.cos(p.yaw*.5f);
            int base=vertex;
            for(int j=0;j<count;j++,vertex++){
                int a=j*7,b=vertex*7;v[b]=p.x+cs*source.vertices[a]+sn*source.vertices[a+2];v[b+1]=p.y+source.vertices[a+1];v[b+2]=p.z-sn*source.vertices[a]+cs*source.vertices[a+2];
                System.arraycopy(source.vertices,a+3,v,b+3,4);System.arraycopy(source.uv,j*2,uv,vertex*2,2);
                int t=j*4,o=vertex*4;float[] n=source.tangents;
                tangent[o]=c*n[t]+s*n[t+2];tangent[o+1]=c*n[t+1]+s*n[t+3];tangent[o+2]=c*n[t+2]-s*n[t];tangent[o+3]=c*n[t+3]-s*n[t+1];
                radius=Math.max(radius,(float)Math.hypot(v[b]-x,v[b+2]-z));
            }
            for(int i:source.indices)indices[index++]=base+i;
        }
        SceneMesh mesh=new SceneMesh(v,indices,x,z,radius+1);mesh.uv=uv;mesh.tangents=tangent;mesh.authoredTangentFrame=true;mesh.pcFacility=true;mesh.pcCliffWall=true;return mesh;
    }
    private static long key(int q,int r){return ((long)r<<32)|(q&0xffffffffL);}
}
