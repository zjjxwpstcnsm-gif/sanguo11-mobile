package game.sanguo.core;
import java.util.*;
/** Real transport dispatch, pure draft, full turn/save replay; synthetic map. */
public final class TransportPlanTest {
 static int checks;
 static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  for(int n=0;n<28;n++){
   World w=LogisticsCampaignTest.fixture();w.city(11).ships[0]=3;w.city(11).ships[1]=2;
   int[] crew=n==0?null:n==1?new int[]{4}:n==2?new int[]{999}:new int[]{5,6};
   int[] gear=n==3?null:n==4?new int[3]:n==5?new int[]{-1,0,0,0}:new int[]{1000,0,0,0};
   int[] ships=n==6?null:n==7?new int[]{-1,0}:n==8?new int[]{4,0}:new int[]{2,1};
   int gold=n==9?-1:n==10?100001:700,food=n==11?200001:n==12?0:10000,troops=n==13?20001:2000;
   int target=n==14?11:n==15?999:12,officer=n==16?999:4;
   if(n==17)w.actionPoints[w.active]=9;if(n==18)w.city(11).gold=0;
   if(n==19)for(int q=9;q<=11;q++)for(int r=0;r<w.height;r++)w.terrain[q][r]=World.Terrain.WATER;
   if(n==24)w.city(12).gold=w.campaign.goldCap(w.city(12));
   if(n==25){gold=food=troops=0;gear=new int[4];ships=new int[2];}
   if(n==26)crew=new int[]{5,6,7};if(n==27)w.domestic.nextMissionId=10000000;
   boolean sea=n>=20,returning=n%2==0;
   // Canonical loaded-save baseline; raw synthetic fixture replay divergence
   // is separately retained by PcTransportCompatibilityProbe without this mode.
   w=SaveCodec.decode(SaveCodec.encode(w));byte[] before=SaveCodec.encode(w);
   TransportPlan p=w.domestic.previewTransport(11,target,officer,crew,gold,food,troops,gear,sea,returning,ships);
   check(Arrays.equals(before,SaveCodec.encode(w)),"entire preview pure including IDs/RNG "+n);
   check(p.stocks.size()==14,"all gold/food/troops/9 equipment/2 ships");
   try{p.stocks.clear();throw new AssertionError("mutable stocks");}catch(UnsupportedOperationException expected){checks++;}
   World.Result result=w.domestic.transport(11,target,officer,crew,gold,food,troops,gear,sea,returning,ships);
   check(p.allowed()==result.ok,"ordinary command agrees "+n);
   if(!result.ok){check(p.failure.detail.equals(result.message)&&p.forecast==null,"exact failure and no fictional route "+n);check(Arrays.equals(before,SaveCodec.encode(w)),"rejection atomic "+n);continue;}
   Domestic.Mission m=w.domestic.missions.get(0);
   check(p.forecast!=null&&p.forecast.foodPerTurn==w.domestic.foodUse(m),"real ration from actual mission");
   check(m.hex.equals(new Hex(p.forecast.departureQ,p.forecast.departureR))&&w.orders.remaining(m)==p.forecast.movementRemaining,"actual departure and movement budget");
   check(w.actionPoints[w.active]==60-p.actionPointsCost&&w.city(11).ships[0]==1&&w.city(11).ships[1]==1,"one actual debit");
   if(n==24)check(!p.forecast.capacityFits,"capacity shortage waits instead of refusing dispatch");
   if(n==12)check(p.forecast.shortage,"zero grain forecast warns existing starvation rule");
   byte[] dispatched=SaveCodec.encode(w);check(!w.domestic.transport(11,target,officer,crew,gold,food,troops,gear,sea,returning,ships).ok&&Arrays.equals(dispatched,SaveCodec.encode(w)),"repeat cannot dispatch/debit twice");
   for(int turn=0;turn<6;turn++){
    World restored=SaveCodec.decode(SaveCodec.encode(w));check(w.nextTurn().ok&&restored.nextTurn().ok,"complete normal turn");check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"full save and RNG replay");
   }
  }
  try(var input=TransportPlanTest.class.getResourceAsStream("/pre-atomic-ship-cargo-v33.sg11")){
   check(input!=null,"genuine old ship-cargo save exists");byte[] raw=input.readAllBytes();World old=SaveCodec.decode(raw);
   check(Arrays.equals(raw,SaveCodec.encode(old)),"old ship-cargo save and historical report remain byte identical");
   Domestic.Mission mission=old.domestic.missions.get(0);
   check(old.city(11).ships[0]==1&&old.city(11).ships[1]==1&&Arrays.equals(mission.cargoShips,new int[]{2,1}),"old ship inventory is not recharged or duplicated");
   World replay=SaveCodec.decode(raw);
   for(int n=0;n<6;n++){check(old.nextTurn().ok&&replay.nextTurn().ok,"old cargo full turn");check(Arrays.equals(SaveCodec.encode(old),SaveCodec.encode(replay)),"old cargo continuation save/RNG equality");replay=SaveCodec.decode(SaveCodec.encode(replay));}
  }
  System.out.println("PASS TransportPlanTest checks="+checks);
 }
}
