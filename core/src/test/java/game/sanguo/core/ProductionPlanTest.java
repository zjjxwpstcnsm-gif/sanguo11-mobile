package game.sanguo.core;
import java.util.*;
public final class ProductionPlanTest {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static World fixture(World.Weapon weapon,Army.Ship ship){
  World w=CityActionPlanTest.fixture();Domestic.Kind kind=ship!=null?Domestic.Kind.SHIPYARD:Domestic.productionFacility(weapon);
  if(kind!=null){Hex h=w.domestic.buildSites(10).get(0);if(ship!=null){Hex water=h.neighbors().stream().filter(n->w.inside(n)&&w.cityAt(n)==null&&w.domestic.at(n)==null).findFirst().orElseThrow();w.terrain[water.q][water.r]=World.Terrain.WATER;}w.domestic.facilities.add(new Domestic.Facility(w.domestic.nextFacilityId++,10,kind,h,-1,0));}
  w.campaign.learned.put(0,EnumSet.allOf(Campaign.Tech.class));w.campaign.learned.get(0).remove(Campaign.Tech.WARSHIP);return w;
 }
 static void sample(World.Weapon weapon,Army.Ship ship,int mode)throws Exception{
  World w=fixture(weapon,ship);int actor=1,city=10;var op=ship!=null?ProductionPlan.Operation.SHIP:ProductionPlan.Operation.EQUIPMENT;
  if(mode==1)w.actionPoints[0]=9;if(mode==2)w.city(10).gold=0;if(mode==3)w.officer(1).acted=true;if(mode==4)actor=999;if(mode==5)city=999;
  if(mode==6)w.domestic.facilities.clear();if(mode==7)for(var f:w.domestic.facilities)f.lastUseTurn=w.turn;
  if(mode==8)w.campaign.learned.clear();if(mode==9){if(ship!=null&&ship!=Army.Ship.BOAT)w.city(10).ships[ship.ordinal()-1]=100;else if(weapon!=null&&weapon!=World.Weapon.SWORD)w.city(10).equipment[weapon.ordinal()]=w.campaign.equipmentCap(w.city(10),weapon);}
  if(mode==10)w.officer(1).skillId=ship!=null?Skill.ZAOCHUAN.id:Army.siegeWeapon(weapon)?Skill.FAMING.id:weapon==World.Weapon.CAVALRY?Skill.FANZHI.id:Skill.NENGLI.id;
  byte[] before=SaveCodec.encode(w);long rng=w.strategy.getRandomState();var p=w.previewProduction(city,actor,op,weapon,ship);
  for(int n=0;n<3;n++)w.previewProduction(city,actor,op,weapon,ship);check(Arrays.equals(before,SaveCodec.encode(w)),"preview pure");
  var r=ship!=null?w.army.produce(city,actor,null,ship):w.produce(city,actor,weapon);check(r.ok==p.allowed(),"ordinary admission "+weapon+":"+ship+":"+mode+":"+r.message);
  if(!r.ok){check(p.effects==null&&p.failure.detail.equals(r.message)&&Arrays.equals(before,SaveCodec.encode(w)),"exact failure atomic");return;}
  var e=p.effects;var c=w.city(city);var o=w.officer(actor);int stock=ship!=null?c.ships[ship.ordinal()-1]:c.equipment[weapon.ordinal()];
  check(c.gold==e.goldAfter&&w.actionPoints[0]==e.actionPointsAfter&&stock==e.stockAfterImmediate,"actual immediate ledger");
  check(o.acted==e.actedAfter&&o.otherTaskTurns==e.busyTurns&&o.otherTask.equals(e.taskLabel)&&w.government.merit(actor)==e.meritAfter,"actual officer ledger");
  Domestic.Kind kind=Domestic.Kind.valueOf(p.facility);check(w.domestic.remainingUses(city,kind)==e.facilityUsesAfter,"actual factory consumed once");check(w.strategy.getRandomState()==rng,"no RNG consumed");
  for(int n=0;n<4;n++){World restored=SaveCodec.decode(SaveCodec.encode(w));check(w.nextTurn().ok&&restored.nextTurn().ok,"ordinary full turn");check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(restored)),"save RNG replay");if(e.delayed){int actual=ship!=null?w.city(city).ships[ship.ordinal()-1]:w.city(city).equipment[weapon.ordinal()];check(actual==p.stockBefore+(n+1>=e.busyTurns?1:0),"arrives once on exact completion turn");}}
 }
 public static void main(String[] args)throws Exception{
  for(var weapon:World.Weapon.values())for(int mode=0;mode<=10;mode++)sample(weapon,null,mode);
  for(var ship:Army.Ship.values())for(int mode=0;mode<=10;mode++)sample(null,ship,mode);
  World w=fixture(World.Weapon.RAM,null);byte[] before=SaveCodec.encode(w);check(!w.previewProduction(10,1,null,null,null).allowed()&&!w.previewProduction(10,1,ProductionPlan.Operation.EQUIPMENT,World.Weapon.RAM,Army.Ship.TOWER_SHIP).allowed()&&Arrays.equals(before,SaveCodec.encode(w)),"malformed operation/union pure");
  w.city(10).equipment[World.Weapon.RAM.ordinal()]=99;check(w.produce(10,1,World.Weapon.RAM).ok,"last manufacturing slot accepted");
  var kind=Domestic.Kind.WORKSHOP;var h=w.domestic.buildSites(10).get(0);w.domestic.facilities.add(new Domestic.Facility(w.domestic.nextFacilityId++,10,kind,h,-1,0));
  var p=w.previewProduction(10,2,ProductionPlan.Operation.EQUIPMENT,World.Weapon.RAM,null);check(p.pendingBefore==1&&p.stockBefore==99&&p.failure.code.equals("PRODUCTION_CAPACITY"),"pending job reserves final slot");
  System.out.println("PASS ProductionPlanTest checks="+checks);
 }
}
