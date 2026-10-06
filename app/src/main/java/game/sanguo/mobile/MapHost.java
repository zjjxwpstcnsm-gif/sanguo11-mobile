package game.sanguo.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.util.*;
import java.util.function.Consumer;
import game.sanguo.core.*;
import game.sanguo.api.SceneFactsSnapshot;
import game.sanguo.api.StateToken;
import game.sanguo.api.GameEvent;
import game.sanguo.api.AppliedEventSnapshot;
import game.sanguo.runtime.query.AppliedEventQuery;

/** The game's sole map host. UI commands and TurnPlayback never select a second World. */
final class MapHost extends FrameLayout implements MapPresentation {
    interface TileListener {void tap(Hex tile);default void unit(int unitId,Hex displayCell){tap(displayCell);}}
    interface EditorStroke {void event(int action,Hex tile);}
    private int territoryMode;
    private boolean gridShown,navigatorShown=true,commandersShown=true,unitBarsShown=true;
    private Territory territory;
    private boolean released;
    private final MapHost.TileListener listener;
    private final SharedPreferences prefs;
    private FilamentMapView spatial;
    private FrameLayout loadingCurtain;
    private final SceneRenderGate renderGate=new SceneRenderGate(active->{if(spatial!=null)spatial.resume(active);});
    @Override public void onWindowFocusChanged(boolean focused){
        super.onWindowFocusChanged(focused);
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
    private SceneFactsSnapshot sceneFacts;
    private StateToken sceneState,appliedFactsState;
    private final LinkedHashMap<String,AppliedEventSnapshot> appliedFacts=new LinkedHashMap<>();
    private long appliedFactsCaptured;
    AppliedEventSnapshot appliedEvent(String id){return appliedFacts.get(id);}
    void recordAppliedEvent(World layout,StateToken state,GameEvent parent,TurnJournal.Event event){
        if(state==null||event==null)return;
        if(!state.equals(appliedFactsState)){appliedFacts.clear();appliedFactsState=state;}
        if(appliedFacts.containsKey(event.id))return;
        GameEvent proven=parent!=null&&state.equals(parent.state)?parent:null;
        AppliedEventSnapshot fact=AppliedEventQuery.capture(layout,state,proven,event);
        appliedFacts.put(fact.id,fact);appliedFactsCaptured++;
        while(appliedFacts.size()>CombatSequence.CAPACITY)appliedFacts.remove(appliedFacts.keySet().iterator().next());
    }
    private int terrainRevision=-1,moving=-1;
    private Hex selected;
    private MapSceneSnapshot.Ground ground;
    private long revision=Long.MIN_VALUE;
    private int turn=-1,player=-1;
    private boolean dirty=true,diagnostics,resumed=true,openingPreview,replayCommitted=true;
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
    private Consumer<String> techniqueFeedback,techniqueSkip;
    private Runnable techniqueDiscard;
    private Consumer<Boolean> techniquePause;
    private List<TurnJournal.Event> techniqueCommandEvents=Collections.emptyList();
    void setTechniqueFeedback(Consumer<String> feedback,Consumer<String> skip,Runnable discard,Consumer<Boolean> pause){techniqueFeedback=feedback;techniqueSkip=skip;techniqueDiscard=discard;techniquePause=pause;}
    private void techniquePhase(TurnJournal.Event event){
        if(event==null)return;
        if(resumed&&replayCommitted&&renderGate.active()&&techniqueFeedback!=null)techniqueFeedback.accept(event.id);
    }
    private void techniqueFinished(TurnJournal.Event event){
        if(event==null)return;
        android.util.Log.i("TechniquePhase","finished phase="+event.id+" resumed="+resumed+" renderActive="+renderGate.active());
        if(resumed&&replayCommitted&&renderGate.active())techniquePhase(event);
        else if(techniqueSkip!=null)techniqueSkip.accept(event.id);
    }
    void discardTechniqueMedia(){if(techniqueDiscard!=null)techniqueDiscard.run();}
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
    private SoundEffects sounds(){Context app=getContext().getApplicationContext();return app instanceof GameApplication?((GameApplication)app).sounds():null;}
    void replayCommitted(boolean value){replayCommitted=value;}
    private void constructionSound(TurnJournal.Event event){SoundEffects audio=sounds();if(audio==null||event==null||!resumed||!replayCommitted)return;for(var change:event.states)if(change.before==null&&change.after!=null&&(change.after.key.startsWith("s")||change.after.key.startsWith("d")))audio.event("journal:"+event.id+":construction:"+change.after.key,SoundEffects.Cue.CONSTRUCTION);}
    private void completionSound(TurnJournal.Event event){
        SoundEffects audio=sounds();if(audio==null||event==null||!resumed||!replayCommitted||!renderGate.active())return;
        Context app=getContext().getApplicationContext();if(!(app instanceof GameApplication)||((GameApplication)app).host().session()==null)return;var token=((GameApplication)app).host().session().state();for(var change:event.states)if(change.before!=null&&change.after!=null&&change.after.key.startsWith("d")&&change.before.remaining>0&&change.after.remaining==0)audio.committed(token,"completion:"+change.after.key,SoundEffects.Cue.COMPLETE);
    }
    private void eventSound(TurnJournal.Event event,float fraction){
        eventSound(event,fraction,true);
    }
    private void eventSound(TurnJournal.Event event,float fraction,boolean visualProgress){
        SoundEffects sounds=sounds();if(sounds==null||event==null||!resumed||!replayCommitted||(visualProgress&&!renderGate.active()))return;
        String id="journal:"+event.id;
        int pcSound=sourceVisuals()?PcPresentationPlan.tacticSound(event):-1;
        boolean critical=event.critical!=null;
        if(!critical)for(var outcome:event.plotOutcomes)if(outcome.success&&outcome.critical){critical=true;break;}
        if(pcSound<0&&fraction>=.35f&&critical&&(!sourceVisuals()||originalCriticalDuration(event)==0))sounds.event(id+":critical",SoundEffects.Cue.CRITICAL);
        completionSound(event);
        SoundEffects.Cue cue=switch(event.kind){case MOVE,ENTER,DEPLOY->SoundEffects.Cue.MARCH;case ATTACK,FACILITY_ATTACK,FACILITY_COUNTER->SoundEffects.Cue.ATTACK;case TACTIC->SoundEffects.Cue.TACTIC;case PLOT->SoundEffects.Cue.PLOT;case RECOVER->SoundEffects.Cue.COMPLETE;default->null;};
        float trigger=cue==SoundEffects.Cue.ATTACK||cue==SoundEffects.Cue.TACTIC?.35f:0;
        if(pcSound>=0){if(fraction>=.35f)sounds.originalTacticEvent(id+":tactic-source",pcSound);}
        else if(cue!=null&&fraction>=trigger)sounds.event(id+":action",cue);
    }
    void pauseEffects(boolean paused){if(techniquePause!=null)techniquePause.accept(paused);if(spatial!=null)spatial.pauseEffects(paused);if(sounds()!=null)sounds().pauseEffects(paused);}
    void finishReplay(TurnJournal.Event event){techniqueFinished(event);completionSound(event);combatLedger().finish(event);}
    void playCommandEffects(List<TurnJournal.Event> events,MapSceneSnapshot before){
        cancelCommandEffects();replayCommitted=true;techniqueCommandEvents=new ArrayList<>(events);for(var event:events)constructionSound(event);
        if(events.size()>CombatSequence.CAPACITY){for(var event:events){if(techniqueSkip!=null)techniqueSkip.accept(event.id);combatLedger().finish(event);}techniqueCommandEvents=Collections.emptyList();return;}
        if(!resumed||!UiMotion.enabled()){
            for(var event:events){
                // Reduced motion commits the presentation directly to its final phase.
                // It does not also mute the already committed action.
                if(resumed){eventSound(event,1,false);if(event.critical!=null&&sounds()!=null&&(!sourceVisuals()||PcPresentationPlan.tacticSound(event)<0))sounds().event("journal:"+event.id+":critical",SoundEffects.Cue.CRITICAL);}
                finishReplay(event);
            }
            techniqueCommandEvents=Collections.emptyList();
            return;
        }
        commandEffects=new CombatSequence(events,combatLedger(),e->presentationDuration(e)+(sourceVisuals()?originalCriticalDuration(e):0),e->sourceVisuals()?originalCriticalDuration(e):0);
        if(spatial!=null&&before!=null&&publishedSnapshot!=null&&before.ground==publishedSnapshot.ground){
            commandFinalSnapshot=publishedSnapshot;spatial.snapshot(before);
        }
        if(sourceVisuals()&&originalCriticalDuration(commandEffects.current())>0){replayFrame(commandEffects.current(),0);criticalEvent(commandEffects.current(),0);}
        commandEffectTime=android.os.SystemClock.uptimeMillis();postOnAnimation(commandEffectTick);
        if(sourceVisuals())setCriticalSkip(this::cancelCommandEffects);
    }
    int presentationDuration(TurnJournal.Event event){
        return is3D()&&ground!=null&&ground.pcMap!=null?PcFacilityRigs.duration(event):event.durationMillis();
    }
    boolean sourceVisuals(){return is3D()&&ground!=null&&ground.pcMap!=null;}
    private PortraitMediaIdentity criticalSource(TurnJournal.Event event){return world==null||event==null||event.critical==null?null:OfficerPortrait.presentationSource(world,event.critical.officerId);}
    private int originalCriticalDuration(TurnJournal.Event event){return PcPresentationPlan.duration(event,criticalSource(event));}
    private List<PcPresentationPlan.Cue> originalCriticalCues(TurnJournal.Event event){return PcPresentationPlan.cues(event,criticalSource(event));}
    int criticalDuration(TurnJournal.Event event){return sourceVisuals()?originalCriticalDuration(event):event!=null&&event.critical!=null?(int)CriticalScene.DURATION:0;}
    boolean criticalReady(){return !sourceVisuals()||spatial.presentationReady();}
    boolean criticalSubmitted(){return !sourceVisuals()||spatial.presentationSubmitted();}
    void criticalEvent(TurnJournal.Event event,float fraction){
        if(!sourceVisuals()){criticalFrame(event==null?null:event.critical,fraction);return;}
        if(event!=null&&fraction>0&&spatial.presentationSubmitted()&&replayCommitted&&resumed&&renderGate.active()&&sounds()!=null){
            int pcSound=PcPresentationPlan.tacticSound(event);
            if(pcSound>=0)sounds().originalTacticEvent("journal:"+event.id+":tactic-source",pcSound);
            else sounds().event("journal:"+event.id+":critical",SoundEffects.Cue.CRITICAL);
        }
        List<PcPresentationPlan.Cue> cues=originalCriticalCues(event);
        if(cues.isEmpty()||fraction>=1){spatial.presentation(null,0);return;}
        float scaled=Math.max(0,fraction)*cues.size();int index=Math.min(cues.size()-1,(int)scaled);
        spatial.presentation(cues.get(index),scaled-index);
    }
    void cancelCommandEffects(){
        if(commandEffects!=null){StringBuilder ids=new StringBuilder();for(var event:techniqueCommandEvents)ids.append(event.id).append(',');android.util.Log.i("TechniquePhase","cancel phases="+ids+" resumed="+resumed+" renderActive="+renderGate.active()+" caller="+java.util.Arrays.toString(new Throwable().getStackTrace()));}
        if(commandEffects!=null){for(var event:techniqueCommandEvents)if(techniqueSkip!=null)techniqueSkip.accept(event.id);}
        techniqueCommandEvents=Collections.emptyList();
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
        if(sourceVisuals()&&originalCriticalDuration(commandEffects.current())>0){
            TurnJournal.Event current=commandEffects.current();int prelude=originalCriticalDuration(current);
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
        if(!commandEffects.paused())for(var event:techniqueCommandEvents)if(combatLedger().completed(event))techniqueFinished(event);
        if(commandEffects.done()){cancelCommandEffects();return;}
        TurnJournal.Event event=commandEffects.current();int prelude=sourceVisuals()?originalCriticalDuration(event):0,action=presentationDuration(event);
        float elapsed=commandEffects.fraction()*(prelude+action);
        if(prelude>0&&elapsed<prelude){replayFrame(event,0);criticalEvent(event,elapsed/prelude);}
        else{criticalEvent(null,0);replayFrame(event,Math.max(0,(elapsed-prelude)/action));}
        postOnAnimation(commandEffectTick);
    }
    MapHost(Context context,MapHost.TileListener listener){
        super(context);this.listener=listener;prefs=context.getSharedPreferences("map-renderer",Context.MODE_PRIVATE);
        territoryMode=prefs.contains("territoryMode")?prefs.getInt("territoryMode",0):context.getSharedPreferences("MainActivity",0).getInt("territoryMode",0);
        SharedPreferences display=context.getSharedPreferences("map-display",0);navigatorShown=display.getBoolean("navigator",true);commandersShown=display.getBoolean("commanders",true);unitBarsShown=display.getBoolean("unitBars",true);
        gridShown=Boolean.TRUE.equals(prefs.getAll().get("gridShown"));
        // A killed/background process retries 3D normally. A recorded renderer failure
        // requires an explicit retry and presents no interactive substitute map.
        if(interruptedSession==null)interruptedSession=Boolean.TRUE.equals(prefs.getAll().get("nativeFailure"));
        safeMode=Boolean.TRUE.equals(interruptedSession);
        if(Boolean.TRUE.equals(prefs.getAll().get("nativeSession"))&&"native crash".equals(interruptedReason(context))){safeMode=true;interruptedSession=true;}
        prefs.edit().putBoolean("sceneEnabled",true).putInt("version",3).apply();
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
    private MapHost.EditorStroke editorStroke;
    private boolean editorDrawing,editorGrid,editorCoords,editorPassability,editorFootprints,editorValid=true;
    private Set<Hex> editorCells=Collections.emptySet();
    private Displacement.Preview tacticPreview;private int panelRight,panelBottom;
    private boolean commandTargeting;
    void setCommandTargeting(boolean active){commandTargeting=active;if(spatial!=null)spatial.commandTargeting(active);}
    void editorMode(MapHost.EditorStroke value){editorStroke=value;if(spatial!=null)spatial.editorMode(value);}
    void editorDrawing(boolean value){editorDrawing=value;if(spatial!=null)spatial.editorDrawing(value);}
    void editorPreview(Set<Hex> cells,boolean valid){editorCells=new HashSet<>(cells);editorValid=valid;if(spatial!=null)spatial.editorPreview(cells,valid);}
    void editorLayers(boolean grid,boolean coords,boolean passability,boolean footprints){editorGrid=grid;editorCoords=coords;editorPassability=passability;editorFootprints=footprints;if(spatial!=null)spatial.editorLayers(projection.blocked(world,passability),grid,coords,footprints);}
    void resetOrientation(){if(spatial!=null)spatial.resetOrientation();}
    void reverseOrientation(){if(spatial!=null)spatial.reverseOrientation();}
    void previewMode(){openingPreview=true;}
    void setPreviewFaction(int side){previewFaction=side;if(spatial!=null){spatial.previewFaction(side);if(world!=null&&(ground==null||!ground.matchesGridContext(world,gridForce())))publish();}}
    private int gridForce(){return gridForce(world);}
    private int gridForce(World w){return openingPreview&&previewFaction>=0&&previewFaction<w.factions.length?previewFaction:w.player;}
    private void persistCamera(){if(spatial==null)return;Bundle b=new Bundle();spatial.saveCamera(b);prefs.edit().putInt("version",2).putFloat("tilt",b.getFloat("sceneTilt",55)).putInt("facing",b.getInt("sceneFacing",1)).putFloat("yaw",b.getFloat("sceneYaw",0)).apply();}
    SceneQuality quality(){return SceneQuality.from(prefs.getAll().get("quality"));}
    void quality(SceneQuality next){
        if(next==quality())return;
        boolean active=is3D();if(active){saveCamera(camera);leave3D();}
        prefs.edit().putString("quality",next.name()).apply();
        if(active)start3D(true,null);
    }
    boolean is3D(){return spatial!=null;}
    /** Compatibility for old test callers: every requested mode is now 3D. */
    void switchMode(boolean use3D){if(spatial==null)start3D(true,null);}
    void retry3D(){if(spatial==null)start3D(true,null);}
    /** Resolve the requested camera before the first native CPU stream starts. */
    void focusNative(Hex target){
        if(target==null||world==null)return;
        if(spatial!=null){spatial.focus(target);return;}
        start3D(true,target);
    }
    private void start3D(boolean manual,Hex initialTarget){
        if(spatial!=null||world==null||released)return;
        if(safeMode&&!manual){showRenderFailure("上次3D渲染未能完成，请重试地图。存档已保留。");return;}
        cancelCommandEffects();
        android.app.ActivityManager manager=(android.app.ActivityManager)getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if(manager==null||manager.getDeviceConfigurationInfo().reqGlEsVersion<0x30000){
            showRenderFailure("设备未提供 OpenGL ES 3.0，无法显示3D地图。存档已保留。");return;
        }
        // Synchronous commit precedes native library load; catches native crashes next launch.
        SharedPreferences.Editor health=prefs.edit().putBoolean("nativeSession",true).putBoolean("nativeFailure",true);
        if(safeMode||Boolean.TRUE.equals(interruptedSession))health.putBoolean("nativeFailure",true);
        if(!health.commit()){showRenderFailure("无法保存3D启动状态，请重试。存档已保留。");return;}
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
                completeNativeRetry(candidate);
            });
            hideLoadingCurtain();addView(spatial,new LayoutParams(-1,-1));showLoadingCurtain();
            Map<String,?> saved=prefs.getAll();if(!camera.containsKey("sceneYaw")&&saved.get("yaw") instanceof Float)camera.putFloat("sceneYaw",(Float)saved.get("yaw"));if(!camera.containsKey("sceneTilt")&&saved.get("tilt") instanceof Float)camera.putFloat("sceneTilt",(Float)saved.get("tilt"));if(!camera.containsKey("sceneFacing")&&saved.get("facing") instanceof Integer)camera.putInt("sceneFacing",(Integer)saved.get("facing"));
            // A Canvas camera has no native span. The opening preview must fit the
            // national map once the new native view has its real layout dimensions.
            // Saved native cameras (including a user's zoom) are restored unchanged.
            boolean firstNativePreview=openingPreview&&!camera.containsKey("sceneSpan");
            spatial.commandTargeting(commandTargeting);spatial.setGridShown(gridShown());spatial.navigator(navigatorShown);spatial.restoreCamera(camera);spatial.setTargets(targets);spatial.setRoute(route);spatial.resume(renderGate.active());spatial.diagnostics(diagnostics);spatial.labels(commandersShown,unitBarsShown);spatial.editorMode(editorStroke);spatial.editorDrawing(editorDrawing);spatial.editorLayers(projection.blocked(world,editorPassability),editorGrid,editorCoords,editorFootprints);spatial.editorPreview(editorCells,editorValid);spatial.setTacticPreview(tacticPreview);spatial.setPanelOcclusion(panelRight,panelBottom);spatial.criticalSkip(criticalSkip);
            if(firstNativePreview)spatial.fit();
            if(initialTarget!=null)spatial.initialFocus(initialTarget);
            // Publish after restoring/requesting the initial camera, so CPU work
            // is generated for the actual preview instead of a default local view.
            dirty=true;publish();
        }catch(Exception|LinkageError|OutOfMemoryError e){fallback(e);}
    }
    private void showLoadingCurtain(){
        hideLoadingCurtain();
        if(spatial!=null)spatial.loadingCovered(true);
        Context context=getContext();loadingCurtain=new FrameLayout(context);loadingCurtain.setTag("map.loading");
        loadingCurtain.setBackgroundColor(UiTheme.INK);loadingCurtain.setClickable(true);
        android.widget.LinearLayout status=new android.widget.LinearLayout(context);status.setOrientation(android.widget.LinearLayout.VERTICAL);status.setGravity(android.view.Gravity.CENTER);
        android.widget.ProgressBar progress=new android.widget.ProgressBar(context);status.addView(progress);
        android.widget.TextView label=new android.widget.TextView(context);UiTheme.text(label);label.setText("正在准备 3D 地图…");label.setTextColor(UiTheme.TEXT);label.setTextSize(15);status.addView(label);
        loadingCurtain.addView(status,new LayoutParams(-1,-1));addView(loadingCurtain,new LayoutParams(-1,-1));
    }
    private void showRenderFailure(String reason){
        hideLoadingCurtain();Context context=getContext();loadingCurtain=new FrameLayout(context);loadingCurtain.setTag("map.failure");loadingCurtain.setBackgroundColor(UiTheme.INK);loadingCurtain.setClickable(true);
        android.widget.LinearLayout status=new android.widget.LinearLayout(context);status.setOrientation(android.widget.LinearLayout.VERTICAL);status.setGravity(android.view.Gravity.CENTER);
        android.widget.TextView label=new android.widget.TextView(context);UiTheme.text(label);label.setText(reason);label.setTextColor(UiTheme.TEXT);label.setPadding(24,24,24,24);status.addView(label);
        android.widget.Button retry=CompactButtons.create(context);retry.setText("重试3D地图");retry.setOnClickListener(v->retry3D());status.addView(retry);
        loadingCurtain.addView(status,new LayoutParams(-1,-1));addView(loadingCurtain,new LayoutParams(-1,-1));
    }
    private void hideLoadingCurtain(){
        if(loadingCurtain!=null){removeView(loadingCurtain);loadingCurtain=null;}
        if(spatial!=null)spatial.loadingCovered(false);
    }
    private void completeNativeRetry(FilamentMapView candidate){
        if(candidate==null||spatial!=candidate)return; // Ignore a retired host's asynchronous callback.
        // Do not clear the process latch if the persistent update failed. nativeSession
        // stays true until normal release, even after visible output is detected.
        if(prefs.edit().putBoolean("nativeFailure",false).commit()){
            safeMode=false;interruptedSession=false;
        }else android.util.Log.w("MapRenderer","Could not persist verified native recovery; protection retained");
    }
    private void fallback(Throwable e){
        safeMode=true;interruptedSession=true;prefs.edit().putBoolean("nativeFailure",true).putString("lastExitReason","Java initialization/render failure").putString("lastFailure",e.getClass().getSimpleName()).putLong("lastFailureTime",System.currentTimeMillis()).commit();
        android.util.Log.e("MapRenderer","3D rendering stopped",e);cancelCommandEffects();leave3D();showRenderFailure("3D地图暂不可用："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage())+"\n存档已保留，可重试或返回。");
    }
    private void leave3D(){hideLoadingCurtain();persistCamera();if(spatial!=null){spatial.release();removeView(spatial);spatial=null;activeNativeHosts=Math.max(0,activeNativeHosts-1);}prefs.edit().putBoolean("nativeSession",activeNativeHosts>0).commit();}
    void release(){
        released=true;clearSceneFacts();appliedFacts.clear();appliedFactsState=null;hideLoadingCurtain();renderGate.close();cancelCommandEffects();persistCamera();
        if(spatial!=null){spatial.release();removeView(spatial);spatial=null;activeNativeHosts=Math.max(0,activeNativeHosts-1);persistOrderlyRelease();}
        // Dismissed dialogs can outlive their Surface. Drop the detached World,
        // terrain wrappers and projection caches at this explicit boundary.
        world=groundWorld=visualResolved=null;ground=null;territory=null;publishedSnapshot=commandFinalSnapshot=null;
        projectedCritical=null;projectedPortrait=null;route=null;tacticPreview=null;targets=editorCells=Collections.emptySet();projection.clear();
        listenerCallbacksClosed();
    }
    private void persistOrderlyRelease(){
        SharedPreferences.Editor health=prefs.edit().putBoolean("nativeSession",activeNativeHosts>0);
        // Closing an initialized-but-not-yet-visible host normally is not a renderer failure.
        // safeMode retains an actual Java/native failure or an unverified manual recovery.
        if(activeNativeHosts==0&&!safeMode)health.putBoolean("nativeFailure",false);
        if(!health.commit())android.util.Log.w("MapRenderer","Could not persist orderly native release");
    }
    private void listenerCallbacksClosed(){unitDrop=null;criticalSkip=null;techniqueFeedback=null;techniqueSkip=null;techniqueDiscard=null;techniquePause=null;editorStroke=null;}
    void resume(boolean value){
        if(!value){removeCallbacks(commandEffectTick);if(commandEffects!=null)pauseEffects(true);persistCamera();}
        resumed=value;renderGate.resumed(value);
        if(value&&commandEffects!=null){commandEffectTime=android.os.SystemClock.uptimeMillis();pauseEffects(commandEffects.paused());postOnAnimation(commandEffectTick);}
    }
    void toggleDiagnostics(){diagnostics=!diagnostics;if(spatial!=null){spatial.diagnostics(diagnostics);spatial.labels(commandersShown,unitBarsShown);spatial.editorMode(editorStroke);spatial.editorDrawing(editorDrawing);spatial.editorLayers(projection.blocked(world,editorPassability),editorGrid,editorCoords,editorFootprints);spatial.editorPreview(editorCells,editorValid);spatial.setTacticPreview(tacticPreview);spatial.setPanelOcclusion(panelRight,panelBottom);spatial.criticalSkip(criticalSkip);}android.util.Log.i("MapRenderer",report());}
    String report(){return "sceneFacts="+(sceneFacts!=null)+" appliedFacts="+appliedFacts.size()+" appliedFactsCaptured="+appliedFactsCaptured+" nativeHosts="+activeNativeHosts+" released="+released+" | "+(spatial==null?"3D 地图暂不可用":spatial.report());}
    void clearSceneFacts(){sceneFacts=null;sceneState=null;dirty=true;}
    @Override public void setWorld(World w,Hex s,int moving){setWorld(w,s,moving,null,null);}
    void setWorld(World w,Hex s,int moving,StateToken expected,SceneFactsSnapshot facts){
        SceneFactsSnapshot accepted=SceneFactsPresentation.accept(w.mapId,w.mapRevision,w.terrainRevision,w.scenarioId,w.dataHash,w.turn,w.player,expected,facts);
        if(!Objects.equals(sceneState,expected)||sceneFacts!=accepted)dirty=true;
        sceneState=expected;sceneFacts=accepted;
        boolean gridChanged=ground!=null&&!ground.matchesGridContext(w,gridForce(w));
        boolean changed=world!=w||gridChanged||revision!=w.commandRevision()||turn!=w.turn||player!=w.player||terrainRevision!=w.terrainRevision||!Objects.equals(selected,s)||this.moving!=moving;
        if(visualResolved!=w){visualResolved=w;if(w.visualMap==null&&!w.customMapId.isEmpty())try{MapPatch p=new MapLibrary(getContext()).visual(w);if(p!=null)w.visualMap=p;}catch(java.io.IOException e){android.util.Log.w("MapRenderer","Visual map unavailable; deterministic defaults",e);}}
        if(world!=w||revision!=w.commandRevision())territory=null;world=w;selected=s;this.moving=moving;revision=w.commandRevision();turn=w.turn;player=w.player;
        Hex initial=!openingPreview&&!camera.containsKey("sceneSpan")&&!camera.containsKey("cameraX")?(s!=null?s:w.home()==null?null:w.home().hex):null;
        if(spatial==null)start3D(false,initial);else if(changed||dirty)publish();
    }
    private void publish(){if(spatial==null||world==null)return;
        android.os.Trace.beginSection("R16.mapProjection");try{
        cancelCommandEffects();
        android.content.Context app=getContext().getApplicationContext();
        if(sceneState!=null)spatial.sceneIdentity(sceneState);
        else if(app instanceof GameApplication){var session=((GameApplication)app).host().session();if(session!=null)spatial.sceneIdentity(session.state());}
        if(ground==null||groundWorld!=world||terrainRevision!=world.terrainRevision){
            if(ground==null||!ground.matchesTerrain(world))ground=new MapSceneSnapshot.Ground(world,gridForce());
            groundWorld=world;terrainRevision=world.terrainRevision;
        }
        ground=ground.withGridContext(world,gridForce());
        publishedSnapshot=new MapSceneSnapshot(ground,world,selected,moving,sceneState,sceneFacts);spatial.snapshot(publishedSnapshot);
        spatial.mapLayers(projection.layers(world,ground,territoryMode),territoryMode,openingPreview,previewFaction);dirty=false;
        }finally{android.os.Trace.endSection();}
    }
    void invalidateScene(){dirty=true;}
    @Override public void fit(){if(spatial!=null)spatial.fit();}
    @Override public void focus(Hex h){if(spatial!=null)spatial.focus(h);}
    @Override public void center(Hex h){if(spatial!=null)spatial.center(h);}
    @Override public void saveCamera(Bundle b){b.putBoolean("sceneEnabled",true);if(spatial!=null)spatial.saveCamera(b);else b.putAll(camera);b.putBoolean("sceneEnabled",true);}
    @Override public void restoreCamera(Bundle b){camera=new Bundle(b);navigatorShown=b.getBoolean("mapNavigator",navigatorShown);if(spatial!=null)spatial.restoreCamera(camera);else if(world!=null)start3D(false,null);camera.putBoolean("sceneEnabled",true);}
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);if(spatial!=null)spatial.setEnabled(enabled);}
    void setUnitDrop(Consumer<MarchOrders.Plan> drop){unitDrop=drop;}
    void setRoute(MarchOrders.Plan value){route=value;if(spatial!=null)spatial.setRoute(value);}
    void setPickTargets(Set<Hex> value){targets=value==null?Collections.emptySet():Set.copyOf(value);if(spatial!=null)spatial.setTargets(value);}
    void setTacticPreview(Displacement.Preview value){tacticPreview=value;if(spatial!=null)spatial.setTacticPreview(value);}
    void battleFeedback(World.Result result,boolean haptics){if(haptics&&result.feedback!=World.Feedback.NONE)performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);}
    void setPanelOcclusion(int right,int bottom){panelRight=right;panelBottom=bottom;if(spatial!=null)spatial.setPanelOcclusion(right,bottom);}
    void setTerritoryMode(int value){territoryMode=Math.max(0,Math.min(2,value));if(!openingPreview&&editorStroke==null)prefs.edit().putInt("territoryMode",territoryMode).apply();if(spatial!=null&&world!=null)spatial.mapLayers(projection.layers(world,ground,territoryMode),territoryMode,openingPreview,previewFaction);}
    boolean gridShown(){return gridShown;}
    void setGridShown(boolean shown){gridShown=shown;if(spatial!=null)spatial.setGridShown(shown);prefs.edit().putBoolean("gridShown",shown).apply();}
    int territoryMode(){return territoryMode;}
    Territory territory(){if(world==null)return null;if(territory==null)territory=new Territory(world);return territory;}
    void toggleNavigator(){navigatorShown=!navigatorShown;getContext().getSharedPreferences("map-display",0).edit().putBoolean("navigator",navigatorShown).apply();if(spatial!=null)spatial.navigator(navigatorShown);}
    boolean commandersShown(){return commandersShown;}
    boolean unitBarsShown(){return unitBarsShown;}
    void setCommandersShown(boolean value){commandersShown=value;getContext().getSharedPreferences("map-display",0).edit().putBoolean("commanders",value).apply();if(spatial!=null)spatial.labels(value,unitBarsShown);}
    void setUnitBarsShown(boolean value){unitBarsShown=value;getContext().getSharedPreferences("map-display",0).edit().putBoolean("unitBars",value).apply();if(spatial!=null)spatial.labels(commandersShown,value);}
    void setCriticalSkip(Runnable skip){criticalSkip=skip;if(spatial!=null)spatial.criticalSkip(skip);}
    void criticalFrame(CriticalHit hit,float phase){if(spatial!=null){if(sourceVisuals()){hit=null;if(hit==null)spatial.presentation(null,0);}if(projectedCritical!=hit){projectedCritical=hit;projectedPortrait=hit==null?null:new OfficerPortrait(getContext(),world,hit.officerCopy(),hit.year);}spatial.critical(hit,phase,projectedPortrait);}}
    void replayFrame(TurnJournal.Event e,float fraction){if(combatLedger().completed(e))e=null;if(e!=null&&fraction>=.35f)techniquePhase(e);if(e!=null)eventSound(e,fraction);if(spatial!=null)spatial.replay(e,fraction);}
    boolean replayVisible(TurnJournal.Event e){return spatial!=null&&spatial.visible(e);}
}
