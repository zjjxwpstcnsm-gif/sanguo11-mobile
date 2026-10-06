package game.sanguo.api;

import java.util.*;

/** Facts from an already-applied journal checkpoint. Text is never an identity protocol. */
public final class AppliedEventSnapshot {
    public static final class EntityState {
        public final String key,type,status,category;public final SceneFactsSnapshot.Cell cell;
        public final int owner,troops,hp,energy,remaining,builderId,direction,power,level,upgradeTo;
        public final Integer maxHp;public final boolean complete,trap;
        public EntityState(String key,String type,String status,String category,SceneFactsSnapshot.Cell cell,int owner,int troops,int hp,int energy,int remaining,Integer maxHp,int builderId,int direction,int power,int level,int upgradeTo,boolean complete,boolean trap){
            this.category=category;
            this.key=key;this.type=type;this.status=status;this.cell=cell;this.owner=owner;this.troops=troops;this.hp=hp;this.energy=energy;this.remaining=remaining;this.maxHp=maxHp;this.builderId=builderId;this.direction=direction;this.power=power;this.level=level;this.upgradeTo=upgradeTo;this.complete=complete;this.trap=trap;
        }
    }
    public static final class Change {
        public final EntityState before,after;
        public Change(EntityState before,EntityState after){this.before=before;this.after=after;}
        /** True only when the authoritative checkpoint actually removed this entity. */
        public boolean removed(){return before!=null&&after==null;}
    }
    public static final class Plot {
        public final String kind,cause;public final int actorUnitId,targetUnitId,owner,actorOfficerId;
        public final SceneFactsSnapshot.Cell start,target;public final boolean success,critical;
        public Plot(String kind,String cause,int actorUnitId,int targetUnitId,int owner,int actorOfficerId,SceneFactsSnapshot.Cell start,SceneFactsSnapshot.Cell target,boolean success,boolean critical){this.kind=kind;this.cause=cause;this.actorUnitId=actorUnitId;this.targetUnitId=targetUnitId;this.owner=owner;this.actorOfficerId=actorOfficerId;this.start=start;this.target=target;this.success=success;this.critical=critical;}
    }
    public static final class PointWrite {
        public final long sequence;public final int owner,before,after,delta,cityId,officerId;
        public final String cause,phase,presentationParentId;
        public PointWrite(long sequence,int owner,int before,int after,int cityId,int officerId,String cause,String phase,String presentationParentId){this.sequence=sequence;this.owner=owner;this.before=before;this.after=after;this.delta=after-before;this.cityId=cityId;this.officerId=officerId;this.cause=cause;this.phase=phase;this.presentationParentId=presentationParentId;}
    }
    public final StateToken state;
    /** Original TurnJournal.Event.id, not a newly created event. */
    public final String id,kind,sourceKey,sourceType,label,message;
    /** Existing API commit ID supplied by the serial host; null when unavailable. */
    public final String committedParentId;
    /** No original speech caller/parent is currently recorded by this journal. */
    public final String originalParentId;public final Integer speakerOfficerId;
    public final long journalId,sequence;
    public final int actorUnitId,owner;public final Integer actorOfficerId,criticalOfficerId,criticalYear;
    public final SceneFactsSnapshot.Cell start,target;
    public final List<SceneFactsSnapshot.Cell> path;public final List<Change> changes;
    public final List<Plot> plots;public final List<PointWrite> pointWrites;public final List<String> unknown;
    public AppliedEventSnapshot(StateToken state,String id,long journalId,long sequence,String kind,String sourceKey,String sourceType,String committedParentId,int actorUnitId,int owner,Integer actorOfficerId,Integer criticalOfficerId,Integer criticalYear,SceneFactsSnapshot.Cell start,SceneFactsSnapshot.Cell target,String label,String message,List<SceneFactsSnapshot.Cell> path,List<Change> changes,List<Plot> plots,List<PointWrite> pointWrites){
        this.state=Objects.requireNonNull(state);this.id=id;this.journalId=journalId;this.sequence=sequence;this.kind=kind;this.sourceKey=sourceKey;this.sourceType=sourceType;this.committedParentId=committedParentId;this.actorUnitId=actorUnitId;this.owner=owner;this.actorOfficerId=actorOfficerId;this.criticalOfficerId=criticalOfficerId;this.criticalYear=criticalYear;this.start=start;this.target=target;this.label=label;this.message=message;this.path=List.copyOf(path);this.changes=List.copyOf(changes);this.plots=List.copyOf(plots);this.pointWrites=List.copyOf(pointWrites);this.originalParentId=null;this.speakerOfficerId=null;this.unknown=List.of("originalSpeechCaller","originalEventParent");
    }
}
