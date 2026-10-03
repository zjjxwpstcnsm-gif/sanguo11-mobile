package game.sanguo.core;

import java.util.Arrays;

/** Native arithmetic boundaries exercised through normal command/save/turn paths. */
public final class PcPatrolTest {
    static int checks;
    static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static World fixture(int leadership,int order,int distance,int relation){
        World w=new World(28,24);
        w.cities.add(new World.City(10,"甲",new Hex(10,10),0));
        w.cities.add(new World.City(20,"乙",new Hex(23,20),1));
        w.officers.add(new World.Officer(0,"君",0,10,50,60,70,80,90));
        w.officers.add(new World.Officer(1,"巡察",0,10,leadership,50,60,1,1));
        w.officers.add(new World.Officer(20,"敌君",1,20,50,60,70,80,90));
        w.officers.add(new World.Officer(21,"周边军",relation==3?0:1,-1,50,60,70,80,90));
        w.strategy.initializeOffices();w.city(10).order=order;
        var o=w.officer(21);var u=new World.Unit(w.nextUnitId++,o.owner,o.id,World.Weapon.SPEAR,new Hex(10+distance,10),1000,10000);
        w.units.add(u);o.unitId=u.id;
        if(relation==1||relation==2)w.campaign.treaties.add(new Campaign.Treaty(0,1,relation==1?Campaign.TreatyKind.ALLIANCE:Campaign.TreatyKind.CEASEFIRE,9));
        return w;
    }
    public static void main(String[] args)throws Exception{
        // Expected constants separately established by original5cba10 execution.
        int[][] vectors={{0,2},{27,2},{28,3},{55,3},{56,4},{84,5},{100,5},{240,10},{765,29}};
        for(int[] v:vectors){
            check(PcPatrolRules.gain(v[0],0,false)==v[1],"native full result");
            check(PcPatrolRules.gain(v[0],0,true)==v[1]/2,"native half before cap");
            check(PcPatrolRules.gain(v[0],99,true)==1,"remaining order cap");
        }
        for(int leadership:new int[]{27,28,84,100})for(int order:new int[]{0,97,99})for(int distance:new int[]{2,3,4})for(int relation=0;relation<4;relation++){
            World w=fixture(leadership,order,distance,relation);
            int raw=leadership<28?2:leadership==28?3:5;
            int expected=Math.min(100-order,distance<=3&&relation==0?raw/2:raw);
            byte[] before=SaveCodec.encode(w);long random=w.strategy.getRandomState();
            var preview=w.strategy.previewCityAction(CityActionPlan.Operation.PATROL,10,1,new int[0]);
            check(preview.allowed()&&preview.effects.orderAfter==order+expected,"authoritative native effect forecast");
            check(Arrays.equals(before,SaveCodec.encode(w)),"preview full-state/RNG purity");
            // Changing political/charm values must have no influence on patrol.
            w.officer(1).politics=100;w.officer(1).charm=100;
            check(w.strategy.previewCityAction(CityActionPlan.Operation.PATROL,10,1,new int[0]).effects.orderAfter==order+expected,"unrelated abilities do not affect patrol");
            int ap=w.actionPoints[0],gold=w.city(10).gold;
            check(w.strategy.patrol(10,1).ok&&w.city(10).order==order+expected,"normal command uses native result");
            check(w.actionPoints[0]==ap-20&&w.city(10).gold==gold-100&&w.officer(1).acted,"single normal debit and actor consumption");
            check(random==w.strategy.getRandomState(),"patrol consumes no RNG");
            byte[] saved=SaveCodec.encode(w);World reopened=SaveCodec.decode(saved);
            check(Arrays.equals(saved,SaveCodec.encode(reopened)),"full save reopen");
            check(!w.strategy.patrol(10,1).ok&&Arrays.equals(saved,SaveCodec.encode(w)),"duplicate rejection is atomic");
            check(w.nextTurn().ok&&reopened.nextTurn().ok,"full normal turn");
            check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(reopened)),"turn/save/RNG continuation agrees");
        }
        System.out.println("PASS PcPatrolTest checks="+checks+"; single actor/current ability and treaty model; full PC modifiers and three actors pending");
    }
}
