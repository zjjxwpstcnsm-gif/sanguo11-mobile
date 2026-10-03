package game.sanguo.core;

/** Native once-per-city-per-turn admission; prices/quantity limits remain modeled. */
public final class TradePlan {
 public enum Operation { BUY, SELL }
 public final RuleFailure failure;
 public final int requestedFood,minimum=1000,maximum=20000,step=1000,pricePerThousand;
 public final int tradedBefore,quotaRemaining,availableMaximum,goldBefore,foodBefore,goldCapacity,foodCapacity,actionPointsBefore,actionPointsCost;
 public final boolean quantityValid;
 public final long quotedGold;
 public final Effects effects;
 public static final class Effects {
  public final int goldAfter,foodAfter,actionPointsAfter,tradedAfter,meritBefore,meritAfter;
  public final boolean actedBefore,actedAfter,abilityStateManaged;
  public final int politicsExperienceBefore,politicsExperienceAfter,politicsBefore,politicsAfter;
  Effects(World w,World.Officer o,TradePlan p,Operation op){
   abilityStateManaged=w.officerAbilities.enabled();politicsBefore=o.politics;
   politicsExperienceBefore=abilityStateManaged?w.officerAbilities.experience(o.id,3):-1;
   politicsExperienceAfter=abilityStateManaged?w.officerAbilities.experienceAfter(o.id,3,5):-1;
   politicsAfter=w.officerAbilities.afterExperience(o.id,3,5);
   int paid=p.actionPointsCost==0?0:(int)p.quotedGold;
   goldAfter=p.goldBefore+(op==Operation.BUY?-paid:(int)p.quotedGold);
   foodAfter=p.foodBefore+(op==Operation.BUY?p.requestedFood:-p.requestedFood);
   actionPointsAfter=p.actionPointsBefore-p.actionPointsCost;tradedAfter=p.tradedBefore+p.requestedFood;
   actedBefore=o.acted;actedAfter=o.acted||p.actionPointsCost>0;meritBefore=w.government.merit(o.id);meritAfter=meritBefore+(p.actionPointsCost==0?0:PcMerchantRules.meritGain(meritBefore));
  }
 }
 TradePlan(World w,int city,int officer,Operation op,int food){
  World.City c=w.city(city);World.Officer o=w.officer(officer);
  requestedFood=food;quantityValid=food>=minimum&&food<=maximum&&food%step==0;
  pricePerThousand=op==null?0:w.campaign.foodPrice(city,op==Operation.BUY);
  quotedGold=(long)(food/step)*pricePerThousand;
  tradedBefore=w.campaign.traded(city);quotaRemaining=c==null||c.kind!=World.SiteKind.CITY||tradedBefore>0?0:maximum;
  goldBefore=c==null?0:c.gold;foodBefore=c==null?0:c.food;
  goldCapacity=c==null?0:w.campaign.goldCap(c);foodCapacity=c==null?0:w.campaign.foodCap(c);
  actionPointsBefore=w.active>=0&&w.active<w.actionPoints.length?w.actionPoints[w.active]:0;actionPointsCost=w.cityActionCost(o,PcCityActionCosts.TRADE);
  long capacity=0;
  if(c!=null&&op!=null&&pricePerThousand>0){
   capacity=op==Operation.BUY?Math.min((long)foodCapacity-foodBefore,actionPointsCost==0?maximum:(long)goldBefore/pricePerThousand*step):Math.min((long)foodBefore,((long)goldCapacity-goldBefore)/pricePerThousand*step);
  }
  availableMaximum=(int)(Math.max(0,Math.min(quotaRemaining,capacity))/step*step);
  failure=validate(w,c,o,op);effects=failure==null?new Effects(w,o,this,op):null;
 }
 private RuleFailure validate(World w,World.City c,World.Officer o,Operation op){
  if(op==null)return new RuleFailure("TRADE_OPERATION","operation","请选择买粮或卖粮");
  if(!quantityValid)return new RuleFailure("TRADE_QUANTITY","food","交易量须为1000至20000的整千粮");
  RuleFailure f=w.cityFailure(c,o,op==Operation.BUY?(int)quotedGold:0,PcCityActionCosts.TRADE);if(f!=null)return f;
  if(c.kind!=World.SiteKind.CITY)return new RuleFailure("TRADE_SITE","city","商人交易只能在城市执行，关隘和港口不能交易");
  if(tradedBefore>0)return new RuleFailure("TRADE_USED","city","本城本旬已执行商人交易，请待下旬再交易");
  if(op==Operation.BUY?c.food>w.campaign.foodCap(c)-requestedFood:c.food<requestedFood||c.gold>w.campaign.goldCap(c)-quotedGold)
   return new RuleFailure("TRADE_STOCK","food","粮草不足或库存容量不足");
  return null;
 }
 public boolean allowed(){return failure==null;}
}
