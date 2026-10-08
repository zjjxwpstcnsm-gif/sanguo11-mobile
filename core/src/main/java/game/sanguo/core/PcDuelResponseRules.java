package game.sanguo.core;
import java.util.*;

/** Original507e20. A response estimate itself may draw RNG; UI previews must
 * use a complete RNG copy and submission must evaluate the current state. */
final class PcDuelResponseRules {
    /** Original4843a0(point,3,589a50,nominatedForce,0): scan rings1..3
     * around the target, requiring own completed native11 DRUM. */
    static boolean currentDrumSupport(World w,int nominated,Hex target)throws java.io.IOException {
        var o=w.officer(nominated);if(o==null||!w.life.present(nominated)||target==null||!w.inside(target)||!w.pcSourceFrame||!NationalMap.ID.equals(w.mapId)||PcScenarioIdentity.saved(w)==null||!PcDuelSourceFacts.saved(w).containsKey(nominated))throw new java.io.IOException("原应战太鼓台当前人物/地图/目标来源不同");
        for(var s:w.war.structures)if(s.kind==War.StructureKind.DRUM&&s.complete&&s.owner==o.owner){int distance=s.hex.distance(target);if(distance>0&&distance<=3)return true;}return false;
    }
    /** Original58a5e0. Counter eligibility/chance reads no random state. */
    static int counter(int ownNative,int ownTroops,int otherTroops,int ownStrength,int otherStrength,PcDuelAdmissionRules.Candidate other,int otherIntelligence){
        if(ownNative<0||ownNative>=1100||ownTroops<0||ownTroops>65535||otherTroops<0||otherTroops>65535||ownStrength<0||otherStrength<0||otherIntelligence<0||otherIntelligence>255)throw new IllegalArgumentException("原反击输入范围未知");
        int own=Math.max(1000,ownTroops),enemy=Math.max(1000,otherTroops);
        if(enemy>=own*3&&enemy-own>=6000||ownStrength>otherStrength*2||other.personality!=3||other.war<70||other.health<70)return 0;
        int chance=Math.max(1,Math.max(0,PcDuelAdmissionRules.score(other,false)-otherIntelligence)/5);
        if(other.nativeId==432||other.nativeId==660){chance+=10;if(other.nativeId==432&&ownNative==660||other.nativeId==660&&ownNative==432)return 100;}
        return chance;
    }
    static int humanSelectionChance(int response,int maximumCounter){return Math.max(0,Math.min(100,maximumCounter+response*(100-maximumCounter)/100));}
    static int currentCounter(World w,int nominated,int opponent,World.Unit left,World.Unit right)throws java.io.IOException {
        if(left==null||right==null||w.unit(left.id)!=left||w.unit(right.id)!=right||!w.army.contains(left,nominated)||!w.army.contains(right,opponent))throw new java.io.IOException("原反击当前编队或指定人物不同");
        List<PcDuelAdmissionRules.Candidate>own=new ArrayList<>(),other=new ArrayList<>();for(var o:w.army.crew(left))own.add(PcDuelAdmissionRules.current(w,o.id));for(var o:w.army.crew(right))other.add(PcDuelAdmissionRules.current(w,o.id));
        var a=PcDuelAdmissionRules.current(w,nominated);var b=PcDuelAdmissionRules.current(w,opponent);
        int ownStrength=PcDuelAdmissionRules.crewStrength(own,true,nominated,true,w.relations::dislikes),otherStrength=PcDuelAdmissionRules.crewStrength(other,true,opponent,true,w.relations::dislikes);
        return counter(a.nativeId,left.troops,right.troops,ownStrength,otherStrength,b,w.officer(opponent).intelligence);
    }
    /** Original58a7f0 uses the highest counter chance, retains the first
     * candidate on ties, then invokes exactly one4721d0 percentage check. */
    static int currentCounterSelection(World w,int nominated,World.Unit left,World.Unit right,PcDuelKernel.Random random)throws java.io.IOException {
        if(left==null||right==null||w.unit(left.id)!=left||w.unit(right.id)!=right||!w.army.contains(left,nominated))throw new java.io.IOException("原反击选择当前编队不同");
        int best=-1,maximum=0;for(var o:w.army.crew(right)){int chance=currentCounter(w,nominated,o.id,left,right);if(chance>maximum){maximum=chance;best=o.id;}}
        return random.percent(maximum)?best:-1;
    }
    static final class CandidateChance {
        final int officerId,chance;
        CandidateChance(int officerId,int chance){this.officerId=officerId;this.chance=chance;}
    }
    /** Original58af20 first human picker evaluates every valid original
     * formation slot in order, before the graphical choice. Its estimation
     * draws belong to the command plan; a query supplies a detached RNG copy. */
    static List<CandidateChance> currentOwnMenu(World w,World.Unit left,World.Unit right,boolean formationSupport,PcDuelKernel.Random random,PcDuelKernel.OriginalSettings settings)throws java.io.IOException {
        if(left==null||right==null||w.unit(left.id)!=left||w.unit(right.id)!=right)throw new java.io.IOException("原人控候补当前部队无效");
        List<CandidateChance>rows=new ArrayList<>();for(var person:w.army.crew(left))rows.add(new CandidateChance(person.id,currentHumanSelection(w,person.id,left,right,formationSupport,random,settings)));return List.copyOf(rows);
    }
    static final class Selection {
        final int officer;final boolean counter;
        Selection(int officer,boolean counter){this.officer=officer;this.counter=counter;}
    }
    /** Original58b400 second AI selection for a normal (tactic−1) challenge:
     * one maximum counter roll, otherwise58ac30 response and eligible best. */
    static Selection currentOpponent(World w,int nominated,World.Unit left,World.Unit right,boolean formationSupport,PcDuelKernel.Random random,PcDuelKernel.OriginalSettings settings)throws java.io.IOException {
        int selected=currentCounterSelection(w,nominated,left,right,random);if(selected>=0)return new Selection(selected,true);
        int chance=currentResponse(w,nominated,left,right,formationSupport,random,settings);
        if(!random.percent(chance))return new Selection(-1,false);
        var crew=new ArrayList<PcDuelAdmissionRules.Candidate>();for(var o:w.army.crew(right))crew.add(PcDuelAdmissionRules.current(w,o.id));return new Selection(PcDuelAdmissionRules.best(crew,false),false);
    }
    static Selection currentOpponent(World w,int nominated,World.Unit left,World.Unit right,PcDuelKernel.Random random,PcDuelKernel.OriginalSettings settings)throws java.io.IOException {
        return currentOpponent(w,nominated,left,right,currentDrumSupport(w,nominated,right.hex),random,settings);
    }
    /** Original58ad60 takes the maximum of three counter candidates before
     * one response estimation. Every call uses a detached or submission RNG. */
    static int currentHumanSelection(World w,int nominated,World.Unit left,World.Unit right,boolean formationSupport,PcDuelKernel.Random random,PcDuelKernel.OriginalSettings settings)throws java.io.IOException {
        if(left==null||right==null||w.unit(left.id)!=left||w.unit(right.id)!=right||!w.army.contains(left,nominated))throw new java.io.IOException("原人控候补当前编队或指定人物不同");
        int maximum=0;for(var o:w.army.crew(right))maximum=Math.max(maximum,currentCounter(w,nominated,o.id,left,right));
        return humanSelectionChance(currentResponse(w,nominated,left,right,formationSupport,random,settings),maximum);
    }
    /** Original58a8a0 contribution from valid units' current C9/CA bytes.
     * These bytes must come from original496570 binding, not Army.attackPower.
     * Original4720f0 is positive right-minus-left troop difference. */
    static int unitFactor(int leftTroops,int rightTroops,int leftAttack,int leftDefense,int rightAttack,int rightDefense){
        if(leftTroops<0||leftTroops>65535||rightTroops<0||rightTroops>65535||leftAttack<0||leftAttack>255||leftDefense<0||leftDefense>255||rightAttack<0||rightAttack>255||rightDefense<0||rightDefense>255)throw new IllegalArgumentException("原单挑当前部队数值范围未知");
        int left=Math.max(1000,leftTroops),right=Math.max(1000,rightTroops),gap=Math.max(0,right-left)/100;
        return left*200/(left+right)-(rightAttack-leftDefense)/4+(leftAttack-rightDefense)/4-gap*gap/150;
    }
    static final class Opening {
        final int side,chance;
        Opening(int side,int chance){this.side=side;this.chance=chance;}
    }
    /** Original58a8a0 after current getter binding. openingChance is the
     * original50bb90 output, signed when the opponent's raw WAR is higher.
     * Formation support is original4843a0, not map render proximity. */
    static int response(int leftTroops,int rightTroops,int factor,int leftScore,int rightScore,int leftCrew,int rightCrew,int leftNative,int rightNative,int personality,boolean ruler,boolean dislikes,boolean confused,int openingChance,boolean formationSupport){
        if(leftTroops<0||leftTroops>65535||rightTroops<0||rightTroops>65535||leftScore<1||rightScore<1||leftCrew<0||rightCrew<0||personality<0||personality>3||leftNative<0||rightNative<0||openingChance< -200||openingChance>200)throw new IllegalArgumentException("原应战人物与部队输入范围未知");
        // Original early return precedes opening estimation/RNG consumption.
        if(earlyResponse(leftCrew,rightCrew,rightScore,leftNative,rightNative,personality))return factor;
        int base=(rightCrew-leftCrew)/3+(rightScore-leftScore)/2+Math.max(1000,leftTroops)/Math.max(1000,rightTroops)+30;
        int mood=new int[]{0,5,10,15}[personality]/(ruler?2:1);
        int chance=Math.max(0,Math.min(99,(base+mood)*(10+(dislikes?1:0)+(confused?2:0))*(100-openingChance)/1000));
        chance=Math.max(0,Math.min(100,chance*factor/100));
        return formationSupport?Math.min(100,chance+20):chance;
    }
    static boolean earlyResponse(int leftCrew,int rightCrew,int leftScore,int leftNative,int rightNative,int personality){
        return leftCrew<=rightCrew*3/2&&(rightNative==660&&(leftNative==432||leftScore>=90)||rightNative==432&&(leftNative==660||leftScore>=90)||rightNative!=660&&rightNative!=432&&personality==3&&leftScore>116);
    }
    /** Original50bb90 held-kind4 adapter. Its result is used only for the
     * out-chance; RNG still advances even when the caller discards the side. */
    static Opening currentOpening(World w,int left,int right,PcDuelKernel.Random random,PcDuelKernel.OriginalSettings settings)throws java.io.IOException {
        int[][] own=PcNativeItemPolicy.held(w,left),other=PcNativeItemPolicy.held(w,right);
        boolean ownFlag=Arrays.stream(own).anyMatch(item->item[1]==4),otherFlag=Arrays.stream(other).anyMatch(item->item[1]==4);
        return opening(PcDuelBindings.actor(w,left,own,settings),PcDuelBindings.actor(w,right,other,settings),ownFlag,otherFlag,random,settings);
    }
    /** Current source identities/crew/relations share the admission getter.
     * The caller must supply verified current496570 C9/CA and4843a0 facts;
     * they deliberately cannot fall back to engineering Army.attackPower. */
    static int currentResponse(World w,int nominated,World.Unit left,World.Unit right,int leftAttack,int leftDefense,int rightAttack,int rightDefense,boolean formationSupport,PcDuelKernel.Random random,PcDuelKernel.OriginalSettings settings)throws java.io.IOException {
        if(left==null||right==null||w.unit(left.id)!=left||w.unit(right.id)!=right||!w.army.contains(left,nominated))throw new java.io.IOException("原应战当前部队或指定人物不同");
        List<PcDuelAdmissionRules.Candidate>own=new ArrayList<>(),other=new ArrayList<>();
        for(var o:w.army.crew(left))own.add(PcDuelAdmissionRules.current(w,o.id));
        for(var o:w.army.crew(right))other.add(PcDuelAdmissionRules.current(w,o.id));
        int opponent=PcDuelAdmissionRules.best(other,false);if(opponent<0)return 0;
        var a=PcDuelAdmissionRules.current(w,nominated);var b=PcDuelAdmissionRules.current(w,opponent);
        int ownScore=PcDuelAdmissionRules.score(a,true),otherScore=PcDuelAdmissionRules.score(b,true);
        int ownCrew=PcDuelAdmissionRules.crewStrength(own,true,nominated,true,w.relations::dislikes),otherCrew=PcDuelAdmissionRules.crewStrength(other,true,opponent,true,w.relations::dislikes);
        int factor=unitFactor(left.troops,right.troops,leftAttack,leftDefense,rightAttack,rightDefense);
        // Native early return occurs before50bb90. Preview/submit must keep
        // both its returned value and its zero RNG consumption.
        if(earlyResponse(ownCrew,otherCrew,otherScore,a.nativeId,b.nativeId,b.personality))return factor;
        int chance=currentOpening(w,nominated,opponent,random,settings).chance;
        if(b.war>a.war)chance=-chance;
        return response(left.troops,right.troops,factor,ownScore,otherScore,ownCrew,otherCrew,a.nativeId,b.nativeId,b.personality,b.ruler,w.relations.dislikes(opponent,nominated),right.status==War.Status.CONFUSED,chance,formationSupport);
    }
    static int currentResponse(World w,int nominated,World.Unit left,World.Unit right,boolean formationSupport,PcDuelKernel.Random random,PcDuelKernel.OriginalSettings settings)throws java.io.IOException {
        var own=PcDuelUnitStats.currentCombat(w,left);var other=PcDuelUnitStats.currentCombat(w,right);
        return currentResponse(w,nominated,left,right,own.attack,own.defense,other.attack,other.defense,formationSupport,random,settings);
    }
    static Opening opening(PcDuelKernel.Actor left,PcDuelKernel.Actor right,boolean firstFlag,boolean secondFlag,PcDuelKernel.Random random,PcDuelKernel.OriginalSettings settings){
        if(!left.valid||!right.valid)return new Opening(-1,-999);
        int winner=left.war>right.war?0:left.war<right.war?1:random.percent(70)?0:1;
        var strong=winner==0?left:right;var weak=winner==0?right:left;
        int high=strong.war,low=Math.max(weak.war,high/2),h2=high*high,l2=low*low;
        int chance=Math.max(0,Math.min(100,(37-l2*74/(l2+h2))*(h2/l2)));
        // The original reads argument3 at fixed stack offset, not the winning
        // side's item bit. Argument4 is retained by the ABI but not consumed.
        if(firstFlag)chance+=5;
        boolean weakHuman=Objects.requireNonNull(weak.virtual48Value,"source virtual48"),strongHuman=Objects.requireNonNull(strong.virtual48Value,"source virtual48");
        if(settings.difficultyValid){if(weakHuman&&!strongHuman){if(settings.rawDifficulty==1)chance=chance*4/3;else if(settings.rawDifficulty==2)chance=chance*3/2;}else if(strongHuman&&!weakHuman){if(settings.rawDifficulty==1)chance=chance*4/5;else if(settings.rawDifficulty==2)chance/=2;}}
        int id=strong.nativeId,bonus=id==98||id==432?5:id==144||id==395||id==515?3:id==660?10:0;
        if(id==185){int age=strong.originalAge;bonus=!settings.lifeValid||settings.rawLifeOption==3?5:age<60?1:age<65?2:age<70?3:age<80?4:age<85?5:age<90?10:15;}
        chance+=bonus;
        if(strong.raw489080<0)throw new IllegalArgumentException("source raw489080 absent");
        if(strong.raw489080<70||!firstFlag&&high-low<5)chance=0;
        if(weak.nativeId==98||weak.nativeId==144||weak.nativeId==395||weak.nativeId==432||weak.nativeId==515||weak.nativeId==660)chance=0;
        return new Opening(random.percent(chance)?winner:-1,chance);
    }
    private PcDuelResponseRules(){}
}
