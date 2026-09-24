package game.sanguo.mobile;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import game.sanguo.core.*;
import game.sanguo.core.map.SourceGridCoord;
import java.io.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Callable;

/** Real isolated editor, durable library and normal campaign host. Fixture additions are labelled. */
public final class NativeR13Instrumentation extends SceneInstrumentation {
    private MapEditorActivity editor;private File dir;
    interface Ui {void run()throws Exception;}
    static void writeText(Path path,String text)throws IOException{Files.write(path,text.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    void ui(Ui r){runOnMainSync(()->{try{r.run();}catch(Exception e){throw new RuntimeException(e);}});}
    MapEditSession session()throws Exception{return (MapEditSession)field(editor,"session");}
    void call(Object target,String name,Class<?>[] types,Object... args)throws Exception{Method m=target.getClass().getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(target,args);}
    void editorReady()throws Exception{long end=SystemClock.uptimeMillis()+120000;while(SystemClock.uptimeMillis()<end){boolean[] done={false};ui(()->done[0]=field(editor,"session")!=null&&!(Boolean)field(editor,"busy"));if(done[0])return;settle();}throw new AssertionError("editor transaction timeout");}
    void edit(String name,Callable<?> action)throws Exception{ui(()->call(editor,"change",new Class<?>[]{String.class,Callable.class},name,action));editorReady();check(!session().dirty(),"durable "+name);}
    void shot(String name)throws Exception{ready();surfaceCapture();capture(name+"-ui");Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);writeText(new File(dir,name+".txt").toPath(),BuildConfig.SOURCE_REVISION+"\n"+host.report());}
    void tap(FilamentMapView view,Hex h)throws Exception{float x=view.camera.screenX(viewSnapshot(view).ground.grid.x(h),viewSnapshot(view).ground.grid.z(h)),y=view.camera.screenY(viewSnapshot(view).ground.grid.x(h),viewSnapshot(view).ground.grid.z(h),viewSnapshot(view).ground.surface.at(h));ui(()->{long now=SystemClock.uptimeMillis();MotionEvent d=MotionEvent.obtain(now,now,0,x,y,0),u=MotionEvent.obtain(now,now+80,1,x,y,0);view.dispatchTouchEvent(d);view.dispatchTouchEvent(u);d.recycle();u.recycle();});}
    MapSceneSnapshot viewSnapshot(FilamentMapView v)throws Exception{return (MapSceneSnapshot)field(v,"snapshot");}
    void siteCrud(int id)throws Exception{
        MapPatch.Site definition=CustomMaps.sites(session().patch()).get(id);
        edit("rename "+id,()->{session().replace("rename",session().putSite(definition.renamed(definition.name()+"改")));return null;});
        MapPatch.Site destination=null;
        for(int radius=1;radius<=24&&destination==null;radius++)for(int dx=-radius;dx<=radius&&destination==null;dx++)for(int dy=-radius;dy<=radius;dy++){
            if(Math.max(Math.abs(dx),Math.abs(dy))!=radius)continue;
            MapPatch.Site candidate=definition.at(definition.x()+dx,definition.y()+dy);
            if(candidate.x()<0||candidate.y()<0||candidate.x()>199||candidate.y()>199)continue;
            if(CustomMaps.placement(session().world(),candidate,id)==null){destination=candidate;break;}
        }
        check(destination!=null,"legal move for "+definition.kind());final MapPatch.Site moved=destination;
        edit("move "+id,()->{session().replace("move",session().putSite(moved));return null;});
        check(session().world().city(id).hex.equals(moved.hex(session().world())),"real moved site "+id);
        ui(()->host.focus(session().world().city(id).hex));shot("r13-moved-"+id);
        edit("undo move "+id,()->{session().undo();return null;});
        MapPatch deleted=CustomMaps.deletionProposal(session().patch(),id,session().world().home().id);
        edit("delete "+id,()->{session().replace("delete",deleted);return null;});ready();
        check(session().world().city(id)==null&&!((Map<?,?>)field(field(host,"spatial"),"objects")).containsKey("site:"+id),"deleted authority and native proxy "+id);
        edit("undo delete "+id,()->{session().undo();return null;});
        edit("redo delete "+id,()->{session().redo();return null;});check(session().world().city(id)==null,"redo site removal "+id);
        edit("restore "+id,()->{session().undo();return null;});check(session().world().city(id)!=null,"site restored "+id);
    }
    @Override public void onStart(){Bundle result=new Bundle();try{
        dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();MapLibrary library=new MapLibrary(getTargetContext());
        MapPatch fixture;try(InputStream in=new FileInputStream(new File(getTargetContext().getExternalFilesDir(null),"editor67/fixture.json"))){fixture=MapPatch.read(in);}library.saveDraft(fixture);
        getTargetContext().getSharedPreferences("map-renderer",0).edit().putBoolean("enabled",false).commit();
        editor=(MapEditorActivity)startActivitySync(new Intent(getTargetContext(),MapEditorActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));editorReady();host=(MapHost)field(editor,"map");
        Hex chosen=null;for(int x=40;x<150&&chosen==null;x++)for(int y=40;y<150;y++){Hex h=MapCoordinates.fromNationalSource(session().world(),new SourceGridCoord(x,y));if(!session().protectedAt(h)&&session().world().terrain[h.q][h.r]==World.Terrain.PLAIN){chosen=h;break;}}
        check(chosen!=null,"editable fixture land");final Hex cell=chosen;
        ui(()->{host.switchMode(true);host.focus(cell);((FilamentMapView)field(host,"spatial")).camera.span=5;});ready();
        FilamentMapView view=(FilamentMapView)field(host,"spatial");Object swap=field(view,"swap");byte[] original=session().patch().encode();
        ui(()->{Field d=MapEditorActivity.class.getDeclaredField("drawing");d.setAccessible(true);d.setBoolean(editor,true);host.editorDrawing(true);view.camera.yaw=47;view.camera.tilt=55;Field b=MapEditorActivity.class.getDeclaredField("brush");b.setAccessible(true);b.set(editor,World.Terrain.FOREST);});
        shot("r13-before-brush");
        check(cell.equals(view.pick(view.camera.screenX(viewSnapshot(view).ground.grid.x(cell),viewSnapshot(view).ground.grid.z(cell)),view.camera.screenY(viewSnapshot(view).ground.grid.x(cell),viewSnapshot(view).ground.grid.z(cell),viewSnapshot(view).ground.surface.at(cell)),false)),"brush oracle hits intended visible cell");
        tap(view,cell);editorReady();check(session().world().terrain[cell.q][cell.r]==World.Terrain.FOREST,"actual rotated 3D touch paints correct cell");ready();check(field(view,"swap")==swap,"edit retains same Surface swapchain");shot("r13-native-brush");
        edit("undo",()->{session().undo();return null;});check(Arrays.equals(original,session().patch().encode()),"native brush undo exact");
        edit("redo",()->{session().redo();return null;});
        ui(()->host.switchMode(false));check(((World)field(field(host,"flat"),"world")).terrain[cell.q][cell.r]==World.Terrain.FOREST,"3D edit reaches 2D");ui(()->host.switchMode(true));ready();
        SourceGridCoord source=MapCoordinates.nationalSource(session().world(),cell);MapPatch visual=session().patch();visual.heights.put(source.x*200+source.y,1400);byte[] before=SaveCodec.encode(session().world());
        edit("visual height",()->{session().replace("height",visual);return null;});check(Arrays.equals(before,SaveCodec.encode(session().world())),"visual edit preserves full save");shot("r13-height");
        for(int id:new int[]{100006701,100006702,100006703}){World.City site=session().world().city(id);ui(()->host.focus(site.hex));shot("r13-custom-site-"+id);check(((Map<?,?>)field(field(host,"spatial"),"objects")).containsKey("site:"+id),"custom entity GPU mapping");}
        for(int id:new int[]{100006701,100006702,100006703})siteCrud(id);
        MapPatch published=library.publish(session().patch());byte[] persisted=session().patch().encode();ui(editor::finish);
        check(Arrays.equals(persisted,library.draft().encode()),"close editor keeps exact draft");
        // Corrupt optional style: fall back to the validated pinned revision, never alter save bytes.
        World pinned=CustomMaps.load(published,published.preview,0,17L);library.rememberVisual(pinned);
        File sidecar=new File(getTargetContext().getFilesDir(),"custom-maps-v1/visual-"+pinned.customMapFingerprint+".json");writeText(sidecar.toPath(),"broken visual JSON");
        byte[] pinnedSave=SaveCodec.encode(pinned);check(library.visual(pinned)!=null,"damaged sidecar falls back to pinned map");check(Arrays.equals(pinnedSave,SaveCodec.encode(pinned)),"style recovery preserves save");
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();host=(MapHost)field(activity,"map");
        ui(()->invoke("startScenario",new Class<?>[]{String.class,int.class,MapPatch.class},published.preview,0,published));
        long deadline=SystemClock.uptimeMillis()+120000;do{settle();ui(()->world=SessionProbe.view(activity));}while((world==null||!world.customMapId.equals(published.id))&&SystemClock.uptimeMillis()<deadline);
        check(world!=null&&world.customMapId.equals(published.id),"normal startScenario installs published custom map");ui(()->{host.switchMode(true);host.focus(world.home().hex);invoke("closePanel",new Class<?>[0]);});shot("r13-custom-campaign");
        commandFlow();shot("r13-custom-after-turn-load");
        result.putString("stream","PASS R13 installed checks="+checks+"; procedural custom fixture; physical/manual/PC art NOT_RUN\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){try{writeText(new File(dir,"r13-failure.txt").toPath(),android.util.Log.getStackTraceString(e));}catch(Exception ignored){}result.putString("stream","FAIL R13 "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
