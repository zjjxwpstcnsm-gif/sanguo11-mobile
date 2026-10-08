package game.sanguo.core;
import java.io.*;
import java.util.*;
/** Original automatic disposition for field-unit context. A detached plan
 * owns original RNG draws; unsupported callbacks remain explicit errors. */
final class PcDuelAiDisposition {
    static final class Plan {
        final int choice,seed,draws;final boolean recruitmentAdmitted;
        Plan(int choice,PcDuelKernel.Random random){this.choice=choice;seed=random.state;draws=random.draws;recruitmentAdmitted=choice==PcDuelDisposition.RECRUIT;}
    }
    static final class Retention {
        final int nativeHome,troops,rankSalarySum,captiveCount;final List<Integer>normalNatives;
        Retention(int home,int troops,int ranks,int captives,List<Integer>people){nativeHome=home;this.troops=troops;rankSalarySum=ranks;captiveCount=captives;normalNatives=List.copyOf(people);}
        boolean retain(){return PcDuelAiDispositionRules.retain(troops,rankSalarySum,captiveCount);}
    }
    static Retention retention(World w,int captor)throws IOException {
        var data=PcGovernorPolicy.data(w);var assigned=data.assignments.get(captor);if(assigned==null||assigned.home<0)return new Retention(-1,0,0,0,List.of());int siteId=PcPersonnelReturnRules.projectSite(w,assigned.home);var site=w.city(siteId);if(site==null)return new Retention(-1,0,0,0,List.of());var facts=PcDuelSourceFacts.saved(w);List<Integer>normal=new ArrayList<>();int salary=0,captives=0;
        for(var o:w.officers)if(o.cityId==siteId&&w.life.present(o.id)&&!data.assignments.containsKey(o.id))throw new IOException("当前拘留驻点的自定义/额外人员原行政身份尚未绑定");
        // Original487bc0 administrative station roster includes deployed people.
        // mask15 is status0..3, mask32 is captive status5, not a skill bit.
        for(var e:data.assignments.entrySet()){var o=w.officer(e.getKey());if(o==null||e.getValue().home!=assigned.home||!w.life.present(o.id)||w.government.captive(o.id)||o.owner<0)continue;var f=facts.get(o.id);if(f==null)throw new IOException("原拘留行政人员身份未覆盖");normal.add(f.nativeId);if(o.role!=Strategy.Role.RULER){int rank=PcGovernorPolicy.nativeRank(w,o);salary+=PcOfficerRanks.all().get(rank>=0&&rank<=80?rank:80).salary;}}
        for(var p:w.government.prisoners())if(p.cityId==siteId&&p.unitId<0)captives++;
        Collections.sort(normal);return new Retention(assigned.home,site.troops,salary,captives,normal);
    }
    static Plan preview(World w,PcDuelCampaign duel,World.Unit winner,World.Unit loser,int targetId,Map<Integer,Integer>terminalInjuries)throws IOException {
        if(!duel.settings.separateDeath||!duel.settings.deathValid||duel.settings.rawDeathOption<0||duel.settings.rawDeathOption>2)throw new IOException("原自动处分需要独立已保存的战死设置");var target=w.officer(targetId);var captor=PcDuelAiActorPolicy.actor(w,winner);if(target==null||captor==null||loser==null||!w.life.present(targetId)||!w.life.present(captor.id)||w.government.captive(targetId))throw new IOException("原自动处分当前人物不同");var random=new PcDuelKernel.Random(duel.state.random.state);
        if(w.relations.dislikes(captor.id,targetId))return new Plan(PcDuelDisposition.EXECUTE,random);
        boolean ruler=target.role==Strategy.Role.RULER;
        if(ruler){
            if(!PcDuelCampaignPolicy.enabled(w)||PcDuelCampaignPolicy.read(w).version!=3||PcGovernorPolicy.data(w).format!=3)throw new IOException("旧保存没有明确君主AI处分策略，完整待办已保留");
            if(!w.governance.grades.containsKey(target.owner)||!w.governance.grades.containsKey(captor.owner))throw new IOException("原AI君主处分的当前爵位来源缺失");
            // Original436740 is force+3c, named爵位/property5, not Han attitude.
            // Both nonzero makes4adb30 false regardless of481910. Native0
            // maps to project9. Court movement/control for imperial cases
            // remains an explicit bound; no native700 project ID is guessed.
            if(w.governance.grade(target.owner)==9||w.governance.grade(captor.owner)==9)throw new IOException("原AI帝号处分的当前汉帝行政驻点尚未绑定");
        }var runtime=PcDuelRuntimeFacts.saved(w);var facts=PcDuelSourceFacts.saved(w);if(runtime==null||!facts.containsKey(targetId)||!facts.containsKey(captor.id))throw new IOException("原自动处分当前来源连接缺失");PcDuelRecruitmentAdmission.unchangedGroup(w,targetId,runtime,facts);PcDuelRecruitmentAdmission.unchangedGroup(w,captor.id,runtime,facts);
        int targetInjury=terminalInjuries.getOrDefault(targetId,w.contests.injury(targetId)),actorInjury=terminalInjuries.getOrDefault(captor.id,w.contests.injury(captor.id));boolean hasCity=w.governance.cityCount(target.owner)>0;
        var recruitment=PcDuelRecruitmentAdmission.preview(w,targetId,captor.id,hasCity?1:2,w.officerAbilities.currentWithInjury(captor.id,4,actorInjury));if(recruitment.decision(random))return new Plan(PcDuelDisposition.RECRUIT,random);
        var t=runtime.people.get(facts.get(targetId).nativeId);var a=runtime.people.get(facts.get(captor.id).nativeId);int[]current=new int[5];for(int stat=0;stat<5;stat++)current[stat]=w.officerAbilities.currentWithInjury(targetId,stat,targetInjury);var ts=PcDebateCampaignRules.source(w,targetId);var as=PcDebateCampaignRules.source(w,captor.id);var profile=PcContestProfiles.saved(w).get(captor.id);if(profile==null)throw new IOException("原AI性格连接缺失");
        if(PcDuelAiDispositionRules.execution(ruler,current,w.government.merit(targetId),ts.field(45),as.field(45),captor.honor-1,profile.nativePersonality,duel.settings.rawDeathOption,PcDuelSwornPolicy.current(w,t.nativeId)==PcDuelSwornPolicy.current(w,a.nativeId),w.relations.spouse(captor.id)==targetId,w.relations.likes(captor.id,targetId),random))return new Plan(PcDuelDisposition.EXECUTE,random);
        if(ruler||!hasCity)return new Plan(PcDuelDisposition.RELEASE,random);var hold=retention(w,winner.officerId);return new Plan(hold.nativeHome>=0&&hold.retain()?PcDuelDisposition.DETAIN:PcDuelDisposition.RELEASE,random);
    }
    private PcDuelAiDisposition(){}
}
