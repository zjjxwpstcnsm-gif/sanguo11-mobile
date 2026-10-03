package game.sanguo.core;

import java.util.*;

public final class DiplomacyPlanTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static World fixture(){
        World w=new World(22,16);w.cities.add(new World.City(10,"甲",new Hex(5,5),0));w.cities.add(new World.City(20,"乙",new Hex(18,5),1));
        for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"将"+i,0,10,70+i,80+i,90+i,85,85));
        w.officers.add(new World.Officer(20,"敌",1,20,80,80,80,80,80));
        w.city(10).gold=30000;w.city(10).food=200000;w.city(20).food=200000;
        w.strategy.initializeOffices();w.strategy.setFactionRelation(0,1,30);return w;
    }
    public static void main(String[] args)throws Exception{
        for(DiplomacyPlan.Operation operation:DiplomacyPlan.Operation.values())for(int duration:new int[]{3,6,12}){
            World w=fixture();if(operation==DiplomacyPlan.Operation.BREAK_TREATY)w.campaign.concludeTreaty(0,1,Campaign.TreatyKind.ALLIANCE,12);
            byte[] before=SaveCodec.encode(w);long rng=w.strategy.getRandomState();
            DiplomacyPlan p=w.campaign.previewDiplomacy(10,0,1,operation,duration);
            check(p.allowed(),"allowed "+operation);check(Arrays.equals(before,SaveCodec.encode(w)),"preview complete save purity");
            check(w.campaign.diplomaticAction(10,0,1,operation,duration).ok,"real normal command");
            check(w.city(10).gold==p.goldRemaining&&w.actionPoints[0]==p.actionPointsRemaining,"actual debit equals forecast");
            check(rng==w.strategy.getRandomState(),"dispatch or break consumes no RNG");
            if(p.delayed){
                Envoys.Mission m=w.envoys.missions().get(0);check(m.destination==p.destinationId&&m.travel==p.oneWayTurns&&m.remaining==p.roundTripTurns,"real journey");
                check(w.strategy.factionRelation(0,1)==p.currentRelation,"departure does not apply arrival effects");
                DiplomacyPlan duplicate=w.campaign.previewDiplomacy(10,1,1,operation,duration);
                check(!duplicate.allowed()&&duplicate.failure.code.equals("ENVOY_PENDING"),"same-target departure blocked");
                World restored=SaveCodec.decode(SaveCodec.encode(w));TravelChecks.complete(w);TravelChecks.complete(restored);
                check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"arrival and return replay incl RNG");
                if(operation==DiplomacyPlan.Operation.GOODWILL){
                    check(w.strategy.factionRelation(0,1)==p.currentRelation+p.relationDeltaOnSuccess,"normal arrival goodwill formula");
                    check(w.strategy.getRandomState()==rng,"goodwill never draws acceptance RNG");
                }else {World oneDraw=SaveCodec.decode(before);oneDraw.strategy.nextInt(100);check(w.strategy.getRandomState()==oneDraw.strategy.getRandomState(),"arrival performs exactly one acceptance draw");}
                check(w.city(10).gold==p.goldRemaining&&w.actionPoints[0]==p.actionPointsRemaining,"arrival and return do not charge twice");
            }else check(w.campaign.treaty(0,1)==null&&w.strategy.factionRelation(0,1)==p.currentRelation+p.relationDeltaOnSuccess,"immediate treaty break");
        }
        String[] codes={"CITY_UNAVAILABLE","LEADER_UNAVAILABLE","ACTION_POINTS","CITY_GOLD","TARGET_SIDE_INVALID","RELATION_MAX","TREATY_TERMS_INVALID","RELATION_TOO_LOW","TREATY_EXISTS","TREATY_MISSING","OPERATION_INVALID"};
        for(int n=0;n<codes.length;n++){
            World w=fixture();int city=10,officer=0,side=1,turns=6;DiplomacyPlan.Operation operation=DiplomacyPlan.Operation.GOODWILL;
            switch(n){case 0:city=-1;break;case 1:officer=-1;break;case 2:w.actionPoints[0]=0;break;case 3:w.city(10).gold=0;break;
                case 4:side=-1;break;case 5:w.strategy.setFactionRelation(0,1,100);break;
                case 6:operation=DiplomacyPlan.Operation.CEASEFIRE;turns=4;break;
                case 7:operation=DiplomacyPlan.Operation.ALLIANCE;w.strategy.setFactionRelation(0,1,0);break;
                case 8:operation=DiplomacyPlan.Operation.CEASEFIRE;w.campaign.concludeTreaty(0,1,Campaign.TreatyKind.ALLIANCE,12);break;
                case 9:operation=DiplomacyPlan.Operation.BREAK_TREATY;break;case 10:operation=null;break;}
            byte[] before=SaveCodec.encode(w);DiplomacyPlan p=w.campaign.previewDiplomacy(city,officer,side,operation,turns);
            check(!p.allowed()&&p.failure.code.equals(codes[n]),"structured rejection "+codes[n]);
            check(Arrays.equals(before,SaveCodec.encode(w)),"invalid preview pure");
            World.Result r=w.campaign.diplomaticAction(city,officer,side,operation,turns);
            check(!r.ok&&r.message.equals(p.failure.detail),"same normal command reason");
            check(Arrays.equals(before,SaveCodec.encode(w)),"invalid normal command atomic");
        }
        World capped=fixture();capped.strategy.setFactionRelation(0,1,99);
        DiplomacyPlan cap=capped.campaign.previewDiplomacy(10,0,1,DiplomacyPlan.Operation.GOODWILL,0);
        check(cap.relationDeltaOnSuccess==1&&cap.initialAcceptancePercent==-1,"bounded relation and no fictitious probability");
        System.out.println("PASS DiplomacyPlanTest checks="+checks);
    }
}
