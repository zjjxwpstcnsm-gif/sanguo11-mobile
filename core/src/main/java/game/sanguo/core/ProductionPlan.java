package game.sanguo.core;
import java.util.*;
import java.util.stream.Collectors;
/** Current-engine manufacturing forecast; formulas/durations still require PC calibration. */
public final class ProductionPlan {
 public enum Operation { EQUIPMENT, SHIP }
 public final RuleFailure failure;
 public final int goldAvailable,goldCost,actionPointsAvailable,actionPointsCost,stockBefore,stockCapacity,pendingBefore,facilityUsesBefore,facilityCapacity;
 public final String facility;
 public final List<Integer> officerIds;
 public final Effects effects;
 public static final class Effects {
  public final boolean delayed,actedBefore,actedAfter;
  public final int goldAfter,actionPointsAfter,outputQuantity,stockAfterImmediate,pendingAfter,busyTurns,facilityUsesAfter,meritBefore,meritAfter;
  public final String taskLabel;
  public final int techniquePointsOwner,techniquePointsBefore,techniquePointsAfter;
  public final boolean nativeTechniquePoints;
  public final OfficerExperiencePlan experience;
  public final List<Actor> actors;
  Effects(World w,int city,World.Officer o,ProductionPlan p,World.Weapon weapon,Army.Ship ship){
   delayed=ship!=null||Army.siegeWeapon(weapon);busyTurns=delayed?w.pcProduction.enabled()?w.pcProduction.taskCounter(p.officerIds.stream().map(w::officer).collect(Collectors.toList()),weapon,ship):w.skills.productionTurns(o.id,weapon):0;
   int requested=delayed?1:w.pcProduction.enabled()?w.pcProduction.amount(city,p.officerIds.stream().map(w::officer).collect(Collectors.toList()),weapon,true):w.skills.produceAmount(city,o.id,weapon);
   outputQuantity=w.pcProduction.enabled()&&!delayed?Math.min(requested,p.stockCapacity-p.stockBefore):requested;stockAfterImmediate=p.stockBefore+(delayed?0:outputQuantity);pendingAfter=p.pendingBefore+(delayed?1:0);
   goldAfter=p.goldAvailable-p.goldCost;actionPointsAfter=p.actionPointsAvailable-p.actionPointsCost;facilityUsesAfter=p.facilityUsesBefore-1;
   techniquePointsOwner=w.city(city).owner;techniquePointsBefore=w.campaign.points(techniquePointsOwner);nativeTechniquePoints=w.pcTechniquePoints.enabled();techniquePointsAfter=delayed?techniquePointsBefore:w.pcTechniquePoints.productionAfter(techniquePointsOwner,outputQuantity);
   taskLabel=delayed?"制造"+(ship!=null?ship.label:weapon.label):"";
   int stat=OfficerExperiencePlan.productionStat(weapon,ship);experience=new OfficerExperiencePlan(w,o,stat,2);
   actedBefore=o.acted;actedAfter=o.acted||p.actionPointsCost>0;meritBefore=w.government.merit(o.id);meritAfter=p.actionPointsCost==0?meritBefore:Math.min(1000000,meritBefore+(w.pcProduction.enabled()&&delayed?0:OfficerExperiencePlan.merit(o,stat,w.government)));
   actors=Collections.unmodifiableList(p.officerIds.stream().map(id->new Actor(w,w.officer(id),stat,p.actionPointsCost,w.pcProduction.enabled()&&delayed)).collect(Collectors.toList()));
  }
 }
 public static final class Actor {
  public final int id,meritBefore,meritAfter;public final boolean actedBefore,actedAfter;public final OfficerExperiencePlan experience;
  Actor(World w,World.Officer o,int stat,int cost,boolean noMerit){id=o.id;experience=new OfficerExperiencePlan(w,o,stat,2);meritBefore=w.government.merit(id);meritAfter=cost==0||noMerit?meritBefore:Math.min(1000000,meritBefore+OfficerExperiencePlan.merit(o,stat,w.government));actedBefore=o.acted;actedAfter=o.acted||cost>0;}
 }
 ProductionPlan(World w,int city,int officer,Operation op,World.Weapon weapon,Army.Ship ship){
  this(w,city,new int[]{officer},op,weapon,ship);
 }
 ProductionPlan(World w,int city,int[] selected,Operation op,World.Weapon weapon,Army.Ship ship){
  int[] ids=selected==null?new int[0]:selected.clone();int officer=ids.length==0?-1:ids[0];officerIds=Collections.unmodifiableList(Arrays.stream(ids).boxed().collect(Collectors.toList()));
  World.City c=w.city(city);World.Officer o=w.officer(officer);
  boolean malformed=op==null||op==Operation.EQUIPMENT&&ship!=null||op==Operation.SHIP&&weapon!=null;
  boolean naval=op==Operation.SHIP;Domestic.Kind kind=naval?Domestic.Kind.SHIPYARD:Domestic.productionFacility(weapon);
  facility=kind==null?"":kind.name();facilityCapacity=kind==null?0:w.domestic.capacity(city,kind);facilityUsesBefore=kind==null?0:w.domestic.remainingUses(city,kind);
  goldAvailable=c==null?0:c.gold;actionPointsAvailable=w.cityActionPoints(c);actionPointsCost=w.cityActionCost(o,PcCityActionCosts.PRODUCTION);
  goldCost=actionPointsCost==0?0:naval?(ship==null||c==null?0:w.pcProduction.enabled()?w.pcProduction.gold(city,PcProduction.nativeItem(ship)):ship.gold):c==null?0:w.skills.productionGold(city,officer,weapon);
  stockBefore=c==null?0:naval?(ship==null||ship==Army.Ship.BOAT?0:c.ships[ship.ordinal()-1]):weapon==null?0:c.equipment[weapon.ordinal()];
  stockCapacity=c==null?0:naval||Army.siegeWeapon(weapon)?100:weapon==null?0:w.campaign.equipmentCap(c,weapon);
  pendingBefore=malformed?0:(int)w.army.productions.stream().filter(p->p.cityId==city&&p.weapon==weapon&&p.ship==ship).count();
  RuleFailure error=ids.length<1||ids.length>3||new HashSet<>(officerIds).size()!=ids.length?new RuleFailure("PRODUCTION_ACTORS","officers","请选择1至3名不同的执行武将"):malformed?new RuleFailure("PRODUCTION_OPERATION","operation","请选择兵装或舰船生产"):ids.length>1&&!w.pcProduction.enabled()?new RuleFailure("PRODUCTION_LEGACY_MODE","officers","此旧存档保留原制造模式，未补填多人制造数据"):null;
  if(error==null)for(int id:ids){error=naval?w.army.productionFailure(city,id,null,ship):w.productionFailure(city,id,weapon);if(error!=null)break;}
  failure=error;
  effects=failure==null?new Effects(w,city,o,this,weapon,ship):null;
 }
 public boolean allowed(){return failure==null;}
}
