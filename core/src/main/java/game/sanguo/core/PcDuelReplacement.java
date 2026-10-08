package game.sanguo.core;
import java.io.*;import java.util.*;
/** Original4cf160: ruler/district, command cap, current LEAD/WAR, merit,
 * native ID. Scoped to native duel; historical lifecycle strategy is intact. */
final class PcDuelReplacement {
    static final class Candidate {
        final int id,nativeId,status,capacity,leadership,war,merit;
        Candidate(int id,int nativeId,int status,int capacity,int leadership,int war,int merit){this.id=id;this.nativeId=nativeId;this.status=status;this.capacity=capacity;this.leadership=leadership;this.war=war;this.merit=merit;}
    }
    static int compare(Candidate a,Candidate b){
        if((a.status<=1||b.status<=1)&&a.status!=b.status)return Integer.compare(a.status,b.status);
        int c=Integer.compare(b.capacity,a.capacity);if(c!=0)return c;c=Integer.compare(b.leadership,a.leadership);if(c!=0)return c;c=Integer.compare(b.war,a.war);if(c!=0)return c;c=Integer.compare(b.merit,a.merit);return c!=0?c:Integer.compare(a.nativeId,b.nativeId);
    }
    static Candidate candidate(World w,int id)throws IOException {return candidate(w,id,null);}
    static Candidate candidate(World w,int id,Integer injury)throws IOException {
        World.Officer o=w.officer(id);var f=PcDuelSourceFacts.saved(w).get(id);if(o==null||f==null||!w.life.present(id)||w.government.captive(id)||o.owner<0)throw new IOException("原单挑接任人物不可用");
        int status=o.role==Strategy.Role.RULER?0:o.role==Strategy.Role.DISTRICT?1:o.role==Strategy.Role.GOVERNOR?2:3;
        return new Candidate(id,f.nativeId,status,w.government.commandLimit(id),injury==null?o.leadership:w.officerAbilities.currentWithInjury(id,0,injury),injury==null?o.war:w.officerAbilities.currentWithInjury(id,1,injury),Math.min(60000,w.government.merit(id)));
    }
    static int select(World w,World.Unit unit,int removed)throws IOException {return select(w,unit,removed,Map.of());}
    static int select(World w,World.Unit unit,int removed,Map<Integer,Integer>injuries)throws IOException {
        Candidate best=null;for(World.Officer o:w.army.crew(unit)){if(o.id==removed||o.unitId!=unit.id||!w.life.present(o.id)||w.government.captive(o.id))continue;Candidate c=candidate(w,o.id,injuries.get(o.id));if(best==null||compare(c,best)<0)best=c;}if(best==null&&PcDuelEscortRelease.current(w))PcDuelEscortRelease.plan(w,unit);return best==null?-1:best.id;
    }
    /** Complete4b21b0(unit,1) promotion: preserve remaining deputy order,
     * append former leader, clamp only when original leader changes. */
    static void reselect(World w,World.Unit unit)throws IOException {
        int next=select(w,unit,-1);if(next<0)throw new IOException("原登位编队没有有效主将");
        if(next==unit.officerId)return;
        int old=unit.officerId;int[] remaining=Arrays.stream(unit.deputies).filter(id->id!=next).toArray();
        unit.deputies=Arrays.copyOf(remaining,remaining.length+1);unit.deputies[remaining.length]=old;unit.officerId=next;
        unit.troops=Math.min(unit.troops,w.government.commandLimit(next));unit.energy=Math.min(unit.energy,w.campaign.energyCap(unit.owner));
    }
    /** Original leader replacement preserves the same unit and cargo. Capacity
     * and energy clamp happens before subsequent terminal troop/energy writes. */
    static int remove(World w,World.Unit unit,int removed)throws IOException {
        if(!w.army.contains(unit,removed))throw new IOException("原单挑离队人物未在当前编队");
        if(unit.officerId!=removed){unit.deputies=Arrays.stream(unit.deputies).filter(id->id!=removed).toArray();return unit.officerId;}
        int next=select(w,unit,removed);if(next<0){if(PcDuelEscortRelease.current(w))PcDuelEscortRelease.apply(w,unit,PcDuelEscortRelease.plan(w,unit));else w.government.escortLost(unit,null);unit.troops=0;unit.energy=0;unit.gold=0;unit.food=0;w.units.remove(unit);PcGovernorPolicy.unitRemoved(w,unit.id);return -1;}
        unit.deputies=Arrays.stream(unit.deputies).filter(id->id!=removed&&id!=next).toArray();unit.officerId=next;unit.troops=Math.min(unit.troops,w.government.commandLimit(next));unit.energy=Math.min(unit.energy,w.campaign.energyCap(unit.owner));return next;
    }
    private PcDuelReplacement(){}
}
