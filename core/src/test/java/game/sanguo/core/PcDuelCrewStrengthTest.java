package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Exact original current crew/health/nominee/dislike getter observations. */
public final class PcDuelCrewStrengthTest {
    public static void main(String[]args)throws Exception {
        int checks=0;
        for(String line:Files.readAllLines(Path.of(args[0]))){
            if(line.startsWith("#")||line.isEmpty())continue;
            String[]v=line.split("\t");int nominee=Integer.parseInt(v[0]),expected=Integer.parseInt(v[1]);
            List<PcDuelAdmissionRules.Candidate>crew=new ArrayList<>();Set<Integer>dislikes=new HashSet<>();
            for(String raw:v[2].split(";")){
                int[]p=Arrays.stream(raw.split(",")).mapToInt(Integer::parseInt).toArray();
                if(p.length!=7)throw new AssertionError("Original crew fixture width");
                crew.add(new PcDuelAdmissionRules.Candidate(p[0],p[0],p[1],p[2],p[3],p[4],p[5]==1));if(p[6]==1)dislikes.add(p[0]);
            }
            int actual=PcDuelAdmissionRules.crewStrength(crew,true,nominee,nominee>=0,(own,target)->dislikes.contains(own));
            if(actual!=expected)throw new AssertionError("Original crew strength differs: "+line+" got="+actual);checks++;
        }
        if(checks!=32)throw new AssertionError("Original source coverage changed");
        System.out.println("PASS original crew strength "+checks+" observations; ordinary human command and APK remain pending");
    }
}
