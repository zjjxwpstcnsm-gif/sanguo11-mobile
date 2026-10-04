package game.sanguo.api.bridge;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import game.sanguo.api.StateToken;
import game.sanguo.api.TechniquePointsFact;

/** Immutable schema-1 transport message. Long counters are never converted to floating point. */
public final class BridgeMessage {
    public final String type, sessionId, commandId, error, detail, terrain;
    public final long sequence, revision;
    public final int mapRevision, width, height, turn, player;
    public final List<BridgeEntity> entities;
    public final List<String> removed;
    /** Additive Java boundary; legacy wire serializers require explicit hookup. */
    public final StateToken state;
    public final List<TechniquePointsFact> techniquePointsFacts;
    public BridgeMessage(String type,String sessionId,long sequence,long revision,int mapRevision,int width,int height,int turn,
            int player,String commandId,String error,String detail,String terrain,List<BridgeEntity> entities,List<String> removed){
        this(type,sessionId,sequence,revision,mapRevision,width,height,turn,player,commandId,error,detail,terrain,entities,removed,
            new StateToken(sessionId,0,revision),Collections.emptyList());
    }
    public BridgeMessage(String type,String sessionId,long sequence,long revision,int mapRevision,int width,int height,int turn,
            int player,String commandId,String error,String detail,String terrain,List<BridgeEntity> entities,List<String> removed,
            StateToken state,List<TechniquePointsFact> techniquePointsFacts){
        if(!sessionId.equals(state.sessionId)||revision!=state.revision)throw new IllegalArgumentException("Bridge state mismatch");
        for(TechniquePointsFact fact:techniquePointsFacts)if(!state.equals(fact.state))throw new IllegalArgumentException("Fact state mismatch");
        this.type=type;this.sessionId=sessionId;this.sequence=sequence;this.revision=revision;
        this.mapRevision=mapRevision;this.width=width;this.height=height;this.turn=turn;this.player=player;
        this.commandId=commandId;this.error=error;this.detail=detail;this.terrain=terrain;
        this.entities=List.copyOf(entities);this.removed=List.copyOf(removed);
        this.state=state;this.techniquePointsFacts=Collections.unmodifiableList(new ArrayList<>(techniquePointsFacts));
    }
}
