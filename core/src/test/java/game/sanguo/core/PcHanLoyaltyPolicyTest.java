package game.sanguo.core;
import java.nio.file.*;import java.util.*;
/** Exact original enum matrix and actual historical39 strategy preservation. */
public final class PcHanLoyaltyPolicyTest {
 static int checks;static void check(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
 static int n(Map<String,Object>r,String k){return ((Number)r.get(k)).intValue();}
 public static void main(String[]args)throws Exception {
  World fresh=PcScenarioCatalog.load(PcScenarioCatalog.all().get(0).identity.scenarioId,2,23);check(PcDebateCampaignPolicy.read(fresh).version==2,"explicit fresh PDC2");byte[]before=SaveCodec.encode(fresh);var receipt=MapJson.object(MapJson.parse(Files.readAllBytes(Path.of(args[0]))));int cases=0;
  for(Object row:MapJson.array(receipt.get("rows"))){var r=MapJson.object(row);int a=n(r,"targetProperty49"),b=n(r,"rulerProperty49");boolean same=PcDebateCampaignPolicy.sameHan(fresh,a,b);int result=PcOfficerJoinRules.loyalty(80,50,0,0,0,false,false,false,false,false,false,false,same,false);check(result==n(r,"originalRawLoyalty"),"full original Han enum case "+a+"/"+b);cases++;}
  check(cases==9&&Arrays.equals(before,SaveCodec.encode(fresh)),"enum preview fullWorld/allRNG pure");World cold=SaveCodec.decode(before);check(PcDebateCampaignPolicy.read(cold).version==2&&Arrays.equals(before,SaveCodec.encode(cold)),"new policy exact cold");
  byte[]fixture=Files.readAllBytes(Path.of("docs/handoff/20261004/session1/batch19-actual-art-mid.sg11"));World old=SaveCodec.decode(fixture);byte[]oldBefore=SaveCodec.encode(old);check(!PcDebateCampaignPolicy.enabled(old),"actual39 no silent PDC adoption");check(!PcDebateCampaignPolicy.sameHan(old,0,0)&&PcDebateCampaignPolicy.sameHan(old,1,1)&&!PcDebateCampaignPolicy.sameHan(old,2,2)&&Arrays.equals(oldBefore,SaveCodec.encode(old)),"old absent strategy remains unchanged/pure");
  PcDebateCampaignPolicy.initialize(old);check(PcDebateCampaignPolicy.read(old).version==1,"explicit prototype39 adoption retains PDC1");byte[]adopted=SaveCodec.encode(old);World resumed=SaveCodec.decode(adopted);check(PcDebateCampaignPolicy.read(resumed).version==1&&Arrays.equals(adopted,SaveCodec.encode(resumed)),"old adopted strategy cold byte exact");check(!PcDebateCampaignPolicy.sameHan(resumed,0,0)&&PcDebateCampaignPolicy.sameHan(resumed,1,1)&&!PcDebateCampaignPolicy.sameHan(resumed,2,2)&&Arrays.equals(adopted,SaveCodec.encode(resumed)),"saved PDC1 never upgraded during new query");check(Arrays.equals(fixture,Files.readAllBytes(Path.of("docs/handoff/20261004/session1/batch19-actual-art-mid.sg11"))),"actual original fixture untouched");
  System.out.println("PASS Han loyalty PDC2 original9 cases/PDC1 genuine39 adoption+save "+checks+" checks; actual normal command/APK separate");
 }
}
