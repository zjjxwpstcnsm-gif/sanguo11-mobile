package game.sanguo.mobile.bridge;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import game.sanguo.api.*;
import game.sanguo.api.bridge.*;
import game.sanguo.runtime.bridge.BridgeSession;
import java.util.List;
import org.json.*;

/** JNI protocol/host-thread adapter. No Activity, View, mutable World or gameplay rules. */
public final class AndroidGameBridge {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final Object LOCK=new Object();
    private static BridgeSession channel;
    private static GameApi game;
    private static GameApi.Subscription lifecycle;
    private static long commandNanos,commandCount;
    private AndroidGameBridge(){}
    public static void bind(GameApi next){
        if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("UI logic thread required");
        synchronized(LOCK){
            if(lifecycle!=null){lifecycle.close();lifecycle=null;}
            if(channel!=null){channel.close();channel=null;}
            game=next;if(game==null)return;
            openChannel();
            lifecycle=game.subscribe(event->{
                synchronized(LOCK){
                    if(event.kind==GameEvent.Kind.WORLD_REPLACED){channel.close();openChannel();}
                    else if(event.kind==GameEvent.Kind.CLOSED)bind(null);
                }
            });
        }
    }
    private static void openChannel(){channel=new BridgeSession(game);channel.snapshot();}
    public static String sessionId(){synchronized(LOCK){return channel==null?null:channel.sessionId;}}
    /** Returning QUEUED is not an execution receipt. The client must poll its command result. */
    public static String request(String json){
        if(json==null||json.length()>4096)return "BAD_REQUEST";
        try{
            JSONObject data=new JSONObject(json);String id=data.getString("sessionId"),kind=data.getString("type");
            synchronized(LOCK){if(channel==null||!channel.sessionId.equals(id))return "STALE_SESSION";}
            if(!"snapshot".equals(kind)&&!"command".equals(kind))return "UNKNOWN_REQUEST";
            // Reject floating-point coercion (especially counters above 2^53) before posting.
            if("command".equals(kind)){exactLong(data,"clientSequence");exactLong(data,"revision");}
            MAIN.post(()->{
                synchronized(LOCK){
                    if(channel==null||!channel.sessionId.equals(id))return;
                    if("snapshot".equals(kind)){channel.snapshot();return;}
                    long started=SystemClock.elapsedRealtimeNanos();
                    try{channel.command(data.optString("commandId",null),exactLong(data,"clientSequence"),
                        exactLong(data,"revision"),data.optString("operation",""),data.optInt("cityId",-1),data.optInt("officerId",-1));}
                    catch(RuntimeException|JSONException ex){channel.reject(data.optString("commandId",null),"HOST_ERROR");}
                    commandNanos+=SystemClock.elapsedRealtimeNanos()-started;commandCount++;
                }
            });return "QUEUED";
        }catch(JSONException|NumberFormatException e){return "BAD_REQUEST";}
    }
    private static long exactLong(JSONObject data,String key)throws JSONException{
        Object value=data.get(key);
        if(value instanceof Integer||value instanceof Long)return ((Number)value).longValue();
        // Decimal strings are exact too; exponent/fraction/double values are not schema-1 integers.
        if(value instanceof String&&((String)value).matches("-?[0-9]+"))return Long.parseLong((String)value);
        throw new JSONException("Expected exact integer: "+key);
    }
    public static String poll(String id){
        synchronized(LOCK){
            if(channel==null||id==null||!channel.sessionId.equals(id))return "{\"status\":\"STALE_SESSION\",\"messages\":[]}";
            JSONArray list=new JSONArray();List<BridgeMessage> items=channel.drain();
            try{for(BridgeMessage message:items){
                JSONObject item=new JSONObject().put("type",message.type).put("sessionId",message.sessionId)
                    .put("schemaVersion",BridgeSession.SCHEMA).put("sequence",message.sequence).put("revision",message.revision)
                    .put("mapRevision",message.mapRevision).put("width",message.width).put("height",message.height)
                    .put("turn",message.turn).put("player",message.player);
                if(message.commandId!=null)item.put("commandId",message.commandId);
                if(message.error!=null)item.put("error",message.error);
                if(message.detail!=null)item.put("detail",message.detail);
                if(message.terrain!=null)item.put("terrain",message.terrain);
                JSONArray entities=new JSONArray();for(BridgeEntity e:message.entities)entities.put(new JSONObject()
                    .put("entityId",e.entityId).put("kind",e.kind).put("name",e.name).put("q",e.q).put("r",e.r)
                    .put("owner",e.owner).put("troops",e.troops).put("energy",e.energy).put("officerId",e.officerId)
                    .put("gold",e.gold).put("food",e.food).put("order",e.order));
                item.put("entities",entities).put("removed",new JSONArray(message.removed));list.put(item);
            }
            return new JSONObject().put("status","OK").put("messages",list).put("pending",channel.pendingCount())
                .put("dropped",channel.droppedCount()).put("commandCount",commandCount)
                .put("meanCommandMicros",commandCount==0?0:(commandNanos/commandCount)/1000).toString();
            }catch(JSONException e){throw new IllegalStateException("Bridge serialization",e);}
        }
    }
}
