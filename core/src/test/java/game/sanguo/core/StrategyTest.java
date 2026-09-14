package game.sanguo.core;

import game.sanguo.core.strategy.StrategyRules;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.zip.CRC32;

/** Pure Java behavioral regressions. No mocks, Android, wall clock, or unseeded randomness. */
public final class StrategyTest {
    private static int checks;
    private static void check(boolean condition,String label){checks++;if(!condition)throw new AssertionError(label);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static World copy(World w)throws IOException{return SaveCodec.decode(SaveCodec.encode(w));}
    private static void reject(World w,Supplier<World.Result> action)throws Exception{
        byte[] before=SaveCodec.encode(w);check(!action.get().ok,"invalid command accepted");
        check(Arrays.equals(before,SaveCodec.encode(w)),"rejected command must preserve resources, actions, log AND random state");
    }
    private static void invalid(World w)throws Exception{
        try{SaveCodec.encode(w);throw new AssertionError("invalid state serialized");}catch(IOException expected){checks++;}
    }
    private static void badBytes(byte[] data)throws Exception{
        try{SaveCodec.decode(data);throw new AssertionError("corrupt/invalid save accepted");}catch(IOException expected){checks++;}
    }
    private static void badConfig(Runnable action){
        try{action.run();throw new AssertionError("invalid configuration accepted");}catch(IllegalArgumentException expected){checks++;}
    }
    private static World fixture(){
        World w=new World(22,16);
        w.cities.add(new World.City(10,"本城",new Hex(2,2),0));w.cities.add(new World.City(20,"友城",new Hex(9,2),0));
        w.cities.add(new World.City(30,"敌都",new Hex(19,12),1));w.cities.add(new World.City(40,"敌城",new Hex(5,5),1));
        for(int i=0;i<8;i++)w.officers.add(new World.Officer(i,"将"+i,0,10,80,70,80,60+i*5,90-i*5));
        w.officers.add(new World.Officer(90,"敌君",1,30,80,80,80,80,80));
        w.officers.add(new World.Officer(91,"敌将",1,40,80,80,80,80,80));
        w.officers.add(new World.Officer(92,"敌副将",1,40,80,80,80,80,80));
        w.city(10).gold=100000;
        for(int id:new int[]{20,30,40}){w.city(id).gold=0;w.city(id).food=0;w.city(id).troops=0;}
        w.strategy.initializeOffices();return w;
    }
    private static Strategy.Talent talent(int id,int city,int turn){return new Strategy.Talent(id,"隐士"+id,city,78,60,85,88,90,turn);}
    private static void next(World w)throws Exception{ok(w.nextTurn());SaveCodec.validate(w);}
    public static void main(String[] args)throws Exception{
        actions();search();probabilities();hiring();rewards();governors();administration();ai();saving();legacy();scenarioData();campaigns();
        System.out.println("PASS: "+checks+" strategy/officer assertions covering locks, seeded search/hiring, loyalty, offices, economy, finite reserves, readiness, AI and save v1-v4.");
    }
    private static void actions()throws Exception{
        World w=fixture();check(w.strategy.officerState(0).canAct,"initial officer available");
        ok(w.domestic.build(10,0,Domestic.Kind.MARKET,w.domestic.buildSites(10).get(0)));
        check(w.strategy.officerState(0).activity==Strategy.Activity.CONSTRUCTION,"construction derived from actual facility");
        reject(w,()->w.strategy.search(10,0));reject(w,()->w.strategy.beginAssignment(10,0,"other",2));next(w);
        check(!w.officer(0).acted&&!w.strategy.officerState(0).canAct,"reset does not unlock unfinished construction");
        reject(w,()->w.train(10,0));next(w);next(w);check(w.strategy.officerState(0).canAct,"completion restores action next旬");
        ok(w.domestic.transfer(10,20,0));check(w.strategy.officerState(0).activity==Strategy.Activity.TRANSFER&&w.officer(0).cityId==-1,"transfer location/status");
        reject(w,()->w.recruit(10,0));World clone=copy(w);next(w);next(clone);
        check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(clone)),"travel reset deterministic");
        while(w.domestic.busy(0))next(w);
        check(w.officer(0).cityId==20&&w.strategy.officerState(0).canAct,"arrival frees officer only after tick");
        World task=fixture();ok(task.strategy.beginAssignment(10,1,"统筹",2));
        reject(task,()->task.deploy(10,1,World.Weapon.SPEAR,3000));reject(task,()->task.domestic.transfer(10,20,1));
        reject(task,()->task.domestic.build(10,1,Domestic.Kind.FARM,task.domestic.buildSites(10).get(0)));
        next(task);check(task.strategy.officerState(1).activity==Strategy.Activity.OTHER_TASK&&task.officer(1).otherTaskTurns==1,"multi-turn assignment stays locked");
        reject(task,()->task.strategy.search(10,1));next(task);
        check(task.officer(1).otherTaskTurns==0&&task.officer(1).otherTask.isEmpty()&&task.strategy.officerState(1).canAct,"other assignment completes");
        World transport=fixture();ok(transport.domestic.transport(10,20,2,10,20,30,new int[]{40,0,0,0}));
        check(transport.strategy.officerState(2).activity==Strategy.Activity.TRANSPORT,"transport distinguished from transfer");
        reject(transport,()->transport.strategy.rewardOfficer(10,0,2));
        World deployed=fixture();ok(deployed.deploy(10,3,World.Weapon.SPEAR,3000));
        check(deployed.strategy.officerState(3).activity==Strategy.Activity.DEPLOYED,"field assignment derived from unit");
        reject(deployed,()->deployed.strategy.search(10,3));ok(deployed.enter(deployed.officer(3).unitId,10));
        check(deployed.strategy.officerState(3).activity==Strategy.Activity.ACTED,"return locks rest of current旬");next(deployed);
        check(deployed.strategy.officerState(3).canAct,"returned officer restored next旬");
        World invalid=fixture();reject(invalid,()->invalid.strategy.search(40,0));reject(invalid,()->invalid.strategy.search(10,999));
        reject(invalid,()->invalid.strategy.beginAssignment(10,0,"",2));reject(invalid,()->invalid.strategy.beginAssignment(10,0,"x",13));
        invalid.actionPoints[0]=9;reject(invalid,()->invalid.strategy.search(10,0));
        badConfig(()->invalid.strategy.officerState(999));badConfig(()->invalid.strategy.getArmyReadiness(999));
    }
    private static void search()throws Exception{
        World found=fixture();found.strategy.addHiddenTalent(talent(1000,10,0));found.strategy.setSeed(1);
        Strategy.SearchResult r=found.strategy.searchTalent(10,0);
        check(r.result.ok&&r.outcome==Strategy.SearchOutcome.OFFICER&&r.officerId==1000,"seeded search discovers eligible talent");
        check(found.officer(1000).owner==-1&&found.officer(1000).role==Strategy.Role.UNAFFILIATED&&found.officer(1000).cityId==10,"discovered officer stays unaffiliated in this city");
        check(found.strategy.hiddenTalents().isEmpty()&&!found.idle(found.city(10)).contains(found.officer(1000)),"discovery is neither a clone nor automatic employment");
        check(found.actionPoints[0]==50&&found.city(10).gold==100000&&found.officer(0).acted,"search AP and officer spent exactly once");
        reject(found,()->found.strategy.search(10,0));ok(found.strategy.search(10,1));
        check(found.officers.stream().filter(o->o.id==1000).count()==1,"same talent cannot be discovered twice");
        World nothing=fixture();nothing.strategy.addHiddenTalent(talent(1000,10,0));nothing.strategy.setSeed(4);
        r=nothing.strategy.searchTalent(10,0);check(r.outcome==Strategy.SearchOutcome.NOTHING&&nothing.strategy.hiddenTalents().size()==1,"seeded failure keeps undiscovered data");
        check(nothing.actionPoints[0]==50&&nothing.officer(0).acted,"unsuccessful search still consumes action");
        World gold=fixture();gold.strategy.addHiddenTalent(talent(1000,10,0));gold.strategy.setSeed(6);gold.city(10).gold=999990;
        r=gold.strategy.searchTalent(10,0);check(r.outcome==Strategy.SearchOutcome.GOLD&&r.goldFound==10&&gold.city(10).gold==1000000,"gold search respects storage cap and reports actual gain");
        World future=fixture();future.strategy.addHiddenTalent(talent(1000,10,2));future.strategy.addHiddenTalent(talent(1001,20,0));future.strategy.setSeed(1);
        check(future.strategy.searchTalent(10,0).outcome!=Strategy.SearchOutcome.OFFICER,"cannot reveal future or other-city talent");
        next(future);next(future);future.strategy.setSeed(1);check(future.strategy.searchTalent(10,0).officerId==1000,"available turn unlocks talent");
        int[] outcomes=new int[Strategy.SearchOutcome.values().length];
        for(int seed=0;seed<128;seed++){
            World a=fixture();a.strategy.setSeed(seed);a.strategy.addHiddenTalent(talent(1000,10,0));World b=copy(a);
            r=a.strategy.searchTalent(10,0);outcomes[r.outcome.ordinal()]++;b.strategy.search(10,0);
            check(Arrays.equals(SaveCodec.encode(a),SaveCodec.encode(b)),"seed replay search "+seed);
            b=copy(b);a.strategy.search(10,1);b.strategy.search(10,1);
            check(Arrays.equals(SaveCodec.encode(a),SaveCodec.encode(b)),"random stream continues across save "+seed);
        }
        check(outcomes[1]>0&&outcomes[2]>0&&outcomes[3]>0,"all three search outcomes exercised");
        World random=fixture();random.strategy.setSeed(1);
        check(random.strategy.nextInt(100)==32&&random.strategy.nextInt(100)==59&&random.strategy.nextInt(100)==95,"PRNG sequence pinned independently of save round trip");
        badConfig(()->random.strategy.addHiddenTalent(talent(0,10,0)));
        random.strategy.addHiddenTalent(talent(1000,10,0));badConfig(()->random.strategy.addHiddenTalent(talent(1000,20,0)));
        badConfig(()->random.strategy.addHiddenTalent(talent(1001,999,0)));
        badConfig(()->random.strategy.addHiddenTalent(new Strategy.Talent(1001,"bad",10,101,0,0,0,0,0)));
    }
    private static void probabilities(){
        check(StrategyRules.searchChance(0,0)==5&&StrategyRules.searchChance(100,100)==95,"search probability extremes");
        check(!StrategyRules.succeeds(0,0)&&StrategyRules.succeeds(100,99),"zero and certain boundaries");
        check(StrategyRules.succeeds(95,94)&&!StrategyRules.succeeds(95,95),"strict less-than probability threshold");
        badConfig(()->StrategyRules.succeeds(101,0));badConfig(()->StrategyRules.succeeds(50,100));
        for(int charm=0;charm<=100;charm+=10)for(int politics=0;politics<=100;politics+=10)for(int loyalty=0;loyalty<=100;loyalty+=10){
            int chance=StrategyRules.recruitmentChance(charm,politics,loyalty,false,0);
            check(chance>=0&&chance<=95,"bounded hire probability");
            check(StrategyRules.recruitmentChance(charm,politics,loyalty+10,false,0)<=chance,"loyalty monotonically resists defection");
        }
        check(StrategyRules.recruitmentChance(100,100,0,true,100)==95,"hire cap");
        check(StrategyRules.recruitmentChance(0,0,100,false,-100)==0,"hire floor");
        check(StrategyRules.recruitmentChance(80,80,40,false,80)>StrategyRules.recruitmentChance(80,80,40,false,-80),"relations materially affect hiring");
        check(StrategyRules.recruitmentChance(90,80,30,false,0)>StrategyRules.recruitmentChance(30,20,30,false,0),"abilities materially affect hiring");
        check(StrategyRules.recruitmentChance(Integer.MAX_VALUE,Integer.MAX_VALUE,-1,true,Integer.MAX_VALUE)==95,"extreme arguments cannot overflow");
    }
    private static void hiring()throws Exception{
        World success=fixture();success.officers.add(new World.Officer(1000,"在野",-1,10,80,80,80,80,80));success.strategy.setSeed(1);
        int gold=success.city(10).gold;ok(success.strategy.recruitOfficer(10,0,1000));
        World.Officer hired=success.officer(1000);
        check(hired.owner==0&&hired.cityId==10&&hired.role==Strategy.Role.OFFICER&&hired.acted,"new officer joins and rests");
        check(hired.loyalty==81&&success.city(10).gold==gold-100&&success.actionPoints[0]==50,"hire loyalty and resource formula");
        reject(success,()->success.strategy.search(10,1000));reject(success,()->success.strategy.recruitOfficer(10,1,1000));next(success);
        check(success.strategy.officerState(1000).canAct,"hired officer usable next旬");
        World failed=fixture();failed.officers.add(new World.Officer(1000,"在野",-1,10,80,80,80,80,80));failed.strategy.setSeed(6);
        ok(failed.strategy.recruitOfficer(10,0,1000));check(failed.officer(1000).owner==-1&&failed.city(10).gold==99900&&failed.actionPoints[0]==50,"failed hire consumes costs but does not transfer ownership");
        World enemy=fixture();enemy.officer(91).loyalty=40;enemy.city(40).governorId=91;enemy.officer(91).role=Strategy.Role.GOVERNOR;
        int neutral=enemy.strategy.recruitmentChance(10,0,91);enemy.strategy.setFactionRelation(0,1,100);
        check(enemy.strategy.recruitmentChance(10,0,91)>neutral&&enemy.strategy.factionRelation(1,0)==100,"symmetric scenario relationship modifier");
        enemy.strategy.setSeed(1);ok(enemy.strategy.recruitOfficer(10,0,91));
        check(enemy.officer(91).owner==0&&enemy.officer(91).cityId==10&&enemy.city(40).governorId==-1,"defection rehomes target and vacates enemy governorship");SaveCodec.validate(enemy);
        World protectedTarget=fixture();protectedTarget.officer(91).loyalty=61;
        reject(protectedTarget,()->protectedTarget.strategy.recruitOfficer(10,0,91));reject(protectedTarget,()->protectedTarget.strategy.recruitOfficer(10,0,90));
        protectedTarget.officer(91).loyalty=20;protectedTarget.officer(91).acted=true;
        reject(protectedTarget,()->protectedTarget.strategy.recruitOfficer(10,0,91));protectedTarget.officer(91).acted=false;
        protectedTarget.active=1;protectedTarget.city(40).gold=5000;
        ok(protectedTarget.domestic.build(40,91,Domestic.Kind.MARKET,protectedTarget.domestic.buildSites(40).get(0)));protectedTarget.active=0;protectedTarget.officer(91).acted=false;
        reject(protectedTarget,()->protectedTarget.strategy.recruitOfficer(10,0,91));
        World otherCity=fixture();otherCity.officers.add(new World.Officer(1000,"他城在野",-1,20,70,70,70,70,70));
        reject(otherCity,()->otherCity.strategy.recruitOfficer(10,0,1000));otherCity.officer(91).loyalty=0;otherCity.officer(91).cityId=30;
        reject(otherCity,()->otherCity.strategy.recruitOfficer(10,0,91));
        World poor=fixture();poor.officer(91).loyalty=40;poor.city(10).gold=99;reject(poor,()->poor.strategy.recruitOfficer(10,0,91));
        World zero=fixture();zero.officer(0).charm=0;zero.officer(0).politics=0;zero.officer(91).loyalty=60;zero.strategy.setFactionRelation(0,1,-100);zero.strategy.setSeed(1);
        check(zero.strategy.recruitmentChance(10,0,91)==0,"eligible target may still have zero success probability");ok(zero.strategy.recruitOfficer(10,0,91));check(zero.officer(91).owner==1,"zero chance cannot succeed");
    }
    private static void rewards()throws Exception{
        World poor=fixture();poor.city(10).gold=199;poor.officer(1).loyalty=40;reject(poor,()->poor.strategy.rewardOfficer(10,0,1));
        World capped=fixture();capped.officer(1).loyalty=99;ok(capped.strategy.rewardOfficer(10,0,1));
        check(capped.officer(1).loyalty==100&&capped.city(10).gold==99800,"reward clamps loyalty and deducts gold");reject(capped,()->capped.strategy.rewardOfficer(10,2,1));
        World once=fixture();once.officer(1).loyalty=40;once.officer(1).acted=true;ok(once.strategy.rewardOfficer(10,0,1));
        check(once.officer(1).acted&&once.officer(1).lastRewardTurn==0,"reward is passive for target but limited per旬");
        reject(once,()->once.strategy.rewardOfficer(10,2,1));next(once);ok(once.strategy.rewardOfficer(10,0,1));
        reject(once,()->once.strategy.rewardOfficer(10,2,0));reject(once,()->once.strategy.rewardOfficer(10,2,91));
        World different=fixture();different.officer(1).loyalty=50;different.officer(1).politics=0;different.officer(1).charm=0;
        different.officer(2).loyalty=50;different.officer(2).politics=100;different.officer(2).charm=100;
        ok(different.strategy.rewardOfficer(10,0,1));ok(different.strategy.rewardOfficer(10,3,2));
        check(different.officer(1).loyalty==64&&different.officer(2).loyalty==56,"officers have different reward response");
        World decay=fixture();decay.city(10).gold=0;decay.city(10).order=20;decay.officer(1).loyalty=1;
        next(decay);next(decay);next(decay);check(decay.officer(1).loyalty==0&&decay.officer(0).loyalty==100,"poor governance decay respects lower bound and ruler identity");
        World malformed=fixture();malformed.officer(1).loyalty=-1;invalid(malformed);malformed.officer(1).loyalty=101;invalid(malformed);
    }
    private static void governors()throws Exception{
        World w=fixture();w.officer(1).politics=80;ok(w.strategy.appointGovernor(10,0,1));
        check(w.city(10).governorId==1&&w.officer(1).role==Strategy.Role.GOVERNOR&&w.officer(0).acted&&w.officer(1).acted,"appointment locks executor and appointee");
        check(w.domestic.monthlyGold(10)==960&&w.domestic.monthlyFood(10)==6000,"governor politics changes actual yields");
        int gold=w.city(10).gold,food=w.city(10).food;next(w);next(w);next(w);
        check(w.city(10).gold==gold+960&&w.city(10).food==food-720+6000,"monthly settlement uses governor bonus, with upkeep");
        ok(w.domestic.transfer(10,20,1));check(w.city(10).governorId==-1&&w.officer(1).role==Strategy.Role.OFFICER&&w.domestic.monthlyGold(10)==800,"departing governor immediately loses city bonus");
        World replace=fixture();ok(replace.strategy.appointGovernor(10,0,1));next(replace);ok(replace.strategy.appointGovernor(10,0,2));
        check(replace.officer(1).role==Strategy.Role.OFFICER&&replace.city(10).governorId==2,"replacement removes old title");next(replace);
        ok(replace.deploy(10,2,World.Weapon.SPEAR,3000));check(replace.city(10).governorId==-1&&replace.officer(2).role==Strategy.Role.OFFICER,"deployment vacates governorship");
        World ruler=fixture();ok(ruler.strategy.appointGovernor(10,0,0));check(ruler.officer(0).role==Strategy.Role.RULER,"ruler can govern without losing ruler role");next(ruler);
        ok(ruler.deploy(10,0,World.Weapon.SPEAR,3000));check(ruler.officer(0).role==Strategy.Role.RULER&&ruler.city(10).governorId==-1,"ruler deployment preserves faction identity");
        World capture=fixture();ok(capture.strategy.appointGovernor(10,0,1));next(capture);
        capture.active=1;capture.officer(91).acted=false;capture.city(40).troops=6000;capture.city(40).food=12000;ok(capture.deploy(40,91,World.Weapon.SPEAR,3000));
        World.Unit unit=capture.unit(capture.officer(91).unitId);unit.hex=new Hex(3,2);capture.city(10).defense=1;ok(capture.siege(unit.id,10));
        check(capture.city(10).governorId==-1&&capture.officer(1).cityId==20&&capture.officer(1).role==Strategy.Role.OFFICER,"capture removes governorship and retreats officer");SaveCodec.validate(capture);
        World neutral=fixture();neutral.cities.add(new World.City(50,"空城",new Hex(12,10),-1));neutral.city(50).defense=1;
        neutral.officers.add(new World.Officer(1000,"当地在野",-1,50,80,80,80,80,80));ok(neutral.deploy(10,0,World.Weapon.SPEAR,3000));
        neutral.unit(1).hex=new Hex(13,10);ok(neutral.siege(1,50));
        check(neutral.officer(1000).cityId==50&&neutral.officer(1000).owner==-1,"neutral capture keeps unaffiliated residents in the city");SaveCodec.validate(neutral);
        World malformed=fixture();malformed.city(10).governorId=999;invalid(malformed);malformed.city(10).governorId=1;invalid(malformed);
        malformed.city(10).governorId=-1;malformed.officer(1).role=Strategy.Role.GOVERNOR;invalid(malformed);
    }
    private static void administration()throws Exception{
        World patrol=fixture();patrol.city(10).order=0;check(patrol.domestic.monthlyGold(10)==80,"zero order cuts real income");
        patrol.officer(1).politics=100;patrol.officer(1).charm=100;ok(patrol.patrol(10,1));
        check(patrol.city(10).order==15&&patrol.city(10).gold==99900&&patrol.domestic.monthlyGold(10)==200,"patrol restores order and yields");
        patrol.city(10).order=98;ok(patrol.patrol(10,2));check(patrol.city(10).order==100,"patrol caps at 100");reject(patrol,()->patrol.patrol(10,3));
        World poor=fixture();poor.city(10).gold=99;reject(poor,()->poor.patrol(10,0));reject(poor,()->poor.train(10,0));
        World reserve=fixture();reserve.city(10).recruitReserve=1300;int troops=reserve.city(10).troops;
        ok(reserve.recruit(10,0));check(reserve.city(10).troops==troops+1300&&reserve.city(10).recruitReserve==0,"recruitment draws a finite reserve exactly");
        check(reserve.city(10).gold==99700&&reserve.city(10).order==85,"recruitment costs gold and order");reject(reserve,()->reserve.recruit(10,1));
        World barracks=fixture();barracks.officer(0).politics=90;
        ok(barracks.domestic.build(10,0,Domestic.Kind.BARRACKS,barracks.domestic.buildSites(10).get(0)));next(barracks);next(barracks);
        check(barracks.strategy.recruitAmount(10,1)==2500,"completed barracks raise real recruitment");
        barracks.officer(1).charm=20;int low=barracks.strategy.recruitAmount(10,1);barracks.officer(1).charm=80;
        check(barracks.strategy.recruitAmount(10,1)>low,"charm affects recruitment");barracks.city(10).order=50;
        check(barracks.strategy.recruitAmount(10,1)<2500,"order affects recruitment");
        World blocked=fixture();blocked.city(10).gold=299;reject(blocked,()->blocked.recruit(10,0));blocked.city(10).gold=10000;blocked.city(10).order=29;
        reject(blocked,()->blocked.recruit(10,0));blocked.city(10).order=30;blocked.city(10).recruitReserve=10;
        ok(blocked.recruit(10,0));check(blocked.city(10).recruitReserve==0&&blocked.city(10).order==25,"minimum order and small reserve stay bounded");
        World training=fixture();training.city(10).morale=95;ok(training.strategy.trainArmy(10,0));
        check(training.getArmyReadiness(10)==100&&training.city(10).gold==99900,"readiness accessor and training cap");
        reject(training,()->training.train(10,1));ok(training.deploy(10,1,World.Weapon.SPEAR,3000));
        check(training.unit(1).energy==100,"existing deployment actually consumes city readiness as starting energy");
        World empty=fixture();empty.city(10).troops=0;reject(empty,()->empty.train(10,0));
        World full=fixture();full.city(10).troops=99000;reject(full,()->full.recruit(10,0));
        check(StrategyRules.trainingGain(100,100)>StrategyRules.trainingGain(0,0),"training depends on commander abilities");
        check(StrategyRules.patrolGain(100,100)>StrategyRules.patrolGain(0,0),"patrol depends on politics and charm");
    }
    private static World calm(){
        World w=fixture();World.City c=w.city(10);c.order=100;c.morale=100;c.troops=20000;c.recruitReserve=0;c.governorId=0;return w;
    }
    private static StrategicAi.Decision decision(World w,StrategicAi.Command expected)throws Exception{
        byte[] before=SaveCodec.encode(w);StrategicAi.Decision d=w.strategy.planAi(10);
        check(d!=null&&d.command==expected,"AI chooses "+expected+" rather than "+(d==null?"nothing":d.command));
        check(Arrays.equals(before,SaveCodec.encode(w)),"AI planning is read-only, including RNG");
        StrategicAi.Decision again=w.strategy.planAi(10);
        check(again.officerId==d.officerId&&again.targetId==d.targetId,"AI tie breaking is stable");
        ok(new StrategicAi(w).execute(d));SaveCodec.validate(w);return d;
    }
    private static void ai()throws Exception{
        World reward=calm();reward.officer(1).loyalty=20;decision(reward,StrategicAi.Command.REWARD);
        check(reward.officer(1).loyalty>20,"AI rewards dangerous core officer");
        World patrol=calm();patrol.city(10).order=10;decision(patrol,StrategicAi.Command.PATROL);check(patrol.city(10).order>10,"AI restores unrest");
        World hire=calm();hire.officers.add(new World.Officer(1000,"在野",-1,10,80,80,80,80,80));hire.strategy.setSeed(1);
        decision(hire,StrategicAi.Command.HIRE);check(hire.officer(1000).owner==0,"AI actually recruits local talent");
        World recruit=calm();recruit.city(10).troops=1000;recruit.city(10).recruitReserve=1000;
        decision(recruit,StrategicAi.Command.RECRUIT);check(recruit.city(10).recruitReserve==0&&recruit.city(10).troops==2000,"AI uses finite reserve");
        World train=calm();train.city(10).morale=10;decision(train,StrategicAi.Command.TRAIN);check(train.city(10).morale>10,"AI trains existing army");
        World search=calm();search.strategy.addHiddenTalent(talent(1000,10,0));search.strategy.setSeed(1);
        decision(search,StrategicAi.Command.SEARCH);check(search.officer(1000)!=null,"AI discovers an actual officer");
        World appoint=calm();appoint.city(10).governorId=-1;decision(appoint,StrategicAi.Command.APPOINT);check(appoint.city(10).governorId==7,"AI appoints best available administrator");
        World poor=calm();poor.city(10).gold=0;poor.city(10).order=1;decision(poor,StrategicAi.Command.SEARCH);
        poor.actionPoints[0]=0;check(poor.strategy.planAi(10)==null,"AI respects exhausted AP");
        World border=calm();border.city(10).troops=9000;border.city(10).recruitReserve=20000;
        check(border.strategy.planAi(10)==null,"peaceful city does not recruit forever");
        int pressure=border.strategy.strategicPressure(10);border.active=1;border.city(30).troops=6000;border.city(30).food=12000;
        ok(border.deploy(30,90,World.Weapon.SPEAR,3000));border.unit(1).hex=new Hex(3,3);border.active=0;
        check(border.strategy.strategicPressure(10)>pressure,"nearby enemy troops increase pressure");decision(border,StrategicAi.Command.RECRUIT);
        World ordinary=ScenarioCatalog.load("regional-sandbox",0);ordinary.strategy.setSeed(1);int officers=ordinary.officers.size();next(ordinary);
        check(ordinary.officers.size()>officers,"nextTurn integrates AI search before it deploys all administrators");
    }
    private static int marker(byte[] bytes){
        for(int i=20;i<bytes.length-4;i++)if(ByteBuffer.wrap(bytes,i,4).getInt()==0x53545234)return i;
        throw new AssertionError("strategy marker not found");
    }
    private static byte[] crc(byte[] bytes){CRC32 crc=new CRC32();crc.update(bytes,20,bytes.length-20);ByteBuffer.wrap(bytes,12,8).putLong(crc.getValue());return bytes;}
    private static void saving()throws Exception{
        World w=fixture();w.strategy.setSeed(-123456789L);w.strategy.setFactionRelation(0,1,-35);
        w.strategy.addHiddenTalent(talent(1000,10,4));w.officers.add(new World.Officer(2000,"在野",-1,10,50,50,50,50,50));
        ok(w.strategy.appointGovernor(10,0,1));w.officer(3).loyalty=40;ok(w.strategy.rewardOfficer(10,2,3));
        ok(w.strategy.beginAssignment(10,4,"筹备",3));ok(w.domestic.build(10,5,Domestic.Kind.MARKET,w.domestic.buildSites(10).get(0)));
        ok(w.domestic.transport(10,20,6,100,500,100,new int[]{100,0,0,0}));ok(w.strategy.search(10,7));
        w.city(10).recruitReserve=4321;w.city(10).morale=33;
        byte[] bytes=SaveCodec.encode(w);check(ByteBuffer.wrap(bytes,4,4).getInt()==6,"writes save v6");World restored=SaveCodec.decode(bytes);
        check(Arrays.equals(bytes,SaveCodec.encode(restored)),"all v4 state has exact binary round-trip");
        check(restored.city(10).governorId==1&&restored.city(10).recruitReserve==4321&&restored.city(10).morale==33,"governor reserve readiness persisted");
        check(restored.officer(3).lastRewardTurn==0&&restored.officer(4).otherTaskTurns==3,"reward guard and task persisted");
        check(restored.strategy.factionRelation(0,1)==-35&&restored.strategy.hiddenTalents().size()==1&&restored.officer(2000).owner==-1,"relation hidden/free populations persisted");
        for(int i=0;i<10&&!w.gameOver();i++){next(w);next(restored);check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"round-trip continuation is deterministic at "+i);}
        World bad=fixture();bad.city(10).recruitReserve=-1;invalid(bad);bad.city(10).recruitReserve=1000001;invalid(bad);
        bad=fixture();bad.officer(1).role=Strategy.Role.RULER;bad.officer(1).loyalty=100;invalid(bad);
        bad=fixture();bad.officer(1).otherTaskTurns=2;invalid(bad);bad.officer(1).otherTask="筹备";bad.officer(1).otherTaskTurns=13;invalid(bad);
        bad=fixture();bad.officer(1).lastRewardTurn=bad.turn+1;invalid(bad);
        bad=fixture();bad.officers.add(new World.Officer(2000,"在野",-1,10,50,50,50,50,50));bad.officer(2000).role=Strategy.Role.OFFICER;invalid(bad);
        bad=fixture();bad.strategy.talents.add(talent(1,10,0));invalid(bad);
        bad=fixture();bad.strategy.relations.put(1L,0);invalid(bad);
        bad=fixture();ok(bad.domestic.build(10,1,Domestic.Kind.FARM,bad.domestic.buildSites(10).get(0)));bad.officer(1).otherTaskTurns=1;bad.officer(1).otherTask="conflict";invalid(bad);
        byte[] valid=SaveCodec.encode(fixture());byte[] broken=valid.clone();broken[broken.length-1]^=1;badBytes(broken);badBytes(Arrays.copyOf(valid,valid.length-3));
        broken=valid.clone();ByteBuffer.wrap(broken,4,4).putInt(100);badBytes(broken);
        int pos=marker(valid);broken=valid.clone();ByteBuffer.wrap(broken,pos+20,4).putInt(-1);badBytes(crc(broken));
        broken=valid.clone();ByteBuffer.wrap(broken,pos+12,4).putInt(3);badBytes(crc(broken));
        broken=valid.clone();broken[pos+16+12*4+4+8]=(byte)127;badBytes(crc(broken));
        broken=valid.clone();ByteBuffer.wrap(broken,pos+24,4).putInt(99999);badBytes(crc(broken));
        broken=valid.clone();ByteBuffer.wrap(broken,pos+16+12,4).putInt(10);badBytes(crc(broken));
    }
    private static byte[] resource(String name)throws IOException{
        try(InputStream in=StrategyTest.class.getResourceAsStream(name)){if(in==null)throw new IOException("Missing fixture "+name);return in.readAllBytes();}
    }
    private static void legacy()throws Exception{
        String[] names={"/m0-v1.sg11.b64","/m1-v2.sg11.b64","/v03-strategy.sg11.b64"};
        for(int v=1;v<=3;v++){
            byte[] bytes=Base64.getMimeDecoder().decode(resource(names[v-1]));check(ByteBuffer.wrap(bytes,4,4).getInt()==v,"genuine archived v"+v+" fixture");
            World w=SaveCodec.decode(bytes);for(World.City c:w.cities)check(c.recruitReserve==20000&&c.governorId==-1,"legacy city defaults");
            for(World.Officer o:w.officers){check(o.loyalty==(o.role==Strategy.Role.RULER?100:85),"legacy loyalty/role defaults");check(o.otherTask.isEmpty()&&o.otherTaskTurns==0&&o.lastRewardTurn==-1,"legacy optional task defaults");}
            check(w.strategy.hiddenTalents().isEmpty()&&w.strategy.relations.isEmpty(),"migration does not invent scenario talent or diplomacy");
            if(v==3){
                check(w.domestic.facilities.size()==1&&w.domestic.facilities.get(0).remaining==2,"v3 pending construction preserved");
                check(w.strategy.officerState(1000).activity==Strategy.Activity.CONSTRUCTION&&w.strategy.officerState(1001).activity==Strategy.Activity.TRANSPORT,"v3 locks reconstructed from actual records");
                Domestic.Mission m=w.domestic.missions.get(0);check(m.gold==100&&m.food==500&&m.troops==100&&m.equipment[0]==100,"v3 cargo preserved exactly");
            }
            byte[] upgraded=SaveCodec.encode(w);check(ByteBuffer.wrap(upgraded,4,4).getInt()==6,"legacy resaves as v6");World paired=SaveCodec.decode(upgraded);
            for(int i=0;i<5&&!w.gameOver();i++){next(w);next(paired);check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(paired)),"legacy migration deterministic continuation v"+v);}
        }
    }
    private static void badScenario(String text)throws Exception{
        try{ScenarioData.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),0);throw new AssertionError("invalid scenario accepted");}catch(IOException expected){checks++;}
    }
    private static void scenarioData()throws Exception{
        World regional=ScenarioCatalog.load("regional-sandbox",0),small=ScenarioCatalog.load("m0-skirmish",0);
        check(regional.officers.size()==18&&regional.strategy.hiddenTalents().size()==3,"new regional optional hidden pool preserves initial roster");
        check(small.officers.size()==6&&small.strategy.hiddenTalents().size()==2,"small scenario gets a local searchable pool");
        check(regional.officers.stream().filter(o->o.role==Strategy.Role.RULER).count()==3,"one ruler per starting faction");
        String data=new String(resource("/scenarios/regional-sandbox.properties"),StandardCharsets.UTF_8);
        String old=data.substring(0,data.indexOf("# Fictional"));World legacy=ScenarioData.read(new ByteArrayInputStream(old.getBytes(StandardCharsets.UTF_8)),0);
        check(legacy.officers.size()==18&&legacy.strategy.hiddenTalents().isEmpty(),"existing format-1 packs without talent fields still load");
        badScenario(data.replace("910000|","1000|"));badScenario(data.replace("910000|杜衡|100|","910000|杜衡|99999|"));
        badScenario(data.replace("910000|杜衡|100|76|","910000|杜衡|100|101|"));badScenario(data.replace("84|80|0","84|80|-1"));
        badConfig(()->regional.strategy.setFactionRelation(0,0,20));badConfig(()->regional.strategy.setFactionRelation(0,1,101));
        badConfig(()->regional.strategy.setFactionRelation(0,99,20));regional.strategy.setFactionRelation(0,1,20);
        check(regional.strategy.factionRelation(1,0)==20,"basic relation symmetric");regional.strategy.setFactionRelation(0,1,0);
        check(regional.strategy.factionRelation(1,0)==0&&regional.strategy.relations.isEmpty(),"neutral relation canonical default");
    }
    private static void campaigns()throws Exception{
        for(int player=0;player<3;player++){
            World a=ScenarioCatalog.load("regional-sandbox",player);a.strategy.setSeed(20260913L+player);World b=copy(a);int turns=0;
            while(turns<60&&!a.gameOver()){
                new StrategicAi(a).run(true);new StrategicAi(a).run(false);new StrategicAi(b).run(true);new StrategicAi(b).run(false);
                check(Arrays.equals(SaveCodec.encode(a),SaveCodec.encode(b)),"same player-side decisions after save");next(a);next(b);turns++;
                check(Arrays.equals(SaveCodec.encode(a),SaveCodec.encode(b)),"multi-faction campaign deterministic "+player+"/"+turns);
                for(World.City c:a.cities)check(c.recruitReserve>=0&&c.troops>=0&&c.gold>=0&&c.order>=0&&c.order<=100,"campaign resource bounds");
                for(World.Officer o:a.officers){check(o.loyalty>=0&&o.loyalty<=100,"campaign loyalty bounds");if(a.domestic.busy(o.id)||a.strategy.busy(o.id))check(!a.strategy.officerState(o.id).canAct,"campaign no busy officer can act");}
                if(turns%7==0)b=copy(b);
            }
            check(turns>=6,"campaign exercises multiple seasons and strategic AI");
        }
    }
}
