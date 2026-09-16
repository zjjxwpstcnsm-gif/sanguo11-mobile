package game.sanguo.core;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Synthetic maps, seed 27016. Independent conservation ledger; no official-map claim. */
public final class TacticalLogisticsTest {
    static int checks;
    static void check(boolean yes,String reason){checks++;if(!yes)throw new AssertionError(reason);}
    static void ok(World.Result r){check(r.ok,r.message);}
    static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    static World copy(World w)throws Exception{return SaveCodec.decode(bytes(w));}
    static World fixture(){World w=LogisticsCampaignTest.fixture();w.strategy.setSeed(27016);return w;}
    static void tick(World w){w.turn++;w.domestic.tick();}
    static Domestic.Mission send(World w,boolean sea,boolean returning){ok(w.domestic.transport(11,12,4,new int[]{5,6},700,10000,2000,new int[]{1000,0,0,0},sea,returning));return w.domestic.missions.get(0);}
    public static void main(String[] args)throws Exception{identity();combat();water();migration();permissions();waypoint();queue();escort();frontline();twoYears();LogisticsCampaignTest.campaign(8000,true);System.out.println("PASS: "+checks+" v027 tactical logistics assertions.");}
    static void identity()throws Exception{
        World w=fixture();Domestic.Mission m=send(w,false,true);long[] before=stock(w);tick(w);
        check(w.unitAt(m.hex)==m&&w.unit(m.id)==m,"one object is both cargo owner and actual battlefield target");check(m.food==9900,"one actual ration");
        byte[] state=bytes(w);MarchOrders.Plan preview=w.marches.preview(m.id,new Hex(m.hex.q+1,m.hex.r));check(Arrays.equals(state,bytes(w)),"preview is pure");
        Hex position=m.hex;ok(w.marches.stop(m.id));int food=m.food;tick(w);check(m.hex.equals(position)&&m.food==food-100,"stop retains cargo and position, still eats");
        World restored=copy(w);check(((Domestic.Mission)restored.unit(m.id)).stopped,"stop survives save");
        ok(w.marches.execute(w.marches.preview(m.id,w.city(12).hex)));int spent=m.movementSpent;byte[] executed=bytes(w);check(!w.marches.execute(preview).ok&&Arrays.equals(executed,bytes(w)),"stale movement confirmation cannot execute again");
        check(spent<=4&&w.orders.remaining(m)<=4-spent,"manual move cannot grant extra movement");
        for(int i=0;i<20&&m.transport;i++)tick(w);check(m.returning&&!m.transport&&m.gold==0&&m.food==0&&m.troops==0,"unloads exactly once, only personnel return");
        for(int i=0;i<20&&w.domestic.mission(m.id)!=null;i++)tick(w);
        check(w.officer(4).cityId==11&&w.officer(5).cityId==11&&w.officer(6).cityId==11,"three crew return to actual origin");
        long[] after=stock(w);check(after[0]==before[0]&&after[1]+m.consumedFood==before[1]&&after[2]==before[2],"full journey conserves gold soldiers and exact consumed grain");
        World blocked=fixture();Domestic.Mission b=send(blocked,false,false);tick(blocked);World.Unit friend=StrategicManagementTest.unit(blocked,7,World.Weapon.SPEAR,b.hex.neighbors().get(0));
        check(!blocked.orders.previewMove(b.id,friend.hex).valid()&&!blocked.orders.previewMove(friend.id,b.hex).valid(),"both actors respect shared occupancy");
        Hex h=b.hex;for(Hex n:h.neighbors())if(!n.equals(friend.hex))blocked.terrain[n.q][n.r]=World.Terrain.MOUNTAIN;tick(blocked);check(b.hex.equals(h)&&b.waiting.contains("受阻"),"blocked convoy waits with reason");
        blocked.terrain[friend.hex.q][friend.hex.r]=World.Terrain.PLAIN;friend.hex=new Hex(2,4); // fixture removes an obstruction, not production teleportation
        for(Hex n:h.neighbors())blocked.terrain[n.q][n.r]=World.Terrain.PLAIN;tick(blocked);check(!b.hex.equals(h),"opening route resumes legal movement");
    }
    static void combat()throws Exception{
        World w=fixture();Domestic.Mission m=send(w,false,false);tick(w);World.Unit attacker=StrategicManagementTest.unit(w,20,World.Weapon.SPEAR,m.hex.neighbors().get(0));w.active=1;w.officer(20).skillId=Skill.BOFU.id;
        int food=attacker.food+m.food,gold=attacker.gold+m.gold;World direct=copy(w);ok(direct.attack(attacker.id,m.id));ok(w.supply.raid(attacker.id,m.id));check(Arrays.equals(bytes(w),bytes(direct)),"raid command uses identical normal combat RNG and consequences");
        for(int i=0;i<10&&w.domestic.mission(m.id)!=null;i++){w.orders.reset(attacker);ok(w.attack(attacker.id,m.id));}
        check(w.domestic.mission(m.id)==null&&w.unit(m.id)==null,"defeat removes map and report target together");
        check(attacker.food==food&&attacker.gold==gold,"only actual gold/grain transferred, no treasury teleport");
        check(w.government.captive(4)&&w.government.captive(5)&&w.government.captive(6),"all three crew enter real captor escort registry");
        byte[] once=bytes(w);check(!w.supply.raid(attacker.id,m.id).ok&&Arrays.equals(once,bytes(w)),"repeated defeat cannot pay loot twice");
    }
    static void water()throws Exception{
        World w=fixture();for(int q=9;q<=11;q++)for(int r=0;r<w.height;r++)w.terrain[q][r]=World.Terrain.WATER;
        w.city(11).ships[0]=3;w.city(11).ships[1]=2;
        byte[] before=bytes(w);check(!w.domestic.transport(11,12,4,0,5000,1000,new int[4]).ok&&Arrays.equals(before,bytes(w)),"land-only transport cannot cross river");
        ok(w.domestic.transport(11,12,4,new int[]{5,6},700,10000,2000,new int[]{1000,0,0,0},true,true,new int[]{2,1}));Domestic.Mission m=w.domestic.missions.get(0);
        check(w.city(11).ships[0]==1&&w.city(11).ships[1]==1,"ship cargo debited once from existing stock");boolean afloat=false;int used=0;
        for(int i=0;i<12&&m.transport;i++){tick(w);afloat|=w.army.water(m.hex);used=m.consumedFood;World saved=copy(w);check(Arrays.equals(bytes(w),bytes(saved)),"water conversion and cargo exact save roundtrip");check(m.ship==Army.Ship.BOAT,"active default boat never consumes cargo ships");}
        check(afloat&&m.returning&&used>0&&w.city(12).ships[0]==2&&w.city(12).ships[1]==1,"inland-water-inland delivery uses actual route, one ship inventory credit");
        World supply=fixture();Domestic.Mission c=send(supply,false,false);tick(supply);World.Unit ally=StrategicManagementTest.unit(supply,7,World.Weapon.SPEAR,c.hex.neighbors().get(0));int total=c.troops+ally.troops,grain=c.food+ally.food,money=c.gold+ally.gold,gear=c.equipment[0];
        ok(supply.supply.convoyTransfer(c.id,ally.id,500,1000,100));check(c.troops+ally.troops==total&&c.food+ally.food==grain&&c.gold+ally.gold==money&&c.equipment[0]==gear-500,"field supply conserves real soldiers food gold and matching equipment");
        before=bytes(supply);check(!supply.supply.convoyTransfer(c.id,ally.id,500,1000,100).ok&&Arrays.equals(before,bytes(supply)),"supply ends actor action and cannot repeat");
    }
    static void migration()throws Exception{
        byte[] old;try(InputStream in=TacticalLogisticsTest.class.getResourceAsStream("/legacy-v20-logistics.sg11.b64")){old=Base64.getMimeDecoder().decode(in.readAllBytes());}
        check(old[7]==20,"actual unmodified v026 encoder fixture");World w=SaveCodec.decode(old);Domestic.Mission m=w.domestic.missions.get(0);
        check(m.food==4950&&m.consumedFood==50&&m.crew().length==3&&m.returnOfficers,"old paid ration, cargo, crew and return option retained");
        check(w.unitAt(m.hex)==m,"old task gains same-object battlefield identity");World replay=copy(w);
        for(int i=0;i<10;i++){tick(w);tick(replay);check(Arrays.equals(bytes(w),bytes(replay)),"v20 continuation equals v21 roundtrip");replay=copy(replay);byte[] done=bytes(w);w.domestic.tick();check(Arrays.equals(done,bytes(w)),"saved last settlement cannot move or consume twice");}
    }
    static void permissions()throws Exception{
        World w=fixture();ok(w.districts.configure(-1,"后勤军",new int[]{11},Districts.Policy.ECONOMY,-1,12,false,false));w.districts.reset(0);ok(w.districts.requestSupport(11,12));Domestic.Mission m=w.domestic.missions.get(0);tick(w);
        byte[] before=bytes(w);check(!w.marches.stop(m.id).ok&&!w.supply.convoyTransfer(m.id,1,0,10,0).ok&&Arrays.equals(before,bytes(w)),"delegated convoy cannot bypass command permissions");
        int sourceGold=w.city(11).gold;w.districts.get(1).supplyEnabled=false;before=bytes(w);check(!w.districts.requestSupport(11,12).ok&&Arrays.equals(before,bytes(w))&&w.city(11).gold==sourceGold,"forbidden transport no fallback spending");
    }

