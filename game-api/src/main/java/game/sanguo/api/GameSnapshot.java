package game.sanguo.api;

import java.util.List;
import game.sanguo.api.bridge.BridgeEntity;

/** Detached game facts. No selection, meshes, materials, camera or mutable rule entity. */
public final class GameSnapshot {
    public final StateToken state;
    public final int width,height,mapRevision,terrainRevision,turn,player;
    public final String terrain;
    public final GridLayout layout;
    public final List<BridgeEntity> entities;
    public GameSnapshot(StateToken state,int width,int height,int mapRevision,int terrainRevision,int turn,int player,
                        String terrain,GridLayout layout,List<BridgeEntity> entities){
        this.state=state;this.width=width;this.height=height;this.mapRevision=mapRevision;this.terrainRevision=terrainRevision;
        this.turn=turn;this.player=player;this.terrain=terrain;this.layout=layout;this.entities=List.copyOf(entities);
    }
}
