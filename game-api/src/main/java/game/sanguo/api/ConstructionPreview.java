package game.sanguo.api;

/** Immutable current-engine forecast; turns, durability and effects still await native calibration. */
public final class ConstructionPreview {
    public final StateToken state;
    public final CommandResult.Error error;
    public final String reasonCode,field,detail;
    public final Resources resources;
    /** Only present when allowed. Completion is conditional on no cancellation/capture/destruction. */
    public final Completion completion;
    public ConstructionPreview(StateToken state,CommandResult.Error error,String reasonCode,String field,String detail,Resources resources,Completion completion){
        this.state=state;this.error=error;this.reasonCode=reasonCode;this.field=field;this.detail=detail;this.resources=resources;this.completion=completion;
    }
    public boolean allowed(){return error==CommandResult.Error.NONE;}
    public static ConstructionPreview unavailable(StateToken state,CommandResult.Error error){return new ConstructionPreview(state,error,error.name(),"global",error.name(),null,null);}
    public static final class Resources {
        public final int goldAvailable,goldCost,actionPointsAvailable,actionPointsCost;
        public final long goldRemaining,actionPointsRemaining;
        public Resources(int goldAvailable,int goldCost,long goldRemaining,int actionPointsAvailable,int actionPointsCost,long actionPointsRemaining){
            this.goldAvailable=goldAvailable;this.goldCost=goldCost;this.goldRemaining=goldRemaining;
            this.actionPointsAvailable=actionPointsAvailable;this.actionPointsCost=actionPointsCost;this.actionPointsRemaining=actionPointsRemaining;
        }
    }
    public static final class Completion {
        public final int turns,level,initialDurability,maximumDurability;
        public final String label,effect;
        public Completion(int turns,int level,int initialDurability,int maximumDurability,String label,String effect){
            this.turns=turns;this.level=level;this.initialDurability=initialDurability;this.maximumDurability=maximumDurability;this.label=label;this.effect=effect;
        }
    }
}
