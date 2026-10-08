package game.sanguo.core;
import java.nio.file.*;import java.util.*;
/** Exact full original human-mask15 selector roster and declared input. */
public final class PcRulerHumanSelectionTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static int n(Map<String,Object>m,String k){return ((Number)m.get(k)).intValue();}
 public static void main(String[]args)throws Exception {
  var r=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[0]))));check(Boolean.TRUE.equals(r.get("queryWholeWorldAndRngPure"))&&Boolean.TRUE.equals(r.get("sourceWorldAndRngRestored")),"actual original human query pure/restored");var row=MapJson.object(MapJson.array(r.get("rows")).get(0));Set<Integer>nativeCandidates=new HashSet<>();for(Object value:MapJson.array(row.get("candidates")))nativeCandidates.add(((Number)value).intValue());
  World w=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,28,23,new PcDuelOptions(0,0,0));Map<Integer,Integer>ids=new HashMap<>();for(var f:PcDuelSourceFacts.saved(w).values())ids.put(f.nativeId,f.officerId);byte[]before=SaveCodec.encode(w);var candidates=PcRulerSuccession.preview(w,ids.get(403));Set<Integer>actual=new HashSet<>();for(int id:candidates.candidates)actual.add(PcDuelSourceFacts.saved(w).get(id).nativeId);check(actual.equals(nativeCandidates),"entire original human roster");check(candidates.selected==ids.get(435),"AI default remains independent of human input");var chosen=PcRulerCoronation.validateExecution(w,ids.get(403),ids.get(n(r,"selectedNative")));check(chosen.heirNative==440&&chosen.heirArmy==6&&chosen.primaryArmy==5,"actual human selected district440/current merge");check(Arrays.equals(before,SaveCodec.encode(w)),"heir preview fullWorld/RNG pure");
  World cold=SaveCodec.decode(before);check(PcRulerCoronation.validateExecution(cold,ids.get(403),ids.get(440)).heirNative==440,"cold human selection input remains valid");boolean rejected=false;try{PcRulerCoronation.validateExecution(w,ids.get(403),ids.get(365));}catch(java.io.IOException e){rejected=true;}check(rejected&&Arrays.equals(before,SaveCodec.encode(w)),"foreign heir rejected pure");rejected=false;try{PcRulerCoronation.validateExecution(w,ids.get(403));}catch(java.io.IOException e){rejected=true;}check(rejected&&Arrays.equals(before,SaveCodec.encode(w)),"multiple human candidates require explicit input");
  System.out.println("PASS original human successor roster/input "+checks+" checks; normal GUI/APK pending");
 }
}
