package game.sanguo.runtime.query;

import game.sanguo.api.*;
import game.sanguo.core.*;
import java.util.*;

/** Read-only projection of saved facts, including explicit legacy unknowns. */
public final class OfficerQuery {
    private OfficerQuery(){}
    public static OfficerSnapshot capture(World w,StateToken state){
        List<OfficerSnapshot.Officer> result=new ArrayList<>();
        Map<Integer,PcOfficerInfo.Person> sourcePeople;boolean invalidSource=false;
        try{sourcePeople=PcOfficerInfo.saved(w);}catch(java.io.IOException e){sourcePeople=Collections.emptyMap();invalidSource=true;}
        Map<Integer,PcScenarioPeople.Person> nativePeople=new HashMap<>(),byNative=new HashMap<>();
        try{for(PcScenarioPeople.Person p:PcScenarioPeople.saved(w)){byNative.put(p.nativeId,p);if(p.officerId>=0)nativePeople.put(p.officerId,p);}}catch(java.io.IOException e){invalidSource=true;}
        Map<Integer,PcContestProfiles.Fact> contestFacts;try{contestFacts=PcContestProfiles.saved(w);}catch(java.io.IOException e){contestFacts=Collections.emptyMap();invalidSource=true;}
        for(World.Officer o:w.officers){
            List<Integer> base=new ArrayList<>(),growth=new ArrayList<>(),xp=new ArrayList<>();
            if(w.officerAbilities.enabled())for(int i=0;i<5;i++){
                base.add(w.officerAbilities.base(o.id,i));growth.add(w.officerAbilities.growthCode(o.id,i));
                xp.add(w.officerAbilities.experience(o.id,i));
            }
            List<String> unknown=new ArrayList<>(Arrays.asList("sourceVariant","nativeId","courtesyName","biography"));
            OfficerSnapshot.SourceInfo source=null;PcOfficerInfo.Person original=sourcePeople.get(o.id);
            if(original!=null&&original.worldName.equals(o.name)){
                unknown.clear();unknown.addAll(original.unknown);
                source=new OfficerSnapshot.SourceInfo(original.nativeId,original.sourceVariant,original.sourcePath,original.sourceSha,original.recordSha,
                    original.courtesy,original.courtesyRaw,original.biography,original.biographyResourceSha,original.biographyRenderedSha,original.unknown);
            }else if(original!=null)unknown.add("sourceIdentityChanged");
            PcScenarioPeople.Person nativePerson=nativePeople.get(o.id);
            if(nativePerson!=null){
                unknown.remove("nativeId");unknown.remove("sourceVariant");
                if(!nativePerson.courtesy.isEmpty()||nativePerson.courtesyRaw.isEmpty())unknown.remove("courtesyName");
                unknown.addAll(nativePerson.unknown);
                try{
                    PcScenarioIdentity.Source scenario=PcScenarioIdentity.saved(w);
                    String identity=nativePerson.strictIdentity?"canonical-identity-verified":"source-only-gaiji";
                    String facts=originalInformation(nativePerson,byNative);
                    PcContestProfiles.Fact contest=contestFacts.get(o.id);
                    if(contest!=null&&contest.nativeId==nativePerson.nativeId&&contest.recordSha.equals(nativePerson.recordSha))facts+="\n原性格："+contest.personality()+"\n原话术标记："+contest.talks();
                    if(source!=null)source=new OfficerSnapshot.SourceInfo(source.nativeId,source.sourceVariant,source.sourcePath,source.sourceSha,source.recordSha,source.courtesy,source.courtesyRaw,source.biography,source.biographyResourceSha,source.biographyRenderedSha,unknown,identity,facts);
                    else if(scenario!=null)source=new OfficerSnapshot.SourceInfo(nativePerson.nativeId,scenario.sourceVariant,scenario.path,scenario.sha,nativePerson.recordSha,nativePerson.courtesy,nativePerson.courtesyRaw,"","","",unknown,identity,facts);
                }catch(java.io.IOException e){unknown.add("sourceOpeningMetadataUnreadable");}
            }
            if(invalidSource)unknown.add("sourceMetadataUnreadable");
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
                Arrays.asList(o.aptitude[0],o.aptitude[1],o.aptitude[2],o.aptitude[3],o.aptitude[4],o.aptitude[5]),unknown,source));
        }
        return new OfficerSnapshot(state,result);
    }
    private static String originalInformation(PcScenarioPeople.Person p,Map<Integer,PcScenarioPeople.Person> byNative){
        String[] statuses={"君主","都督","太守","一般","在野","俘虜","未登","未發","死亡"};
        Integer status=p.fields.get(20);StringBuilder b=new StringBuilder("原文件人物记录（随本局保存）\n原姓名：").append(p.originalName);
        b.append("\n原身份：").append(status!=null&&status>=0&&status<statuses.length?statuses[status]:"未核实");
        for(int field:new int[]{12,13,15,16}){
            Integer target=p.fields.get(field);if(target==null||target<0)continue;
            String label=field==12?"父亲":field==13?"母亲":field==15?"配偶":"义兄标识";
            PcScenarioPeople.Person referenced=byNative.get(target);
            b.append("\n原").append(label).append("：").append(referenced==null||referenced.originalName.isEmpty()?"原编号"+target+"（人物资料未解码）":referenced.originalName);
        }
        int[] keys={11,14,17,22,43,44,45,46,47,48,49,50,51};
        String[] names={"血缘编号","世代","相性","登场预定君主编号","舌战得意话题编号","情义编号","野心编号","起用编号","性格编号","音声编号","汉室倾向编号","战略倾向编号","地元执编号"};
        for(int i=0;i<keys.length;i++){Integer value=p.fields.get(keys[i]);if(value!=null)b.append("\n").append(names[i]).append("：").append(value);}
        if(!p.strictIdentity)b.append("\n姓名含未解原字形；来源身份独立保存，尚未映射到标准人物。");
        return b.toString();
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
            case UNDISCOVERED:return "未发现 · 可通过所在据点搜索发现";
            case UNAPPEARED:return "未登场 · "+w.life.life(o.id).appearance+"年"+(w.life.state(o.id)==Lifecycle.State.SOURCE_WAIT?" · 原条件未核实":"");
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
