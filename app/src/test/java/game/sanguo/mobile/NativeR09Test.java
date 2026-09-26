package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
import java.io.*;
/** Frozen official region and same production geometry at both LODs. */
public final class NativeR09Test {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception {
  World w=ScenarioCatalog.load("coalition-190",5);byte[] before=SaveCodec.encode(w);
  MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);EnumMap<World.Terrain,Integer> counts=new EnumMap<>(World.Terrain.class);
  Set<Integer> sites=new TreeSet<>();for(World.City c:w.cities)if(c.hex.q>=120&&c.hex.q<152&&c.hex.r>=68&&c.hex.r<100)sites.add(c.id);
  for(int r=68;r<100;r++)for(int q=120;q<152;q++)counts.merge(w.terrain[q][r],1,Integer::sum);
  check(sites.containsAll(List.of(20015,20043,20063)),"Luoyang/Hulao/Mengjin official sites in fixed 32x32 region");
  for(World.Terrain t:List.of(World.Terrain.WATER,World.Terrain.MOUNTAIN,World.Terrain.FOREST,World.Terrain.ROAD))check(counts.getOrDefault(t,0)>100,"mixed authentic landscape "+t);
  check(Arrays.equals(before,SaveCodec.encode(w)),"region/profile reads preserve full authority and RNG");
  FieldAssets assets=new FieldAssets(n->new FileInputStream("app/src/main/assets/3d/field/"+n));
  // A T junction crossing both chunk axes exercises joint ownership and socket continuity.
  World path=new World(20,20,"A","B");for(World.Terrain[] row:path.terrain)Arrays.fill(row,World.Terrain.PLAIN);
  Hex hub=new Hex(8,8);path.terrain[8][8]=World.Terrain.MOUNTAIN_PATH;
  java.util.List<Hex> neighbors=hub.neighbors();for(int i:new int[]{0,2,4})path.terrain[neighbors.get(i).q][neighbors.get(i).r]=World.Terrain.MOUNTAIN_PATH;
  MapSceneSnapshot.Ground pg=new MapSceneSnapshot.Ground(path);
  List<SceneMesh> meshes=Vegetation.buildWindow(pg,Set.of(),List.of(),assets,new SceneMesh.TerrainWindow(8,8,6,6,10));
  Set<String> triangles=new HashSet<>();int triCount=0;
  for(SceneMesh m:meshes){
   check(Arrays.equals(m.vertices,m.distant.vertices),"route identity/height/color is LOD independent");
   for(int i=0;i<m.indices.length;i+=3){
    List<String> vertices=new ArrayList<>();float[] x=new float[3],z=new float[3];
    for(int k=0;k<3;k++){int at=m.indices[i+k]*7;x[k]=m.vertices[at];z[k]=m.vertices[at+2];
     check(Math.abs(m.vertices[at+1]-pg.surface.meshHeight(x[k],z[k])-LandscapeProfile.SURFACE_LIFT)<1e-5,"terrain contact");
     vertices.add(Float.floatToIntBits(x[k])+":"+Float.floatToIntBits(z[k]));}
    double area=(x[1]-x[0])*(z[2]-z[0])-(x[2]-x[0])*(z[1]-z[0]);check(Math.abs(area)>1e-8,"no degenerate junction face");
    Collections.sort(vertices);check(triangles.add(vertices.toString()),"no duplicate coplanar face across chunks");triCount++;
   }
  }
  check(triCount>100,"actual connected path geometry");
  check(LandscapeProfile.sceneryScale(0,1)<1&&LandscapeProfile.sceneryScale(2,1)==1,"canopy ratio preserves shrub scale");
  System.out.println("PASS R09 "+checks+" checks; official region q120..151/r68..99 "+counts+" sites="+sites+" path triangles="+triCount);
 }
}
