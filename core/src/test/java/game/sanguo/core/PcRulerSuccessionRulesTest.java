package game.sanguo.core;
import java.nio.file.*;import java.util.*;
/** Full original roster/pairwise callback/RNG receipt, no answer table in rules. */
public final class PcRulerSuccessionRulesTest {
 public static void main(String[]args)throws Exception{
  var receipt=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[0]))));int count=0,forces=0;
  for(Object value:MapJson.array(receipt.get("rows"))){var row=MapJson.object(value);Map<Integer,PcRulerSuccessionRules.Person>people=new LinkedHashMap<>();
   for(Object cv:MapJson.array(row.get("candidates"))){var c=MapJson.object(cv);var values=MapJson.array(c.get("flags"));boolean[]flags=new boolean[9];for(int i=0;i<9;i++)flags[i]=Boolean.TRUE.equals(values.get(i));int id=n(c,"nativeId"),preference=n(c,"preference")&255;if(PcRulerSuccessionRules.preference(id)!=preference)throw new AssertionError("original preference "+id);people.put(id,new PcRulerSuccessionRules.Person(id,n(c,"age"),n(c,"merit"),preference,flags));}
   var refs=MapJson.array(row.get("referenceValid"));boolean fv=Boolean.TRUE.equals(refs.get(0)),rv=Boolean.TRUE.equals(refs.get(1)),mv=Boolean.TRUE.equals(refs.get(2));
   for(Object pv:MapJson.array(row.get("comparisons"))){var p=MapJson.object(pv);boolean actual=PcRulerSuccessionRules.better(people.get(n(p,"first")),people.get(n(p,"second")),fv,rv,mv);if(actual!=Boolean.TRUE.equals(p.get("originalBetter")))throw new AssertionError("original pair differs force="+n(row,"forceNative")+" first="+n(p,"first")+" second="+n(p,"second"));count++;}
   PcRulerSuccessionRules.Person best=null;for(var candidate:people.values())if(best==null||PcRulerSuccessionRules.better(candidate,best,fv,rv,mv))best=candidate;
   if((best==null?-1:best.nativeId)!=n(row,"selectedNative"))throw new AssertionError("full original selected successor "+n(row,"forceNative"));forces++;
  }
  if(forces!=8||count<20)throw new AssertionError("original coverage incomplete");System.out.println("PASS original ruler comparator "+count+" ordered pairs/"+forces+" complete selections; current binding/settlement/APK pending");
 }
 static int n(Map<String,Object>m,String k){return ((Number)m.get(k)).intValue();}
}
