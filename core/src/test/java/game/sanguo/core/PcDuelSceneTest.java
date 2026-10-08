package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
public final class PcDuelSceneTest {
    static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static int n(Map<String,Object>r,String key){return ((Number)r.get(key)).intValue();}
    public static void main(String[]args)throws Exception {
        var receipt=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[0]))));int count=0;
        for(Object row:MapJson.array(receipt.get("rows"))){var r=MapJson.object(row);int kind=n(r,"kind"),gap=n(r,"gap");boolean forest=Boolean.TRUE.equals(r.get("forest"));boolean near=(kind==0||kind==1||kind==4||kind==5)&&gap>0&&gap<=2;check(PcDuelEntryRules.scene(near,forest,false)==n(r,"scene"),"original full scene fixture "+count++);}
        check(count==144,"all original kinds/completion/ring/terrain combinations");
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);Hex target=null;outer:for(int q=4;q<w.width-4;q++)for(int r=4;r<w.height-4;r++){Hex h=new Hex(q,r);boolean distant=true;for(var city:w.cities)distant&=city.hex.distance(h)>5;if(distant&&!w.army.water(h)&&w.cost(h,World.Weapon.SWORD)>0){target=h;break outer;}}check(target!=null,"isolated current saved scene fixture");
        // Explicit authored fixture terrain: actual World rules, not a renderer.
        for(Hex h:target.neighbors())w.terrain[h.q][h.r]=World.Terrain.PLAIN;w.terrain[target.q][target.r]=World.Terrain.PLAIN;
        check(PcDuelEntryRules.currentScene(w,target)==1,"plain current scene");Hex neighbor=target.neighbors().get(0);w.terrain[neighbor.q][neighbor.r]=World.Terrain.FOREST;check(PcDuelEntryRules.currentScene(w,target)==2,"adjacent forest original ring1");
        var structure=new War.Structure(w.war.nextStructureId++,2,War.StructureKind.FORT,neighbor,1100);structure.complete=false;w.war.structures.add(structure);check(PcDuelEntryRules.currentScene(w,target)==0,"unfinished fort takes original precedence");structure.kind=War.StructureKind.CAMP;check(PcDuelEntryRules.currentScene(w,target)==2,"native camp3 does not use fort scene");structure.kind=War.StructureKind.FORT;structure.hp=0;check(PcDuelEntryRules.currentScene(w,target)==2,"invalid destroyed fort omitted");
        w.war.structures.clear();w.terrain[neighbor.q][neighbor.r]=World.Terrain.PLAIN;byte[]saved=SaveCodec.encode(w);World cold=SaveCodec.decode(saved);check(PcDuelEntryRules.currentScene(cold,target)==1&&Arrays.equals(saved,SaveCodec.encode(cold)),"scene query full saved World/RNG pure");
        System.out.println("PASS original current scene "+checks+" checks; normal challenge action/AP/camera strategy and APK remain pending");
    }
}
