package game.sanguo.mobile;
import game.sanguo.core.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;

/** Exercises actual CPU meshes, rigid poses and authority. Not an APK/phone art test. */
public final class NativeFeedback122Test {
    static int checks;
    static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    static SceneMesh load(String path)throws Exception{try(InputStream in=Files.newInputStream(Path.of("app/src/main/assets/3d/"+path))){return SiteGlb.read(in);}}
    public static void main(String[] args)throws Exception{
        FieldAssets assets=new FieldAssets(name->Files.newInputStream(Path.of("app/src/main/assets/3d/field",name)));
        for(boolean staggered:new boolean[]{false,true}){
            World w=new World(24,24,"甲","乙");w.cities.add(new World.City(1,"test city",new Hex(2,2),0));w.columnStaggered=staggered;
            for(int q=0;q<24;q++)for(int r=0;r<24;r++)w.terrain[q][r]=World.Terrain.PLAIN;
            Hex wood=new Hex(12,12),rough=wood.neighbors().get(0);
            w.terrain[wood.q][wood.r]=World.Terrain.PLANK_ROAD;w.terrain[rough.q][rough.r]=World.Terrain.MOUNTAIN_PATH;
            Hex road=rough.neighbors().stream().filter(h->!h.equals(wood)&&!wood.neighbors().contains(h)).findFirst().orElseThrow();w.terrain[road.q][road.r]=World.Terrain.ROAD;
            byte[] before=SaveCodec.encode(w);MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(w);
            SceneMesh.TerrainWindow window=new SceneMesh.TerrainWindow(g.grid.x(wood),g.grid.z(wood),8,8,8);
            List<SceneMesh> meshes=Vegetation.buildWindow(g,Set.of(),List.of(),assets,window);
            int woodTriangles=0,soilTriangles=0;
            for(SceneMesh m:meshes)for(int i=0;i<m.indices.length;i+=3){
                float x=0,z=0,u=0;for(int j=0;j<3;j++){int v=m.indices[i+j];x+=m.vertices[v*7]/3;z+=m.vertices[v*7+2]/3;u+=m.uv[v*2]/3;}
                int panel=(int)(u*8);Hex h=g.grid.cell(x,z);
                if(panel==2){woodTriangles++;check(h.equals(wood),"wood never spreads into rough/plain road: "+h);}
                if(panel==7)soilTriangles++;
            }
            check(woodTriangles>0&&soilTriangles>0,"mixed edge has timber and earth halves");
            List<SceneMesh> again=Vegetation.buildWindow(g,Set.of(),meshes,assets,window);
            for(int i=0;i<meshes.size();i++)check(meshes.get(i)==again.get(i),"unchanged chunks reused");
            check(Arrays.equals(before,SaveCodec.encode(w)),"all terrain, save and RNG bytes unchanged");
            System.out.println("path staggered="+staggered+" timberTriangles="+woodTriangles+" soilTriangles="+soilTriangles);
            World marsh=new World(24,24,"甲","乙");for(int q=0;q<24;q++)for(int r=0;r<24;r++)marsh.terrain[q][r]=World.Terrain.SWAMP;
            MapSceneSnapshot.Ground mg=new MapSceneSnapshot.Ground(marsh);TerrainMaterialField field=new TerrainMaterialField(mg);
            float x=mg.grid.x(12,12),z=mg.grid.z(12,12);check(field.sample(x,z)[1]>.70,"actual marsh visibly mud dominated");check(field.groundTone(x,z)<.81,"actual marsh darker wet tone");check(!mg.surface.water(12,12),"marsh remains land, not navigable water");
        }
        String[] kinds={"SPEAR","HALBERD","CROSSBOW","CAVALRY","SWORD","RAM","SIEGE_TOWER","WOODEN_BEAST","CATAPULT","transport","BOAT","TOWER_SHIP","WARSHIP"};
        for(String kind:kinds)for(int lod=0;lod<2;lod++){
            String id="unit-"+kind+"-lod"+lod;SceneMesh rest=assets.mesh(id);
            check(rest.vertices.length/7<=30000,"runtime vertex budget");
            for(String clip:new String[]{"idle","walk","attack","hit","defeat"})for(int frame=0;frame<12;frame++){
                SceneMesh m=assets.pose(id,clip,frame,1);check(m.indices.length==rest.indices.length,"pose retains topology");
                for(float v:m.vertices)check(Float.isFinite(v),"every rigid pose finite");
            }
            System.out.println("model="+id+" triangles="+rest.indices.length/3);
        }
        World national=ScenarioCatalog.all().get(0);byte[] before=SaveCodec.encode(national);MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground(national);
        Set<String> families=new HashSet<>();int sites=0;
        for(World.City c:national.cities){SiteVisual v=new SiteVisual(national,c,g.grid);families.add(v.model);sites++;
            for(int lod=0;lod<3;lod++){SceneMesh model=load("sites/v122/"+v.model+"-lod"+lod+".glb");check(model.indices.length>0,"official site resolves real model");check(model.vertices.length/7<=30000,"site loader budget");
                if(c.kind==World.SiteKind.GATE){SceneMesh joined=SiteVisual.joinGate(model,g,c.hex,v.yaw);check(joined.indices.length>=model.indices.length,"original gate join retained");}
            }
        }
        check(families.size()==5,"all 5 site families used by actual national map");check(Arrays.equals(before,SaveCodec.encode(national)),"all national save and RNG bytes unchanged");
        int swamp=0,plank=0,rough=0;for(int t:g.terrain){if(t==World.Terrain.SWAMP.ordinal())swamp++;if(t==World.Terrain.PLANK_ROAD.ordinal())plank++;if(t==World.Terrain.MOUNTAIN_PATH.ordinal())rough++;}
        System.out.println("national sites="+sites+" swamp="+swamp+" plank="+plank+" rough="+rough+"; DATA RESTORATION NOT CLAIMED");
        System.out.println("PASS FEEDBACK122 checks="+checks);
    }
}
