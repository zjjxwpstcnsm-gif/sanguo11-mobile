package game.sanguo.mobile;

import game.sanguo.api.GameEvent;
import game.sanguo.api.StateToken;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/** Read-only membership/dedup barrier for source playback. Overflow requires an explicit snapshot resync. */
final class MediaCueLedger {
    private final int capacity;
    private StateToken state;
    private long barrier;
    private boolean resync,closed;
    private final LinkedHashMap<String,StateToken> parents=new LinkedHashMap<>();
    private final LinkedHashSet<String> claimed=new LinkedHashSet<>();
    MediaCueLedger(int capacity){if(capacity<1)throw new IllegalArgumentException("capacity");this.capacity=capacity;}
    private boolean same(StateToken s){return state!=null&&state.sessionId.equals(s.sessionId)&&state.generation==s.generation;}
    private boolean stale(StateToken s){return state!=null&&(s.generation<state.generation||s.generation==state.generation&&(!state.sessionId.equals(s.sessionId)||s.revision<state.revision));}
    boolean baseline(StateToken s){
        if(s==null||stale(s))return false;
        boolean changed=!same(s);
        if(changed){parents.clear();claimed.clear();resync=false;closed=false;barrier=s.revision;}
        state=s;return changed;
    }
    void resynchronize(StateToken s){if(s==null||stale(s))return;baseline(s);parents.clear();claimed.clear();barrier=s.revision;resync=false;closed=false;}
    boolean observe(GameEvent event){
        if(event==null)return false;
        if(event.kind==GameEvent.Kind.WORLD_REPLACED){if(state!=null&&event.state.generation<=state.generation)return false;baseline(event.state);return true;}
        if(!same(event.state)||closed)return false;
        if(event.kind==GameEvent.Kind.CLOSED){if(event.state.revision<state.revision)return false;parents.clear();closed=true;return true;}
        if(event.state.revision<=barrier||event.state.revision<state.revision||parents.containsKey(event.id))return false;
        state=event.state;parents.put(event.id,event.state);
        while(parents.size()>capacity)parents.remove(parents.keySet().iterator().next());
        return false;
    }
    /** A receipt must have arrived through the actual subscribed session. Muted/background facts still claim their ID. */
    boolean claim(StateToken s,String id,String parent){
        if(s==null||id==null||id.isEmpty()||parent==null||parent.isEmpty()||!same(s)||closed||resync||!s.equals(parents.get(parent))||claimed.contains(id))return false;
        if(claimed.size()>=capacity){resync=true;parents.clear();return false;}
        claimed.add(id);return true;
    }
    boolean needsResync(){return resync;}
    void overflow(){resync=true;parents.clear();}
    boolean closed(){return closed;}
    void retire(){parents.clear();closed=true;}
}
