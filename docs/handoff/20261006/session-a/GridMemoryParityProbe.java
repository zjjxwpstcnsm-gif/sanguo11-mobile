package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
/** Actual production grid memory and indexed payload; desktop only. */
public final class GridMemoryParityProbe {
 public static void main(String[] args)throws Exception{
  for(int source:new int[]{7,11,14}){
   World w=PcScenarioCatalog.preview(PcScenarioCatalog.all().get(source).identity.scenarioId);byte[] before=SaveCodec.encode(w);var g=new MapSceneSnapshot.Ground(w);
   for(float span:new float[]{8,33,47,50}){
    SceneCamera camera=new SceneCamera();camera.perspective=true;camera.width=930;camera.height=1000;camera.heightLimit=PcMap.MAX_HEIGHT;
    camera.x=(g.minX+g.maxX)/2;camera.z=(g.minZ+g.maxZ)/2;camera.span=span;camera.sanitize();
    var window=new SceneMesh.TerrainWindow(camera.x,camera.z,camera.extentX(),camera.extentZ(),span);
    var meshes=SceneMesh.ground(g,List.of(),window);MapMemoryProbe.report("source="+source+" span="+span,meshes);
    long gridBytes=0,gridVertices=0;for(SceneMesh m:meshes)if(m.grid!=null){gridBytes+=4L*(m.grid.vertices.length+m.grid.indices.length);gridVertices+=m.grid.vertices.length/7;}
    System.out.println("PARITY source="+source+" span="+span+" chunks="+meshes.size()+" sha="+MeshParityProbe.digest(meshes));
    System.out.println("GRID source="+source+" span="+span+" vertices="+gridVertices+" bytes="+gridBytes);
    String hash=MeshParityProbe.digest(meshes);if(!hash.equals(MeshParityProbe.digest(SceneMesh.ground(g,meshes,window))))throw new AssertionError("cache changed payload");
    SceneMesh.ground(g,meshes,new SceneMesh.TerrainWindow(camera.x+16,camera.z+16,camera.extentX(),camera.extentZ(),span));
    if(!hash.equals(MeshParityProbe.digest(meshes)))throw new AssertionError("published mesh mutated");
   }
   if(!Arrays.equals(before,SaveCodec.encode(w)))throw new AssertionError("changed complete save/RNG");
  }
 }
}
