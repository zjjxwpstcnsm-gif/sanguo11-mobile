package game.sanguo.mobile;

import game.sanguo.api.SceneFactsSnapshot;
import game.sanguo.api.StateToken;

/** Accept only detached facts from this exact serial authority boundary. */
final class SceneFactsPresentation {
    private SceneFactsPresentation(){}
    static SceneFactsSnapshot accept(String mapId,long mapRevision,long terrainRevision,String scenarioId,String dataHash,int turn,int player,StateToken expected,SceneFactsSnapshot facts){
        if(facts==null)return null; // Explicit legacy preview/replay path, no authoritative claim.
        if(expected==null||!expected.equals(facts.state))throw new IllegalArgumentException("Scene facts StateToken differs");
        if(!facts.available)return null;
        if(mapId==null||!mapId.equals(facts.mapId)||mapRevision!=facts.mapRevision
            ||terrainRevision!=facts.terrainRevision||!scenarioId.equals(facts.scenarioId)
            ||!dataHash.equals(facts.dataHash)||turn!=facts.turn||player!=facts.player)
            throw new IllegalArgumentException("Scene facts and detached layout differ");
        return facts;
    }
}
