package game.sanguo.mobile;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import game.sanguo.core.*;
import java.lang.reflect.*;
import java.util.Arrays;

/** S09 recovery contract on the production host. Does not claim native process-death testing. */
public final class FinalSceneInstrumentation extends SceneInstrumentation {
    @Override public void onStart(){Bundle result=new Bundle();try{
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        world=ScenarioCatalog.all().get(0);
        runOnMainSync(()->{invoke("activateWorld",new Class<?>[]{World.class},world);activity.refresh();});
        host=(MapHost)field(activity,"map");
        byte[] before=SaveCodec.encode(world);
        runOnMainSync(()->host.switchMode(true));settle();ready();
        Bundle restore=new Bundle();runOnMainSync(()->host.saveCamera(restore));
        runOnMainSync(()->{try{
            Method fallback=MapHost.class.getDeclaredMethod("fallback",Throwable.class);fallback.setAccessible(true);
            fallback.invoke(host,new IllegalStateException("S09 injected renderer failure"));
        }catch(Exception e){throw new RuntimeException(e);}});
        check(!host.is3D(),"renderer failure returns to 2D");
        SharedPreferences prefs=getTargetContext().getSharedPreferences("map-renderer",0);
        check("IllegalStateException".equals(prefs.getString("lastFailure","")),"failure type persisted");
        runOnMainSync(()->host.restoreCamera(restore));check(!host.is3D(),"same host cannot automatically restart failed 3D");
        runOnMainSync(()->{
            MapHost second=new MapHost(activity,h->{});second.setWorld(world,null,-1);second.restoreCamera(restore);
            check(!second.is3D(),"new host cannot erase process recovery latch");second.release();
        });
        runOnMainSync(()->host.switchMode(true));settle();ready();check(host.is3D(),"explicit manual retry available");
        capture("s09-manual-recovery");surfaceCapture();
        runOnMainSync(()->host.switchMode(false));
        check(!prefs.getBoolean("nativeSession",true),"normal release clears native marker");
        check(Arrays.equals(before,SaveCodec.encode(world)),"recovery leaves full strategic state unchanged");
        result.putString("stream","PASS S09 "+checks+" installed recovery checks; injected Java failure, not native crash or process death\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL S09 "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
