package game.sanguo.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.CRC32;

public final class TechnologyFieldworksTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static World copy(World w)throws Exception{return SaveCodec.decode(bytes(w));}
    private interface Command{World.Result run();}
    private static void reject(World w,Command c)throws Exception{byte[] before=bytes(w);check(!c.run().ok,"invalid command rejected");check(Arrays.equals(before,bytes(w)),"rejection does not spend resources or RNG");}
    private static World fixture(){
        World w=new World(30,22,"工营","守营");w.cities.add(new World.City(0,"工城",new Hex(2,2),0));w.cities.add(new World.City(10,"守城",new Hex(26,18),1));
        w.city(0).gold=500000;w.city(0).troops=80000;w.city(0).food=500000;
        for(int i=0;i<8;i++)w.officers.add(new World.Officer(i,"将"+i,i<6?0:1,i<6?0:10,80,80,80,80,80));w.strategy.initializeOffices();w.campaign.points.put(0,100000);return w;
    }
    private static World.Unit unit(World w,int officer,World.Weapon weapon,Hex h){
        World.Officer o=w.officer(officer);World.Unit u=new World.Unit(w.nextUnitId++,o.owner,officer,weapon,h,5000,40000);w.strategy.releaseGovernor(officer);o.cityId=-1;o.unitId=u.id;Arrays.fill(o.aptitude,3);w.units.add(u);return u;
    }
    private static void learn(World w,int side,Campaign.Tech t){if(t.prerequisite!=null)learn(w,side,t.prerequisite);w.campaign.finishTech(side,t);}
    private static void tick(World w)throws Exception{
        w.turn++;w.campaign.tick();w.strategy.tick();w.war.tick();
        for(World.Officer o:w.officers)o.acted=false;Arrays.fill(w.actionPoints,60);for(World.Unit u:w.units)w.orders.reset(u);w.fieldworks.continueOwner(0);w.fieldworks.continueOwner(1);SaveCodec.validate(w);
    }
    public static void main(String[] args)throws Exception{
        research();migration();construction();gold();effects();traps();terrainAndPorts();malformed();
        World drill=ScenarioCatalog.load("fieldworks-drill",0);check(drill.units.get(0).gold==10000&&drill.city(11).kind==World.SiteKind.PORT,"bundled playable drill includes actual gold and ports");
        check(Arrays.equals(bytes(drill),bytes(copy(drill))),"drill roundtrip");
        World again=copy(drill);for(int i=0;i<24&&!drill.gameOver();i++){ok(drill.nextTurn());ok(again.nextTurn());check(Arrays.equals(bytes(drill),bytes(again)),"full AI continuation deterministic");}
        System.out.println("PASS: "+checks+" technology/fieldworks assertions: 36 real research completions, legacy v10, high-bit saves, construction/gold/upgrade/cancel, battle effects, traps, terrain, ports and deterministic campaigns.");
    }
    private static void research()throws Exception{
        World w=fixture();check(Campaign.Tech.researchable().size()==36,"36 canonical technologies");
        check(!Campaign.Tech.researchable().contains(Campaign.Tech.WARSHIP),"legacy warship excluded from new research");
        for(int branch=0;branch<9;branch++){
            List<Campaign.Tech> list=Campaign.Tech.branch(branch);check(list.size()==4,"four levels per branch");
            for(int level=0;level<4;level++){
                Campaign.Tech tech=list.get(level);check(tech.points==new int[]{1000,2000,3000,5000}[level]&&tech.gold==new int[]{1000,2000,5000,10000}[level],"official level costs "+tech);
                if(level>0)check(tech.prerequisite==list.get(level-1),"ordered prerequisite "+tech);
                int points=w.campaign.points(0),gold=w.city(0).gold;ok(w.campaign.research(0,0,tech));
                check(w.campaign.points(0)==points-tech.points&&w.city(0).gold==gold-tech.gold,"exact single debit "+tech);
                reject(w,()->w.campaign.research(0,1,tech));World restored=copy(w);
                for(int turn=0;turn<tech.turns;turn++){tick(w);tick(restored);check(Arrays.equals(bytes(w),bytes(restored)),"mid-research save resumes "+tech);}
                check(w.campaign.has(0,tech)&&w.campaign.projects().isEmpty(),"effect only after real research completion "+tech);
            }
        }
        check(w.campaign.has(0,Campaign.Tech.WARSHIP),"catapult unlocks warship without 37th research");
        check(copy(w).campaign.has(0,Campaign.Tech.POPULAR_SUPPORT)&&copy(w).campaign.has(0,Campaign.Tech.WOODEN_OX),"technologies beyond bit32 survive saves");
        World p=fixture();reject(p,()->p.campaign.research(0,0,Campaign.Tech.STRONG_BOW));reject(p,()->p.campaign.research(0,0,Campaign.Tech.WARSHIP));
        p.officer(0).skillId=Skill.ZHIDAO.id;ok(p.campaign.research(0,0,Campaign.Tech.AXLE));check(p.city(0).gold==499500,"guidance halves real gold cost");
    }
    private static void migration()throws Exception{
        try(InputStream in=TechnologyFieldworksTest.class.getResourceAsStream("/legacy-v10.sg11.b64")){
            check(in!=null,"real pre-change v10 fixture exists");byte[] raw=Base64.getMimeDecoder().decode(in.readAllBytes());check(raw[7]==10,"fixture truly written by old codec");World old=SaveCodec.decode(raw);
            check(old.city(10).gold==17000&&old.officer(3).otherTaskTurns==5,"legacy resources and clock unchanged");
            check(old.campaign.has(0,Campaign.Tech.WALLS)&&!old.campaign.has(0,Campaign.Tech.FACILITY_REINFORCEMENT),"old effects retained without granting unearned predecessors");
            check(old.campaign.has(0,Campaign.Tech.STRONG_BOW)&&!old.campaign.has(0,Campaign.Tech.RETURN_FIRE),"old strong bow grandfathered without free return fire");
            check(old.war.structures().get(0).complete&&old.war.structures().get(0).hp==800,"legacy finished structure preserves current hp");
            World restored=copy(old);for(int i=0;i<5;i++){tick(old);tick(restored);check(Arrays.equals(bytes(old),bytes(restored)),"old pending research resumes deterministically");}
            check(old.campaign.has(0,Campaign.Tech.CATAPULT)&&!old.campaign.has(0,Campaign.Tech.STONE_BUILDING),"legacy project reward completes once");
            check(bytes(old)[7]==14,"migrated writer uses v14");
        }
    }
    private static void construction()throws Exception{
        World w=fixture();World.Unit u=unit(w,1,World.Weapon.SPEAR,new Hex(7,6));u.gold=5000;Hex site=new Hex(8,6);
        byte[] before=bytes(w);w.fieldworks.sites(u.id,War.StructureKind.CAMP);check(Arrays.equals(before,bytes(w)),"construction preview pure");
        reject(w,()->w.fieldworks.build(u.id,War.StructureKind.FORT,site,0));
        ok(w.fieldworks.build(u.id,War.StructureKind.CAMP,site,0));War.Structure s=w.war.at(site);
        check(u.gold==3500&&!s.complete&&s.builder==u.id&&w.fieldworks.defensePercent(u)==0,"incomplete camp spends real carried gold and grants no buff");
        reject(w,()->w.fieldworks.build(u.id,War.StructureKind.CAMP,site,0));World restored=copy(w);tick(w);tick(restored);check(Arrays.equals(bytes(w),bytes(restored)),"auto repair clock and unit action survive reload");
        ok(w.fieldworks.stop(u.id));int hp=s.hp;tick(w);check(s.hp==hp&&!s.complete,"cancel leaves incomplete shell without free progress");
        ok(w.fieldworks.repair(u.id,s.id));while(!s.complete)tick(w);check(s.hp==s.kind.hp&&s.builder<0&&w.fieldworks.defensePercent(u)==15,"completion enables aura and releases engineer");
        learn(w,0,Campaign.Tech.FACILITY_REINFORCEMENT);check(s.kind==War.StructureKind.FORT&&w.fieldworks.defensePercent(u)==25,"research actually upgrades placed camp");
        learn(w,0,Campaign.Tech.WALLS);check(s.kind==War.StructureKind.FORTRESS&&w.fieldworks.foodUse(u,250)==125,"fortress upgrade halves food use");
        check(!w.fieldworks.available(0).contains(War.StructureKind.CAMP),"obsolete camp removed from available construction");
        tick(w);reject(w,()->w.fieldworks.build(u.id,War.StructureKind.MUSIC,new Hex(7,7),0));
        World fast=fixture();World.Unit f=unit(fast,1,World.Weapon.SPEAR,new Hex(7,6));f.gold=5000;fast.officer(1).skillId=Skill.ZHUCHENG.id;int speed=fast.fieldworks.constructionRate(f);fast.officer(1).skillId="none";check(speed==2*fast.fieldworks.constructionRate(f),"construction skill changes real rate");
        f.gold=100;reject(fast,()->fast.fieldworks.build(f.id,War.StructureKind.CAMP,new Hex(8,6),0));
        World walls=fixture();World.Unit mason=unit(walls,1,World.Weapon.SPEAR,new Hex(7,6));mason.gold=5000;
        ok(walls.fieldworks.build(mason.id,War.StructureKind.EARTH_WALL,new Hex(8,6),0));War.Structure earth=walls.war.at(new Hex(8,6));int earthHp=earth.hp;
        learn(walls,0,Campaign.Tech.STONE_BUILDING);check(earth.kind==War.StructureKind.EARTH_WALL&&earth.hp==earthHp,"stone construction unlocks stone walls without transforming existing earth walls");
        check(walls.fieldworks.available(0).contains(War.StructureKind.EARTH_WALL)&&walls.fieldworks.available(0).contains(War.StructureKind.STONE_WALL),"both earth and stone wall choices remain available");
    }
    private static void gold()throws Exception{
        World w=fixture();int gold=w.city(0).gold;ok(w.army.deploy(0,1,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,3000,6000,3000));World.Unit u=w.unit(1);
        check(u.gold==3000&&w.city(0).gold==gold-3000,"deploy conserves gold");check(copy(w).unit(1).gold==3000,"carried gold saved");
        ok(w.enter(1,0));check(w.city(0).gold==gold,"enter returns carried gold once");reject(w,()->w.enter(1,0));
        w.officer(1).acted=false;ok(w.army.deploy(0,1,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,3000,6000,0));u=w.unit(2);
        ok(w.fieldworks.withdraw(u.id,0,3000));check(u.gold==3000,"adjacent transfer adds money");reject(w,()->w.fieldworks.withdraw(2,0,3000));
    }
    private static void effects()throws Exception{
        World w=fixture();World.Unit a=unit(w,1,World.Weapon.CAVALRY,new Hex(7,6)),b=unit(w,6,World.Weapon.SPEAR,new Hex(9,6));World ranged=w;reject(ranged,()->ranged.attack(1,2));learn(w,0,Campaign.Tech.MOUNTED_ARCHERY);ok(w.attack(a.id,b.id));check(a.troops==5000&&b.troops<5000,"research enables actual ranged cavalry attack");
        w=fixture();a=unit(w,1,World.Weapon.CROSSBOW,new Hex(7,6));b=unit(w,6,World.Weapon.CROSSBOW,new Hex(9,6));learn(w,1,Campaign.Tech.RETURN_FIRE);ok(w.attack(a.id,b.id));check(a.troops<5000&&b.troops<5000,"return fire hits ranged attacker without recursive loop");
        w=fixture();a=unit(w,1,World.Weapon.SPEAR,new Hex(7,6));b=unit(w,6,World.Weapon.SPEAR,new Hex(8,6));w.terrain[7][6]=World.Terrain.FOREST;learn(w,0,Campaign.Tech.FOREST_AMBUSH);int total=a.food+b.food;ok(w.attack(a.id,b.id));check(a.troops==5000&&a.food>40000&&a.food+b.food==total,"forest attack avoids counter and steals existing food");
        for(World.Weapon weapon:new World.Weapon[]{World.Weapon.SPEAR,World.Weapon.HALBERD,World.Weapon.CROSSBOW,World.Weapon.CAVALRY}){
            World plain=fixture();World.Unit x=unit(plain,1,weapon,new Hex(7,6)),y=unit(plain,6,weapon,new Hex(8,6));World elite=copy(plain);learn(elite,0,elite.campaign.elite(elite.unit(x.id)));
            check(elite.war.previewDamage(x.id,y.id)>plain.war.previewDamage(x.id,y.id)&&elite.war.movement(elite.unit(x.id))>plain.war.movement(x),"elite research improves actual damage and movement "+weapon);
        }
        w=fixture();a=unit(w,1,World.Weapon.CATAPULT,new Hex(7,6));b=unit(w,6,World.Weapon.SPEAR,new Hex(9,6));World.Unit collateral=unit(w,2,World.Weapon.SPEAR,new Hex(10,6));learn(w,0,Campaign.Tech.THUNDERBOLT);ok(w.army.tactic(a.id,b.hex,Army.Tactic.STONE));check(collateral.troops<5000,"thunderbolt actually hits friendly neighboring troop");
        w=fixture();learn(w,0,Campaign.Tech.MILITARY_REFORM);check(w.government.commandLimit(1)==13000,"military reform adds3000 to real command limit");ok(w.army.deploy(0,1,new int[0],World.Weapon.SWORD,Army.Ship.BOAT,13000,26000));check(copy(w).unit(1).troops==13000,"larger formation deploys and persists");
    }
    private static void traps()throws Exception{
        World w=fixture();World.Unit engineer=unit(w,1,World.Weapon.SPEAR,new Hex(7,6));engineer.gold=3000;Hex h=new Hex(8,6);
        ok(w.fieldworks.build(engineer.id,War.StructureKind.FIRE_BALL,h,0));War.Structure s=w.war.at(h);check(s.complete,"small trap completes immediately when construction rate sufficient");
        Hex delta=new Hex(0,0).neighbors().get(0);Hex victim=new Hex(h.q+delta.q*2,h.r+delta.r*2);World.Unit enemy=unit(w,6,World.Weapon.SPEAR,victim);
        learn(w,0,Campaign.Tech.EXPLOSIVES);check(s.kind==War.StructureKind.INFERNO_BALL,"existing trap upgraded automatically");
        World restored=copy(w);w.war.ignite(h,engineer);restored.war.ignite(h,restored.unit(engineer.id));
        check(w.war.at(h)==null&&enemy.troops<5000,"directional ignition removes trap and damages units on its ray");check(Arrays.equals(bytes(w),bytes(restored)),"trap direction and cascade survive save/reload");
        int after=enemy.troops;w.war.ignite(h,engineer);check(enemy.troops==after,"consumed trap cannot deal burst damage twice");
    }
    private static void terrainAndPorts()throws Exception{
        World w=fixture();World.Unit u=unit(w,1,World.Weapon.SPEAR,new Hex(7,6));Hex h=new Hex(8,6);w.terrain[8][6]=World.Terrain.MOUNTAIN_PATH;
        check(!w.reachable(u).containsKey(h),"hard path blocked without research");learn(w,0,Campaign.Tech.DIFFICULT_MARCH);check(w.reachable(u).containsKey(h),"research opens actual hard path");ok(w.move(u.id,h));check(copy(w).terrain[8][6]==World.Terrain.MOUNTAIN_PATH,"new terrain saved");
        for(World.Terrain terrain:new World.Terrain[]{World.Terrain.MOUNTAIN_PATH,World.Terrain.SHALLOWS}){
            World landing=fixture();World.Unit boat=unit(landing,1,World.Weapon.SPEAR,new Hex(7,6));landing.terrain[7][6]=World.Terrain.WATER;landing.terrain[8][6]=terrain;
            reject(landing,()->landing.move(boat.id,h));check(!landing.reachable(boat).containsKey(h),"landing cannot bypass terrain research "+terrain);
            learn(landing,0,Campaign.Tech.DIFFICULT_MARCH);ok(landing.move(boat.id,h));check(boat.hex.equals(h),"researched landing reaches terrain "+terrain);
            World exit=fixture();for(Hex neighbor:exit.city(0).hex.neighbors())exit.terrain[neighbor.q][neighbor.r]=terrain;
            reject(exit,()->exit.deploy(0,1,World.Weapon.SPEAR,3000));learn(exit,0,Campaign.Tech.DIFFICULT_MARCH);ok(exit.deploy(0,1,World.Weapon.SPEAR,3000));
        }
        World.City port=new World.City(2,"港",new Hex(7,14),0);port.kind=World.SiteKind.PORT;port.baseDefense=1000;port.gold=9000;port.food=99000;port.troops=30000;w.cities.add(port);w.officer(2).cityId=2;
        check(w.campaign.goldCap(port)==10000&&w.campaign.foodCap(port)==100000&&w.campaign.troopCap(port)==30000,"base port capacities");reject(w,()->w.campaign.trade(2,2,true,5000));learn(w,0,Campaign.Tech.PORT_EXPANSION);ok(w.campaign.trade(2,2,true,5000));check(port.food==104000&&w.campaign.foodCap(port)==400000&&w.campaign.troopCap(port)==60000,"expansion changes actual port command acceptance");
        check(copy(w).city(2).kind==World.SiteKind.PORT,"port identity saved independent of data pack");
        World reduced=fixture();learn(reduced,0,Campaign.Tech.ADMINISTRATION);check(reduced.campaign.orderLoss(0,10)==5,"administration changes loss");learn(reduced,0,Campaign.Tech.POPULAR_SUPPORT);check(reduced.campaign.loyaltyLoss(0,2)==1,"popular support changes loss");
    }
    private static void malformed()throws Exception{
        World w=fixture();learn(w,0,Campaign.Tech.WOODEN_OX);byte[] raw=bytes(w);byte[] needle="WOODEN_OX".getBytes(java.nio.charset.StandardCharsets.UTF_8);int offset=-1;
        outer:for(int i=20;i<=raw.length-needle.length;i++){for(int j=0;j<needle.length;j++)if(raw[i+j]!=needle[j])continue outer;offset=i;break;}
        check(offset>20,"stable technique ID encoded");Arrays.fill(raw,offset,offset+needle.length,(byte)'Z');CRC32 crc=new CRC32();crc.update(raw,20,raw.length-20);ByteBuffer.wrap(raw,12,8).putLong(crc.getValue());boolean rejected=false;try{SaveCodec.decode(raw);}catch(IOException e){rejected=true;}check(rejected,"unknown ID rejected even with valid CRC");
        World.Unit u=unit(w,1,World.Weapon.SPEAR,new Hex(7,6));u.gold=10001;rejected=false;try{bytes(w);}catch(IOException e){rejected=true;}check(rejected,"invalid carried gold rejected");
    }
}
