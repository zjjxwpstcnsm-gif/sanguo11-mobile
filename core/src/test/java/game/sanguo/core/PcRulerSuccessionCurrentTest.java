package game.sanguo.core;
import java.nio.file.*;import java.util.*;
/** Current saved authority against actual full original roster/comparator. */
public final class PcRulerSuccessionCurrentTest {
 static int checks;static void check(boolean ok,String s){checks++;if(!ok)throw new AssertionError(s);}
 static int n(Map<String,Object>m,String k){return ((Number)m.get(k)).intValue();}
 public static void main(String[]args)throws Exception {
  World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23,new PcDuelOptions(0,0,0));byte[]before=SaveCodec.encode(w);Map<Integer,Integer>stable=new HashMap<>();for(var f:PcDuelSourceFacts.saved(w).values())stable.put(f.nativeId,f.officerId);
  var receipt=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[0]))));int forces=0;
  for(Object rv:MapJson.array(receipt.get("rows"))){var row=MapJson.object(rv);int ruler=stable.get(n(row,"rulerNative"));var plan=PcRulerSuccession.preview(w,ruler);List<Integer>expected=new ArrayList<>();
   for(Object pv:MapJson.array(row.get("candidates"))){var p=MapJson.object(pv);int id=stable.get(n(p,"nativeId"));expected.add(id);var f=plan.facts.get(id);check(f!=null,"original candidate "+n(p,"nativeId"));check(f.age==n(p,"age")&&f.merit==n(p,"merit")&&f.preference==(n(p,"preference")&255),"current original scalar facts "+n(p,"nativeId"));var flags=MapJson.array(p.get("flags"));for(int i=0;i<9;i++)check(f.flags[i]==Boolean.TRUE.equals(flags.get(i)),"current original predicate "+n(p,"nativeId")+" / "+i);}
   check(new HashSet<>(plan.candidates).equals(new HashSet<>(expected)),"whole current original roster");check(plan.selected==stable.get(n(row,"selectedNative")),"current full original selected");forces++;
  }
  check(forces==8&&Arrays.equals(before,SaveCodec.encode(w)),"all8 fullWorld/allRNG pure");World cold=SaveCodec.decode(before);for(var o:cold.officers)if(o.owner>=0&&o.role==Strategy.Role.RULER)check(PcRulerSuccession.preview(cold,o.id).selected==PcRulerSuccession.preview(w,o.id).selected,"cold current authority");
  // A changed candidate is selected from the current roster, never from the
  // previous original sample's cached winner. Restore the source world after.
  int ruler=stable.get(517),selected=PcRulerSuccession.preview(w,ruler).selected;var absent=w.officer(selected);var jail=w.cities.stream().filter(c->c.owner==2).findFirst().orElseThrow();w.government.capture(absent,jail);var captured=SaveCodec.encode(w);var changed=PcRulerSuccession.preview(w,ruler);check(!changed.candidates.contains(selected)&&changed.selected!=selected,"actual capture excludes current mask5 from original roster");check(Arrays.equals(captured,SaveCodec.encode(w)),"changed roster query allWorld pure");
  System.out.println("PASS current original succession "+checks+" roster/predicate/scalar/cold/current-owner checks; production death/UI/APK pending");
 }
}
