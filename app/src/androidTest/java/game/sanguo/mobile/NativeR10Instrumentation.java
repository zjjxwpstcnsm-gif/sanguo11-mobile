package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Installed production Activity, GameSession, Filament and recorded legal commands.
 * Explicit, saveable fixtures are labeled; no injected damage or fabricated playback events. */
public final class NativeR10Instrumentation extends SceneInstrumentation {
    private File dir;
    private FilamentMapView view()throws Exception{return (FilamentMapView)field(host,"spatial");}
    private void assetsReady()throws Exception{
        ready();long end=SystemClock.uptimeMillis()+120000;boolean[] done={false};
        while(SystemClock.uptimeMillis()<end){runOnMainSync(()->{try{
            FilamentMapView v=view();done[0]=!(Boolean)field(v,"assetSyncPending")&&((SceneAssetQueue)field(v,"assetWork")).pending()==0;
        }catch(Exception e){throw new RuntimeException(e);}});if(done[0])break;settle();}
        check(done[0],"bounded asset queue settled");check(host.is3D(),"actual native renderer retained");
        runOnMainSync(()->{try{check(((Set<?>)field(view(),"missingAssets")).isEmpty(),"no missing asset/pose fallback");}catch(Exception e){throw new RuntimeException(e);}});
    }
    private void text(String name,String body)throws Exception{Files.write(new File(dir,name).toPath(),body.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private void shell(String cmd,String name)throws Exception{
        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand(cmd));OutputStream out=new FileOutputStream(new File(dir,name))){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
    }
    private void shot(String name,String label)throws Exception{
        settle();assetsReady();surfaceCapture();capture(name+"-ui");
        Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);
        String[] report={""};runOnMainSync(()->report[0]=host.report());
        text(name+".txt","source="+BuildConfig.SOURCE_REVISION+"\nscenario="+label+"\nuptime="+SystemClock.uptimeMillis()+"\n"+report[0]);
        android.util.Log.i("R10Evidence",name+" "+label);
    }
    private void activate(World prepared,Hex at,float span)throws Exception{
        runOnMainSync(()->{SessionProbe.install(activity,prepared);activity.refresh();world=SessionProbe.view(activity);host.switchMode(true);host.quality(SceneQuality.HIGH);host.center(at);try{view().camera.span=span;view().camera.yaw=0;view().setGridShown(false);}catch(Exception e){throw new RuntimeException(e);}});
        assetsReady();
    }
    private void inspect(String name)throws Exception{
        StringBuilder report=new StringBuilder();
        runOnMainSync(()->{try{
            FilamentMapView v=view();Map<?,?> objects=(Map<?,?>)field(v,"objects"),shapes=(Map<?,?>)field(v,"shapes");
            Map<Object,Integer> refs=new IdentityHashMap<>();int groups=0,members=0;
            for(Object p:objects.values()){
                for(String slot:new String[]{"shape","flagShape","baseShape","stateShape"}){Object g=field(p,slot);if(g!=null)refs.put(g,refs.getOrDefault(g,0)+1);}
                MapSceneSnapshot.Item item=(MapSceneSnapshot.Item)field(p,"item");if(item.unit==null)continue;
                UnitFormation f=(UnitFormation)field(p,"formation");UnitMotion m=(UnitMotion)field(p,"motion");UnitAnimation a=(UnitAnimation)field(p,"animation");
                check((Integer)field(p,"memberCount")==f.count,"native draw count equals formation count");check(f.count>=1&&f.count<=8,"bounded actual formation");
                check(item.unit.identity().contains("#"+item.unit.id),"non-color stable ID");
                if((Boolean)field(p,"shown")){groups++;members+=f.count;}
                report.append(item.key).append(" count=").append(f.count).append(" root=").append(m.x).append(',').append(f.rootY).append(',').append(m.z).append(" yaw=").append(m.yaw).append(" clip=").append(a.clip).append(" frame=").append(a.frame).append(" poseKey=").append(field(p,"poseKey")).append('\n');
            }
            int shared=0;for(Object shape:shapes.values()){int n=refs.getOrDefault(shape,0);check((Integer)field(shape,"references")==n,"actual GPU reference ownership");if(n>1)shared++;}
            report.append("visibleGroups=").append(groups).append(" representatives=").append(members).append(" sharedBuffers=").append(shared).append('\n').append(host.report());
        }catch(Exception e){throw new RuntimeException(e);}});
        text(name+"-native.txt",report.toString());
    }
    private void sequence(String kind,boolean attack,boolean ridge)throws Exception{
        World before=SaveCodec.decode(SaveCodec.encode(attack?UnitR10Fixture.fighting(kind):UnitR10Fixture.moving(kind,ridge)));
        World control=SaveCodec.decode(SaveCodec.encode(before)),display=SaveCodec.decode(SaveCodec.encode(before));
        int actor=UnitR10Fixture.actor(before).id;String name="r10-"+kind+(attack?"-attack":ridge?"-ridge":"-move");
        Files.write(new File(dir,name+"-fixture.sg11").toPath(),SaveCodec.encode(before));
        activate(before,UnitR10Fixture.actor(before).hex,attack?3.4f:4.3f);
        shot(name+"-before","EXPLICIT UnitR10Fixture; real "+(attack?"combat":"movement"));
        List<TurnJournal.Event> events=new ArrayList<>();
        runOnMainSync(()->{
            World.Result r=SessionProbe.command(activity,w->{TurnJournal j=new TurnJournal(w);try{return attack?UnitR10Fixture.fight(w,kind):UnitR10Fixture.move(w);}finally{j.close();events.addAll(j.events());}});
            check(r.ok,"installed GameSession command "+kind+" "+r.message);world=SessionProbe.view(activity);
        });
        World.Result result=attack?UnitR10Fixture.fight(control,kind):UnitR10Fixture.move(control);check(result.ok,"headless command valid");
        byte[] expected=SaveCodec.encode(control);check(Arrays.equals(expected,SaveCodec.encode(world)),"installed session equals complete headless state/RNG");check(!events.isEmpty(),"recorded actual command events");
        int ei=0;for(TurnJournal.Event event:events){
            final int index=ei++;runOnMainSync(()->{host.invalidateScene();host.setWorld(display,null,-1);host.replayFrame(event,0);});assetsReady();
            for(int frame=0;frame<=24;frame++){
                float f=frame/24f;runOnMainSync(()->host.replayFrame(event,f));SystemClock.sleep(85);
                if(frame==8||frame==17){
                    shot(name+"-e"+index+"-f"+frame,"EXPLICIT fixture; actual journal kind="+event.kind+" actor="+event.actorId+" fraction="+f);
                    inspect(name+"-e"+index+"-f"+frame);
                }
            }
            event.applyVisual(display);
            runOnMainSync(()->{host.replayFrame(null,0);host.invalidateScene();host.setWorld(display,null,-1);});
        }
        runOnMainSync(()->{world=SessionProbe.view(activity);host.invalidateScene();host.setWorld(world,null,-1);});
        check(Arrays.equals(expected,SaveCodec.encode(world)),"drawing phases cannot change GameSession authority");
        // A real SaveCodec round-trip is installed through the same normal session activation.
        World loaded=SaveCodec.decode(expected);activate(loaded,control.unit(actor)!=null?control.unit(actor).hex:before.unit(actor)!=null?before.unit(actor).hex:before.cities.get(0).hex,3.4f);
        check(Arrays.equals(expected,SaveCodec.encode(SessionProbe.view(activity))),"load converges to final authority");
        shot(name+"-after","EXPLICIT fixture; exact post-command save loaded; unit="+actor);
    }
    private void lodAndPicking()throws Exception{
        World w=SaveCodec.decode(SaveCodec.encode(UnitR10Fixture.moving("SPEAR",false)));Hex h=UnitR10Fixture.actor(w).hex;
        activate(w,h,3);byte[] initial=SaveCodec.encode(SessionProbe.view(activity));
        int[] expected={0,0,1,1,0,1,1,2,2,1};float[] spans={3,8.7f,8.9f,7.3f,7.1f,9,23.9f,24.1f,20.1f,19.9f};
        for(int i=0;i<spans.length;i++){final float span=spans[i];runOnMainSync(()->{try{view().camera.span=span;}catch(Exception e){throw new RuntimeException(e);}});settle();check((Integer)field(view(),"unitLod")==expected[i],"installed hysteretic unit LOD "+span);}
        runOnMainSync(()->{try{view().camera.span=3;}catch(Exception e){throw new RuntimeException(e);}});assetsReady();
        runOnMainSync(()->{try{
            FilamentMapView v=view();Object p=((Map<?,?>)field(v,"objects")).get("unit:1");UnitMotion m=(UnitMotion)field(p,"motion");UnitFormation f=(UnitFormation)field(p,"formation");
            float sx=v.camera.screenX(m.x,m.z),sy=v.camera.screenY(m.x,m.z,f.rootY+.20f);
            v.pick(sx,sy,true);check((Integer)field(v,"pickedUnitId")==1,"actual textured formation hit resolves stable unit ID");
        }catch(Exception e){throw new RuntimeException(e);}});
        check(Arrays.equals(initial,SaveCodec.encode(SessionProbe.view(activity))),"LOD and picking preserve full state");
        shot("r10-lod-picking","EXPLICIT fixture; GPU LOD plus actual triangle picking");
        for(int i=0;i<2;i++){
            FilamentMapView old=view();runOnMainSync(()->{host.switchMode(false);host.switchMode(true);});assetsReady();
            check(((Map<?,?>)field(old,"shapes")).isEmpty(),"released old GPU meshes");check(((SceneAssetQueue)field(old,"assetWork")).bytes()==0,"released old CPU cache");
        }
        check(Arrays.equals(initial,SaveCodec.encode(SessionProbe.view(activity))),"2D/3D recreation state parity");
    }
    private void stress(int count)throws Exception{
        World prepared=SaveCodec.decode(SaveCodec.encode(FieldSceneFixture.create(count,false,false)));activate(prepared,new Hex(16,17),12);
        byte[] before=SaveCodec.encode(SessionProbe.view(activity));shot("r10-stress-"+count,"EXPLICIT FieldSceneFixture "+count+" units, not official scenario");inspect("r10-stress-"+count);
        shell("dumpsys meminfo game.sanguo.mobile.dev","r10-stress-"+count+"-live-meminfo.txt");
        long begin=SystemClock.elapsedRealtime();settle();settle();text("r10-stress-"+count+"-sample.txt","wall sample ms="+(SystemClock.elapsedRealtime()-begin)+"\n"+host.report()+"\nSoftware renderer only; not hardware FPS/thermal acceptance.\n");
        check(Arrays.equals(before,SaveCodec.encode(SessionProbe.view(activity))),"stress draws preserve authority");
    }
    private void turn(int speed,boolean skip)throws Exception{
        World initial=SaveCodec.decode(SaveCodec.encode(FieldSceneFixture.march()));FieldSceneFixture.depart(initial);initial=SaveCodec.decode(SaveCodec.encode(initial));
        World control=SaveCodec.decode(SaveCodec.encode(initial));check(control.nextTurn().ok,"headless turn");activate(initial,new Hex(8,9),8);
        runOnMainSync(()->invoke("advanceTurn",new Class<?>[0]));TurnWork work=(TurnWork)field(activity,"turnWork");runOnMainSync(()->{work.speed=speed;work.fullReplay=true;work.skipAnimations=skip;});
        long end=SystemClock.uptimeMillis()+180000;while((Boolean)field(activity,"aiRunning")&&SystemClock.uptimeMillis()<end)settle();check(!(Boolean)field(activity,"aiRunning"),"normal TurnWork/TurnPlayback completed");
        runOnMainSync(()->world=SessionProbe.view(activity));check(Arrays.equals(SaveCodec.encode(control),SaveCodec.encode(world)),"normal/fast/skip full SaveCodec and RNG parity");
        shot("r10-turn-"+speed+"-"+skip,"EXPLICIT legal departure fixture; normal TurnWork speed="+speed+" skip="+skip);
        text("r10-turn-"+speed+"-"+skip+"-time.txt","computeMs="+work.computeMillis+" totalMs="+work.totalMillis+" visible="+work.visibleCount+"\n");
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
        shell("settings put global animator_duration_scale 1","r10-animation-setting.txt");
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        runOnMainSync(()->invoke("startScenario",new Class<?>[]{String.class,int.class},"coalition-190",0));
        long end=SystemClock.uptimeMillis()+90000;while(field(activity,"map")==null&&SystemClock.uptimeMillis()<end)settle();host=(MapHost)field(activity,"map");
        runOnMainSync(()->{world=SessionProbe.view(activity);host.switchMode(true);host.quality(SceneQuality.MEDIUM);});assetsReady();
        for(World.SiteKind kind:new World.SiteKind[]{World.SiteKind.GATE,World.SiteKind.PORT}){
            World.City site=world.cities.stream().filter(c->c.kind==kind).findFirst().orElseThrow();runOnMainSync(()->{host.center(site.hex);try{view().camera.span=5;}catch(Exception e){throw new RuntimeException(e);}});shot("r10-official-"+kind,"coalition-190 official; unchanged inherited site/ground, not newly accepted V2");
        }
        commandFlow();shot("r10-official-command-flow","coalition-190 official deployment/movement/turn/autosave load");
        for(String kind:UnitR10Fixture.KINDS){sequence(kind,false,false);if(!kind.equals("transport"))sequence(kind,true,false);}
        sequence("SPEAR",false,true);lodAndPicking();stress(50);stress(100);turn(1,false);turn(4,false);turn(1,true);
        text("R10-RESULT.txt","PASS R10 "+checks+" installed checks\nsource="+BuildConfig.SOURCE_REVISION+"\nActual native APK on x86_64 software emulator. Fixtures explicitly labeled. Full manual touchscreen, PC art parity and ARM64 devices NOT_RUN.\n");
        result.putString("stream","PASS R10 "+checks+" installed checks\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){try{capture("r10-failure");text("R10-FAIL.txt",android.util.Log.getStackTraceString(e));}catch(Throwable ignored){}result.putString("stream","FAIL R10 "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
