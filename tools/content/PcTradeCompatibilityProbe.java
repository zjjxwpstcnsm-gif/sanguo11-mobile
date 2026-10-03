package game.sanguo.core;
import java.util.*;
import java.security.*;
/** Run unchanged against a complete frozen baseline and candidate, compare every output byte. */
public final class PcTradeCompatibilityProbe {
 static World fixture(){
  World w=new World(24,20);w.cities.add(new World.City(10,"A",new Hex(4,4),0));w.cities.add(new World.City(20,"B",new Hex(18,14),1));
  for(int i=0;i<4;i++)w.officers.add(new World.Officer(i,"O"+i,0,10,80,80,80,90,80));w.officers.add(new World.Officer(20,"E",1,20,80,80,80,80,80));w.strategy.initializeOffices();w.city(10).gold=30000;w.city(10).food=100000;w.strategy.setSeed(20261003L);return w;
 }
 static String hash(World w)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(SaveCodec.encode(w)));}
 static void sample(boolean buy,int food,int mode,int month)throws Exception{
  World w=fixture();w.startMonth=month;int actor=1,city=10;
  if(mode==1)w.actionPoints[0]=9;if(mode==2)w.city(10).gold=99;if(mode==3)w.officer(1).acted=true;if(mode==4)actor=999;if(mode==5)city=999;
  if(mode==6)w.campaign.traded.put(10,19000);if(mode==7)w.city(10).food=999;if(mode==8){w.city(10).food=w.campaign.foodCap(w.city(10))-1000;w.city(10).gold=w.campaign.goldCap(w.city(10))-100;}
  World.Result result=w.campaign.trade(city,actor,buy,food);String key=buy+":"+food+":"+mode+":"+month;
  System.out.println(key+"|"+result.ok+"|"+result.message+"|"+hash(w)+"|"+w.strategy.getRandomState());
  if(result.ok){World restored=SaveCodec.decode(SaveCodec.encode(w));if(!w.nextTurn().ok||!restored.nextTurn().ok||!hash(w).equals(hash(restored)))throw new AssertionError("continuation "+key);System.out.println(key+":turn|"+hash(w)+"|"+w.strategy.getRandomState());}
 }
 public static void main(String[] args)throws Exception{
  for(boolean buy:new boolean[]{true,false})for(int food:new int[]{Integer.MIN_VALUE,-1000,0,999,1000,1999,2000,19000,20000,21000,Integer.MAX_VALUE})for(int mode=0;mode<9;mode++)sample(buy,food,mode,1);
  for(int month=1;month<=12;month++)for(boolean buy:new boolean[]{true,false})sample(buy,5000,0,month);
 }
}
