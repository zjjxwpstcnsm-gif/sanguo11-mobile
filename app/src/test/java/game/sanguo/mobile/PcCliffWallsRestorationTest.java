package game.sanguo.mobile;

import game.sanguo.core.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public final class PcCliffWallsRestorationTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void close(float a,float b,String why){check(Math.abs(a-b)<.00005f,why+": "+a+" != "+b);}
    private record Row(int slot,int x,int z,int height,float yaw,int mask,int body) {}
    private record Part(SceneMesh source,float[] matrix,float x,float y,float z) {}
    private static final int[][] STEPS={{-4,-2},{0,-4},{4,-2},{-4,2},{0,4},{4,2}};
    public static void main(String[] args)throws Exception {
        Path path=Path.of("app/src/main/assets/3d/pc-facilities/cliff-walls.pcz");
        PcFacilities library=new PcFacilities(Files.newInputStream(Path.of("app/src/main/assets/3d/pc-facilities/facilities.pcz")));
        PcCliffWalls assets=new PcCliffWalls(Files.newInputStream(path),library);check(assets.placementCount()==161,"all original source placements loaded");check(assets.connectionCount()==154,"source six-neighbor graph emits 154 unique segments");
        byte[] raw;try(var in=new GZIPInputStream(Files.newInputStream(path))){raw=in.readAllBytes();}
        ByteBuffer b=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);b.position(12);List<Row> rows=new ArrayList<>();
        while(b.hasRemaining())rows.add(new Row(b.getShort()&65535,b.getShort()&65535,b.getShort()&65535,b.get()&255,b.getFloat(),b.get()&255,b.getShort()&65535));
        int[][] neighbors={{-1,-1},{0,-1},{1,-1},{-1,0},{0,1},{1,0},{-1,0},{0,-1},{1,0},{-1,1},{0,1},{1,1}};
        Map<Long,Row> occupancy=new HashMap<>();for(Row r:rows){int q=(r.x*2-112)/4,z=(r.z*2-112-2*(q&1))/4;check(occupancy.put(((long)q<<32)|(z&0xffffffffL),r)==null,"unambiguous source wall occupancy");}
        int edges=0;
        for(Row r:rows){int q=(r.x*2-112)/4,z=(r.z*2-112-2*(q&1))/4,mask=0;
            for(int dir=0;dir<6;dir++){int[] d=neighbors[(q&1)*6+dir];if(occupancy.containsKey(((long)(q+d[0])<<32)|((z+d[1])&0xffffffffL)))mask|=1<<dir;}
            check(mask==r.mask&&mask!=0,"source six-neighbor mask matches original same-kind objects");
            check(r.body==(((q&1)!=0||(z&1)!=0)?120:118),"source pillar parity selects original model");edges+=Integer.bitCount(mask&56);
        }check(edges==154,"each source edge emitted once in directions3..5");
        World world=ScenarioCatalog.load("heroes-250",0);byte[] before=SaveCodec.encode(world);MapSceneSnapshot.Ground ground=new MapSceneSnapshot.Ground(world);
        var full=new SceneMesh.TerrainWindow(100,100,160,160,50);List<SceneMesh> previous=List.of();int eligible=0;
        for(int month:new int[]{1,4,7,10}){
            List<SceneMesh> chunks=assets.buildWindow(ground,previous,full,month);check(!chunks.isEmpty(),"source walls resident");
            int quarter=PcFacilities.quarter(month);
            Map<Long,List<Row>> expected=new TreeMap<>();
            for(Row r:rows){Hex h=ground.grid.cell(r.x*.5f-28.5f,r.z*.5f-28.5f);if(!ground.valid(h))continue;int q=h.q/16*16,z=h.r/16*16;expected.computeIfAbsent(((long)z<<32)|(q&0xffffffffL),k->new ArrayList<>()).add(r);}
            eligible=expected.values().stream().mapToInt(List::size).sum();check(chunks.size()==expected.size(),"bounded far batching covers every valid source wall");
            int count=0;
            for(SceneMesh m:chunks){
                check(m.pcCliffWall&&m.pcFacility&&!m.pcScenery&&m.authoredTangentFrame,"distinct original wall texture/normal stream");
                check(m.distant==m&&m.landscapeChunkSize==16,"source far geometry and larger immutable batch");
                List<Row> group=expected.get(((long)m.chunkR<<32)|(m.chunkQ&0xffffffffL));int expectedVertices=0;for(Row row:group)for(Part part:parts(row,library,ground,quarter))expectedVertices+=part.source.vertices.length/7;check(m.vertices.length/7==expectedVertices,"no discarded or duplicated pillars/connected segments");
                int v=0,index=0;
                for(Row r:group){
                    for(Part part:parts(r,library,ground,quarter)){
                        SceneMesh canonical=part.source;float[] matrix=part.matrix;int nv=canonical.vertices.length/7,base=v;
                        for(int j=0;j<nv;j++,v++){
                            int a=j*7,d=v*7;float px=canonical.vertices[a],py=canonical.vertices[a+1],pz=canonical.vertices[a+2];
                            close(m.vertices[d],part.x+matrix[0]*px+matrix[1]*py+matrix[2]*pz,"source endpoint matrix X");
                            close(m.vertices[d+1],part.y+matrix[3]*px+matrix[4]*py+matrix[5]*pz,"source height shear Y");
                            close(m.vertices[d+2],part.z+matrix[6]*px+matrix[7]*py+matrix[8]*pz,"source endpoint matrix Z");
                            for(int k=3;k<7;k++)close(m.vertices[d+k],canonical.vertices[a+k],"source BGRA");
                            for(int k=0;k<2;k++)close(m.uv[v*2+k],canonical.uv[j*2+k],"exact original quarter sheet UV");
                            float[] local=normal(canonical.tangents,j*4),actual=normal(m.tangents,v*4),reference=inverseTranspose(matrix,local);
                            for(int k=0;k<3;k++)close(actual[k],reference[k],"source inverse-transpose authored normal");
                        }
                        for(int i:canonical.indices)check(m.indices[index++]==base+i,"original triangles retained per pillar/segment");
                    }count++;
                }
            }
            check(count==eligible,"all playable original wall placements present");List<SceneMesh> cached=assets.buildWindow(ground,chunks,full,month);
            for(int i=0;i<chunks.size();i++)check(cached.get(i)==chunks.get(i),"immutable chunk cache reuse");previous=chunks;
        }
        for(World crop:ScenarioCatalog.all()){
            var g=new MapSceneSnapshot.Ground(crop);int expected=0;for(Row r:rows)if(g.valid(g.grid.cell(r.x*.5f-28.5f-g.sourceOriginX,r.z*.5f-28.5f-g.sourceOriginY)))expected++;
            int expectedVertices=0;for(Row r:rows)if(g.valid(g.grid.cell(r.x*.5f-28.5f-g.sourceOriginX,r.z*.5f-28.5f-g.sourceOriginY)))for(Part part:parts(r,library,g,0))expectedVertices+=part.source.vertices.length/7;
            int total=0;
            for(SceneMesh m:assets.buildWindow(g,List.of(),new SceneMesh.TerrainWindow((g.minX+g.maxX)*.5f,(g.minZ+g.maxZ)*.5f,300,300,50),1))total+=m.vertices.length/7;
            check(total==expectedVertices&&assets.anchors(g).size()==expected,"crop origins/bounds retain only original eligible placements");
        }
        world.mapRevision=63;check(assets.buildWindow(new MapSceneSnapshot.Ground(world),previous,full,1).isEmpty(),"legacy maps reject new original walls");world.mapRevision=NationalMap.REVISION;
        check(Arrays.equals(before,SaveCodec.encode(world)),"source wall render never changes authority/RNG/save");
        byte[] bytes=Files.readAllBytes(path);boolean rejected=false;try{new PcCliffWalls(new ByteArrayInputStream(Arrays.copyOf(bytes,bytes.length/2)),library);}catch(IOException expected){rejected=true;}check(rejected,"truncated source transform data rejected");
        System.out.println("PASS PC cliff walls "+checks+" checks; source=161 playable="+eligible);
    }
    private static List<Part> parts(Row r,PcFacilities library,MapSceneSnapshot.Ground g,int quarter){
        List<Part> parts=new ArrayList<>();float x=r.x*.5f-28.5f-g.sourceOriginX,z=r.z*.5f-28.5f-g.sourceOriginY;
        float cs=(float)Math.cos(r.yaw),sn=(float)Math.sin(r.yaw);
        parts.add(new Part(library.meshIndex(14,0,r.body,quarter,1,0),new float[]{cs,0,sn,0,1,0,-sn,0,cs},x,r.height*.025f,z));
        for(int dir=3;dir<6;dir++)if((r.mask&(1<<dir))!=0){
            float dx=STEPS[dir][0]*.25f,dz=STEPS[dir][1]*.25f,length=(float)Math.hypot(dx,dz),scale=length*20*.04330126941204071f;
            int start=g.pcMap.heightByte(r.x*2,r.z*2),end=g.pcMap.heightByte(r.x*2+STEPS[dir][0],r.z*2+STEPS[dir][1]);
            parts.add(new Part(library.meshIndex(14,0,114,quarter,1,0),new float[]{dx/length*scale,0,-dz/length,(end-start)*.025f/length,1,0,dz/length*scale,0,dx/length},x,start*.025f,z));
        }return parts;
    }
    private static float[] inverseTranspose(float[] a,float[] n){
        float[] c={a[4]*a[8]-a[5]*a[7],a[5]*a[6]-a[3]*a[8],a[3]*a[7]-a[4]*a[6],a[2]*a[7]-a[1]*a[8],a[0]*a[8]-a[2]*a[6],a[1]*a[6]-a[0]*a[7],a[1]*a[5]-a[2]*a[4],a[2]*a[3]-a[0]*a[5],a[0]*a[4]-a[1]*a[3]};
        float det=a[0]*c[0]+a[1]*c[1]+a[2]*c[2];check(Math.abs(det)>.001f,"nonsingular source wall matrix");
        float[] v={(c[0]*n[0]+c[1]*n[1]+c[2]*n[2])/det,(c[3]*n[0]+c[4]*n[1]+c[5]*n[2])/det,(c[6]*n[0]+c[7]*n[1]+c[8]*n[2])/det};
        float len=(float)Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);for(int k=0;k<3;k++)v[k]/=len;return v;
    }
    private static float[] normal(float[] q,int i){float x=q[i],y=q[i+1],z=q[i+2],w=q[i+3];return new float[]{2*(x*z+w*y),2*(y*z-w*x),1-2*(x*x+y*y)};}
}
