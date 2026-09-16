package game.sanguo.core;
public final class TacticalLogisticsProbe {
 public static void main(String[] args) throws Exception {
  World w=LogisticsCampaignTest.fixture();
  w.domestic.transport(11,12,4,new int[]{5,6},700,5000,1000,new int[4],false,true);
  w.turn++;w.domestic.tick();Domestic.Mission m=w.domestic.missions.get(0);
  if(args.length>0&&args[0].equals("fixture")){System.out.println(java.util.Base64.getEncoder().encodeToString(SaveCodec.encode(w)));return;}
  System.out.println("position="+m.hex+" food="+m.food+" spent="+m.consumedFood+" battlefieldUnit="+(w.unitAt(m.hex)!=null));
  if(w.unitAt(m.hex)==null)throw new AssertionError("transport has no real battlefield identity / target");
 }
}
