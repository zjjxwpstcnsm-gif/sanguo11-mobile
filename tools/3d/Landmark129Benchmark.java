package game.sanguo.mobile;
import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.lang.management.ManagementFactory;
import org.json.*;
/** Actual SiteGlb/Vegetation CPU-only measurements; never Android/GPU/FPS claims. */
public final class Landmark129Benchmark {
 static final Path ROOT=Path.of("app/src/main/assets/3d/field");
 static final Set<String> FAMILIES=Set.of("fall-narrow","fall-wide","fall-hukou","wall-earth","beacon-han","cliff-granite","cliff-sandstone","cliff-karst","shore-reeds","shore-rock");
 static volatile long sink;
 static long bytes(SceneMesh m){return 4L*(m.vertices.length+m.indices.length+(m.uv==null?0:m.uv.length)+(m.tangents==null?0:m.tangents.length));}
 static FieldAssets assets(int version)throws Exception{return new FieldAssets(n->{String file=Path.of(n).getFileName().toString(),family=file.replaceAll("-lod[01]\\.glb$","");return Files.newInputStream(ROOT.resolve(FAMILIES.contains(family)?"v"+version+"/"+file:n));});}
 static long median(long[] v){Arrays.sort(v);return v[v.length/2];}
 public static void main(String[] args)throws Exception{
  var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);bean.setThreadCpuTimeEnabled(true);long id=Thread.currentThread().getId();JSONObject result=new JSONObject();JSONArray decode=new JSONArray(),windows=new JSONArray();
  for(int version:new int[]{128,129}){
   List<byte[]> files=new ArrayList<>();for(String f:new TreeSet<>(FAMILIES))for(int lod=0;lod<2;lod++)files.add(Files.readAllBytes(ROOT.resolve("v"+version+"/"+f+"-lod"+lod+".glb")));
   long[] cpu=new long[7],alloc=new long[7];long resident=0;
   for(int round=-3;round<7;round++){
    long c=bean.getCurrentThreadCpuTime(),a=bean.getThreadAllocatedBytes(id),sum=0;
    for(int repeat=0;repeat<10;repeat++)for(byte[] raw:files){SceneMesh m=SiteGlb.read(new ByteArrayInputStream(raw));sum+=bytes(m);sink+=m.indices.length;}
    if(round>=0){cpu[round]=bean.getCurrentThreadCpuTime()-c;alloc[round]=bean.getThreadAllocatedBytes(id)-a;resident=sum/10;}
   }
   decode.put(new JSONObject().put("version",version).put("models_per_round",200).put("median_thread_cpu_ms_per_20_models",median(cpu)/1e7).put("median_allocated_bytes_per_20_models",median(alloc)/10).put("resident_arrays_bytes_20_models",resident));
  }
  World w=ScenarioCatalog.load(ScenarioCatalog.summaries().get(0).id,0);byte[] before=SaveCodec.encode(w);var snap=new MapSceneSnapshot(new MapSceneSnapshot.Ground(w),w,null,-1);var g=snap.ground;Set<Hex> exclusions=Vegetation.exclusions(snap);
  for(int version:new int[]{128,129})for(int[] xy:new int[][]{{147,59},{78,43},{31,175},{146,47},{92,15}}){
   var a=assets(version);Hex at=MapCoordinates.fromNationalSource(w,new SourceGridCoord(xy[0],xy[1]));var win=new SceneMesh.TerrainWindow(g.grid.x(at),g.grid.z(at),5,5,10);
   long[] cpu=new long[3],alloc=new long[3];long nearTris=0,farTris=0,nearBytes=0,farBytes=0;int chunks=0;List<SceneMesh> last=null;
   for(int round=-1;round<3;round++){
    long c=bean.getCurrentThreadCpuTime(),ab=bean.getThreadAllocatedBytes(id);var meshes=Vegetation.buildWindow(g,exclusions,List.of(),a,win);long dt=bean.getCurrentThreadCpuTime()-c,da=bean.getThreadAllocatedBytes(id)-ab;
    if(round>=0){cpu[round]=dt;alloc[round]=da;}nearTris=farTris=nearBytes=farBytes=0;chunks=meshes.size();
    for(var m:meshes){nearTris+=m.indices.length/3;farTris+=m.distant.indices.length/3;nearBytes+=bytes(m);farBytes+=bytes(m.distant);}
    last=meshes;
   }
   if(!last.equals(Vegetation.buildWindow(g,exclusions,last,a,win)))throw new AssertionError("cache reuse");
   if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("authority/RNG changed");
   JSONObject row=new JSONObject().put("version",version).put("source_x",xy[0]).put("source_y",xy[1]).put("span",10).put("chunks",chunks).put("near_triangles",nearTris).put("far_triangles",farTris).put("near_arrays_bytes",nearBytes).put("far_arrays_bytes",farBytes).put("median_thread_cpu_build_ms",median(cpu)/1e6).put("median_allocated_bytes",median(alloc)).put("cache_identity","PASS").put("full_save_rng","PASS");windows.put(row);System.err.println(row.toString());
  }
  result.put("kind","HOST_JAVA17_ACTUAL_PARSER_AND_PRODUCTION_VEGETATION_NOT_ANDROID_FPS").put("decode",decode).put("windows",windows).put("note","Asset Source remaps only ten landmark GLB families to v128/v129; production parser and window builder are unchanged isolated-clone baseline. Parent v129 footprint/support fixes and actual device timing are separate.");
  System.out.println(result.toString(2));
 }
}
