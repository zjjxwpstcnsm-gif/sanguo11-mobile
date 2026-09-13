package game.sanguo.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Behavioral tests for data packs, selectable factions, AI routing and save migrations. */
public final class ScenarioTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static String resource(String path)throws IOException {
        try(InputStream in=ScenarioTest.class.getResourceAsStream(path)) {
            if(in==null)throw new IOException("Missing fixture "+path);
            return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static World parse(String data,int player)throws IOException {return ScenarioData.read(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)),player);}
    private static void bad(String data,int player,String reason) {
        try{parse(data,player);throw new AssertionError("Accepted "+reason);}catch(IOException expected){checks++;}
    }
    private static void invalidSave(World world,String reason) {
        try{SaveCodec.encode(world);throw new AssertionError("Accepted "+reason);}catch(IOException expected){checks++;}
    }
    public static void main(String[] args)throws Exception {
        data();migration();factions();routing();campaigns();
        System.out.println("PASS: "+checks+" scenario assertions covering malformed data, faction turns, migration, AI detours, and multi-faction campaigns.");
    }
    private static void data()throws Exception {
        List<World> packs=ScenarioCatalog.all();check(packs.size()==2,"two bundled packs");
        World w=ScenarioCatalog.load("regional-sandbox",2);
        check(w.cities.size()==9&&w.officers.size()==18&&w.factions.length==3,"sandbox content loaded");
        check(w.player==2&&w.active==2&&w.home().owner==2,"selected faction controls own city");
        check(w.city(300)!=null&&w.officer(3005)!=null,"stable non-index IDs preserved");
        check(w.dataHash.matches("[0-9a-f]{64}"),"raw byte digest retained");
        World second=ScenarioCatalog.load("regional-sandbox",2);w.city(300).food=1;
        check(second.city(300).food==40000,"new games have independent mutable state");
        String text=resource("/scenarios/regional-sandbox.properties");
        bad(text.replace("format=1","format=9"),0,"future format");
        bad(text+"width=28\n",0,"duplicate key");
        bad(text+"unrecognized=3\n",0,"unknown key");
        bad(text.replace("width=28","width=129"),0,"oversized map");
        bad(text.replace("year=208","year=NaN"),0,"invalid number");
        bad(text.replace("source=engineering-original","source=original-verified"),0,"unsupported source claim");
        bad(text.replace("city.1=110|","city.1=100|"),0,"duplicate city ID");
        bad(text.replace("officer.0=1000|刘备|0|100|","officer.0=1000|刘备|0|300|"),0,"foreign city assignment");
        bad(text.replace("officer.1=1001|","officer.1=1000|"),0,"duplicate officer ID");
        bad(text.replace("faction.2=孙权军\n",""),0,"missing faction");
        bad(text.replace("faction.2=孙权军","faction.2=曹操军"),0,"duplicate faction name");
        bad(text,3,"player outside factions");bad(text,-1,"negative player");
        bad(text.replaceFirst("terrain.0=.","terrain.0=X"),0,"unknown terrain");
        bad(text.replaceFirst("terrain.0=.","terrain.0="),0,"short terrain row");
        bad(text.replace("|孙权|2|300|","|孙权|1|300|"),0,"officer owner mismatch");
        String isolated=text;
        for(int r=0;r<w.height;r++)if(r!=13)isolated=isolated.replaceFirst("terrain\\."+r+"=[PFMW]+","terrain."+r+"="+"M".repeat(w.width));
        bad(isolated,0,"blocked city tiles");
        String river=text.replaceFirst("terrain.13=[PFMW]+","terrain.13="+"W".repeat(w.width));
        bad(river,0,"disconnected cities");
        try{ScenarioCatalog.load("../regional-sandbox",0);throw new AssertionError("path traversal accepted");}catch(IOException expected){checks++;}
        bad(text+"#".repeat(1024*1024),0,"oversized input");
        World snapshot=ScenarioCatalog.load("regional-sandbox",1);snapshot.dataHash="xyz";invalidSave(snapshot,"bad data hash");
        snapshot=ScenarioCatalog.load("regional-sandbox",1);snapshot.active=3;invalidSave(snapshot,"bad active faction");
        snapshot=ScenarioCatalog.load("regional-sandbox",1);snapshot.winner=0;invalidSave(snapshot,"premature winner");
    }
    private static void migration()throws Exception {
        byte[] old=Base64.getDecoder().decode(resource("/m0-v1.sg11.b64").trim());
        World w=SaveCodec.decode(old);
        check(w.turn==1&&w.player==0&&w.active==0,"actual v1 fixture migrates turn and player");
        check(w.cities.size()==3&&w.officers.size()==6&&w.factions.length==2,"v1 faction defaults migrate");
        check(w.unit(1).weapon==World.Weapon.CROSSBOW&&w.unit(1).food==5850,"v1 army and supply preserved");
        check(w.officer(0).unitId==1&&w.city(0).equipment[2]==9000,"v1 references and stocks preserved");
        check(w.dataHash.isEmpty()&&w.scenarioId.equals("m0-skirmish"),"legacy save has no fabricated data fingerprint");
        byte[] migrated=SaveCodec.encode(w);check(migrated[7]==4,"new writes use save v4");
        World restored=SaveCodec.decode(migrated);w.nextTurn();restored.nextTurn();
        check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"migrated games continue identically");
        w=ScenarioCatalog.load("regional-sandbox",2);w.nextTurn();byte[] saved=SaveCodec.encode(w);restored=SaveCodec.decode(saved);
        check(restored.player==2&&restored.factions[2].equals("孙权军"),"v2 selected player round trip");
        check(restored.scenarioId.equals(w.scenarioId)&&restored.dataHash.equals(w.dataHash)&&restored.startMonth==9,"v2 scenario identity and date preserved");
        check(Arrays.equals(saved,SaveCodec.encode(restored)),"v2 all fields round trip");
        w.nextTurn();restored.nextTurn();check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"v2 deterministic continuation");
        w.turn=12;check(w.date().equals("209年 1月 上旬"),"non-January scenario date crosses year");
        w.scenarioId="retired-pack";w.scenarioName="旧数据包";
        check(SaveCodec.decode(SaveCodec.encode(w)).scenarioId.equals("retired-pack"),"saves do not require installed scenario data");
    }
    // This fixture deletes cities outright; remove dependent unrevealed records as well.
    private static void dropMissingTalents(World w) { w.strategy.talents.removeIf(t->w.city(t.cityId)==null); }
    private static void factions()throws Exception {
        for(int player=0;player<3;player++) {
            World w=ScenarioCatalog.load("regional-sandbox",player);World.City home=w.home();World.Officer o=w.idle(home).get(0);
            check(w.train(home.id,o.id).ok,"chosen faction can command own city "+player);
            World.City enemy=null;for(World.City c:w.cities)if(c.owner>=0&&c.owner!=player){enemy=c;break;}
            byte[] before=SaveCodec.encode(w);
            check(!w.recruit(enemy.id,w.officers.stream().filter(x->x.owner!=w.player).findFirst().get().id).ok,"foreign orders rejected "+player);
            check(Arrays.equals(before,SaveCodec.encode(w)),"foreign order leaves state unchanged "+player);
            check(w.nextTurn().ok&&w.turn==1&&w.active==player,"turn returns to chosen player "+player);
            check(!o.acted&&w.actionPoints[player]==60,"chosen faction reset "+player);
            for(int side=0;side<3;side++){final int owner=side;long count=w.units.stream().filter(u->u.owner==owner).count();check(side==player?count==0:count==2,"each AI acts once, human not automated");}
            SaveCodec.validate(w);
        }
        World w=ScenarioCatalog.load("regional-sandbox",0);
        w.cities.removeIf(c->c.owner==2);w.officers.removeIf(o->o.owner==2);dropMissingTalents(w);w.checkVictory();
        check(w.winner==-1&&!w.gameOver(),"eliminating one of three forces is not victory");
        check(w.nextTurn().ok&&w.active==0,"eliminated force is skipped");
        w=ScenarioCatalog.load("regional-sandbox",0);w.cities.removeIf(c->c.owner==0);w.officers.removeIf(o->o.owner==0);dropMissingTalents(w);w.checkVictory();
        check(w.gameOver()&&w.winner==-1,"human loss with two AI forces remaining has no fabricated victor");
        byte[] before=SaveCodec.encode(w);check(!w.nextTurn().ok&&Arrays.equals(before,SaveCodec.encode(w)),"defeat freezes commands and state");
        w=ScenarioCatalog.load("regional-sandbox",0);w.deploy(100,1000,World.Weapon.SPEAR,3000);
        w.cities.removeIf(c->c.owner==0);w.officers.removeIf(o->o.owner==0&&o.unitId<0);dropMissingTalents(w);w.checkVictory();
        check(!w.gameOver(),"landless army can continue fighting");SaveCodec.validate(w);
        w=ScenarioCatalog.load("regional-sandbox",2);w.cities.removeIf(c->c.owner==0||c.owner==1);w.officers.removeIf(o->o.owner!=2);dropMissingTalents(w);w.checkVictory();
        check(w.winner==2&&w.gameOver(),"third faction can win");SaveCodec.validate(w);
    }
    private static void routing()throws Exception {
        World w=new World(13,11);w.cities.add(new World.City(0,"玩家城",new Hex(3,5),0));w.cities.add(new World.City(1,"电脑城",new Hex(10,5),1));
        w.officers.add(new World.Officer(0,"守将",0,0,75,70,70,70,70));w.officers.add(new World.Officer(1,"进攻将",1,1,75,70,70,70,70));
        for(int r=1;r<11;r++)w.terrain[6][r]=World.Terrain.MOUNTAIN;
        w.active=1;check(w.deploy(1,1,World.Weapon.SPEAR,3000).ok,"detour fixture deployment");w.unit(1).hex=new Hex(7,5);w.active=0;
        int defense=w.city(0).defense;
        for(int turn=0;turn<14&&!w.gameOver();turn++){check(w.nextTurn().ok,"detour turn "+turn);SaveCodec.validate(w);}
        check(w.city(0).defense<defense||w.city(0).owner==1,"AI takes long detour and reaches enemy city");
        w=ScenarioCatalog.load("regional-sandbox",0);w.deploy(100,1000,World.Weapon.SPEAR,3000);
        World.Unit unit=w.unit(1);unit.hex=new Hex(15,6);w.city(200).defense=1;
        check(w.siege(unit.id,200).ok&&w.officer(2000).cityId==210,"captured officers retreat to surviving friendly city");SaveCodec.validate(w);
    }
    private static void campaigns()throws Exception {
        for(int player=0;player<3;player++) {
            World a=ScenarioCatalog.load("regional-sandbox",player),b=ScenarioCatalog.load("regional-sandbox",player);
            for(int turn=0;turn<90&&!a.gameOver();turn++) {
                playerOrders(a);playerOrders(b);a.nextTurn();b.nextTurn();SaveCodec.validate(a);
                check(Arrays.equals(SaveCodec.encode(a),SaveCodec.encode(b)),"deterministic campaign side "+player+" turn "+turn);
                if(turn%7==0)b=SaveCodec.decode(SaveCodec.encode(b));
            }
        }
    }
    private static void playerOrders(World w) {
        for(World.City c:w.cities)if(c.owner==w.player) {
            List<World.Officer> available=w.idle(c);
            if(!available.isEmpty())w.deploy(c.id,available.get(0).id,World.Weapon.CROSSBOW,3000);
        }
        for(World.Unit u:new ArrayList<>(w.units))if(u.owner==w.player&&!u.acted) {
            for(World.Unit enemy:new ArrayList<>(w.units))if(enemy.owner!=w.player&&u.hex.distance(enemy.hex)<=u.weapon.range){w.attack(u.id,enemy.id);break;}
            if(!u.acted)for(World.City c:w.cities)if(c.owner!=w.player&&u.hex.distance(c.hex)==1){w.siege(u.id,c.id);break;}
        }
    }
}
