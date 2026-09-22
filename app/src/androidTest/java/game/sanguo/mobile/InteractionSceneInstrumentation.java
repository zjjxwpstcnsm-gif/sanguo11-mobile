package game.sanguo.mobile;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.MotionEvent;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;

/** Installed production interaction, preferences and isolated editor roundtrip. */
public final class InteractionSceneInstrumentation extends SceneInstrumentation {
    @Override public void onStart(){Bundle result=new Bundle();ParcelFileDescriptor recording=null;try{
        World w=ScenarioCatalog.all().get(0);try(OutputStream out=getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(w));}
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putString("tilt","corrupt-old-type").putFloat("facing",Float.NaN).putString("nativeSession","corrupt").commit();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        host=(MapHost)field(activity,"map");world=(World)field(activity,"world");
        runOnMainSync(()->host.switchMode(true));settle();ready();check(host.is3D(),"bad preferences recover without clearing application data");
        recording=getUiAutomation().executeShellCommand("screenrecord --time-limit 180 /sdcard/interaction-scene.mp4");
        FilamentMapView view=(FilamentMapView)field(host,"spatial");
        runOnMainSync(()->{host.setCommandersShown(false);host.setUnitBarsShown(false);host.setTerritoryMode(1);host.resetOrientation();});
        check(host.is3D(),"display toggles retain 3D");runOnMainSync(()->host.setTerritoryMode(0));
        runOnMainSync(()->{Bundle bad=new Bundle();bad.putFloat("sceneSpan",Float.NaN);bad.putFloat("sceneTilt",Float.POSITIVE_INFINITY);view.restoreCamera(bad);});
        check(Float.isFinite(view.camera.span)&&view.camera.tilt==55,"bad camera bundle sanitized");
        runOnMainSync(()->{host.focus(world.home().hex);host.reverseOrientation();host.resume(false);host.resume(true);});
        check(view.camera.facing==-1,"orientation retained across pause");capture("s07-camera");
        final ScenarioFactionPicker[] picker=new ScenarioFactionPicker[1];String scenario=world.scenarioId;World previous=world;
        runOnMainSync(()->{picker[0]=new ScenarioFactionPicker(activity,previous,side->invoke("startScenario",new Class<?>[]{String.class,int.class,MapPatch.class},scenario,side,null));picker[0].show();});settle();
        MapHost previewHost=(MapHost)field(picker[0],"map");check(previewHost.is3D(),"new-game faction preview inherits current 3D mode");
        runOnMainSync(()->{try{((android.widget.Button)field(picker[0],"start")).performClick();}catch(Exception e){throw new RuntimeException(e);}});
        long startDeadline=SystemClock.uptimeMillis()+60000;while(field(activity,"world")==previous&&SystemClock.uptimeMillis()<startDeadline)settle();world=(World)field(activity,"world");
        check(world!=previous&&host.is3D(),"confirming faction starts new scenario and preserves 3D");
        check(Boolean.TRUE.equals(getTargetContext().getSharedPreferences("map-renderer",0).getAll().get("nativeSession")),"closing preview does not clear active game's native health mark");
        commandFlow();
        MapPatch p=CustomMaps.base().fresh();MapEditSession edit=new MapEditSession(p);Hex chosen=null;
        for(int x=30;x<160&&chosen==null;x++)for(int y=30;y<160;y++){Hex h=MapCoordinates.fromNationalSource(edit.world(),new SourceGridCoord(x,y));if(!edit.protectedAt(h)&&edit.world().terrain[h.q][h.r]==World.Terrain.PLAIN){chosen=h;break;}}
        SourceGridCoord source=MapCoordinates.nationalSource(edit.world(),chosen);p.heights.put(source.x*200+source.y,2200);p.appearances.put(edit.world().home().id,new MapPatch.Appearance(2,90));
        MapLibrary library=new MapLibrary(getTargetContext());library.saveDraft(p);
        MapEditorActivity editor=(MapEditorActivity)startActivitySync(new Intent(getTargetContext(),MapEditorActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        long deadline=SystemClock.uptimeMillis()+60000;while((field(editor,"session")==null||(Boolean)field(editor,"busy"))&&SystemClock.uptimeMillis()<deadline)settle();
        MapHost editorHost=(MapHost)field(editor,"map");Hex tile=chosen;
        runOnMainSync(()->{editorHost.switchMode(true);editorHost.focus(tile);});settle();check(editorHost.is3D(),"existing production editor enters 3D");
        MapHost gameHost=host;host=editorHost;ready();FilamentMapView editorView=(FilamentMapView)field(host,"spatial");
        runOnMainSync(()->{try{java.lang.reflect.Field brush=MapEditorActivity.class.getDeclaredField("brush");brush.setAccessible(true);brush.set(editor,World.Terrain.FOREST);}catch(Exception e){throw new RuntimeException(e);}editorHost.editorDrawing(true);long time=SystemClock.uptimeMillis();MapSceneSnapshot snap;try{snap=(MapSceneSnapshot)field(editorView,"snapshot");}catch(Exception e){throw new RuntimeException(e);}float x=editorView.camera.screenX(snap.ground.grid.x(tile)),y=editorView.camera.screenY(snap.ground.grid.z(tile),snap.ground.surface.at(tile));MotionEvent down=MotionEvent.obtain(time,time,0,x,y,0),up=MotionEvent.obtain(time,time+40,1,x,y,0);editorView.onTouchEvent(down);editorView.onTouchEvent(up);down.recycle();up.recycle();});
        deadline=SystemClock.uptimeMillis()+60000;while((Boolean)field(editor,"busy")&&SystemClock.uptimeMillis()<deadline)settle();
        MapEditSession actual=(MapEditSession)field(editor,"session");check(actual.world()!=world,"editor isolated from campaign");check(actual.world().terrain[tile.q][tile.r]==World.Terrain.FOREST,"actual 3D brush changes selected terrain");check(!actual.patch().heights.containsKey(source.x*200+source.y),"brush invalidates height override");actual.undo();check(actual.patch().heights.get(source.x*200+source.y)==2200,"editor undo restores height");actual.redo();
        capture("s07-editor-3d");surfaceCapture();
        MapPatch roundtrip=MapPatch.decode(actual.patch().encode());check(roundtrip.appearances.equals(p.appearances),"installed editor retains imported model metadata");
        File cleanDir=new File(getTargetContext().getFilesDir(),"s07-clean-import-"+SystemClock.uptimeMillis());check(cleanDir.mkdirs(),"new empty import data directory");
        Context cleanContext=new ContextWrapper(getTargetContext()){@Override public File getFilesDir(){return cleanDir;}};
        MapLibrary cleanLibrary=new MapLibrary(cleanContext);check(cleanLibrary.entries().isEmpty(),"clean map library has no previous metadata");
        MapPatch published=cleanLibrary.publish(MapPatch.read(new ByteArrayInputStream(roundtrip.encode())));MapPatch loaded=cleanLibrary.load(new MapLibrary.Entry(published.id,published.revision,published.name));
        World custom=CustomMaps.load(loaded,loaded.preview,0,123);library.rememberVisual(custom);
        World save=SaveCodec.decode(SaveCodec.encode(custom));check(library.visual(save)!=null,"visual sidecar restored for unchanged strategic save");
        check(MapPatch.decode(loaded.encode()).heights.equals(loaded.heights),"export/import visual roundtrip");
        runOnMainSync(editor::finish);host=gameHost;settle();check(host.is3D(),"return from editor retains gameplay 3D");
        runOnMainSync(()->{invoke("activateWorld",new Class<?>[]{World.class},custom);activity.refresh();host.focus(tile);});world=custom;settle();ready();check(host.is3D(),"imported custom map actually playable in same production 3D host");capture("s07-imported-custom-game");
        result.putString("stream","PASS S07 "+checks+" installed checks; command APIs + actual editor touch, not complete manual UI acceptance\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL S07 "+e+"\n"+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}finally{if(recording!=null)try{recording.close();}catch(IOException ignored){}}}
}
