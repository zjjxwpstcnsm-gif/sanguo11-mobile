package game.sanguo.mobile;

import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.*;
import java.util.*;

/** Real authority regression: force a cavalry duel, progress and settle without unlocking normal commands. */
public final class ContestSession125Test {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(GameSession s,World w)throws Exception{check(Arrays.equals(s.captureSave(),SaveCodec.encode(w)),"entire authoritative state/RNG equal headless");}
    static World force()throws Exception{
        World w=DisplacementFixture.create("mountain",World.Weapon.CAVALRY);w.unit(2).status=War.Status.NORMAL;w.unit(2).statusTurns=0;
        w.contests.configure(1,new Contests.Profile(Debate.Temper.RASH,0,0));w.strategy.setSeed(29016L+55L*982451653L);
        check(w.war.tactic(1,2,War.Tactic.BREAKTHROUGH).ok&&w.contests.busy(),"actual blocked cavalry command reached duel");return w;
    }
    public static void main(String[] args)throws Exception{
        World reference=force();GameSession s=new GameSession(reference);Contests.Session c=reference.contests.current();int id=c.id(),rev=c.revision();final int forceId=id,forceRevision=rev;
        byte[] before=s.captureSave();StateToken token=s.state();int[] called={0};LegacyView draft=s.legacyView();
        check(!s.legacy(draft.draft,()->{called[0]++;return draft.draft.contests.concede(forceId,forceRevision);}).ok&&called[0]==0,"ordinary callback still rejected before execution");
        check(s.execute(new GameCommand(GameCommand.Operation.RECRUIT,token,10,10)).error==CommandResult.Error.HOST_BUSY,"ordinary typed city command remains blocked");
        check(Arrays.equals(before,s.captureSave())&&token.equals(s.state()),"busy rejections have no costs/RNG/revision effects");
        check(s.execute(ContestCommand.card(token,id,rev,0)).error==CommandResult.Error.RULE_REJECTED,"wrong contest type rejected");
        check(s.execute(ContestCommand.duel(token,id,rev,"bad","EXCHANGE",-1)).error==CommandResult.Error.RULE_REJECTED,"unknown stance rejected");
        check(s.execute(ContestCommand.duel(token,id,rev,"ATTACK","bad",-1)).error==CommandResult.Error.RULE_REJECTED,"unknown move rejected");
        check(s.execute(ContestCommand.concede(token,id+1,rev)).error==CommandResult.Error.RULE_REJECTED,"wrong contest ID rejected");
        check(s.execute(ContestCommand.concede(token,id,rev+1)).error==CommandResult.Error.RULE_REJECTED,"wrong contest revision rejected");same(s,reference);
        check(reference.contests.duelMove(id,rev,Duel.Stance.ATTACK,Duel.Move.EXCHANGE,-1).ok,"headless exchange");
        CommandResult result=s.execute(ContestCommand.duel(token,id,rev,"ATTACK","EXCHANGE",-1));check(result.ok()&&result.event.kind==GameEvent.Kind.CONTEST_ADVANCED&&s.state().revision==token.revision+1,"typed exchange exactly one committed fact");same(s,reference);
        before=s.captureSave();check(s.execute(ContestCommand.concede(token,id,rev)).error==CommandResult.Error.STALE_REVISION,"repeated stale state token rejected");check(Arrays.equals(before,s.captureSave()),"stale rejection atomic");
        c=reference.contests.current();int current=c.revision();check(reference.contests.concede(id,current).ok,"headless concede");
        StateToken settle=s.state();check(s.execute(ContestCommand.concede(settle,id,current)).ok(),"typed forced-duel settlement");same(s,reference);
        before=s.captureSave();check(!s.execute(ContestCommand.concede(s.state(),id,current)).ok()&&Arrays.equals(before,s.captureSave()),"cannot settle twice");
        s.replace(force());check(s.execute(ContestCommand.concede(settle,id,current)).error==CommandResult.Error.STALE_SESSION,"restore invalidates prior token");
        byte[] saved=s.captureSave();GameSession loaded=new GameSession(SaveCodec.decode(saved));World copy=SaveCodec.decode(saved);c=copy.contests.current();
        int restoredId=c.id(),restoredRevision=c.revision();check(copy.contests.concede(restoredId,restoredRevision).ok,"restored headless settlement");check(loaded.execute(ContestCommand.concede(loaded.state(),restoredId,restoredRevision)).ok(),"restored typed settlement");same(loaded,copy);
        s.close();check(s.execute(ContestCommand.concede(s.state(),id,0)).error==CommandResult.Error.CLOSED,"closed session rejected");loaded.close();
        GameSession turn=new GameSession(DisplacementFixture.create("mountain",World.Weapon.CAVALRY));TurnTicket ticket=turn.beginTurn();before=turn.captureSave();
        check(turn.execute(ContestCommand.concede(turn.state(),1,0)).error==CommandResult.Error.HOST_BUSY&&Arrays.equals(before,turn.captureSave()),"contest command cannot cross turn ticket");turn.cancelTurn(ticket);turn.close();
        // Existing debate operations use the same closed command set and unchanged core rules.
        World debate=DisplacementFixture.create("plain",World.Weapon.CAVALRY);World.Officer free=new World.Officer(30,"在野辩士",-1,10,70,70,70,70,70);debate.officers.add(free);
        check(debate.contests.persuade(10,10,30).ok,"real debate setup");GameSession ds=new GameSession(debate);c=debate.contests.current();id=c.id();rev=c.revision();
        int card=-1;for(int i=0;i<debate.contests.current().debate().speaker(0).hand().size();i++)if(debate.contests.current().debate().cardError(i)==null){card=i;break;}
        check(card>=0&&debate.contests.debateCard(id,rev,card).ok,"headless debate card");check(ds.execute(ContestCommand.card(ds.state(),id,rev,card)).ok(),"typed debate card");same(ds,debate);
        c=debate.contests.current();id=c.id();rev=c.revision();check(debate.contests.concede(id,rev).ok,"headless debate concede");check(ds.execute(ContestCommand.concede(ds.state(),id,rev)).ok(),"typed debate settlement");same(ds,debate);ds.close();
        System.out.println("PASS CONTEST_SESSION125 checks="+checks+" typed authority, forced UI settlement, state/contest tokens, save reload and normal-command guards");
    }
}
