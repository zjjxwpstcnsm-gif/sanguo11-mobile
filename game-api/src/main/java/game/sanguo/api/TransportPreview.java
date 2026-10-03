package game.sanguo.api;
import java.util.*;
/** Current-engine draft facts; ETA/food are conditional on unchanged route and rate. */
public final class TransportPreview {
 public final StateToken state;
 public final CommandResult.Error error;
 public final String reasonCode,field,detail;
 public final Resources resources;
 public final Forecast forecast;
 public TransportPreview(StateToken state,CommandResult.Error error,String code,String field,String detail,Resources resources,Forecast forecast){this.state=state;this.error=error;reasonCode=code;this.field=field;this.detail=detail;this.resources=resources;this.forecast=forecast;}
 public boolean allowed(){return error==CommandResult.Error.NONE;}
 public static TransportPreview unavailable(StateToken state,CommandResult.Error error){return new TransportPreview(state,error,error.name(),"global",error.name(),null,null);}
 public static final class Stock {
  public final String kind;
  public final int sourceAvailable,dispatchLimit,destinationFree;
  public Stock(String kind,int available,int limit,int free){this.kind=kind;sourceAvailable=available;dispatchLimit=limit;destinationFree=free;}
 }
 public static final class Resources {
  public final int actionPointsAvailable,actionPointsCost;
  public final List<Stock> stocks;
  public Resources(int ap,int cost,List<Stock> stocks){actionPointsAvailable=ap;actionPointsCost=cost;this.stocks=Collections.unmodifiableList(new ArrayList<>(stocks));}
 }
 public static final class Forecast {
  public final int turns,foodPerTurn,departureQ,departureR,departureCost,movementRemaining;
  public final long projectedFoodUse,projectedFoodArrival;
  public final boolean shortage,capacityFits,returnOfficers;
  public Forecast(int turns,int foodPerTurn,long use,long arrival,int q,int r,int departureCost,int movementRemaining,boolean shortage,boolean fits,boolean returning){this.turns=turns;this.foodPerTurn=foodPerTurn;projectedFoodUse=use;projectedFoodArrival=arrival;departureQ=q;departureR=r;this.departureCost=departureCost;this.movementRemaining=movementRemaining;this.shortage=shortage;capacityFits=fits;returnOfficers=returning;}
 }
}
