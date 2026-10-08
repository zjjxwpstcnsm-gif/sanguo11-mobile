package game.sanguo.mobile;
import game.sanguo.core.*;
import java.lang.management.ManagementFactory;
import java.util.*;
/** Real CPU producer attribution only; this is not Android normal-entry acceptance. */
public final class PrepareAllocationProbe313 {
 public static void main(String[] args)throws Exception {
  var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
  if(!bean.isThreadAllocatedMemorySupported())throw new AssertionError("allocation counter unavailable");
  bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().getId();
  World world=PcScenarioCatalog.preview(PcScenarioCatalog.all().get(14).identity.scenarioId);
  byte[] save=SaveCodec.encode(world);var g=new MapSceneSnapshot.Ground(world);
  SceneCamera c=new SceneCamera();c.perspective=true;c.width=1028;c.height=589;c.heightLimit=PcMap.MAX_HEIGHT;
  c.x=(g.minX+g.maxX)/2;c.z=(g.minZ+g.maxZ)/2;c.yaw=.6000061f;c.span=87.153015f;c.sanitize();
  var window=new SceneMesh.TerrainWindow(c.x,c.z,c.extentX(),c.extentZ(),c.span);
  long allocated=bean.getThreadAllocatedBytes(thread),cpu=bean.getCurrentThreadCpuTime(),wall=System.nanoTime();
  var stats=new SceneMesh.BuildStats();var meshes=SceneMesh.ground(g,List.of(),window,stats);
  long bytes=bean.getThreadAllocatedBytes(thread)-allocated,ns=System.nanoTime()-wall,cpuNs=bean.getCurrentThreadCpuTime()-cpu;
  long payload=SceneMesh.payloadBytes(meshes);
  if(!Arrays.equals(save,SaveCodec.encode(world)))throw new AssertionError("full Save/RNG changed");
  System.out.println("source14\t"+meshes.size()+"\t"+bytes+"\t"+payload+"\t"+ns+"\t"+cpuNs+"\t"+MeshParityProbe.digest(meshes));
 }
}
