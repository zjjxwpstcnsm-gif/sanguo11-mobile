package game.sanguo.core;
import java.io.*;
import java.util.*;

/** Original589ac0/589ca0 candidate score and stable formation ordering.
 * Acceptance/RNG and ordinary command costs are separate original functions. */
final class PcDuelAdmissionRules {
    static final class Candidate {
        final int id,nativeId,health,war,personality,treasureBonus;
        final boolean ruler;
        Candidate(int id,int nativeId,int health,int war,int personality,int treasureBonus,boolean ruler){
            if(id<0||nativeId<0||nativeId>=1100||health<0||health>100||war<0||war>255||personality<0||personality>3||treasureBonus<0||treasureBonus>30)throw new IllegalArgumentException("原单挑候补字段无效");
            this.id=id;this.nativeId=nativeId;this.health=health;this.war=war;this.personality=personality;this.treasureBonus=treasureBonus;this.ruler=ruler;
        }
    }
    static Candidate current(World w,int id)throws IOException {
        var person=w.officer(id);var profile=PcContestProfiles.saved(w).get(id);var source=PcDuelSourceFacts.saved(w).get(id);
        if(person==null||profile==null||source==null||!w.life.present(id)||profile.nativeId!=source.nativeId||!profile.recordSha.equals(source.recordSha)||person.abilityProfile==null)throw new IOException("原单挑候补人物来源未核实");
        return new Candidate(id,source.nativeId,PcDuelHealthPolicy.health(w,id),person.war,profile.nativePersonality,PcDuelKernel.treasureAiBonus(PcNativeItemPolicy.held(w,id)),person.role==Strategy.Role.RULER);
    }
    static int score(Candidate c,boolean allowLowHealth){
        if(!allowLowHealth&&c.health<80-10*c.personality)return 0;
        // Exact single precision source constants promoted to the numerical
        // calculation. WAR is current489080, without duel-specific age bonus.
        double nativeValue=(c.health+200.0)*(double)Float.intBitsToFloat(0x3c888889)*(c.war*c.war)*(double)Float.intBitsToFloat(0xbb23d70a);
        int score=Math.max(1,c.treasureBonus-(int)nativeValue);
        return c.ruler&&c.war<95?Math.max(1,score-20):score;
    }
    private static int nativeBonus(int nativeId){
        switch(nativeId){case 40:case 144:case 568:return 15;case 98:case 248:case 370:return 5;case 432:case 660:return 25;default:return 0;}
    }
    static int best(List<Candidate>crew,boolean allowLowHealth){return best(crew,true,allowLowHealth);}
    static int best(List<Candidate>crew,boolean validOriginalUnit,boolean allowLowHealth){
        if(!validOriginalUnit)return -1;
        if(crew.size()>3)throw new IllegalArgumentException("原单挑编队超过三人");
        int best=-1,high=0;
        for(Candidate person:crew){int score=score(person,allowLowHealth);if(score<=0)continue;score+=5*person.personality+nativeBonus(person.nativeId);if(score>high){high=score;best=person.id;}}
        return best;
    }
    /** Original58a200: current crew contribution for this nominated fighter.
     * The original x87 flags include health equal to its threshold.
     * The caller supplies current saved dislike facts, never source initial ties. */
    static int crewStrength(List<Candidate>crew,boolean validOriginalUnit,int nominated,boolean validNominee,java.util.function.BiPredicate<Integer,Integer>dislikes){
        if(!validOriginalUnit)return 0;
        if(crew.size()>3)throw new IllegalArgumentException("原单挑编队超过三人");
        int nominee=validNominee?nominated:best(crew,true,false);
        if(nominee<0)return 0;
        int strength=0;
        for(Candidate c:crew)if(c.health>=80-10*c.personality&&!dislikes.test(c.id,nominee))strength+=score(c,true);
        return strength;
    }
    private PcDuelAdmissionRules(){}
}
