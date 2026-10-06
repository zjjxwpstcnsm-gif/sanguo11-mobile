package game.sanguo.core;
import java.util.*;
/** Immutable current-engine city effects. Native formula calibration remains separate. */
public final class CityActionPlan {
 public enum Operation { PATROL, TRAIN, RECRUIT, SEARCH, REWARD, APPOINT_GOVERNOR }
 public final RuleFailure failure;
 public final int goldAvailable,actionPointsAvailable,actionPointsCost;
 public final long goldCost,goldRemaining,actionPointsRemaining;
 public final Effects effects;
 public final Search search;
 public static final class Effects {
  public final int orderBefore,orderAfter,moraleBefore,moraleAfter,troopsBefore,troopsAfter,reserveBefore,reserveAfter,governorBefore,governorAfter,busyTurns,barracksUsesBefore,barracksUsesAfter;
  public final List<OfficerEffect> officers;
  Effects(World w,Operation op,World.City c,World.Officer actor,int[] targets){
   orderBefore=c.order;moraleBefore=c.morale;troopsBefore=c.troops;reserveBefore=c.recruitReserve;governorBefore=c.governorId;busyTurns=0;
   int amount=op==Operation.RECRUIT?w.strategy.recruitAmount(c.id,actor.id):0;
   orderAfter=op==Operation.PATROL?c.order+w.strategy.patrolGain(c,actor):op==Operation.RECRUIT?c.order-w.strategy.recruitOrderLoss(c,actor):c.order;
   moraleAfter=op==Operation.TRAIN?c.morale+w.strategy.trainingGain(c,actor):op==Operation.RECRUIT?Conscription.moraleAfter(c,amount):c.morale;
   troopsAfter=c.troops+amount;reserveAfter=c.recruitReserve-amount;governorAfter=op==Operation.APPOINT_GOVERNOR?targets[0]:governorBefore;
   barracksUsesBefore=op==Operation.RECRUIT?w.domestic.remainingUses(c.id,Domestic.Kind.BARRACKS):-1;barracksUsesAfter=barracksUsesBefore<0?-1:barracksUsesBefore-1;
   Set<Integer> ids=new LinkedHashSet<>();ids.add(actor.id);for(int id:targets)ids.add(id);if(op==Operation.APPOINT_GOVERNOR&&governorBefore>=0)ids.add(governorBefore);
   List<OfficerEffect> list=new ArrayList<>();for(int id:ids){World.Officer o=w.officer(id);if(o!=null)list.add(new OfficerEffect(w,op,actor,o,targets,governorBefore));}officers=Collections.unmodifiableList(list);
  }
 }
 public static final class OfficerEffect {
  public final int id,loyaltyBefore,loyaltyAfter,meritBefore,meritAfter,lastRewardTurnBefore,lastRewardTurnAfter,remainingTurnsBefore,remainingTurnsAfter;
  public final boolean actedBefore,actedAfter;
  public final String roleBefore,roleAfter;
  public final OfficerExperiencePlan experience;
  OfficerEffect(World w,Operation op,World.Officer actor,World.Officer o,int[] targets,int oldGovernor){
   id=o.id;boolean target=false;for(int t:targets)target|=t==id;
   boolean pendingSearch=op==Operation.SEARCH&&PcSearchPolicy.pendingPreview(w,w.city(actor.cityId),actor);
   boolean paidActor=id==actor.id&&w.cityActionCost(actor)>0&&!pendingSearch;
   loyaltyBefore=o.loyalty;loyaltyAfter=op==Operation.REWARD&&target?o.loyalty+Strategy.rewardGain(o):o.loyalty;
   int stat=id==actor.id&&!pendingSearch?OfficerExperiencePlan.cityStat(op):-1;
   experience=new OfficerExperiencePlan(w,o,stat,2);
   meritBefore=w.government.merit(id);meritAfter=paidActor?Math.min(1000000,meritBefore+OfficerExperiencePlan.merit(o,stat,w.government)):meritBefore;
   actedBefore=o.acted;actedAfter=o.acted||paidActor||op==Operation.APPOINT_GOVERNOR&&target;
   lastRewardTurnBefore=o.lastRewardTurn;lastRewardTurnAfter=op==Operation.REWARD&&target?w.turn:o.lastRewardTurn;
   roleBefore=o.role.name();Strategy.Role next=o.role;if(op==Operation.APPOINT_GOVERNOR){if(id==oldGovernor&&next==Strategy.Role.GOVERNOR)next=Strategy.Role.OFFICER;if(target&&next!=Strategy.Role.RULER&&next!=Strategy.Role.DISTRICT)next=Strategy.Role.GOVERNOR;}roleAfter=next.name();
   remainingTurnsBefore=w.strategy.officerState(o.id).remainingTurns;remainingTurnsAfter=remainingTurnsBefore;
  }
 }
 /** Conditional checks, not unconditional outcome odds. Never exposes hidden talent identity. */
 public static final class Search {
  public final int officerCheckChance,goldCheckChance,goldFoundMinimum,goldFoundMaximum;
  public final List<String> possibleOutcomeKinds=Collections.unmodifiableList(Arrays.asList("OFFICER","TREASURE","GOLD","NOTHING"));
  Search(World w,World.City c,World.Officer o,long postCostGold){int chance=w.strategy.searchChance(o.id);if(PcSearchPolicy.operative(w))try{chance=PcSearchPolicy.chance(w,c.id,o.id);}catch(java.io.IOException e){throw new IllegalStateException(e);}officerCheckChance=chance;goldCheckChance=10+o.politics/5;goldFoundMinimum=0;goldFoundMaximum=(int)Math.min(120,Math.max(0,w.campaign.goldCap(c)-postCostGold));}
 }
 CityActionPlan(World w,Operation op,World.City c,World.Officer o,int[] targets,RuleFailure failure){
  this.failure=failure;goldAvailable=c==null?0:c.gold;goldCost=w.cityActionCost(o)==0?0:Strategy.cityActionGold(op,targets);
  actionPointsAvailable=w.cityActionPoints(c);actionPointsCost=w.cityActionCost(o,w.strategy.cityActionBaseCost(op));goldRemaining=goldAvailable-goldCost;actionPointsRemaining=(long)actionPointsAvailable-actionPointsCost;
  effects=failure==null?new Effects(w,op,c,o,targets):null;search=failure==null&&op==Operation.SEARCH?new Search(w,c,o,goldRemaining):null;
 }
 public boolean allowed(){return failure==null;}
}
