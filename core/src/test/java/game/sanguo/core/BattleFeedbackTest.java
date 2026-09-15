package game.sanguo.core;

import java.util.*;

/** Player-reported kill/cargo/tile regression scenarios against the real campaign commands. */
public final class BattleFeedbackTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static void ok(World.Result r){check(r.ok,r.message);}
    private static World fixture(){return GovernmentTest.world();}
    private static World.Unit unit(World w,int officer,int owner,int q,int troops,int... deputies){return GovernmentTest.unit(w,officer,owner,new Hex(q,4),troops,deputies);}
    private static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    public static void main(String[] args)throws Exception{
        lootCaptureAndVacancy();capsAndExactlyOnce();ordinaryProbability();friendlyAndEnvironmental();fireVacancy();feedbackBoundaries();transportGold();
        System.out.println("PASS: "+checks+" battle-feedback assertions: cargo/capture/escape, immutable previews, single settlement, caps, save/replay, actual defeated-tile march and fire visibility errors.");
    }
    private static void lootCaptureAndVacancy()throws Exception{
        World w=fixture();World.Unit a=unit(w,1,0,4,8000),b=unit(w,7,1,5,1,8,9);b.gold=1234;b.food=5678;
        w.officer(1).skillId=Skill.BOFU.id;w.officer(9).skillId=Skill.QIANGYUN.id;
        byte[] before=bytes(w);check(w.government.captureChance(a,b,w.officer(7))==100,"capture skill shown");
        check(w.government.captureChance(a,b,w.officer(9))==0,"immunity shown");check(Arrays.equals(before,bytes(w)),"capture preview preserves RNG and saves");
        World replay=SaveCodec.decode(before);World.Result r=w.attack(a.id,b.id);ok(r);ok(replay.attack(a.id,b.id));
        check(Arrays.equals(bytes(w),bytes(replay)),"same saved RNG produces identical battle outcome");
        check(a.gold==1234&&a.food==25678,"cargo received by killer");check(b.gold==0&&b.food==0,"defeated cargo drained");
        check(w.unitAt(b.hex)==null&&w.unit(b.id)==null,"defeated tile immediately vacant");
        check(w.government.captive(7)&&w.government.captive(8)&&!w.government.captive(9),"all crew individually resolved");
        check(r.feedback==World.Feedback.DEFEAT&&r.impact.equals(b.hex)&&r.message.contains("金+1234")&&r.message.contains("粮+5678")&&r.message.contains("逃脱：将9"),"visible result contains exact cargo, escape and defeat impact");
        MarchOrders.Plan move=w.marches.preview(a.id,b.hex);check(move.valid()&&move.stepsNow==0&&move.estimatedTurns==1,"acted killer may queue newly empty tile next turn");ok(w.marches.execute(move));
        check(a.hex.equals(new Hex(4,4))&&a.march!=null,"attack does not grant second action");
        w.orders.reset(a);w.marches.advanceAll();check(a.hex.equals(b.hex)&&a.march==null,"queued route enters actual defeated tile when action refreshes");
        check(Arrays.equals(bytes(w),bytes(SaveCodec.decode(bytes(w)))),"loot and prisoner metadata persist in existing v16");
        World ally=fixture();World.Unit killer=unit(ally,1,0,4,8000),enemy=unit(ally,7,1,5,1);
        World.Unit follower=unit(ally,2,0,6,3000);ok(ally.attack(killer.id,enemy.id));
        check(ally.orders.marchReachable(follower).containsKey(enemy.hex),"another fresh unit sees the defeated tile as reachable immediately");
        ok(ally.marches.execute(ally.marches.preview(follower.id,enemy.hex)));check(follower.hex.equals(enemy.hex),"another fresh unit immediately walks onto the cleared enemy tile");
    }
    private static void capsAndExactlyOnce()throws Exception{
        World w=fixture();World.Unit a=unit(w,1,0,4,8000),b=unit(w,7,1,5,1);a.gold=9990;a.food=999995;b.gold=400;b.food=100;
        World.Result r=w.attack(a.id,b.id);ok(r);check(a.gold==10000&&a.food==1000000,"carry caps respected");
        check(r.message.contains("遗失金390、粮95"),"overflow explicitly reported");
        byte[] after=bytes(w);w.defeatUnit(b,a);check(Arrays.equals(after,bytes(w)),"second callback cannot pay, award merit or free captive twice");
        check(!w.attack(a.id,b.id).ok&&Arrays.equals(after,bytes(w)),"repeated attack rejected without state changes");
    }
    private static void ordinaryProbability()throws Exception{
        int caught=0,escaped=0;
        for(int seed=1;seed<=80;seed++){
            World w=fixture();World.Unit a=unit(w,1,0,4,8000),b=unit(w,7,1,5,1);w.strategy.setSeed(seed);
            check(w.government.captureChance(a,b,w.officer(7))==20,"equal war baseline 20 percent");
            ok(w.attack(a.id,b.id));if(w.government.captive(7))caught++;else escaped++;
        }
        check(caught>0&&escaped>0,"ordinary capture is probabilistic, not disabled or guaranteed");
        World w=fixture();World.Unit a=unit(w,1,0,4,8000),b=unit(w,7,1,5,1,8);w.officer(1).skillId=Skill.BOFU.id;w.officer(8).skillId=Skill.XUELU.id;
        check(w.government.captureChance(a,b,w.officer(7))==0,"blood road protects crew");
    }
    private static void friendlyAndEnvironmental()throws Exception{
        World w=fixture();World.Unit a=unit(w,1,0,4,8000),b=unit(w,2,0,5,1);b.gold=700;int food=a.food;
        w.defeatUnit(b,a);check(a.gold==0&&a.food==food&&!w.government.captive(2),"friendly casualty never yields cargo or prisoners");
        World.Unit enemy=unit(w,7,1,6,1);enemy.gold=600;w.removeUnit(enemy);check(a.gold==0&&!w.government.captive(7),"unattributed fire/starvation cannot assign loot to an arbitrary unit");
        SaveCodec.validate(w);
    }
    private static void fireVacancy()throws Exception{
        World w=fixture();World.Unit a=new World.Unit(w.nextUnitId++,0,1,World.Weapon.CROSSBOW,new Hex(4,4),8000,20000);w.units.add(a);w.officer(1).cityId=-1;w.officer(1).unitId=a.id;w.officer(1).aptitude[2]=3;
        World.Unit b=unit(w,7,1,5,1);b.status=War.Status.CONFUSED;b.statusTurns=1;
        ok(w.war.tactic(a.id,b.id,War.Tactic.FIRE_ARROW));check(w.unitAt(b.hex)==null&&w.war.fireAt(b.hex)!=null,"lethal fire arrow leaves real fire, not ghost unit");
        MarchOrders.Plan blocked=w.marches.preview(a.id,b.hex);check(!blocked.valid()&&blocked.error.contains("火场")&&blocked.error.contains("剩"),"automatic route explains remaining fire");
        byte[] before=bytes(w);check(!w.marches.execute(blocked).ok&&Arrays.equals(before,bytes(w)),"fire rejection consumes nothing");
        w.orders.reset(a);check(!w.orders.marchReachable(a).containsKey(b.hex),"map highlight does not advertise a fire tile that automatic routing refuses");
        w.war.fires.clear();w.orders.reset(a);MarchOrders.Plan clear=w.marches.preview(a.id,b.hex);check(clear.valid()&&clear.stepsNow==1,"extinguished target reachable immediately");ok(w.marches.execute(clear));check(a.hex.equals(b.hex),"actual automatic route executes onto empty former enemy tile");
        Hex next=new Hex(6,4);w.domestic.facilities.add(new Domestic.Facility(1,0,Domestic.Kind.MARKET,next,-1,0));
        check(w.marches.preview(a.id,next).error.contains("市场"),"occupied target names facility");
        w.domestic.facilities.clear();w.terrain[6][4]=World.Terrain.MOUNTAIN;check(w.marches.preview(a.id,next).error.contains("山地"),"impassable mountain explained");
    }
    private static void feedbackBoundaries()throws Exception{
        World w=fixture();World.Unit a=unit(w,1,0,4,8000),b=unit(w,7,1,5,8000);World.Result hit=w.attack(a.id,b.id);ok(hit);
        check(hit.feedback==World.Feedback.ATTACK&&hit.impact.equals(b.hex),"nonlethal attack feedback");
        check(w.move(a.id,new Hex(3,4)).feedback==World.Feedback.NONE,"invalid command never vibrates");
        w.orders.reset(a);World.Result wait=w.war.waitUnit(a.id);ok(wait);check(wait.feedback==World.Feedback.NONE&&!wait.message.contains("缴获"),"noncombat success cannot replay old battle feedback");
        World restored=SaveCodec.decode(bytes(w));restored.orders.reset(restored.unit(a.id));check(restored.war.waitUnit(a.id).feedback==World.Feedback.NONE,"save restore cannot replay haptics");
    }
    private static void transportGold()throws Exception{
        World w=fixture();World.Unit a=unit(w,1,0,4,8000);w.active=1;ok(w.domestic.transport(2,3,7,750,5000,1000,new int[4]));w.active=0;
        Domestic.Mission m=w.domestic.missions.get(0);m.hex=new Hex(5,4);m.troops=1;w.officer(1).skillId=Skill.BOFU.id;
        World.Result r=w.supply.raid(a.id,m.id);ok(r);check(a.gold==750&&a.food==25000&&r.feedback==World.Feedback.DEFEAT&&r.message.contains("俘虏：将7"),"transport gold, food and prisoner included in result");
        byte[] after=bytes(w);check(!w.supply.raid(a.id,m.id).ok&&Arrays.equals(after,bytes(w)),"transport cannot be looted twice");
    }
}
