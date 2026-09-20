package game.sanguo.core;

import java.util.*;
import java.io.*;

/** Executable checks for v53; no presentation journal is attached in these tests. */
public final class Reports53Test {
    private static int checks;
    private static void require(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static List<BattleReports.Entry> all(World w){return w.reports.query(-1,w.player,BattleReports.Scope.ALL,null,"");}
    public static void main(String[] args)throws Exception{
        retention();ownership();roundTrip();turnOwnership();preview();facilities();
        System.out.println("REPORTS53 CORE PASS: "+checks+" checks");
    }
    private static void retention(){
        World w=Personnel49Fixture.world();w.reports.rebase();
        for(int t=0;t<12;t++){w.turn=t;for(int i=0;i<125;i++)w.note("旬"+t+"-行动"+i);}
        require(w.log.size()==40,"legacy HUD log remains bounded");require(all(w).size()==9*125,"nine complete turns retained, not forty messages");
        for(BattleReports.Entry e:all(w))require(e.turn>=3&&e.turn<=11,"only current and previous eight turns");
        require(w.reports.query(3,0,BattleReports.Scope.ALL,null,"").size()==125,"oldest retained turn complete");
        require(w.reports.query(2,0,BattleReports.Scope.ALL,null,"").isEmpty(),"expired turn removed");
        require(w.reports.query(11,0,BattleReports.Scope.RELATED,null,"行动124").size()==1,"combined own turn and keyword filter");
        int size=w.reports.size();w.reports.query(-1,0,BattleReports.Scope.ALL,null,"");require(size==w.reports.size(),"query does not generate reports");
    }
    private static void ownership()throws Exception{
        World w=Personnel49Fixture.world();World.Unit enemy=w.unit(1),own=w.unit(2);enemy.hex=new Hex(7,7);w.active=1;w.reports.rebase();
        require(w.turnJournal==null,"no animation recording needed");int before=own.troops;
        World.Result result=w.attack(enemy.id,own.id);require(result.ok,result.message);require(own.troops<before,"real enemy attack applied");
        List<BattleReports.Entry> incoming=w.reports.query(-1,0,BattleReports.Scope.RECEIVED,null,"");require(!incoming.isEmpty(),"enemy action affecting player appears in received reports");
        BattleReports.Entry entry=incoming.get(0);require(entry.actor==1&&entry.involves(0)&&entry.involves(1),"source and target ownership recorded");
        require(w.reports.query(-1,0,BattleReports.Scope.INITIATED,null,"").isEmpty(),"enemy attack is not a player initiated command");
        require(w.reports.query(-1,2,BattleReports.Scope.RELATED,null,"").isEmpty(),"unrelated faction not attributed");
        String frozen=entry.detail;enemy.owner=2;for(World.Officer o:w.army.crew(enemy))o.owner=2;w.reports.checkpoint("部队易主");
        require(entry.actor==1&&entry.detail.equals(frozen),"past report ownership and result remain frozen after defection");
        require(w.reports.query(-1,2,BattleReports.Scope.RELATED,null,"").size()>0,"new owner included in ownership transfer event");
    }
    private static void roundTrip()throws Exception{
        World w=Personnel49Fixture.world();w.reports.rebase();
        for(int t=0;t<3;t++){w.turn=t;for(int i=0;i<90;i++)w.note("实际结果 "+t+":"+i);}
        w.city(0).gold+=213;w.reports.checkpoint("内政收支");
        List<BattleReports.Entry> before=all(w);World loaded=SaveCodec.decode(SaveCodec.encode(w));List<BattleReports.Entry> after=all(loaded);
        require(before.size()==after.size(),"all report records persist through save");
        for(int i=0;i<before.size();i++){BattleReports.Entry a=before.get(i),b=after.get(i);require(a.id==b.id&&a.turn==b.turn&&a.related==b.related&&a.actor==b.actor&&a.detail.equals(b.detail),"save keeps exact event data");}
        long newest=after.get(0).id;loaded.note("新结果");require(all(loaded).get(0).id>newest,"IDs do not collide after loading");
        require(loaded.reports.query(0,0,BattleReports.Scope.RELATED,null,"").size()==90,"old turn filter preserved after loading");
        require(w.reports.query(-1,0,BattleReports.Scope.RELATED,BattleReports.Kind.ECONOMY,"+213").size()==1,"economic changes include actual before/after delta");
        byte[] encoded=SaveCodec.encode(loaded);encoded[encoded.length-5]^=1;boolean rejected=false;try{SaveCodec.decode(encoded);}catch(IOException expected){rejected=true;}require(rejected,"corrupt save rejected");
    }
    private static void turnOwnership(){
        World w=Personnel49Fixture.world();w.reports.rebase();w.turn=4;w.reports.beginTurn();w.reports.globalPhase();w.turn=5;
        w.city(0).food-=42;w.note("全局粮耗42");require(all(w).get(0).turn==4,"old turn settlement not mislabelled as new turn");
        w.reports.nextPlayer();w.note("新旬主动行动");require(all(w).get(0).turn==5,"new player command is in new turn");w.reports.endTurn();
        w.turn=8;require(w.reports.date(8).contains("3月下旬"),"calendar labels follow three turns per month");
    }
    private static void preview()throws Exception{
        World w=Personnel49Fixture.world();w.reports.rebase();World.City city=w.city(0);int[] deputies={4,5};
        byte[] before=SaveCodec.encode(w);long revision=w.commandRevision();
        World.Unit view=w.army.deploymentPreview(city,1,deputies,World.Weapon.SPEAR,Army.Ship.BOAT,3000,6000,100);
        require(view!=null,"deployment preview exists");double attack=w.combat.attackRating(view),defense=w.combat.defenseRating(view);int move=w.war.movement(view),range=w.war.range(view);
        for(int i=0;i<50;i++)w.army.deploymentPreview(city,1,deputies,World.Weapon.SPEAR,Army.Ship.BOAT,3000,6000,100);
        require(revision==w.commandRevision()&&Arrays.equals(before,SaveCodec.encode(w)),"preview does not mutate world, RNG, save or report log");
        World.Result result=w.army.deploy(city.id,1,deputies,World.Weapon.SPEAR,Army.Ship.BOAT,3000,6000,100);require(result.ok,result.message);World.Unit actual=w.unit(w.officer(1).unitId);
        require(view.hex.equals(actual.hex),"preview and real deployment use identical exit selection");require(attack==w.combat.attackRating(actual)&&defense==w.combat.defenseRating(actual),"exact attack and defense match");
        require(move==w.war.movement(actual)&&range==w.war.range(actual)&&view.energy==actual.energy,"movement, range and energy match");require(Arrays.equals(actual.deputies,deputies),"three officers deployed in selected roles");
        require(!w.reports.query(-1,0,BattleReports.Scope.INITIATED,null,"出征").isEmpty(),"actual sortie automatically logged without animation journal");
        World loaded=SaveCodec.decode(SaveCodec.encode(w));require(loaded.unit(actual.id).deputies.length==2,"formation and its reports survive save");
    }
    private static void facilities(){
        World w=Personnel49Fixture.world();World.Unit own=w.unit(2);War.Structure camp=new War.Structure(100,1,War.StructureKind.CAMP,new Hex(7,7),1100);w.war.structures.add(camp);w.reports.rebase();
        World.Result attack=w.war.attackStructure(own.id,camp.hex);require(attack.ok,attack.message);
        require(w.reports.query(-1,0,BattleReports.Scope.INITIATED,null,"攻击阵").size()==1,"original attack keeps player as initiator despite counterattack");
        List<BattleReports.Entry> counters=w.reports.query(-1,0,BattleReports.Scope.RECEIVED,null,"阵反击");require(counters.size()==1&&counters.get(0).actor==1,"facility counter is independently attributed to enemy");
        w.war.structures.clear();own.energy=40;w.war.structures.add(new War.Structure(101,0,War.StructureKind.MUSIC,new Hex(6,6),800));w.reports.rebase();w.energy.settleTurn();
        require(own.energy==50,"actual music recovery performed");require(w.reports.query(-1,0,BattleReports.Scope.INITIATED,null,"军乐台：气力+10").size()==1,"actual music gain recorded once without animation journal");
        w.war.structures.clear();w.war.structures.add(new War.Structure(102,1,War.StructureKind.ARROW_TOWER,new Hex(8,7),700));w.reports.rebase();w.fieldworks.towers();
        require(w.reports.query(-1,0,BattleReports.Scope.RECEIVED,null,"箭楼射击").size()==1,"enemy arrow tower result remains incoming and ownership-aware");
    }
}
