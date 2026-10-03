package game.sanguo.mobile;

import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Source scenery through the real streamed mesh path, all seasons/LODs and crops. */
public final class PcSceneryRestorationTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        Path path=Path.of("app/src/main/assets/3d/pc-scenery/scenery.pcz");
        PcScenery assets=new PcScenery(Files.newInputStream(path));
        check(assets.placementCount()==943,"all enabled source 46/47/48 placements");
        check(PcScenery.resource(46,PcScenery.season(1),0)==4808,"source spring model paired with spring sheet4841");
        check(PcScenery.resource(46,PcScenery.season(4),0)==4810,"source summer model paired with summer sheet4842");
        check(PcScenery.resource(46,PcScenery.season(7),0)==4812,"source autumn model paired with autumn sheet4840");
        check(PcScenery.resource(46,PcScenery.season(10),0)==4814,"source winter model paired with winter sheet4843");
        // Independent original EXE41bae0 results, including all six climates.
        List<String> oracle=Files.readAllLines(Path.of("app/src/test/fixtures/native-v154/scenery-model-resolver.tsv"));
        check(oracle.size()==145,"144 original model resolver cases retained");
        for(String row:oracle.subList(1,oracle.size())){
            int[] v=Arrays.stream(row.split("\t")).mapToInt(Integer::parseInt).toArray();
            int atlasSeason=new int[]{1,2,0,3}[v[1]];
            check(PcScenery.resource(v[0],atlasSeason,v[3],v[2])==v[4],"original EXE climate/quarter/LOD resolver "+row);
        }
        World w=ScenarioCatalog.load("heroes-250",0);byte[] before=SaveCodec.encode(w);
        MapSceneSnapshot.Ground ground=new MapSceneSnapshot.Ground(w);
        List<SceneMesh> previous=Collections.emptyList();
        Set<Hex> excluded=new HashSet<>(ground.bases);
        for(MapSceneSnapshot.Item item:new MapSceneSnapshot(ground,w,w.cities.get(0).hex,-1).items)if(item.facility!=null)excluded.add(item.hex);
        for(int month:new int[]{1,4,7,10}){
            SceneMesh.TerrainWindow window=new SceneMesh.TerrainWindow(100,100,150,150,10);
            List<SceneMesh> chunks=assets.buildWindow(ground,excluded,previous,window,month);
            check(!chunks.isEmpty(),"native source forest resident");
            for(SceneMesh mesh:chunks)for(SceneMesh lod:new SceneMesh[]{mesh,mesh.distant}){
                check(lod.pcScenery&&lod.authoredTangentFrame,"source mesh and normals tagged");
                check(lod.uv.length==lod.vertices.length/7*2&&lod.indices.length%3==0,"complete source UV and triangle streams");
                for(int i=0;i<lod.vertices.length/7;i++){
                    check(Float.isFinite(lod.vertices[i*7])&&Float.isFinite(lod.vertices[i*7+1])&&Float.isFinite(lod.vertices[i*7+2]),"finite authored transform");
                    int season=PcScenery.season(month);
                    check(lod.uv[i*2]>=season*.25f-.00001f&&lod.uv[i*2]<=(season+1)*.25f+.00001f,"source UV remains within selected seasonal sheet");
                    float length=0;for(int k=0;k<4;k++)length+=lod.tangents[i*4+k]*lod.tangents[i*4+k];
                    check(Math.abs(length-1)<.001f,"rotated authored normal frame normalized");
                }
            }
            List<SceneMesh> cached=assets.buildWindow(ground,excluded,chunks,window,month);
            check(cached.size()==chunks.size(),"bounded chunk cache");
            for(int i=0;i<chunks.size();i++)check(cached.get(i)==chunks.get(i),"unchanged source chunks reused");
            if(!previous.isEmpty())check(previous.get(0)!=chunks.get(0),"season transition invalidates geometry/UV");
            previous=chunks;
        }
        for(World crop:ScenarioCatalog.all()){
            MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(crop);
            float x=(g.minX+g.maxX)*.5f,z=(g.minZ+g.maxZ)*.5f;
            for(SceneMesh mesh:assets.buildWindow(g,Collections.emptySet(),Collections.emptyList(),new SceneMesh.TerrainWindow(x,z,200,200,50),1)){
                check(mesh.pcScenery,"cropped original map uses source scenery");
                for(int i=0;i<mesh.vertices.length;i+=7){
                    check(mesh.vertices[i]>=g.minX-2&&mesh.vertices[i]<=g.maxX+2,"crop source-origin X applied");
                    check(mesh.vertices[i+2]>=g.minZ-2&&mesh.vertices[i+2]<=g.maxZ+2,"crop source-origin Z applied");
                }
            }
        }
        // Compare a far model rendered in 8-cell and 16-cell batches. Only batch
        // boundaries may change: every world vertex/UV/frame and triangle remains.
        for(int month:new int[]{1,10}){
            var small=assets.buildWindow(ground,excluded,Collections.emptyList(),new SceneMesh.TerrainWindow(-1000,-1000,2000,2000,13),month);
            var large=assets.buildWindow(ground,excluded,Collections.emptyList(),new SceneMesh.TerrainWindow(-1000,-1000,2000,2000,50),month);
            check(large.size()<small.size(),"far batching reduces GPU entities without changing source LOD");
            check(geometry(small,false).equals(geometry(large,false)),"every source far vertex, BGRA, UV and authored frame preserved");
            check(geometry(small,true).equals(geometry(large,true)),"every source far triangle and winding preserved");
            var medium=assets.buildWindow(ground,excluded,Collections.emptyList(),new SceneMesh.TerrainWindow(-1000,-1000,2000,2000,20),month);
            check(large.size()<medium.size(),"national batch count below existing medium16-cell path");
            check(geometry(medium,false).equals(geometry(large,false)),"existing16-cell far vertices and attributes exactly preserved at32 cells");
            check(geometry(medium,true).equals(geometry(large,true)),"existing16-cell far triangles and winding exactly preserved at32 cells");
            System.out.println("SOURCE_NATIONAL_BATCH month="+month+" chunks="+medium.size()+"→"+large.size());
            System.out.println("SOURCE_FAR_BATCH month="+month+" chunks="+small.size()+"→"+large.size());
        }
        w.mapRevision=63;
        check(assets.buildWindow(new MapSceneSnapshot.Ground(w),Collections.emptySet(),previous,new SceneMesh.TerrainWindow(100,100,150,150,10),1).isEmpty(),"legacy saves never receive source placements");
        w.mapRevision=NationalMap.REVISION;
        check(Arrays.equals(before,SaveCodec.encode(w)),"all season/LOD/crop rendering leaves authority/RNG/save byte-identical");
        byte[] truncated=Files.readAllBytes(path);boolean rejected=false;
        try{new PcScenery(new ByteArrayInputStream(Arrays.copyOf(truncated,truncated.length/2)));}catch(IOException expected){rejected=true;}
        check(rejected,"truncated source data rejected before GPU");
        System.out.println("PASS PC scenery "+checks+" checks; source placements=943");
    }
    private static Map<String,Integer> geometry(List<SceneMesh> meshes,boolean triangles){
        Map<String,Integer> result=new HashMap<>();
        for(SceneMesh m:meshes){
            check(m==m.distant,"comparison uses identical original far model precision");int count=m.vertices.length/7;String[] vertices=new String[count];
            for(int i=0;i<count;i++){
                java.nio.ByteBuffer b=java.nio.ByteBuffer.allocate(52);
                for(int k=0;k<7;k++)b.putInt(Float.floatToIntBits(m.vertices[i*7+k]));
                for(int k=0;k<2;k++)b.putInt(Float.floatToIntBits(m.uv[i*2+k]));
                for(int k=0;k<4;k++)b.putInt(Float.floatToIntBits(m.tangents[i*4+k]));
                vertices[i]=Base64.getEncoder().encodeToString(b.array());
                if(!triangles)result.merge(vertices[i],1,Integer::sum);
            }
            if(triangles)for(int i=0;i<m.indices.length;i+=3)result.merge(vertices[m.indices[i]]+vertices[m.indices[i+1]]+vertices[m.indices[i+2]],1,Integer::sum);
        }return result;
    }
}
