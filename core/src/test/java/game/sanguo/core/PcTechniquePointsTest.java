package game.sanguo.core;
import java.io.*;import java.nio.*;import java.nio.charset.StandardCharsets;import java.util.*;
public final class PcTechniquePointsTest {
 static int checks;static void check(boolean value,String m){checks++;if(!value)throw new AssertionError(m);}
 static byte[] bounded(InputStream in)throws IOException{
  if(in==null)throw new IOException("Missing genuine legacy fixture");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] block=new byte[8192];int length;
  while((length=in.read(block))!=-1){if(length==0)throw new IOException("Legacy stream made no progress");if(out.size()+length>32*1024*1024)throw new IOException("Legacy fixture exceeds real save limit");out.write(block,0,length);}
  byte[] raw=out.toByteArray();if(raw.length!=238756)throw new IOException("Pinned legacy fixture byte count changed");return raw;
 }
 static World normal(){World w=PcDelayedProductionFlowTest.fixture(true);w.pcTechniquePoints.initializeOpening();return w;}
 static void progress(String phase){Runtime r=Runtime.getRuntime();System.out.println("PROGRESS "+phase+" checks="+checks+" heapUsed="+(r.totalMemory()-r.freeMemory())+" heapMax="+r.maxMemory());}
 public static void main(String[] args)throws Exception{
  progress("source84");
  try(var in=PcTechniquePointsTest.class.getResourceAsStream("/pc-technique-gain-native.tsv");var reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){reader.readLine();int rows=0;for(String line;(line=reader.readLine())!=null;){String[] a=line.split("\t");int before=Integer.parseInt(a[0]),requested=Integer.parseInt(a[1]),after=Integer.parseInt(a[2]);check(PcTechniquePoints.after(before,requested)==after,"original signed/clamped source observation "+line);rows++;}check(rows==84,"all actual source samples");}
  progress("normal252");
  for(int workers=1;workers<=3;workers++)for(World.Weapon weapon:new World.Weapon[]{World.Weapon.SPEAR,World.Weapon.HALBERD,World.Weapon.CROSSBOW,World.Weapon.CAVALRY})for(int room:new int[]{1,299,300,599,600,2999,100000})for(int points:new int[]{0,9999,10000}){
   World w=normal();int[] ids=new int[workers];for(int n=0;n<workers;n++)ids[n]=n+1;w.city(10).equipment[weapon.ordinal()]=100000-room;w.campaign.points.put(0,points);byte[] before=SaveCodec.encode(w);check(ByteBuffer.wrap(before).getInt(4)==37,"explicit new profile schema37");long rng=w.strategy.getRandomState();var p=w.previewProduction(10,ids,ProductionPlan.Operation.EQUIPMENT,weapon,null);check(p.allowed(),"real admission "+weapon);check(p.effects.nativeTechniquePoints&&p.effects.techniquePointsBefore==points&&p.effects.techniquePointsAfter==PcTechniquePoints.after(points,Math.min(10,p.effects.outputQuantity/300+1)),"actual clipped credit preview TP");check(Arrays.equals(before,SaveCodec.encode(w)),"preview preserves entire state and RNG");World replay=SaveCodec.decode(before);int stock=w.city(10).equipment[weapon.ordinal()];check(w.produce(10,ids,weapon).ok&&replay.produce(10,ids,weapon).ok,"normal actual crew production");int credited=w.city(10).equipment[weapon.ordinal()]-stock;check(w.campaign.points(0)==p.effects.techniquePointsAfter&&credited==p.effects.outputQuantity,"one real quantity-bound point credit");check(w.strategy.getRandomState()==rng&&Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"save continuation exact");byte[] after=SaveCodec.encode(w);check(!w.produce(10,ids,weapon).ok&&Arrays.equals(after,SaveCodec.encode(w)),"repeated acted command adds no points");
  }
  progress("delayed18");
  for(int workers=1;workers<=3;workers++)for(int item:new int[]{5,6,7,8,10,11}){
   progress("delayed workers="+workers+" item="+item);
   World w=normal(),control=PcDelayedProductionFlowTest.fixture(true);w.campaign.points.put(0,250);control.campaign.points.put(0,250);int[] ids=new int[workers];for(int n=0;n<workers;n++)ids[n]=n+1;World.Weapon weapon=item==5?World.Weapon.RAM:item==6?World.Weapon.SIEGE_TOWER:item==7?World.Weapon.CATAPULT:item==8?World.Weapon.WOODEN_BEAST:null;Army.Ship ship=item==10?Army.Ship.TOWER_SHIP:item==11?Army.Ship.WARSHIP:null;
   check(w.army.produce(10,ids,weapon,ship).ok&&control.army.produce(10,ids,weapon,ship).ok,"real delayed start");check(w.campaign.points(0)==250,"delayed start grants no points");int turns=w.officer(ids[0]).otherTaskTurns;World replay=SaveCodec.decode(SaveCodec.encode(w));
   for(int turn=1;turn<=turns;turn++){w=SaveCodec.decode(SaveCodec.encode(w));check(w.nextTurn().ok&&control.nextTurn().ok&&replay.nextTurn().ok,"real saved delayed full turn");check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"entire native completion and committed reports replay exactly");check(w.campaign.points(0)-control.campaign.points(0)==(turn==turns?10:0),"source20 request credits10 only once on completion");check(w.strategy.getRandomState()==control.strategy.getRandomState(),"policy never uses rule RNG");}
   check(PcDelayedProductionFlowTest.stock(w,weapon,ship)==PcDelayedProductionFlowTest.stock(control,weapon,ship)&&w.officerAbilities.experience(ids[0],2)==control.officerAbilities.experience(ids[0],2)&&w.government.merit(ids[0])==control.government.merit(ids[0]),"source reward changes no legacy stock/XP/merit");

  }
  for(int loss:new int[]{0,1,2}){World w=normal();check(w.army.produce(10,1,World.Weapon.RAM,null).ok,"loss/cancel fixture");int before=w.campaign.points(0);if(loss==0)check(w.army.cancelProduction(1).ok,"normal cancellation");else{if(loss==1)w.city(10).owner=1;else w.domestic.facility(w.army.productions.get(0).facilityId).hp=0;w.army.cleanup();}check(w.army.productions.isEmpty()&&w.campaign.points(0)==before,"cancellation/loss grants no completion points");}
  progress("genuine-old36");
  boolean android="Dalvik".equals(System.getProperty("java.vm.name"));byte[] raw;try(var in=PcTechniquePointsTest.class.getResourceAsStream(android?"/legacy-production-v36/coalition-190-art.sg11":"/legacy-production-v36/coalition-190-host.sg11")){raw=bounded(in);}progress("old36 resource bytes="+raw.length);World legacy=SaveCodec.decode(raw);progress("old36 decoded");byte[] encoded=SaveCodec.encode(legacy);progress("old36 reencoded bytes="+encoded.length);check(!legacy.pcTechniquePoints.enabled()&&Arrays.equals(raw,encoded),"genuine v36 remains36 and byte exact");legacy.campaign.points.put(0,12000);byte[] high=SaveCodec.encode(legacy);check(ByteBuffer.wrap(high).getInt(4)==36&&SaveCodec.decode(high).campaign.points(0)==12000,"old excess points not truncated or opted in");
  progress("native-cap");
  World cap=normal();cap.campaign.points.put(0,9999);cap.campaign.earn(0,100);check(cap.campaign.points(0)==10000,"explicit new global native cap");
  System.out.println("PASS PcTechniquePointsTest checks="+checks+" native84, actual4weapon/1–3crew/clipped quantities, delayed6items/turns/save/cancel/loss, genuine old36 preservation");
 }
}
