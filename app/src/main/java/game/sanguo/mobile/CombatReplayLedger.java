package game.sanguo.mobile;

import game.sanguo.core.TurnJournal;
import java.util.TreeMap;

/** Retained per-session completion watermarks. Bounded, not a rule event bus or a save field. */
final class CombatReplayLedger {
    static final int CAPACITY=32;
    private final TreeMap<Long,Long> completed=new TreeMap<>();
    private long expiredThrough;
    boolean completed(TurnJournal.Event event){return event!=null&&(event.journalId<=expiredThrough||completed.getOrDefault(event.journalId,0L)>=event.sequence);}
    void finish(TurnJournal.Event event){
        if(event==null||event.journalId<=expiredThrough)return;
        completed.merge(event.journalId,event.sequence,Math::max);
        while(completed.size()>CAPACITY)expiredThrough=Math.max(expiredThrough,completed.pollFirstEntry().getKey());
    }
    int size(){return completed.size();}
    void clear(){completed.clear();expiredThrough=0;}
}
