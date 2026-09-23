package game.sanguo.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import game.sanguo.core.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.json.*;

/** Native Android host/JNI entry probe. Requires a Unity-bearing APK for an actual C# → JNI run. */
public final class UnityBridgeInstrumentation extends Instrumentation {
    @Override public void onStart(){
        Bundle report=new Bundle();MainActivity activity=null;
        try{
            World world=ScenarioCatalog.load("coalition-190",0,20260923L);
            World direct=ScenarioCatalog.load("coalition-190",0,20260923L);
            Intent intent=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity=(MainActivity)startActivitySync(intent);
            MainActivity host=activity;
            runOnMainSync(()->{
                try{
                    Field field=MainActivity.class.getDeclaredField("world");field.setAccessible(true);field.set(host,world);
                    Method build=MainActivity.class.getDeclaredMethod("buildGameUi",Bundle.class,boolean.class,boolean.class);
                    build.setAccessible(true);build.invoke(host,null,false,true);
                }catch(Exception e){throw new RuntimeException(e);}
            });
            String session=UnityBridge.sessionId();
            if(session==null||!UnityBridge.request(new JSONObject().put("type","snapshot").put("sessionId",session).toString()).equals("QUEUED"))
                throw new AssertionError("snapshot request");
            JSONObject snap=waitFor(session,"snapshot");
            if(snap.getJSONArray("entities").length()<world.cities.size()||snap.getString("terrain").length()!=world.width*world.height)
                throw new AssertionError("actual scenario snapshot");
            World.City city=null;World.Officer officer=null;
            for(World.City candidate:world.cities)if(candidate.owner==world.active&&candidate.order<100&&!world.idle(candidate).isEmpty()){
                city=candidate;officer=world.idle(candidate).get(0);break;
            }
            if(city==null)throw new AssertionError("patrol candidate");
            byte[] before=SaveCodec.encode(world);
            command(session,"bad",1,0,city.id,-1);
            JSONObject bad=waitFor(session,"receipt");
            if(bad.isNull("error")||!Arrays.equals(before,SaveCodec.encode(world)))throw new AssertionError("bad command changed state");
            if(!direct.patrol(city.id,officer.id).ok)throw new AssertionError("direct command");
            command(session,"good",2,0,city.id,officer.id);
            JSONObject good=waitFor(session,"receipt");
            if(!good.isNull("error")||!Arrays.equals(SaveCodec.encode(direct),SaveCodec.encode(world)))
                throw new AssertionError("Android host command did not match direct Java rules");
            report.putString("u01","PASS native Android Java entry; real Unity JNI still pending Player run");
        }catch(Throwable error){report.putString("u01","FAIL "+error);android.util.Log.e("UnityBridgeProbe","failure",error);finish(Activity.RESULT_CANCELED,report);return;}
        finally{if(activity!=null){MainActivity closing=activity;runOnMainSync(closing::finish);}}
        finish(Activity.RESULT_OK,report);
    }
    private void command(String session,String id,long seq,long revision,int city,int officer)throws JSONException{
        String result=UnityBridge.request(new JSONObject().put("type","command").put("sessionId",session)
            .put("commandId",id).put("clientSequence",seq).put("revision",revision)
            .put("operation","patrol").put("cityId",city).put("officerId",officer).toString());
        if(!"QUEUED".equals(result))throw new AssertionError(result);
    }
    private JSONObject waitFor(String session,String type)throws Exception{
        long deadline=android.os.SystemClock.uptimeMillis()+15000;
        while(android.os.SystemClock.uptimeMillis()<deadline){
            JSONObject batch=new JSONObject(UnityBridge.poll(session));
            JSONArray messages=batch.getJSONArray("messages");for(int i=0;i<messages.length();i++){
                JSONObject message=messages.getJSONObject(i);if(type.equals(message.getString("type")))return message;
            }
            Thread.sleep(40);
        }
        throw new AssertionError("no "+type+" within timeout");
    }
}
