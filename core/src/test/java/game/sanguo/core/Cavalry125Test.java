package game.sanguo.core;

import java.util.*;

/** Actual campaign commands: optional landing, RNG purity and contest settlement. */
public final class Cavalry125Test {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static byte[] bytes(World w)throws Exception{return SaveCodec.encode(w);}
    static void ok(World.Result r){check(r.ok,r.message);}
    static World fixture(String obstacle,long seed){
        World w=DisplacementFixture.create(obstacle,World.Weapon.CAVALRY);w.strategy.setSeed(29016L+seed*982451653L);
        w.unit(2).status=War.Status.NORMAL;w.unit(2).statusTurns=0;
        if(obstacle.equals("gate"))w.city(20).owner=2;
        w.contests.configure(1,new Contests.Profile(Debate.Temper.RASH,0,0));
        return w;
    }
    public static void main(String[] args)throws Exception{
        int forced=0;
        for(War.Tactic t:new War.Tactic[]{War.Tactic.CHARGE,War.Tactic.BREAKTHROUGH,War.Tactic.ADVANCE})
            for(String obstacle:new String[]{"mountain","cliff","shore","edge","friend","enemy","gate","tower"})
                for(int seed=0;seed<256;seed++){
                    World w=fixture(obstacle,seed);World.Unit a=w.unit(1),b=w.unit(2);Hex original=a.hex,target=b.hex;
                    byte[] before=bytes(w);Displacement.Preview p=w.war.tacticPreview(1,2,t);
                    check(p.valid()&&p.blocked!=null,obstacle+" "+t+" castable and blocked in preview");
                    check(p.actorPath.size()==1&&p.targetPath.size()==1,"preview has no blocked ghost movement");
                    check(Arrays.equals(before,bytes(w)),"preview preserves all state/RNG");
                    World replay=SaveCodec.decode(before);World.Result result=w.war.tactic(1,2,t);ok(result);ok(replay.war.tactic(1,2,t));
                    check(Arrays.equals(bytes(w),bytes(replay)),"command deterministic after reload");
                    check(a.hex.equals(original)&&b.hex.equals(target),"obstacle prevents only displacement");
                    check((result.message.contains("未命中")?b.troops==8000:b.troops<8000)&&a.energy==100-t.energy&&a.acted,"hit damage or miss, one cost and one action");
                    if(w.contests.busy()){
                        forced++;check(w.contests.current().isDuel(),"blocked landing still triggers real Duel");
                        World loaded=SaveCodec.decode(bytes(w));check(loaded.contests.busy(),"in-progress forced contest survives save/load");
                        check(!w.war.tactic(1,2,t).ok,"cannot repeat tactic during contest");
                        Contests.Session s=w.contests.current();ok(w.contests.concede(s.id(),s.revision()));check(!w.contests.busy(),"existing contest settles once");
                    }
                    if(w.unit(3)!=null)check(w.unit(3).troops==4000,"cavalry obstruction receives no collision damage");
                }
        check(forced>0,"fixed seed coverage actually reached forced duels behind obstacles");
        World w=fixture("mountain",125);int blockedChance=w.contests.cavalryChance(w.unit(1),w.unit(2));
        w.terrain[7][6]=World.Terrain.PLAIN;check(blockedChance==w.contests.cavalryChance(w.unit(1),w.unit(2)),"landing has no probability input");
        w.contests.configure(1,new Contests.Profile(Debate.Temper.TIMID,0,0));check(w.contests.cavalryChance(w.unit(1),w.unit(2))==0,"timid opener cannot force duel");
        w=fixture("mountain",125);Arrays.fill(w.officer(1).aptitude,1);byte[] before=bytes(w);
        check(w.war.tacticError(1,2,War.Tactic.BREAKTHROUGH).contains("适性"),"rank still authoritative");
        check(!w.war.tactic(1,2,War.Tactic.BREAKTHROUGH).ok&&Arrays.equals(before,bytes(w)),"insufficient rank rejected without cost/RNG");
        boolean aiReached=false;
        for(int seed=0;seed<200&&!aiReached;seed++){
            w=fixture("mountain",seed);w.player=2;ok(w.war.tactic(1,2,War.Tactic.BREAKTHROUGH));
            if(!w.contests.lastResult().isEmpty()){aiReached=true;check(!w.contests.busy(),"AI force duel completes finite existing settlement");}
        }
        check(aiReached,"AI forced duel reached by actual tactic");
        System.out.println("PASS CAVALRY125 checks="+checks+" forced="+forced+" (all three cavalry tactics, eight obstacles, actual commands and save/replay)");
    }
}
