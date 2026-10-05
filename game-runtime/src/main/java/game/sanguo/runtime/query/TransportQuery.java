package game.sanguo.runtime.query;
import game.sanguo.api.*;
import game.sanguo.core.*;
import java.util.*;
public final class TransportQuery {
 private TransportQuery(){}
 public static TransportPreview capture(World w,StateToken state,TransportCommand c){
  TransportPlan p=w.domestic.previewTransport(c.sourceCityId,c.targetCityId,c.officerId,c.deputies(),c.gold,c.food,c.troops,c.equipment(),c.sea,c.returnOfficers,c.ships());
  List<TransportPreview.Stock> stocks=new ArrayList<>();for(TransportPlan.Stock s:p.stocks)stocks.add(new TransportPreview.Stock(s.kind,s.sourceAvailable,s.dispatchLimit,s.destinationFree));
  TransportPlan.Forecast f=p.forecast;
  return new TransportPreview(state,p.allowed()?CommandResult.Error.NONE:CommandResult.Error.RULE_REJECTED,p.allowed()?"NONE":p.failure.code,p.allowed()?"global":p.failure.field,p.allowed()?"":p.failure.detail,
   new TransportPreview.Resources(p.actionPointsAvailable,p.actionPointsCost,stocks),f==null?null:new TransportPreview.Forecast(f.turns,f.foodPerTurn,f.projectedFoodUse,f.projectedFoodArrival,f.departureQ,f.departureR,f.departureCost,f.movementRemaining,f.shortage,f.capacityFits,f.returnOfficers));
 }
}
