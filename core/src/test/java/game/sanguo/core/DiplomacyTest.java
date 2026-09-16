package game.sanguo.core;

import java.io.*;
import java.util.*;
import java.util.function.*;

/** Integration checks across diplomacy, actual armies, cargo, prisoners, PK, and old saves. */
public final class DiplomacyTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static void ok(World.Result result){check(result.ok,result.message);}
    static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    static World copy(World w)throws Exception{return SaveCodec.decode(bytes(w));}
    static void rejected(World w,Supplier<World.Result> command)throws Exception{byte[] before=bytes(w);check(!command.get().ok,"rejected");check(Arrays.equals(before,bytes(w)),"rejection is atomic including RNG");}
    static World world(){
        World w=new World(32,18,"汉营","盟营","敌营");
        w.cities.add(new World.City(0,"汉都",new Hex(2,3),0));w.cities.add(new World.City(1,"汉后方",new Hex(2,12),0));
        w.cities.add(new World.City(10,"盟都",new Hex(10,3),1));w.cities.add(new World.City(20,"敌都",new Hex(26,3),2));
        for(World.City c:w.cities){c.gold=30000;c.food=150000;c.troops=c.owner==0?60000:18000;c.morale=90;
            for(int i=0;i<5;i++)w.officers.add(new World.Officer(c.id*10+i,"将"+(c.id*10+i),c.owner,c.id,80,80,85,90,95));}
        w.strategy.initializeOffices();w.strategy.setFactionRelation(0,1,100);w.campaign.concludeTreaty(0,1,Campaign.TreatyKind.ALLIANCE,30);return w;
    }
    static void seed(World w,Function<World,World.Result> command,Predicate<World> outcome)throws Exception{
        for(int i=0;i<1000;i++){w.strategy.setSeed(i);World c=copy(w);World.Result r=command.apply(c);if(r.ok&&outcome.test(c))return;}throw new AssertionError("no deterministic outcome seed");
    }
    static void reset(World w){Arrays.fill(w.actionPoints,60);for(World.Officer o:w.officers)o.acted=false;}
    public static void main(String[] args)throws Exception{
        aidLifecycle();aidInvalidation();exchange();surrender();restoredSkills();migration();
        System.out.println("PASS: "+checks+" diplomacy assertions: actual allied expedition dispatch/return, pure previews, treaties/capacity/stale requests, atomic prisoner exchange, whole-force absorption/cargo/tasks, eight restored PK skills, v16 migration and deterministic saves.");
    }
    static void aidLifecycle()throws Exception{
        World w=world();byte[] before=bytes(w);
        check(w.diplomacy.aidError(0,1,10,20,1000)==null,"valid ally can supply expedition");
        CampaignAi.Deployment p=w.diplomacy.plannedAid(10,20);check(p!=null&&p.troops>=3000&&p.reserve>=6000,"real constrained deployment plan");
        check(Arrays.equals(before,bytes(w)),"preview is byte-for-byte pure");
        seed(w,x->x.diplomacy.requestAid(0,1,10,20,1000),x->!x.diplomacy.aids.isEmpty());
        int gold=w.city(0).gold+w.city(10).gold;ok(w.diplomacy.requestAid(0,1,10,20,1000));
        check(w.actionPoints[0]==30&&w.city(0).gold+w.city(10).gold==gold,"30 AP; gift conserves money");
        check(w.units.isEmpty()&&w.diplomacy.aids.get(0).unit==-1,"accepted request waits for ally action");
        rejected(w,()->w.diplomacy.requestAid(0,2,10,20,1000));
        World saved=copy(w);w.active=1;saved.active=1;int troops=w.city(10).troops,food=w.city(10).food;
        w.diplomacy.dispatch();saved.diplomacy.dispatch();check(Arrays.equals(bytes(w),bytes(saved)),"saved dispatch produces identical army");
        Diplomacy.Aid aid=w.diplomacy.aids.get(0);World.Unit u=w.unit(aid.unit);check(u!=null&&u.owner==1,"real allied unit keeps ownership");
        check(w.city(10).troops+u.troops==troops&&w.city(10).food+u.food==food,"dispatch conserves troops and food");
        int id=u.id;Hex start=u.hex;new CampaignAi(w).runUnits();check(w.unit(id)!=null&&!w.unit(id).hex.equals(start),"aid follows actual weighted movement");
        w.active=0;reset(w);ok(w.diplomacy.cancelAid(10));check(aid.returning,"cancel recalls existing army");
        for(int i=0;i<15&&!w.diplomacy.aids.isEmpty();i++){w.active=1;for(World.Unit army:w.units)w.orders.reset(army);new CampaignAi(w).runUnits();w.diplomacy.cleanup();}
        check(w.diplomacy.aids.isEmpty()&&w.unit(id)==null,"recalled army enters allied city once");
        w.active=0;check(w.city(10).troops==troops,"return restores actual surviving troops");SaveCodec.validate(w);
    }
    static void aidInvalidation()throws Exception{
        for(int reason=0;reason<5;reason++){
            World w=world();seed(w,x->x.diplomacy.requestAid(0,1,10,20,0),x->!x.diplomacy.aids.isEmpty());ok(w.diplomacy.requestAid(0,1,10,20,0));
            if(reason==0)w.campaign.treaties.clear();
            if(reason==1){w.city(20).owner=0;for(World.Officer o:w.officers)if(o.owner==2){o.owner=0;o.role=Strategy.Role.OFFICER;}}
            if(reason==2)w.turn=18;
            if(reason==3){w.city(10).owner=0;for(World.Officer o:w.officers)if(o.owner==1){o.owner=0;o.role=Strategy.Role.OFFICER;}}
            if(reason==4)w.campaign.concludeTreaty(1,2,Campaign.TreatyKind.CEASEFIRE,6);
            w.diplomacy.cleanup();check(w.diplomacy.aids.isEmpty(),"pending aid ends when its reason changes "+reason);
        }
        World caps=world();caps.city(10).kind=World.SiteKind.PORT;caps.city(10).gold=10000;
        rejected(caps,()->caps.diplomacy.requestAid(0,1,10,20,1000));caps.city(10).gold=9000;
        caps.actionPoints[0]=20;rejected(caps,()->caps.diplomacy.requestAid(0,1,10,20,1000));
        caps.actionPoints[0]=60;caps.campaign.treaties.clear();rejected(caps,()->caps.diplomacy.requestAid(0,1,10,20,0));
        // Resource loss after acceptance must wait, never create free soldiers or lose the request.
        World w=world();final World waiting=w;seed(w,x->x.diplomacy.requestAid(0,1,10,20,0),x->!x.diplomacy.aids.isEmpty());ok(w.diplomacy.requestAid(0,1,10,20,0));
        w.city(10).troops=0;w.active=1;w.diplomacy.dispatch();check(w.diplomacy.aids.get(0).unit==-1&&w.units.isEmpty(),"dispatch revalidates actual resources");copy(w);
        w.active=0;rejected(w,()->waiting.diplomacy.requestAid(0,2,10,20,0));
    }
    static void exchange()throws Exception{
        World w=world();w.government.capture(w.officer(2),w.city(10));w.government.capture(w.officer(102),w.city(0));
        seed(w,x->x.diplomacy.exchange(0,1,2,102,1000),x->!x.government.captive(2));
        byte[] before=bytes(w);check(w.diplomacy.exchangeChance(1,2,102,1000)>0&&w.diplomacy.exchangeError(0,1,2,102,1000)==null,"exchange preview");check(Arrays.equals(before,bytes(w)),"exchange preview pure");
        int gold=w.city(0).gold+w.city(10).gold;ok(w.diplomacy.exchange(0,1,2,102,1000));
        check(!w.government.captive(2)&&!w.government.captive(102)&&w.officer(2).cityId==0&&w.officer(102).cityId==10,"simultaneous officer return");
        check(w.officer(2).owner==0&&w.officer(102).owner==1&&gold==w.city(0).gold+w.city(10).gold,"allegiance and money conserved");
        rejected(w,()->w.diplomacy.exchange(0,3,2,102,1000));copy(w);
        World caps=world();caps.government.capture(caps.officer(2),caps.city(10));caps.city(10).kind=World.SiteKind.GATE;caps.city(10).gold=10000;
        rejected(caps,()->caps.government.ransom(0,1,2));rejected(caps,()->caps.diplomacy.exchange(0,1,2,-1,1000));
        caps.city(10).gold=10000-caps.government.ransomCost(2);ok(caps.government.ransom(0,1,2));check(caps.city(10).gold==10000,"legacy ransom respects exact gate capacity");
        World fail=world();fail.government.capture(fail.officer(2),fail.city(10));seed(fail,x->x.diplomacy.exchange(0,1,2,-1,100),x->x.government.captive(2));
        int oldGold=fail.city(0).gold;ok(fail.diplomacy.exchange(0,1,2,-1,100));check(fail.city(0).gold==oldGold&&fail.actionPoints[0]==30,"denial keeps offered money");rejected(fail,()->fail.diplomacy.exchange(0,3,2,-1,100));
    }
    static void surrender()throws Exception{
        World w=world();w.city(10).troops=1000;w.city(10).defense=500;
        World.Unit u=GovernmentTest.unit(w,101,1,new Hex(11,3),4000,102);u.gold=2400;u.ship=Army.Ship.TOWER_SHIP;u.acted=false;
        w.government.capture(w.officer(3),u);w.government.capture(w.officer(103),w.city(0));w.government.capture(w.officer(202),u);
        w.treasures.place(Treasures.definition("item-000"),Treasures.Place.TREASURY,1);
        w.domestic.facilities.add(new Domestic.Facility(w.domestic.nextFacilityId++,10,Domestic.Kind.FARM,new Hex(10,4),104,2));
        w.officer(104).acted=true;
        w.campaign.learned.put(1,EnumSet.of(Campaign.Tech.LOGISTICS));u.energy=120;
        w.campaign.projects.add(new Campaign.Project(1,10,100,Campaign.Tech.SPEAR_DRILL,null));w.officer(100).otherTask="研究枪兵锻炼";w.officer(100).otherTaskTurns=3;
        seed(w,x->x.diplomacy.surrender(0,1,1),x->!x.alive(1));int food=u.food,gold=w.city(10).gold;
        ok(w.diplomacy.surrender(0,1,1));World.Unit army=w.unit(u.id);
        check(!w.alive(1)&&w.city(10).owner==0&&w.city(10).gold==gold,"peaceful city and stock transfer");
        check(army.owner==0&&army.hex.equals(u.hex)&&army.troops==4000&&army.food==food&&army.gold==2400&&army.ship==u.ship&&army.deputies.length==1,"whole unit transfer keeps crew/cargo/location");
        check(army.acted&&army.march==null&&w.officer(100).role==Strategy.Role.OFFICER&&w.officer(0).role==Strategy.Role.RULER,"no double action or duplicate ruler");
        check(!w.government.captive(3)&&!w.government.captive(103)&&w.government.prisoner(202).captor==0&&w.government.prisoner(202).unitId==army.id,"friendly prisoners freed, foreign prisoners still escorted");
        check(w.campaign.projects.isEmpty()&&w.officer(100).otherTaskTurns==0&&w.domestic.facilities.size()==1,"research cancels; local construction persists");
        check(w.campaign.has(0,Campaign.Tech.LOGISTICS)&&army.energy==120&&w.treasures.item("item-000").holder==0,"completed tech and treasury preserved");
        check(w.campaign.treaty(0,1)==null&&w.actionPoints[1]==0,"former diplomacy/AP cleared");copy(w);
        World tasks=world();World.Officer o=tasks.officer(104);o.cityId=-1;tasks.domestic.missions.add(new Domestic.Mission(tasks.domestic.nextMissionId++,1,o.id,10,10,new Hex(11,4),true,500,2000,1000,new int[]{500,0,0,0}));
        tasks.diplomacy.absorb(1,0);Domestic.Mission cargo=tasks.domestic.missions.get(0);check(cargo.owner==0&&cargo.gold==500&&cargo.food==2000&&cargo.equipment[0]==500,"in-flight cargo preserved");copy(tasks);
        World invalid=world();invalid.strategy.setFactionRelation(0,1,0);rejected(invalid,()->invalid.diplomacy.surrender(0,1,1));
    }
    static void restoredSkills()throws Exception{
        for(Skill skill:Arrays.asList(Skill.TIEBI,Skill.WEIYA,Skill.LUNKE,Skill.JIJIAO,Skill.DUNZOU,Skill.GUIMEN,Skill.FENGSHUI,Skill.QIYUAN)){
            World w=world();AbilityResearch.Node n=AbilityResearch.catalog().stream().filter(x->x.skill==skill).findFirst().get();
            if(!n.slot.isEmpty()){AbilityResearch.State state=w.abilities.states[0];String old=state.hidden.stream().filter(id->AbilityResearch.node(id).slot.equals(n.slot)).findFirst().orElse(state.hidden.first());state.hidden.remove(old);state.hidden.add(n.id);}
            AbilityResearchTest.unlock(w,0,n.id);check(w.abilities.trainingError(0,1,n.id,false)==null,"train unlocked "+skill.label);ok(w.abilities.train(0,1,n.id,false));
            World restored=copy(w);for(int i=0;i<3;i++){w.turn++;w.abilities.tick();w.strategy.tick();restored.turn++;restored.abilities.tick();restored.strategy.tick();}
            check(w.officer(1).skillId.equals(skill.id)&&w.abilities.remaining(0,n.id)==n.uses-1,"teaches actual skill once "+skill.label);
            check(Arrays.equals(bytes(w),bytes(restored)),"training save continuation "+skill.label);
        }
    }
    static void migration()throws Exception{
        try(InputStream in=DiplomacyTest.class.getResourceAsStream("/save-v16-before-diplomacy.b64")){
            byte[] old=Base64.getMimeDecoder().decode(in.readAllBytes());check(old[7]==16,"real previous writer fixture");World w=SaveCodec.decode(old);
            check(w.government.prisoner(7).unitId==1&&w.diplomacy.aids.isEmpty(),"old escort migrates without invented diplomacy");check(bytes(w)[7]==20&&Arrays.equals(bytes(w),bytes(copy(w))),"current save exact roundtrip");
        }
        World w=world();w.diplomacy.attempts.add("exchange:999:2");try{bytes(w);throw new AssertionError("invalid attempt saved");}catch(IOException expected){checks++;}
    }
}
