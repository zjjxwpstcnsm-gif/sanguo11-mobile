package game.sanguo.runtime.query;

import game.sanguo.api.*;
import game.sanguo.core.*;
import java.util.*;

/** Read-only projection of saved facts, including explicit legacy unknowns. */
public final class OfficerQuery {
    private OfficerQuery(){}
    public static OfficerSnapshot capture(World w,StateToken state){
        List<OfficerSnapshot.Officer> result=new ArrayList<>();
        for(World.Officer o:w.officers){
            List<Integer> base=new ArrayList<>(),growth=new ArrayList<>(),xp=new ArrayList<>();
            if(w.officerAbilities.enabled())for(int i=0;i<5;i++){
                base.add(w.officerAbilities.base(o.id,i));growth.add(w.officerAbilities.growthCode(o.id,i));
                xp.add(w.officerAbilities.experience(o.id,i));
            }
            List<String> unknown=new ArrayList<>(Arrays.asList("sourceVariant","nativeId","courtesyName","biography"));
            if(base.isEmpty())unknown.addAll(Arrays.asList("base","growth","experience"));
            else for(int i=0;i<5;i++)if(growth.get(i)<0)unknown.add("growth:"+i);
            if(o.sex==World.Sex.UNKNOWN)unknown.add("sex");
            Lifecycle.Life life=w.life.life(o.id);if(life==null||life.birth==0)unknown.add("birth");
            if(life==null||life.expectedDeath==0)unknown.add("expectedDeath");
            if(life==null||life.appearance==0)unknown.add("appearance");
            result.add(new OfficerSnapshot.Officer(o.id,o.owner,o.cityId,o.unitId,o.loyalty,w.government.merit(o.id),
                w.government.commandLimit(o.id),w.treasures.held(o.id).size(),w.contests.injury(o.id),w.contests.injuryTurns(o.id),
                w.life.present(o.id),o.name,o.sex.name(),o.role.label,!w.life.present(o.id)?w.life.state(o.id).label:o.owner<0?"在野":w.faction(o.owner),
                location(w,o),status(w,o),w.governance.office(o),o.skillId,Skill.label(o.skillId),Skill.description(o.skillId),
                w.life.describe(o.id),w.loyalty.describe(o),w.relations.describe(o.id),w.treasures.describe(o.id),
                Arrays.asList(o.leadership,o.war,o.intelligence,o.politics,o.charm),base,growth,xp,
                Arrays.asList(o.aptitude[0],o.aptitude[1],o.aptitude[2],o.aptitude[3],o.aptitude[4],o.aptitude[5]),unknown));
        }
        return new OfficerSnapshot(state,result);
    }
    private static String location(World w,World.Officer o){
        if(!w.life.present(o.id))return w.life.state(o.id).label;
        if(w.government.captive(o.id))return w.government.locationLabel(w.government.prisoner(o.id));
        for(Domestic.Mission m:w.domestic.missions)if(m.contains(o.id))return w.city(m.sourceCity).name+" → "+w.city(m.targetCity).name;
        for(Recruitment.Mission m:w.recruitment.missions())if(m.actor==o.id||m.joined&&m.target==o.id)return w.recruitment.describe(m);
        for(Envoys.Mission m:w.envoys.missions())if(m.actor==o.id)return w.envoys.describe(m);
        if(o.unitId>=0)return "战场";
        World.City c=w.city(o.cityId);return c==null?"已退出战场":c.name;
    }
    private static String status(World w,World.Officer o){
        Strategy.OfficerState state=w.strategy.officerState(o.id);
        switch(state.activity){
            case DEAD:return "已故";
            case UNAPPEARED:return "未登场 · "+w.life.life(o.id).appearance+"年";
            case CAPTIVE:return w.government.status(o.id);
            case CONSTRUCTION:case TRANSFER:case TRANSPORT:return w.domestic.assignment(o.id);
            case OTHER_TASK:return o.otherTask+" · 剩"+state.remainingTurns+"旬";
            case DEPLOYED:{World.Unit u=w.unit(o.unitId);if(u!=null&&u.march!=null)return "行军 → "+w.marches.label(u.march)+(u.march.paused.isEmpty()?"":" · 暂停");return u!=null&&u.acted?"出征 · 已行动":"出征 · 待命";}
            case UNAFFILIATED:return "在野 · 待登用";
            case UNAVAILABLE:return "不可派遣";
            case ACTED:return "本旬已行动";
            default:return "闲置";
        }
    }
}
