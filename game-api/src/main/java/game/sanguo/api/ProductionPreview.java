package game.sanguo.api;
import java.util.*;
/** Immediate stock changes are separate from conditional future production output. */
public final class ProductionPreview {
 public final StateToken state;
 public final CommandResult.Error error;
 public final String reasonCode,field,detail;
 public final Resources resources;
 public final Effects effects;
 public ProductionPreview(StateToken state,CommandResult.Error error,String reasonCode,String field,String detail,Resources resources,Effects effects){this.state=state;this.error=error;this.reasonCode=reasonCode;this.field=field;this.detail=detail;this.resources=resources;this.effects=effects;}
 public boolean allowed(){return error==CommandResult.Error.NONE;}
 public static ProductionPreview unavailable(StateToken state,CommandResult.Error error){return new ProductionPreview(state,error,error.name(),"global",error.name(),null,null);}
 public static final class Resources {
  public final int goldAvailable,goldCost,actionPointsAvailable,actionPointsCost,stockBefore,stockCapacity,pendingBefore,facilityUsesBefore,facilityCapacity;
  public final String facility;
  public Resources(int goldAvailable,int goldCost,int actionPointsAvailable,int actionPointsCost,int stockBefore,int stockCapacity,int pendingBefore,int facilityUsesBefore,int facilityCapacity,String facility){this.goldAvailable=goldAvailable;this.goldCost=goldCost;this.actionPointsAvailable=actionPointsAvailable;this.actionPointsCost=actionPointsCost;this.stockBefore=stockBefore;this.stockCapacity=stockCapacity;this.pendingBefore=pendingBefore;this.facilityUsesBefore=facilityUsesBefore;this.facilityCapacity=facilityCapacity;this.facility=facility;}
 }
 public static final class Effects {
  public final boolean delayed,actedBefore,actedAfter;
  public final int goldAfter,actionPointsAfter,outputQuantity,stockAfterImmediate,pendingAfter,busyTurns,facilityUsesAfter,meritBefore,meritAfter;
  public final String taskLabel;
  public final OfficerExperienceChange experience;
  public final List<ActorEffect> actors;
  public final TechniquePointsChange techniquePoints;
  public final boolean nativeTechniquePoints;
  public Effects(boolean delayed,boolean actedBefore,boolean actedAfter,int goldAfter,int actionPointsAfter,int outputQuantity,int stockAfterImmediate,int pendingAfter,int busyTurns,int facilityUsesAfter,int meritBefore,int meritAfter,String taskLabel){this(delayed,actedBefore,actedAfter,goldAfter,actionPointsAfter,outputQuantity,stockAfterImmediate,pendingAfter,busyTurns,facilityUsesAfter,meritBefore,meritAfter,taskLabel,null);}
  public Effects(boolean delayed,boolean actedBefore,boolean actedAfter,int goldAfter,int actionPointsAfter,int outputQuantity,int stockAfterImmediate,int pendingAfter,int busyTurns,int facilityUsesAfter,int meritBefore,int meritAfter,String taskLabel,OfficerExperienceChange experience){this(delayed,actedBefore,actedAfter,goldAfter,actionPointsAfter,outputQuantity,stockAfterImmediate,pendingAfter,busyTurns,facilityUsesAfter,meritBefore,meritAfter,taskLabel,experience,List.of());}
  public Effects(boolean delayed,boolean actedBefore,boolean actedAfter,int goldAfter,int actionPointsAfter,int outputQuantity,int stockAfterImmediate,int pendingAfter,int busyTurns,int facilityUsesAfter,int meritBefore,int meritAfter,String taskLabel,OfficerExperienceChange experience,List<ActorEffect> actors){this(delayed,actedBefore,actedAfter,goldAfter,actionPointsAfter,outputQuantity,stockAfterImmediate,pendingAfter,busyTurns,facilityUsesAfter,meritBefore,meritAfter,taskLabel,experience,actors,null,false);}
  public Effects(boolean delayed,boolean actedBefore,boolean actedAfter,int goldAfter,int actionPointsAfter,int outputQuantity,int stockAfterImmediate,int pendingAfter,int busyTurns,int facilityUsesAfter,int meritBefore,int meritAfter,String taskLabel,OfficerExperienceChange experience,List<ActorEffect> actors,TechniquePointsChange points,boolean nativePoints){this.techniquePoints=points;this.nativeTechniquePoints=nativePoints;this.delayed=delayed;this.actedBefore=actedBefore;this.actedAfter=actedAfter;this.goldAfter=goldAfter;this.actionPointsAfter=actionPointsAfter;this.outputQuantity=outputQuantity;this.stockAfterImmediate=stockAfterImmediate;this.pendingAfter=pendingAfter;this.busyTurns=busyTurns;this.facilityUsesAfter=facilityUsesAfter;this.meritBefore=meritBefore;this.meritAfter=meritAfter;this.taskLabel=taskLabel;this.experience=experience;this.actors=Collections.unmodifiableList(new ArrayList<>(actors));}
 }
 public static final class ActorEffect {
  public final int officerId,meritBefore,meritAfter;public final boolean actedBefore,actedAfter;public final OfficerExperienceChange experience;
  public ActorEffect(int id,int before,int after,boolean actedBefore,boolean actedAfter,OfficerExperienceChange experience){officerId=id;meritBefore=before;meritAfter=after;this.actedBefore=actedBefore;this.actedAfter=actedAfter;this.experience=experience;}
 }
}
