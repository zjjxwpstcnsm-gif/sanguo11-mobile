package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Actual rules, commands, assignments and saved state, not UI string-only assertions. */
public final class Personnel49Test {
    private static int checks;
    private static void check(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
    private static World copy(World w)throws IOException{return SaveCodec.decode(SaveCodec.encode(w));}
    private static void same(World w,byte[] before)throws IOException{check(Arrays.equals(before,SaveCodec.encode(w)),"preview/rejection preserves complete world and RNG");}
    private static void settle(World w)throws Exception{java.lang.reflect.Method method=World.class.getDeclaredMethod("settleGlobalTurn",java.util.function.Consumer.class);method.setAccessible(true);method.invoke(w,(java.util.function.Consumer<String>)s->{});}
    private static void tickMission(World w)throws Exception{w.turn++;w.recruitment.tick();w.strategy.tick();SaveCodec.validate(w);}
    public static void main(String[] args)throws Exception{
        contributions();rations();loyalty();recruitment();defection();missions();convoy();content();
        System.out.println("PASS v49: "+checks+" stat, rations, loyalty, recommendation, defection, field mission and save assertions");
    }
    private static void contributions()throws Exception{
        World w=Personnel49Fixture.world();World.Unit u=w.unit(2);World.Officer a=w.officer(2),b=w.officer(3);a.war=a.leadership=50;b.war=b.leadership=100;
        check(w.army.war(u)==62&&w.army.leadership(u)==62,"ordinary deputy adds one quarter positive gap");
        check(w.combat.baseAttack(u)==58&&w.combat.baseDefense(u)==58,"spear S panel floors 62 times .95");
        w.relations.link(2,3,Relations.Kind.LIKE);check(w.army.war(u)==75,"like adds half");w.relations.unlink(2,3,Relations.Kind.LIKE);
        w.relations.link(2,3,Relations.Kind.SWORN);check(w.army.war(u)==100,"sworn takes full higher value");w.relations.unlink(2,3,Relations.Kind.SWORN);
        w.relations.link(2,3,Relations.Kind.SPOUSE);check(w.army.war(u)==100,"spouse full value");w.relations.unlink(2,3,Relations.Kind.SPOUSE);
        w.relations.link(3,2,Relations.Kind.FATHER);check(w.army.war(u)==66,"blood adds one third");w.relations.unlink(3,2,Relations.Kind.FATHER);
        w.relations.link(3,2,Relations.Kind.DISLIKE);check(w.army.war(u)==50,"disliked leader gets no deputy contribution");w.relations.unlink(3,2,Relations.Kind.DISLIKE);
        World.Officer second=w.officer(4);second.cityId=-1;second.unitId=u.id;second.war=100;u.deputies=new int[]{3,4};check(w.army.war(u)==62,"two deputies take max, never add");
        b.war=20;second.war=10;check(w.army.war(u)==50,"low deputy cannot weaken commander");
        a.war=100;a.leadership=20;b.leadership=10;second.leadership=10;
        check(w.combat.baseAttack(u)==95&&w.combat.baseDefense(u)==19,"war controls attack and leadership defense independently");
        byte[] before=SaveCodec.encode(w);for(int i=0;i<10;i++){w.combat.statExplanation(u);w.combat.preview(u,w.unit(1),1,false);}same(w,before);
    }
    private static void rations()throws Exception{
        check(Logistics.baseUse(6000,false)==600&&Logistics.baseUse(6000,true)==300,"1/10 field and 1/20 transport");
        check(Logistics.baseUse(1,false)==1&&Logistics.baseUse(11,false)==2&&Logistics.baseUse(0,false)==0,"ceil ration and empty transport boundaries");
        check(Logistics.defaultFood(6000,100000)==12000&&Logistics.turns(12000,600)==20,"twice troop food means twenty turns");
        check(Logistics.defaultFood(6000,7000)==7000&&Logistics.defaultFood(Integer.MAX_VALUE,Integer.MAX_VALUE)==1000000,"stock/carry clamp and overflow safe");
        check(Logistics.deserters(6000)==600&&Logistics.deserters(9)==1&&Logistics.deserters(0)==0,"ten percent desertion with min one");
        World w=Personnel49Fixture.world();World.Unit u=w.unit(2);u.food=600;
        // Execute global settlement, but no faction combat or AI; remove unrelated troops.
        w.units.remove(w.unit(1));for(int id:new int[]{11,12,13}){w.officer(id).unitId=-1;w.officer(id).cityId=10;}
        settle(w);check(u.food==0&&u.troops==6000,"exact final ration funds this whole turn");
        settle(w);check(u.food==0&&u.troops==5400,"next empty turn loses exactly ten percent");
        u.food=Logistics.foodUse(w,u);settle(w);check(u.troops==5400,"resupply before settlement prevents further desertion");
        u.food=0;u.troops=1;settle(w);check(w.unit(u.id)==null&&w.officer(2).unitId==-1,"final deserter removes unit and releases crew");SaveCodec.validate(w);
    }
    private static void loyalty()throws Exception{
        World w=Personnel49Fixture.world();World.Officer o=w.officer(2),r=w.officer(0);o.loyalty=90;
        w.turn=3;w.loyalty.tick();check(o.loyalty==90,"not a season boundary");w.turn=9;w.loyalty.tick();check(o.loyalty<90,"field loyalty decays at season boundary");
        w.relations.link(0,2,Relations.Kind.SWORN);o.loyalty=90;w.loyalty.tick();check(o.loyalty==90&&w.loyalty.lose(o,50)==0,"ruler sworn immunity across natural and other penalties");
        w.relations.unlink(0,2,Relations.Kind.SWORN);w.relations.link(0,2,Relations.Kind.SPOUSE);w.loyalty.tick();check(o.loyalty==90,"ruler spouse immunity");
        check(w.loyalty.lose(r,99)==0&&r.loyalty==100,"ruler loyalty remains 100");
        byte[] before=SaveCodec.encode(w);check(!w.strategy.rewardOfficer(0,1,2).ok,"deployed officer cannot be rewarded remotely");same(w,before);
        w.relations.unlink(0,2,Relations.Kind.SPOUSE);w.startMonth=2;w.turn=6;o.loyalty=90;w.loyalty.tick();check(o.loyalty<90,"non-January scenario uses real season boundary");
        w.officer(3).affinity=149;r.affinity=1;check(Loyalty.distance(w.officer(3),r)==2,"cyclic compatibility wraps at 150");
        r.affinity=-1;check(Loyalty.distance(w.officer(3),r)==-1,"unknown affinity not fabricated");
    }
    private static void recruitment()throws Exception{
        World w=Personnel49Fixture.world();World.Officer o=w.officer(11);w.officer(1).affinity=50;w.officer(4).affinity=125;
        check(w.strategy.canRecruitTarget(0,11),"field commander target is selectable");
        int near=w.strategy.recruitmentChance(0,1,11),far=w.strategy.recruitmentChance(0,4,11);check(near>far,"equal ability near compatibility improves actual probability");
        o.loyalty=94;o.honor=3;check(w.strategy.recruitmentChance(0,1,11)==0,"loyalty plus honor above 96 blocks ordinary recruitment");o.loyalty=50;
        int low=w.strategy.recruitmentChance(0,1,11);o.loyalty=80;check(w.strategy.recruitmentChance(0,1,11)<low,"lower loyalty easier to recruit");o.loyalty=30;
        w.government.advisors.put(0,6);byte[] before=SaveCodec.encode(w);
        for(int i=0;i<25;i++){check(w.loyalty.recruitmentActors(0,11).get(0).id==1,"best compatible actor recommended");check(w.loyalty.recommendation(0,11,1).contains("将6推荐"),"appointed adviser credited");}same(w,before);
        w.relations.link(10,11,Relations.Kind.SPOUSE);check(!w.strategy.canRecruitTarget(0,11),"ruler spouse cannot be recruited despite low loyalty");
        World captive=Personnel49Fixture.world();captive.officer(1).affinity=50;captive.officer(4).affinity=125;
        captive.officer(21).affinity=50;captive.officer(21).loyalty=60;captive.government.capture(captive.officer(21),captive.unit(2));
        check(captive.government.recruitChance(1,21)>captive.government.recruitChance(4,21),"captive recruitment shares compatibility adjustment");
        captive.officer(21).honor=1;int lowHonor=captive.government.recruitChance(1,21);captive.officer(21).honor=5;
        check(captive.government.recruitChance(1,21)<lowHonor,"captive honor also resists recruitment");
    }
    private static void defection()throws Exception{
        boolean followed=false,refused=false;
        for(int seed=1;seed<=32;seed++){
            World w=Personnel49Fixture.world();w.strategy.setSeed(seed);World.Unit u=w.unit(1);w.government.capture(w.officer(5),u);w.government.capture(w.officer(21),u);
            byte[] before=SaveCodec.encode(w);World clone=copy(w);w.strategy.join(w.officer(1),w.officer(11),0);clone.strategy.join(clone.officer(1),clone.officer(11),0);
            check(u.owner==0&&u.troops==6000&&u.food==12000&&u.gold==500,"commander defection preserves actual unit resources");
            check(w.officer(13).owner==1&&w.officer(13).unitId==-1,"high loyalty deputy refuses and returns");
            check(!w.government.captive(5)&&w.officer(5).owner==0,"new faction prisoner freed");
            check(w.government.prisoner(21).captor==0&&w.government.prisoner(21).unitId==u.id,"other captive changes escort captor with unit");
            check(u.acted&&u.march==null,"defection cancels prior objective and consumes available action");
            followed|=w.officer(12).owner==0;refused|=w.officer(12).owner==1;
            check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(clone)),"seeded transfer and followers remain save deterministic");check(before.length>0,"baseline captured");
        }
        check(followed&&refused,"ordinary low loyalty deputy can either follow or refuse");
        World w=Personnel49Fixture.world();w.strategy.join(w.officer(1),w.officer(12),0);
        check(w.unit(1).owner==1&&w.unit(1).officerId==11&&!w.army.contains(w.unit(1),12)&&w.officer(12).owner==0,"recruiting deputy never steals commander unit");SaveCodec.validate(w);
    }
    private static void missions()throws Exception{
        World w=Personnel49Fixture.world();w.strategy.setSeed(1);w.officer(1).affinity=50;
        int food=w.unit(1).food;check(w.strategy.recruitOfficer(0,1,11).ok,"real field recruitment command starts");
        Recruitment.Mission m=w.recruitment.missions().get(0);check(m.fieldUnit==1&&m.travel>=1,"field identity and nonzero travel recorded");
        World restored=copy(w);same(w,SaveCodec.encode(restored));
        for(int i=0;i<m.travel;i++){tickMission(w);tickMission(restored);check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"saved field mission continues deterministically");}
        check(m.joined&&m.fieldJoined&&w.unit(1).owner==0&&w.unit(1).food==food,"arrival transfers commander in place without extra food deduction");
        check(w.officer(11).otherTaskTurns==0&&w.officer(11).unitId==1,"commander is not turned into returning messenger");
        while(!w.recruitment.missions().isEmpty())tickMission(w);
        check(w.officer(1).cityId==0&&w.officer(1).otherTaskTurns==0&&w.unit(1).officerId==11,"only envoy returns to departure city");
        w=Personnel49Fixture.world();w.strategy.setSeed(1);w.officer(1).affinity=50;check(w.strategy.recruitOfficer(0,1,12).ok,"field deputy mission starts");
        int turns=w.recruitment.missions().get(0).travel;for(int i=0;i<turns;i++)tickMission(w);
        check(w.officer(12).owner==0&&w.unit(1).owner==1,"arriving envoy recruits only deputy");
        while(!w.recruitment.missions().isEmpty())tickMission(w);check(w.officer(12).cityId==0,"recruited deputy returns with envoy");
        World changed=Personnel49Fixture.world();check(changed.strategy.recruitOfficer(0,1,11).ok,"race mission starts");changed.officer(11).loyalty=100;
        turns=changed.recruitment.missions().get(0).travel;for(int i=0;i<turns;i++)tickMission(changed);
        check(!changed.recruitment.missions().get(0).joined&&changed.unit(1).owner==1,"arrival recalculates loyalty instead of trusting departure preview");
    }
    private static void convoy()throws Exception{
        World w=Personnel49Fixture.world();w.active=1;w.officer(14).loyalty=20;
        // Enemy needs a second owned destination for a genuine transport command.
        w.cities.add(new World.City(11,"敌港",new Hex(18,14),1));w.city(11).kind=World.SiteKind.PORT;
        check(w.domestic.transport(10,11,14,50,5000,2000,new int[]{100,0,0,0}).ok,"real transport creation");w.active=0;
        Domestic.Mission m=w.domestic.missions.get(0);check(w.loyalty.fieldUnit(14)==m&&w.strategy.canRecruitTarget(0,14),"transport leader field target accessible");
        check(w.domestic.foodUse(m)==100,"transport actual consumption is one twentieth");
        w.strategy.join(w.officer(1),w.officer(14),0);
        check(m.owner==0&&m.troops==2000&&m.food==5000&&m.equipment[0]==100&&m.stopped,"transport defection retains cargo and awaits orders");
        check(w.city(m.sourceCity).owner==0&&m.targetCity==m.sourceCity&&m.escortId==-1,"captured transport command district rerooted");SaveCodec.validate(w);copy(w);
    }
    private static void content()throws Exception{
        ContentCatalog c=ContentCatalog.get();World w=TestScenarios.load("officer-reference-drill",0);
        // This legacy test fixture opts out of biographies; explicit sourced application must use the real row.
        ContentProfiles.biography(w,c,1000,w.officer(1000).cityId,false);
        check(w.officer(1000).affinity==c.profile(1000).affinity&&w.officer(1000).honor==c.profile(1000).honor,"source affinity and honor applied, not guessed from faction");
        byte[] legacy;
        try(InputStream in=Personnel49Test.class.getResourceAsStream("/v048-save27.sg11.b64")){legacy=Base64.getMimeDecoder().decode(in.readAllBytes());}
        check(legacy[7]==27,"legacy fixture genuinely written by baseline v48/save27 engine");
        World old=SaveCodec.decode(legacy);check(old.units.size()==3&&old.width==28&&old.unit(1).hex.equals(new Hex(8,8)),"v27 migration preserves actual map, units and positions");
        check(old.officers.stream().allMatch(o->o.affinity==-1&&o.honor==3),"old save uses explicit unknown affinity / neutral honor, no source overwrite");
        check(SaveCodec.encode(old)[7]==28&&Arrays.equals(SaveCodec.encode(old),SaveCodec.encode(copy(old))),"v27 migrates to exact roundtripping v28");
        World saved=copy(w);check(saved.officer(1000).affinity==w.officer(1000).affinity&&saved.officer(1000).honor==w.officer(1000).honor,"v28 records runtime affinity and honor");
        saved.officer(1000).affinity=150;try{SaveCodec.encode(saved);throw new AssertionError("invalid affinity accepted");}catch(IOException expected){checks++;}
    }
}
