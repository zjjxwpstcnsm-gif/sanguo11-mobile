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
    private Bundle camera=new Bundle();
    private Set<Hex> targets=Collections.emptySet();
    private MarchOrders.Plan route;
    private Runnable criticalSkip;
    MapHost(Context context,MapView.TileListener listener){
        super(context);this.listener=listener;prefs=context.getSharedPreferences("map-renderer",Context.MODE_PRIVATE);
        flat=new MapView(context,listener);flat.setGridShown(Boolean.TRUE.equals(prefs.getAll().get("gridShown")));addView(flat,new LayoutParams(-1,-1));
        // Default stays 2D. An interrupted native session is never restarted automatically.
        if(interruptedSession==null)interruptedSession=Boolean.TRUE.equals(prefs.getAll().get("nativeSession"));
        safeMode=interruptedSession;if(safeMode){String reason=interruptedReason(context);prefs.edit().putString("lastExitReason",reason).apply();android.util.Log.w("MapRenderer","Previous native session: "+reason);Toast.makeText(context,"上次 3D 会话未正常结束（"+reason+"），已安全回到 2D；可手动重试",Toast.LENGTH_LONG).show();}
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
    void editorMode(MapView.EditorStroke value){editorStroke=value;flat.editorMode(value);if(spatial!=null)spatial.editorMode(value);}
    void editorDrawing(boolean value){editorDrawing=value;flat.editorDrawing(value);if(spatial!=null)spatial.editorDrawing(value);}
    void editorPreview(Set<Hex> cells,boolean valid){editorCells=new HashSet<>(cells);editorValid=valid;flat.editorPreview(cells,valid);if(spatial!=null)spatial.editorPreview(cells,valid);}
    void editorLayers(boolean grid,boolean coords,boolean passability,boolean footprints){editorGrid=grid;editorCoords=coords;editorPassability=passability;editorFootprints=footprints;flat.editorLayers(grid,coords,passability,footprints);if(spatial!=null)spatial.editorLayers(projection.blocked(world,passability),grid,coords,footprints);}
    void resetOrientation(){if(spatial!=null)spatial.resetOrientation();}
    void reverseOrientation(){if(spatial!=null)spatial.reverseOrientation();}
    void previewMode(){openingPreview=true;flat.previewMode();}
    void setPreviewFaction(int side){previewFaction=side;flat.setPreviewFaction(side);if(spatial!=null)spatial.previewFaction(side);}
    private void persistCamera(){if(spatial==null)return;Bundle b=new Bundle();spatial.saveCamera(b);prefs.edit().putInt("version",1).putFloat("tilt",b.getFloat("sceneTilt",55)).putInt("facing",b.getInt("sceneFacing",1)).apply();}
    SceneQuality quality(){return SceneQuality.from(prefs.getAll().get("quality"));}
    void quality(SceneQuality next){
        if(next==quality())return;
        boolean active=is3D();if(active)switchMode(false);
        prefs.edit().putString("quality",next.name()).apply();
        if(active)switchMode(true);
    }
    boolean is3D(){return spatial!=null;}
    void switchMode(boolean use3D){switchMode(use3D,true);}
    private void switchMode(boolean use3D,boolean manual){
        if(use3D==is3D()||world==null)return;
        replayFrame(null,0);criticalFrame(null,0);saveCamera(camera);
        if(!use3D){leave3D();flat.setWorld(world,selected,moving);flat.restoreCamera(camera);return;}
        android.app.ActivityManager manager=(android.app.ActivityManager)getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if(manager==null||manager.getDeviceConfigurationInfo().reqGlEsVersion<0x30000){
            Toast.makeText(getContext(),"设备未提供 OpenGL ES 3.0，保留 2D",Toast.LENGTH_LONG).show();return;
        }
        if(!manual&&(safeMode||interruptedSession))return;
        // Synchronous commit precedes native library load; catches native crashes next launch.
        if(!prefs.edit().putBoolean("nativeSession",true).commit()){Toast.makeText(getContext(),"无法保存 3D 启动健康标记，保留 2D",Toast.LENGTH_LONG).show();return;}
        try{
            spatial=new FilamentMapView(getContext(),listener,this::fallback);activeNativeHosts++;safeMode=false;
            // The user explicitly retried the native renderer. Do not let the previous
            // process's interruption veto later Activity restoration in this process.
            // Keep nativeSession=true on disk until normal release so a new crash still
            // starts safely in 2D; fallback() also reinstates the in-process latch.
            if(manual)interruptedSession=false;
            removeView(flat);addView(spatial,new LayoutParams(-1,-1));
            dirty=true;publish();
            Map<String,?> saved=prefs.getAll();if(!camera.containsKey("sceneTilt")&&saved.get("tilt") instanceof Float)camera.putFloat("sceneTilt",(Float)saved.get("tilt"));if(!camera.containsKey("sceneFacing")&&saved.get("facing") instanceof Integer)camera.putInt("sceneFacing",(Integer)saved.get("facing"));
            spatial.setGridShown(gridShown());spatial.restoreCamera(camera);spatial.setTargets(targets);spatial.setRoute(route);spatial.resume(resumed);spatial.diagnostics(diagnostics);spatial.labels(flat.commandersShown(),flat.unitBarsShown());spatial.editorMode(editorStroke);spatial.editorDrawing(editorDrawing);spatial.editorLayers(projection.blocked(world,editorPassability),editorGrid,editorCoords,editorFootprints);spatial.editorPreview(editorCells,editorValid);spatial.setTacticPreview(tacticPreview);spatial.setPanelOcclusion(panelRight,panelBottom);spatial.criticalSkip(criticalSkip);
        }catch(Exception|LinkageError|OutOfMemoryError e){fallback(e);}
    }
    private void fallback(Throwable e){safeMode=true;interruptedSession=true;prefs.edit().putString("lastExitReason","Java initialization/render failure").putString("lastFailure",e.getClass().getSimpleName()).putLong("lastFailureTime",System.currentTimeMillis()).commit();android.util.Log.e("MapRenderer","Filament fallback to 2D",e);leave3D();if(world!=null)flat.setWorld(world,selected,moving);flat.restoreCamera(camera);Toast.makeText(getContext(),"3D 初始化或渲染失败，已返回 2D："+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}
    private void leave3D(){persistCamera();if(spatial!=null){spatial.release();removeView(spatial);spatial=null;activeNativeHosts=Math.max(0,activeNativeHosts-1);}if(flat.getParent()==null)addView(flat,new LayoutParams(-1,-1));prefs.edit().putBoolean("nativeSession",activeNativeHosts>0).commit();}
    void release(){persistCamera();if(spatial!=null){spatial.release();removeView(spatial);spatial=null;activeNativeHosts=Math.max(0,activeNativeHosts-1);prefs.edit().putBoolean("nativeSession",activeNativeHosts>0).commit();}}
    void resume(boolean value){if(!value)persistCamera();resumed=value;if(spatial!=null)spatial.resume(value);}
    void toggleDiagnostics(){diagnostics=!diagnostics;if(spatial!=null){spatial.diagnostics(diagnostics);spatial.labels(flat.commandersShown(),flat.unitBarsShown());spatial.editorMode(editorStroke);spatial.editorDrawing(editorDrawing);spatial.editorLayers(projection.blocked(world,editorPassability),editorGrid,editorCoords,editorFootprints);spatial.editorPreview(editorCells,editorValid);spatial.setTacticPreview(tacticPreview);spatial.setPanelOcclusion(panelRight,panelBottom);spatial.criticalSkip(criticalSkip);}android.util.Log.i("MapRenderer",report());}
    String report(){return spatial==null?"2D · "+getWidth()+" × "+getHeight():spatial.report();}
    @Override public void setWorld(World w,Hex s,int moving){
        boolean changed=world!=w||revision!=w.commandRevision()||turn!=w.turn||player!=w.player||terrainRevision!=w.terrainRevision||!Objects.equals(selected,s)||this.moving!=moving;
        if(visualResolved!=w){visualResolved=w;if(w.visualMap==null&&!w.customMapId.isEmpty())try{MapPatch p=new MapLibrary(getContext()).visual(w);if(p!=null)w.visualMap=p;}catch(java.io.IOException e){android.util.Log.w("MapRenderer","Visual map unavailable; deterministic defaults",e);}}
        world=w;selected=s;this.moving=moving;revision=w.commandRevision();turn=w.turn;player=w.player;
        if(spatial==null)flat.setWorld(w,s,moving);else if(changed||dirty)publish();
    }
    private void publish(){if(spatial==null||world==null)return;
        android.content.Context app=getContext().getApplicationContext();
        if(app instanceof GameApplication){var session=((GameApplication)app).host().session();if(session!=null)spatial.sceneIdentity(session.state());}
if(ground==null||groundWorld!=world||terrainRevision!=world.terrainRevision){if(ground==null||!ground.matches(world))ground=new MapSceneSnapshot.Ground(world);groundWorld=world;terrainRevision=world.terrainRevision;}spatial.snapshot(new MapSceneSnapshot(ground,world,selected,moving));spatial.mapLayers(projection.layers(world,ground,flat.territoryMode()),flat.territoryMode(),openingPreview,previewFaction);dirty=false;}
    void invalidateScene(){dirty=true;flat.invalidateScene();}
    @Override public void fit(){if(spatial==null)flat.fit();else spatial.fit();}
    @Override public void focus(Hex h){if(spatial==null)flat.focus(h);else spatial.focus(h);}
    @Override public void center(Hex h){if(spatial==null)flat.center(h);else spatial.center(h);}
    @Override public void saveCamera(Bundle b){b.putBoolean("sceneEnabled",is3D());if(spatial==null)flat.saveCamera(b);else spatial.saveCamera(b);}
    @Override public void restoreCamera(Bundle b){camera=new Bundle(b);if(!safeMode&&Boolean.TRUE.equals(b.get("sceneEnabled"))&&spatial==null)switchMode(true,false);if(spatial==null)flat.restoreCamera(b);else spatial.restoreCamera(b);}
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);if(flat!=null)flat.setEnabled(enabled);if(spatial!=null)spatial.setEnabled(enabled);}
    void setUnitDrop(Consumer<MarchOrders.Plan> drop){flat.setUnitDrop(drop);}
    void setRoute(MarchOrders.Plan value){route=value;flat.setRoute(value);if(spatial!=null)spatial.setRoute(value);}
    void setPickTargets(Set<Hex> value){targets=value;flat.setPickTargets(value);if(spatial!=null)spatial.setTargets(value);}
    void setTacticPreview(Displacement.Preview value){tacticPreview=value;flat.setTacticPreview(value);if(spatial!=null)spatial.setTacticPreview(value);}
    void battleFeedback(World.Result result,boolean haptics){if(spatial==null)flat.battleFeedback(result,haptics);else if(haptics&&result.feedback!=World.Feedback.NONE)performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);}
    void setPanelOcclusion(int right,int bottom){panelRight=right;panelBottom=bottom;flat.setPanelOcclusion(right,bottom);if(spatial!=null)spatial.setPanelOcclusion(right,bottom);}
    void setTerritoryMode(int value){flat.setTerritoryMode(value);if(spatial!=null&&world!=null)spatial.mapLayers(projection.layers(world,ground,flat.territoryMode()),flat.territoryMode(),openingPreview,previewFaction);}
    boolean gridShown(){return flat.gridShown();}
    void setGridShown(boolean shown){flat.setGridShown(shown);if(spatial!=null)spatial.setGridShown(shown);prefs.edit().putBoolean("gridShown",shown).apply();}
    int territoryMode(){return flat.territoryMode();}
    Territory territory(){if(spatial!=null&&world!=null)flat.setWorld(world,selected,moving);return flat.territory();}
    void toggleNavigator(){if(spatial!=null){spatial.fit();return;}flat.toggleNavigator();}
    boolean commandersShown(){return flat.commandersShown();}
    boolean unitBarsShown(){return flat.unitBarsShown();}
    void setCommandersShown(boolean value){flat.setCommandersShown(value);if(spatial!=null)spatial.labels(value,flat.unitBarsShown());}
    void setUnitBarsShown(boolean value){flat.setUnitBarsShown(value);if(spatial!=null)spatial.labels(flat.commandersShown(),value);}
    void setCriticalSkip(Runnable skip){criticalSkip=skip;flat.setCriticalSkip(skip);if(spatial!=null)spatial.criticalSkip(skip);}
    void criticalFrame(CriticalHit hit,float phase){if(spatial==null)flat.criticalFrame(hit,phase);else {if(projectedCritical!=hit){projectedCritical=hit;projectedPortrait=hit==null?null:new OfficerPortrait(getContext(),world,hit.officerCopy());}spatial.critical(hit,phase,projectedPortrait);}}
    void replayFrame(TurnJournal.Event e,float fraction){if(spatial==null)flat.replayFrame(e,fraction);else spatial.replay(e,fraction);}
    boolean replayVisible(TurnJournal.Event e){return spatial==null?flat.replayVisible(e):spatial.visible(e);}
}
