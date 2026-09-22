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
    private boolean dirty=true,diagnostics,resumed=true;
    private Bundle camera=new Bundle();
    private Set<Hex> targets=Collections.emptySet();
    private MarchOrders.Plan route;
    MapHost(Context context,MapView.TileListener listener){
        super(context);this.listener=listener;prefs=context.getSharedPreferences("map-renderer",Context.MODE_PRIVATE);
        flat=new MapView(context,listener);addView(flat,new LayoutParams(-1,-1));
        // Default stays 2D. An interrupted native session is never restarted automatically.
        if(prefs.getBoolean("nativeSession",false))Toast.makeText(context,"上次 3D 会话未正常结束，已安全回到 2D；可在视图中手动重试",Toast.LENGTH_LONG).show();
    }
    boolean is3D(){return spatial!=null;}
    void switchMode(boolean use3D){
        if(use3D==is3D()||world==null)return;
        saveCamera(camera);
        if(!use3D){leave3D();flat.setWorld(world,selected,moving);flat.restoreCamera(camera);return;}
        // Synchronous commit precedes native library load; catches native crashes next launch.
        if(!prefs.edit().putBoolean("nativeSession",true).commit()){Toast.makeText(getContext(),"无法保存 3D 启动健康标记，保留 2D",Toast.LENGTH_LONG).show();return;}
        try{
            spatial=new FilamentMapView(getContext(),listener,this::fallback);
            removeView(flat);addView(spatial,new LayoutParams(-1,-1));
            dirty=true;publish();spatial.restoreCamera(camera);spatial.setTargets(targets);spatial.setRoute(route);spatial.resume(resumed);spatial.diagnostics(diagnostics);
        }catch(Exception|LinkageError e){fallback(e);}
    }
    private void fallback(Throwable e){android.util.Log.e("MapRenderer","Filament fallback to 2D",e);leave3D();if(world!=null)flat.setWorld(world,selected,moving);flat.restoreCamera(camera);Toast.makeText(getContext(),"3D 初始化或渲染失败，已返回 2D："+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}
    private void leave3D(){if(spatial!=null){spatial.release();removeView(spatial);spatial=null;}if(flat.getParent()==null)addView(flat,new LayoutParams(-1,-1));prefs.edit().putBoolean("nativeSession",false).commit();}
    void release(){if(spatial!=null){spatial.release();removeView(spatial);spatial=null;prefs.edit().putBoolean("nativeSession",false).commit();}}
    void resume(boolean value){resumed=value;if(spatial!=null)spatial.resume(value);}
    void toggleDiagnostics(){diagnostics=!diagnostics;if(spatial!=null)spatial.diagnostics(diagnostics);android.util.Log.i("MapRenderer",report());}
    String report(){return spatial==null?"2D · "+getWidth()+" × "+getHeight():spatial.report();}
    @Override public void setWorld(World w,Hex s,int moving){
        boolean changed=world!=w||revision!=w.commandRevision()||turn!=w.turn||player!=w.player||terrainRevision!=w.terrainRevision||!Objects.equals(selected,s)||this.moving!=moving;
        world=w;selected=s;this.moving=moving;revision=w.commandRevision();turn=w.turn;player=w.player;
        if(spatial==null)flat.setWorld(w,s,moving);else if(changed||dirty)publish();
    }
    private void publish(){if(spatial==null||world==null)return;if(ground==null||groundWorld!=world||terrainRevision!=world.terrainRevision){if(ground==null||!ground.matches(world))ground=new MapSceneSnapshot.Ground(world);groundWorld=world;terrainRevision=world.terrainRevision;}spatial.snapshot(new MapSceneSnapshot(ground,world,selected,moving));dirty=false;}
    void invalidateScene(){dirty=true;flat.invalidateScene();}
    @Override public void fit(){if(spatial==null)flat.fit();else spatial.fit();}
    @Override public void focus(Hex h){if(spatial==null)flat.focus(h);else spatial.focus(h);}
    @Override public void center(Hex h){if(spatial==null)flat.center(h);else spatial.center(h);}
    @Override public void saveCamera(Bundle b){if(spatial==null)flat.saveCamera(b);else spatial.saveCamera(b);}
    @Override public void restoreCamera(Bundle b){camera=new Bundle(b);if(spatial==null)flat.restoreCamera(b);else spatial.restoreCamera(b);}
    @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);if(flat!=null)flat.setEnabled(enabled);if(spatial!=null)spatial.setEnabled(enabled);}
    void setUnitDrop(Consumer<MarchOrders.Plan> drop){flat.setUnitDrop(drop);}
    void setRoute(MarchOrders.Plan value){route=value;flat.setRoute(value);if(spatial!=null)spatial.setRoute(value);}
    void setPickTargets(Set<Hex> value){targets=value;flat.setPickTargets(value);if(spatial!=null)spatial.setTargets(value);}
    void setTacticPreview(Displacement.Preview value){if(value!=null&&spatial!=null)switchMode(false);flat.setTacticPreview(value);}
    void battleFeedback(World.Result result,boolean haptics){if(spatial==null)flat.battleFeedback(result,haptics);else if(haptics&&result.feedback!=World.Feedback.NONE)performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);}
    void setPanelOcclusion(int right,int bottom){flat.setPanelOcclusion(right,bottom);}
    void setTerritoryMode(int value){if(spatial!=null)switchMode(false);flat.setTerritoryMode(value);}
    int territoryMode(){return flat.territoryMode();}
    Territory territory(){if(spatial!=null&&world!=null)flat.setWorld(world,selected,moving);return flat.territory();}
    void toggleNavigator(){if(spatial!=null){switchMode(false);Toast.makeText(getContext(),"导航图使用 2D 视图",Toast.LENGTH_SHORT).show();}flat.toggleNavigator();}
    boolean commandersShown(){return flat.commandersShown();}
    boolean unitBarsShown(){return flat.unitBarsShown();}
    void setCommandersShown(boolean value){if(spatial!=null)switchMode(false);flat.setCommandersShown(value);}
    void setUnitBarsShown(boolean value){if(spatial!=null)switchMode(false);flat.setUnitBarsShown(value);}
    void setCriticalSkip(Runnable skip){flat.setCriticalSkip(skip);}
    void criticalFrame(CriticalHit hit,float phase){if(spatial==null)flat.criticalFrame(hit,phase);}
    void replayFrame(TurnJournal.Event e,float fraction){if(spatial==null)flat.replayFrame(e,fraction);else spatial.replay(e,fraction);}
    boolean replayVisible(TurnJournal.Event e){return spatial==null?flat.replayVisible(e):spatial.visible(e);}
}
