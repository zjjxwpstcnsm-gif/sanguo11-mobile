package game.sanguo.mobile;

import game.sanguo.api.*;

public final class MediaCueLedgerTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static GameEvent event(StateToken state,GameEvent.Kind kind){return new GameEvent(kind,state,1,1000,0,0,"untrusted text never used");}
    public static void main(String[] args){
        MediaCueLedger q=new MediaCueLedger(2);long large=9007199254740993L;
        StateToken initial=new StateToken("session",large,large);q.baseline(initial);
        check(!q.claim(initial,"snapshot-voice","missing"),"snapshot cannot manufacture an observed parent");
        var one=event(new StateToken("session",large,large+1),GameEvent.Kind.PATROLLED);q.observe(one);
        check(q.claim(one.state,"v1",one.id),"actual observed parent admits exact64-bit state");
        check(!q.claim(one.state,"v1",one.id),"duplicate voice identity rejected");
        check(!q.claim(new StateToken("session",large,large+2),"mismatch",one.id),"revision mismatch rejects");
        q.observe(one);check(!q.claim(one.state,"v1",one.id),"duplicate receipt never revokes dedup");
        check(q.claim(one.state,"v2",one.id),"second declared phase of same observed parent");
        check(!q.claim(one.state,"overflow",one.id)&&q.needsResync(),"overflow fail closed, no LRU replay");
        q.resynchronize(one.state);q.observe(one);check(!q.claim(one.state,"v1",one.id),"resync baseline suppresses old receipt and voice");
        var two=event(new StateToken("session",large,large+2),GameEvent.Kind.RECRUITED);q.observe(two);check(q.claim(two.state,"new",two.id),"fresh actual commit after resync");
        var restore=event(new StateToken("replacement",large+1,0),GameEvent.Kind.WORLD_REPLACED);check(q.observe(restore),"real replacement establishes new source epoch");
        check(!q.claim(two.state,"old-generation",two.id),"prior generation cannot replay");
        check(!q.claim(restore.state,"restore",restore.id),"restore receipt is not a voice-producing parent");
        q.baseline(two.state);check(!q.claim(two.state,"old-after-baseline",two.id),"stale baseline cannot rewind generation");
        var next=event(new StateToken("replacement",large+1,1),GameEvent.Kind.LEGACY_COMMITTED);q.observe(next);check(q.claim(next.state,"actual-next",next.id),"actual new epoch command admits");
        check(q.observe(event(next.state,GameEvent.Kind.CLOSED))&&q.closed(),"close drops transient membership");
        check(!q.claim(next.state,"after-close",next.id),"closed cannot play");
        System.out.println("PASS source media receipt ledger checks="+checks+"; exact long membership/dedup/snapshot/overflow/restore/close");
    }
}
