package game.sanguo.mobile;

import game.sanguo.core.Hex;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Immutable source objects/models. No World, RNG, command or persistence access. */
final class PcScenery {
    private record Placement(int kind,float x,float z,float y,float yaw,int climate) {}
    private record Model(float[] data,int[] indices) {}
    private final Placement[] placements;
    private final Map<Integer,Model> models=new HashMap<>();
    PcScenery(InputStream source)throws IOException {
        byte[] bytes;
        try(InputStream raw=source;InputStream in=new GZIPInputStream(raw);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] block=new byte[8192];int n;
            while((n=in.read(block))!=-1){if(out.size()+n>2_000_000)throw new IOException("PC scenery budget");out.write(block,0,n);}
            bytes=out.toByteArray();
        }
        try{
            ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            if(b.getLong()!=0x3330304e43534350L)throw new IOException("PC scenery header");
            int count=b.getInt(),modelCount=b.getInt();
            if(count<1||count>65535||modelCount!=26)throw new IOException("PC scenery counts");
            placements=new Placement[count];
            for(int i=0;i<count;i++){
                int kind=b.getShort()&65535,x=b.getShort()&65535,z=b.getShort()&65535,y=b.getShort()&65535;
                float yaw=b.getFloat();int climate=b.getShort()&65535;
                if(kind<46||kind>48||x>512||z>512||y>255||climate>5||!Float.isFinite(yaw))throw new IOException("PC scenery transform");
                placements[i]=new Placement(kind,x*.5f-28.5f,z*.5f-28.5f,y*game.sanguo.core.PcMap.SCALE,yaw,climate);
            }
            for(int i=0;i<modelCount;i++){
                int id=b.getInt(),nv=b.getInt(),ni=b.getInt();
                if(id!=4808+i||nv<1||nv>30000||ni<3||ni%3!=0||ni>100000)throw new IOException("PC scenery model");
                float[] data=new float[nv*12];
                for(int v=0;v<data.length;v++){data[v]=b.getFloat();if(!Float.isFinite(data[v]))throw new IOException("PC scenery vertex");}
                int[] indices=new int[ni];
                for(int v=0;v<ni;v++){indices[v]=b.getInt();if(indices[v]<0||indices[v]>=nv)throw new IOException("PC scenery index");}
                models.put(id,new Model(data,indices));
            }
            if(b.hasRemaining())throw new IOException("PC scenery trailing bytes");
        }catch(BufferUnderflowException|IllegalArgumentException e){throw new IOException("PC scenery truncated",e);}
    }
    static int season(int month){if(month<1||month>12)throw new IllegalArgumentException("month");return month<=3?1:month<=6?2:month<=9?0:3;}
    /** Source41c3a0 derives climate from SHEX region/city/province. Resolver
     *41bae0 uses climate even though initial OBJS state bytes are zero. */
    static int resource(int kind,int season,int lod){
        return resource(kind,season,lod,0);
    }
    static int resource(int kind,int season,int lod,int climate){
        if(season<0||season>3||lod<0||lod>1||climate<0||climate>5)throw new IllegalArgumentException("PC model variant");
        // Model table uses spring/summer/autumn/winter; packed source sheets use
        // autumn/spring/summer/winter. They are distinct orderings.
        int modelSeason=season==1?0:season==2?1:season==0?2:3;
        boolean cold=climate==0||climate==2;
        return (kind==46?(modelSeason==3?(cold?4814:4816):4808+modelSeason*2):kind==47?4818+climate*2:kind==48?(cold?4830:4832):invalid())+lod;
    }
    private static int invalid(){throw new IllegalArgumentException("PC object kind");}
    int placementCount(){return placements.length;}
    List<SceneMesh> buildWindow(MapSceneSnapshot.Ground g,Set<Hex> excluded,List<SceneMesh> previous,
            SceneMesh.TerrainWindow window,int month)throws InterruptedException {
        if(g.pcMap==null)return Collections.emptyList();
        // National views upload the identical source far meshes in larger batches.
        // This changes ownership/culling granularity, never source model detail,
        // attributes or triangles. Medium views retain the16-cell cache.
        int season=season(month),chunk=window.span<14?8:window.span<36?16:32;
        Map<Long,SceneMesh> cache=new HashMap<>();for(SceneMesh m:previous)if(m.pcScenery)cache.put(key(m.chunkQ,m.chunkR),m);
        Map<Long,List<Placement>> groups=new TreeMap<>();
        for(Placement p:placements){
            float x=p.x-g.sourceOriginX,z=p.z-g.sourceOriginY;Hex h=g.grid.cell(x,z);
            if(!g.valid(h)||excluded.contains(h))continue;
            int q=h.q/chunk*chunk,r=h.r/chunk*chunk;
            float cx=g.grid.x(q+chunk/2,r+chunk/2),cz=g.grid.z(q+chunk/2,r+chunk/2);
            if(window.contains(cx,cz,chunk))groups.computeIfAbsent(key(q,r),k->new ArrayList<>()).add(p);
        }
        List<SceneMesh> result=new ArrayList<>();
        for(var group:groups.entrySet()){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException("PC scenery superseded");
            int q=(int)(long)group.getKey(),r=(int)(group.getKey()>>32);
            float cx=g.grid.x(q+chunk/2,r+chunk/2),cz=g.grid.z(q+chunk/2,r+chunk/2);
            boolean near=window.span<14&&Math.hypot(cx-window.x,cz-window.z)<20;
            long fingerprint=0x504353434eL^g.mapSeed^season^((long)g.sourceOriginX<<32)^((long)g.sourceOriginY<<16)^((long)chunk<<48)^(near?0x800:0);
            for(Placement p:group.getValue())fingerprint=(fingerprint^p.hashCode())*1099511628211L;
            SceneMesh prior=cache.get(group.getKey());
            if(prior!=null&&prior.fingerprint==fingerprint){result.add(prior);continue;}
            SceneMesh far=merge(g,group.getValue(),season,1,cx,cz),mesh=near?merge(g,group.getValue(),season,0,cx,cz):far;
            mesh.distant=far;mesh.chunkQ=far.chunkQ=q;mesh.chunkR=far.chunkR=r;mesh.fingerprint=fingerprint;
            mesh.landscapeChunkSize=far.landscapeChunkSize=chunk;result.add(mesh);
        }
        return Collections.unmodifiableList(result);
    }
    private SceneMesh merge(MapSceneSnapshot.Ground g,List<Placement> group,int season,int lod,float cx,float cz){
        int vertices=0,indices=0;
        for(Placement p:group){Model m=models.get(resource(p.kind,season,lod,p.climate));vertices+=m.data.length/12;indices+=m.indices.length;}
        float[] v=new float[vertices*7],normals=new float[vertices*3],uv=new float[vertices*2];int[] ix=new int[indices];
        int vertex=0,index=0;float radius=0;
        for(Placement p:group){
            Model m=models.get(resource(p.kind,season,lod,p.climate));float cs=(float)Math.cos(p.yaw),sn=(float)Math.sin(p.yaw);
            for(int a=0;a<m.data.length;a+=12){
                float[] d=m.data;float x=p.x-g.sourceOriginX+cs*d[a]+sn*d[a+2],z=p.z-g.sourceOriginY-sn*d[a]+cs*d[a+2];
                v[vertex*7]=x;v[vertex*7+1]=p.y+d[a+1];v[vertex*7+2]=z;
                System.arraycopy(d,a+8,v,vertex*7+3,4);
                normals[vertex*3]=cs*d[a+3]+sn*d[a+5];normals[vertex*3+1]=d[a+4];normals[vertex*3+2]=-sn*d[a+3]+cs*d[a+5];
                // Source UVs extend slightly beyond the sheet edge. Clamp each
                // independent source sheet before packing, so neighbouring seasons
                // never supply texels through bilinear filtering at those edges.
                uv[vertex*2]=(Math.max(.5f/512,Math.min(1-.5f/512,d[a+6]))+season)/4;
                uv[vertex*2+1]=Math.max(.5f/256,Math.min(1-.5f/256,d[a+7]));
                radius=Math.max(radius,(float)Math.hypot(x-cx,z-cz));vertex++;
            }
            int base=vertex-m.data.length/12;for(int i:m.indices)ix[index++]=i+base;
        }
        SceneMesh mesh=new SceneMesh(v,ix,cx,cz,radius+1);mesh.uv=uv;mesh.setNormals(normals);mesh.authoredTangentFrame=true;mesh.pcScenery=true;return mesh;
    }
    private static long key(int q,int r){return ((long)r<<32)|(q&0xffffffffL);}
}
