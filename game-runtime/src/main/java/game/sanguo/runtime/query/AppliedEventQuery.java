package game.sanguo.runtime.query;

import game.sanguo.api.*;
import game.sanguo.core.*;
import java.util.*;

/** Copy only already-applied observations. Never resolves an action or constructs a journal. */
public final class AppliedEventQuery {
    private AppliedEventQuery(){}
    private static SceneFactsSnapshot.Cell cell(World w,Hex h){
        if(h==null)return null;var s=MapCoordinates.nationalSource(w,h);return new SceneFactsSnapshot.Cell(h.q,h.r,s.x,s.y);
    }
    private static AppliedEventSnapshot.EntityState state(World w,TurnJournal.State s){
        if(s==null)return null;
        return new AppliedEventSnapshot.EntityState(s.key,s.type,s.status,s.category,cell(w,s.hex),s.owner,s.troops,s.hp,s.energy,s.remaining,s.maxHp,s.builderId,s.direction,s.power,s.level,s.upgradeTo,s.complete,s.trap);
    }
    public static AppliedEventSnapshot capture(World w,StateToken token,GameEvent committedParent,TurnJournal.Event event){
        Objects.requireNonNull(event);
        if(committedParent!=null&&!committedParent.state.equals(token))throw new IllegalArgumentException("Parent commit uses a different StateToken");
        List<SceneFactsSnapshot.Cell> path=new ArrayList<>();for(Hex h:event.path)path.add(cell(w,h));
        List<AppliedEventSnapshot.Change> changes=new ArrayList<>();for(TurnJournal.StateChange change:event.states)changes.add(new AppliedEventSnapshot.Change(state(w,change.before),state(w,change.after)));
        List<AppliedEventSnapshot.Plot> plots=new ArrayList<>();for(TurnJournal.PlotOutcome p:event.plotOutcomes)plots.add(new AppliedEventSnapshot.Plot(p.plot.name(),p.cause.name(),p.actorId,p.targetId,p.owner,p.officerId,cell(w,p.start),cell(w,p.target),p.success,p.critical));
        List<AppliedEventSnapshot.PointWrite> writes=new ArrayList<>();for(TechniquePointsJournal.Fact f:event.techniquePointsFacts)writes.add(new AppliedEventSnapshot.PointWrite(f.sequence,f.owner,f.before,f.after,f.cityId,f.officerId,f.cause.name(),f.phase.name(),f.presentationParentId));
        World.Unit actor=event.actorCopy();Integer officer=actor==null?null:Integer.valueOf(actor.officerId);
        return new AppliedEventSnapshot(token,event.id,event.journalId,event.sequence,event.kind.name(),event.sourceKey,event.sourceType,committedParent==null?null:committedParent.id,event.actorId,event.owner,officer,event.critical==null?null:Integer.valueOf(event.critical.officerId),event.critical==null?null:Integer.valueOf(event.critical.year),cell(w,event.start),cell(w,event.target),event.label,event.message,path,changes,plots,writes);
    }
}
