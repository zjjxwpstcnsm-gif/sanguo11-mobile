package game.sanguo.runtime.query;
import game.sanguo.api.*;
import game.sanguo.core.*;
import java.util.stream.Collectors;
public final class ProductionQuery {
 private ProductionQuery(){}
 public static ProductionPlan.Operation operation(String name){try{return ProductionPlan.Operation.valueOf(name);}catch(IllegalArgumentException|NullPointerException e){return null;}}
 public static World.Weapon weapon(ProductionCommand c){try{return "EQUIPMENT".equals(c.operation)?World.Weapon.valueOf(c.item):null;}catch(IllegalArgumentException|NullPointerException e){return null;}}
 public static Army.Ship ship(ProductionCommand c){try{return "SHIP".equals(c.operation)?Army.Ship.valueOf(c.item):null;}catch(IllegalArgumentException|NullPointerException e){return null;}}
 public static ProductionPreview capture(World w,StateToken state,ProductionCommand c){
  ProductionPlan p=w.previewProduction(c.cityId,c.officers(),operation(c.operation),weapon(c),ship(c));var f=p.effects;
  return new ProductionPreview(state,p.allowed()?CommandResult.Error.NONE:CommandResult.Error.RULE_REJECTED,p.allowed()?"NONE":p.failure.code,p.allowed()?"global":p.failure.field,p.allowed()?"":p.failure.detail,
   new ProductionPreview.Resources(p.goldAvailable,p.goldCost,p.actionPointsAvailable,p.actionPointsCost,p.stockBefore,p.stockCapacity,p.pendingBefore,p.facilityUsesBefore,p.facilityCapacity,p.facility),
   f==null?null:new ProductionPreview.Effects(f.delayed,f.actedBefore,f.actedAfter,f.goldAfter,f.actionPointsAfter,f.outputQuantity,f.stockAfterImmediate,f.pendingAfter,f.busyTurns,f.facilityUsesAfter,f.meritBefore,f.meritAfter,f.taskLabel,new OfficerExperienceChange(f.experience.managed,f.experience.stat,f.experience.requestedAmount,f.experience.experienceBefore,f.experience.experienceAfter,f.experience.currentBefore,f.experience.currentAfter),f.actors.stream().map(a->new ProductionPreview.ActorEffect(a.id,a.meritBefore,a.meritAfter,a.actedBefore,a.actedAfter,new OfficerExperienceChange(a.experience.managed,a.experience.stat,a.experience.requestedAmount,a.experience.experienceBefore,a.experience.experienceAfter,a.experience.currentBefore,a.experience.currentAfter))).collect(Collectors.toList())));
 }
}
