package game.sanguo.core;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;

/** End-to-end formation / production / water / siege / cargo invariants, including real v5 migration. */
public final class ArmyTest {
    private static int checks,cases;
    private static void check(boolean valid,String message){checks++;if(!valid)throw new AssertionError(message);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static void reject(World w,Supplier<World.Result> command)throws Exception{byte[] before=bytes(w);check(!command.get().ok,"invalid command rejected");check(Arrays.equals(before,bytes(w)),"invalid command is fully atomic incl RNG");}
    private static World fixture(){
        World w=new World(22,16);w.cities.add(new World.City(10,"甲城",new Hex(5,5),0));w.cities.add(new World.City(11,"乙城",new Hex(3,11),0));w.cities.add(new World.City(20,"敌城",new Hex(18,5),1));
        for(int i=0;i<8;i++)w.officers.add(new World.Officer(i,"将"+i,0,10,70+i,70+i,70+i,85,85));
        w.officers.add(new World.Officer(20,"敌将",1,20,80,80,80,80,80));
        w.city(10).gold=30000;w.city(10).food=200000;w.city(10).troops=30000;w.city(20).gold=0;w.city(20).troops=0;
        for(int r=0;r<w.height;r++){w.terrain[9][r]=World.Terrain.WATER;w.terrain[10][r]=World.Terrain.WATER;}
        for(int i=5;i<9;i++)w.city(10).equipment[i]=2;w.city(10).ships[0]=2;w.city(10).ships[1]=2;w.strategy.initializeOffices();return w;
    }
    private static World.Unit unit(World w,int id,int owner,int officer,World.Weapon weapon,Hex h){World.Unit u=new World.Unit(id,owner,officer,weapon,h,5000,20000);w.units.add(u);w.nextUnitId=Math.max(w.nextUnitId,id+1);World.Officer o=w.officer(officer);w.strategy.releaseGovernor(o.id);o.unitId=id;o.cityId=-1;return u;}
    private static void reset(World w){w.actionPoints[0]=60;for(World.Officer o:w.officers)if(o.owner==0)o.acted=false;for(World.Unit u:w.units)u.acted=false;}
    private static Domestic.Facility factory(World w,Domestic.Kind kind){Hex h=new Hex(6,4);if(kind==Domestic.Kind.SHIPYARD){h=new Hex(7,5);w.terrain[8][5]=World.Terrain.WATER;}Domestic.Facility f=new Domestic.Facility(w.domestic.nextFacilityId++,10,kind,h,-1,0);w.domestic.facilities.add(f);return f;}
    public static void main(String[] args)throws Exception{
        formations();equipment();water();combat();manufacturing();cargo();saves();scenarioData();simulation();
        System.out.println("PASS: "+cases+" army cases, "+checks+" assertions covering crew locks, equipment conservation, water transitions, siege/naval combat, manufacturing, cargo, save v1-v6 and deterministic AI.");
    }
    private static void formations()throws Exception{
        World w=fixture();w.officer(1).intelligence=99;w.officer(2).war=98;w.officer(2).aptitude[0]=3;
        reject(w,()->w.army.deploy(10,0,new int[]{0},World.Weapon.SPEAR,Army.Ship.BOAT,3000,6000));
        reject(w,()->w.army.deploy(10,0,new int[]{1,1},World.Weapon.SPEAR,Army.Ship.BOAT,3000,6000));
        reject(w,()->w.army.deploy(10,0,new int[]{20},World.Weapon.SPEAR,Army.Ship.BOAT,3000,6000));
        reject(w,()->w.army.deploy(10,0,new int[]{1,2,3},World.Weapon.SPEAR,Army.Ship.BOAT,3000,6000));
        reject(w,()->w.army.deploy(10,0,new int[]{1},World.Weapon.SPEAR,Army.Ship.BOAT,3000,100));
        ok(w.army.deploy(10,0,new int[]{1,2},World.Weapon.SPEAR,Army.Ship.TOWER_SHIP,3000,18000));World.Unit u=w.unit(1);
        check(w.actionPoints[0]==50&&w.city(10).troops==27000&&w.city(10).food==182000,"three-officer deployment charges once");
        for(int id:new int[]{0,1,2})check(w.officer(id).unitId==1&&w.officer(id).cityId==-1&&w.officer(id).acted,"every crew member locked");
        check(w.army.aptitude(u)==3&&w.army.intelligence(u)==99&&w.army.war(u)==84,"deputy stats and aptitude contribute");
        check(w.army.leadership(u)==71,"unrelated deputy contributes half of leadership advantage");
        reject(w,()->w.train(10,1));reject(w,()->w.deploy(10,2,World.Weapon.CAVALRY,3000));
        byte[] snapshot=bytes(w);check(Arrays.equals(snapshot,bytes(SaveCodec.decode(snapshot))),"crew survives exact roundtrip");
        ok(w.enter(1,10));check(w.city(10).ships[0]==2&&w.city(10).troops==30000&&w.city(10).equipment[0]==12000,"return restores gear once");
        for(int id:new int[]{0,1,2})check(w.officer(id).unitId==-1&&w.officer(id).cityId==10&&w.officer(id).acted,"return releases whole crew but prevents reusing this turn");cases++;
        reset(w);ok(w.army.deploy(10,0,new int[]{1,2},World.Weapon.CAVALRY,Army.Ship.WARSHIP,3000,6000));u=w.unit(2);w.removeUnit(u);
        for(int id:new int[]{0,1,2})check(w.officer(id).unitId==-1&&w.officer(id).cityId>=0,"defeat releases every crew member");check(w.city(10).ships[1]==1,"destroyed ship not refunded");SaveCodec.validate(w);cases++;
    }
    private static void equipment()throws Exception{
        for(World.Weapon weapon:World.Weapon.values()){
            World w=fixture();int before=w.city(10).equipment[weapon.ordinal()];ok(w.army.deploy(10,0,new int[0],weapon,Army.Ship.BOAT,8000,8000));
            int expected=weapon==World.Weapon.SWORD?0:Army.siegeWeapon(weapon)?1:8000;
            check(w.city(10).equipment[weapon.ordinal()]==before-expected,"proper equipment units "+weapon);
            w.unit(1).troops=5000;ok(w.enter(1,10));check(w.city(10).equipment[weapon.ordinal()]==before-expected+(weapon==World.Weapon.SWORD?0:Army.siegeWeapon(weapon)?1:5000),"surviving equipment returned "+weapon);cases++;
        }
        World w=fixture();ok(w.army.deploy(10,0,new int[]{1},World.Weapon.RAM,Army.Ship.WARSHIP,3000,6000));w.city(10).ships[1]=100;
        reject(w,()->w.enter(1,10));w.city(10).ships[1]=0;w.city(10).equipment[5]=100;reject(w,()->w.enter(1,10));w.city(10).equipment[5]=0;ok(w.enter(1,10));cases++;
    }
    private static void water()throws Exception{
        for(World.Weapon weapon:World.Weapon.values()){
            World w=fixture();World.Unit u=unit(w,1,0,0,weapon,new Hex(8,8));u.ship=Army.Ship.TOWER_SHIP;
            check(w.reachable(u).containsKey(new Hex(9,8)),"every land weapon can embark "+weapon);ok(w.move(1,new Hex(9,8)));
            check(w.army.water(u.hex)&&w.war.range(u)==2&&w.war.movement(u)==5,"water uses selected ship profile");
            check(w.army.equipmentLabel(u).contains("楼船"),"water equipment displayed");w.orders.reset(u);ok(w.move(1,new Hex(11,8)));
            check(!w.army.water(u.hex)&&u.weapon==weapon&&u.ship==Army.Ship.TOWER_SHIP,"disembark preserves both equipment identities");SaveCodec.validate(w);cases++;
        }
        World w=fixture();World.Unit u=unit(w,1,0,0,World.Weapon.CAVALRY,new Hex(9,8));
        w.terrain[11][8]=World.Terrain.MOUNTAIN;check(!w.reachable(u).containsKey(new Hex(11,8)),"no mountain landing");
        World.Unit enemy=unit(w,2,1,20,World.Weapon.SPEAR,new Hex(10,8));check(!w.reachable(u).containsKey(enemy.hex),"enemy ship blocks navigation");
        reject(w,()->w.war.tactic(1,2,War.Tactic.CHARGE));cases++;
    }
    private static void combat()throws Exception{
        for(World.Weapon weapon:Arrays.asList(World.Weapon.RAM,World.Weapon.SIEGE_TOWER,World.Weapon.WOODEN_BEAST,World.Weapon.CATAPULT)){
            World w=fixture();World.Unit u=unit(w,1,0,0,weapon,new Hex(17,5));w.city(20).troops=10000;
            int defense=w.city(20).defense;ok(w.army.tactic(1,w.city(20).hex,w.army.tactics(u).get(0)));int defenseLoss=defense-w.city(20).defense,troopLoss=10000-w.city(20).troops;
            check(weapon==World.Weapon.SIEGE_TOWER?troopLoss>defenseLoss:defenseLoss>=500,"siege specialization "+weapon);cases++;
        }
        World w=fixture();World.Unit u=unit(w,1,0,0,World.Weapon.SIEGE_TOWER,new Hex(16,5));w.city(20).troops=1;
        ok(w.army.tactic(1,w.city(20).hex,w.army.tactics(u).get(0)));check(w.city(20).owner==0,"zero garrison captures with remaining walls");SaveCodec.validate(w);cases++;
        w=fixture();u=unit(w,1,0,0,World.Weapon.CATAPULT,new Hex(15,5));w.city(20).troops=10000;ok(w.army.tactic(1,w.city(20).hex,w.army.tactics(u).get(0)));check(w.city(20).defense<3000,"catapult sieges at range three");cases++;
        World ram=fixture();unit(ram,1,0,0,World.Weapon.RAM,new Hex(7,8));unit(ram,2,1,20,World.Weapon.SPEAR,new Hex(8,8));reject(ram,()->ram.attack(1,2));cases++;
        for(Army.Ship ship:Army.Ship.values()){
            World battle=fixture();World.Unit a=unit(battle,1,0,0,World.Weapon.SPEAR,new Hex(9,8));a.ship=ship;a.energy=100;battle.officer(0).aptitude[5]=3;
            World.Unit b=unit(battle,2,1,20,World.Weapon.CAVALRY,new Hex(10,8));b.ship=Army.Ship.BOAT;battle.strategy.setSeed(0);
            byte[] before=bytes(battle);battle.war.previewDamage(1,2);check(Arrays.equals(before,bytes(battle)),"naval preview does not consume RNG");
            if(ship==Army.Ship.BOAT){check(battle.army.tactics(a).isEmpty(),"basic boat has no advanced tactics");ok(battle.attack(1,2));check(b.troops<5000,"boat ordinary attack works");}
            else {ok(battle.army.tactic(1,b.hex,Army.Tactic.FIRE_ARROW));check(b.burning==2&&b.troops<5000,"naval fire causes persistent burn");
                World loaded=SaveCodec.decode(bytes(battle));check(loaded.unit(2).burning==2,"burn survives save");battle.active=1;b.acted=false;ok(battle.army.extinguish(2));check(b.burning==0&&b.energy==75,"burn can be extinguished");}
            SaveCodec.validate(battle);cases++;
        }
        World treaty=fixture();World.Unit fire=unit(treaty,1,0,0,World.Weapon.SPEAR,new Hex(9,8));fire.ship=Army.Ship.WARSHIP;treaty.officer(0).aptitude[5]=3;
        World.Unit ship=unit(treaty,2,1,20,World.Weapon.CAVALRY,new Hex(10,8));treaty.strategy.setSeed(0);ok(treaty.army.tactic(1,ship.hex,Army.Tactic.FIRE_ARROW));
        treaty.campaign.treaties.add(new Campaign.Treaty(0,1,Campaign.TreatyKind.CEASEFIRE,3));int troops=ship.troops;treaty.army.tick();check(ship.troops==troops&&ship.burning==1,"ceasefire suppresses persistent damage but fire clock advances");
        treaty.army.tick();check(ship.burning==0&&ship.burningOwner==-1,"expired fire source cleared");SaveCodec.validate(treaty);cases++;
        World retreat=fixture();World.Unit misled=unit(retreat,1,0,0,World.Weapon.SPEAR,new Hex(10,8));misled.status=War.Status.MISLED;misled.statusTurns=1;Hex old=misled.hex;
        retreat.war.resetOwner(0);check(!old.equals(misled.hex)&&misled.hex.distance(retreat.city(10).hex)<old.distance(retreat.city(10).hex),"naval mislead retreats toward city");SaveCodec.validate(retreat);cases++;
    }
    private static void manufacturing()throws Exception{
        World w=fixture();final World initial=w;reject(w,()->initial.army.produce(10,1,World.Weapon.RAM,null));factory(w,Domestic.Kind.WORKSHOP);
        reject(w,()->initial.army.produce(10,1,World.Weapon.CATAPULT,null));
        int before=w.city(10).equipment[5],gold=w.city(10).gold;ok(w.army.produce(10,1,World.Weapon.RAM,null));
        check(w.city(10).gold==gold-1500&&w.officer(1).otherTaskTurns==3,"manual-listed RAM production cost charged once");reject(w,()->initial.train(10,1));
        for(int turn=0;turn<3;turn++){w=SaveCodec.decode(bytes(w));ok(w.nextTurn());check(w.city(10).equipment[5]==before+(turn==2?1:0),"production completes on third turn only");}
        check(w.army.productions().isEmpty()&&w.officer(1).otherTaskTurns==0,"production and task clocks agree");ok(w.nextTurn());check(w.city(10).equipment[5]==before+1,"no repeated completion");cases++;
        World canceled=fixture();Domestic.Facility factory=factory(canceled,Domestic.Kind.WORKSHOP);ok(canceled.army.produce(10,1,World.Weapon.RAM,null));ok(canceled.domestic.demolish(factory.id,2));
        check(canceled.army.productions().isEmpty()&&canceled.officer(1).otherTaskTurns==0,"demolition cancels dependent job");SaveCodec.validate(canceled);cases++;
        World captured=fixture();factory(captured,Domestic.Kind.WORKSHOP);ok(captured.army.produce(10,1,World.Weapon.RAM,null));World.Unit attacker=unit(captured,1,1,20,World.Weapon.CAVALRY,new Hex(6,5));captured.active=1;captured.city(10).defense=1;ok(captured.siege(attacker.id,10));
        check(captured.army.productions().isEmpty()&&captured.officer(1).cityId==11,"capture cancels production and retreats worker");SaveCodec.validate(captured);cases++;
        World ship=fixture();factory(ship,Domestic.Kind.SHIPYARD);ok(ship.army.produce(10,1,null,Army.Ship.TOWER_SHIP));ok(ship.army.cancelProduction(1));check(ship.city(10).ships[0]==2,"cancellation produces nothing");
        reset(ship);ship.campaign.learned.put(0,EnumSet.of(Campaign.Tech.ENGINEERING,Campaign.Tech.WARSHIP));ok(ship.army.produce(10,1,null,Army.Ship.WARSHIP));for(int i=0;i<3;i++)ok(ship.nextTurn());check(ship.city(10).ships[1]==3,"researched warship completes");cases++;
    }
    private static void cargo()throws Exception{
        World w=fixture();int[] payload=new int[9];payload[5]=1;payload[8]=2;ok(w.domestic.transport(10,11,1,0,0,0,payload));check(w.city(10).equipment[5]==1&&w.city(10).equipment[8]==0,"siege cargo deducted once");
        w.city(11).equipment[8]=100;for(int i=0;i<8;i++){w=SaveCodec.decode(bytes(w));ok(w.nextTurn());}
        check(w.domestic.missions.size()==1&&w.city(11).equipment[5]==0,"one full siege inventory blocks entire delivery");w.city(11).equipment[8]=0;ok(w.nextTurn());
        check(w.domestic.missions.isEmpty()&&w.city(11).equipment[5]==1&&w.city(11).equipment[8]==2,"siege cargo delivery atomic and exact");cases++;
    }
    private static void saves()throws Exception{
        try(InputStream in=ArmyTest.class.getResourceAsStream("/legacy-v5.sg11.b64")){
            byte[] raw=Base64.getDecoder().decode(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim());check(raw[7]==5,"fixture generated by unmodified v5 encoder");World w=SaveCodec.decode(raw);
            check(w.unit(1).weapon==World.Weapon.CROSSBOW&&w.unit(1).deputies.length==0&&w.unit(1).ship==Army.Ship.BOAT,"v5 army migration defaults preserve old unit");
            check(w.officer(3001).otherTaskTurns==3&&w.campaign.projects().size()==1,"v5 running study preserved");
            World copy=SaveCodec.decode(bytes(w));for(int i=0;i<10&&!w.gameOver();i++){ok(w.nextTurn());ok(copy.nextTurn());check(Arrays.equals(bytes(w),bytes(copy)),"v5 deterministic continuation "+i);}cases++;
        }
        World w=fixture();ok(w.army.deploy(10,0,new int[]{1,2},World.Weapon.RAM,Army.Ship.WARSHIP,3000,6000));w.unit(1).deputies=new int[]{1,1};
        try{bytes(w);throw new AssertionError("duplicate deputy accepted");}catch(IOException expected){checks++;}
        w.unit(1).deputies=new int[]{1,2};w.city(10).ships[0]=101;try{bytes(w);throw new AssertionError("excess ship stock accepted");}catch(IOException expected){checks++;}cases++;
    }
    private static void simulation()throws Exception{
        for(int player=0;player<2;player++){
            World a=TestScenarios.load("river-siege-sandbox",player),b=TestScenarios.load("river-siege-sandbox",player);boolean sawWater=false;
            for(int i=0;i<35&&!a.gameOver();i++){
                for(World x:Arrays.asList(a,b)){for(World.City c:x.cities)if(c.owner==player&&!x.idle(c).isEmpty())x.army.deploy(c.id,x.idle(c).get(0).id,new int[0],World.Weapon.SWORD,Army.Ship.BOAT,3000,6000);ok(x.nextTurn());}
                SaveCodec.validate(a);check(Arrays.equals(bytes(a),bytes(b)),"water campaign deterministic after save "+i);
                for(World.Unit u:a.units)sawWater|=a.army.water(u.hex);b=SaveCodec.decode(bytes(b));
            }
            check(sawWater,"AI uses water routes on playable sandbox");cases++;
        }
    }
    private static void scenarioData()throws Exception{
        String data;try(InputStream in=ArmyTest.class.getResourceAsStream("/test-scenarios/river-siege-sandbox.properties")){data=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
        for(String bad:Arrays.asList(data.replace("arsenal.1=11|","arsenal.1=10|"),data.replace("arsenal.0=10|2|","arsenal.0=999|2|"),data.replace("arsenal.0=10|2|","arsenal.0=10|101|"),data.replace("aptitude.1=1|","aptitude.1=0|"),data.replace("aptitude.0=0|2|","aptitude.0=0|4|"))){
            try{ScenarioData.read(new ByteArrayInputStream(bad.getBytes(java.nio.charset.StandardCharsets.UTF_8)),0);throw new AssertionError("invalid arsenal/aptitude accepted");}catch(IOException expected){checks++;}
        }cases++;
        World ai=fixture();ai.officers.add(new World.Officer(21,"敌军副将",1,20,75,75,75,75,75));Arrays.fill(ai.city(20).equipment,0);ai.city(20).equipment[5]=1;ai.city(20).troops=16000;ai.city(20).food=60000;ok(ai.nextTurn());
        check(ai.units.stream().anyMatch(u->u.owner==1&&u.weapon==World.Weapon.RAM),"AI deploys siege stock even without basic weapon stock");SaveCodec.validate(ai);cases++;
    }
}
