package game.sanguo.core;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Declared native terminal inputs; no ordinary admission/capture claim. */
public final class PcDuelSettlementTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why+" checks="+checks);}
    public static void main(String[]args)throws Exception{
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);byte[]world=SaveCodec.encode(w),model=null;int[]ids=new int[6],natives=null;int rows=0;
        for(String line:Files.readAllLines(Path.of(args[0]))){if(line.startsWith("#"))continue;String[]p=line.split("\t");if(p[0].equals("MODEL")){model=PcDuelKernel.hex(p[1]);}else if(p[0].equals("NATIVES")){natives=Arrays.stream(p[1].split(",")).mapToInt(Integer::parseInt).toArray();for(int i=0;i<6;i++){final int n=natives[i];ids[i]=PcDuelSourceFacts.saved(w).values().stream().filter(f->f.nativeId==n).findFirst().orElseThrow().officerId;}}
            else if(p[0].equals("ROW")){rows++;int winner=Integer.parseInt(p[1]),troops=Integer.parseInt(p[3]),energy=Integer.parseInt(p[4]),seed=(int)Long.parseLong(p[5]);var state=PcDuelModelSaveTest.normalized(model,PcDuelKernel.hex(p[2]),seed,0,ids,natives);byte[]before=PcDuelModelSave.write(state);var plan=PcDuelSettlement.preview(state,new int[]{troops,troops},new int[]{Integer.parseInt(p[35]),Integer.parseInt(p[36])});check(plan.winner==winner&&Integer.toUnsignedLong(plan.seed)==Long.parseLong(p[6]),"original winner and RNG callback");int at=7;for(int i=0;i<6;i++){check(plan.health.get(ids[i])==Integer.parseInt(p[at++]),"original terminal health min1");check(plan.injury.get(ids[i])==Integer.parseInt(p[at++]),"original terminal injury");check(plan.merit[i]==Integer.parseInt(p[at++]),"original merit");check(plan.warXp[i]==Integer.parseInt(p[at++]),"original WAR XP");}for(int side=0;side<2;side++){int afterTroops=troops-(winner>=0&&side!=winner?plan.loserTroopLoss:0),afterEnergy=Math.max(0,Math.min(100,energy+plan.energy[side]));check(afterTroops==Integer.parseInt(p[at++]),"original troop loss with50-bound RNG");check(afterEnergy==Integer.parseInt(p[at++]),"original energy cap");}check(Arrays.equals(before,PcDuelModelSave.write(state)),"preview retains full model manager RNG bytes");check(plan.draws==(winner<0?0:1),"original exact numeric draws");
                PcDuelKernel.writeManager(state.manager,0x64,1);boolean rejected=false;try{PcDuelSettlement.preview(state,new int[]{troops,troops},new int[]{Integer.parseInt(p[35]),Integer.parseInt(p[36])});}catch(IOException e){rejected=true;}check(rejected,"unclosed casualty never relabeled as numeric-only victory");}
        }
        check(rows==108,"all original numeric boundary cases");check(Arrays.equals(world,SaveCodec.encode(w)),"source World and both RNG unchanged");System.out.println("PASS original terminal numeric plans "+checks+" checks / "+rows+" original callbacks; capture/death/ordinary campaign remain incomplete");
    }
}
