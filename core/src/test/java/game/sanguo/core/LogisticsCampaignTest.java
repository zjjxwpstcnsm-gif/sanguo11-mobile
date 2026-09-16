package game.sanguo.core;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Fixed seed regression through real commands; synthetic geometry is explicitly not official terrain. */
public final class LogisticsCampaignTest {
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    private static void ok(World.Result result){check(result.ok,result.message);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    private static World copy(World w)throws Exception{return SaveCodec.decode(bytes(w));}
    private static void tick(World w){w.turn++;w.domestic.tick();}
    static World fixture(){World w=StrategicManagementTest.fixture();for(int i=8;i<12;i++)w.officer(i).cityId=12;w.strategy.initializeOffices();return w;}
    private static long[] stock(World w){long[] out=new long[3+World.Weapon.values().length];for(World.City c:w.cities){out[0]+=c.gold;out[1]+=c.food;out[2]+=c.troops;for(int i=0;i<c.equipment.length;i++)out[3+i]+=c.equipment[i];}for(Domestic.Mission m:w.domestic.missions){out[0]+=m.gold;out[1]+=m.food;out[2]+=m.troops;for(int i=0;i<m.equipment.length;i++)out[3+i]+=m.equipment[i];}return out;}
    public static void main(String[] args)throws Exception{migration();transport();starvationAndBlock();capacityAndRedirect();raidAndDeath();permissionsAndSupport();localProgress();zoneSearch();longEconomy();campaign();System.out.println("PASS: "+checks+" v026 logistics/campaign assertions.");}
    private static void migration()throws Exception{
        byte[] raw;try(InputStream in=LogisticsCampaignTest.class.getResourceAsStream("/legacy-v19-logistics.sg11.b64")){raw=Base64.getMimeDecoder().decode(in.readAllBytes());}
        check(raw[7]==19,"actual old encoder fixture");World w=SaveCodec.decode(raw);check(w.city(10).gold==29400,"old paid 100 fee preserved without refund or recharge");
        check(w.domestic.missions.size()==1&&w.domestic.missions.get(0).deputies.length==0&&!w.domestic.missions.get(0).returnOfficers,"legacy convoy no invented personnel or return policy");
        check(w.districts.get(1).reserveTroops()==14000&&w.aiOrders.orders.size()==1,"old settings and intention survive");
        World restored=copy(w);check(bytes(w)[7]==20&&Arrays.equals(bytes(w),bytes(restored)),"v20 exact roundtrip");
        for(int t=0;t<5;t++){ok(w.nextTurn());ok(restored.nextTurn());check(Arrays.equals(bytes(w),bytes(restored)),"old pending convoy and AI resume identically");restored=copy(restored);}
    }
    private static void transport()throws Exception{
        World w=fixture();long[] before=stock(w);int people=w.officers.size(),ap=w.actionPoints[0];byte[] preview=bytes(w);
        String detail=w.domestic.transportPreview(11,12,4,new int[]{5,6},700,5000,1000,new int[]{200,0,0,0},false,true);
        check(detail.contains("派遣费0金")&&detail.contains("途中耗粮")&&Arrays.equals(preview,bytes(w)),"preview exact/pure");
        ok(w.domestic.transport(11,12,4,new int[]{5,6},700,5000,1000,new int[]{200,0,0,0},false,true));
        check(Arrays.equals(before,stock(w))&&w.actionPoints[0]==ap-10,"one AP charge no gold fee, exact departure conservation");
        Domestic.Mission m=w.domestic.missions.get(0);check(m.crew().length==3&&w.domestic.busy(5),"three officers occupied");
        byte[] sent=bytes(w);check(!w.domestic.transport(11,12,4,1,1,1,new int[4]).ok&&Arrays.equals(sent,bytes(w)),"double confirmation cannot resend acted courier");
        int cost=0,steps=0;while(m.transport&&steps++<10){int use=w.domestic.foodUse(m);tick(w);cost+=use;World replay=copy(w);byte[] processed=bytes(replay);replay.domestic.tick();check(Arrays.equals(processed,bytes(replay)),"same saved tick cannot consume/deliver twice");}
        before[1]-=cost;check(Arrays.equals(before,stock(w)),"actual arriving cargo equals loaded cargo minus counted food");
        check(m.returning&&!m.transport&&m.gold==0&&m.food==0&&m.troops==0&&Arrays.stream(m.equipment).sum()==0,"only personnel return, all cargo unloaded once");
        check(w.officer(5).cityId==-1&&w.domestic.busy(6),"return journey is real travel");
        for(int i=0;i<10&&!w.domestic.missions.isEmpty();i++)tick(w);
        check(w.domestic.missions.isEmpty()&&w.officer(4).cityId==11&&w.officer(5).cityId==11&&w.officer(6).cityId==11,"all three return and unlock");check(w.officers.size()==people&&Arrays.equals(before,stock(w)),"return creates no resources or people");
        World duplicate=fixture();byte[] original=bytes(duplicate);check(!duplicate.domestic.transport(11,12,4,new int[]{4},0,2000,1000,new int[4],false,false).ok&&Arrays.equals(original,bytes(duplicate)),"duplicate crew rejected atomically");
    }
    private static void starvationAndBlock()throws Exception{
        World w=fixture();ok(w.domestic.transport(11,12,4,500,20,1000,new int[4]));Domestic.Mission m=w.domestic.missions.get(0);
        Hex start=m.hex;for(Hex h:start.neighbors())w.terrain[h.q][h.r]=World.Terrain.MOUNTAIN;
        tick(w);check(m.food==0&&m.troops==900&&m.hex.equals(start),"blocked convoy still eats real cargo and loses troops on famine");
        check(w.domestic.status(m).contains("受阻"),"blocked status");World b=copy(w);tick(w);tick(b);check(Arrays.equals(bytes(w),bytes(b)),"starving save replay");
        for(Hex h:start.neighbors())w.terrain[h.q][h.r]=World.Terrain.PLAIN;tick(w);check(!m.hex.equals(start),"opened route resumes ordinary movement");
        World enemy=fixture();ok(enemy.domestic.transport(11,12,4,0,5000,1000,new int[4]));Domestic.Mission convoy=enemy.domestic.missions.get(0);Hex next=enemy.domestic.route(convoy.hex,enemy.city(12).hex,0).get(0);StrategicManagementTest.unit(enemy,20,World.Weapon.SPEAR,next);tick(enemy);check(!convoy.hex.equals(next),"cannot move through actual hostile occupancy");
    }
    private static void capacityAndRedirect()throws Exception{
        World w=fixture();World.City target=w.city(12);target.gold=w.campaign.goldCap(target);long[] before=stock(w);
        ok(w.domestic.transport(11,12,4,500,5000,1000,new int[4]));Domestic.Mission m=w.domestic.missions.get(0);for(int i=0;i<4;i++)tick(w);
        check(m.hex.equals(target.hex)&&w.domestic.status(m).contains("满仓"),"full capacity retains actual cargo");int consumed=m.consumedFood;before[1]-=consumed;check(Arrays.equals(before,stock(w)),"full warehouse waiting food accounted");
        World saved=copy(w);ok(w.domestic.redirect(m.id,11));ok(saved.domestic.redirect(m.id,11));for(int i=0;i<5;i++){tick(w);tick(saved);check(Arrays.equals(bytes(w),bytes(saved)),"redirect and return exact replay");}
        check(w.domestic.missions.isEmpty()&&w.officer(4).cityId==11,"cancel by return delivers once to source");
        World lost=fixture();ok(lost.domestic.transport(11,12,4,new int[]{5},500,5000,1000,new int[4],false,true));tick(lost);lost.city(12).owner=1;for(int i=8;i<12;i++){lost.officer(i).cityId=11;}for(int i=0;i<10&&!lost.domestic.missions.isEmpty();i++)tick(lost);
        check(lost.officer(4).cityId>=0&&lost.city(lost.officer(4).cityId).owner==0&&lost.officer(5).cityId==lost.officer(4).cityId,"lost destination redirects all crew safely");
    }
    private static void raidAndDeath()throws Exception{
        World w=fixture();ok(w.domestic.transport(11,12,4,new int[]{5,6},500,5000,1000,new int[4],false,false));Domestic.Mission m=w.domestic.missions.get(0);tick(w);
        w.life.die(4,"test");check(m.officerId==5&&m.deputies.length==1&&m.food<5000,"living deputy inherits same cargo");
        World.Unit raider=StrategicManagementTest.unit(w,20,World.Weapon.SPEAR,m.hex.neighbors().get(0));raider.troops=10000;raider.gold=0;raider.food=10000;w.active=1;
        int loot=m.food;ok(w.supply.raid(raider.id,m.id));check(w.domestic.missions.isEmpty()&&raider.food==10000+loot&&raider.gold==500,"raid transfers only existing cargo once");
        check((w.government.captive(5)||w.officer(5).cityId>=0)&&(w.government.captive(6)||w.officer(6).cityId>=0),"all remaining crew captured or escaped");w.active=0;SaveCodec.validate(w);
    }
    private static void permissionsAndSupport()throws Exception{
        World w=fixture();ok(w.districts.configure(-1,"后方军",new int[]{11},Districts.Policy.ECONOMY,-1,-1,false,true));ok(w.districts.configure(-1,"前线军",new int[]{12},Districts.Policy.DEFENSE,-1,-1,false,false));w.districts.reset(0);
        w.city(12).food=0;w.city(12).troops=2000;Districts.District donor=w.districts.get(1);byte[] before=bytes(w);
        check(!w.districts.requestSupport(11,12).ok&&Arrays.equals(before,bytes(w)),"no cross district draw without explicit target authorization");
        w.actionPoints[0]=60;ok(w.districts.configure(1,"后方军",new int[]{11},Districts.Policy.ECONOMY,-1,12,false,true));w.districts.reset(0);
        before=bytes(w);DistrictManagement.SupplyPlan p=w.districts.supportPlan(11,12);check(p.valid()&&Arrays.equals(before,bytes(w)),"authorized request preview read only");
        int mainAp=w.actionPoints[0],groupAp=donor.points;ok(w.districts.requestSupport(11,12));check(donor.points==groupAp-10&&w.actionPoints[0]==mainAp,"support consumes donor real AP only");
        check(w.city(11).troops>=donor.reserveTroops&&new DistrictManagement(w).residents(w.city(11))>=2,"real guards/admin retained");
        for(int i=0;i<12&&!w.domestic.missions.isEmpty();i++)tick(w);check(w.city(12).food>0&&w.city(12).troops>2000,"authorized cross district request dispatched and arrived");
        donor.supplyEnabled=false;before=bytes(w);check(!w.districts.requestSupport(11,12).ok&&Arrays.equals(before,bytes(w)),"forbidden transport has no fallback bypass");
        donor.supplyEnabled=true;donor.points=0;before=bytes(w);check(!w.districts.requestSupport(11,12).ok&&Arrays.equals(before,bytes(w)),"exhausted district cannot spend main budget");
        check(donor.reserveFood==40000&&donor.reserveGold==5000,"forecast does not overwrite player reserves");
    }
    private static void localProgress()throws Exception{
        World w=fixture();World.Unit stuck=StrategicManagementTest.unit(w,4,World.Weapon.RAM,new Hex(10,8));for(Hex h:stuck.hex.neighbors())w.terrain[h.q][h.r]=World.Terrain.MOUNTAIN;
        World.Unit archer=StrategicManagementTest.unit(w,5,World.Weapon.CROSSBOW,new Hex(31,20)),enemy=StrategicManagementTest.unit(w,20,World.Weapon.SPEAR,new Hex(33,20));enemy.troops=16000;
        for(int i=0;i<4;i++){w.turn++;w.orders.reset(archer);w.orders.reset(stuck);ok(w.attack(archer.id,enemy.id));new CampaignAi(w).runUnit(stuck,true,c->true,c->c.owner==0);}
        check(w.aiOrders.get(stuck).stalled>=4,"distant real fighting does not hide blocked unit");
        w.terrain[9][8]=World.Terrain.PLAIN;w.turn++;w.orders.reset(stuck);new CampaignAi(w).runUnit(stuck,true,c->true,c->c.owner==0);check(!stuck.hex.equals(new Hex(10,8)),"blocked unit leaves when legal path opens");
    }
    private static void zoneSearch()throws Exception{
        World w=fixture();World.Unit u=StrategicManagementTest.unit(w,4,World.Weapon.SPEAR,new Hex(8,8));World.Unit enemy=StrategicManagementTest.unit(w,20,World.Weapon.SPEAR,new Hex(10,8));
        for(int q=9;q<13;q++)for(int r=6;r<10;r++)w.terrain[q][r]=World.Terrain.WATER;
        int n=1;for(War.StructureKind kind:War.StructureKind.values())w.war.structures.add(new War.Structure(n++,1,kind,new Hex(5+n,15),kind.hp));
        for(World.Weapon weapon:new World.Weapon[]{World.Weapon.SPEAR,World.Weapon.RAM})for(Skill skill:new Skill[]{Skill.TUIJIN,Skill.DUNZOU,Skill.FEIJIANG,Skill.NENGLI}){
            World.Unit probe=new World.Unit(u.id,u.owner,u.officerId,weapon,u.hex,u.troops,u.food);w.officer(4).skillId=skill.id;java.util.function.Predicate<Hex> zone=w.advancedBattle.zoneForSearch(probe);boolean same=true;
            for(int q=0;q<w.width;q++)for(int r=0;r<w.height;r++){Hex h=new Hex(q,r);same&=zone.test(h)==w.advancedBattle.zone(probe,h);}
            check(same,"per-search zone matches live rules for all terrain, structures, weapon and skill");
        }
        enemy.hex=new Hex(3,3);check(w.advancedBattle.zoneForSearch(u).test(new Hex(3,4))==w.advancedBattle.zone(u,new Hex(3,4)),"next search sees moved enemy without persistent cache");
    }
    private static void longEconomy()throws Exception{
        World w=fixture();w.city(12).food=4000;w.city(12).troops=12000;w.city(11).food=52000;w.city(11).gold=6500;for(int id=8;id<12;id++)w.officer(id).cityId=11;
        w.cities.add(new World.City(13,"第三军仓",new Hex(4,5),0));for(int i=30;i<35;i++)w.officers.add(new World.Officer(i,"仓将"+i,0,13,75,70,80,88,85));w.city(13).food=100000;w.city(13).gold=25000;
        w.officers.removeIf(o->o.owner==1);w.city(20).governorId=-1;
        w.campaign.treaties.add(new Campaign.Treaty(0,1,Campaign.TreatyKind.ALLIANCE,60));
        ok(w.districts.configure(-1,"第二军",new int[]{11,12},Districts.Policy.ECONOMY,-1,12,false,true));ok(w.districts.configure(-1,"第三军",new int[]{13},Districts.Policy.ECONOMY,-1,12,false,true));
        for(World.City c:w.cities)Arrays.fill(c.equipment,0);World replay=copy(w);Set<Integer> missions=new HashSet<>();boolean transfer=false,transport=false,production=false,received=false;int farms=0;
        for(int t=0;t<36;t++){
            w.log.clear();replay.log.clear();World beforeOrders=copy(w);
            w.districts.run();replay.districts.run();auditOrders(beforeOrders,w);
            long[] settled=stock(w);long convoyFood=0,cityFood=0;for(Domestic.Mission m:w.domestic.missions)convoyFood+=Math.min(m.food,w.domestic.foodUse(m));for(World.City c:w.cities)cityFood+=w.cityFoodUse(c);
            check(w.government.ranks.isEmpty()&&!w.events.enabled(),"audit fixture has no unaccounted payroll or random disaster");
            ok(w.nextTurn());ok(replay.nextTurn());
            long[] after=stock(w);long goldIncome=0,foodIncome=0;for(World.City c:w.cities){goldIncome+=w.domestic.goldIncome(c.id,w.turn);foodIncome+=w.domestic.foodIncome(c.id,w.turn);}
            check(after[0]==settled[0]+goldIncome,"season gold equals paid-command balance plus scheduled income at "+w.turn);
            check(after[1]==settled[1]+foodIncome-cityFood-convoyFood,"season food accounts for every city/convoy ration and harvest at "+w.turn);
            for(int i=2;i<after.length;i++)check(after[i]==settled[i],"settlement/arrival conserves troops/equipment index "+i+" at "+w.turn);
            System.out.println("LEDGER turn "+w.turn+": gold "+settled[0]+" + income "+goldIncome+" = "+after[0]+"; food "+settled[1]+" + harvest "+foodIncome+" - city "+cityFood+" - convoy "+convoyFood+" = "+after[1]);check(Arrays.equals(bytes(w),bytes(replay)),"36-turn multi district save/reload deterministic at "+t);replay=copy(replay);
            for(Domestic.Mission m:w.domestic.missions){missions.add(m.id);transfer|=!m.transport&&!m.returning;transport|=m.transport;}
            production|=w.city(11).equipment[0]>0||w.city(13).equipment[0]>0;received|=new DistrictManagement(w).residents(w.city(12))>0;
            for(World.City c:w.cities){check(c.gold>=0&&c.food>=0&&c.troops>=0,"nonnegative actual inventory");}
            check(w.units.isEmpty(),"both districts respect no attack over all seasons");
            check(w.officers.size()==17,"no invented staff");
            check(w.districts.get(1).reserveFood==40000&&w.districts.get(2).reserveFood==40000,"explicit reserves stable");
            if(t%9==8)System.out.println("v026 season "+(t+1)+": food="+stock(w)[1]+", gold="+stock(w)[0]+", tasks="+missions.size()+", source staff="+new DistrictManagement(w).residents(w.city(11)));
        }
        for(Domestic.Facility f:w.domestic.facilities)if(f.kind==Domestic.Kind.FARM&&f.remaining==0)farms++;
        check(transfer&&transport&&production&&received&&farms>0,"36 turns include real transfers, transport arrivals, paid production and completed farms");
        check(missions.size()>=3,"repeated actual logistics survives multiple seasons");check(w.city(11).food>40000&&new DistrictManagement(w).residents(w.city(11))>=3,"rear recovers above grain reserve and keeps usable staff after repeated exports");
    }
    private static void auditOrders(World before,World after){
        long[] expected=stock(before);Set<Integer> old=new HashSet<>();for(Domestic.Facility f:before.domestic.facilities)old.add(f.id);
        for(Domestic.Facility f:after.domestic.facilities)if(!old.contains(f.id))expected[0]-=f.kind.cost;
        check(after.log.size()<40,"audit log has not truncated any command");
        for(String line:after.log){
            Matcher production=Pattern.compile("生产(\\d+)份(.+)兵装，金−(\\d+)").matcher(line);
            if(production.find()){expected[0]-=Integer.parseInt(production.group(3));for(World.Weapon weapon:World.Weapon.values())if(weapon.label.equals(production.group(2)))expected[3+weapon.ordinal()]+=Integer.parseInt(production.group(1));}
            Matcher buy=Pattern.compile("买入(\\d+)粮，支出(\\d+)金").matcher(line);if(buy.find()){expected[1]+=Integer.parseInt(buy.group(1));expected[0]-=Integer.parseInt(buy.group(2));}
            Matcher recruit=Pattern.compile("征得(\\d+)兵").matcher(line);if(recruit.find()){expected[2]+=Integer.parseInt(recruit.group(1));expected[0]-=Strategy.RECRUIT_COST;}
            if(line.contains("巡察，"))expected[0]-=Strategy.PATROL_COST;if(line.contains("训练，"))expected[0]-=Strategy.TRAIN_COST;
            if(line.contains("搜索")){expected[0]-=Strategy.SEARCH_COST;Matcher found=Pattern.compile("搜索获得金(\\d+)").matcher(line);if(found.find())expected[0]+=Integer.parseInt(found.group(1));}
        }
        check(Arrays.equals(expected,stock(after)),"all command costs, produced equipment, purchases and search income reconcile: "+after.log);
    }
    private static void campaign()throws Exception{
        World w=fixture();w.city(20).troops=16000;w.city(20).defense=2000;w.officers.removeIf(o->o.owner==1);
        for(int i=30;i<36;i++)w.officers.add(new World.Officer(i,"援将"+i,0,12,85,85,80,70,80));
        for(World.City c:w.cities)if(c.owner==0){c.troops=42000;c.food=240000;c.gold=40000;}
        w.city(11).equipment[World.Weapon.RAM.ordinal()]=2;w.officer(4).aptitude[4]=3;w.city(12).equipment[2]=20000;
        for(int q=23;q<=24;q++)for(int rr=0;rr<w.height;rr++)w.terrain[q][rr]=rr==15?World.Terrain.PLAIN:World.Terrain.MOUNTAIN;
        ok(w.districts.configure(-1,"联合攻城",new int[]{11,12},Districts.Policy.CITY_ATTACK,20,-1,true,true));
        Set<Integer> departed=new HashSet<>(),weapons=new HashSet<>();boolean staging=false,crossed=false,damage=false;int minDef=w.city(20).defense;
        for(int t=0;t<40&&w.city(20).owner==1;t++){
            ok(w.nextTurn());SaveCodec.validate(w);
            for(World.Unit u:w.units)if(u.owner==0){AiOrders.Order o=w.aiOrders.orders.get(u.id);if(o!=null){departed.add(o.home);staging|=o.staging;}weapons.add(u.weapon.ordinal());crossed|=u.hex.q>=25;}
            damage|=w.city(20).defense<minDef||w.city(20).troops<16000-w.turn*400;minDef=Math.min(minDef,w.city(20).defense);
            if(t%5==4)System.out.println("v026 campaign turn "+w.turn+" units="+w.units.size()+" defender="+w.city(20).troops+" hp="+w.city(20).defense+" origins="+departed);
        }
        check(departed.contains(11)&&departed.contains(12),"actual departures from two cities");check(staging,"actual staging observed");check(crossed,"real march traverses one tile choke");check(damage||w.city(20).owner==0,"actual mixed campaign causes combat result");check(weapons.size()>=2,"mixed weapon departures");
    }
}
