package game.sanguo.api;
/** Pure current-engine quote. Constraints survive invalid quantities; effects exist only when allowed. */
public final class TradePreview {
 public final StateToken state;
 public final CommandResult.Error error;
 public final String reasonCode,field,detail;
 public final Quote quote;
 public final Effects effects;
 public TradePreview(StateToken state,CommandResult.Error error,String reasonCode,String field,String detail,Quote quote,Effects effects){this.state=state;this.error=error;this.reasonCode=reasonCode;this.field=field;this.detail=detail;this.quote=quote;this.effects=effects;}
 public boolean allowed(){return error==CommandResult.Error.NONE;}
 public static TradePreview unavailable(StateToken state,CommandResult.Error error){return new TradePreview(state,error,error.name(),"global",error.name(),null,null);}
 public static final class Quote {
  /** tradedBefore retains saved volume; any positive value means used this turn.
   * quotaRemaining is zero for non-city sites or after any trade, otherwise maximum,
   * not maximum minus volume. Neither quantityValid nor availableMaximum replaces allowed(). */
  public final int requestedFood,minimum,maximum,step,pricePerThousand,tradedBefore,quotaRemaining,availableMaximum,goldBefore,foodBefore,goldCapacity,foodCapacity,actionPointsBefore,actionPointsCost;
  public final boolean quantityValid;
  /** Arithmetic quote; only payable if quantityValid and the whole preview is allowed. */
  public final long quotedGold;
  public Quote(int requestedFood,int minimum,int maximum,int step,int pricePerThousand,int tradedBefore,int quotaRemaining,int availableMaximum,int goldBefore,int foodBefore,int goldCapacity,int foodCapacity,int actionPointsBefore,int actionPointsCost,boolean quantityValid,long quotedGold){this.requestedFood=requestedFood;this.minimum=minimum;this.maximum=maximum;this.step=step;this.pricePerThousand=pricePerThousand;this.tradedBefore=tradedBefore;this.quotaRemaining=quotaRemaining;this.availableMaximum=availableMaximum;this.goldBefore=goldBefore;this.foodBefore=foodBefore;this.goldCapacity=goldCapacity;this.foodCapacity=foodCapacity;this.actionPointsBefore=actionPointsBefore;this.actionPointsCost=actionPointsCost;this.quantityValid=quantityValid;this.quotedGold=quotedGold;}
 }
 public static final class Effects {
  public final int goldAfter,foodAfter,actionPointsAfter,tradedAfter,meritBefore,meritAfter;
  public final boolean actedBefore,actedAfter,abilityStateManaged;
  /** Experience is -1 in legacy saves whose base state is unknown. */
  public final int politicsExperienceBefore,politicsExperienceAfter,politicsBefore,politicsAfter;
  public Effects(int goldAfter,int foodAfter,int actionPointsAfter,int tradedAfter,int meritBefore,int meritAfter,boolean actedBefore,boolean actedAfter){this(goldAfter,foodAfter,actionPointsAfter,tradedAfter,meritBefore,meritAfter,actedBefore,actedAfter,false,-1,-1,-1,-1);}
  public Effects(int goldAfter,int foodAfter,int actionPointsAfter,int tradedAfter,int meritBefore,int meritAfter,boolean actedBefore,boolean actedAfter,boolean abilityStateManaged,int politicsExperienceBefore,int politicsExperienceAfter,int politicsBefore,int politicsAfter){this.abilityStateManaged=abilityStateManaged;this.politicsExperienceBefore=politicsExperienceBefore;this.politicsExperienceAfter=politicsExperienceAfter;this.politicsBefore=politicsBefore;this.politicsAfter=politicsAfter;this.goldAfter=goldAfter;this.foodAfter=foodAfter;this.actionPointsAfter=actionPointsAfter;this.tradedAfter=tradedAfter;this.meritBefore=meritBefore;this.meritAfter=meritAfter;this.actedBefore=actedBefore;this.actedAfter=actedAfter;}
 }
}
