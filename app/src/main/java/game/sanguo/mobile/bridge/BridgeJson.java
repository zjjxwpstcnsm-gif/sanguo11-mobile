package game.sanguo.mobile.bridge;

import game.sanguo.api.StateToken;
import game.sanguo.api.TechniquePointsFact;
import game.sanguo.api.bridge.BridgeEntity;
import game.sanguo.api.bridge.BridgeMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Schema-1 additive wire adapter. Never converts counters through floating point. */
public final class BridgeJson {
    private BridgeJson() {}
    private static JSONObject token(StateToken state) throws JSONException {
        return new JSONObject().put("sessionId",state.sessionId).put("generation",state.generation)
            .put("revision",state.revision);
    }
    public static JSONObject message(BridgeMessage message) throws JSONException {
        JSONObject item=new JSONObject().put("type",message.type).put("sessionId",message.sessionId)
            .put("schemaVersion",1).put("sequence",message.sequence).put("revision",message.revision)
            .put("mapRevision",message.mapRevision).put("width",message.width).put("height",message.height)
            .put("turn",message.turn).put("player",message.player).put("state",token(message.state));
        if(message.commandId!=null)item.put("commandId",message.commandId);
        if(message.error!=null)item.put("error",message.error);
        if(message.detail!=null)item.put("detail",message.detail);
        if(message.terrain!=null)item.put("terrain",message.terrain);
        JSONArray entities=new JSONArray();
        for(BridgeEntity e:message.entities)entities.put(new JSONObject()
            .put("entityId",e.entityId).put("kind",e.kind).put("name",e.name).put("q",e.q).put("r",e.r)
            .put("owner",e.owner).put("troops",e.troops).put("energy",e.energy).put("officerId",e.officerId)
            .put("gold",e.gold).put("food",e.food).put("order",e.order));
        JSONArray facts=new JSONArray();
        for(TechniquePointsFact f:message.techniquePointsFacts)facts.put(new JSONObject()
            .put("id",f.id).put("parentId",f.parentId).put("presentationParentId",f.presentationParentId)
            .put("state",token(f.state)).put("sequence",f.sequence).put("owner",f.owner)
            .put("before",f.before).put("after",f.after).put("delta",f.delta)
            .put("cause",f.cause).put("phase",f.phase).put("cityId",f.cityId).put("officerId",f.officerId));
        return item.put("entities",entities).put("removed",new JSONArray(message.removed))
            .put("techniquePointsFacts",facts);
    }
}
