package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Fixed real positions; checks outcomes, resource conservation, old saves and replay. */
public final class StrategicManagementTest {
    private static int checks;
    private static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static World copy(World w)throws Exception{return SaveCodec.decode(bytes(w));}
    static World fixture(){
        World w=new World(44,24,"甲","乙");w.strategy.setSeed(25016);
        w.cities.add(new World.City(10,"主城",new Hex(2,2),0));w.cities.add(new World.City(11,"后方",new Hex(3,15),0));w.cities.add(new World.City(12,"前方",new Hex(15,15),0));w.cities.add(new World.City(20,"敌城",new Hex(36,15),1));
        for(World.City c:w.cities){c.gold=30000;c.food=160000;c.troops=30000;c.order=90;c.morale=100;}
        for(int i=0;i<12;i++)w.officers.add(new World.Officer(i,"将"+i,0,i<3?10:11,80,80,80,80+i%5,80));
        for(int i=20;i<26;i++)w.officers.add(new World.Officer(i,"敌将"+i,1,20,80,80,50,50,50));w.strategy.initializeOffices();return w;
    }
    static World.Unit unit(World w,int officer,World.Weapon weapon,Hex at){World.Officer o=w.officer(officer);w.strategy.releaseGovernor(officer);World.Unit u=new World.Unit(w.nextUnitId++,o.owner,officer,weapon,at,6000,60000);o.cityId=-1;o.unitId=u.id;u.energy=100;w.units.add(u);return u;}
    private static Districts.District group(World w,boolean attack,boolean produce){check(w.districts.configure(-1,"二军",new int[]{11,12},Districts.Policy.DEFENSE,-1,-1,attack,produce).ok,"create real district");return w.districts.all().get(0);}
    private static void reset(World w){for(World.Officer o:w.officers)o.acted=false;for(World.Unit u:w.units)w.orders.reset(u);w.actionPoints[0]=60;w.actionPoints[1]=60;w.districts.reset(0);}
    private static long[] stock(World w){long[] v=new long[3+World.Weapon.values().length];for(World.City c:w.cities){v[0]+=c.gold;v[1]+=c.food;v[2]+=c.troops;for(int i=0;i<c.equipment.length;i++)v[i+3]+=c.equipment[i];}for(Domestic.Mission m:w.domestic.missions){v[0]+=m.gold;v[1]+=m.food;v[2]+=m.troops;for(int i=0;i<m.equipment.length;i++)v[i+3]+=m.equipment[i];}return v;}
    public static void main(String[] args)throws Exception{migration();personnel();logistics();permissions();overview();coordination();stuck();longRun();performance();largeTurn();System.out.println("PASS: "+checks+" strategic management assertions: v18 migration, settings/replay, real transfers/production/cargo, reserves/permissions, stable targets/siege lanes, national filters and performance.");}
    private static void migration()throws Exception{
        try(InputStream in=StrategicManagementTest.class.getResourceAsStream("/legacy-v18.sg11.b64")){byte[] old=Base64.getDecoder().decode(in.readAllBytes());check(old[7]==18,"fixture produced by actual v0.24 writer");World w=SaveCodec.decode(old);check(bytes(w)[7]==19&&Arrays.equals(bytes(w),bytes(copy(w))),"real v18 upgrades and roundtrips");}
        try(InputStream in=StrategicManagementTest.class.getResourceAsStream("/legacy-v18-district.sg11.b64")){World old=SaveCodec.decode(Base64.getDecoder().decode(in.readAllBytes()));Districts.District prior=old.districts.all().get(0);check(prior.reserveTroops()==10000&&prior.reserveGold()==5000&&prior.reserveFood()==40000&&prior.transfer()&&prior.supplyEnabled(),"actual legacy district gets defaults");byte[] saved=bytes(old);old.districts.run();check(Arrays.equals(saved,bytes(old)),"old acted district and pending cargo do not reexecute on migration");}
        World w=fixture();Districts.District d=group(w,false,true);check(d.reserveTroops()==10000&&d.transfer()&&d.supplyEnabled(),"legacy-compatible defaults");check(w.districts.settings(d.id,17000,7000,65000,false,false).ok,"save settings through paid command");World b=copy(w);Districts.District bd=b.districts.get(d.id);check(bd.reserveTroops()==17000&&bd.reserveGold()==7000&&bd.reserveFood()==65000&&!bd.transfer()&&!bd.supplyEnabled(),"settings retained");
        byte[] before=bytes(w);check(!w.districts.settings(d.id,-1,0,0,true,true).ok&&Arrays.equals(before,bytes(w)),"invalid settings atomic");
    }
    private static void personnel()throws Exception{
        World w=fixture();Districts.District d=group(w,false,false);w.turn=1;reset(w);w.districts.run();
        check(w.domestic.missions.stream().anyMatch(m->!m.transport&&m.targetCity==12),"empty city receives actual personnel mission");
        check(new DistrictManagement(w).residents(w.city(12))==0,"officer does not teleport");
        byte[] before=bytes(w);w.districts.run();check(Arrays.equals(before,bytes(w)),"same-turn execution cannot dispatch twice");World replay=copy(w);replay.districts.run();check(Arrays.equals(before,bytes(replay)),"saved acted turn prevents duplicate mission");
        for(int t=0;t<5;t++)w.domestic.tick();check(new DistrictManagement(w).residents(w.city(12))>0,"personnel really arrives");check(d.report().contains("调将"),"report identifies personnel action");
    }
    private static void logistics()throws Exception{
        World w=fixture();World.City front=w.city(12);front.kind=World.SiteKind.PORT;front.gold=0;front.food=0;front.troops=0;Arrays.fill(front.equipment,0);
        Districts.District d=new Districts.District(-1,0,"测试后勤");d.cities.add(11);d.cities.add(12);d.supply=12;
        DistrictManagement manager=new DistrictManagement(w);long[] original=stock(w);int sent=0;
        for(int i=3;i<9;i++)if(manager.supply(d,w.city(11),w.officer(i)))sent++;
        check(sent>0&&w.domestic.missions.stream().allMatch(m->m.troops>=1000),"real convoys carry escorts");long[] after=stock(w);original[0]-=sent*100;check(Arrays.equals(original,after),"all inventories and pending cargo conserved minus real fees");
        int incomingFood=0,incomingGold=0,incomingTroops=0;for(Domestic.Mission m:w.domestic.missions){incomingFood+=m.food;incomingGold+=m.gold;incomingTroops+=m.troops;}
        check(incomingFood<=w.campaign.foodCap(front)&&incomingGold<=w.campaign.goldCap(front)&&incomingTroops<=w.campaign.troopCap(front),"all in-flight cargo counted against port capacities");
        check(w.city(11).troops>=d.reserveTroops&&w.city(11).food>=d.reserveFood&&w.city(11).gold>=d.reserveGold,"source reserves include fee");World replay=copy(w);
        for(int i=0;i<8;i++){w.domestic.tick();replay.domestic.tick();check(Arrays.equals(bytes(w),bytes(replay)),"cargo replay deterministic");}
        check(w.domestic.missions.isEmpty()&&front.food>0&&front.troops>0&&front.equipment[0]>0,"troops food gold equipment really delivered");check(Arrays.equals(original,stock(w)),"arrival neither duplicates nor swallows resources");
        World support=fixture();support.city(12).troops=2000;support.city(12).food=2000;unit(support,21,World.Weapon.SPEAR,new Hex(20,15));
        check(new CampaignAi(support).support(11),"nearby rear actually supports threatened city");check(support.domestic.missions.get(0).targetCity==12&&support.domestic.missions.get(0).troops>1000,"support addresses actual troop deficit");
    }
    private static void permissions()throws Exception{
        World w=fixture();Districts.District d=group(w,false,false);check(w.districts.settings(d.id,18000,8000,80000,false,false).ok,"disable permissions");int[] equipment=w.city(11).equipment.clone();w.turn=1;reset(w);w.districts.run();
        check(w.units.isEmpty()&&w.domestic.missions.isEmpty()&&Arrays.equals(equipment,w.city(11).equipment)&&w.army.productions().isEmpty(),"no offensive production transfer or transport through fallback");
        w.turn=2;d.points=0;byte[] before=bytes(w);int gold=w.city(11).gold;w.districts.run();check(w.city(11).gold==gold&&d.report().contains("预算"),"zero budget cannot spend and reports reason");
        World target=fixture();check(target.districts.configure(-1,"攻略",new int[]{11,12},Districts.Policy.CITY_ATTACK,20,-1,true,true).ok,"configured attack target");Districts.District g=target.districts.all().get(0);target.campaign.treaties.add(new Campaign.Treaty(0,1,Campaign.TreatyKind.CEASEFIRE,10));target.districts.cleanup();check(g.policy()==Districts.Policy.DEFENSE&&g.target()==-1,"ceasefire invalidates offensive policy");
        reset(w);check(w.districts.dissolve(d.id).ok&&w.districts.directCity(11),"dissolve restores real control");
    }
    private static void overview()throws Exception{
        World w=fixture();Districts.District d=group(w,false,false);w.city(12).food=0;w.city(12).order=40;unit(w,21,World.Weapon.CROSSBOW,new Hex(20,15));byte[] before=bytes(w);CityOverview view=new CityOverview(w);
        check(view.cities(2,-1,0,"",-1).stream().anyMatch(c->c.id==12),"approaching enemies filter");check(view.cities(3,d.id,6,"",-1).get(0).id==12,"food and district intersection");check(view.cities(4,-1,7,"",0).stream().anyMatch(c->c.id==12),"vacant city filter");check(view.detail(w.city(12)).contains("敌军逼近")&&view.detail(w.city(11)).contains("无敌军逼近"),"border and immediate threat labels distinct");
        for(int filter=0;filter<7;filter++)for(int sort=0;sort<9;sort++)view.cities(filter,-1,sort,"",-1);check(Arrays.equals(before,bytes(w)),"all inspections leave save and RNG identical");
    }
    private static void coordination()throws Exception{
        World w=fixture();World.City enemy=w.city(20);enemy.troops=1000;enemy.defense=2500;
        World.Unit melee=unit(w,3,World.Weapon.SPEAR,new Hex(35,15));World.Unit engine=unit(w,4,World.Weapon.RAM,new Hex(34,15));World.Unit bow=unit(w,5,World.Weapon.CROSSBOW,new Hex(34,16));int defense=enemy.defense;Hex before=melee.hex;
        boolean yielded=false;for(int t=0;t<5&&enemy.owner==1;t++){w.turn=t;reset(w);new CampaignAi(w).runUnits();yielded|=!melee.hex.equals(before);}
        check(enemy.defense<defense||enemy.owner==0,"mixed siege causes real city damage");check(yielded,"field army yields occupied engine approach");
        World stable=fixture();World.Unit army=unit(stable,3,World.Weapon.SPEAR,new Hex(10,5));new CampaignAi(stable).runUnits();AiOrders.Order order=stable.aiOrders.orders.get(army.id);check(order!=null&&order.target==20,"real unit receives reachable saved target");World replay=copy(stable);stable.turn++;replay.turn++;reset(stable);reset(replay);new CampaignAi(stable).runUnits();new CampaignAi(replay).runUnits();check(Arrays.equals(bytes(stable),bytes(replay)),"strategic intention survives save replay");
        stable.campaign.treaties.add(new Campaign.Treaty(0,1,Campaign.TreatyKind.CEASEFIRE,10));stable.turn++;reset(stable);new CampaignAi(stable).runUnits();check(stable.unit(army.id)==null||stable.aiOrders.get(army).target==-1,"treaty clears old objective before attack");
    }
    private static void stuck()throws Exception{
        World w=fixture();World.Unit ram=unit(w,3,World.Weapon.RAM,new Hex(10,8));for(Hex h:ram.hex.neighbors())w.terrain[h.q][h.r]=World.Terrain.MOUNTAIN;
        for(int i=0;i<4;i++){w.turn=i;w.orders.reset(ram);new CampaignAi(w).runUnit(ram,true,c->true,c->c.owner==0);}check(w.aiOrders.get(ram).stalled>=3,"actual blocked turns tracked");
        w.terrain[9][8]=World.Terrain.PLAIN;w.turn++;w.orders.reset(ram);new CampaignAi(w).runUnit(ram,true,c->true,c->c.owner==0);check(!ram.hex.equals(new Hex(10,8)),"stalled unit replans and leaves after route opens");
    }
    private static void longRun()throws Exception{
        World w=fixture();w.campaign.treaties.add(new Campaign.Treaty(0,1,Campaign.TreatyKind.ALLIANCE,36));Districts.District d=group(w,false,true);check(w.districts.configure(d.id,d.name(),new int[]{11,12},Districts.Policy.ECONOMY,-1,-1,false,true).ok,"economic long-run policy");w.city(11).equipment[0]=0;World replay=copy(w);long start=System.nanoTime();
        for(int turn=0;turn<18;turn++){check(w.nextTurn().ok&&replay.nextTurn().ok,"multi-city turn completes");check(Arrays.equals(bytes(w),bytes(replay)),"multi-city real turns deterministic after restart");replay=copy(replay);}
        check(new DistrictManagement(w).residents(w.city(12))>0,"long-term vacant city acquires staff");check(w.city(11).equipment[0]>0,"economy district actually manufactures missing equipment over time");check(w.domestic.count(12)>0||w.city(12).governorId>=0,"staff operates destination");
        System.out.println("18 multi-city turns + save replay: "+(System.nanoTime()-start)/1000000+" ms");
    }
    private static void performance()throws Exception{
        World w=ScenarioCatalog.load("heroes-mobile-sandbox",0,25016);long start=System.nanoTime();check(w.nextTurn().ok,"42-city/670-officer real turn");long elapsed=(System.nanoTime()-start)/1000000;check(elapsed<30000,"nation turn remains bounded under 30s on test runner");System.out.println("42 cities / 670 officers next turn: "+elapsed+" ms");
    }
    private static void largeTurn()throws Exception{
        World w=new World(200,200,"甲","乙");w.strategy.setSeed(25016);
        for(int i=0;i<40;i++){int owner=i<20?0:1;Hex h=new Hex(10+i%8*24,10+i/8*40);w.cities.add(new World.City(i,"城"+i,h,owner));World.Officer o=new World.Officer(i,"将"+i,owner,-1,80,80,70,70,70);World.Unit u=new World.Unit(w.nextUnitId++,owner,i,World.Weapon.SPEAR,new Hex(h.q+2,h.r),6000,60000);o.unitId=u.id;w.officers.add(o);w.units.add(u);}
        w.strategy.initializeOffices();SaveCodec.validate(w);long start=System.nanoTime();check(w.nextTurn().ok,"200x200 / 40-city / 40-army actual turn");long elapsed=(System.nanoTime()-start)/1000000;SaveCodec.validate(w);check(elapsed<30000,"large multi-army turn bounded under 30s on runner");System.out.println("Synthetic axial 200x200 / 40 cities / 40 units next turn: "+elapsed+" ms");
    }

}
