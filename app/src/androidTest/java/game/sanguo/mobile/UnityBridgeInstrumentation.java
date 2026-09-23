package game.sanguo.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import game.sanguo.api.*;
import game.sanguo.core.*;
import game.sanguo.runtime.GameSession;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.json.*;

/** Installed native host probe. This is NOT evidence of a Unity Player or C# JNI execution. */
public final class UnityBridgeInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle report=new Bundle();MainActivity activity=null;
        try{
            World direct=ScenarioCatalog.load("coalition-190",0,20260923L);
            World.City city=direct.home();int builder=direct.idle(city).get(0).id;
            require(direct.domestic.build(city.id,builder,Domestic.Kind.BARRACKS,direct.domestic.buildSites(city.id).get(0)).ok,"prepare barracks");
            for(int n=0;n<3;n++)require(direct.nextTurn().ok,"prepare complete turn");
            World world=SaveCodec.decode(SaveCodec.encode(direct));
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            MainActivity first=activity;
            runOnMainSync(()->{
                try{
                    Method activate=MainActivity.class.getDeclaredMethod("activateWorld",World.class);activate.setAccessible(true);
                    require((Boolean)activate.invoke(first,world),"production world install");
                    Method build=MainActivity.class.getDeclaredMethod("buildGameUi",Bundle.class,boolean.class,boolean.class);build.setAccessible(true);build.invoke(first,null,false,true);
                }catch(Exception e){throw new RuntimeException(e);}
            });
            NativeGameHost host=((GameApplication)activity.getApplication()).host();
            String session=UnityBridge.sessionId();
            require("QUEUED".equals(UnityBridge.request(new JSONObject().put("type","snapshot").put("sessionId",session).toString())),"snapshot request");
            JSONObject snap=waitFor(session,"snapshot");
            require(snap.getJSONArray("entities").length()>=world.cities.size()&&snap.getString("terrain").length()==world.width*world.height,"real map facts");
            int patrolOfficer=world.idle(world.home()).get(0).id;
            int recruitOfficer=world.idle(world.home()).get(1).id;
            byte[] before=capture(host);
            command(session,"bad",1,0,"patrol",city.id,-1);require(!waitFor(session,"receipt").isNull("error"),"invalid officer error receipt");
            require(Arrays.equals(before,capture(host)),"failed command atomicity");
            require(direct.patrol(city.id,patrolOfficer).ok,"old direct patrol");
            runOnMainSync(()->{
                try{Field f=MainActivity.class.getDeclaredField("world");f.setAccessible(true);first.executeCity((World)f.get(first),GameCommand.Operation.PATROL,city.id,patrolOfficer);}
                catch(Exception e){throw new RuntimeException(e);}
            });
            require(Arrays.equals(SaveCodec.encode(direct),capture(host)),"actual native command path full-state parity");
            require(direct.recruit(city.id,recruitOfficer).ok,"old direct recruit");
            command(session,"good",2,1,"recruit",city.id,recruitOfficer);JSONObject receipt=waitFor(session,"receipt");
            require(receipt.isNull("error")&&receipt.getLong("revision")==2,"bridge recruit success/version");
            require(Arrays.equals(SaveCodec.encode(direct),capture(host)),"actual JNI Java adapter full-state parity");
            command(session,"good",2,1,"recruit",city.id,recruitOfficer);require(waitFor(session,"receipt").isNull("error"),"retry receipt");
            require(Arrays.equals(SaveCodec.encode(direct),capture(host)),"no repeated execution");
            String inaccurate=new JSONObject().put("type","command").put("sessionId",session).put("clientSequence",1.5).put("revision",2).toString();
            require("BAD_REQUEST".equals(UnityBridge.request(inaccurate)),"fractional sequence rejected");
            AtomicReference<GameSession> original=new AtomicReference<>();runOnMainSync(()->original.set(host.session()));
            ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);
            runOnMainSync(first::recreate);
            Activity recreated=monitor.waitForActivityWithTimeout(20000);removeMonitor(monitor);
            require(recreated instanceof MainActivity,"Activity recreated");activity=(MainActivity)recreated;waitForIdleSync();
            runOnMainSync(()->require(host.session()==original.get(),"recreation retains identical session owner"));
            require(session.equals(UnityBridge.sessionId())&&Arrays.equals(SaveCodec.encode(direct),capture(host)),"recreation retains identity and complete game");
            MainActivity current=activity;
            runOnMainSync(()->{
                try{Field f=MainActivity.class.getDeclaredField("map");f.setAccessible(true);MapHost map=(MapHost)f.get(current);map.switchMode(true);map.switchMode(false);}
                catch(Exception e){throw new RuntimeException(e);}
            });
            require(Arrays.equals(SaveCodec.encode(direct),capture(host)),"presentation switch preserves rule state");
            runOnMainSync(()->{try{host.install(world);}catch(Exception e){throw new RuntimeException(e);}});
            require("STALE_SESSION".equals(UnityBridge.request(new JSONObject().put("type","snapshot").put("sessionId",session).toString())),"old session request rejected after load");
            report.putString("u01","PASS native Android Java entry, shared native/bridge rules, recreate and renderer switch; no Unity Player claim");
        }catch(Throwable error){report.putString("u01","FAIL "+error);android.util.Log.e("UnityBridgeProbe","failure",error);finish(Activity.RESULT_CANCELED,report);return;}
        finally{if(activity!=null){MainActivity closing=activity;runOnMainSync(closing::finish);}}
        finish(Activity.RESULT_OK,report);
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private byte[] capture(NativeGameHost host){AtomicReference<byte[]> bytes=new AtomicReference<>();runOnMainSync(()->{try{bytes.set(host.capture());}catch(Exception e){throw new RuntimeException(e);}});return bytes.get();}
    private void command(String session,String id,long seq,long revision,String operation,int city,int officer)throws JSONException{
        require("QUEUED".equals(UnityBridge.request(new JSONObject().put("type","command").put("sessionId",session)
            .put("commandId",id).put("clientSequence",seq).put("revision",revision).put("operation",operation).put("cityId",city).put("officerId",officer).toString())),"command queued");
    }
    private JSONObject waitFor(String session,String type)throws Exception{
        long deadline=android.os.SystemClock.uptimeMillis()+15000;
        while(android.os.SystemClock.uptimeMillis()<deadline){
            JSONArray messages=new JSONObject(UnityBridge.poll(session)).getJSONArray("messages");
            for(int i=0;i<messages.length();i++){JSONObject message=messages.getJSONObject(i);if(type.equals(message.getString("type")))return message;}
            Thread.sleep(40);
        }
        throw new AssertionError("no "+type+" within timeout");
    }
}
