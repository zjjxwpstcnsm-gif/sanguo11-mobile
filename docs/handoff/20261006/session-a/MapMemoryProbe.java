package game.sanguo.mobile;
import game.sanguo.core.*;
import java.lang.management.*;
import java.util.*;
/** Actual CPU terrain producer under a384MiB host heap; not an Android reproduction. */
public final class MapMemoryProbe {
 static long bytes(SceneMesh m,Set<SceneMesh> seen){if(m==null||!seen.add(m))return 0;return 4L*(m.vertices.length+m.indices.length+(m.surfaceData==null?0:m.surfaceData.length)+(m.tangents==null?0:m.tangents.length)+(m.uv==null?0:m.uv.length))+bytes(m.grid,seen)+bytes(m.sourceWater,seen)+bytes(m.distant,seen);}
 static void report(String phase,List<SceneMesh> meshes){Set<SceneMesh> seen=Collections.newSetFromMap(new IdentityHashMap<>());long n=0,vertices=0;for(SceneMesh m:meshes){n+=bytes(m,seen);vertices+=m.vertices.length/7;}Runtime r=Runtime.getRuntime();System.out.println(phase+" chunks="+meshes.size()+" vertices="+vertices+" cpuArrays="+n+" heapUsed="+(r.totalMemory()-r.freeMemory())+" heapLimit="+r.maxMemory());System.out.flush();}
 public static void main(String[] args)throws Exception{
  World w=PcScenarioCatalog.preview(PcScenarioCatalog.all().get(7).identity.scenarioId);byte[] save=SaveCodec.encode(w);MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);
  SceneCamera c=new SceneCamera();c.perspective=true;c.width=930;c.height=1000;c.heightLimit=PcMap.MAX_HEIGHT;c.x=(g.minX+g.maxX)/2;c.z=(g.minZ+g.maxZ)/2;
  float dx=g.maxX-g.minX+3,dz=g.maxZ-g.minZ+3;c.span=(float)Math.max((dx*Math.abs(c.rightX())+dz*Math.abs(c.backX()))*c.height/c.width,(dx*Math.abs(c.backX())+dz*Math.abs(c.rightX()))*c.sin())*.52f;c.sanitize();
  System.out.println("viewport="+c.width+"x"+c.height+" camera="+c.x+","+c.z+" span="+c.span+" extent="+c.extentX()+","+c.extentZ()+" source="+w.scenarioId);report("world",List.of());
  SceneMesh.BuildStats stats=new SceneMesh.BuildStats();stats.firstLoadBatch=m->{if(m.size()%32==0)report("partial",m);};
  List<SceneMesh> all=SceneMesh.ground(g,List.of(),new SceneMesh.TerrainWindow(c.x,c.z,c.extentX(),c.extentZ(),c.span),stats);report("national",all);
  for(int n=0;n<4;n++){
   c.span=8;c.x=g.grid.x(w.cities.get(n*10).hex);c.z=g.grid.z(w.cities.get(n*10).hex);
   List<SceneMesh> previous=all;stats.firstLoadBatch=m->report("zoom-partial-retained",SceneMesh.retainWindowCoverage(previous,m));
   List<SceneMesh> near=SceneMesh.ground(g,all,new SceneMesh.TerrainWindow(c.x,c.z,c.extentX(),c.extentZ(),c.span),stats);report("near",near);all=near;
   c.span=110;c.x=(g.minX+g.maxX)/2;c.z=(g.minZ+g.maxZ)/2;stats.firstLoadBatch=null;
   all=SceneMesh.ground(g,all,new SceneMesh.TerrainWindow(c.x,c.z,c.extentX(),c.extentZ(),c.span),stats);report("national-return-"+n,all);
  }
  if(!Arrays.equals(save,SaveCodec.encode(w)))throw new AssertionError("pure terrain changed fullSave/RNG");
  System.out.println("HOST CPU ONLY PASS fullSave/RNG unchanged; GPU/native/device/Android lifecycle unmeasured");
 }
}
