package game.sanguo.core;

import java.util.*;

/** Fresh-game acceptance for the feature set recovered from v35–v40 and retained v41 balance. */
public final class Release42Test {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static World fixture(){
        World w=new World(24,16);w.cities.add(new World.City(10,"甲城",new Hex(3,6),0));w.cities.add(new World.City(20,"乙城",new Hex(19,6),1));w.cities.add(new World.City(11,"甲港",new Hex(5,6),0));w.city(11).kind=World.SiteKind.PORT;w.city(11).troops=0;w.city(11).food=0;w.city(11).gold=0;Arrays.fill(w.city(11).equipment,0);
        for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"甲将"+i,0,10,90,90,90,90,100));
        w.officers.add(new World.Officer(20,"乙君",1,20,70,70,70,70,70));w.officers.add(new World.Officer(21,"乙将",1,20,70,70,70,70,70));w.strategy.initializeOffices();w.city(20).gold=0;w.city(20).troops=0;return w;
    }
    private static void replay(World w,int turns)throws Exception{
        World copy=SaveCodec.decode(SaveCodec.encode(w));for(int t=0;t<turns;t++){check(w.nextTurn().ok,"fresh turn advances");check(copy.nextTurn().ok,"saved turn advances");check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(copy)),"mission/AI save replay deterministic");copy=SaveCodec.decode(SaveCodec.encode(copy));}
    }
    private static void travel()throws Exception{
        World w=fixture();int gold=w.city(10).gold;check(w.personnel.turns(10,11)==1,"same-region port takes one personnel turn");check(w.domestic.transfer(10,11,1).ok,"personnel dispatch");replay(w,1);check(w.officer(1).cityId==11&&w.city(10).gold==gold,"transfer arrives in one turn without a gold fee");
        w=fixture();gold=w.city(10).gold;check(w.campaign.goodwill(10,1,1).ok,"goodwill dispatch");check(w.strategy.factionRelation(0,1)==0&&w.city(10).gold==gold-500&&w.envoys.missions().size()==1,"pay once at departure; no instant remote diplomacy");replay(w,1);check(w.strategy.factionRelation(0,1)>0&&w.officer(1).otherTaskTurns>0,"resolve on arrival, actor remains on return journey");replay(w,1);check(w.envoys.missions().isEmpty()&&w.officer(1).otherTaskTurns==0&&w.city(10).gold==gold-500,"return unlocks officer without second charge");
        w=fixture();w.officer(21).loyalty=0;
        for(int seed=0;seed<100;seed++){w.strategy.setSeed(seed);World probe=SaveCodec.decode(SaveCodec.encode(w));probe.strategy.recruitOfficer(10,1,21);probe.nextTurn();if(probe.officer(21).owner==0)break;}
        check(w.strategy.recruitOfficer(10,1,21).ok,"remote recruitment dispatch");check(w.officer(21).owner==1,"target remains enemy during outward travel");replay(w,1);check(w.officer(21).owner==0&&w.officer(21).cityId==20&&w.officer(21).otherTaskTurns>0,"recruited officer actually returns from destination");replay(w,1);check(w.officer(21).cityId==10&&w.recruitment.missions().isEmpty(),"both officers complete return");
    }
    private static void portsAndCollapse()throws Exception{
        World w=fixture();check(w.army.deploy(10,1,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,3000,6000,200).ok,"deploy crew with equipment and cargo");World.Unit u=w.unit(w.officer(1).unitId);u.hex=new Hex(5,5);check(w.enter(u.id,11).ok,"enter friendly port");check(w.city(11).troops==3000&&w.city(11).equipment[0]==3000&&w.city(11).gold==200&&w.units.isEmpty(),"port garrison conserves stocks");w.officer(1).acted=false;w.city(11).gold=1000;check(!w.recruit(11,1).ok&&!w.produce(11,1,World.Weapon.SPEAR).ok,"port cannot recruit or manufacture");
        check(w.army.deploy(11,1,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,1000,1000,0).ok,"garrison may redeploy");w.city(10).owner=1;w.checkVictory();check(!w.alive(0)&&w.city(11).owner==-1&&w.units.isEmpty(),"final-city loss neutralizes ports and removes residual army");for(World.Officer o:w.officers)if(o.id<4)check(o.owner==-1&&o.unitId==-1&&o.role==Strategy.Role.UNAFFILIATED,"collapsed officers settle as unaffiliated");SaveCodec.validate(w);
    }
    private static void damAndGate()throws Exception{
        World w=fixture();Hex at=new Hex(10,8);w.terrain[at.q][at.r]=World.Terrain.DAM;War.Structure dam=new War.Structure(w.war.nextStructureId++,-1,War.StructureKind.DAM,at,1200);w.war.structures.add(dam);
        World.Officer o=w.officer(1);World.Unit u=new World.Unit(w.nextUnitId++,0,o.id,World.Weapon.SPEAR,new Hex(9,8),3000,6000);w.units.add(u);o.cityId=-1;o.unitId=u.id;w.fieldworks.destroy(dam);check(w.terrain[at.q][at.r]==World.Terrain.SHALLOWS&&w.terrainRevision==1&&u.troops==2400,"dam breach changes terrain and floods nearby troops once");w.fieldworks.destroy(dam);check(u.troops==2400,"dam cannot deal damage twice");SaveCodec.validate(w);
        World.City gate=new World.City(30,"敌关",new Hex(12,8),1);gate.kind=World.SiteKind.GATE;w.cities.add(gate);check(w.gateBlocks(0,new Hex(11,8),new Hex(12,7)),"hostile gate blocks adjacent bypass");gate.owner=0;check(!w.gateBlocks(0,new Hex(11,8),new Hex(12,7)),"friendly gate allows passage");
    }
    private static void content()throws Exception{
        for(Skill s:Skill.values())check(s.description!=null&&!s.description.isEmpty(),"skill has detailed text "+s.id);
        World w=TestScenarios.load("heroes-250",0);check(w.cities.size()==87&&w.sourceMapWidth==100,"restored national map has 42 cities, 10 gates and 35 ports");List<World.TurnProgress> progress=new ArrayList<>();check(w.nextTurn(progress::add).ok,"fresh national campaign actually advances");check(progress.size()>w.factions.length&&progress.get(progress.size()-1).completed==progress.get(progress.size()-1).total,"per-faction and phase progress completes");SaveCodec.validate(w);World copy=SaveCodec.decode(SaveCodec.encode(w));check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(copy)),"new national save roundtrips");
    }
    public static void main(String[] args)throws Exception{travel();portsAndCollapse();damAndGate();content();System.out.println("PASS: "+checks+" fresh v42 release checks: travel, mission timing/replay, ports, collapse, dam, gate, 100 skills and national turn.");}
}
