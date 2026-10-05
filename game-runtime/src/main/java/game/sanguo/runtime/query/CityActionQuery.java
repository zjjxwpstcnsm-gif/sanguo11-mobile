package game.sanguo.runtime.query;
import game.sanguo.api.*;
import game.sanguo.core.*;
import java.util.*;
public final class CityActionQuery {
 private CityActionQuery(){}
 public static CityActionPlan.Operation operation(String name){try{return CityActionPlan.Operation.valueOf(name);}catch(IllegalArgumentException|NullPointerException e){return null;}}
 public static CityActionPreview capture(World w,StateToken state,CityActionCommand c){
  CityActionPlan p=w.strategy.previewCityAction(operation(c.operation),c.cityId,c.officerId,c.targets());CityActionPreview.Effects effects=null;
  if(p.effects!=null){var f=p.effects;List<CityActionPreview.OfficerEffect> officers=new ArrayList<>();for(var o:f.officers){var e=o.experience;officers.add(new CityActionPreview.OfficerEffect(o.id,o.loyaltyBefore,o.loyaltyAfter,o.meritBefore,o.meritAfter,o.lastRewardTurnBefore,o.lastRewardTurnAfter,o.remainingTurnsBefore,o.remainingTurnsAfter,o.actedBefore,o.actedAfter,o.roleBefore,o.roleAfter,new OfficerExperienceChange(e.managed,e.stat,e.requestedAmount,e.experienceBefore,e.experienceAfter,e.currentBefore,e.currentAfter)));}
   effects=new CityActionPreview.Effects(f.orderBefore,f.orderAfter,f.moraleBefore,f.moraleAfter,f.troopsBefore,f.troopsAfter,f.reserveBefore,f.reserveAfter,f.governorBefore,f.governorAfter,f.busyTurns,f.barracksUsesBefore,f.barracksUsesAfter,officers);
  }
  var s=p.search;return new CityActionPreview(state,p.allowed()?CommandResult.Error.NONE:CommandResult.Error.RULE_REJECTED,p.allowed()?"NONE":p.failure.code,p.allowed()?"global":p.failure.field,p.allowed()?"":p.failure.detail,
   new CityActionPreview.Resources(p.goldAvailable,p.goldCost,p.goldRemaining,p.actionPointsAvailable,p.actionPointsCost,p.actionPointsRemaining),effects,s==null?null:new CityActionPreview.Search(s.officerCheckChance,s.goldCheckChance,s.goldFoundMinimum,s.goldFoundMaximum,s.possibleOutcomeKinds));
 }
}
