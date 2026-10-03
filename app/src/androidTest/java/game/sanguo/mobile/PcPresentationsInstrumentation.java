package game.sanguo.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Genuine tactic/plot commands, installed source GPU layers, lifecycle and exact authority. */
public final class PcPresentationsInstrumentation extends SceneInstrumentation {
    private File dir;private FilamentMapView renderer;private boolean continuous,lifecycleOnly,rasterDiagnostic;private String selectedCase="all";
    @Override public void onCreate(Bundle args){continuous=args!=null&&"true".equals(args.getString("continuous"));rasterDiagnostic=args!=null&&"true".equals(args.getString("raster"));lifecycleOnly=args!=null&&"true".equals(args.getString("lifecycleOnly"));if(args!=null)selectedCase=args.getString("case","all");if(!Arrays.asList("all","tactic-guanyu","plot-sorcery","plot-lightning","tactic-liubei-young","tactic-liubei-old","tactic-guanyu-young","tactic-guanyu-old","tactic-zhangfei-young","tactic-zhangfei-old","tactic-zhaoyun-young","tactic-zhaoyun-old","tactic-zhugeliang-young","tactic-zhugeliang-old","tactic-caocao-young","tactic-caocao-old").contains(selectedCase))throw new IllegalArgumentException("Unknown normal-command fixture "+selectedCase);super.onCreate(args);}
    @Override public void onStart(){
        Bundle result=new Bundle();dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();File report=new File(dir,continuous?"pc-presentations-continuous-report.txt":lifecycleOnly?"pc-presentations-lifecycle-report.txt":"pc-presentations-report.txt");
        File auto=new File(getTargetContext().getFilesDir(),"auto.sg11");boolean existed=auto.isFile(),backed=false;byte[] backup=null;
        var prefs=getTargetContext().getSharedPreferences("map-renderer",0);Map<String,?> old=new HashMap<>(prefs.getAll());
        var client=getTargetContext().getSharedPreferences("MainActivity",0);Map<String,?> oldClient=new HashMap<>(client.getAll());
        try{
            if(existed)backup=Files.readAllBytes(auto.toPath());backed=true;
            prefs.edit().clear().putInt("version",2).putBoolean("3d",false).putBoolean("nativeSession",false).putBoolean("nativeFailure",false).putString("quality","LOW").commit();client.edit().putBoolean("viewOnly",false).commit();
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
            checkedMain(()->invoke("closePanel",new Class<?>[0]));
            for(var fixture:PcCriticalsFixture.presentations()){
                boolean tactic=fixture.tactic;String label=fixture.label;
                if(!selectedCase.equals("all")&&!selectedCase.equals(label))continue;
                World expected=SaveCodec.decode(SaveCodec.encode(fixture.world));TurnJournal journal=new TurnJournal(expected);World.Result reference=fixture.command(expected);journal.close();
                check(reference.ok,"independent normal "+label+" command");
                int count=0;for(TurnJournal.Event event:journal.events())count+=PcPresentationPlan.cues(event).size();
                check(count==1,"normal command records one original cue "+label);
                checkedMain(()->{host.switchMode(false);SessionProbe.install(activity,fixture.world);activity.refresh();host.focusNative(fixture.world.unit(fixture.actor).hex);});
                renderer=(FilamentMapView)field(host,"spatial");checkedMain(()->{renderer.camera.span=2;renderer.camera.yaw=0;renderer.camera.tilt=55;host.setGridShown(false);});settle();ready();presentationReady();
                check(host.sourceVisuals(),"original map presentation mode");
                if(continuous){continuousCommand(fixture,expected,label,report);continue;}
                if(rasterDiagnostic)for(String kind:new String[]{"source","stage-first","stage"})Files.deleteIfExists(new File(dir,"pc-presentation-"+label+"-gpu-"+kind+".png").toPath());
                checkedMain(()->{try{PcPresentationStage stage=(PcPresentationStage)field(renderer,"presentationStage");if(rasterDiagnostic)stage.rasterObserver=(kind,bitmap)->{try(OutputStream file=new FileOutputStream(new File(dir,"pc-presentation-"+label+"-gpu-"+kind+".png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,file);}catch(IOException e){throw new RuntimeException(e);}};stage.captureObserver=bitmap->{try(OutputStream file=new FileOutputStream(new File(dir,"pc-presentation-"+label+"-captured-map.png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,file);}catch(IOException e){throw new RuntimeException(e);}};}catch(Exception e){throw new RuntimeException(e);}});
                World.Result[] command={null};checkedMain(()->{command[0]=SessionProbe.command(activity,fixture::command);host.pauseCommandEffects(true);});
                check(command[0].ok&&host.commandEffectsActive()&&host.commandEffectsPaused(),"real session starts pausable presentation "+label);
                Object sequence=field(host,"commandEffects");TurnJournal.Event event=((CombatSequence)sequence).current();
                check(PcPresentationPlan.duration(event)==PcPresentationPlan.CUE_MILLIS&&PcPresentationPlan.CUE_MILLIS>=500&&PcPresentationPlan.CUE_MILLIS<=1000,"single critical duration within user-approved500–1000ms range");
                if(lifecycleOnly){pausedStageReady();Files.write(report.toPath(),(label+" lifecycle-only scope: cache color NOT RUN; separate strict phasezero max<=3 acceptance remains required.\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
                else criticalBackdrop(label,report);
                if(rasterDiagnostic)verifyFirstRaster(label,report);
                for(int step=0;step<3;step++){
                    advancePausedCommand(step==0?180:75);settle();submittedPausedPose();
                    verifyGpu(label,report,fixture.selector);shot(label+"-step"+step);
                }
                float frozen=(Float)field(field(renderer,"pcPresentations"),"phase");SystemClock.sleep(250);settle();
                check(frozen==(Float)field(field(renderer,"pcPresentations"),"phase"),"pause keeps exact source timeline fraction");
                checkedMain(()->callActivityOnPause(activity));check(!(Boolean)field(renderer,"resumed"),"Activity pause gates source frame");checkedMain(()->callActivityOnResume(activity));settle();
                check(frozen==(Float)field(field(renderer,"pcPresentations"),"phase"),"resume preserves paused critical fraction");
                if(tactic)checkedMain(()->{host.commandEffectSpeed(4);host.pauseCommandEffects(false);});else checkedMain(()->host.cancelCommandEffects());
                long deadline=SystemClock.uptimeMillis()+30000;
                while(host.commandEffectsActive()&&SystemClock.uptimeMillis()<deadline)settle();check(!host.commandEffectsActive(),tactic?"4x normal command presentation finishes":"normal skip control finishes presentation");
                settle();
                settledAuthority(expected,label);shot(label+"-after");
                Object owner=field(renderer,"pcPresentations");check((Integer)field(owner,"shown")==0&&field(renderer,"presentationCue")==null,"finished source presentation removed");
                checkedMain(()->host.switchMode(false));check(field(renderer,"pcPresentations")==null&&field(renderer,"presentationStage")==null,"scene exit releases source presentation owner and captured map stage");
                check(((Map<?,?>)field(owner,"textures")).isEmpty()&&((Map<?,?>)field(owner,"instances")).isEmpty()&&field(owner,"prepared")==null,"scene exit releases textures/material instances/CPU packets");
            }
            result.putString("stream","PASS PC PRESENTATIONS installed checks="+checks+"; case="+selectedCase+"; mode="+(continuous?"continuous normal commands":lifecycleOnly?"lifecycle only; strict cache color acceptance excluded and still required":"controlled paused host ticks/pause/resume/4x/skip/exit with strict cache color")+"; original source GPU layers, exact authority/RNG/save; emulator only; source fullscreen lens/MOD/encoded blend/other bindings pending\n");
        }catch(Throwable e){result.putString("stream","FAIL PC PRESENTATIONS "+android.util.Log.getStackTraceString(e));try{capture("pc-presentations-failed");}catch(Exception ignored){}}
        finally{
            try{finishActivityForRestore();}catch(Throwable e){result.putString("stream",result.getString("stream")+"FAIL lifecycle restoration barrier "+e);}
            try{if(backed){if(existed){Files.write(auto.toPath(),backup);if(!Arrays.equals(backup,Files.readAllBytes(auto.toPath())))throw new IOException("restored autosave differs");}else Files.deleteIfExists(auto.toPath());}}catch(IOException e){result.putString("stream",result.getString("stream")+"FAIL autosave restoration "+e);}
            restore(prefs,old);restore(client,oldClient);
        }
        try{Files.write(report.toPath(),result.getString("stream").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException ignored){}finish(Activity.RESULT_OK,result);
    }
    private void continuousCommand(PcCriticalsFixture.Case fixture,World expected,String label,File report)throws Exception{
        // Start the owned short recording only after real scene/asset readiness.
        // Loading videos cannot substitute for continuous command evidence.
        ParcelFileDescriptor recording=getUiAutomation().executeShellCommand("screenrecord --bit-rate 4000000 --time-limit 30 /sdcard/pc-presentation-"+label+"-continuous.mp4");
        Thread recorder=new Thread(()->{try(InputStream shell=new ParcelFileDescriptor.AutoCloseInputStream(recording)){byte[] bytes=new byte[128];while(shell.read(bytes)!=-1){}}catch(IOException e){android.util.Log.e("PcPresentationAcceptance","Recording failed",e);}},"owned critical recording");recorder.setDaemon(true);recorder.start();SystemClock.sleep(500);
        List<String> trace=new ArrayList<>();long[] start={0};World.Result[] command={null};
        checkedMain(()->{start[0]=SystemClock.uptimeMillis();command[0]=SessionProbe.command(activity,fixture::command);});
        check(command[0].ok&&host.commandEffectsActive(),"continuous normal "+label+" starts");
        long deadline=SystemClock.uptimeMillis()+30000,first=-1,last=-1;float firstPhase=0,lastPhase=0,gpuBegin=-1,gpuEnd=-1;boolean shown=false;long gpuFirst=-1,gpuLast=-1,observedGpu=-1;int peakQuads=0;
        while(SystemClock.uptimeMillis()<deadline){
            long[] stamp={0},frames={0},attempts={0},rejected={0},fences={0};float[] phase={-1},gpuPhase={-1};int[] quads={0};boolean[] active={false};
            checkedMain(()->{try{stamp[0]=SystemClock.uptimeMillis();active[0]=host.commandEffectsActive();attempts[0]=(Long)field(renderer,"beginAttempts");rejected[0]=(Long)field(renderer,"beginSkipped");fences[0]=(Long)field(renderer,"presentationFenceWaits");if(field(renderer,"presentationCue")!=null){PcPresentations owner=(PcPresentations)field(renderer,"pcPresentations");phase[0]=(Float)field(renderer,"presentationPhase");gpuPhase[0]=(Float)field(owner,"submittedPhase");quads[0]=(Integer)field(owner,"shown");frames[0]=(Long)field(owner,"rendered");}}catch(Exception e){throw new RuntimeException(e);}});
            trace.add((stamp[0]-start[0])+","+phase[0]+","+quads[0]+","+frames[0]+","+attempts[0]+","+rejected[0]+","+fences[0]+","+gpuPhase[0]);
            if(phase[0]>0){if(first<0){first=stamp[0];firstPhase=phase[0];}last=stamp[0];lastPhase=phase[0];if(quads[0]>0){shown=true;if(gpuFirst<0)gpuFirst=frames[0];gpuLast=frames[0];}}
            if(quads[0]>0&&frames[0]!=observedGpu){observedGpu=frames[0];if(gpuBegin<0)gpuBegin=gpuPhase[0];gpuEnd=gpuPhase[0];peakQuads=Math.max(peakQuads,quads[0]);}
            if(!active[0])break;SystemClock.sleep(10);
        }
        Files.write(new File(dir,"pc-presentation-"+label+"-owner-frames.csv").toPath(),renderer.frameSamples().getBytes("UTF-8"));
        Files.write(new File(dir,"pc-presentation-"+label+"-continuous.csv").toPath(),("elapsed_ms,cue_phase,gpu_quads,source_frames,begin_attempts,begin_rejected,preparation_fence_waits,owner_phase\n"+String.join("\n",trace)+"\n").getBytes("UTF-8"));
        double estimate=lastPhase>firstPhase?(last-first)/(double)(lastPhase-firstPhase):Double.NaN;
        Files.write(report.toPath(),(label+" continuous phase="+firstPhase+".."+lastPhase+" wall_ms="+(last-first)+" estimated_cue_ms="+estimate+" source_frames="+(gpuLast-gpuFirst+1)+" gpu_phase="+gpuBegin+".."+gpuEnd+" peak_quads="+peakQuads+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        recorder.join(35000);check(!recorder.isAlive(),"owned short command recording finishes");
        check(!host.commandEffectsActive(),"continuous normal command finishes");check(shown&&gpuLast>gpuFirst,"continuous original GPU frames actually advance");
        check(firstPhase<.3f&&lastPhase>.65f,"continuous entrance and exit sampled");
        check(gpuBegin<.3f&&gpuEnd>.65f&&peakQuads>10,"source GPU covers entrance/main original layers/exit");
        check(estimate>=400&&estimate<=1200,"continuous user-approved500–1000ms range with emulator sampling tolerance; estimate="+estimate);
        settledAuthority(expected,label);shot(label+"-continuous-after");checkedMain(()->host.switchMode(false));
    }
    private void advancePausedCommand(long elapsed)throws Exception{
        // Deterministic paused-pose inspection uses the production host tick
        // with an explicit elapsed interval. Uninterrupted timing is tested
        // separately by continuousCommand; emulator scheduling cannot define
        // which paused source pose this lifecycle check samples.
        while(elapsed>0){
        submittedStagePose();final long part=Math.min(PcPresentationClock.MAX_VISUAL_STEP_MILLIS,elapsed);elapsed-=part;
        checkedMain(()->{try{
            check(host.commandEffectsPaused(),"normal command paused before controlled visual tick");
            host.pauseCommandEffects(false);
            java.lang.reflect.Field clock=MapHost.class.getDeclaredField("commandEffectTime");clock.setAccessible(true);clock.setLong(host,SystemClock.uptimeMillis()-part);
            java.lang.reflect.Method tick=MapHost.class.getDeclaredMethod("advanceCommandEffects");tick.setAccessible(true);tick.invoke(host);
            host.pauseCommandEffects(true);
        }catch(Exception e){throw new RuntimeException(e);}});
        }
    }
    private void submittedStagePose()throws Exception{
        long deadline=SystemClock.uptimeMillis()+30000;
        while(SystemClock.uptimeMillis()<deadline){boolean[] done={false};checkedMain(()->done[0]=renderer.presentationSubmitted());if(done[0])return;settle();}
        throw new AssertionError("Normal command pose never entered the actual source stage: "+host.report());
    }
    private void submittedPausedPose()throws Exception{
        // Paused inspection has no wall-time playback requirement. Wait for
        // the actual owner submission of the chosen production clock pose;
        // waitForIdleSync alone cannot prove a backend frame was admitted.
        long deadline=SystemClock.uptimeMillis()+30000;
        while(SystemClock.uptimeMillis()<deadline){boolean[] done={false};checkedMain(()->{try{
            PcPresentations owner=(PcPresentations)field(renderer,"pcPresentations");
            done[0]=renderer.presentationSubmitted()&&(Integer)field(owner,"shown")>0&&Math.abs((Float)field(owner,"submittedPhase")-(Float)field(renderer,"presentationPhase"))<.0001f;
        }catch(Exception e){throw new RuntimeException(e);}});if(done[0])return;settle();}
        throw new AssertionError("Paused original source pose was not actually submitted: "+host.report());
    }
    private void criticalBackdrop(String label,File report)throws Exception{
        // Keep the normal command paused at its actual source phase zero. No
        // test-only art or source timeline is installed to perform this check.
        PcPresentationStage stage=pausedStageReady();
        int[] reference=((int[])field(stage,"referencePixels")).clone();
        android.graphics.Bitmap shown=android.graphics.Bitmap.createBitmap((Integer)field(stage,"width"),(Integer)field(stage,"height"),android.graphics.Bitmap.Config.ARGB_8888);
        java.util.concurrent.CountDownLatch copied=new java.util.concurrent.CountDownLatch(1);int[] status={-1};
        checkedMain(()->android.view.PixelCopy.request((android.view.SurfaceView)uncheckedField(renderer,"surface"),shown,r->{status[0]=r;copied.countDown();},new Handler(Looper.getMainLooper())));
        check(copied.await(20,java.util.concurrent.TimeUnit.SECONDS)&&status[0]==android.view.PixelCopy.SUCCESS,"captured stage real surface readback");
        try(OutputStream file=new FileOutputStream(new File(dir,"pc-presentation-"+label+"-stage-phasezero.png"))){shown.compress(android.graphics.Bitmap.CompressFormat.PNG,100,file);}
        int maximum=0,total=0;StringBuilder samples=new StringBuilder("x,y,source_argb,stage_argb\n");
        try{for(int y=0;y<8;y++)for(int x=0;x<8;x++){
            int color=shown.getPixel((2*x+1)*shown.getWidth()/16,(2*y+1)*shown.getHeight()/16);
            samples.append(x).append(',').append(y).append(',').append(Integer.toHexString(reference[y*8+x])).append(',').append(Integer.toHexString(color)).append('\n');
            for(int shift:new int[]{0,8,16}){int delta=Math.abs(((color>>shift)&255)-((reference[y*8+x]>>shift)&255));maximum=Math.max(maximum,delta);total+=delta;}
        }}finally{shown.recycle();}
        Files.write(new File(dir,"pc-presentation-"+label+"-cache-color.csv").toPath(),samples.toString().getBytes("UTF-8"));
        Files.write(report.toPath(),(label+" srgb_swap="+field(renderer,"srgbSwapChain")+" captured-map sample_max_channel_error="+maximum+" sample_total_error="+total+" (SurfaceView resampling and framebuffer encode included)\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        check(maximum<=3,"captured real map keeps orientation/color at source phasezero; max="+maximum);
    }
    private PcPresentationStage pausedStageReady()throws Exception{
        // Lifecycle-only checks need the same real source/cache preparation
        // barrier, even though their scope excludes color acceptance.
        long deadline=SystemClock.uptimeMillis()+30000;
        while(SystemClock.uptimeMillis()<deadline){boolean[] done={false};checkedMain(()->{try{PcPresentationStage stage=(PcPresentationStage)field(renderer,"presentationStage");done[0]=renderer.presentationReady()&&stage!=null&&(Long)field(stage,"screenFrames")>=3;}catch(Exception e){throw new RuntimeException(e);}});if(done[0])break;settle();}
        settle();PcPresentationStage stage=(PcPresentationStage)field(renderer,"presentationStage");
        check(stage!=null&&stage.ready()&&(Long)field(stage,"screenFrames")>=3,"three source stage submissions while normal command paused before color readback");
        return stage;
    }
    private void presentationReady()throws Exception{
        long deadline=SystemClock.uptimeMillis()+30000;
        while(SystemClock.uptimeMillis()<deadline){boolean[] ready={false};checkedMain(()->ready[0]=renderer.presentationReady());if(ready[0])return;settle();}
        throw new AssertionError("Original presentation assets not ready: "+host.report());
    }
    private void verifyGpu(String label,File report,int expectedSelector)throws Exception{
        String[] text={null};checkedMain(()->{
            try{
                PcPresentations owner=(PcPresentations)field(renderer,"pcPresentations");
                PcPresentationPlan.Cue cue=(PcPresentationPlan.Cue)field(renderer,"presentationCue");
                check(cue!=null&&cue.selector==expectedSelector,"normal event binds its source selector");
                int shown=(Integer)field(owner,"shown");check(shown>0,"source presentation has actual GPU quads");
                var engine=(com.google.android.filament.Engine)field(renderer,"engine");var manager=engine.getRenderableManager();
                int batches=(Integer)field(owner,"visibleBatches");check(batches>0&&batches<=shown,"all original quads retained in contiguous batches");
                List<Integer> entities=(List<Integer>)field(owner,"entities");for(int i=0;i<batches;i++)check(manager.getPrimitiveCount(manager.getInstance(entities.get(i)))==1,"source batch actual renderable");
                check((Long)field(owner,"rendered")>0&&((Map<?,?>)field(owner,"textures")).containsKey(1000+cue.selector),"source atlas uploaded on accepted owner frames");
                PcPresentationStage stage=(PcPresentationStage)field(renderer,"presentationStage");check(stage!=null&&stage.ready()&&(Long)field(stage,"captures")>=1&&field(stage,"captured")!=null,"one retained real map raster is ready after viewport changes");
                check(renderer.presentationSubmitted(),"current source pose entered the independent stage, not only a CPU packet or map warmup");
                var mapCamera=(com.google.android.filament.Camera)field(renderer,"lens");
                check(stage.camera!=mapCamera&&Math.abs(stage.camera.getNear()-PcPresentationLens.NEAR)<1e-6&&Math.abs(stage.camera.getCullingFar()-PcPresentationLens.FAR)<1e-4,"independent source depth interval does not borrow the map zoom clipping planes");
                double[] mapProjection=mapCamera.getProjectionMatrix(new double[16]),sourceProjection=stage.camera.getProjectionMatrix(new double[16]);
                for(int i=0;i<16;i++)if(i!=10&&i!=14)check(sourceProjection[i]==mapProjection[i],"actual source camera preserves map projection size/aspect/center");
                check(((Integer)field(stage,"width")).intValue()==((Integer)field(renderer,"bufferWidth")).intValue()&&((Integer)field(stage,"height")).intValue()==((Integer)field(renderer,"bufferHeight")).intValue(),"captured map matches the actual current buffer after battle-log layout/rotation/resolution changes");
                check((Long)field(stage,"bytes")==4L*(Integer)field(stage,"width")*(Integer)field(stage,"height"),"capture memory exactly one viewport RGBA texture");
                check(field(renderer,"criticalHit")==null&&field(renderer,"criticalPortrait")==null,"no legacy generic font/portrait flash in PC mode");
                text[0]=label+" "+owner.report()+"\n";
            }catch(Exception e){throw new RuntimeException(e);}
        });Files.write(report.toPath(),text[0].getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    private void verifyFirstRaster(String label,File report)throws Exception{
        File firstFile=new File(dir,"pc-presentation-"+label+"-gpu-stage-first.png"),laterFile=new File(dir,"pc-presentation-"+label+"-gpu-stage.png");
        long deadline=SystemClock.uptimeMillis()+30000;
        while((!firstFile.isFile()||!laterFile.isFile())&&SystemClock.uptimeMillis()<deadline)settle();
        check(firstFile.isFile()&&laterFile.isFile(),"fresh first and later GPU rasters completed; stale evidence deleted before command");
        android.graphics.Bitmap first=android.graphics.BitmapFactory.decodeFile(firstFile.getPath()),later=android.graphics.BitmapFactory.decodeFile(laterFile.getPath());
        check(first!=null&&later!=null,"actual diagnostic PNGs decode");
        try{
            check(first.getWidth()==later.getWidth()&&first.getHeight()==later.getHeight(),"first native raster retains actual viewport dimensions");
            int size=Math.multiplyExact(first.getWidth(),first.getHeight()),maximum=0;long total=0;
            int[] a=new int[size],b=new int[size];first.getPixels(a,0,first.getWidth(),0,0,first.getWidth(),first.getHeight());later.getPixels(b,0,later.getWidth(),0,0,later.getWidth(),later.getHeight());
            for(int i=0;i<size;i++)for(int shift=0;shift<=16;shift+=8){int error=Math.abs(((a[i]>>>shift)&255)-((b[i]>>>shift)&255));maximum=Math.max(maximum,error);total+=error;}
            Files.write(report.toPath(),(label+" first_native_raster_max_rgb="+maximum+" total_rgb_error="+total+" pixels="+size+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            check(maximum<=3,"first source background matches later phasezero GPU raster; no warm1x1 edge clamp; max="+maximum);
        }finally{if(first!=null)first.recycle();if(later!=null)later.recycle();}
    }
    private void settledAuthority(World expected,String label)throws Exception{
        byte[][] actual={null};checkedMain(()->{try{actual[0]=((GameApplication)activity.getApplication()).host().capture();}catch(IOException e){throw new RuntimeException(e);}});
        check(Arrays.equals(SaveCodec.encode(expected),actual[0]),"complete authority/RNG/save matches independent normal "+label);
        check(Arrays.equals(actual[0],SaveCodec.encode(SaveCodec.decode(actual[0]))),"critical command save roundtrip");
    }
    private Object uncheckedField(Object object,String name){try{return field(object,name);}catch(Exception e){throw new RuntimeException(e);}}
    private void checkedMain(Runnable action){Throwable[] error={null};super.runOnMainSync(()->{try{action.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new IllegalStateException("Main-thread presentation verification",error[0]);}
    private void shot(String name)throws Exception{surfaceCapture();Files.copy(new File(dir,"surface.png").toPath(),new File(dir,"pc-presentation-"+name+".png").toPath(),StandardCopyOption.REPLACE_EXISTING);capture("pc-presentation-"+name+"-ui");}
    private static void restore(android.content.SharedPreferences prefs,Map<String,?> old){var e=prefs.edit().clear();for(var r:old.entrySet()){Object v=r.getValue();String k=r.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));else throw new IllegalArgumentException(k);}e.commit();}
}
