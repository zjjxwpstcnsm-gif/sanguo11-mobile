package game.sanguo.api.bridge;

import java.util.List;

/** Immutable schema-1 transport message. Long counters are never converted to floating point. */
public final class BridgeMessage {
    public final String type, sessionId, commandId, error, detail, terrain;
    public final long sequence, revision;
    public final int mapRevision, width, height, turn, player;
    public final List<BridgeEntity> entities;
    public final List<String> removed;
    public BridgeMessage(String type,String sessionId,long sequence,long revision,int mapRevision,int width,int height,int turn,
            int player,String commandId,String error,String detail,String terrain,List<BridgeEntity> entities,List<String> removed){
        this.type=type;this.sessionId=sessionId;this.sequence=sequence;this.revision=revision;
        this.mapRevision=mapRevision;this.width=width;this.height=height;this.turn=turn;this.player=player;
        this.commandId=commandId;this.error=error;this.detail=detail;this.terrain=terrain;
        this.entities=List.copyOf(entities);this.removed=List.copyOf(removed);
    }
}
