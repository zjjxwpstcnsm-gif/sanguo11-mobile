package game.sanguo.api;

import java.util.*;

/** Immutable query result. Rules are current engine behavior, not a claim of verified PC parity. */
public final class DeploymentPreview {
    public final StateToken state;
    public final CommandResult.Error error;
    public final String reasonCode,field,detail;
    public final Limits limits;
    public final Resources stock,reserve,cost,remaining;
    public final boolean reserveEnforced;
    public final int actionPointsAvailable,actionPointsCost;
    public final UnitFacts unit;
    public DeploymentPreview(StateToken state,CommandResult.Error error,String reasonCode,String field,String detail,Limits limits,
            Resources stock,Resources reserve,Resources cost,Resources remaining,boolean reserveEnforced,int actionPointsAvailable,int actionPointsCost,UnitFacts unit){
        this.state=state;this.error=error;this.reasonCode=reasonCode;this.field=field;this.detail=detail;this.limits=limits;
        this.stock=stock;this.reserve=reserve;this.cost=cost;this.remaining=remaining;this.reserveEnforced=reserveEnforced;
        this.actionPointsAvailable=actionPointsAvailable;this.actionPointsCost=actionPointsCost;this.unit=unit;
    }
    public boolean allowed(){return error==CommandResult.Error.NONE;}
    public static DeploymentPreview unavailable(StateToken state,CommandResult.Error error){
        return new DeploymentPreview(state,error,error.name(),"global",error.name(),null,null,null,null,null,false,0,0,null);
    }
    /** Empty intervals have max < min. Other constraints (crew, ships, exit) still apply. */
    public static final class Limits {
        public final int commandLimit,troopsMin,troopsMax,foodMin,foodMax,goldMin,goldMax;
        public Limits(int commandLimit,int troopsMin,int troopsMax,int foodMin,int foodMax,int goldMin,int goldMax){
            this.commandLimit=commandLimit;this.troopsMin=troopsMin;this.troopsMax=troopsMax;this.foodMin=foodMin;this.foodMax=foodMax;this.goldMin=goldMin;this.goldMax=goldMax;
        }
    }
    /** Equipment/ships refer to the selected types. BOAT is innate and has zero stock/cost. */
    public static final class Resources {
        public final long troops,food,gold,equipment,ships;
        public Resources(long troops,long food,long gold,long equipment,long ships){this.troops=troops;this.food=food;this.gold=gold;this.equipment=equipment;this.ships=ships;}
    }
    /** Present only when allowed. Food estimates hold troop count, position and supply aura fixed. */
    public static final class UnitFacts {
        public final int exitQ,exitR,movementBudget,movementSpent,movementRemaining,aptitude,leadership,war,intelligence,baseAttack,baseDefense,foodUse,foodTurns;
        public final double attackRating,defenseRating;
        public final int energy,attackRange;
        public final String equipmentLabel;
        public final List<SkillFact> skills;
        public final List<TacticFact> tactics;
        public UnitFacts(int exitQ,int exitR,int movementBudget,int movementSpent,int movementRemaining,int aptitude,int leadership,int war,int intelligence,
                int baseAttack,int baseDefense,double attackRating,double defenseRating,int foodUse,int foodTurns){
            this(exitQ,exitR,movementBudget,movementSpent,movementRemaining,aptitude,leadership,war,intelligence,baseAttack,baseDefense,attackRating,defenseRating,foodUse,foodTurns,0,0,"",Collections.emptyList(),Collections.emptyList());
        }
        public UnitFacts(int exitQ,int exitR,int movementBudget,int movementSpent,int movementRemaining,int aptitude,int leadership,int war,int intelligence,
                int baseAttack,int baseDefense,double attackRating,double defenseRating,int foodUse,int foodTurns,int energy,int attackRange,String equipmentLabel,List<SkillFact> skills,List<TacticFact> tactics){
            this.energy=energy;this.attackRange=attackRange;this.equipmentLabel=equipmentLabel;
            this.skills=Collections.unmodifiableList(new ArrayList<>(skills));this.tactics=Collections.unmodifiableList(new ArrayList<>(tactics));
            this.exitQ=exitQ;this.exitR=exitR;this.movementBudget=movementBudget;this.movementSpent=movementSpent;this.movementRemaining=movementRemaining;
            this.aptitude=aptitude;this.leadership=leadership;this.war=war;this.intelligence=intelligence;this.baseAttack=baseAttack;this.baseDefense=baseDefense;
            this.attackRating=attackRating;this.defenseRating=defenseRating;this.foodUse=foodUse;this.foodTurns=foodTurns;
        }
    }
    public static final class SkillFact {
        public final String id,label,description;
        public SkillFact(String id,String label,String description){this.id=id;this.label=label;this.description=description;}
    }
    /** A null formationError does not authorize a target; target legality is checked by the real command. */
    public static final class TacticFact {
        public final String group,code,label,description,formationError;
        public final int energy,rank,minRange,maxRange;
        public TacticFact(String group,String code,String label,String description,int energy,int rank,int minRange,int maxRange,String formationError){
            this.group=group;this.code=code;this.label=label;this.description=description;this.energy=energy;this.rank=rank;this.minRange=minRange;this.maxRange=maxRange;this.formationError=formationError;
        }
    }
}
