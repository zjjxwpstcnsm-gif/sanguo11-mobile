package game.sanguo.mobile;

import game.sanguo.api.GameEvent;
import game.sanguo.api.StateToken;
import game.sanguo.api.TechniquePointsFact;
import game.sanguo.core.SaveCodec;
import game.sanguo.core.ScenarioCatalog;
import game.sanguo.runtime.GameSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Real committed zero-net facts plus transport/lifecycle adversaries; no Android or fake sound assertion. */
public final class TechniqueFactQueueTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static StateToken token(long generation,long revision){return new StateToken("fixture",generation,revision);}
    private static GameEvent batch(long generation,long revision,String... phases){
        var state=token(generation,revision);var kind=GameEvent.Kind.LEGACY_COMMITTED;
        String parent=state.sessionId+":"+generation+":"+revision+":"+kind;
        List<TechniquePointsFact> facts=new ArrayList<>();
        for(int i=0;i<phases.length;i++)facts.add(new TechniquePointsFact(parent,state,i+1,0,100+i,101+i,"EDITOR","COMMIT",-1,-1,phases[i]));
        return new GameEvent(kind,state,-1,-1,0,0,"unused",List.of(),facts);
    }
    public static void main(String[] args)throws Exception {
        try(GameSession session=new GameSession(ScenarioCatalog.load("coalition-190",0,20260923L))){
            var queue=new TechniqueFactQueue();var view=session.legacyView();int side=view.draft.player,points=view.draft.campaign.points(side);
            queue.baseline(session.state(),side);queue.foreground(true);List<GameEvent> events=new ArrayList<>();
            try(var subscription=session.subscribe(events::add)){
                var control=SaveCodec.decode(session.captureSave());
                check(control.editor.apply(control.editor.faction(side,control.actionPoints[side],points+20)).ok,"control gain");
                check(control.editor.apply(control.editor.faction(side,control.actionPoints[side],points)).ok,"control loss");
                check(session.legacy(view.draft,()->{
                    var gain=view.draft.editor.apply(view.draft.editor.faction(side,view.draft.actionPoints[side],points+20));
                    return gain.ok?view.draft.editor.apply(view.draft.editor.faction(side,view.draft.actionPoints[side],points)):gain;
                }).ok,"actual command commit");
                check(events.size()==1&&events.get(0).techniquePointsChanges.isEmpty(),"actual net-zero event");
                byte[] saved=session.captureSave();var event=events.get(0);
                check(queue.committed(event,side)==TechniqueFactQueue.Result.ACCEPTED,"actual accepted");
                queue.baseline(session.state(),side);
                var first=queue.poll();var second=queue.poll();
                check(first!=null&&first.delta==20&&second!=null&&second.delta==-20,"two opposite facts survive refresh");
                check(first.parentId.equals(event.id)&&first.id.equals(event.techniquePointsFacts.get(0).id),"exact authoritative identity");
                check(queue.poll()==null&&queue.committed(event,side)==TechniqueFactQueue.Result.IGNORED,"no duplicate");
                check(Arrays.equals(saved,session.captureSave())&&Arrays.equals(saved,SaveCodec.encode(control)),"complete Save/RNG unchanged by media");
                var bad=session.legacyView();check(!session.legacy(bad.draft,()->bad.draft.patrol(-1,-1)).ok&&events.size()==1,"failed command publishes no media");
                check(Arrays.equals(saved,session.captureSave()),"failure full Save/RNG unchanged");
                StateToken previous=session.state();session.replace(control);GameEvent restore=events.get(events.size()-1);
                check(!previous.sessionId.equals(restore.state.sessionId)&&restore.state.generation>previous.generation&&restore.state.revision==0,"actual restore changes sessionId and resets revision");
                check(queue.committed(restore,side)==TechniqueFactQueue.Result.RESET&&queue.size()==0,"actual changed-id restore queue reset");
                check(queue.committed(event,side)==TechniqueFactQueue.Result.IGNORED,"actual old token after restore silent");
                StateToken closing=session.state();session.close();GameEvent closed=events.get(events.size()-1);
                check(closing.equals(closed.state)&&queue.committed(closed,side)==TechniqueFactQueue.Result.RESET,"actual close at same revision clears media");
                check(queue.committed(closed,side)==TechniqueFactQueue.Result.IGNORED,"duplicate close ignored");
            }
        }
        var queue=new TechniqueFactQueue(3);queue.baseline(token(1,0),0);queue.foreground(true);
        check(queue.committed(batch(1,1,"journal:a","journal:b",""),0)==TechniqueFactQueue.Result.ACCEPTED,"phase batch");
        queue.releasePresentation("journal:b");check(queue.poll()==null,"later phase cannot overtake earlier");
        queue.releasePresentation("wrong");check(queue.poll()==null,"unrelated phase silent");
        queue.releasePresentation("journal:a");var first=queue.poll();
        check(first.presentationParentId.equals("journal:a"),"first correct phase");
        queue.releasePresentation("journal:a");check(queue.poll().presentationParentId.equals("journal:b"),"second exact phase once");
        check(queue.poll().presentationParentId.isEmpty()&&queue.poll()==null,"ordinary committed fact follows phases");
        var pauseQueue=new TechniqueFactQueue();pauseQueue.baseline(token(1,0),0);pauseQueue.foreground(true);
        pauseQueue.paused(true);check(pauseQueue.committed(batch(1,1,"paused-phase"),0)==TechniqueFactQueue.Result.ACCEPTED,"paused commit retained");
        pauseQueue.releasePresentation("paused-phase");check(pauseQueue.poll()==null,"paused phase silent");
        pauseQueue.paused(false);check(pauseQueue.poll()!=null&&pauseQueue.poll()==null,"resume once");
        var lateQueue=new TechniqueFactQueue(2);lateQueue.baseline(token(1,0),0);lateQueue.foreground(true);
        lateQueue.skipPresentation("finished-before-commit");lateQueue.committed(batch(1,1,"finished-before-commit",""),0);
        check(lateQueue.poll().sequence==2&&lateQueue.poll()==null,"late committed fact at discarded phase neither blocks nor replays");
        lateQueue.skipPresentation("a");lateQueue.skipPresentation("b");check(lateQueue.needsResync(),"phase history overflow explicit");
        lateQueue.resynchronize(token(1,1),0);lateQueue.baseline(token(2,0),0);lateQueue.committed(batch(2,1,"finished-before-commit"),0);
        lateQueue.releasePresentation("finished-before-commit");check(lateQueue.poll()!=null,"new generation clears old skipped phases");
        queue.committed(batch(1,2,"skipped",""),0);
        check(queue.pendingPresentation("skipped")&&!queue.pendingPresentation("unrelated")&&!queue.pendingPresentation(""),"only exact queued phase is pending");
        queue.skipPresentation("skipped");check(!queue.pendingPresentation("skipped"),"discarded queued phase no longer pending");
        check(queue.poll().sequence==2&&queue.poll()==null,"skip suppresses only its transient sound");
        queue.committed(batch(1,3,"paused"),0);queue.foreground(false);queue.releasePresentation("paused");queue.foreground(true);
        check(queue.poll()==null&&queue.committed(batch(1,3,"paused"),0)==TechniqueFactQueue.Result.IGNORED,"pause discards without replay");
        queue.foreground(false);check(queue.committed(batch(1,4,""),0)==TechniqueFactQueue.Result.ACCEPTED,"background commit consumed");
        queue.foreground(true);check(queue.poll()==null,"foreground cannot resurrect background");
        queue.committed(batch(1,5,"wait","wait","wait"),0);
        check(queue.committed(batch(1,6,""),0)==TechniqueFactQueue.Result.RESYNC&&queue.size()==0&&queue.needsResync(),"overflow explicit resync");
        queue.baseline(token(1,6),0);check(queue.needsResync(),"ordinary refresh cannot conceal overflow");
        queue.resynchronize(token(1,6),0);check(!queue.needsResync()&&queue.committed(batch(1,6,""),0)==TechniqueFactQueue.Result.IGNORED,"snapshot recovery no replay");
        var malformed=batch(1,7,"");var other=batch(1,8,"");
        var mixed=new GameEvent(malformed.kind,malformed.state,-1,-1,0,0,"unused",List.of(),List.of(malformed.techniquePointsFacts.get(0),other.techniquePointsFacts.get(0)));
        check(queue.committed(mixed,0)==TechniqueFactQueue.Result.RESYNC&&queue.poll()==null,"malformed batch atomic rejection");
        queue.resynchronize(token(2,0),0);check(queue.committed(batch(1,99,""),0)==TechniqueFactQueue.Result.IGNORED,"old generation cannot resurrect");
        queue.baseline(token(1,100),0);check(queue.committed(batch(2,1,""),0)==TechniqueFactQueue.Result.ACCEPTED,"stale snapshot cannot regress generation");
        check(queue.poll()!=null,"new generation legitimate fact");
        queue.committed(batch(2,2,"defer"),0);
        var restored=new GameEvent(GameEvent.Kind.WORLD_REPLACED,token(3,0),-1,-1,0,0,"restore");
        check(queue.committed(restored,0)==TechniqueFactQueue.Result.RESET&&queue.size()==0,"restore clears transient media");
        check(queue.committed(batch(2,99,""),0)==TechniqueFactQueue.Result.IGNORED,"restore old receipt silent");
        var ownerQueue=new TechniqueFactQueue();ownerQueue.baseline(token(1,0),1);ownerQueue.foreground(true);
        ownerQueue.committed(batch(1,1,""),1);check(ownerQueue.poll()==null,"other owner silent");
        ownerQueue.close();check(ownerQueue.committed(batch(1,2,""),1)==TechniqueFactQueue.Result.IGNORED,"closed silent");
        long exact=9007199254740993L;var precise=new TechniqueFactQueue();precise.baseline(token(exact,exact),0);precise.foreground(true);
        check(precise.committed(batch(exact,exact+1,""),0)==TechniqueFactQueue.Result.ACCEPTED&&precise.poll().state.revision==exact+1,"long identity exact beyond 2^53");
        System.out.println("Technique fact queue PASS checks="+checks+"; actual zero-net commit and full Save/RNG; no installed PCM claim");
    }
}
