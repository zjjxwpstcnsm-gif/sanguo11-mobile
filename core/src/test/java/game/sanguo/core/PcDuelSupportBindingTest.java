package game.sanguo.core;
import java.nio.file.*;
import java.util.*;

/** Original source getter values, never model-output answers as inputs. */
public final class PcDuelSupportBindingTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception {
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);var runtime=PcDuelRuntimeFacts.saved(w);Map<Integer,Integer>ids=new HashMap<>();for(var f:PcDuelSourceFacts.saved(w).values())ids.put(f.nativeId,f.officerId);int[]natives={116,163,195,222,658,590};byte[]before=SaveCodec.encode(w);
        String start=Files.readAllLines(Path.of(args[0])).stream().filter(l->l.startsWith("START\t")).findFirst().orElseThrow();String[]p=start.split("\t");
        for(String row:p[8].split(",")){String[]v=row.split(":");int side=Integer.parseInt(v[0]),own=Integer.parseInt(v[1]),other=Integer.parseInt(v[2]),slot=Integer.parseInt(v[3]);int candidateNative=natives[side*3+slot];var f=PcDuelBindings.support(w,ids.get(candidateNative),ids.get(natives[side*3+own]),ids.get(natives[(1-side)*3+other]),runtime,runtime.people.get(candidateNative).rawLoyalty);int[]actual={f.valid?1:0,f.threshold,f.p4887d0?1:0,f.p488790?1:0,f.p4889e0Other?1:0,f.p4889e0Own?1:0,f.p488910?1:0,f.p48bb70?1:0,f.ownStatusZero?1:0,f.ownOwnerValid?1:0,f.sameOwner?1:0,f.rawLoyalty,f.rawE4Equal?1:0,f.gap};for(int i=0;i<14;i++)check(actual[i]==Integer.parseInt(v[i+4]),"source support getter side="+side+" own="+own+" other="+other+" slot="+slot+" field="+i+" got="+actual[i]+" expected="+v[i+4]);}
        check(Arrays.equals(before,SaveCodec.encode(w)),"wholeWorld/allRNG binding pure");System.out.println("PASS source support bindings "+checks+" checks; edited/current raw strategies and full ordinary duel pending");
    }
}
