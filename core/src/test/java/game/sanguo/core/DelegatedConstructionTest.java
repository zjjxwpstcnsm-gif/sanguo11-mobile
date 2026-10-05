package game.sanguo.core;
import java.util.*;
/** Delegation obeys the same native base cost/AP as the ordinary player command. */
public final class DelegatedConstructionTest {
 static int checks;
 static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 public static void main(String[] args)throws Exception{
  for(boolean farm:new boolean[]{false,true})for(int gold:new int[]{199,200,299,1499,1500})for(int ap:new int[]{0,10,19,20,60}){
   World w=GovernmentTest.world();w.strategy.initializeOffices();
   var city=w.city(0);city.gold=gold;city.food=farm?10000:200000;city.order=100;city.morale=w.campaign.energyCap(0);city.defense=2000;
   // One available governor removes tie-breaking and unrelated fallback work.
   for(var o:w.officers)if(o.cityId==0)o.acted=o.id!=0&&o.id!=1;w.actionPoints[0]=ap+10;
   check(w.government.delegate(0,0,Government.Policy.ECONOMY).ok,"normal policy command establishes report baseline after fixture setup");
   World direct=SaveCodec.decode(SaveCodec.encode(w));var kind=farm?Domestic.Kind.FARM:Domestic.Kind.MARKET;
   var site=direct.domestic.buildSites(0).get(0);byte[] before=SaveCodec.encode(w);w.government.runDelegated();
   if(gold>=200&&ap>=20){
    check(direct.domestic.build(0,1,kind,site).ok,"ordinary build succeeds at 200 gold/20 AP");
    check(w.domestic.facilities.size()==1&&w.domestic.facilities.get(0).kind==kind&&w.domestic.facilities.get(0).level==1,"delegation really starts requested base facility");
    check(w.city(0).gold==gold-200&&w.actionPoints[0]==ap-20&&w.officer(1).acted,"exact native construction debit and officer use");
    check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(direct)),"delegation/normal command full-save and RNG equality");
    for(int n=0;n<3;n++){var restored=SaveCodec.decode(SaveCodec.encode(w));check(w.nextTurn().ok&&restored.nextTurn().ok,"complete delegated construction turn");check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"save continuation exact");}
   }else{
    check(w.domestic.facilities.isEmpty(),"cannot build without actual gold/AP");
    check(w.city(0).gold>=0&&w.actionPoints[0]>=0,"fallback never overspends");
    if(ap<10)check(Arrays.equals(before,SaveCodec.encode(w)),"no legal AP means no state/RNG change");
   }
  }
  System.out.println("PASS DelegatedConstructionTest checks="+checks);
 }
}
