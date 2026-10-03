package game.sanguo.core;

import java.util.*;

/** Real commands, fixed distinct seeds, unchanged skill precedence and pure previews. */
public final class ControlProbabilityTest {
    static int checks;
    static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
    static World fixture(){World w=CombatSceneFixture.world("counter");w.unit(1).energy=100;return w;}
    public static void main(String[] args)throws Exception {
        boolean baseline=args.length>0&&args[0].equals("baseline");
        World w=fixture();World.Unit a=w.unit(1),b=w.unit(2);
        int plot=w.war.plotChance(1,b.hex,War.Plot.CONFUSE),hit=w.war.tacticChance(1,2,War.Tactic.SPIRAL),confuse=w.combat.spiralConfusionChance(a,b);
        check(plot==(baseline?65:20),"equal-intelligence ordinary confusion");
        check(confuse==(baseline?25:15),"equal-war ordinary spiral conditional confusion");
        check(w.war.plotChance(1,b.hex,War.Plot.MISLEAD)==(baseline?65:20),"equal-intelligence ordinary false report");
        check(w.war.plotChance(1,b.hex,War.Plot.FIRE)==65,"non-control plot base unchanged");
        byte[] before=SaveCodec.encode(w);
        for(int i=0;i<30;i++){w.war.tacticPreview(1,2,War.Tactic.SPIRAL);w.war.plotChance(1,b.hex,War.Plot.CONFUSE);}
        check(Arrays.equals(before,SaveCodec.encode(w)),"preview never consumes authority/RNG");
        for(int war:new int[]{1,50,90,100})for(int defense:new int[]{1,50,90,100}){
            w.officer(1).war=war;w.officer(3).war=defense;
            int p=w.combat.spiralConfusionChance(a,b);check(p>0&&p<=(baseline?40:25),"bounded ordinary spiral");
        }
        w=fixture();a=w.unit(1);b=w.unit(2);w.officer(1).skillId=Skill.QIANGSHEN.id;
        check(w.combat.spiralConfusionChance(a,b)==100,"spear god critical guaranteed confusion preserved");
        for(Skill skill:new Skill[]{Skill.SHENSUAN,Skill.XUSHI,Skill.JILUE}){
            w.officer(1).skillId=skill.id;w.officer(1).intelligence=100;
            check(w.war.plotChance(1,b.hex,War.Plot.CONFUSE)==100,"guaranteed plot skill "+skill);
            for(Skill immune:new Skill[]{Skill.DONGCHA,Skill.CHENZHUO,Skill.MINGJING}){
                w.officer(3).skillId=immune.id;check(w.war.plotChance(1,b.hex,War.Plot.CONFUSE)==0,"immunity before guaranteed "+immune);
            }
            w.officer(3).skillId="none";
        }
        int samples=10000,plotWins=0,spiralHits=0,spiralWins=0;
        for(int i=0;i<samples;i++){
            w=fixture();w.strategy.setSeed(1000003L*i+118);b=w.unit(2);
            check(w.war.plot(1,b.hex,War.Plot.CONFUSE).ok,"actual plot accepted");
            if(b.status==War.Status.CONFUSED)plotWins++;
            check(w.unit(1).acted&&w.unit(1).energy==85,"plot action/energy consumed on success and failure");
            w=fixture();w.strategy.setSeed(1000003L*i+118);b=w.unit(2);int troops=b.troops;
            check(w.war.tactic(1,2,War.Tactic.SPIRAL).ok,"actual spiral accepted");
            if(b.troops<troops)spiralHits++;if(b.status==War.Status.CONFUSED)spiralWins++;
            check(w.unit(1).acted&&w.unit(1).energy==80,"spiral action/energy consumed on success and failure");
        }
        check(Math.abs(plotWins*100.0/samples-plot)<2,"actual plot distribution");
        check(Math.abs(spiralHits*100.0/samples-hit)<2,"tactic hit distribution unchanged");
        check(Math.abs(spiralWins*100.0/spiralHits-confuse)<2,"actual conditional spiral distribution");
        System.out.printf(Locale.ROOT,"PASS CONTROL %d checks; phase=%s samples=%d plot=%d/%d (%.2f%%), spiral hits=%d/%d (%.2f%%), confusion=%d/%d (%.2f%% of casts, %.2f%% of hits)%n",checks,baseline?"baseline":"candidate",samples,plotWins,samples,plotWins*100.0/samples,spiralHits,samples,spiralHits*100.0/samples,spiralWins,samples,spiralWins*100.0/samples,spiralWins*100.0/spiralHits);
    }
}
