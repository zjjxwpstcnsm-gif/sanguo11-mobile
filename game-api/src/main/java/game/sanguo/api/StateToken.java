package game.sanguo.api;

import java.util.Objects;

/** Identity of an authority commit, not a transport sequence or render frame. */
public final class StateToken {
    public final String sessionId;
    public final long generation, revision;
    public StateToken(String sessionId,long generation,long revision){
        this.sessionId=Objects.requireNonNull(sessionId);this.generation=generation;this.revision=revision;
    }
    @Override public boolean equals(Object other){
        if(!(other instanceof StateToken))return false;StateToken t=(StateToken)other;
        return sessionId.equals(t.sessionId)&&generation==t.generation&&revision==t.revision;
    }
    @Override public int hashCode(){return Objects.hash(sessionId,generation,revision);}
}
