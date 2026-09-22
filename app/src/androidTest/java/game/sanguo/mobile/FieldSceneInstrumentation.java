package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.graphics.Bitmap;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;

/** Native production renderer, explicit stress fixtures and actual port commands. */
public final class FieldSceneInstrumentation extends SceneInstrumentation {
    private void activate(World w)throws Exception{
        runOnMainSync(()->{invoke("activateWorld",new Class<?>[]{World.class},w);activity.refresh();host.switchMode(true);host.focus(new Hex(18,18));});
        world=w;settle();ready();check(host.is3D(),"native field renderer active");
    }
    private void preview(String name)throws Exception{
        capture(name);surfaceCapture();
        Bitmap b=getUiAutomation().takeScreenshot();Bitmap small=Bitmap.createScaledBitmap(b,432,Math.max(1,b.getHeight()*432/b.getWidth()),true);
        ByteArrayOutputStream out=new ByteArrayOutputStream();small.compress(Bitmap.CompressFormat.PNG,100,out);
        // Bounded screenshot in test output permits inspection even when artifact download is unavailable.
        String encoded=android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);
        int chunks=(encoded.length()+2799)/2800;
        for(int i=0;i<chunks;i++)System.out.println("FIELD_PREVIEW "+name+" "+i+"/"+chunks+" "+encoded.substring(i*2800,Math.min(encoded.length(),(i+1)*2800)));
        small.recycle();b.recycle();
    }
    private void stress(int count,boolean forest,boolean facilities)throws Exception{
        World fixture=FieldSceneFixture.create(count,forest,facilities);byte[] before=SaveCodec.encode(fixture);activate(fixture);
        FilamentMapView view=(FilamentMapView)field(host,"spatial");
        for(float span:new float[]{7,15,28}){
            runOnMainSync(()->view.camera.span=span);settle();ready();
            long start=SystemClock.uptimeMillis();long frames=(Long)field(view,"renderedFrames");SystemClock.sleep(2000);
            long delta=(Long)field(view,"renderedFrames")-frames;
            android.os.Debug.MemoryInfo memory=new android.os.Debug.MemoryInfo();android.os.Debug.getMemoryInfo(memory);
            System.out.println("FIELD_STRESS units="+count+" forest="+forest+" facilities="+facilities+" span="+span+" rendered_submissions="+delta+" durationMs="+(SystemClock.uptimeMillis()-start)+" PSS_KB="+memory.getTotalPss()+" "+host.report().replace('\n',' '));
            check(delta>0,"native frames submitted at every LOD");
            if(span==7)preview("field-"+count+"-"+forest+"-"+facilities);
        }
        check(Arrays.equals(before,SaveCodec.encode(fixture)),"all LOD/animation samples preserve fixture resources and RNG");
        runOnMainSync(()->{fixture.units.clear();host.invalidateScene();host.setWorld(fixture,null,-1);});settle();
        Map<?,?> objects=(Map<?,?>)field(view,"objects");check(objects.keySet().stream().noneMatch(k->k.toString().startsWith("unit:")),"removed units leave no renderable or label");
    }
    private void lifecycle()throws Exception{
        World w=FieldLifecycleFixture.start();activate(w);
        FilamentMapView view=(FilamentMapView)field(host,"spatial");
        runOnMainSync(()->{host.focus(new Hex(13,14));view.camera.span=3;FieldLifecycleFixture.build(w);host.invalidateScene();host.setWorld(w,null,-1);});settle();preview("field-construction");
        runOnMainSync(()->{FieldLifecycleFixture.finish(w);FieldLifecycleFixture.upgrade(w);FieldLifecycleFixture.damage(w);host.invalidateScene();host.setWorld(w,null,-1);});settle();preview("field-upgrade-damage");
        runOnMainSync(()->{FieldLifecycleFixture.repair(w);FieldLifecycleFixture.destroy(w);host.invalidateScene();host.setWorld(w,null,-1);});settle();
        Map<?,?> remaining=(Map<?,?>)field(view,"objects");check(remaining.keySet().stream().noneMatch(k->k.toString().startsWith("structure:")),"real destruction removes native facility");
        runOnMainSync(()->{FieldLifecycleFixture.domesticBuild(w);host.invalidateScene();host.setWorld(w,null,-1);});settle();
        runOnMainSync(()->{FieldLifecycleFixture.domesticFinish(w);FieldLifecycleFixture.domesticDemolish(w);host.invalidateScene();host.setWorld(w,null,-1);});settle();
        check(((Map<?,?>)field(view,"objects")).keySet().stream().noneMatch(k->k.toString().startsWith("domestic:")),"real demolition removes native facility");
    }
    private void port()throws Exception{
        World w=FieldSceneFixture.port();activate(w);World.Unit u=w.units.get(0);World reference=SaveCodec.decode(SaveCodec.encode(w));World visual=SaveCodec.decode(SaveCodec.encode(w));
        TurnJournal journal=new TurnJournal(w);Hex target=new Hex(21,19);
        World.Result move=w.move(u.id,target);check(move.ok,"actual port move: "+move.message);journal.close();check(reference.move(u.id,target).ok,"reference port move");
        runOnMainSync(()->{host.invalidateScene();host.setWorld(visual,u.hex,u.id);host.focus(new Hex(19,19));});settle();
        for(TurnJournal.Event event:journal.events()){
            for(int frame=0;frame<=36;frame++){final float f=frame/36f;runOnMainSync(()->host.replayFrame(event,f));SystemClock.sleep(70);}
            event.applyVisual(visual);runOnMainSync(()->{host.replayFrame(null,0);host.invalidateScene();host.setWorld(visual,null,-1);});
        }
        check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(reference)),"port replay keeps exact authoritative result");preview("field-port-arrival");
        runOnMainSync(()->{host.invalidateScene();host.setWorld(w,null,-1);});
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
        stress(50,false,true);stress(100,false,false);stress(0,true,false);stress(50,true,true);lifecycle();port();
        runOnMainSync(()->host.switchMode(false));check(!host.is3D(),"2D fallback remains available");
        result.putString("stream","PASS S04 S05 installed "+checks+" checks\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL S04 S05 "+e+"\n"+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