    static void waypoint()throws Exception{
        World w=fixture();Domestic.Mission m=send(w,false,false);tick(w);Hex goal=new Hex(10,11);
        ok(w.marches.execute(w.marches.preview(m.id,goal)));check(m.march!=null,"waypoint spans more than current budget");
        for(int i=0;i<8&&m.march!=null;i++)tick(w);check(m.hex.equals(goal)&&m.stopped,"manual multi-turn waypoint completes then waits");
        Hex at=m.hex;int food=m.food;tick(w);check(m.hex.equals(at)&&m.food==food-100,"waypoint does not silently resume old city order");
    }
    static void queue()throws Exception{
        World w=fixture();for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++)w.terrain[q][r]=World.Terrain.MOUNTAIN;
        for(World.City c:w.cities)w.terrain[c.hex.q][c.hex.r]=World.Terrain.PLAIN;
        for(int q=3;q<=15;q++)w.terrain[q][15]=World.Terrain.PLAIN;
        w.terrain[10][14]=World.Terrain.PLAIN;w.terrain[10][16]=World.Terrain.PLAIN;
        Domestic.Mission a=send(w,false,false);ok(w.domestic.transport(12,11,8,new int[]{9,10},300,10000,2000,new int[4],false,false));Domestic.Mission b=w.domestic.missions.get(1);
        long[] before=stock(w);Set<String> positions=new HashSet<>();
        for(int i=0;i<30&&!w.domestic.missions.isEmpty();i++){tick(w);SaveCodec.validate(w);check(a.movementSpent<=4&&b.movementSpent<=4,"queuing/yield shares budget");if(a.transport&&b.transport&&w.cityAt(a.hex)==null&&w.cityAt(b.hex)==null)check(!a.hex.equals(b.hex),"two-way convoy cannot occupy same choke");positions.add(a.hex+"/"+b.hex);}
        check(w.domestic.missions.isEmpty(),"two-way narrow lane with passing bay eventually delivers both");check(positions.size()>3,"queue makes actual progress");long[] after=stock(w);check(after[0]==before[0]&&after[1]+a.consumedFood+b.consumedFood==before[1]&&after[2]==before[2],"passing bay has no teleport or refund");
    }
    static void escort()throws Exception{
        World w=fixture();Domestic.Mission m=send(w,false,false);tick(w);World.Unit threat=StrategicManagementTest.unit(w,20,World.Weapon.SPEAR,new Hex(11,15));
        Hex at=m.hex;tick(w);check(m.hex.equals(at)&&m.waiting.contains("护军"),"valuable convoy waits for actual nearby protection");
        World.Unit guard=StrategicManagementTest.unit(w,7,World.Weapon.SPEAR,new Hex(7,13));AiOrders.Order intent=w.aiOrders.get(guard);intent.home=10;intent.target=20;int homeTroops=w.city(10).troops;
        new CampaignAi(w).runUnit(guard,true,c->c.id==20,c->c.id==10);check(m.escortId==guard.id&&guard.acted,"existing army takes actual escort order and action");check(w.city(10).troops==homeTroops&&guard.movementSpent<=w.war.movement(guard),"escort invents no troops or movement");
        check(w.aiOrders.describe(guard).contains("护送")&&intent.stalled==0,"legitimate escort wait is not stuck");
        World defended=fixture();Domestic.Mission c=send(defended,false,false);tick(defended);World.Unit defender=StrategicManagementTest.unit(defended,7,World.Weapon.SPEAR,new Hex(4,3));AiOrders.Order defense=defended.aiOrders.get(defender);defense.home=10;defense.defending=true;
        StrategicManagementTest.unit(defended,20,World.Weapon.SPEAR,new Hex(5,2));new CampaignAi(defended).runUnit(defender,true,x->true,x->x.id==10);check(c.escortId<0&&defender.hex.distance(defended.city(10).hex)<=7,"threatened home defender never diverted to convoy");
        // Out-of-range convoy does not even enter local attack scoring.
        w.active=1;threat.hex=new Hex(30,15);CampaignAi ai=new CampaignAi(w);CampaignAi.Action action=ai.bestAction(threat.id,true);check(action==null||action.target!=m.id,"distant convoy cannot attract local combat action");
    }
    static void frontline()throws Exception{
        World w=fixture();Domestic.Mission m=send(w,false,false);tick(w);World.Unit hungry=StrategicManagementTest.unit(w,7,World.Weapon.SPEAR,m.hex.neighbors().get(0));hungry.food=600;int total=m.food+hungry.food;AiOrders.Order order=w.aiOrders.get(hungry);order.home=11;order.target=20;
        new CampaignAi(w).runUnit(hungry,true,c->c.id==20,c->c.owner==0);check(m.acted&&hungry.food>600&&m.food+hungry.food==total,"frontline AI receives actual adjacent convoy grain without creating supply");
        World denied=fixture();Domestic.Mission foreign=send(denied,false,false);tick(denied);World.Unit unit=StrategicManagementTest.unit(denied,7,World.Weapon.SPEAR,foreign.hex.neighbors().get(0));unit.food=600;
        ok(denied.districts.configure(-1,"战区",new int[]{12},Districts.Policy.ECONOMY,-1,-1,false,false));denied.districts.units.put(unit.id,1);int grain=foreign.food;new CampaignAi(denied).runUnit(unit,true,c->true,c->c.id==12);check(foreign.food==grain,"delegated frontline cannot siphon first-district convoy");
    }
    static long[] stock(World w){long[] a=new long[14];for(World.City c:w.cities){a[0]+=c.gold;a[1]+=c.food;a[2]+=c.troops;for(int i=0;i<9;i++)a[3+i]+=c.equipment[i];for(int i=0;i<2;i++)a[12+i]+=c.ships[i];}for(Domestic.Mission m:w.domestic.missions){a[0]+=m.gold;a[1]+=m.food;a[2]+=m.troops;for(int i=0;i<9;i++)a[3+i]+=m.equipment[i];for(int i=0;i<2;i++)a[12+i]+=m.cargoShips[i];}return a;}
    static long income(World w,World.City c,boolean food){
        int base=food?5000:800;for(Domestic.Facility f:w.domestic.facilities)if(f.cityId==c.id&&f.remaining==0){check(f.kind==Domestic.Kind.FARM||f.kind==Domestic.Kind.MARKET,"ledger admits no unaccounted support facilities");if(f.kind==(food?Domestic.Kind.FARM:Domestic.Kind.MARKET))base+=food?3750:600;}
        World.Officer governor=w.officer(c.governorId);int bonus=governor==null?0:governor.politics/4;
        return (long)base*(10+c.order)*(100+bonus)/10000; // Independent documented fixture arithmetic, no income API calls.
    }
    static void twoYears()throws Exception{
        World w=fixture();w.officers.removeIf(o->o.owner==1);w.city(20).governorId=-1;w.city(11).food=52000;w.city(11).gold=6500;w.city(12).food=4000;w.city(12).troops=12000;
        w.cities.add(new World.City(13,"第二后方",new Hex(3,8),0));for(int i=30;i<35;i++)w.officers.add(new World.Officer(i,"后勤将"+i,0,13,75,70,80,88,85));w.city(13).food=100000;w.city(13).gold=25000;
        w.campaign.treaties.add(new Campaign.Treaty(0,1,Campaign.TreatyKind.ALLIANCE,100));
        ok(w.districts.configure(-1,"二军",new int[]{11,12},Districts.Policy.ECONOMY,-1,12,false,false));ok(w.districts.configure(-1,"三军",new int[]{13},Districts.Policy.ECONOMY,-1,12,false,false));
        World replay=copy(w);Set<Integer> dispatched=new HashSet<>();boolean built=false,received=false;int purchases=0;
        for(int turn=1;turn<=72;turn++){
            w.log.clear();replay.log.clear();long[] expected=stock(w);Set<Integer> oldFacilities=new HashSet<>();for(Domestic.Facility f:w.domestic.facilities)oldFacilities.add(f.id);w.districts.run();replay.districts.run();
            for(String log:w.log){Matcher buy=Pattern.compile("买入(\\d+)粮，支出(\\d+)金").matcher(log);if(buy.find()){expected[1]+=Integer.parseInt(buy.group(1));expected[0]-=Integer.parseInt(buy.group(2));purchases++;}}
            // Real build starts create one facility and pay listed cost; detect via pre/post stable IDs.
            for(Domestic.Facility f:w.domestic.facilities)if(!oldFacilities.contains(f.id))expected[0]-=f.kind==Domestic.Kind.FARM?800:1000;
            for(String log:w.log){if(log.contains("巡察，"))expected[0]-=100;if(log.contains("训练，"))expected[0]-=100;}
            long[] command=stock(w);for(int i=0;i<14;i++)check(command[i]==expected[i],"no hidden troop/gear/food creation during permitted orders at "+turn+" index "+i+" expected "+expected[i]+" got "+command[i]+" log="+w.log);
            for(Domestic.Facility f:w.domestic.facilities)if(f.remaining>0&&f.builderId>=0&&w.officer(f.builderId).acted)built=true;
            long cityUse=0,convoyUse=0;for(World.City c:w.cities)cityUse+=(c.troops+49)/50;for(Domestic.Mission m:w.domestic.missions)if(m.transport){convoyUse+=Math.min(m.food,(m.troops+19)/20);dispatched.add(m.taskId);}
            ok(w.nextTurn());ok(replay.nextTurn());long[] after=stock(w);long gold=0,food=0;
            for(World.City c:w.cities){if(turn%3==0)gold+=income(w,c,false);if(turn%9==0)food+=income(w,c,true);}
            check(after[0]==command[0]+gold,"independent monthly gold ledger at "+turn);
            check(after[1]==command[1]+food-cityUse-convoyUse,"independent seasonal grain/city/transport ledger at "+turn+": expected "+(command[1]+food-cityUse-convoyUse)+" got "+after[1]);
            for(int i=2;i<14;i++)check(after[i]==command[i],"settlement preserves troops/gear/ships index "+i+" at "+turn);
            check(Arrays.equals(bytes(w),bytes(replay)),"72-turn full state saved replay at "+turn);replay=copy(replay);
            check(w.units.isEmpty()&&w.districts.get(1).reserveFood==40000&&w.districts.get(2).reserveFood==40000,"no attack and player reserves unchanged");
            Set<Integer> people=new HashSet<>();for(World.Officer o:w.officers){check(people.add(o.id),"no duplicate personnel");check(o.cityId>=0||w.domestic.busy(o.id),"every officer has actual city or task");}
            received|=w.city(12).food>4000;
            System.out.println("V027 LEDGER "+turn+" gold="+command[0]+"+"+gold+"="+after[0]+" food="+command[1]+"+"+food+"-"+cityUse+"-"+convoyUse+"="+after[1]);
        }
        check(built&&received&&dispatched.size()>=3,"two years include production recovery and repeated actual transport");
        check(w.city(11).food>40000&&new DistrictManagement(w).residents(w.city(11))>=2,"rear retains grain and people after two years");check(purchases<12,"production recovery avoids permanent food-market dependence");
    }
}
