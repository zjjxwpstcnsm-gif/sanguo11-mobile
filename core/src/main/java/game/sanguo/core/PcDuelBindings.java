package game.sanguo.core;

import java.io.*;
import java.util.*;

/** Pure current-person binding. Native equipment is supplied by a separately
 * checked ownership snapshot, never inferred from project item IDs or names. */
final class PcDuelBindings {
    static PcDuelKernel.Actor actor(World w,int id,PcDuelKernel.OriginalSettings settings)throws IOException {
        if(!PcNativeItemPolicy.enabled(w))throw new IOException("当前存档没有原宝物身份策略");
        return actor(w,id,PcNativeItemPolicy.held(w,id),settings);
    }
    static PcDuelKernel.Actor actor(World w,int id,int[][]held,PcDuelKernel.OriginalSettings settings)throws IOException {
        World.Officer o=w.officer(id);var profile=PcContestProfiles.saved(w).get(id);var source=PcDuelSourceFacts.saved(w).get(id);
        if(o==null||profile==null||source==null||!w.life.present(id)||o.abilityProfile==null||profile.nativeId!=source.nativeId||!profile.recordSha.equals(source.recordSha))throw new IOException("原单挑当前人物来源未核实");
        for(int[]item:held)if(item.length!=2||item[0]<0||item[0]>=100||item[1]<0||item[1]>7)throw new IOException("原单挑携物来源无效");
        int age=w.life.age(id);if(age<0)throw new IOException("原单挑当前年龄未知");
        int currentInjury=w.contests.injury(id);int raw=w.officerAbilities.currentWithInjury(id,1,currentInjury);
        PcDuelKernel.Actor a=new PcDuelKernel.Actor(source.nativeId,profile.nativePersonality,0,PcDuelKernel.treasureAiBonus(held),true,o.role==Strategy.Role.RULER);
        a.originalAge=age;a.raw489080=raw;a.warByInjury=new int[4];
        for(int injury=0;injury<4;injury++)a.warByInjury[injury]=PcDuelKernel.duelWar(w.officerAbilities.currentWithInjury(id,1,injury),source.nativeId,age,settings.rawLifeOption,true);
        a.war=a.warByInjury[currentInjury];
        // Actual4890f0(mask32) is the primary native E8 skill. Current editor
        // replacement uses its explicit content source ID; no extra-skill guess.
        String skill=o.skillId;if(skill==null||!skill.equals("none")&&Skill.find(skill)==null)throw new IOException("原单挑当前主特技来源未知");
        a.sourceInjuryProtection=ContentRuntime.skill("skill-032").id.equals(skill);
        // Original47a690 resolves the person's current owner, then force
        // virtual48 (player slot0..7), even while that person is in a city.
        // This campaign has one player force; manual army control47a6d0 is a
        // separate admission binding and must not change numerical difficulty.
        a.virtual48Value=o.owner>=0&&o.owner==w.player;
        return a;
    }
    /** Original508890 inputs. Original internal family root and sworn group
     * stay distinct from public parents or a project ancestor closure. */
    static PcDuelKernel.SupportFacts support(World w,int candidate,int own,int other,PcDuelRuntimeFacts.State runtime,int currentRawLoyalty)throws IOException {
        if(currentRawLoyalty<0||currentRawLoyalty>255)throw new IOException("原单挑支援来源缺失");
        return supportFacts(w,candidate,own,other,runtime,currentRawLoyalty);
    }
    /** Unknown raw remains unknown. Original508890 alone decides whether the
     * current relation/ruler/round branch actually consumes this strict read. */
    static PcDuelKernel.SupportFacts supportCurrent(World w,int candidate,int own,int other,PcDuelRuntimeFacts.State runtime)throws IOException {
        var facts=supportFacts(w,candidate,own,other,runtime,-1);
        facts.rawLoyaltyGetter=()->{try{return PcDuelRawLoyalty.current(w,candidate);}catch(IOException e){throw new java.io.UncheckedIOException(e);}};
        return facts;
    }
    private static PcDuelKernel.SupportFacts supportFacts(World w,int candidate,int own,int other,PcDuelRuntimeFacts.State runtime,int currentRawLoyalty)throws IOException {
        var c=PcDuelSourceFacts.saved(w).get(candidate);var a=PcDuelSourceFacts.saved(w).get(own);var b=PcDuelSourceFacts.saved(w).get(other);
        if(c==null||a==null||b==null||runtime==null)throw new IOException("原单挑支援来源缺失");
        var cp=runtime.people.get(c.nativeId);var ap=runtime.people.get(a.nativeId);var profile=PcContestProfiles.saved(w).get(candidate);World.Officer helper=w.officer(candidate),leader=w.officer(own);int gap=Loyalty.distance(helper,leader);if(cp==null||ap==null||profile==null||gap<0)throw new IOException("原单挑支援人物输入未知");
        // Public relationship edits need explicit native group/parent mutation
        // semantics; source initial groups may not silently overwrite edits.
        var facts=PcDuelSourceFacts.saved(w);PcDuelRecruitmentAdmission.unchangedGroup(w,candidate,runtime,facts);PcDuelRecruitmentAdmission.unchangedGroup(w,own,runtime,facts);
        boolean sworn=candidate!=own&&PcDuelSwornPolicy.current(w,cp.nativeId)>=0&&PcDuelSwornPolicy.current(w,cp.nativeId)==PcDuelSwornPolicy.current(w,ap.nativeId);
        boolean family=PcDuelRecruitmentAdmission.sameInternalFather(w,candidate,own);
        int threshold=80-10*profile.nativePersonality;
        // Original47a600 validity is separate from playable presence. Ordinary
        // crew admission must reject inactive persons before invoking this getter.
        int[]v={1,threshold,sworn?1:0,w.relations.spouse(candidate)==own?1:0,w.relations.dislikes(candidate,other)?1:0,w.relations.dislikes(candidate,own)?1:0,w.relations.likes(candidate,own)?1:0,family?1:0,leader.role==Strategy.Role.RULER?1:0,leader.owner>=0&&leader.owner<42?1:0,helper.owner==leader.owner?1:0,currentRawLoyalty,cp.birthplace==ap.birthplace?1:0,gap};
        return new PcDuelKernel.SupportFacts(v);
    }
    private PcDuelBindings(){}
}
