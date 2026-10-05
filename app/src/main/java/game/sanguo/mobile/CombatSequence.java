package game.sanguo.mobile;

import game.sanguo.core.TurnJournal;
import java.util.*;
import java.util.function.Predicate;

/** Command presentation only. No World, session, RNG, command, persistence or completion callback. */
final class CombatSequence {
    static final int CAPACITY=256;
    private final List<TurnJournal.Event> events;
    private final CombatReplayLedger ledger;
    private final java.util.function.ToIntFunction<TurnJournal.Event> duration;
    private final java.util.function.ToIntFunction<TurnJournal.Event> prelude;
    private int cursor;
    private float fraction;
    private boolean paused;
    private int speed=1;
    CombatSequence(List<TurnJournal.Event> source,CombatReplayLedger ledger){
        this(source,ledger,TurnJournal.Event::durationMillis);
    }
    CombatSequence(List<TurnJournal.Event> source,CombatReplayLedger ledger,java.util.function.ToIntFunction<TurnJournal.Event> duration){
        this(source,ledger,duration,e->0);
    }
    CombatSequence(List<TurnJournal.Event> source,CombatReplayLedger ledger,java.util.function.ToIntFunction<TurnJournal.Event> duration,java.util.function.ToIntFunction<TurnJournal.Event> prelude){
        this.ledger=ledger;this.duration=duration;this.prelude=prelude;
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
    int speed(){return speed;}
    void skip(){for(;cursor<events.size();cursor++)ledger.finish(events.get(cursor));fraction=0;paused=false;}
    void advance(long elapsed,Predicate<TurnJournal.Event> visible){
        if(paused)return;
        while(!done()){
            var event=current();
            if(ledger.completed(event)||!event.visibleAction()||!visible.test(event)){
                ledger.finish(event);cursor++;fraction=0;continue;
            }
            int total=Math.max(1,duration.applyAsInt(event));
            // Original screen cues follow real elapsed visual time. Keep the
            // existing50ms action cap after the cue and on legacy/custom maps.
            double remaining=Math.max(0,prelude.applyAsInt(event)-fraction*total);
            double scaled=Math.max(0,elapsed)*(double)speed;
            double advance=Math.min(remaining,scaled)+Math.min(50.0*speed,Math.max(0,scaled-remaining));
            fraction=Math.min(1,fraction+(float)(advance/total));
            if(fraction<1)return;
            ledger.finish(event);cursor++;fraction=0;return;
        }
    }
}
