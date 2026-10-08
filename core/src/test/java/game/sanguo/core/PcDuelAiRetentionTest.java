package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Full source context/rank and home roster threshold, explicit prisoners. */
public final class PcDuelAiRetentionTest {
    static int checks;static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    static int n(Map<String,Object>r,String key){return ((Number)r.get(key)).intValue();}
    public static void main(String[]args)throws Exception {
        var receipt=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[0]))));for(Object item:MapJson.array(receipt.get("rankByte35"))){var rank=MapJson.object(item);check(PcOfficerRanks.all().get(n(rank,"nativeId")).salary==n(rank,"byte35"),"original rankbyte35 salary");}
        World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);var facts=PcDuelSourceFacts.saved(w);var joined=new HashMap<Integer,Integer>();for(var f:facts.values())joined.put(f.nativeId,f.officerId);int captor=joined.get(365),home=PcPersonnelReturnRules.projectSite(w,8);var homeCity=w.city(home);
        for(Object item:MapJson.array(receipt.get("rows"))){var row=MapJson.object(item);homeCity.troops=n(row,"troops");int count=n(row,"prisonerFixture");w.government.prisoners.clear();for(int i=0;i<count;i++){int nativeId=new int[]{222,235,348,449}[i];var p=new Government.Prisoner(joined.get(nativeId),2,home,w.turn);w.government.prisoners.put(p.officerId,p);}var plan=PcDuelAiDisposition.retention(w,captor);var normal=new ArrayList<Integer>();for(Object person:MapJson.array(row.get("normalRoster")))normal.add(n(MapJson.object(person),"nativeId"));Collections.sort(normal);check(plan.normalNatives.equals(normal),"original home roster includes deployed officers");check(plan.rankSalarySum==n(row,"normalRankByte35Sum"),"original home salary55");check(plan.captiveCount==n(row,"captiveMask32Count"),"original city current captive count");check(plan.retain()==(n(row,"decision")!=0),"original field retention threshold");}
        w.government.prisoners.clear();byte[]before=SaveCodec.encode(w);PcDuelAiDisposition.retention(w,captor);check(Arrays.equals(before,SaveCodec.encode(w)),"actual unmodified detention preview wholeWorld/bothRNG pure");System.out.println("PASS original current detention "+checks+" checks; prisoners are explicit counting fixtures, ordinary triggers/APK pending");
    }
}
