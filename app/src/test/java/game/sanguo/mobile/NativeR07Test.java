package game.sanguo.mobile;
import game.sanguo.core.*;
import java.nio.file.*;
import java.util.*;

/** Coverage comes from current catalog/enums, never a pinned historical site count. */
public final class NativeR07Test {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static SceneMesh asset(String family,String id)throws Exception{
  try(var in=Files.newInputStream(Path.of("app/src/main/assets/3d",family,id+".glb"))){SceneMesh m=SiteGlb.read(in);check(m.indices.length/3<15000,"triangle budget "+id);return m;}
 }
 public static void main(String[] args)throws Exception{
  List<String> rows=new ArrayList<>();rows.add("scenario\tid\tname\tkind\tasset\tyaw\tscale\tdamage\tstatus");
  Set<String> families=new TreeSet<>();int sites=0;
  for(ScenarioCatalog.Summary summary:ScenarioCatalog.summaries()){
   World w=ScenarioCatalog.load(summary.id,0);byte[] before=SaveCodec.encode(w);MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);
   for(World.City c:w.cities){SiteVisual v=new SiteVisual(w,c,g.grid);sites++;families.add(v.model);
    for(int lod=0;lod<3;lod++)asset("sites",v.model+"-lod"+lod);
    check(v.cells.equals(SiteFootprint.cells(c)),"authority footprint "+c.name);
    for(Hex cell:v.cells)check(w.cityAt(cell)==c,"entry identity "+cell);
    check(Float.isFinite(v.yaw),"finite approach");
    rows.add(summary.id+"\t"+c.id+"\t"+c.name+"\t"+c.kind+"\t"+v.model+"\t"+v.yaw+"\t"+v.scale+"\t"+v.damage+"\tTRANSITIONAL_ART");
    if(c.kind==World.SiteKind.CITY){
     World.City reidentified=new World.City(c.id+100000,c.name,c.hex,c.owner);reidentified.kind=c.kind;
     check(v.model.equals(new SiteVisual(w,reidentified,g.grid).model),"region independent of ID");
    }
   }
   check(Arrays.equals(before,SaveCodec.encode(w)),"all mapping reads preserve full save/RNG");
   World restored=SaveCodec.decode(before);MapSceneSnapshot.Ground rg=new MapSceneSnapshot.Ground(restored);
   for(World.City c:restored.cities)check(new SiteVisual(w,w.city(c.id),g.grid).model.equals(new SiteVisual(restored,c,rg.grid).model),"restored family");
  }
  // Geometry proves meaningful regional differences at every LOD, not just flag colors.
  for(int lod=0;lod<3;lod++)for(int a=0;a<3;a++)for(int b=a+1;b<3;b++)
   check(!Arrays.equals(asset("sites","city"+a+"-lod"+lod).vertices,asset("sites","city"+b+"-lod"+lod).vertices),"different regional silhouette");
  World custom=new World(24,24,"甲","乙");for(World.Terrain[] column:custom.terrain)Arrays.fill(column,World.Terrain.PLAIN);
  World.City site=new World.City(90001,"custom",new Hex(12,12),0);custom.cities.add(site);
  for(World.SiteKind kind:World.SiteKind.values()){
   site.kind=kind;MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(custom);SiteVisual v=new SiteVisual(custom,site,g.grid);
   check(families.contains(v.model),"custom default family");
   custom.cities.remove(site);site=new World.City(90001,"custom",new Hex(13,12),0);site.kind=kind;custom.cities.add(site);MapSceneSnapshot moved=new MapSceneSnapshot(new MapSceneSnapshot.Ground(custom),custom,null,-1);
   check(moved.items.get(0).hex.equals(site.hex),"move updates anchor");
   site.defense=0;check(new SiteVisual(custom,site,g.grid).damage==2,"damage projection");
   site.owner=1;check(new MapSceneSnapshot(g,custom,null,-1).items.get(0).color==FactionColors.color(custom,1),"new owner display");
  }
  custom.cities.remove(site);check(new MapSceneSnapshot(new MapSceneSnapshot.Ground(custom),custom,null,-1).items.isEmpty(),"delete removes model");
  // Diagonal pass: the open axis must follow the actual approach, not rounded cardinal sums.
  site=new World.City(90001,"custom",new Hex(12,12),0);site.kind=World.SiteKind.GATE;custom.cities.add(site);
  List<Hex> neighbors=site.hex.neighbors();for(Hex h:neighbors)custom.terrain[h.q][h.r]=World.Terrain.MOUNTAIN;
  Hex approach=neighbors.get(1);custom.terrain[approach.q][approach.r]=World.Terrain.ROAD;
  MapSceneSnapshot.Ground gateGround=new MapSceneSnapshot.Ground(custom);SiteVisual gate=new SiteVisual(custom,site,gateGround.grid);
  check(Math.abs(gate.yaw-Math.atan2(gateGround.grid.x(approach)-gateGround.grid.x(site.hex),gateGround.grid.z(approach)-gateGround.grid.z(site.hex)))<.0001,"diagonal gate follows open approach");
  List<String> facilityRows=new ArrayList<>();facilityRows.add("type\tlevel\tlod\tasset\tstates\tstatus");
  World all=SceneFacilityFixture.create();MapSceneSnapshot snapshot=new MapSceneSnapshot(new MapSceneSnapshot.Ground(all),all,null,-1);
  Set<String> mapped=new HashSet<>();
  for(MapSceneSnapshot.Item i:snapshot.items)if(i.facility!=null)for(int lod=0;lod<3;lod++){
   String id=FieldAssets.facility(i.facility,lod);asset("field",id);mapped.add(id);
   facilityRows.add(i.facility.type+"\t"+i.facility.level+"\t"+lod+"\t"+id+"\tconstruction/complete/damage/fire/deleted\tTRANSITIONAL_ART");
  }
  int expected=War.StructureKind.values().length;
  for(Domestic.Kind k:Domestic.Kind.values())expected+=Domestic.mergeable(k)?3:1;
  check(mapped.size()==expected*3,"all live enum type/level/LOD combinations");
  for(String state:new String[]{"scaffold","fire"})asset("field",state);
  Path out=Path.of("out/r07");Files.createDirectories(out);Files.write(out.resolve("site-mapping.tsv"),rows);Files.write(out.resolve("facility-mapping.tsv"),facilityRows);
  System.out.println("PASS R07 "+checks+" checks; "+sites+" catalog site instances, families="+families+", facility LOD mappings="+mapped.size());
 }
}
