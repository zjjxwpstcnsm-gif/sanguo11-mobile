package game.sanguo.core;
import java.util.*;
/** Current-engine limits and conditional forecast; not a claim of PC formula parity. */
public final class TransportPlan {
 public final RuleFailure failure;
 public final int actionPointsAvailable,actionPointsCost;
 public final List<Stock> stocks;
 public final Forecast forecast;
 public static final class Stock {
  public final String kind;
  public final int sourceAvailable,dispatchLimit,destinationFree;
  Stock(String kind,int available,int limit,int free){this.kind=kind;sourceAvailable=available;dispatchLimit=limit;destinationFree=free;}
 }
 public static final class Forecast {
  public final int turns,foodPerTurn,departureQ,departureR,departureCost,movementRemaining;
  public final long projectedFoodUse,projectedFoodArrival;
  public final boolean shortage,capacityFits,returnOfficers;
  Forecast(World w,Domestic.Mission m,SiteFootprint.Deployment departure,int turns,boolean fits){
   this.turns=turns;foodPerTurn=w.domestic.foodUse(m);projectedFoodUse=turns<0?-1:(long)turns*foodPerTurn;
   projectedFoodArrival=turns<0?-1:Math.max(0,(long)m.food-projectedFoodUse);shortage=turns>=0&&m.food<projectedFoodUse;
   departureQ=m.hex.q;departureR=m.hex.r;departureCost=departure.cost;movementRemaining=w.orders.remaining(m);capacityFits=fits;returnOfficers=m.returnOfficers;
  }
 }
 TransportPlan(World w,World.City c,World.City d,World.Officer o,RuleFailure failure,Domestic.Mission m,SiteFootprint.Deployment departure,int turns,boolean fits){
  this.failure=failure;actionPointsAvailable=w.active>=0&&w.active<w.actionPoints.length?w.actionPoints[w.active]:0;actionPointsCost=w.cityActionCost(o);
  List<Stock> values=new ArrayList<>();
  int reserveGold=0,reserveFood=0,reserveTroops=0;
  if(c!=null&&w.districts.executing(c.id)){Districts.District district=w.districts.city(c.id);reserveGold=district.reserveGold();reserveFood=district.reserveFood();reserveTroops=district.reserveTroops();}
  values.add(stock("GOLD",c==null?0:c.gold,100000,reserveGold,d==null?0:w.campaign.goldCap(d)-d.gold));
  values.add(stock("FOOD",c==null?0:c.food,200000,reserveFood,d==null?0:w.campaign.foodCap(d)-d.food));
  values.add(stock("TROOPS",c==null?0:c.troops,20000,reserveTroops,d==null?0:w.campaign.troopCap(d)-d.troops));
  for(World.Weapon weapon:World.Weapon.values()){
   int i=weapon.ordinal();values.add(stock(weapon.name(),c==null?0:c.equipment[i],Domestic.equipmentCargoLimit(i),0,d==null?0:w.campaign.equipmentCap(d,weapon)-d.equipment[i]));
  }
  for(int i=0;i<2;i++)values.add(stock(Army.Ship.values()[i+1].name(),c==null?0:c.ships[i],100,0,d==null?0:100-d.ships[i]));
  stocks=Collections.unmodifiableList(values);forecast=m==null?null:new Forecast(w,m,departure,turns,fits);
 }
 private static Stock stock(String kind,int available,int hardLimit,int reserve,int free){return new Stock(kind,available,Math.min(hardLimit,Math.max(0,available-reserve)),Math.max(0,free));}
 public boolean allowed(){return failure==null;}
}
