package game.sanguo.mobile;
import game.sanguo.core.*;
import java.io.*;import java.nio.file.*;import java.util.*;
/** Offline production mesh payload export; intentionally no APK/device acceptance claim. */
public final class PoisonSpring129Export {
 public static void main(String[] args)throws Exception{
  Path out=Path.of(args[0]);Files.createDirectories(out);World w=ScenarioCatalog.all().get(0);byte[] authority=SaveCodec.encode(w);var g=new MapSceneSnapshot.Ground(w);
  List<SceneMesh> meshes=SceneMesh.ground(g,List.of(),new SceneMesh.TerrainWindow(18,165,12,12,20));
  try(DataOutputStream d=new DataOutputStream(Files.newOutputStream(out.resolve("mesh.bin")))){
   d.writeInt(0x50533129);d.writeInt(TerrainMaterialField.VERSION);d.writeInt(meshes.size());
   for(var m:meshes){d.writeInt(m.vertices.length);d.writeInt(m.indices.length);d.writeInt(m.landIndexCount);for(float f:m.vertices)d.writeFloat(f);for(float f:m.surfaceData)d.writeFloat(f);for(int i:m.indices)d.writeInt(i);}
  }
  StringBuilder csv=new StringBuilder("q,r,source_x,source_y,world_x,world_z\n");
  for(int r=0;r<g.height;r++)for(int q=0;q<g.width;q++)if(g.terrain[r*g.width+q]==World.Terrain.POISON.ordinal()){
   var s=g.source(new Hex(q,r));csv.append(q+","+r+","+s.x+","+s.y+","+g.grid.x(q,r)+","+g.grid.z(q,r)+"\n");
  }
  Files.writeString(out.resolve("all-poison-cells.csv"),csv.toString());
  if(!Arrays.equals(authority,SaveCodec.encode(w)))throw new AssertionError("export changed authority");
  System.out.println("HOST ONLY material-version="+TerrainMaterialField.VERSION+" chunks="+meshes.size()+" complete-save/RNG=unchanged path="+out);
 }
}
