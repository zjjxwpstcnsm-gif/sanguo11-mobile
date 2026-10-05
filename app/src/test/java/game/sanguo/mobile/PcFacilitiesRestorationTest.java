package game.sanguo.mobile;
import game.sanguo.core.*;
import java.nio.file.*;
import java.util.*;

public final class PcFacilitiesRestorationTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        PcFacilities assets=new PcFacilities(Files.newInputStream(Path.of("app/src/main/assets/3d/pc-facilities/facilities.pcz")));
        World w=ScenarioCatalog.load("heroes-250",0);byte[] before=SaveCodec.encode(w);var ground=new MapSceneSnapshot.Ground(w);
        Set<String> types=new LinkedHashSet<>();for(var kind:Domestic.Kind.values())types.add("domestic/"+kind.name());for(var kind:War.StructureKind.values())types.add("military/"+kind.name());
        for(String type:types)for(int level=1;level<=(type.startsWith("domestic/")?3:1);level++)for(int mode=0;mode<4;mode++){
            boolean complete=mode<2;int hp=mode==0?1000:mode==1?400:mode==2?500:100;
            var state=new MapSceneSnapshot.FacilityState(type,0,type.startsWith("domestic/")?level:0,0,hp,1000,complete?0:2,0,complete,false);
            var item=new MapSceneSnapshot.Item("source-facility",type,w.cities.get(0).hex,4,0xffffffff,state);
            check(assets.supports(ground,item),"all supported core facilities bind source geometry: "+type);
            for(int month:new int[]{1,4,7,10})for(int lod:new int[]{0,1,2}){
                SceneMesh m=assets.mesh(item,month,lod);check(m.pcFacility&&m.authoredTangentFrame,"source normals and source facility tag");
                check(m.uv.length==m.vertices.length/7*2&&m.indices.length>0,"complete original geometry");
                for(float uv:m.uv)check(uv>=0&&uv<=1,"original variable-size sheet bounds");
                for(int i:m.indices)check(i>=0&&i<m.vertices.length/7,"source triangle bounds");
            }
        }
        var farm=new MapSceneSnapshot.FacilityState("domestic/FARM",0,1,0,1000,1000,0,0,true,false);
        var item=new MapSceneSnapshot.Item("farm","farm",w.cities.get(0).hex,4,0xffffffff,farm);
        SceneMesh spring=assets.mesh(item,1,0),summer=assets.mesh(item,4,0),winter=assets.mesh(item,10,0);
        check(Arrays.equals(spring.vertices,summer.vertices)&&Arrays.equals(spring.indices,winter.indices),"farm seasonal appearance changes material, not source geometry");
        check(!Arrays.equals(spring.uv,summer.uv)&&!Arrays.equals(spring.uv,winter.uv),"four-season source farm sheets actually selected");
        w.mapRevision=63;check(!assets.supports(new MapSceneSnapshot.Ground(w),item),"legacy save uses explicit compatible asset path");w.mapRevision=NationalMap.REVISION;
        check(Arrays.equals(before,SaveCodec.encode(w)),"facility selection preserves entire authority, RNG and save bytes");
        for(String mode:new String[]{"domestic-build","military-build","domestic-damage","military-damage","domestic-destroy","military-destroy"}){
            var prepared=PcFacilitiesFixture.prepare(mode);World commandWorld=SaveCodec.decode(SaveCodec.encode(prepared.world()));
            var result=prepared.command(commandWorld);check(result.ok,"normal source-map command "+mode+": "+result.message);
            MapSceneSnapshot scene=new MapSceneSnapshot(new MapSceneSnapshot.Ground(commandWorld),commandWorld,null,-1);
            boolean found=false;for(var target:scene.items)if(target.hex.equals(prepared.target())&&target.facility!=null){
                found=true;check(assets.mesh(target,1,0).pcFacility,"normal command binds source body");
                check(PcFacilities.state(target.facility)==(mode.endsWith("build")?target.facility.type.startsWith("domestic/")?2:target.facility.hp<target.facility.maxHp*.3f?3:2:1),"normal command enters source construction/damage state");
            }
            check(found!=mode.endsWith("destroy"),"normal destruction removes facility and its render item");
            System.out.println("NORMAL_FACILITY "+mode+" "+result.message);
        }
        System.out.println("PASS PC facilities "+checks+" checks; core types="+types.size());
    }
}
