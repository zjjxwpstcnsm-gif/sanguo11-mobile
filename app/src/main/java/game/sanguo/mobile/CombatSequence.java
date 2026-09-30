package game.sanguo.mobile;

import game.sanguo.core.TurnJournal;
import java.util.*;
import java.util.function.Predicate;

/** Command presentation only. No World, session, RNG, command, persistence or completion callback. */
final class CombatSequence {
    static final int CAPACITY=256;
    private final List<TurnJournal.Event> events;
    private final CombatReplayLedger ledger;
    private int cursor;
    private float fraction;
    private boolean paused;
    private int speed=1;
    CombatSequence(List<TurnJournal.Event> source,CombatReplayLedger ledger){
        this.ledger=ledger;
        // Large deltas retain their battle report; skip all effects rather than tail-replay older IDs.
        if(source.size()>CAPACITY){for(var event:source)ledger.finish(event);events=Collections.emptyList();}
        else events=new ArrayList<>(source);
    }
    TurnJournal.Event current(){return cursor<events.size()?events.get(cursor):null;}
    float fraction(){return fraction;}
    boolean done(){return current()==null;}
    boolean paused(){return paused;}
    void pause(boolean value){paused=value;}
    void speed(int value){speed=value<=1?1:value<=2?2:4;}
    void skip(){for(;cursor<events.size();cursor++)ledger.finish(events.get(cursor));fraction=0;paused=false;}
    void advance(long elapsed,Predicate<TurnJournal.Event> visible){
        if(paused)return;
        while(!done()){
            var event=current();
            if(ledger.completed(event)||!event.visibleAction()||!visible.test(event)){
                ledger.finish(event);cursor++;fraction=0;continue;
            }
            fraction=Math.min(1,fraction+Math.max(0,Math.min(50,elapsed))*speed/(float)event.durationMillis());
            if(fraction<1)return;
            ledger.finish(event);cursor++;fraction=0;return;
        }
    }
}
