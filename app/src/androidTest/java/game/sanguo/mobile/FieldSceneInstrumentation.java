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
        capture(name);
        Bitmap b=getUiAutomation().takeScreenshot();Bitmap small=Bitmap.createScaledBitmap(b,432,Math.max(1,b.getHeight()*432/b.getWidth()),true);
        ByteArrayOutputStream out=new ByteArrayOutputStream();small.compress(Bitmap.CompressFormat.PNG,100,out);
        // Bounded screenshot in test output permits inspection even when artifact download is unavailable.
        String encoded=android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);
        int chunks=(encoded.length()+2799)/2800;
        for(int i=0;i<chunks;i++)System.out.println("FIELD_PREVIEW "+name+" "+i+"/"+chunks+" "+encoded.substring(i*2800,Math.min(encoded.length(),(i+1)*2800)));
        small.recycle();b.recycle();surfaceCapture();
    }
    private void stress(int count,boolean forest,boolean facilities)throws Exception{
        android.util.Log.i("SceneAcceptance","BEGIN field units="+count+" forest="+forest+" facilities="+facilities);
        // Use the same loaded-save normalization for the renderer and command reference.
        World fixture=SaveCodec.decode(SaveCodec.encode(FieldSceneFixture.create(count,forest,facilities)));byte[] before=SaveCodec.encode(fixture);activate(fixture);
        FilamentMapView view=(FilamentMapView)field(host,"spatial");
        for(float span:new float[]{7,15,28}){
            runOnMainSync(()->view.camera.span=span);settle();ready();
            runOnMainSync(view::resetMetrics);long start=SystemClock.uptimeMillis();long frames=(Long)field(view,"renderedFrames");SystemClock.sleep(2000);
            long delta=(Long)field(view,"renderedFrames")-frames;
            android.os.Debug.MemoryInfo memory=new android.os.Debug.MemoryInfo();android.os.Debug.getMemoryInfo(memory);
            String[] metrics={""};runOnMainSync(()->metrics[0]=host.report());
            System.out.println("FIELD_STRESS units="+count+" forest="+forest+" facilities="+facilities+" span="+span+" rendered_submissions="+delta+" durationMs="+(SystemClock.uptimeMillis()-start)+" PSS_KB="+memory.getTotalPss()+" "+metrics[0].replace('\n',' '));
            check(delta>0,"native frames submitted at every LOD");
            if(span==7)preview("field-"+count+"-"+forest+"-"+facilities);
        }
        check(Arrays.equals(before,SaveCodec.encode(fixture)),"all LOD/animation samples preserve fixture resources and RNG");
        if(count==50&&!forest)independentActions(fixture,view);
        runOnMainSync(()->{fixture.units.clear();host.invalidateScene();host.setWorld(fixture,null,-1);});settle();
        Map<?,?> objects=(Map<?,?>)field(view,"objects");check(objects.keySet().stream().noneMatch(k->k.toString().startsWith("unit:")),"removed units leave no renderable or label");
    }
    private void independentActions(World w,FilamentMapView view)throws Exception{
        World reference=SaveCodec.decode(SaveCodec.encode(w)),visual=SaveCodec.decode(SaveCodec.encode(w));
        TurnJournal log=new TurnJournal(w);check(w.move(1,new Hex(11,14)).ok,"stress actor legal movement");log.close();check(reference.move(1,new Hex(11,14)).ok,"same command reference");
        runOnMainSync(()->{host.invalidateScene();host.setWorld(visual,null,-1);host.focus(new Hex(18,18));view.camera.span=15;});settle();
        boolean compared=false;
        for(TurnJournal.Event event:log.events()){
            if(event.kind==TurnJournal.Kind.MOVE){
                runOnMainSync(()->host.replayFrame(event,.4f));settle();
                runOnMainSync(()->{try{
                    Map<?,?> proxies=(Map<?,?>)field(view,"objects");UnitAnimation actor=(UnitAnimation)field(proxies.get("unit:1"),"animation"),other=(UnitAnimation)field(proxies.get("unit:10"),"animation");
                    check("walk".equals(actor.clip)&&"idle".equals(other.clip),"two actual SPEAR formations simultaneously walk and idle");
                }catch(Exception e){throw new RuntimeException(e);}});compared=true;preview("field-independent-actions");
            }
            event.applyVisual(visual);runOnMainSync(()->{host.replayFrame(null,0);host.invalidateScene();host.setWorld(visual,null,-1);});
        }
        check(compared&&Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(reference)),"independent playback preserves full authoritative state");
        runOnMainSync(()->{host.invalidateScene();host.setWorld(w,null,-1);});
    }
    private void lifecycle()throws Exception{
        android.util.Log.i("SceneAcceptance","BEGIN facility lifecycle");
        World w=FieldLifecycleFixture.start();activate(w);
        FilamentMapView view=(FilamentMapView)field(host,"spatial");
        runOnMainSync(()->{host.focus(new Hex(13,14));view.camera.span=3;FieldLifecycleFixture.build(w);host.invalidateScene();host.setWorld(w,null,-1);});settle();preview("field-construction");
        runOnMainSync(()->{
            try{MapSceneSnapshot snap=(MapSceneSnapshot)field(view,"snapshot");Hex tile=new Hex(13,14);
                float x=view.camera.screenX(snap.ground.grid.x(tile)),y=view.camera.screenY(snap.ground.grid.z(tile),snap.ground.surface.at(tile)+.15f);
                long t=SystemClock.uptimeMillis();android.view.MotionEvent down=android.view.MotionEvent.obtain(t,t,0,x,y,0),up=android.view.MotionEvent.obtain(t,t+40,1,x,y,0);
                view.onTouchEvent(down);view.onTouchEvent(up);down.recycle();up.recycle();
            }catch(Exception e){throw new RuntimeException(e);}
        });settle();check(new Hex(13,14).equals(field(activity,"selected")),"tap real construction model selects exact facility tile");
        runOnMainSync(()->{FieldLifecycleFixture.finish(w);FieldLifecycleFixture.upgrade(w);FieldLifecycleFixture.damage(w);host.invalidateScene();host.setWorld(w,null,-1);});settle();preview("field-upgrade-damage");
        runOnMainSync(()->{FieldLifecycleFixture.repair(w);FieldLifecycleFixture.destroy(w);host.invalidateScene();host.setWorld(w,null,-1);});settle();
        Map<?,?> remaining=(Map<?,?>)field(view,"objects");check(remaining.keySet().stream().noneMatch(k->k.toString().startsWith("structure:")),"real destruction removes native facility");
        runOnMainSync(()->{FieldLifecycleFixture.domesticBuild(w);host.invalidateScene();host.setWorld(w,null,-1);});settle();
        runOnMainSync(()->{FieldLifecycleFixture.domesticFinish(w);FieldLifecycleFixture.domesticDemolish(w);host.invalidateScene();host.setWorld(w,null,-1);});settle();
        check(((Map<?,?>)field(view,"objects")).keySet().stream().noneMatch(k->k.toString().startsWith("domestic:")),"real demolition removes native facility");
    }
    private void march()throws Exception{
        android.util.Log.i("SceneAcceptance","BEGIN automatic march/convoy");
        World w=SaveCodec.decode(SaveCodec.encode(FieldSceneFixture.march()));activate(w);
        World reference=SaveCodec.decode(SaveCodec.encode(w));FieldSceneFixture.depart(w);FieldSceneFixture.depart(reference);
        int turns=0;boolean entered=false;
        while((!w.units.isEmpty()||!w.domestic.missions.isEmpty())&&turns++<20){
            World visual=SaveCodec.decode(SaveCodec.encode(w));TurnJournal journal=new TurnJournal(w);
            check(w.nextTurn().ok,"automatic march turn");journal.close();check(reference.nextTurn().ok,"reference march turn");
            runOnMainSync(()->{host.invalidateScene();host.setWorld(visual,null,-1);});
            for(TurnJournal.Event event:journal.events()){
                entered|=event.kind==TurnJournal.Kind.ENTER;
                if(event.kind==TurnJournal.Kind.MOVE||event.kind==TurnJournal.Kind.ENTER){
                    if(event.start!=null)runOnMainSync(()->host.focus(event.start));
                    for(int frame=0;frame<=8;frame++){final float f=frame/8f;runOnMainSync(()->host.replayFrame(event,f));SystemClock.sleep(45);}
                }
                event.applyVisual(visual);runOnMainSync(()->{host.replayFrame(null,0);host.invalidateScene();host.setWorld(visual,null,-1);});
            }
            check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(reference)),"installed auto march and convoy exact core equality");
        }
        check(entered&&w.units.isEmpty()&&w.domestic.missions.isEmpty(),"automatic arrival garrison and convoy unloading");
        runOnMainSync(()->{host.invalidateScene();host.setWorld(w,null,-1);host.focus(w.city(2).hex);});settle();preview("field-garrison-convoy");
    }
    private void port()throws Exception{
        android.util.Log.i("SceneAcceptance","BEGIN port replay");
        World w=FieldSceneFixture.port();activate(w);
        ParcelFileDescriptor recording=getUiAutomation().executeShellCommand("screenrecord --time-limit 30 /sdcard/field-port.mp4");World.Unit u=w.units.get(0);World reference=SaveCodec.decode(SaveCodec.encode(w));World visual=SaveCodec.decode(SaveCodec.encode(w));
        TurnJournal journal=new TurnJournal(w);Hex target=new Hex(21,19);
        World.Result move=w.move(u.id,target);check(move.ok,"actual port move: "+move.message);journal.close();check(reference.move(u.id,target).ok,"reference port move");
        runOnMainSync(()->{host.invalidateScene();host.setWorld(visual,u.hex,u.id);host.focus(new Hex(19,19));try{((FilamentMapView)field(host,"spatial")).camera.span=4;}catch(Exception e){throw new RuntimeException(e);}});settle();
        for(TurnJournal.Event event:journal.events()){
            for(int frame=0;frame<=36;frame++){final float f=frame/36f;runOnMainSync(()->host.replayFrame(event,f));SystemClock.sleep(100);
                if(event.kind==TurnJournal.Kind.MOVE&&frame==18){settle();
                    runOnMainSync(()->{try{FilamentMapView view=(FilamentMapView)field(host,"spatial");Object proxy=((Map<?,?>)field(view,"objects")).get("unit:"+u.id);
                        check(((UnitAnimation)field(proxy,"animation")).naval&&((String)field(proxy,"poseKey")).startsWith("unit-TOWER_SHIP"),"actual tower ship renderable at legal water node");
                    }catch(Exception e){throw new RuntimeException(e);}});preview("field-port-water");
                }
            }
            event.applyVisual(visual);runOnMainSync(()->{host.replayFrame(null,0);host.invalidateScene();host.setWorld(visual,null,-1);});
        }
        check(Arrays.equals(SaveCodec.encode(w),SaveCodec.encode(reference)),"port replay keeps exact authoritative result");preview("field-port-arrival");
        runOnMainSync(()->{host.invalidateScene();host.setWorld(w,null,-1);});recording.close();
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(FieldSceneFixture.create(0,false,false)));}
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
        stress(50,false,true);stress(100,false,false);stress(0,true,false);stress(50,true,true);lifecycle();port();march();
        runOnMainSync(()->host.switchMode(false));check(!host.is3D(),"2D fallback remains available");
        result.putString("stream","PASS S04 S05 installed "+checks+" checks\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL S04 S05 "+e+"\n"+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
