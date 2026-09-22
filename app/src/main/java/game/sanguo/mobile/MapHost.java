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
    private Bundle camera=new Bundle();
    private Set<Hex> targets=Collections.emptySet();
    private MarchOrders.Plan route;
    private Runnable criticalSkip;
    MapHost(Context context,MapView.TileListener listener){
        super(context);this.listener=listener;prefs=context.getSharedPreferences("map-renderer",Context.MODE_PRIVATE);
        flat=new MapView(context,listener);addView(flat,new LayoutParams(-1,-1));
        // Default stays 2D. An interrupted native session is never restarted automatically.
        safeMode=activeNativeHosts==0&&Boolean.TRUE.equals(prefs.getAll().get("nativeSession"));if(safeMode)Toast.makeText(context,"上次 3D 会话未正常结束，已安全回到 2D；可在视图中手动重试",Toast.LENGTH_LONG).show();
    }
    private MapView.EditorStroke editorStroke;
    private boolean editorDrawing,editorGrid,editorCoords,editorPassability,editorFootprints,editorValid=true;
    private Set<Hex> editorCells=Collections.emptySet();
    private Displacement.Preview tacticPreview;private int panelRight,panelBottom;
    void editorMode(MapView.EditorStroke value){editorStroke=value;flat.editorMode(value);if(spatial!=null)spatial.editorMode(value);}
    void editorDrawing(boolean value){editorDrawing=value;flat.editorDrawing(value);if(spatial!=null)spatial.editorDrawing(value);}
    void editorPreview(Set<Hex> cells,boolean valid){editorCells=new HashSet<>(cells);editorValid=valid;flat.editorPreview(cells,valid);if(spatial!=null)spatial.editorPreview(cells,valid);}
    void editorLayers(boolean grid,boolean coords,boolean passability,boolean footprints){editorGrid=grid;editorCoords=coords;editorPassability=passability;editorFootprints=footprints;flat.editorLayers(grid,coords,passability,footprints);if(spatial!=null)spatial.editorLayers(world,grid,coords,passability,footprints);}
    void resetOrientation(){if(spatial!=null)spatial.resetOrientation();}
    void reverseOrientation(){if(spatial!=null)spatial.reverseOrientation();}
    void previewMode(){openingPreview=true;flat.previewMode();}
    void setPreviewFaction(int side){previewFaction=side;flat.setPreviewFaction(side);if(spatial!=null)spatial.previewFaction(side);}
    private void persistCamera(){if(spatial==null)return;Bundle b=new Bundle();spatial.saveCamera(b);prefs.edit().putInt("version",1).putFloat("tilt",b.getFloat("sceneTilt",55)).putInt("facing",b.getInt("sceneFacing",1)).apply();}
    boolean is3D(){return spatial!=null;}
    void switchMode(boolean use3D){
        if(use3D==is3D()||world==null)return;
        replayFrame(null,0);criticalFrame(null,0);saveCamera(camera);
        if(!use3D){leave3D();flat.setWorld(world,selected,moving);flat.restoreCamera(camera);return;}
        // Synchronous commit precedes native library load; catches native crashes next launch.
        if(!prefs.edit().putBoolean("nativeSession",true).commit()){Toast.makeText(getContext(),"无法保存 3D 启动健康标记，保留 2D",Toast.LENGTH_LONG).show();return;}
        try{
            spatial=new FilamentMapView(getContext(),listener,this::fallback);activeNativeHosts++;safeMode=false;
            removeView(flat);addView(spatial,new LayoutParams(-1,-1));
            dirty=true;publish();
            Map<String,?> saved=prefs.getAll();if(!camera.containsKey("sceneTilt")&&saved.get("tilt") instanceof Float)camera.putFloat("sceneTilt",(Float)saved.get("tilt"));if(!camera.containsKey("sceneFacing")&&saved.get("facing") instanceof Integer)camera.putInt("sceneFacing",(Integer)saved.get("facing"));
            spatial.restoreCamera(camera);spatial.setTargets(targets);spatial.setRoute(route);spatial.resume(resumed);spatial.diagnostics(diagnostics);spatial.labels(flat.commandersShown(),flat.unitBarsShown());spatial.editorMode(editorStroke);spatial.editorDrawing(editorDrawing);spatial.editorLayers(world,editorGrid,editorCoords,editorPassability,editorFootprints);spatial.editorPreview(editorCells,editorValid);spatial.setTacticPreview(tacticPreview);spatial.setPanelOcclusion(panelRight,panelBottom);spatial.criticalSkip(criticalSkip);
        }catch(Exception|LinkageError e){fallback(e);}
    }
    private void fallback(Throwable e){android.util.Log.e("MapRenderer","Filament fallback to 2D",e);leave3D();if(world!=null)flat.setWorld(world,selected,moving);flat.restoreCamera(camera);Toast.makeText(getContext(),"3D 初始化或渲染失败，已返回 2D："+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}
    private void leave3D(){persistCamera();if(spatial!=null){spatial.release();removeView(spatial);spatial=null;activeNativeHosts=Math.max(0,activeNativeHosts-1);}if(flat.getParent()==null)addView(flat,new LayoutParams(-1,-1));prefs.edit().putBoolean("nativeSession",activeNativeHosts>0).commit();}
    void release(){persistCamera();if(spatial!=null){spatial.release();removeView(spatial);spatial=null;activeNativeHosts=Math.max(0,activeNativeHosts-1);prefs.edit().putBoolean("nativeSession",activeNativeHosts>0).commit();}}
    void resume(boolean value){if(!value)persistCamera();resumed=value;if(spatial!=null)spatial.resume(value);}
    void toggleDiagnostics(){diagnostics=!diagnostics;if(spatial!=null)spatial.diagnostics(diagnostics);spatial.labels(flat.commandersShown(),flat.unitBarsShown());spatial.editorMode(editorStroke);spatial.editorDrawing(editorDrawing);spatial.editorLayers(world,editorGrid,editorCoords,editorPassability,editorFootprints);spatial.editorPreview(editorCells,editorValid);spatial.setTacticPreview(tacticPreview);spatial.setPanelOcclusion(panelRight,panelBottom);spatial.criticalSkip(criticalSkip);android.util.Log.i("MapRenderer",report());}
    String report(){return spatial==null?"2D · "+getWidth()+" × "+getHeight():spatial.report();}
    @Override public void setWorld(World w,Hex s,int moving){
        boolean changed=world!=w||revision!=w.commandRevision()||turn!=w.turn||player!=w.player||terrainRevision!=w.terrainRevision||!Objects.equals(selected,s)||this.moving!=moving;
        if(visualResolved!=w){visualResolved=w;if(w.visualMap==null&&!w.customMapId.isEmpty())try{MapPatch p=new MapLibrary(getContext()).visual(w);if(p!=null)w.visualMap=p;}catch(java.io.IOException e){android.util.Log.w("MapRenderer","Visual map unavailable; deterministic defaults",e);}}
        world=w;selected=s;this.moving=moving;revision=w.commandRevision();turn=w.turn;player=w.player;
        if(spatial==null)flat.setWorld(w,s,moving);else if(changed||dirty)publish();
    }
    private void publish(){if(spatial==null||world==null)return;if(ground==null||groundWorld!=world||terrainRevision!=world.terrainRevision){if(ground==null||!ground.matches(world))ground=new MapSceneSnapshot.Ground(world);groundWorld=world;terrainRevision=world.terrainRevision;}spatial.snapshot(new MapSceneSnapshot(ground,world,selected,moving));spatial.mapLayers(world,flat.territoryMode(),openingPreview,previewFaction);dirty=false;}
    void invalidateScene(){dirty=true;flat.invalidateScene();}
    @Override public void fit(){if(spatial==null)flat.fit();else spatial.fit();}
    @Override public void focus(Hex h){if(spatial==null)flat.focus(h);else spatial.focus(h);}
    @Override public void center(Hex h){if(spatial==null)flat.center(h);else spatial.center(h);}
    @Override public void saveCamera(Bundle b){b.putBoolean("sceneEnabled",is3D());if(spatial==null)flat.saveCamera(b);else spatial.saveCamera(b);}
    @Override public void restoreCamera(Bundle b){camera=new Bundle(b);if(!safeMode&&Boolean.TRUE.equals(b.get("sceneEnabled"))&&spatial==null)switchMode(true);if(spatial==null)flat.restoreCamera(b);else spatial.restoreCamera(b);}
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);if(flat!=null)flat.setEnabled(enabled);if(spatial!=null)spatial.setEnabled(enabled);}
    void setUnitDrop(Consumer<MarchOrders.Plan> drop){flat.setUnitDrop(drop);}
    void setRoute(MarchOrders.Plan value){route=value;flat.setRoute(value);if(spatial!=null)spatial.setRoute(value);}
    void setPickTargets(Set<Hex> value){targets=value;flat.setPickTargets(value);if(spatial!=null)spatial.setTargets(value);}
    void setTacticPreview(Displacement.Preview value){tacticPreview=value;flat.setTacticPreview(value);if(spatial!=null)spatial.setTacticPreview(value);}
    void battleFeedback(World.Result result,boolean haptics){if(spatial==null)flat.battleFeedback(result,haptics);else if(haptics&&result.feedback!=World.Feedback.NONE)performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);}
    void setPanelOcclusion(int right,int bottom){panelRight=right;panelBottom=bottom;flat.setPanelOcclusion(right,bottom);if(spatial!=null)spatial.setPanelOcclusion(right,bottom);}
    void setTerritoryMode(int value){flat.setTerritoryMode(value);if(spatial!=null&&world!=null)spatial.mapLayers(world,flat.territoryMode(),openingPreview,previewFaction);}
    int territoryMode(){return flat.territoryMode();}
    Territory territory(){if(spatial!=null&&world!=null)flat.setWorld(world,selected,moving);return flat.territory();}
    void toggleNavigator(){if(spatial!=null){spatial.fit();return;}flat.toggleNavigator();}
    boolean commandersShown(){return flat.commandersShown();}
    boolean unitBarsShown(){return flat.unitBarsShown();}
    void setCommandersShown(boolean value){flat.setCommandersShown(value);if(spatial!=null)spatial.labels(value,flat.unitBarsShown());}
    void setUnitBarsShown(boolean value){flat.setUnitBarsShown(value);if(spatial!=null)spatial.labels(flat.commandersShown(),value);}
    void setCriticalSkip(Runnable skip){criticalSkip=skip;flat.setCriticalSkip(skip);if(spatial!=null)spatial.criticalSkip(skip);}
    void criticalFrame(CriticalHit hit,float phase){if(spatial==null)flat.criticalFrame(hit,phase);else spatial.critical(world,hit,phase);}
    void replayFrame(TurnJournal.Event e,float fraction){if(spatial==null)flat.replayFrame(e,fraction);else spatial.replay(e,fraction);}
    boolean replayVisible(TurnJournal.Event e){return spatial==null?flat.replayVisible(e):spatial.visible(e);}
}
