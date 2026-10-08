package game.sanguo.core;
import java.nio.file.*;
import java.util.*;
/** Original declared source40 field-mode forced gates; current campaign binding separate. */
public final class PcDuelRecruitmentGateTest {
 public static void main(String[]args)throws Exception {
  var receipt=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[0]))));int count=0;
  for(Object row:MapJson.array(receipt.get("rows"))){var r=MapJson.object(row);var g=new PcDuelRecruitmentRules.Gate();g.mode=((Number)r.get("mode")).intValue();g.ruler=((Number)r.get("status")).intValue()==0;g.targetNative=558;g.actorNative=g.rulerNative=365;g.targetForce=3;g.rulerForce=2;g.oldRuler=517;String relation=(String)r.get("relation");
   g.refusedRuler=relation.equals("refuses-ruler")?365:-1;g.swornValid=relation.startsWith("sworn");g.swornNative=relation.equals("sworn-actor")?365:relation.equals("sworn-old")?517:-1;g.swornForce=relation.equals("sworn-actor")?2:relation.equals("sworn-old")?3:-1;g.swornProperty75Old=relation.equals("sworn-old");g.swornProperty75New=relation.equals("sworn-actor");g.spouseValid=relation.startsWith("spouse");g.spouseNative=relation.equals("spouse-actor")?365:relation.equals("spouse-old")?517:-1;g.spouseForce=relation.equals("spouse-actor")?2:relation.equals("spouse-old")?3:-1;g.dislikesRuler=g.dislikesActor=relation.startsWith("disliked");g.likesOldRuler=relation.equals("liked-old");g.likesNewRuler=relation.equals("liked-new");
   int expected=Boolean.TRUE.equals(r.get("handled"))?((Number)r.get("decision")).intValue():-1;int actual=PcDuelRecruitmentRules.forced(g);if(actual!=expected)throw new AssertionError("Original forced gate "+g.mode+"/"+g.ruler+"/"+relation+" expected"+expected+" actual"+actual);count++;
  }
  if(count!=40)throw new AssertionError("Original gate domain incomplete");System.out.println("PASS original field forced gate40; current metadata/property75/ordinary entry binding remains required");
 }
}
