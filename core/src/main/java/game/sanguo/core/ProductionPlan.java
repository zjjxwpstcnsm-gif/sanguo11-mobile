package game.sanguo.core;
/** Current-engine manufacturing forecast; formulas/durations still require PC calibration. */
public final class ProductionPlan {
 public enum Operation { EQUIPMENT, SHIP }
 public final RuleFailure failure;
 public final int goldAvailable,goldCost,actionPointsAvailable,actionPointsCost,stockBefore,stockCapacity,pendingBefore,facilityUsesBefore,facilityCapacity;
 public final String facility;
 public final Effects effects;
 public static final class Effects {
  public final boolean delayed,actedBefore,actedAfter;
  public final int goldAfter,actionPointsAfter,outputQuantity,stockAfterImmediate,pendingAfter,busyTurns,facilityUsesAfter,meritBefore,meritAfter;
  public final String taskLabel;
  Effects(World w,int city,World.Officer o,ProductionPlan p,World.Weapon weapon,Army.Ship ship){
   delayed=ship!=null||Army.siegeWeapon(weapon);busyTurns=delayed?w.skills.productionTurns(o.id,weapon):0;
   outputQuantity=delayed?1:w.skills.produceAmount(city,o.id,weapon);stockAfterImmediate=p.stockBefore+(delayed?0:outputQuantity);pendingAfter=p.pendingBefore+(delayed?1:0);
   goldAfter=p.goldAvailable-p.goldCost;actionPointsAfter=p.actionPointsAvailable-p.actionPointsCost;facilityUsesAfter=p.facilityUsesBefore-1;
   taskLabel=delayed?"制造"+(ship!=null?ship.label:weapon.label):"";
   actedBefore=o.acted;actedAfter=o.acted||p.actionPointsCost>0;meritBefore=w.government.merit(o.id);meritAfter=p.actionPointsCost==0?meritBefore:Math.min(1000000,meritBefore+100);
  }
 }
 ProductionPlan(World w,int city,int officer,Operation op,World.Weapon weapon,Army.Ship ship){
  World.City c=w.city(city);World.Officer o=w.officer(officer);
  boolean malformed=op==null||op==Operation.EQUIPMENT&&ship!=null||op==Operation.SHIP&&weapon!=null;
  boolean naval=op==Operation.SHIP;Domestic.Kind kind=naval?Domestic.Kind.SHIPYARD:Domestic.productionFacility(weapon);
  facility=kind==null?"":kind.name();facilityCapacity=kind==null?0:w.domestic.capacity(city,kind);facilityUsesBefore=kind==null?0:w.domestic.remainingUses(city,kind);
  goldAvailable=c==null?0:c.gold;actionPointsAvailable=w.active>=0&&w.active<w.actionPoints.length?w.actionPoints[w.active]:0;actionPointsCost=w.cityActionCost(o,PcCityActionCosts.PRODUCTION);
  goldCost=actionPointsCost==0?0:naval?(ship==null?0:ship.gold):w.skills.productionGold(officer,weapon);
  stockBefore=c==null?0:naval?(ship==null||ship==Army.Ship.BOAT?0:c.ships[ship.ordinal()-1]):weapon==null?0:c.equipment[weapon.ordinal()];
  stockCapacity=c==null?0:naval||Army.siegeWeapon(weapon)?100:weapon==null?0:w.campaign.equipmentCap(c,weapon);
  pendingBefore=malformed?0:(int)w.army.productions.stream().filter(p->p.cityId==city&&p.weapon==weapon&&p.ship==ship).count();
  failure=malformed?new RuleFailure("PRODUCTION_OPERATION","operation","请选择兵装或舰船生产"):naval?w.army.productionFailure(city,officer,null,ship):w.productionFailure(city,officer,weapon);
  effects=failure==null?new Effects(w,city,o,this,weapon,ship):null;
 }
 public boolean allowed(){return failure==null;}
}
