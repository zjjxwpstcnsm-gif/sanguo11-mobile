package game.sanguo.core;
import java.util.*;

/** Normal source-verified fixed rewards, full forecasts, saves and failure paths. */
public final class CityCommandRewardsTest {
 static int checks;
 static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
 static World fixture(boolean managed,World.Weapon weapon){
  World w=weapon==null?CityActionPlanTest.fixture():ProductionPlanTest.fixture(weapon,null);
  if(managed)w.officerAbilities.initializeOpening(null,false,false);return w;
 }
 static World.Result command(World w,CityActionPlan.Operation operation,World.Weapon weapon){
  return weapon!=null?w.produce(10,1,weapon):w.strategy.executeCityAction(operation,10,1,new int[0]);
 }
 static void sample(CityActionPlan.Operation operation,World.Weapon weapon,int stat,boolean managed,int xp,int merit,int base)throws Exception{
  World w=fixture(managed,weapon);OfficerAbilities.setBase(w.officer(1),stat,base);
  if(managed)w.officerAbilities.gainExperience(1,stat,xp);w.government.earn(1,merit);
  int[] priorBase=new int[5],priorXp=new int[5],priorGrowth=new int[5];for(int n=0;n<5;n++){priorBase[n]=w.officerAbilities.base(1,n);priorXp[n]=w.officerAbilities.experience(1,n);priorGrowth[n]=w.officerAbilities.growthCode(1,n);}
  byte[] before=SaveCodec.encode(w);long rng=w.strategy.getRandomState();World replay=SaveCodec.decode(before);
  CityActionPlan city=weapon==null?w.strategy.previewCityAction(operation,10,1,new int[0]):null;
  ProductionPlan production=weapon!=null?w.previewProduction(10,1,ProductionPlan.Operation.EQUIPMENT,weapon,null):null;
  OfficerExperiencePlan e=city!=null?city.effects.officers.stream().filter(o->o.id==1).findFirst().get().experience:production.effects.experience;
  check(e.managed==managed&&e.stat==stat&&e.requestedAmount==2,"source fixed reward metadata");
  check(e.experienceBefore==(managed?xp:-1)&&e.experienceAfter==(managed?Math.min(3000,xp+2):-1),"XP forecast uses cap and explicit legacy mode");
  check(e.currentBefore==(managed?Math.min(100,base+xp/100):base)&&e.currentAfter==(managed?Math.min(100,base+Math.min(3000,xp+2)/100):base),"current forecast preserves base and legacy numbers");
  for(int n=0;n<3;n++)if(weapon!=null)w.previewProduction(10,1,ProductionPlan.Operation.EQUIPMENT,weapon,null);else w.strategy.previewCityAction(operation,10,1,new int[0]);
  check(Arrays.equals(before,SaveCodec.encode(w)),"repeated forecast is full-save/RNG pure");
  check(command(w,operation,weapon).ok&&command(replay,operation,weapon).ok,"normal command commits");
  check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"normal command full save/report/RNG matches restored execution");
  check(w.strategy.getRandomState()==rng,"fixed reward consumes no RNG");
  check(OfficerAbilities.raw(w.officer(1),stat)==e.currentAfter&&w.officerAbilities.experience(1,stat)==(managed?e.experienceAfter:0),"actual current/XP match preview");
  for(int n=0;n<5;n++)check(w.officerAbilities.base(1,n)==priorBase[n]&&w.officerAbilities.growthCode(1,n)==priorGrowth[n]&&w.officerAbilities.experience(1,n)==(managed&&n==stat?Math.min(3000,xp+2):priorXp[n]),"base/growth and other XP remain intact");
  int expectedMerit=managed?merit+PcMerchantRules.meritGain(merit):merit+100;
  check(w.government.merit(1)==expectedMerit&&w.actionPoints[0]==40&&w.officer(1).acted,"native merit50/cap or legacy100 and one20AP/action");
  if(city!=null){check(w.city(10).order==city.effects.orderAfter&&w.city(10).morale==city.effects.moraleAfter&&w.city(10).troops==city.effects.troopsAfter,"city effect uses authoritative pre-reward input");check(city.effects.officers.stream().filter(o->o.id==1).findFirst().get().meritAfter==expectedMerit,"city merit forecast");}
  else check(w.city(10).equipment[weapon.ordinal()]==production.effects.stockAfterImmediate&&production.effects.meritAfter==expectedMerit,"production effect and merit forecast");
  byte[] committed=SaveCodec.encode(w);check(!command(w,operation,weapon).ok&&Arrays.equals(committed,SaveCodec.encode(w)),"fresh duplicate rejects without extra reward/state/RNG");
  check(Arrays.equals(committed,SaveCodec.encode(SaveCodec.decode(committed))),"complete save roundtrip");
 }
 public static void main(String[] args)throws Exception{
  CityActionPlan.Operation[] operations={CityActionPlan.Operation.PATROL,CityActionPlan.Operation.TRAIN,CityActionPlan.Operation.RECRUIT};int[] stats={0,1,4};
  World.Weapon[] weapons={World.Weapon.SPEAR,World.Weapon.HALBERD,World.Weapon.CROSSBOW,World.Weapon.CAVALRY};
  for(boolean managed:new boolean[]{false,true})for(int base:new int[]{1,50,99})for(int xp:new int[]{0,98,99,100,2998,2999,3000})for(int merit:new int[]{0,59949,59950,60000,60090}){
   for(int n=0;n<operations.length;n++)sample(operations[n],null,stats[n],managed,xp,merit,base);
   for(var weapon:weapons)sample(null,weapon,2,managed,xp,merit,base);
  }
  for(var operation:operations)for(int mode=0;mode<4;mode++){
   World w=fixture(true,null);if(mode==0)w.actionPoints[0]=19;if(mode==1)w.city(10).gold=0;if(mode==2)w.officer(1).acted=true;if(mode==3)w.active=1;
   byte[] before=SaveCodec.encode(w);check(!command(w,operation,null).ok&&Arrays.equals(before,SaveCodec.encode(w)),"refusal adds no XP/merit or RNG");
  }
  for(var weapon:weapons)for(int mode=0;mode<4;mode++){
   World w=fixture(true,weapon);if(mode==0)w.actionPoints[0]=19;if(mode==1)w.city(10).gold=0;if(mode==2)w.officer(1).acted=true;if(mode==3)w.domestic.facilities.clear();
   byte[] before=SaveCodec.encode(w);check(!command(w,null,weapon).ok&&Arrays.equals(before,SaveCodec.encode(w)),"production refusal adds no reward");
  }
  for(var weapon:new World.Weapon[]{World.Weapon.RAM,World.Weapon.CATAPULT,World.Weapon.SIEGE_TOWER,World.Weapon.WOODEN_BEAST}){
   World w=fixture(true,weapon);byte[] before=SaveCodec.encode(w);var p=w.previewProduction(10,1,ProductionPlan.Operation.EQUIPMENT,weapon,null);
   check(p.allowed()&&p.effects.experience.stat==-1&&p.effects.experience.requestedAmount==0&&Arrays.equals(before,SaveCodec.encode(w)),"delayed start does not invent immediate fixed XP");
   check(w.produce(10,1,weapon).ok&&w.officerAbilities.experience(1,2)==0,"real delayed start has no immediate XP2");
  }
  World w=fixture(true,null);check(w.strategy.patrol(10,1).ok,"AI/turn continuation starts from real rewarded command");World replay=SaveCodec.decode(SaveCodec.encode(w));
  for(int n=0;n<6;n++){check(w.nextTurn().ok&&replay.nextTurn().ok,"actual full turn continues");check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(replay)),"multi-turn AI/full-save/RNG continuation");}
  System.out.println("PASS CityCommandRewardsTest checks="+checks+" actual fixed commands, native rewards, legacy, failure, duplicate and continuation");
 }
}
