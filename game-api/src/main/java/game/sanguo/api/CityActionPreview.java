package game.sanguo.api;
import java.util.*;
/** Immutable authority forecast, never a speculative execution or advance of RNG. */
public final class CityActionPreview {
 public final StateToken state;
 public final CommandResult.Error error;
 public final String reasonCode,field,detail;
 public final Resources resources;
 public final Effects effects;
 public final Search search;
 public CityActionPreview(StateToken state,CommandResult.Error error,String code,String field,String detail,Resources resources,Effects effects,Search search){this.state=state;this.error=error;reasonCode=code;this.field=field;this.detail=detail;this.resources=resources;this.effects=effects;this.search=search;}
 public boolean allowed(){return error==CommandResult.Error.NONE;}
 public static CityActionPreview unavailable(StateToken state,CommandResult.Error error){return new CityActionPreview(state,error,error.name(),"global",error.name(),null,null,null);}
 public static final class Resources {
  public final int goldAvailable,actionPointsAvailable,actionPointsCost;
  public final long goldCost,goldRemaining,actionPointsRemaining;
  public Resources(int gold,long cost,long remaining,int ap,int apCost,long apRemaining){goldAvailable=gold;goldCost=cost;goldRemaining=remaining;actionPointsAvailable=ap;actionPointsCost=apCost;actionPointsRemaining=apRemaining;}
 }
 public static final class Effects {
  public final int orderBefore,orderAfter,moraleBefore,moraleAfter,troopsBefore,troopsAfter,reserveBefore,reserveAfter,governorBefore,governorAfter,busyTurns,barracksUsesBefore,barracksUsesAfter;
  public final List<OfficerEffect> officers;
  public Effects(int orderBefore,int orderAfter,int moraleBefore,int moraleAfter,int troopsBefore,int troopsAfter,int reserveBefore,int reserveAfter,int governorBefore,int governorAfter,int busyTurns,int usesBefore,int usesAfter,List<OfficerEffect> officers){this.orderBefore=orderBefore;this.orderAfter=orderAfter;this.moraleBefore=moraleBefore;this.moraleAfter=moraleAfter;this.troopsBefore=troopsBefore;this.troopsAfter=troopsAfter;this.reserveBefore=reserveBefore;this.reserveAfter=reserveAfter;this.governorBefore=governorBefore;this.governorAfter=governorAfter;this.busyTurns=busyTurns;barracksUsesBefore=usesBefore;barracksUsesAfter=usesAfter;this.officers=Collections.unmodifiableList(new ArrayList<>(officers));}
 }
 public static final class OfficerEffect {
  public final int id,loyaltyBefore,loyaltyAfter,meritBefore,meritAfter,lastRewardTurnBefore,lastRewardTurnAfter,remainingTurnsBefore,remainingTurnsAfter;
  public final boolean actedBefore,actedAfter;
  public final String roleBefore,roleAfter;
  public final OfficerExperienceChange experience;
  public OfficerEffect(int id,int loyaltyBefore,int loyaltyAfter,int meritBefore,int meritAfter,int rewardBefore,int rewardAfter,int remainingBefore,int remainingAfter,boolean actedBefore,boolean actedAfter,String roleBefore,String roleAfter){this(id,loyaltyBefore,loyaltyAfter,meritBefore,meritAfter,rewardBefore,rewardAfter,remainingBefore,remainingAfter,actedBefore,actedAfter,roleBefore,roleAfter,null);}
  public OfficerEffect(int id,int loyaltyBefore,int loyaltyAfter,int meritBefore,int meritAfter,int rewardBefore,int rewardAfter,int remainingBefore,int remainingAfter,boolean actedBefore,boolean actedAfter,String roleBefore,String roleAfter,OfficerExperienceChange experience){this.id=id;this.loyaltyBefore=loyaltyBefore;this.loyaltyAfter=loyaltyAfter;this.meritBefore=meritBefore;this.meritAfter=meritAfter;lastRewardTurnBefore=rewardBefore;lastRewardTurnAfter=rewardAfter;remainingTurnsBefore=remainingBefore;remainingTurnsAfter=remainingAfter;this.actedBefore=actedBefore;this.actedAfter=actedAfter;this.roleBefore=roleBefore;this.roleAfter=roleAfter;this.experience=experience;}
 }
 /** Both probabilities are conditional checks, not unconditional chances of outcomes. */
 public static final class Search {
  public final int officerCheckChance,goldCheckChance,goldFoundMinimum,goldFoundMaximum;
  public final List<String> possibleOutcomeKinds;
  public Search(int officerChance,int goldChance,int minimum,int maximum,List<String> kinds){officerCheckChance=officerChance;goldCheckChance=goldChance;goldFoundMinimum=minimum;goldFoundMaximum=maximum;possibleOutcomeKinds=Collections.unmodifiableList(new ArrayList<>(kinds));}
 }
}
