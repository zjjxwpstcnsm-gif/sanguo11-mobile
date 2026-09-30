package game.sanguo.mobile;

import game.sanguo.core.*;
import org.json.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Current catalog traversal and asset/anchor coverage, explicitly separate from visual acceptance. */
public final class NativeR15Test {
    static int checks;static JSONArray maps=new JSONArray(),sites=new JSONArray(),routes=new JSONArray(),errors=new JSONArray(),assets=new JSONArray();
    static Set<String> checkedAssets=new TreeSet<>(),geometry=new HashSet<>();
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static String hash(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
    static void asset(String family,String model)throws Exception{
        String path="app/src/main/assets/3d/"+family+"/"+model+".glb";if(!checkedAssets.add(path))return;
        try(var in=Files.newInputStream(Path.of(path))){SceneMesh m=SiteGlb.read(in);
            check(m.indices.length/3<=15000,"per-model initial triangle budget: "+model);
            assets.put(new JSONObject().put("path",path).put("sha256",hash(Files.readAllBytes(Path.of(path)))).put("triangles",m.indices.length/3).put("primitives",1).put("mapping","PASS").put("art","NOT_ACCEPTED"));}
    }
    static void audit(World w,String label,boolean persistent)throws Exception{
        byte[] before=persistent?SaveCodec.encode(w):null;
        MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);TerrainMaterialField field=new TerrainMaterialField(g);
        Map<String,Integer> counts=new TreeMap<>();int valid=0;
        for(int r=0;r<w.height;r++)for(int q=0;q<w.width;q++){
            Hex h=new Hex(q,r);if(!g.valid(h))continue;valid++;counts.merge(w.terrain[q][r].name(),1,Integer::sum);
            float x=g.grid.x(h),z=g.grid.z(h);check(g.grid.cell(x,z).equals(h),label+" center roundtrip "+h);
            float y=g.surface.at(h);check(Float.isFinite(y)&&y>=0&&y<=TerrainSurface.MAX_HEIGHT,"bounded height");
            if(g.surface.water(h)||g.bases.contains(h))check(y==0,"hard water/site plane");
            // Full center coverage; view-dependent normals/slopes are tested by R03/R04.
            float[] weight=field.sample(x,z);float sum=0;for(float f:weight){check(Float.isFinite(f)&&f>=0,"finite material");sum+=f;}check(Math.abs(sum-1)<.00001,"normalized material");
        }
        String key=hash(g.terrain)+":"+w.width+":"+w.height+":"+g.grid.offset+":"+g.grid.staggered;
        maps.put(new JSONObject().put("input",label).put("mapId",w.mapId).put("width",w.width).put("height",w.height).put("sourceWidth",w.sourceMapWidth).put("sourceHeight",w.sourceMapHeight).put("sourceOriginX",w.sourceOriginX).put("sourceOriginY",w.sourceOriginY).put("validCells",valid).put("terrainCounts",counts).put("geometryKey",key).put("siteCount",w.cities.size()).put("unitCount",w.units.size()));
        for(World.City c:w.cities){SiteVisual v=new SiteVisual(w,c,g.grid);int wet=0,dry=0;
            for(Hex h:c.hex.neighbors())if(w.inside(h)){if(SiteVisual.navigable(w.terrain[h.q][h.r]))wet++;else if(w.terrain[h.q][h.r]!=World.Terrain.MOUNTAIN&&w.terrain[h.q][h.r]!=World.Terrain.NON_NAVIGABLE_WATER&&w.terrain[h.q][h.r]!=World.Terrain.SHALLOWS)dry++;}
            for(Hex h:v.cells){check(g.valid(h),"valid footprint "+label+"/"+c.id);check(w.cityAt(h)==c,"footprint identity");check(g.surface.at(h)==0,"site seated at shared height");}
            check(Float.isFinite(v.yaw)&&Float.isFinite(v.scale),"finite placement");
            for(int lod=0;lod<3;lod++)asset("sites",v.model+"-lod"+lod);
            if(c.kind==World.SiteKind.PORT&&(wet==0||dry==0))errors.put(new JSONObject().put("input",label).put("site",c.id).put("error","PORT_CONTACT_MISSING").put("wet",wet).put("dry",dry));
            sites.put(new JSONObject().put("input",label).put("id",c.id).put("name",c.name).put("kind",c.kind).put("q",c.hex.q).put("r",c.hex.r).put("family",v.model).put("yaw",v.yaw).put("scale",v.scale).put("footprint",v.cells.toString()).put("navigableNeighbors",wet).put("landNeighbors",dry).put("anchor","PASS_HEIGHT_AND_MAPPING_ONLY").put("visualContact","NOT_RUN"));
        }
        if(geometry.add(key)){
            for(R15TourPlan.Stop stop:R15TourPlan.regions(w))routes.put(new JSONObject().put("input",label).put("id",stop.id).put("reason",stop.reason).put("q",stop.hex.q).put("r",stop.hex.r).put("spans",new JSONArray(new int[]{5,14,30})).put("yaws",new JSONArray(new int[]{0,90})).put("tilt",48).put("runtime","NOT_RUN").put("art","NOT_ACCEPTED"));
        }
        MapSceneSnapshot snap=new MapSceneSnapshot(g,w,null,-1);
        for(MapSceneSnapshot.Item item:snap.items)for(int lod=0;lod<3;lod++){
            if(item.facility!=null)asset("field",FieldAssets.facility(item.facility,lod));
            if(item.unit!=null)asset("field",FieldAssets.unit(item.unit,item.unit.naval,lod));
        }
        if(persistent){check(Arrays.equals(before,SaveCodec.encode(w)),"audit preserves exact authority/RNG");World restored=SaveCodec.decode(before);check(Arrays.equals(g.terrain,new MapSceneSnapshot.Ground(restored).terrain),"saved map restored");}
        System.out.println("R15 "+label+" valid="+valid+" sites="+w.cities.size()+" geometry="+key);
    }
    static void roadPalette(){
        for(boolean staggered:new boolean[]{false,true}){
            World plain=new World(18,16),roads=new World(18,16);plain.columnStaggered=roads.columnStaggered=staggered;
            for(int q=0;q<18;q++)for(int r=0;r<16;r++){
                World.Terrain t=q<3?World.Terrain.SAND:r<3?World.Terrain.MOUNTAIN:r>12?World.Terrain.FOREST:World.Terrain.PLAIN;
                plain.terrain[q][r]=t;roads.terrain[q][r]=t==World.Terrain.PLAIN&&(q+r)%2==0?World.Terrain.ROAD:t;
            }
            MapSceneSnapshot.Ground a=new MapSceneSnapshot.Ground(plain),b=new MapSceneSnapshot.Ground(roads);
            TerrainMaterialField af=new TerrainMaterialField(a),bf=new TerrainMaterialField(b);
            for(int q=2;q<16;q++)for(int r=2;r<14;r++)for(float dx:new float[]{-.37f,0,.41f}){
                float x=a.grid.x(q,r)+dx,z=a.grid.z(q,r)+.21f;
                check(Arrays.equals(af.sample(x,z,.3f),bf.sample(x,z,.3f)),"ordinary ROAD must match PLAIN palette at identical world coordinates/slope");
            }
            roads.terrain[8][8]=World.Terrain.PLANK_ROAD;
            TerrainMaterialField path=new TerrainMaterialField(new MapSceneSnapshot.Ground(roads));
            check(!Arrays.equals(af.sample(a.grid.x(8,8),a.grid.z(8,8)),path.sample(a.grid.x(8,8),a.grid.z(8,8))),"explicit plank path stays distinct");
        }
    }
    public static void main(String[] args)throws Exception{
        roadPalette();if(args.length>0&&args[0].equals("road")){System.out.println("PASS R15 road palette "+checks);return;}
        for(ScenarioCatalog.Summary s:ScenarioCatalog.summaries())audit(ScenarioCatalog.load(s.id,0),s.id,true);
        // Representative custom layout fixtures, not user-authored content or a new official map.
        for(boolean staggered:new boolean[]{false,true}){
            World w=new World(31,19,"甲","乙");w.columnStaggered=staggered;
            for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++)w.terrain[q][r]=q<2||r<2?World.Terrain.VOID:q>23?World.Terrain.WATER:q<7?World.Terrain.SAND:r>14?World.Terrain.MOUNTAIN:r<6?World.Terrain.FOREST:World.Terrain.PLAIN;
            World.City c=new World.City(90001,"自定义城",new Hex(15,9),0);w.cities.add(c);
            World.City p=new World.City(90002,"自定义港",new Hex(23,9),0);p.kind=World.SiteKind.PORT;w.cities.add(p);
            audit(w,"custom-layout-"+staggered,false);
        }
        World facilities=SceneFacilityFixture.create();for(MapSceneSnapshot.Item i:new MapSceneSnapshot(new MapSceneSnapshot.Ground(facilities),facilities,null,-1).items)if(i.facility!=null)for(int lod=0;lod<3;lod++)asset("field",FieldAssets.facility(i.facility,lod));
        for(String kind:UnitR10Fixture.KINDS){World w=UnitR10Fixture.moving(kind,false);UnitVisual u=new UnitVisual(w,UnitR10Fixture.actor(w));for(int lod=0;lod<3;lod++)asset("field",FieldAssets.unit(u,u.naval,lod));}
        Path out=Path.of("out/r15");Files.createDirectories(out);
        Files.writeString(out.resolve("coverage.json"),new JSONObject().put("schema",1).put("checks",checks).put("maps",maps).put("sites",sites).put("assets",assets).put("errors",errors).put("visualStatus","V2_FAIL; V3_NOT_ACCEPTED").toString(2));
        Files.writeString(out.resolve("tour-plan.json"),routes.toString(2));
        check(errors.length()==0,"unclosed anchor errors: "+errors);
        System.out.println("PASS R15 host "+checks+" checks; inputs="+maps.length()+" mapShapes="+geometry.size()+" siteInstances="+sites.length()+" assets="+assets.length()+" plannedRegions="+routes.length()+"; ART NOT_ACCEPTED");
    }
}
