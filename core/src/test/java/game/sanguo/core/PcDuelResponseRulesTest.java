package game.sanguo.core;
import java.nio.file.*;import java.util.*;
/** Original independent source getter facts and full RNG after each estimate. */
public final class PcDuelResponseRulesTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception{
        Map<Integer,PcDuelKernel.Actor>people=new HashMap<>();PcDuelKernel.OriginalSettings settings=null;int rows=0;
        for(String line:Files.readAllLines(Path.of(args[0]))){if(line.startsWith("#"))continue;String[]p=line.split("\t");
            if(p[0].equals("SETTINGS"))settings=new PcDuelKernel.OriginalSettings(p[1].equals("1"),Integer.parseInt(p[3]),p[1].equals("1"),Integer.parseInt(p[2]));
            if(p[0].equals("P")){var actor=new PcDuelKernel.Actor(Integer.parseInt(p[2]),0,Integer.parseInt(p[3]),0,true,false);actor.raw489080=Integer.parseInt(p[4]);actor.originalAge=Integer.parseInt(p[5]);actor.virtual48Value=p[6].equals("1");people.put(Integer.parseInt(p[1]),actor);}
            if(p[0].equals("R")||p[0].equals("M")){int seed=(int)Long.parseLong(p[5]);var random=new PcDuelKernel.Random(seed);var result=PcDuelResponseRules.opening(people.get(Integer.parseInt(p[1])),people.get(Integer.parseInt(p[2])),p[3].equals("1"),p[4].equals("1"),random,settings);int expected=(int)Long.parseLong(p[6]);check(result.side==expected,"original response side "+line);if(p[0].equals("R"))check(result.chance==Integer.parseInt(p[7]),"original output chance "+line);else{var model=new PcDuelKernel(new byte[0x59c]);model.set(0x24+0xc4,0);model.set(0x110+0xc4,0);model.set(model.fighter(0,0)+28,p[3].equals("1")?128:0);model.set(model.fighter(1,0)+28,p[4].equals("1")?128:0);var modelRng=new PcDuelKernel.Random(seed);var actors=new PcDuelKernel.Actor[2][3];actors[0][0]=people.get(Integer.parseInt(p[1]));actors[1][0]=people.get(Integer.parseInt(p[2]));byte[]before=model.state.clone();check(model.openingSide(actors,modelRng,settings)==expected&&Integer.toUnsignedLong(modelRng.state)==Long.parseLong(p[8]),"original model entry gear ordering/full RNG "+line);check(Arrays.equals(before,model.state),"model opening getter is pure");}check(Integer.toUnsignedLong(random.state)==Long.parseLong(p[8]),"original full RNG "+line);rows++;}
        }
        check(rows==2028,"full argument matrix");System.out.println("PASS original response opening "+checks+" checks; ordinary admission/fee/APK pending");
    }
}
