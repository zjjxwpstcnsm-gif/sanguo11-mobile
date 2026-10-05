package game.sanguo.core;

import game.sanguo.api.*;
import game.sanguo.runtime.*;
import java.util.*;

/** Real command chains, discarded candidates, turn phases and save/RNG equivalence. */
public final class TechniquePointsOrderedFactsTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static World edit(World w,int points){
        var draft=w.editor.faction(w.player,w.actionPoints[w.player],points);
        check(draft.valid()&&w.editor.apply(draft).ok,"normal editor point command");return w;
    }
    public static void main(String[] args)throws Exception{
        World initial=PcTechniquePointsSessionTest.fixture();initial.campaign.points.put(0,250);
        try(GameSession session=new GameSession(initial)){
            List<GameEvent> events=new ArrayList<>();session.subscribe(events::add);
            byte[] before=session.captureSave();var token=session.state();
            var command=new ProductionCommand(token,"EQUIPMENT",10,1,"SPEAR");
            session.preview(command);session.snapshot();session.legacyView();
            check(events.isEmpty()&&Arrays.equals(before,session.captureSave()),"all reads and previews publish no facts or mutate save");
            World control=SaveCodec.decode(before);check(control.produce(10,1,World.Weapon.SPEAR).ok,"normal production control");
            var result=session.execute(command);check(result.ok()&&result.event.techniquePointsFacts.size()==1,"one actual native production write");
            var fact=result.event.techniquePointsFacts.get(0);
            check(fact.cause.equals("NATIVE_PRODUCTION")&&fact.phase.equals("COMMAND")&&fact.cityId==10&&fact.officerId==1,"actual producer context");
            check(fact.state.equals(result.state)&&fact.parentId.equals(result.event.id)&&fact.id.equals(result.event.id+":technique:1"),"full committed token and parent dedup identity");
            check(fact.before==250&&fact.after==control.campaign.points(0)&&fact.delta==fact.after-fact.before,"actual before/after");
            check(Arrays.equals(session.captureSave(),SaveCodec.encode(control)),"observer instrumentation preserves every production save/RNG byte");
            boolean immutable=false;try{result.event.techniquePointsFacts.clear();}catch(UnsupportedOperationException e){immutable=true;}
            check(immutable,"defensive immutable ordered fact collection");
            int count=events.size();check(!session.execute(command).ok()&&events.size()==count,"duplicate revision cannot publish facts");
            before=session.captureSave();var view=session.legacyView();World direct=SaveCodec.decode(before);int value=view.draft.campaign.points(0);
            edit(direct,value+20);edit(direct,value);
            check(session.legacy(view.draft,()->{edit(view.draft,value+20);return view.draft.editor.apply(view.draft.editor.faction(0,view.draft.actionPoints[0],value));}).ok,"normal zero-net command chain");
            var zero=events.get(events.size()-1);check(zero.techniquePointsChanges.isEmpty()&&zero.techniquePointsFacts.size()==2,"zero-net preserves both actual changes");
            check(zero.techniquePointsFacts.get(0).delta==20&&zero.techniquePointsFacts.get(1).delta==-20,"point writes remain ordered");
            for(int i=0;i<2;i++)check(zero.techniquePointsFacts.get(i).sequence==i+1&&zero.techniquePointsFacts.get(i).cause.equals("EDITOR_SET"),"explicit edit cause and fresh commit sequence");
            check(Arrays.equals(session.captureSave(),SaveCodec.encode(direct)),"zero-net metadata changes no rule/save semantics");
            var rejected=session.legacyView();count=events.size();before=session.captureSave();
            check(!session.legacy(rejected.draft,()->{edit(rejected.draft,value+10);return World.Result.rejected("discard");}).ok,"actual dirty candidate rejected");
            check(events.size()==count&&Arrays.equals(before,session.captureSave()),"rejected partial candidate facts cannot escape");
            var malformed=session.legacyView();count=events.size();before=session.captureSave();
            check(!session.legacy(malformed.draft,()->{malformed.draft.campaign.points.put(0,-1);malformed.draft.campaign.points.put(0,value);return malformed.draft.success("invalid transient write");}).ok,"invalid transient fact rejected before installation");
            check(events.size()==count&&Arrays.equals(before,session.captureSave()),"fact validation failure is fully atomic");
            var valid=session.legacyView();check(session.legacy(valid.draft,()->valid.draft.editor.apply(valid.draft.editor.faction(0,valid.draft.actionPoints[0],value+1))).ok,"following success");
            check(events.get(events.size()-1).techniquePointsFacts.size()==1&&events.get(events.size()-1).techniquePointsFacts.get(0).sequence==1,"rejection cannot leak into next commit");
            var ticket=session.beginTurn();World computed=SaveCodec.decode(ticket.initial());TurnJournal turnJournal=new TurnJournal(computed);check(computed.nextTurn().ok,"actual complete turn");turnJournal.close();
            check(session.commitTurn(ticket,computed),"computed turn committed");var turn=events.get(events.size()-1);
            check(!turn.techniquePointsFacts.isEmpty(),"full turn retains actual reward writes");
            Set<String> checkpointIds=new HashSet<>();for(var event:turnJournal.events())checkpointIds.add(event.id);
            long sequence=0;for(var f:turn.techniquePointsFacts){check(f.sequence==++sequence&&f.state.equals(turn.state),"turn fact order and committed token");check(!f.phase.equals("COMMAND"),"actual turn phase captured at write time");check(checkpointIds.contains(f.presentationParentId),"actual presentation checkpoint parent");}
            check(Arrays.equals(session.captureSave(),SaveCodec.encode(computed)),"full turn facts preserve complete save/RNG");
            ticket=session.beginTurn();World canceled=SaveCodec.decode(ticket.initial());canceled.nextTurn();count=events.size();session.cancelTurn(ticket);
            check(!session.commitTurn(ticket,canceled)&&events.size()==count,"canceled computed facts never publish");
            session.replace(initial);check(events.get(events.size()-1).techniquePointsFacts.isEmpty(),"restore adds no fake point write");
            check(fact.before==250&&fact.cause.equals("NATIVE_PRODUCTION"),"retained facts survive replacement unchanged");
            session.close();check(events.get(events.size()-1).techniquePointsFacts.isEmpty(),"close emits no point reward");
        }
        World w=SaveCodec.decode(SaveCodec.encode(initial));byte[] before=SaveCodec.encode(w);long rng=w.strategy.getRandomState();
        TurnJournal journal=new TurnJournal(w);int points=w.campaign.points(0);
        // Explicit raw writes share one checkpoint; real Editor commands above
        // intentionally have their own reports.prepare boundaries.
        w.campaign.points.put(0,points+20);w.campaign.points.put(0,points);journal.checkpoint("zero net actual writes");var batch=journal.drainEvents();
        check(batch.size()==1&&batch.get(0).visibleAction()&&batch.get(0).techniquePointsChanges.isEmpty()&&batch.get(0).techniquePointsFacts.size()==2,"journal retains zero-net point-only writes");
        check(batch.get(0).techniquePointsFacts.get(0).after==points+20&&batch.get(0).techniquePointsFacts.get(1).after==points,"journal exact ordered scalar chain");
        check(batch.get(0).techniquePointsFacts.get(0).presentationParentId.equals(batch.get(0).id),"point-only journal parent survives zero net");
        journal.checkpoint("unchanged");check(journal.drainEvents().isEmpty(),"journal cursor prevents replay duplication");
        World visual=SaveCodec.decode(before);byte[] after=SaveCodec.encode(w);batch.get(0).applyVisual(visual);
        check(Arrays.equals(after,SaveCodec.encode(w))&&rng==w.strategy.getRandomState(),"visual replay cannot change authority/RNG");journal.close();
        check(SaveCodec.decode(after).techniquePointsJournal.facts().isEmpty(),"transient point facts never round-trip through a save");
        World delayed=PcDelayedProductionSessionTest.fixture();delayed.pcTechniquePoints.initializeOpening();delayed.campaign.points.put(0,250);
        try(GameSession session=new GameSession(delayed)){
            var start=session.execute(new ProductionCommand(session.state(),"EQUIPMENT",10,new int[]{1,2,3},"RAM"));
            check(start.ok()&&start.event.techniquePointsFacts.isEmpty(),"delayed registration creates no reward write");
            int turns=SaveCodec.decode(session.captureSave()).officer(1).otherTaskTurns;
            int rewards=0;
            List<GameEvent> events=new ArrayList<>();session.subscribe(events::add);
            for(int tick=1;tick<=turns;tick++){
                var ticket=session.beginTurn();World computed=SaveCodec.decode(ticket.initial());
                check(computed.nextTurn().ok&&session.commitTurn(ticket,computed),"normal delayed full turn commit");
                check(Arrays.equals(session.captureSave(),SaveCodec.encode(computed)),"delayed full save/RNG exact");
                var event=events.get(events.size()-1);
                for(var fact:event.techniquePointsFacts)if(fact.cause.equals("NATIVE_MANUFACTURE")){
                    rewards++;check(tick==turns&&fact.cityId==10&&fact.officerId==1&&fact.delta==10&&fact.phase.equals("GLOBAL"),"only real completion carries exact native producer/phase/reward");
                }
            }
            check(rewards==1,"manufacture reward appears once across all real turns");
        }
        World cap=PcTechniquePointsSessionTest.fixture();cap.campaign.points.put(0,10000);
        try(GameSession session=new GameSession(cap)){
            var result=session.execute(new ProductionCommand(session.state(),"EQUIPMENT",10,1,"SPEAR"));
            check(result.ok()&&result.event.techniquePointsFacts.isEmpty(),"clipped zero gain is not invented as an actual change");
        }
        System.out.println("PASS TechniquePointsOrderedFactsTest checks="+checks+" native normal commands, zero-net, rejected/canceled candidates, phases, tokens, journal, full-save/RNG");
    }
}
