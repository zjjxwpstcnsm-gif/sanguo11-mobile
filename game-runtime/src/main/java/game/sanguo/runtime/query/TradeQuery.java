package game.sanguo.runtime.query;
import game.sanguo.api.*;
import game.sanguo.core.*;
public final class TradeQuery {
 private TradeQuery(){}
 public static TradePlan.Operation operation(String name){try{return TradePlan.Operation.valueOf(name);}catch(IllegalArgumentException|NullPointerException e){return null;}}
 public static TradePreview capture(World w,StateToken state,TradeCommand c){
  TradePlan p=w.campaign.previewTrade(c.cityId,c.officerId,operation(c.operation),c.food);var f=p.effects;
  return new TradePreview(state,p.allowed()?CommandResult.Error.NONE:CommandResult.Error.RULE_REJECTED,p.allowed()?"NONE":p.failure.code,p.allowed()?"global":p.failure.field,p.allowed()?"":p.failure.detail,
   new TradePreview.Quote(p.requestedFood,p.minimum,p.maximum,p.step,p.pricePerThousand,p.tradedBefore,p.quotaRemaining,p.availableMaximum,p.goldBefore,p.foodBefore,p.goldCapacity,p.foodCapacity,p.actionPointsBefore,p.actionPointsCost,p.quantityValid,p.quotedGold,p.nativePricing,p.marketRate,p.pricingPoliticsBefore,p.pricingPoliticsAfter,p.rawGoldQuote),
   f==null?null:new TradePreview.Effects(f.goldAfter,f.foodAfter,f.actionPointsAfter,f.tradedAfter,f.meritBefore,f.meritAfter,f.actedBefore,f.actedAfter,f.abilityStateManaged,f.politicsExperienceBefore,f.politicsExperienceAfter,f.politicsBefore,f.politicsAfter));
 }
}
