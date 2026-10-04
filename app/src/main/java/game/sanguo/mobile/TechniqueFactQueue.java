package game.sanguo.mobile;

import game.sanguo.api.GameEvent;
import game.sanguo.api.StateToken;
import game.sanguo.api.TechniquePointsFact;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Serial presentation queue. Never reads World, runs commands, or reconstructs a missing fact. */
final class TechniqueFactQueue {
    enum Result { ACCEPTED, IGNORED, RESET, RESYNC }
    private static final class Pending {
        final TechniquePointsFact fact;
        boolean ready;
        Pending(TechniquePointsFact fact){this.fact=fact;ready=fact.presentationParentId.isEmpty();}
    }
    private final int capacity;
    private final ArrayDeque<Pending> pending=new ArrayDeque<>();
    private StateToken state;
    private int owner=-1;
    private long watermark;
    private boolean foreground,resync;
    TechniqueFactQueue(){this(4096);}
    TechniqueFactQueue(int capacity){if(capacity<1)throw new IllegalArgumentException("capacity");this.capacity=capacity;}
    private boolean identity(StateToken next,int side){return state!=null&&state.sessionId.equals(next.sessionId)&&state.generation==next.generation&&owner==side;}
    private boolean stale(StateToken next){return state!=null&&state.sessionId.equals(next.sessionId)
        &&(next.generation<state.generation||next.generation==state.generation&&next.revision<state.revision);}
    /** A snapshot sets the barrier but cannot replay facts. Same-identity refresh preserves pending phases. */
    void baseline(StateToken next,int side){
        if(stale(next))return;
        if(!identity(next,side)){pending.clear();watermark=next.revision;resync=false;}
        else watermark=Math.max(watermark,next.revision);
        state=next;owner=side;
    }
    /** Explicit snapshot recovery after overflow; retains the watermark so the failed batch stays silent. */
    void resynchronize(StateToken next,int side){if(stale(next))return;baseline(next,side);pending.clear();resync=false;}
    void foreground(boolean value){foreground=value;if(!value)pending.clear();}
    void discard(){pending.clear();}
    void close(){pending.clear();state=null;owner=-1;foreground=false;resync=false;}
    boolean needsResync(){return resync;}
    int size(){return pending.size();}
    private Result invalid(){pending.clear();resync=true;return Result.RESYNC;}
    /** Atomic batch admission: one malformed row rejects every row, including already pending media. */
    Result committed(GameEvent event,int side){
        if(state==null||!state.sessionId.equals(event.state.sessionId)||event.state.generation<state.generation)return Result.IGNORED;
        if(event.kind==GameEvent.Kind.WORLD_REPLACED||event.kind==GameEvent.Kind.CLOSED){
            if(event.state.generation==state.generation&&event.state.revision<=watermark)return Result.IGNORED;
            baseline(event.state,side);pending.clear();return Result.RESET;
        }
        if(!identity(event.state,side))return Result.IGNORED;
        if(event.state.revision<=watermark)return Result.IGNORED;
        watermark=event.state.revision;state=event.state;
        if(resync)return Result.RESYNC;
        long sequence=0;List<TechniquePointsFact> selected=new ArrayList<>();
        for(TechniquePointsFact fact:event.techniquePointsFacts){
            if(fact==null||!event.state.equals(fact.state)||!event.id.equals(fact.parentId)
                ||!fact.id.equals(fact.parentId+":technique:"+fact.sequence)||fact.sequence<=sequence
                ||fact.before<0||fact.after<0||fact.before==fact.after
                ||(long)fact.after-fact.before!=fact.delta)return invalid();
            sequence=fact.sequence;if(fact.owner==side)selected.add(fact);
        }
        // Background commits are consumed silently; returning to foreground never replays them.
        if(!foreground)return Result.ACCEPTED;
        if(selected.size()>capacity-pending.size())return invalid();
        for(TechniquePointsFact fact:selected)pending.addLast(new Pending(fact));
        return Result.ACCEPTED;
    }
    /** Only the committed renderer's exact journal ID opens its phase. */
    void releasePresentation(String parent){
        if(parent==null||parent.isEmpty())return;
        for(Pending item:pending)if(item.fact.presentationParentId.equals(parent))item.ready=true;
    }
    /** Skipping a phase discards transient media; it does not manufacture success or play deferred audio. */
    void skipPresentation(String parent){if(parent!=null&&!parent.isEmpty())pending.removeIf(item->item.fact.presentationParentId.equals(parent));}
    TechniquePointsFact poll(){
        if(!foreground||resync||pending.isEmpty()||!pending.peekFirst().ready)return null;
        return pending.removeFirst().fact;
    }
}
