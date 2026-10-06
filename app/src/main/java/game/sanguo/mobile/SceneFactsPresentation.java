package game.sanguo.mobile;

import game.sanguo.api.SceneFactsSnapshot;
import game.sanguo.api.StateToken;
import game.sanguo.core.World;

/** Accept only detached facts from this exact serial authority boundary. */
final class SceneFactsPresentation {
    private SceneFactsPresentation(){}
    static SceneFactsSnapshot accept(World layout,StateToken expected,SceneFactsSnapshot facts){
        if(facts==null)return null; // Explicit legacy preview/replay path, no authoritative claim.
        if(expected==null||!expected.equals(facts.state))throw new IllegalArgumentException("Scene facts StateToken differs");
        if(!facts.available)return null;
        if(layout==null||!layout.mapId.equals(facts.mapId)||layout.mapRevision!=facts.mapRevision
            ||layout.terrainRevision!=facts.terrainRevision||!layout.scenarioId.equals(facts.scenarioId)
            ||!layout.dataHash.equals(facts.dataHash)||layout.turn!=facts.turn||layout.player!=facts.player)
            throw new IllegalArgumentException("Scene facts and detached layout differ");
        return facts;
    }
}
