package game.sanguo.mobile;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import game.sanguo.core.BridgeSession;
import game.sanguo.core.World;
import java.lang.ref.WeakReference;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Android JNI surface. All World reads and commands execute on the host UI/logic thread. */
public final class UnityBridge {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final Object LOCK=new Object();
    private static BridgeSession session;
    private static WeakReference<MainActivity> owner=new WeakReference<>(null);
    private static long commandNanos, commandCount;
    private UnityBridge(){}
    static void attach(MainActivity activity,World world){
        if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("UI thread required");
        synchronized(LOCK){if(session==null||session.world()!=world){session=new BridgeSession(world);session.snapshot();}owner=new WeakReference<>(activity);}
    }
    static void detach(MainActivity activity){synchronized(LOCK){if(owner.get()==activity){owner.clear();if(activity.isFinishing())session=null;}}}
    static World currentWorld(){synchronized(LOCK){return session==null?null:session.world();}}
    static String sessionId(){synchronized(LOCK){return session==null?null:session.sessionId;}}
    static void changed(World world){
        synchronized(LOCK){if(session!=null&&session.world()==world)session.changed();}
    }
    /** Called by UnityPlayer on its main thread; never calls Unity from an Android callback. */
    public static String request(String json){
        if(json==null||json.length()>4096)return "BAD_REQUEST";
        try{
            JSONObject data=new JSONObject(json);String id=data.getString("sessionId"),kind=data.getString("type");
            synchronized(LOCK){if(session==null||!session.sessionId.equals(id))return "STALE_SESSION";}
            if(!"snapshot".equals(kind)&&!"command".equals(kind))return "UNKNOWN_REQUEST";
            MAIN.post(()->{
                synchronized(LOCK){
                    MainActivity activity=owner.get();
                    if(session==null||!session.sessionId.equals(id)||activity==null||activity.isFinishing()||activity.isDestroyed())return;
                    if("snapshot".equals(kind)){session.snapshot();return;}
                    if(activity.bridgeBusy()){session.reject(data.optString("commandId",null),"HOST_BUSY");return;}
                    long started=SystemClock.elapsedRealtimeNanos();
                    BridgeSession.Message result;
                    try{result=session.command(data.optString("commandId",null),data.optLong("clientSequence",-1),
                        data.optLong("revision",-1),data.optString("operation",""),data.optInt("cityId",-1),data.optInt("officerId",-1));}
                    catch(RuntimeException ex){session.reject(data.optString("commandId",null),"HOST_ERROR");return;}
                    commandNanos+=SystemClock.elapsedRealtimeNanos()-started;commandCount++;
                    if(result.error==null)activity.bridgeCommandApplied();
                }
            });
            return "QUEUED";
        }catch(JSONException e){return "BAD_REQUEST";}
    }
    public static String poll(String id){
        synchronized(LOCK){
            if(session==null||id==null||!session.sessionId.equals(id))return "{\"status\":\"STALE_SESSION\",\"messages\":[]}";
            JSONArray list=new JSONArray();List<BridgeSession.Message> items=session.drain();
            try{for(BridgeSession.Message message:items){
                JSONObject item=new JSONObject().put("type",message.type).put("sessionId",message.sessionId)
                    .put("schemaVersion",BridgeSession.SCHEMA).put("sequence",message.sequence).put("revision",message.revision)
                    .put("mapRevision",message.mapRevision).put("width",message.width).put("height",message.height)
                    .put("turn",message.turn).put("player",message.player);
                if(message.commandId!=null)item.put("commandId",message.commandId);
                if(message.error!=null)item.put("error",message.error);
                if(message.detail!=null)item.put("detail",message.detail);
                if(message.terrain!=null)item.put("terrain",message.terrain);
                JSONArray entities=new JSONArray();for(BridgeSession.Entity e:message.entities)entities.put(new JSONObject()
                    .put("entityId",e.entityId).put("kind",e.kind).put("name",e.name).put("q",e.q).put("r",e.r)
                    .put("owner",e.owner).put("troops",e.troops).put("energy",e.energy).put("officerId",e.officerId)
                    .put("gold",e.gold).put("food",e.food).put("order",e.order));
                item.put("entities",entities).put("removed",new JSONArray(message.removed));list.put(item);
            }
            return new JSONObject().put("status","OK").put("messages",list).put("pending",session.pendingCount())
                .put("dropped",session.droppedCount()).put("commandCount",commandCount)
                .put("meanCommandMicros",commandCount==0?0:(commandNanos/commandCount)/1000).toString();
            }catch(JSONException e){throw new IllegalStateException("Bridge serialization",e);}
        }
    }
}
