package game.sanguo.core;

import java.util.*;

/** Regressions for borders, real automated administration and reacting to ranged attackers. */
public final class TerritoryAiTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static World fixture(){
        World w=new World(30,20,"我军","敌军");
        w.cities.add(new World.City(10,"后方",new Hex(2,3),0));
        w.cities.add(new World.City(11,"前线",new Hex(12,3),0));
        w.cities.add(new World.City(20,"敌城",new Hex(26,3),1));
        for(World.City c:w.cities){c.gold=30000;c.food=100000;c.troops=20000;c.order=90;c.morale=90;}
        for(int id=0;id<6;id++)w.officers.add(new World.Officer(id,"我将"+id,0,id<3?10:11,80,80,80,80,80));
        for(int id=20;id<24;id++)w.officers.add(new World.Officer(id,"敌将"+id,1,20,80,80,80,80,80));
        w.strategy.initializeOffices();return w;
    }
    private static World.Unit unit(World w,int officer,World.Weapon weapon,int q,int r){
        World.Officer o=w.officer(officer);w.strategy.releaseGovernor(officer);
        World.Unit u=new World.Unit(w.nextUnitId++,o.owner,officer,weapon,new Hex(q,r),6000,20000);
        o.cityId=-1;o.unitId=u.id;w.units.add(u);return u;
    }
    public static void main(String[] args)throws Exception{
        borders();geography();nation();rangedPressure();facilityTarget();delegation();fairness();
        System.out.println("PASS: "+checks+" territory/AI assertions: catchments, capture/treaties, gates/ports, map bounds, ranged pressure, district budgets/fairness/save replay.");
    }
    private static void borders()throws Exception{
        World w=fixture();w.city(11).kind=World.SiteKind.GATE;w.city(20).kind=World.SiteKind.PORT;
        byte[] before=SaveCodec.encode(w);Territory map=new Territory(w);
        check(!map.frontline(10)&&map.frontline(11)&&map.frontline(20),"frontline is border adjacency, rear city is not frontline");
        int total=0;for(World.City c:w.cities){check(map.siteAt(c.hex)==c.id&&map.size(c.id)>0,"every city gate and port owns its seed and catchment");total+=map.size(c.id);}
        check(total==w.width*w.height,"every open map tile belongs to exactly one site");
        boolean internal=false;for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){
            if(map.siteAt(q,r)==10&&map.boundary(q,r,true)!=map.boundary(q,r,false))internal=true;
        }
        check(internal,"same-faction site boundaries disappear in the union view");
        check(Arrays.equals(before,SaveCodec.encode(w)),"territory inspection consumes no RNG and changes no save");
        Collections.reverse(w.cities);Territory reverse=new Territory(w);
        for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++)check(reverse.siteAt(q,r)==map.siteAt(q,r),"site-list order does not change borders");
        w.city(20).owner=0;check(map.ownerAt(26,3)==0&&!map.frontline(11),"capture changes colors/frontlines without stale geometry");
        w.city(20).owner=1;w.campaign.treaties.add(new Campaign.Treaty(0,1,Campaign.TreatyKind.ALLIANCE,10));
        check(!map.frontline(11)&&!map.frontline(20),"alliance is not a hostile frontline");
        w.campaign.treaties.clear();check(map.frontline(11),"expired treaty restores frontline");
        w.city(20).owner=-1;check(!map.frontline(11)&&map.ownerAt(26,3)==-1,"neutral site retains its own catchment without hostile front");
    }
    private static void geography(){
        World w=fixture();for(int r=0;r<w.height;r++)w.terrain[18][r]=World.Terrain.MOUNTAIN;
        Territory map=new Territory(w);check(!map.frontline(11),"mountain wall does not invent a traversable border");
        for(int r=0;r<w.height;r++)check(map.siteAt(18,r)==-1,"impassable mountains remain untinted");
        w.terrain[18][3]=World.Terrain.MOUNTAIN_PATH;map=new Territory(w);check(map.frontline(11),"a real pass links catchments");
        check(map.siteAt(-1,0)==-1&&map.siteAt(w.width,0)==-1&&map.siteAt((Hex)null)==-1,"safe outside bounds");
    }
    private static void nation()throws Exception{
        World w=new World(299,200,"甲","乙");w.sourceMapWidth=200;
        for(int q=0;q<w.width;q++)Arrays.fill(w.terrain[q],World.Terrain.MOUNTAIN);
        for(int x=0;x<200;x++)for(int y=0;y<200;y++){Hex h=MapCoordinates.axial(x,y,200);w.terrain[h.q][h.r]=World.Terrain.PLAIN;}
        for(int i=0;i<87;i++)w.cities.add(new World.City(i,"据点"+i,MapCoordinates.axial(5+i%10*20,5+i/10*20,200),i%2));
        long start=System.nanoTime();Territory map=new Territory(w);int count=0;
        for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++)if(map.siteAt(q,r)>=0)count++;
        check(count==40000,"200x200 source map covers 40000 cells, never skew-padding cells");
        System.out.println("Territory 200x200 / 87 sites: "+(System.nanoTime()-start)/1000000+" ms");
    }
    private static void rangedPressure()throws Exception{
        for(World.Weapon weapon:new World.Weapon[]{World.Weapon.SPEAR,World.Weapon.HALBERD,World.Weapon.CAVALRY,World.Weapon.SWORD}){
            World w=fixture();World.Unit defender=unit(w,21,weapon,10,10),archer=unit(w,1,World.Weapon.CROSSBOW,12,10);
            check(w.attack(archer.id,defender.id).ok,"archer fires from two tiles away");
            w.active=1;Hex start=defender.hex;World replay=SaveCodec.decode(SaveCodec.encode(w));
            new CampaignAi(w).runUnit(defender,true,c->true,c->c.owner==1);
            new CampaignAi(replay).runUnit(replay.unit(defender.id),true,c->true,c->c.owner==1);
            check(!start.equals(defender.hex)&&defender.hex.distance(archer.hex)==1,"melee closes on the archer before consuming its action: "+weapon);
            check(defender.acted&&(archer.troops<6000||archer.status!=War.Status.NORMAL||archer.burning>0),"movement is followed by an effective attack/control command");
            check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"engagement remains deterministic after load");
        }
        World w=fixture();World.Unit a=unit(w,21,World.Weapon.SPEAR,10,10),b=unit(w,1,World.Weapon.CROSSBOW,12,10);w.active=1;a.energy=0;
        new CampaignAi(w).runUnit(a,true,c->true,c->c.owner==1);check(a.hex.distance(b.hex)==1&&b.troops<6000,"zero-energy melee still moves and attacks");
        w=fixture();a=unit(w,21,World.Weapon.SPEAR,10,10);b=unit(w,1,World.Weapon.CROSSBOW,12,10);w.active=1;Hex original=a.hex;
        new CampaignAi(w).runUnit(a,false,c->false,c->false);check(a.hex.equals(original)&&b.troops==6000,"attack-disabled armies respect permission");
        w=fixture();a=unit(w,21,World.Weapon.SPEAR,10,10);b=unit(w,1,World.Weapon.CROSSBOW,12,10);w.active=1;a.status=War.Status.CONFUSED;a.statusTurns=2;original=a.hex;
        new CampaignAi(w).runUnit(a,true,c->true,c->c.owner==1);check(a.hex.equals(original)&&b.troops==6000,"confusion remains a valid reason to be unable to move");
    }
    private static void delegation()throws Exception{
        World w=fixture();check(w.districts.configure(-1,"第2军团",new int[]{11},Districts.Policy.ECONOMY,-1,-1,false,true).ok,"create district");
        w.turn=1;w.districts.reset(0);int main=w.actionPoints[0],gold=w.city(11).gold;
        World saved=SaveCodec.decode(SaveCodec.encode(w));w.districts.run();saved.districts.run();
        check(w.domestic.count(11)>0&&w.city(11).gold<gold,"district actually constructs and pays from its city");
        check(w.actionPoints[0]==main&&w.districts.all().get(0).points()<60,"independent budget never steals first district AP");
        check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(saved)),"delegation resumes identically after save/load");
        byte[] once=SaveCodec.encode(w);w.districts.run();check(Arrays.equals(once,SaveCodec.encode(w)),"district cannot execute twice in the same turn");
        w.actionPoints[0]=60;check(w.districts.dissolve(w.districts.all().get(0).id).ok&&w.districts.directCity(11),"dissolve restores direct management");
        w=fixture();check(w.districts.configure(-1,"守备",new int[]{11},Districts.Policy.DEFENSE,-1,-1,false,false).ok,"defense district");
        w.city(11).order=40;w.city(11).morale=30;w.turn=1;w.districts.reset(0);int troops=w.city(11).troops;int[] gear=w.city(11).equipment.clone();w.districts.run();
        check(w.city(11).order>40&&w.city(11).troops>=troops,"defense repairs public order without sending out its garrison");
        check(Arrays.equals(gear,w.city(11).equipment)&&w.units.isEmpty(),"disabled production and attack respected");
    }
    private static void facilityTarget(){
        World w=fixture();World.Unit bow=unit(w,1,World.Weapon.CROSSBOW,23,3);bow.energy=0;
        Domestic.Facility farm=new Domestic.Facility(w.domestic.nextFacilityId++,20,Domestic.Kind.FARM,new Hex(25,3),-1,0);w.domestic.facilities.add(farm);
        int before=farm.hp;CampaignAi ai=new CampaignAi(w);CampaignAi.Action action=ai.bestAction(bow.id,true);
        check(action!=null&&action.kind==CampaignAi.Kind.FACILITY&&ai.execute(action).ok&&farm.hp<before,"AI attacks enemy economic facilities with actual damage");
        w.orders.reset(bow);check(ai.bestAction(bow.id,false)==null,"attack-disabled delegation cannot damage a facility");
    }
    private static void fairness(){
        World w=fixture();List<Integer> members=new ArrayList<>();
        for(int i=0;i<8;i++){
            int id=40+i;World.City c=new World.City(id,"托管城"+i,new Hex(2+i*3,16),0);c.gold=5000;c.order=50;w.cities.add(c);members.add(id);
            w.officers.add(new World.Officer(100+i,"城将"+i,0,id,80,80,80,80,80));
        }
        check(w.districts.configure(-1,"后方",members.stream().mapToInt(i->i).toArray(),Districts.Policy.DEFENSE,-1,-1,false,false).ok,"large district");
        for(int turn=1;turn<=3;turn++){w.turn=turn;for(World.Officer o:w.officers)o.acted=false;w.districts.reset(0);w.districts.run();}
        for(int id:members)check(w.city(id).order>50,"limited AP eventually services every district city: "+id);
    }
}
