package game.sanguo.core;

import java.util.*;

/** Behavioral positions: outcomes and conservation, not a copy of the scoring implementation. */
public final class CampaignAiTest {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static World fixture(){
        World w=new World(32,20,"甲军","乙军","丙军");
        w.cities.add(new World.City(10,"甲都",new Hex(2,3),0));w.cities.add(new World.City(11,"后方",new Hex(2,16),0));
        w.cities.add(new World.City(20,"乙都",new Hex(28,3),1));w.cities.add(new World.City(30,"丙都",new Hex(28,16),2));
        for(World.City c:w.cities){c.troops=24000;c.food=150000;c.gold=30000;c.morale=90;}
        for(int i=0;i<8;i++)w.officers.add(new World.Officer(i,"甲将"+i,0,i<5?10:11,80,80,80,80,80));
        for(int i=20;i<26;i++)w.officers.add(new World.Officer(i,"乙将"+i,1,20,80,80,60,60,60));
        w.officers.add(new World.Officer(30,"丙将",2,30,75,75,60,60,60));w.strategy.initializeOffices();return w;
    }
    private static World.Unit unit(World w,int officer,World.Weapon weapon,int q,int r){
        World.Officer o=w.officer(officer);w.strategy.releaseGovernor(officer);World.Unit u=new World.Unit(w.nextUnitId++,o.owner,officer,weapon,new Hex(q,r),6000,60000);
        o.cityId=-1;o.unitId=u.id;u.energy=100;w.units.add(u);return u;
    }
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static void run(World w,World.Unit u){new CampaignAi(w).runUnit(u,true,c->true,c->c.owner==u.owner);}
    public static void main(String[] args)throws Exception{
        planning();friendlyFire();control();siege();retreat();deployment();supply();permissions();routing();replay();
        System.out.println("PASS: "+checks+" campaign AI assertions: pure/stable target selection, area friendly fire, rescue/immunity, siege, retreat/supply, reserves/formations, district permissions, hazards/detours and saved replay.");
    }
    private static void planning()throws Exception{
        World w=fixture();World.Unit a=unit(w,1,World.Weapon.CROSSBOW,9,8),strong=unit(w,21,World.Weapon.HALBERD,11,7),weak=unit(w,22,World.Weapon.CROSSBOW,11,8);weak.troops=100;a.energy=0;
        CampaignAi ai=new CampaignAi(w);byte[] before=bytes(w);CampaignAi.Action choice=ai.bestAction(a.id,true);
        check(choice!=null&&choice.target==weak.id&&choice.kind==CampaignAi.Kind.ATTACK,"choose a kill over first high-strength enemy");
        check(Arrays.equals(before,bytes(w)),"planning does not mutate state or consume RNG");
        Collections.reverse(w.units);CampaignAi.Action reverse=ai.bestAction(a.id,true);check(reverse.target==choice.target&&reverse.kind==choice.kind,"target choice independent of unit-list order");
        check(ai.execute(choice).ok&&w.unit(weak.id)==null&&strong.troops==6000,"chosen command actually removes intended enemy");
        byte[] after=bytes(w);check(!ai.execute(choice).ok&&Arrays.equals(after,bytes(w)),"replaying completed order cannot act twice");
    }
    private static void friendlyFire()throws Exception{
        World w=fixture();World.Unit a=unit(w,1,World.Weapon.CROSSBOW,9,8),b=unit(w,21,World.Weapon.SPEAR,11,8),friend=unit(w,2,World.Weapon.SPEAR,11,7);unit(w,22,World.Weapon.SPEAR,12,8);w.officer(1).aptitude[2]=3;
        CampaignAi.Action choice=new CampaignAi(w).bestAction(a.id,true);check(choice!=null&&choice.tactic!=War.Tactic.VOLLEY,"avoid volley damaging a friendly in the area");
        World magic=fixture();World.Unit mage=unit(magic,1,World.Weapon.SPEAR,9,8),enemy=unit(magic,21,World.Weapon.SPEAR,11,8);unit(magic,22,World.Weapon.SPEAR,12,8);magic.officer(1).skillId=Skill.GUIMEN.id;
        Domestic.Facility farm=new Domestic.Facility(magic.domestic.nextFacilityId++,10,Domestic.Kind.FARM,new Hex(11,7),-1,0);magic.domestic.facilities.add(farm);
        CampaignAi.Action spell=new CampaignAi(magic).bestAction(mage.id,true);check(spell==null||spell.plot!=War.Plot.LIGHTNING,"protect friendly buildings from lightning even without nearby friendly troops");
        check(friend.troops==6000&&b.troops==6000&&enemy.troops==6000,"all tactical assessment is read-only");
        World stone=fixture();World.Unit engine=unit(stone,1,World.Weapon.CATAPULT,9,8);unit(stone,2,World.Weapon.SPEAR,11,7);
        stone.campaign.learned.put(0,EnumSet.of(Campaign.Tech.THUNDERBOLT));
        stone.war.structures.add(new War.Structure(stone.war.nextStructureId++,1,War.StructureKind.ARROW_TOWER,new Hex(11,8),700));
        CampaignAi.Action shot=new CampaignAi(stone).bestAction(engine.id,true);check(shot==null,"structure targeting must not bypass thunderbolt friendly-fire protection");
    }
    private static void control()throws Exception{
        World w=fixture();World.Unit caster=unit(w,1,World.Weapon.SPEAR,9,8),friend=unit(w,2,World.Weapon.SPEAR,10,8);friend.status=War.Status.CONFUSED;friend.statusTurns=2;
        CampaignAi ai=new CampaignAi(w);CampaignAi.Action calm=ai.bestAction(caster.id,false);
        check(calm!=null&&calm.plot==War.Plot.CALM&&ai.execute(calm).ok&&friend.status==War.Status.NORMAL,"non-offensive orders rescue confused allies");
        w=fixture();caster=unit(w,1,World.Weapon.SPEAR,9,8);World.Unit enemy=unit(w,21,World.Weapon.SPEAR,11,8);w.officer(1).skillId=Skill.SHENSUAN.id;w.officer(21).skillId=Skill.DONGCHA.id;
        CampaignAi.Action choice=new CampaignAi(w).bestAction(caster.id,true);check(choice==null||choice.kind!=CampaignAi.Kind.PLOT,"do not waste energy on a plot-immune enemy");
        w.officer(21).skillId="none";choice=new CampaignAi(w).bestAction(caster.id,true);check(choice!=null&&choice.plot==War.Plot.CONFUSE,"intelligent caster uses guaranteed control at range");
    }
    private static void siege()throws Exception{
        World w=fixture();World.Unit ram=unit(w,1,World.Weapon.RAM,27,3);int before=w.city(20).defense;run(w,ram);
        check(w.city(20).defense<before&&ram.acted&&ram.energy<100,"ram attacks city through its real siege tactic");
        w.orders.reset(ram);ram.energy=0;int old=w.city(20).defense;run(w,ram);check(w.city(20).defense==old&&ram.energy==5,"exhausted siege engine waits to recover energy");
    }
    private static void retreat()throws Exception{
        World w=fixture();World.Unit u=unit(w,1,World.Weapon.SPEAR,6,3);u.food=0;int troops=w.city(10).troops;run(w,u);
        check(w.unit(u.id)==null&&w.city(10).troops==troops+6000&&w.officer(1).cityId==10,"starving unit returns to a reachable friendly city");
        w=fixture();u=unit(w,1,World.Weapon.SPEAR,6,3);u.troops=1000;w.city(10).troops=w.campaign.troopCap(w.city(10));Hex before=u.hex;run(w,u);
        check(w.unit(u.id)!=null&&!u.hex.equals(before)&&u.hex.distance(w.city(11).hex)<before.distance(w.city(11).hex),"full nearest city does not trap retreat; seek another accepting city");
    }
    private static void deployment()throws Exception{
        World w=fixture();w.officer(0).leadership=20;w.officer(0).war=20;w.officer(0).politics=100;w.officer(1).leadership=100;w.officer(1).war=100;w.officer(1).aptitude[3]=3;
        w.officer(2).intelligence=100;w.officer(2).skillId=Skill.BAICHU.id;w.officer(3).skillId=Skill.SHENSUAN.id;
        CampaignAi ai=new CampaignAi(w);byte[] before=bytes(w);CampaignAi.Deployment d=ai.deployment(10,6000);
        check(d!=null&&d.leader==1&&d.weapon==World.Weapon.CAVALRY,"choose combat leader and strongest available aptitude, not first officer");
        check(d.troops>3000&&d.deputies().length>0&&Arrays.equals(before,bytes(w)),"size and support formation planned without mutation");
        int total=w.city(10).troops,food=w.city(10).food,gear=w.city(10).equipment[d.weapon.ordinal()];check(ai.deploy(10,6000),"planned army is deployable");World.Unit army=w.units.get(0);
        check(w.city(10).troops>=d.reserve&&army.troops+w.city(10).troops==total&&army.food+w.city(10).food==food&&w.city(10).equipment[d.weapon.ordinal()]+army.troops==gear,"troops, food and equipment conserved; reserves retained");
        check(w.officer(0).cityId==10&&w.officer(0).unitId<0,"administrator remains at home");
        World poor=fixture();poor.city(10).troops=8000;check(new CampaignAi(poor).deployment(10,6000)==null,"do not empty an understrength garrison");
        poor.city(10).troops=24000;poor.city(10).food=6500;check(new CampaignAi(poor).deployment(10,6000)==null,"do not launch an army without food reserve");
        poor.city(10).food=150000;unit(poor,21,World.Weapon.SPEAR,5,3).troops=18000;check(new CampaignAi(poor).reserve(poor.city(10))>6000,"nearby invaders increase retained garrison");
        World blocked=fixture();for(Hex h:blocked.city(20).hex.neighbors())blocked.terrain[h.q][h.r]=World.Terrain.MOUNTAIN;
        check(new CampaignAi(blocked).deployment(10,10000,c->c.id==20)==null,"unreachable designated district objective does not launch a pointless army toward another force");
    }
    private static void supply()throws Exception{
        World w=fixture();World.Unit u=unit(w,1,World.Weapon.SPEAR,3,3);u.food=0;CampaignAi ai=new CampaignAi(w);int total=w.city(10).food;
        check(ai.replenish(10)&&u.food>0&&w.city(10).food+u.food==total&&w.actionPoints[0]==50,"adjacent resupply consumes actual stock and action points");
        w=fixture();w.city(10).food=3000;w.city(10).gold=100;unit(w,21,World.Weapon.SPEAR,5,3);ai=new CampaignAi(w);
        check(ai.prepare(11)&&w.domestic.missions.size()==1&&w.domestic.missions.get(0).transport,"backline sends a real escorted supply mission");
        int count=w.domestic.missions.size();ai.prepare(11);check(w.domestic.missions.size()==count,"avoid duplicate shipments to the same frontline");bytes(w);
    }
    private static void permissions()throws Exception{
        World w=fixture();World.Unit a=unit(w,1,World.Weapon.SPEAR,9,8),enemy=unit(w,21,World.Weapon.SPEAR,10,8);
        new CampaignAi(w).runUnit(a,false,c->false,c->false);check(enemy.troops==6000&&enemy.status==War.Status.NORMAL,"attack-disabled district does not damage or control enemy units");
        w=fixture();a=unit(w,1,World.Weapon.RAM,27,3);int defense=w.city(20).defense;new CampaignAi(w).runUnit(a,true,c->c.id==30,c->false);
        check(w.city(20).defense==defense,"city objectives constrain siege even when other cities are already in range");
        w=fixture();a=unit(w,1,World.Weapon.SPEAR,9,8);enemy=unit(w,21,World.Weapon.SPEAR,10,8);w.campaign.treaties.add(new Campaign.Treaty(0,1,Campaign.TreatyKind.ALLIANCE,10));
        CampaignAi.Action action=new CampaignAi(w).bestAction(a.id,true);check(action==null||action.target!=enemy.id,"allied targets excluded from hostile decisions");
    }
    private static void routing()throws Exception{
        World w=fixture();World.Unit u=unit(w,1,World.Weapon.SPEAR,7,8);for(int r=1;r<w.height;r++)w.terrain[10][r]=World.Terrain.MOUNTAIN;
        int distance=u.hex.distance(w.city(20).hex);run(w,u);check(!u.hex.equals(new Hex(7,8))&&u.hex.r<8,"route can move away from target to bypass a long wall");
        w=fixture();u=unit(w,1,World.Weapon.SPEAR,7,8);w.terrain[8][8]=World.Terrain.POISON;w.war.fires.add(new War.Fire(new Hex(9,8),1,2));int troops=u.troops;run(w,u);
        check(u.troops==troops&&w.terrain[u.hex.q][u.hex.r]!=World.Terrain.POISON&&w.war.fireAt(u.hex)==null,"route avoids avoidable poison and fire");
        w=fixture();u=unit(w,1,World.Weapon.CROSSBOW,9,8);World.Unit enemy=unit(w,21,World.Weapon.SPEAR,10,8);u.energy=0;run(w,u);
        check(u.hex.distance(enemy.hex)>1&&enemy.troops<6000,"ranged unit repositions to shoot outside melee counter range");
    }
    private static void replay()throws Exception{
        for(String id:new String[]{"regional-sandbox","river-siege-sandbox","world-drill"})for(int seed=0;seed<3;seed++){
            World a=ScenarioCatalog.load(id,0,seed);a.strategy.setSeed(seed);World b=SaveCodec.decode(bytes(a));
            for(int turn=0;turn<36&&!a.gameOver();turn++){
                check(a.nextTurn().ok&&b.nextTurn().ok,"paired campaign advances");check(Arrays.equals(bytes(a),bytes(b)),"AI and battles reproduce exactly after save/restore");
                b=SaveCodec.decode(bytes(b));
            }
        }
    }
}
