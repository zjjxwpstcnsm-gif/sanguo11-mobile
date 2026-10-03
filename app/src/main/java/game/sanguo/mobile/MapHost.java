package game.sanguo.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.util.*;
import java.util.function.Consumer;
import game.sanguo.core.*;

/** The game's sole map host. UI commands and TurnPlayback never select a second World. */
final class MapHost extends FrameLayout implements MapPresentation {
    private final MapView flat;
    private final MapView.TileListener listener;
    private final SharedPreferences prefs;
    private FilamentMapView spatial;
    private FrameLayout loadingCurtain;
    private final SceneRenderGate renderGate=new SceneRenderGate(active->{if(spatial!=null)spatial.resume(active);});
    @Override public void onWindowFocusChanged(boolean focused){
        super.onWindowFocusChanged(focused);
        if(!focused&&flat!=null)flat.cancelInteraction();
        if(renderGate!=null)renderGate.focused(focused);
    }
    @Override protected void onWindowVisibilityChanged(int visibility){
        super.onWindowVisibilityChanged(visibility);
        if(renderGate!=null)renderGate.visible(visibility==VISIBLE);
    }
    @Override protected void onAttachedToWindow(){
        super.onAttachedToWindow();renderGate.focused(hasWindowFocus());renderGate.visible(getWindowVisibility()==VISIBLE);
    }
    @Override protected void onDetachedFromWindow(){
        renderGate.visible(false);super.onDetachedFromWindow();
    }
    private final MapProjectionQuery projection=new MapProjectionQuery();
    private CriticalHit projectedCritical;
    private android.graphics.drawable.Drawable projectedPortrait;
    private World world,groundWorld;
    private int terrainRevision=-1,moving=-1;
    private Hex selected;
    private MapSceneSnapshot.Ground ground;
    private long revision=Long.MIN_VALUE;
    private int turn=-1,player=-1;
    private boolean dirty=true,diagnostics,resumed=true,openingPreview;
    private int previewFaction=-1;
    private World visualResolved;
    private boolean safeMode;
    private static int activeNativeHosts;
    // Latch per process until an explicit successful manual retry. Creating another host
    // must not erase evidence; a successful retry may restore 3D across Activity recreation.
    private static Boolean interruptedSession;
    private static boolean interruptionAnnounced;
    private Bundle camera=new Bundle();
    private Set<Hex> targets=Collections.emptySet();
    private MarchOrders.Plan route;
    private Consumer<MarchOrders.Plan> unitDrop;
    private Runnable criticalSkip;
    private CombatSequence commandEffects;
    private MapSceneSnapshot publishedSnapshot,commandFinalSnapshot;
    MapSceneSnapshot captureCombatSnapshot(){return spatial==null?null:publishedSnapshot;}
    private long commandEffectTime;
    private TurnJournal.Event commandPreparedEvent;
    private final CombatReplayLedger localLedger=new CombatReplayLedger();
    private CombatReplayLedger combatLedger(){
        Context app=getContext().getApplicationContext();
        return app instanceof GameApplication?((GameApplication)app).host().combatLedger:localLedger;
    }
    void pauseEffects(boolean paused){if(spatial!=null)spatial.pauseEffects(paused);}
    void finishReplay(TurnJournal.Event event){combatLedger().finish(event);}
    void playCommandEffects(List<TurnJournal.Event> events,MapSceneSnapshot before){
        cancelCommandEffects();
        if(!resumed||!UiMotion.enabled()){for(var event:events)finishReplay(event);return;}
        commandEffects=new CombatSequence(events,combatLedger(),e->presentationDuration(e)+(sourceVisuals()?PcPresentationPlan.duration(e):0),e->sourceVisuals()?PcPresentationPlan.duration(e):0);
        if(spatial!=null&&before!=null&&publishedSnapshot!=null&&before.ground==publishedSnapshot.ground){
            commandFinalSnapshot=publishedSnapshot;spatial.snapshot(before);
        }
        if(sourceVisuals()&&PcPresentationPlan.duration(commandEffects.current())>0){replayFrame(commandEffects.current(),0);criticalEvent(commandEffects.current(),0);}
        commandEffectTime=android.os.SystemClock.uptimeMillis();postOnAnimation(commandEffectTick);
        if(sourceVisuals())setCriticalSkip(this::cancelCommandEffects);
    }
    int presentationDuration(TurnJournal.Event event){
        return is3D()&&ground!=null&&ground.pcMap!=null?PcFacilityRigs.duration(event):event.durationMillis();
    }
    boolean sourceVisuals(){return is3D()&&ground!=null&&ground.pcMap!=null;}
    int criticalDuration(TurnJournal.Event event){return sourceVisuals()?PcPresentationPlan.duration(event):event!=null&&event.critical!=null?(int)CriticalScene.DURATION:0;}
    boolean criticalReady(){return !sourceVisuals()||spatial.presentationReady();}
    boolean criticalSubmitted(){return !sourceVisuals()||spatial.presentationSubmitted();}
    void criticalEvent(TurnJournal.Event event,float fraction){
        if(!sourceVisuals()){criticalFrame(event==null?null:event.critical,fraction);return;}
        List<PcPresentationPlan.Cue> cues=PcPresentationPlan.cues(event);
        if(cues.isEmpty()||fraction>=1){spatial.presentation(null,0);return;}
        float scaled=Math.max(0,fraction)*cues.size();int index=Math.min(cues.size()-1,(int)scaled);
        spatial.presentation(cues.get(index),scaled-index);
    }
    void cancelCommandEffects(){
        removeCallbacks(commandEffectTick);
        commandPreparedEvent=null;
        if(commandEffects!=null){commandEffects.skip();commandEffects=null;replayFrame(null,0);criticalEvent(null,0);setCriticalSkip(null);}
        if(commandFinalSnapshot!=null){if(spatial!=null)spatial.snapshot(commandFinalSnapshot);commandFinalSnapshot=null;}
        pauseEffects(false);
    }
    void pauseCommandEffects(boolean value){if(commandEffects!=null)commandEffects.pause(value);pauseEffects(value);commandEffectTime=android.os.SystemClock.uptimeMillis();}
    boolean commandEffectsActive(){return commandEffects!=null;}
    boolean commandEffectsPaused(){return commandEffects!=null&&commandEffects.paused();}
    void commandEffectSpeed(int value){if(commandEffects!=null)commandEffects.speed(value);}
    private final Runnable commandEffectTick=this::advanceCommandEffects;
    private void advanceCommandEffects(){
        if(commandEffects==null)return;
        long now=android.os.SystemClock.uptimeMillis();
        boolean sourcePrelude=false;
        if(sourceVisuals()&&PcPresentationPlan.duration(commandEffects.current())>0){
            TurnJournal.Event current=commandEffects.current();int prelude=PcPresentationPlan.duration(current);
            boolean preparing=commandPreparedEvent!=current;
            float elapsed=commandEffects.fraction()*(prelude+presentationDuration(current));
            if(preparing||elapsed<prelude){
                sourcePrelude=true;
                // A viewport change invalidates only the captured presentation
                // raster. Exclude that preparation interval from its visual
                // clock, including changes during an already prepared cue.
                if(!criticalReady()){commandEffectTime=now;postOnAnimation(commandEffectTick);return;}
                if(preparing){commandPreparedEvent=current;commandEffectTime=now;}
            }
        }
        long wallElapsed=now-commandEffectTime;
        if(sourcePrelude&&!commandEffects.paused()){
            // Keep elapsed between normal30/60fps submissions, but never let
            // a stalled/rejected pose disappear without entering the stage.
            if(!criticalSubmitted()){postOnAnimation(commandEffectTick);return;}
            wallElapsed=PcPresentationClock.elapsedMillis(wallElapsed,commandEffects.speed(),true);
        }
        commandEffects.advance(wallElapsed,this::replayVisible);commandEffectTime=now;
        if(commandEffects.done()){cancelCommandEffects();return;}
        TurnJournal.Event event=commandEffects.current();int prelude=sourceVisuals()?PcPresentationPlan.duration(event):0,action=presentationDuration(event);
        float elapsed=commandEffects.fraction()*(prelude+action);
        if(prelude>0&&elapsed<prelude){replayFrame(event,0);criticalEvent(event,elapsed/prelude);}
        else{criticalEvent(null,0);replayFrame(event,Math.max(0,(elapsed-prelude)/action));}
        postOnAnimation(commandEffectTick);
    }
    MapHost(Context context,MapView.TileListener listener){
        super(context);this.listener=listener;prefs=context.getSharedPreferences("map-renderer",Context.MODE_PRIVATE);
        flat=new MapView(context,listener);flat.setTerritoryMode(prefs.contains("territoryMode")?prefs.getInt("territoryMode",0):context.getSharedPreferences("MainActivity",0).getInt("territoryMode",0));flat.setGridShown(Boolean.TRUE.equals(prefs.getAll().get("gridShown")));addView(flat,new LayoutParams(-1,-1));
        // Default stays 2D. An interrupted native session is never restarted automatically.
        if(interruptedSession==null)interruptedSession=Boolean.TRUE.equals(prefs.getAll().get("nativeSession"))||Boolean.TRUE.equals(prefs.getAll().get("nativeFailure"));
        safeMode=interruptedSession;if(safeMode){String reason=Boolean.TRUE.equals(prefs.getAll().get("nativeFailure"))?"上次初始化或渲染失败":interruptedReason(context);prefs.edit().putString("lastExitReason",reason).apply();android.util.Log.w("MapRenderer","Previous native session: "+reason);if(!interruptionAnnounced){interruptionAnnounced=true;Toast.makeText(context,"上次 3D 会话未正常结束，已恢复为 2D；可在视图中手动重试",Toast.LENGTH_LONG).show();}}
    }
    private static String interruptedReason(Context context){
        if(android.os.Build.VERSION.SDK_INT>=30){
            android.app.ActivityManager manager=context.getSystemService(android.app.ActivityManager.class);
            if(manager!=null)for(android.app.ApplicationExitInfo exit:manager.getHistoricalProcessExitReasons(context.getPackageName(),0,1)){
                switch(exit.getReason()){
                    case android.app.ApplicationExitInfo.REASON_CRASH_NATIVE:return "native crash";
                    case android.app.ApplicationExitInfo.REASON_CRASH:return "Java crash";
                    case android.app.ApplicationExitInfo.REASON_USER_REQUESTED:return "user stopped process";
                    case android.app.ApplicationExitInfo.REASON_LOW_MEMORY:return "system low memory";
                    default:return "process death / reason="+exit.getReason();
                }
            }
        }
        return "unknown interruption"; // A health marker alone cannot identify a native crash.
    }
    private MapView.EditorStroke editorStroke;
    private boolean editorDrawing,editorGrid,editorCoords,editorPassability,editorFootprints,editorValid=true;
    private Set<Hex> editorCells=Collections.emptySet();
    private Displacement.Preview tacticPreview;private int panelRight,panelBottom;
    private boolean commandTargeting;
    void setCommandTargeting(boolean active){commandTargeting=active;if(spatial!=null)spatial.commandTargeting(active);}
    void editorMode(MapView.EditorStroke value){editorStroke=value;flat.editorMode(value);if(spatial!=null)spatial.editorMode(value);}
    void editorDrawing(boolean value){editorDrawing=value;flat.editorDrawing(value);if(spatial!=null)spatial.editorDrawing(value);}
    void editorPreview(Set<Hex> cells,boolean valid){editorCells=new HashSet<>(cells);editorValid=valid;flat.editorPreview(cells,valid);if(spatial!=null)spatial.editorPreview(cells,valid);}
    void editorLayers(boolean grid,boolean coords,boolean passability,boolean footprints){editorGrid=grid;editorCoords=coords;editorPassability=passability;editorFootprints=footprints;flat.editorLayers(grid,coords,passability,footprints);if(spatial!=null)spatial.editorLayers(projection.blocked(world,passability),grid,coords,footprints);}
    void resetOrientation(){if(spatial!=null)spatial.resetOrientation();}
    void reverseOrientation(){if(spatial!=null)spatial.reverseOrientation();}
    void previewMode(){openingPreview=true;flat.previewMode();}
    void setPreviewFaction(int side){previewFaction=side;flat.setPreviewFaction(side);if(spatial!=null){spatial.previewFaction(side);if(world!=null&&(ground==null||!ground.matchesGridContext(world,gridForce())))publish();}}
    private int gridForce(){return gridForce(world);}
    private int gridForce(World w){return openingPreview&&previewFaction>=0&&previewFaction<w.factions.length?previewFaction:w.player;}
    private void persistCamera(){if(spatial==null)return;Bundle b=new Bundle();spatial.saveCamera(b);prefs.edit().putInt("version",2).putFloat("tilt",b.getFloat("sceneTilt",55)).putInt("facing",b.getInt("sceneFacing",1)).putFloat("yaw",b.getFloat("sceneYaw",0)).apply();}
    SceneQuality quality(){return SceneQuality.from(prefs.getAll().get("quality"));}
    void quality(SceneQuality next){
        if(next==quality())return;
        boolean active=is3D();if(active)switchMode(false);
        prefs.edit().putString("quality",next.name()).apply();
        if(active)switchMode(true);
    }
    boolean is3D(){return spatial!=null;}
    void switchMode(boolean use3D){switchMode(use3D,true);}
    /** Resolve the requested camera before the first native CPU stream starts. */
    void focusNative(Hex target){
        if(target==null||world==null)return;
        if(spatial!=null){spatial.focus(target);return;}
        switchMode(true,true,target);
    }
    private void switchMode(boolean use3D,boolean manual){switchMode(use3D,manual,null);}
    private void switchMode(boolean use3D,boolean manual,Hex initialTarget){
        if(use3D==is3D()||world==null)return;
        cancelCommandEffects();replayFrame(null,0);criticalFrame(null,0);saveCamera(camera);
        if(!use3D){leave3D();flat.setWorld(world,selected,moving);flat.restoreCamera(camera);return;}
        android.app.ActivityManager manager=(android.app.ActivityManager)getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if(manager==null||manager.getDeviceConfigurationInfo().reqGlEsVersion<0x30000){
            Toast.makeText(getContext(),"设备未提供 OpenGL ES 3.0，保留 2D",Toast.LENGTH_LONG).show();return;
        }
        if(!manual&&(safeMode||interruptedSession))return;
        // Synchronous commit precedes native library load; catches native crashes next launch.
        SharedPreferences.Editor health=prefs.edit().putBoolean("nativeSession",true);
        if(safeMode||Boolean.TRUE.equals(interruptedSession))health.putBoolean("nativeFailure",true);
        if(!health.commit()){Toast.makeText(getContext(),"无法保存 3D 启动健康标记，保留 2D",Toast.LENGTH_LONG).show();return;}
        try{
            spatial=new FilamentMapView(getContext(),listener,this::fallback);activeNativeHosts++;
            spatial.setUnitDrag(new FilamentMapView.UnitDrag(){
                public int actorId(){return moving;}
                public boolean begin(Hex h){
                    World.Unit u=world.unit(moving);
                    return unitDrop!=null&&isEnabled()&&!openingPreview&&editorStroke==null&&!commandTargeting
                        &&targets.isEmpty()&&!commandEffectsActive()&&!world.commandsBlocked()
                        &&u!=null&&u.hex.equals(h)&&world.orders.error(u)==null;
                }
                public MarchOrders.Plan preview(Hex h){
                    World.Unit u=world.unit(moving);
                    if(u==null||h==null||h.equals(u.hex)||!begin(u.hex)||(publishedSnapshot==null||!publishedSnapshot.reachable.contains(h)))return null;
                    MarchOrders.Plan plan=world.marches.previewMove(u.id,h);
                    return plan.valid()&&plan.stepsNow==plan.path.size()-1?plan:null;
                }
                public void drop(MarchOrders.Plan plan){if(unitDrop!=null)unitDrop.accept(plan);}
            });
            // A constructed renderer can still be blank or stalled. Keep recovery latched
            // until this exact host receives a current, settled Surface-content observation.
            FilamentMapView candidate=spatial;
            candidate.onVerifiedOutput(()->{
                if(spatial!=candidate)return;
                hideLoadingCurtain();
                if(manual)completeNativeRetry(candidate);
            });
            removeView(flat);addView(spatial,new LayoutParams(-1,-1));showLoadingCurtain();
            Map<String,?> saved=prefs.getAll();if(!camera.containsKey("sceneYaw")&&saved.get("yaw") instanceof Float)camera.putFloat("sceneYaw",(Float)saved.get("yaw"));if(!camera.containsKey("sceneTilt")&&saved.get("tilt") instanceof Float)camera.putFloat("sceneTilt",(Float)saved.get("tilt"));if(!camera.containsKey("sceneFacing")&&saved.get("facing") instanceof Integer)camera.putInt("sceneFacing",(Integer)saved.get("facing"));
            // A Canvas camera has no native span. The opening preview must fit the
            // national map once the new native view has its real layout dimensions.
            // Saved native cameras (including a user's zoom) are restored unchanged.
            boolean firstNativePreview=openingPreview&&!camera.containsKey("sceneSpan");
            spatial.commandTargeting(commandTargeting);spatial.setGridShown(gridShown());spatial.navigator(flat.navigatorShown());spatial.restoreCamera(camera);spatial.setTargets(targets);spatial.setRoute(route);spatial.resume(renderGate.active());spatial.diagnostics(diagnostics);spatial.labels(flat.commandersShown(),flat.unitBarsShown());spatial.editorMode(editorStroke);spatial.editorDrawing(editorDrawing);spatial.editorLayers(projection.blocked(world,editorPassability),editorGrid,editorCoords,editorFootprints);spatial.editorPreview(editorCells,editorValid);spatial.setTacticPreview(tacticPreview);spatial.setPanelOcclusion(panelRight,panelBottom);spatial.criticalSkip(criticalSkip);
            if(firstNativePreview)spatial.fit();
            if(initialTarget!=null)spatial.initialFocus(initialTarget);
            // Publish after restoring/requesting the initial camera, so CPU work
            // is generated for the actual preview instead of a default local view.
            dirty=true;publish();
        }catch(Exception|LinkageError|OutOfMemoryError e){fallback(e);}
    }
    /** Keep the existing map visible until real Surface pixels are verified; no synthetic ready state. */
    private void showLoadingCurtain(){
        hideLoadingCurtain();
        if(spatial!=null)spatial.loadingCovered(true);
        Context context=getContext();loadingCurtain=new FrameLayout(context);loadingCurtain.setTag("map.loading");
        loadingCurtain.setBackgroundColor(UiTheme.INK);
        flat.setWorld(world,selected,moving);flat.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        loadingCurtain.addView(flat,new LayoutParams(-1,-1));
        android.view.View shield=new android.view.View(context);shield.setClickable(true);shield.setContentDescription("正在准备3D地图，地图操作暂不可用");
        loadingCurtain.addView(shield,new LayoutParams(-1,-1));
        android.widget.LinearLayout status=new android.widget.LinearLayout(context);status.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pad=UiTheme.dp(context,8);status.setPadding(pad,pad,pad,pad);UiTheme.panel(status);
        android.widget.ProgressBar progress=new android.widget.ProgressBar(context);status.addView(progress,new android.widget.LinearLayout.LayoutParams(UiTheme.dp(context,24),UiTheme.dp(context,24)));
        android.widget.TextView label=new android.widget.TextView(context);UiTheme.text(label);label.setText("正在准备 3D 地图…\n暂时显示原地图");label.setTextColor(UiTheme.TEXT);label.setTextSize(13);label.setPadding(pad,0,pad,0);status.addView(label,new android.widget.LinearLayout.LayoutParams(0,-2,1));
        android.widget.Button cancel=CompactButtons.create(context);cancel.setText("取消加载");cancel.setContentDescription("取消3D加载，继续使用2D地图");cancel.setOnClickListener(v->switchMode(false));status.addView(cancel,new android.widget.LinearLayout.LayoutParams(UiTheme.dp(context,96),UiTheme.dp(context,48)));
        LayoutParams bar=new LayoutParams(-1,-2,android.view.Gravity.TOP);bar.setMargins(pad,pad,pad,pad);loadingCurtain.addView(status,bar);
        addView(loadingCurtain,new LayoutParams(-1,-1));if(openingPreview)flat.post(flat::fit);
    }
    private void hideLoadingCurtain(){
        if(loadingCurtain==null)return;
        loadingCurtain.removeView(flat);removeView(loadingCurtain);loadingCurtain=null;
        if(spatial!=null)spatial.loadingCovered(false);
        flat.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }
    private void completeNativeRetry(FilamentMapView candidate){
        if(candidate==null||spatial!=candidate)return; // Ignore a retired host's asynchronous callback.
        // Do not clear the process latch if the persistent update failed. nativeSession
        // stays true until normal release, even after visible output is detected.
        if(prefs.edit().putBoolean("nativeFailure",false).commit()){
            safeMode=false;interruptedSession=false;
        }else android.util.Log.w("MapRenderer","Could not persist verified native recovery; protection retained");
    }
    private void fallback(Throwable e){safeMode=true;interruptedSession=true;prefs.edit().putBoolean("nativeFailure",true).putString("lastExitReason","Java initialization/render failure").putString("lastFailure",e.getClass().getSimpleName()).putLong("lastFailureTime",System.currentTimeMillis()).commit();android.util.Log.e("MapRenderer","Filament fallback to 2D",e);leave3D();if(world!=null)flat.setWorld(world,selected,moving);flat.restoreCamera(camera);Toast.makeText(getContext(),"3D 初始化或渲染失败，已返回 2D："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()),Toast.LENGTH_LONG).show();}
    private void leave3D(){hideLoadingCurtain();persistCamera();if(spatial!=null){spatial.release();removeView(spatial);spatial=null;activeNativeHosts=Math.max(0,activeNativeHosts-1);}if(flat.getParent()==null)addView(flat,new LayoutParams(-1,-1));prefs.edit().putBoolean("nativeSession",activeNativeHosts>0).commit();}
    void release(){hideLoadingCurtain();renderGate.close();cancelCommandEffects();persistCamera();if(spatial!=null){spatial.release();removeView(spatial);spatial=null;activeNativeHosts=Math.max(0,activeNativeHosts-1);prefs.edit().putBoolean("nativeSession",activeNativeHosts>0).commit();}}
    void resume(boolean value){
        if(!value){flat.cancelInteraction();removeCallbacks(commandEffectTick);if(commandEffects!=null)pauseEffects(true);persistCamera();}
        resumed=value;renderGate.resumed(value);
        if(value&&commandEffects!=null){commandEffectTime=android.os.SystemClock.uptimeMillis();pauseEffects(commandEffects.paused());postOnAnimation(commandEffectTick);}
    }
    void toggleDiagnostics(){diagnostics=!diagnostics;if(spatial!=null){spatial.diagnostics(diagnostics);spatial.labels(flat.commandersShown(),flat.unitBarsShown());spatial.editorMode(editorStroke);spatial.editorDrawing(editorDrawing);spatial.editorLayers(projection.blocked(world,editorPassability),editorGrid,editorCoords,editorFootprints);spatial.editorPreview(editorCells,editorValid);spatial.setTacticPreview(tacticPreview);spatial.setPanelOcclusion(panelRight,panelBottom);spatial.criticalSkip(criticalSkip);}android.util.Log.i("MapRenderer",report());}
    String report(){return spatial==null?"2D · "+getWidth()+" × "+getHeight():spatial.report();}
    @Override public void setWorld(World w,Hex s,int moving){
        boolean gridChanged=ground!=null&&!ground.matchesGridContext(w,gridForce(w));
        boolean changed=world!=w||gridChanged||revision!=w.commandRevision()||turn!=w.turn||player!=w.player||terrainRevision!=w.terrainRevision||!Objects.equals(selected,s)||this.moving!=moving;
        if(visualResolved!=w){visualResolved=w;if(w.visualMap==null&&!w.customMapId.isEmpty())try{MapPatch p=new MapLibrary(getContext()).visual(w);if(p!=null)w.visualMap=p;}catch(java.io.IOException e){android.util.Log.w("MapRenderer","Visual map unavailable; deterministic defaults",e);}}
        world=w;selected=s;this.moving=moving;revision=w.commandRevision();turn=w.turn;player=w.player;
        if(spatial==null)flat.setWorld(w,s,moving);else {if(loadingCurtain!=null)flat.setWorld(w,s,moving);if(changed||dirty)publish();}
    }
    private void publish(){if(spatial==null||world==null)return;
        android.os.Trace.beginSection("R16.mapProjection");try{
        cancelCommandEffects();
        android.content.Context app=getContext().getApplicationContext();
        if(app instanceof GameApplication){var session=((GameApplication)app).host().session();if(session!=null)spatial.sceneIdentity(session.state());}
        if(ground==null||groundWorld!=world||terrainRevision!=world.terrainRevision){
            if(ground==null||!ground.matchesTerrain(world))ground=new MapSceneSnapshot.Ground(world,gridForce());
            groundWorld=world;terrainRevision=world.terrainRevision;
        }
        ground=ground.withGridContext(world,gridForce());
        publishedSnapshot=new MapSceneSnapshot(ground,world,selected,moving);spatial.snapshot(publishedSnapshot);
        spatial.mapLayers(projection.layers(world,ground,flat.territoryMode()),flat.territoryMode(),openingPreview,previewFaction);dirty=false;
        }finally{android.os.Trace.endSection();}
    }
    void invalidateScene(){dirty=true;flat.invalidateScene();}
    @Override public void fit(){if(spatial==null)flat.fit();else spatial.fit();}
    @Override public void focus(Hex h){if(spatial==null)flat.focus(h);else spatial.focus(h);}
    @Override public void center(Hex h){if(spatial==null)flat.center(h);else spatial.center(h);}
    @Override public void saveCamera(Bundle b){b.putBoolean("sceneEnabled",is3D());if(spatial==null){for(String key:new String[]{"sceneSpan","sceneTilt","sceneYaw"})if(camera.containsKey(key))b.putFloat(key,camera.getFloat(key));if(camera.containsKey("sceneFacing"))b.putInt("sceneFacing",camera.getInt("sceneFacing"));flat.saveCamera(b);}else spatial.saveCamera(b);}
    @Override public void restoreCamera(Bundle b){camera=new Bundle(b);if(!safeMode&&Boolean.TRUE.equals(b.get("sceneEnabled"))&&spatial==null)switchMode(true,false);if(spatial==null)flat.restoreCamera(b);else spatial.restoreCamera(b);}
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);if(flat!=null)flat.setEnabled(enabled);if(spatial!=null)spatial.setEnabled(enabled);}
    void setUnitDrop(Consumer<MarchOrders.Plan> drop){unitDrop=drop;flat.setUnitDrop(drop);}
    void setRoute(MarchOrders.Plan value){route=value;flat.setRoute(value);if(spatial!=null)spatial.setRoute(value);}
    void setPickTargets(Set<Hex> value){targets=value==null?Collections.emptySet():Set.copyOf(value);flat.setPickTargets(value==null?null:targets);if(spatial!=null)spatial.setTargets(value);}
    void setTacticPreview(Displacement.Preview value){tacticPreview=value;flat.setTacticPreview(value);if(spatial!=null)spatial.setTacticPreview(value);}
    void battleFeedback(World.Result result,boolean haptics){if(spatial==null)flat.battleFeedback(result,haptics);else if(haptics&&result.feedback!=World.Feedback.NONE)performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);}
    void setPanelOcclusion(int right,int bottom){panelRight=right;panelBottom=bottom;flat.setPanelOcclusion(right,bottom);if(spatial!=null)spatial.setPanelOcclusion(right,bottom);}
    void setTerritoryMode(int value){flat.setTerritoryMode(value);if(!openingPreview&&editorStroke==null)prefs.edit().putInt("territoryMode",flat.territoryMode()).apply();if(spatial!=null&&world!=null)spatial.mapLayers(projection.layers(world,ground,flat.territoryMode()),flat.territoryMode(),openingPreview,previewFaction);}
    boolean gridShown(){return flat.gridShown();}
    void setGridShown(boolean shown){flat.setGridShown(shown);if(spatial!=null)spatial.setGridShown(shown);prefs.edit().putBoolean("gridShown",shown).apply();}
    int territoryMode(){return flat.territoryMode();}
    Territory territory(){if(spatial!=null&&world!=null)flat.setWorld(world,selected,moving);return flat.territory();}
    void toggleNavigator(){flat.toggleNavigator();if(spatial!=null)spatial.navigator(flat.navigatorShown());}
    boolean commandersShown(){return flat.commandersShown();}
    boolean unitBarsShown(){return flat.unitBarsShown();}
    void setCommandersShown(boolean value){flat.setCommandersShown(value);if(spatial!=null)spatial.labels(value,flat.unitBarsShown());}
    void setUnitBarsShown(boolean value){flat.setUnitBarsShown(value);if(spatial!=null)spatial.labels(flat.commandersShown(),value);}
    void setCriticalSkip(Runnable skip){criticalSkip=skip;flat.setCriticalSkip(skip);if(spatial!=null)spatial.criticalSkip(skip);}
    void criticalFrame(CriticalHit hit,float phase){if(spatial==null)flat.criticalFrame(hit,phase);else {if(sourceVisuals()){hit=null;if(hit==null)spatial.presentation(null,0);}if(projectedCritical!=hit){projectedCritical=hit;projectedPortrait=hit==null?null:new OfficerPortrait(getContext(),world,hit.officerCopy());}spatial.critical(hit,phase,projectedPortrait);}}
    void replayFrame(TurnJournal.Event e,float fraction){if(combatLedger().completed(e))e=null;if(spatial==null)flat.replayFrame(e,fraction);else spatial.replay(e,fraction);}
    boolean replayVisible(TurnJournal.Event e){return spatial==null?flat.replayVisible(e):spatial.visible(e);}
}
