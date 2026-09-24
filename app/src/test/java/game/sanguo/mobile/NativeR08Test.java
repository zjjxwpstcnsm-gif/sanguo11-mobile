package game.sanguo.mobile;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;
public final class NativeR08Test {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static SceneMesh.TerrainWindow window(float x,float z){return new SceneMesh.TerrainWindow(x,z,7,7,10);}
 static long bytes(List<SceneMesh> list){long n=0;Set<SceneMesh> unique=Collections.newSetFromMap(new IdentityHashMap<>());for(SceneMesh m:list){unique.add(m);unique.add(m.distant);}for(SceneMesh a:unique)n+=4L*(a.vertices.length+a.indices.length+a.uv.length+a.tangents.length);return n;}
 public static void main(String[] args)throws Exception{
  FieldAssets assets=new FieldAssets(n->new FileInputStream("app/src/main/assets/3d/field/"+n));
  World w=new World(96,96,"甲","乙");for(World.Terrain[] row:w.terrain)Arrays.fill(row,World.Terrain.FOREST);
  MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);Set<Hex> excluded=new HashSet<>();excluded.add(new Hex(21,22));
  long started=System.nanoTime();List<SceneMesh> first=Vegetation.buildWindow(g,excluded,List.of(),assets,window(30,30));long elapsed=System.nanoTime()-started;
  check(first.size()<100,"bounded view residency rather than all 144 map chunks");
  List<SceneMesh> repeat=Vegetation.buildWindow(g,excluded,first,assets,window(30,30));check(repeat.equals(first),"same request reuses every chunk");
  List<SceneMesh> reload=Vegetation.buildWindow(g,excluded,List.of(),assets,window(30,30));
  for(int i=0;i<first.size();i++)check(Arrays.equals(first.get(i).vertices,reload.get(i).vertices),"reload byte determinism");
  w.mapRevision++;MapSceneSnapshot.Ground revision=new MapSceneSnapshot.Ground(w);
  check(Vegetation.buildWindow(revision,excluded,first,assets,window(30,30)).equals(first),"version alone cannot reshuffle production forest");
  w.terrain[24][24]=World.Terrain.ROAD;
  MapSceneSnapshot.Ground edited=new MapSceneSnapshot.Ground(w);
  List<SceneMesh> patch=Vegetation.buildWindow(edited,excluded,first,assets,window(30,30));
  long reuse=patch.stream().filter(first::contains).count();check(reuse>0&&reuse>=first.size()-9&&reuse<first.size(),"local edit changes only bounded halo");
  Set<String> anchors=new HashSet<>();int[] families=new int[4];
  for(int r=0;r<96;r++)for(int q=0;q<96;q++){
   Hex h=new Hex(q,r);for(Vegetation.Placement a:Vegetation.placements(edited,excluded,h)){
    check(anchors.add(Float.floatToIntBits(a.x)+":"+Float.floatToIntBits(a.z)),"unique world anchor");
    check(h.equals(edited.grid.cell(a.x,a.z)),"canonical owning cell");
    check(Math.hypot(a.x-edited.grid.x(24,24),a.z-edited.grid.z(24,24))>1.44,"road apron");
    check(Math.hypot(a.x-edited.grid.x(21,22),a.z-edited.grid.z(21,22))>1.44,"entity apron");families[a.family]++;
   }
  }
  for(int i=0;i<3;i++)check(families[i]>0,"broadleaf/conifer/shrub present");
  Set<Hex> unitExclusion=Vegetation.exclusions(new MapSceneSnapshot(g,w,null,-1));check(unitExclusion.containsAll(g.bases),"site footprint exclusion");
  for(SceneMesh m:patch)for(SceneMesh a:new SceneMesh[]{m,m.distant}){
   check(a.chunkQ==m.chunkQ&&a.chunkR==m.chunkR,"both LODs have stable replacement identity");
   check(a.uv.length==a.vertices.length/7*2,"merged UV count");for(int index:a.indices)check(index>=0&&index<a.vertices.length/7,"bounded index");
   for(float v:a.vertices)check(Float.isFinite(v),"finite geometry");
   check(a.indices.length%3==0,"triangles");
  }
  for(String name:new String[]{"tree","tree-upland","shrub","rock-strata"}){
   SceneMesh near=assets.mesh(name+"-lod0"),far=assets.mesh(name+"-lod1");check(far.indices.length<near.indices.length,"real reduced geometry "+name);
  }
  // Every connected road vertex conforms to the same canonical surface, including a chunk seam.
  World roads=new World(24,24,"甲","乙");for(World.Terrain[] row:roads.terrain)Arrays.fill(row,World.Terrain.ROAD);
  MapSceneSnapshot.Ground rg=new MapSceneSnapshot.Ground(roads);
  for(SceneMesh m:Vegetation.buildWindow(rg,Set.of(),List.of(),assets,window(12,12)))for(int i=0;i<m.vertices.length;i+=7)
   check(Math.abs(m.vertices[i+1]-rg.surface.meshHeight(m.vertices[i],m.vertices[i+2])-.012f)<.00001f,"road follows ground");
  // No visual mutation of terrain/revision by construction.
  check(w.mapRevision==1&&w.terrain[24][24]==World.Terrain.ROAD,"authority unchanged");
  System.out.println("PASS R08 "+checks+" checks; mergedChunks="+first.size()+" CPU_mesh_bytes_estimate="+bytes(first)+" build_ms="+elapsed/1e6+" reusedAfterEdit="+reuse+" (host, not GPU/device performance)");
 }
}
