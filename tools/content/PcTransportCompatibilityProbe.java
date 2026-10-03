package game.sanguo.core;
import java.util.*;
import java.security.*;
/** Full current-worktree behavior comparison, not an old HEAD baseline. */
public final class PcTransportCompatibilityProbe {
 static String hash(World w)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(SaveCodec.encode(w)));}
 public static void main(String[] args)throws Exception{
  for(int n=0;n<24;n++){
   World w=LogisticsCampaignTest.fixture();w.strategy.setSeed(27016);w.city(11).ships[0]=3;w.city(11).ships[1]=2;
   int[] crew=n==0?null:n==1?new int[]{4}:n==2?new int[]{999}:new int[]{5,6};
   int[] gear=n==3?null:n==4?new int[3]:n==5?new int[]{-1,0,0,0}:new int[]{1000,0,0,0};
   int[] ships=n==6?null:n==7?new int[]{-1,0}:n==8?new int[]{4,0}:new int[]{2,1};
   int gold=n==9?-1:n==10?100001:700,food=n==11?200001:n==12?0:10000,troops=n==13?20001:2000;
   int target=n==14?11:n==15?999:12,officer=n==16?999:4;
   if(n==17)w.actionPoints[w.active]=9;if(n==18)w.city(11).gold=0;
   if(n==19)for(int q=9;q<=11;q++)for(int r=0;r<w.height;r++)w.terrain[q][r]=World.Terrain.WATER;
   boolean sea=n>=20,returning=n%2==0;
   if(args.length>0&&args[0].equals("canonical"))w=SaveCodec.decode(SaveCodec.encode(w));
   if(args.length>0&&args[0].equals("without-ships"))ships=new int[2];
   World.Result result=w.domestic.transport(11,target,officer,crew,gold,food,troops,gear,sea,returning,ships);
   System.out.println(n+"|"+result.ok+"|"+result.message+"|"+hash(w));
   if(result.ok)for(int turn=0;turn<6;turn++){World restored=SaveCodec.decode(SaveCodec.encode(w));if(!w.nextTurn().ok||!restored.nextTurn().ok||!hash(w).equals(hash(restored)))throw new AssertionError("turn replay");System.out.println(n+":"+turn+"|"+hash(w));}
  }
 }
}
