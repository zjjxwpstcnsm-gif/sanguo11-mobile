package game.sanguo.mobile;
import game.sanguo.core.*;
import java.nio.file.*;
import java.util.*;
public final class PcSitesRestorationTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        PcSites assets=new PcSites(Files.newInputStream(Path.of("app/src/main/assets/3d/pc-sites/sites.pcz")));
        // Independent original41bae0 and41bbb0, not copied renderer formulas.
        List<String> oracle=Files.readAllLines(Path.of("app/src/test/fixtures/native-v155/site-resolver.tsv"));
        check(oracle.size()==1057,"all1056 original site climate/state/quarter/LOD cases");
        for(String row:oracle.subList(1,oracle.size())){
            int[] v=Arrays.stream(row.split("\t")).mapToInt(Integer::parseInt).toArray();
            check(assets.modelIndex(v[0],v[3],v[4])==v[5],"original site model "+row);
            check(PcSites.textureDelta(v[0],v[1]*3+1,v[2])==v[6],"original geographic winter texture "+row);
        }
        World w=ScenarioCatalog.load("heroes-250",0);byte[] before=SaveCodec.encode(w);
        var g=new MapSceneSnapshot.Ground(w);int matched=0;
        for(var item:new MapSceneSnapshot(g,w,null,-1).items)if(item.site!=null){
            var p=assets.placement(g,item);check(p!=null,"source placement "+item.key+" "+item.label+" "+item.hex);matched++;
            check(Math.hypot(g.grid.x(item.hex)-p.x(),g.grid.z(item.hex)-p.z())<1.13,"documented source/grid centre agreement incl inherited Yangping offset");
            for(int month:new int[]{1,4,7,10})for(int lod:new int[]{0,1,2}){
                SceneMesh m=assets.mesh(p,item.site,month,lod);check(m.pcSite&&m.authoredTangentFrame,"native model/normal tag");
                check(m.uv.length==m.vertices.length/7*2&&m.indices.length>0,"geometry complete");
                for(float uv:m.uv)check(uv>=0&&uv<=1,"source sheet UV bounds");
                for(int i:m.indices)check(i>=0&&i<m.vertices.length/7,"index bounds");
            }
        }
        check(matched==87,"all source sites bound");
        check(Arrays.equals(before,SaveCodec.encode(w)),"source site reads preserve gameplay/RNG/save");
        for(World crop:ScenarioCatalog.all()){
            var cropGround=new MapSceneSnapshot.Ground(crop);
            for(var item:new MapSceneSnapshot(cropGround,crop,null,-1).items)if(item.site!=null){
                var p=assets.placement(cropGround,item);
                check(cropGround.pcMap==null?p==null:p!=null,"source origin/crop and explicit legacy site separation");
            }
        }
        w.mapRevision=63;var legacyGround=new MapSceneSnapshot.Ground(w);
        for(var item:new MapSceneSnapshot(legacyGround,w,null,-1).items)if(item.site!=null)check(assets.placement(legacyGround,item)==null,"old save keeps compatibility geometry");
        w.mapRevision=NationalMap.REVISION;
        check(Arrays.equals(before,SaveCodec.encode(w)),"compatibility checks preserve complete save");
        for(int wantedKind:new int[]{0,2,1}){
            World commandWorld=ScenarioCatalog.load("heroes-250",0);
            World.City target=null;
            for(World.City c:commandWorld.cities)if((c.kind==World.SiteKind.CITY?0:c.kind==World.SiteKind.GATE?2:1)==wantedKind&&c.owner>0){target=c;break;}
            check(target!=null,"source kind enemy fixture");
            target.defense=wantedKind==0?1010:510;target.troops=12000;
            World.Officer leader=null;for(World.Officer o:commandWorld.officers)if(o.owner==0){leader=o;break;}
            check(leader!=null,"legal faction commander");
            Hex approach=null;Set<Hex> candidates=new LinkedHashSet<>();for(Hex h:SiteFootprint.cells(target))candidates.addAll(h.neighbors());
            for(Hex h:candidates)if(commandWorld.inside(h)&&commandWorld.cityAt(h)==null&&!commandWorld.army.water(h)&&commandWorld.terrain[h.q][h.r]!=World.Terrain.MOUNTAIN){approach=h;break;}
            check(approach!=null,"source land approach");
            World.Unit attacker=new World.Unit(commandWorld.nextUnitId++,0,leader.id,World.Weapon.SWORD,approach,5000,10000);
            commandWorld.units.add(attacker);leader.unitId=attacker.id;leader.cityId=-1;
            SiteVisual oldState=new SiteVisual(commandWorld,target,new GridWorldTransform(99,true));
            World.Result result=commandWorld.siege(attacker.id,target.id);check(result.ok,"normal siege succeeds: "+result.message);
            SiteVisual newState=new SiteVisual(commandWorld,target,new GridWorldTransform(99,true));
            check(target.defense>0&&target.owner!=0,"fixture remains damaged enemy site");
            check(wantedKind==0?oldState.sourceWalls==0&&newState.sourceWalls==1:oldState.sourceBuildings==0&&newState.sourceBuildings==1,"real siege crosses source model threshold");
            MapSceneSnapshot scene=new MapSceneSnapshot(new MapSceneSnapshot.Ground(commandWorld),commandWorld,null,-1);
            for(var item:scene.items)if(item.key.equals("site:"+target.id))check(assets.mesh(assets.placement(scene.ground,item),item.site,1,0).pcSite,"normal siege state binds original geometry");
            System.out.println("NORMAL_SIEGE kind="+wantedKind+" defense="+target.defense+" "+result.message);
        }
        System.out.println("PASS PC sites "+checks+" checks; matched="+matched);
    }
}
