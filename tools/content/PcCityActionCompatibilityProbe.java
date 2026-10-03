package game.sanguo.core;
import java.util.*;
import java.security.*;
public final class PcCityActionCompatibilityProbe {
 static World fixture(){
  World w=new World(24,20);w.cities.add(new World.City(10,"A",new Hex(4,4),0));w.cities.add(new World.City(20,"B",new Hex(18,14),1));
  for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"O"+i,0,10,80,80,80,90,80));w.officers.add(new World.Officer(20,"E",1,20,80,80,80,80,80));w.strategy.initializeOffices();w.city(10).order=70;w.city(10).morale=40;w.city(10).gold=30000;
  var h=w.domestic.buildSites(10).get(0);w.domestic.facilities.add(new Domestic.Facility(w.domestic.nextFacilityId++,10,Domestic.Kind.BARRACKS,h,-1,0));w.strategy.setSeed(20261003L);return w;
 }
 static String hash(World w)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(SaveCodec.encode(w)));}
 static World.Result execute(World w,int op,int actor,int[] targets){return switch(op){case 0->w.strategy.patrol(10,actor);case 1->w.strategy.trainArmy(10,actor);case 2->w.strategy.recruitSoldiers(10,actor);case 3->w.strategy.search(10,actor);case 4->w.strategy.rewardOfficers(10,actor,targets);default->w.strategy.appointGovernor(10,actor,targets[0]);};}
 public static void main(String[] args)throws Exception{
  for(int op=0;op<6;op++)for(int v=0;v<12;v++){
   World w=fixture();int actor=1;int[] targets={2,3};
   if(v==1)w.actionPoints[0]=9;if(v==2)w.city(10).gold=0;if(v==3)w.officer(actor).acted=true;if(v==4)actor=999;
   if(v==5){w.city(10).order=100;w.city(10).morale=100;}if(v==6)w.city(10).recruitReserve=0;if(v==7)w.city(10).order=29;
   if(v==8){w.domestic.facilities.get(0).remaining=2;w.domestic.facilities.get(0).builderId=3;}if(v==9)targets=new int[]{2,2};if(v==10)targets=new int[]{0};
   if(v==11)w.strategy.addHiddenTalent(new Strategy.Talent(100,"Hidden",10,70,70,70,70,70,0));
   World.Result r=execute(w,op,actor,targets);System.out.println(op+":"+v+"|"+r.ok+"|"+r.message+"|"+hash(w)+"|"+w.strategy.getRandomState());
   if(r.ok)for(int n=0;n<2;n++){World loaded=SaveCodec.decode(SaveCodec.encode(w));if(!w.nextTurn().ok||!loaded.nextTurn().ok||!hash(w).equals(hash(loaded)))throw new AssertionError("save replay "+op+":"+v);System.out.println(op+":"+v+":"+n+"|"+hash(w));}
  }
 }
}
