package game.sanguo.core;

import java.util.*;

/** Checks formation intelligence, skill precedence and real paid control commands. */
public final class ControlIntelligenceTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        boolean old=args.length>0&&args[0].equals("baseline118");
        War.Plot[] plots={War.Plot.CONFUSE,War.Plot.MISLEAD};
        // Leader/deputy on both sides; expected candidate, old CONFUSE, old MISLEAD.
        int[][] cases={{40,80,40,80,20,45,65},{80,40,80,40,20,45,65},
            {40,100,40,80,30,55,75},{40,80,40,100,10,35,55},
            {80,100,100,80,20,45,65},{20,20,20,20,20,45,65},
            {100,100,100,100,20,45,65},{0,0,100,100,5,5,15},{100,100,0,0,70,85,95}};
        for(int[] c:cases)for(War.Plot plot:plots){
            World w=ControlIntelligenceFixture.world(c[0],c[1],c[2],c[3],119);
            int expected=c[old?(plot==War.Plot.CONFUSE?5:6):4];
            check(w.war.plotChance(1,w.unit(2).hex,plot)==expected,"formation intelligence "+Arrays.toString(c)+plot);
            byte[] before=SaveCodec.encode(w);
            for(int i=0;i<20;i++)w.war.plotChance(1,w.unit(2).hex,plot);
            check(Arrays.equals(before,SaveCodec.encode(w)),"preview pure, including RNG");
            World copy=SaveCodec.decode(before);check(copy.war.plotChance(1,copy.unit(2).hex,plot)==expected,"save retains deputies and chance");
            w.officer(1).war=1;w.officer(3).leadership=1;
            check(w.war.plotChance(1,w.unit(2).hex,plot)==expected,"martial/leadership do not replace intelligence");
        }
        for(War.Plot plot:plots){
            Skill specialist=plot==War.Plot.CONFUSE?Skill.JILUE:Skill.YANDU;
            for(Skill skill:new Skill[]{Skill.XUSHI,Skill.SHENSUAN,specialist}){
                World w=ControlIntelligenceFixture.world(40,80,40,80,119);
                w.officer(1).skillId=skill.id;
                check(w.war.plotChance(1,w.unit(2).hex,plot)==(old?(plot==War.Plot.CONFUSE?45:65):20),"low-int skill holder cannot borrow deputy intelligence for guarantee");
                w.officer(4).skillId=skill.id;w.officer(4).intelligence=81;
                check(w.war.plotChance(1,w.unit(2).hex,plot)==100,"deputy holder superior intelligence guarantees");
                for(Skill immune:new Skill[]{Skill.DONGCHA,Skill.MINGJING,plot==War.Plot.CONFUSE?Skill.CHENZHUO:Skill.GUILV}){
                    w.officer(5).skillId=immune.id;
                    check(w.war.plotChance(1,w.unit(2).hex,plot)==0,"defensive deputy immunity first");
                }
            }
            World charm=ControlIntelligenceFixture.world(40,80,40,80,119);charm.officer(4).skillId=Skill.QINGGUO.id;
            check(charm.war.plotChance(1,charm.unit(2).hex,plot)==Math.min(100,2*(old?(plot==War.Plot.CONFUSE?45:65):20)),"existing all-male skill multiplier retained");
            int samples=10000,wins=0;int expected=old?(plot==War.Plot.CONFUSE?45:65):20;
            for(int i=0;i<samples;i++){
                World w=ControlIntelligenceFixture.world(40,80,40,80,1000003L*i+119);
                check(w.war.plot(1,w.unit(2).hex,plot).ok,"real legal command accepted");
                if(w.unit(2).status==(plot==War.Plot.CONFUSE?War.Status.CONFUSED:War.Status.MISLED))wins++;
                check(w.unit(1).acted&&w.unit(1).energy==85,"success/failure both pay one action and 15 energy");
            }
            check(Math.abs(wins*100.0/samples-expected)<2,"actual distribution near displayed chance");
            System.out.printf(Locale.ROOT,"%s phase=%s leader40/deputy80 both sides: preview=%d%% actual=%d/%d %.2f%%%n",plot,old?"baseline118":"candidate119",expected,wins,samples,wins*100.0/samples);
        }
        System.out.println("PASS CONTROL_INTELLIGENCE checks="+checks);
    }
}
