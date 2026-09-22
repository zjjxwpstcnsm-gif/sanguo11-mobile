package game.sanguo.mobile;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import game.sanguo.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.*;

/** Installed tests use a dedicated emulator and production Activities/AtomicFile. */
final class MapEditor67Probe {
    final MapEditor67Instrumentation test;int checks;final StringBuilder report=new StringBuilder();MapEditorActivity editor;
    MapEditor67Probe(MapEditor67Instrumentation test){this.test=test;}
    interface Condition{boolean ready()throws Exception;}
    interface UiAction{void run()throws Exception;}
    void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);report.append("PASS ").append(message).append('\n');}
    File directory(){File dir=new File(test.getTargetContext().getExternalFilesDir(null),"editor67");dir.mkdirs();return dir;}
    void run()throws Exception {
        try{
            MapLibrary library=new MapLibrary(test.getTargetContext());require(library.entries().isEmpty(),"dedicated fresh emulator library");
            test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();shot("01-editor-entry");
            click("地图编辑器 · 自定义地图",true);await(()->test.current instanceof MapEditorActivity,30000);editor=(MapEditorActivity)test.current;ready();require(session().world().sourceColumns()==200,"actual editor shares native 200 grid");shot("02-independent-editor");
            Hex at=null;World world=session().world();for(int x=30;x<160&&at==null;x++)for(int y=30;y<160;y++){Hex h=MapCoordinates.fromNationalSource(world,new SourceGridCoord(x,y));if(!session().protectedAt(h)&&world.terrain[h.q][h.r]==World.Terrain.PLAIN){at=h;break;}}
            require(at!=null,"editable production tile");final Hex target=at;ui(()->map().focus(target));settle();
            click(TerrainPresentation.of(World.Terrain.PLAIN).name(),true);click(TerrainPresentation.of(World.Terrain.FOREST).name(),true);click("浏览",true);click("范围 1格",true);
            String original=session().patch().fingerprint();tap(target);ready();require(!session().patch().terrain.isEmpty(),"actual one-finger brush commits terrain");String painted=session().patch().fingerprint();shot("03-real-brush");
            click("撤销",true);ready();require(original.equals(session().patch().fingerprint()),"actual undo control restores entire stroke");click("重做",true);ready();require(painted.equals(session().patch().fingerprint()),"actual redo control restores entire stroke");
            cancelWithSecondPointer(target);ready();require(painted.equals(session().patch().fingerprint()),"second pointer cancels entire pending brush");
            MapPatch expected=session().patch();ui(editor::finish);await(()->test.current instanceof MainActivity,30000);settle();require(Arrays.equals(new MapLibrary(test.getTargetContext()).draft().encode(),expected.encode()),"reopening disk store restores actual autosaved draft");
            MapPatch fixture;try(InputStream in=new FileInputStream(new File(directory(),"fixture.json"))){fixture=MapPatch.read(in);}MapPatch published=library.publish(fixture);require(published.revision>=1,"real Android library publishes validated map");
            File root=new File(test.getTargetContext().getFilesDir(),"custom-maps-v1"),version=new File(root,published.id+"-r"+published.revision+".json");require(version.renameTo(new File(version+".bak")),"simulate interrupted AtomicFile write");
            require(new MapLibrary(test.getTargetContext()).entries().size()==1&&version.exists(),"backup-only revision recovered and listed");require(!library.checkImport(published).forkRequired(),"identical reimport remains idempotent");MapPatch conflict=published.copy();conflict.name="不同内容";require(library.checkImport(conflict).forkRequired(),"same identity conflict requires explicit fork");
            byte[] oldSave=SaveCodec.encode(CustomMaps.load(published,published.preview,0,17L));library.delete(published.id);require(library.entries().isEmpty(),"atomic tombstone hides all revisions");require(library.checkImport(published).forkRequired(),"deleted identity cannot be reused by import");MapPatch second=library.publish(published);require(second.revision>published.revision,"republishing old draft allocates newer revision");require(SaveCodec.decode(oldSave).customMapRevision==published.revision,"library deletion does not break pinned campaign save");
            library.saveDraft(second);click("地图编辑器 · 自定义地图",true);await(()->test.current instanceof MapEditorActivity,30000);editor=(MapEditorActivity)test.current;ready();require(session().world().cities.size()==90,"three additional sites in actual editor world");
            World.City city=session().world().city(100006701);ui(()->map().focus(city.hex));settle();for(Hex h:SiteFootprint.cells(city)){tap(h);ready();require(h.equals(field(editor,"selected")),"actual touch selects each of seven city cells");}shot("04-seven-cell-city");
            tap(city.hex);click("据点",true);click("属性",true);awaitText(city.name,false);shot("05-real-site-properties");test.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();
            MapPatch.Site definition=CustomMaps.sites(session().patch()).get(city.id);MapPatch.Site destination=null;
            for(int dx=5;dx<35&&destination==null;dx++)for(int dy=-8;dy<=8;dy++){int x=definition.x()+dx,y=definition.y()+dy;if(x>=200||y<0||y>=200)continue;MapPatch.Site candidate=definition.at(x,y);if(CustomMaps.placement(session().world(),candidate,city.id)==null){destination=candidate;break;}}
            require(destination!=null,"legal city move destination");final MapPatch.Site moving=destination;click("移动",true);ui(()->map().focus(moving.hex(session().world())));settle();tap(moving.hex(session().world()));click("确认位置",true);ready();require(session().world().cityAt(city.hex)==null,"actual move clears old city footprint");require(session().world().city(city.id).hex.equals(moving.hex(session().world())),"actual move changes entity position");shot("06-moved-city");click("撤销",true);ready();require(session().world().city(city.id).hex.equals(city.hex),"actual move undo restores city");
            ui(editor::finish);await(()->test.current instanceof MainActivity,30000);settle();click("新建游戏 · 选择剧本",true);click("250 幻想群雄争霸",false);click(new MapLibrary.Entry(second.id,second.revision,second.name).label(),true);awaitText("开始新局",false);shot("07-custom-map-faction-picker");click("开始新局",false);click("执行",true);
            await(()->test.current instanceof MainActivity&&field(test.current,"world")!=null&&!((World)field(test.current,"world")).customMapId.isEmpty(),120000);settle();MainActivity playing=(MainActivity)test.current;World campaign=(World)field(playing,"world");require(campaign.customMapId.equals(second.id)&&campaign.customMapRevision==second.revision,"actual new-game chooser loads selected immutable map");require(campaign.city(100006701)!=null&&campaign.city(100006702)!=null&&campaign.city(100006703)!=null,"real game includes new city port gate");shot("08-custom-campaign");
            ui(()->playing.openRealmPage("cities",-1));settle();shot("09-production-site-list");require(((World)field(playing,"world")).cities.size()==90,"real list backed by extended campaign roster");
            MapPatch changed=second.copy();changed.name="编辑器后续修订";changed.revision++;library.saveDraft(changed);World pinned=SaveCodec.decode(oldSave);require(pinned.customMapRevision==published.revision&&pinned.city(100006701)!=null,"later draft cannot alter pinned save");require(ScenarioCatalog.load("heroes-250",0).cities.size()==87,"packaged original unchanged");
            File exported=new File(directory(),"phone-roundtrip.json");try(OutputStream out=new FileOutputStream(exported)){out.write(session().patch().encode());}try(InputStream in=new FileInputStream(exported)){require(Arrays.equals(MapPatch.read(in).encode(),session().patch().encode()),"Android-generated JSON round trip");}
            report.insert(0,"MAP_EDITOR67 ANDROID PASS "+checks+" checks\n");
        }finally{try{shot("last-window");}catch(Exception ignored){}Files.write(new File(directory(),"checks.txt").toPath(),report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    }
    MapEditSession session()throws Exception{return (MapEditSession)field(editor,"session");}
    MapView map()throws Exception{return (MapView)field(field(editor,"map"),"flat");}
    void ready()throws Exception{await(()->field(editor,"session")!=null&&!((Boolean)field(editor,"busy")),120000);settle();}
    static Object field(Object object,String name)throws Exception{Class<?> type=object.getClass();while(type!=null){try{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException e){type=type.getSuperclass();}}throw new NoSuchFieldException(name);}
    Object call(Object object,String name,Class<?>[] types,Object... args)throws Exception{Method m=object.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(object,args);}
    void ui(UiAction action)throws Exception{Throwable[] error={null};test.runOnMainSync(()->{try{action.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new RuntimeException(error[0]);}
    void settle(){test.waitForIdleSync();SystemClock.sleep(300);test.waitForIdleSync();}
    void await(Condition condition,long timeout)throws Exception{long until=SystemClock.uptimeMillis()+timeout;while(SystemClock.uptimeMillis()<until){if(condition.ready())return;SystemClock.sleep(100);}throw new AssertionError("Timed out; window="+texts(test.getUiAutomation().getRootInActiveWindow()));}
    AccessibilityNodeInfo find(AccessibilityNodeInfo root,String text,boolean exact){return find(root,text,exact,true);}
    AccessibilityNodeInfo find(AccessibilityNodeInfo root,String text,boolean exact,boolean visible){if(root==null)return null;String value=root.getText()==null?"":root.getText().toString();if((!visible||root.isVisibleToUser())&&(exact?value.equals(text):value.contains(text)))return root;for(int i=0;i<root.getChildCount();i++){AccessibilityNodeInfo n=find(root.getChild(i),text,exact,visible);if(n!=null)return n;}return null;}
    String texts(AccessibilityNodeInfo n){if(n==null)return "";StringBuilder s=new StringBuilder(n.getText()==null?"":n.getText());for(int i=0;i<n.getChildCount();i++)s.append(' ').append(texts(n.getChild(i)));return s.toString();}
    void awaitText(String text,boolean exact)throws Exception{await(()->{AccessibilityNodeInfo root=test.getUiAutomation().getRootInActiveWindow();if(find(root,text,exact)!=null)return true;AccessibilityNodeInfo hidden=find(root,text,exact,false);if(hidden!=null)hidden.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId());return false;},120000);}
    void click(String text,boolean exact)throws Exception{awaitText(text,exact);AccessibilityNodeInfo node=find(test.getUiAutomation().getRootInActiveWindow(),text,exact);while(node!=null&&!node.isClickable())node=node.getParent();if(node==null||!node.performAction(AccessibilityNodeInfo.ACTION_CLICK))throw new AssertionError("Cannot click "+text);settle();}
    float[] point(Hex h)throws Exception{MapView map=map();MapCamera camera=(MapCamera)field(map,"camera");float x=(Float)call(map,"x",new Class<?>[]{Hex.class},h),y=(Float)call(map,"y",new Class<?>[]{Hex.class},h);int[] location=new int[2];ui(()->map.getLocationOnScreen(location));return new float[]{location[0]+x*camera.scale+camera.x,location[1]+y*camera.scale+camera.y};}
    void tap(Hex h)throws Exception{float[] p=point(h);long time=SystemClock.uptimeMillis();send(MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,p[0],p[1],0));send(MotionEvent.obtain(time,time+50,MotionEvent.ACTION_UP,p[0],p[1],0));settle();}
    void send(MotionEvent event){event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);test.sendPointerSync(event);event.recycle();}
    void cancelWithSecondPointer(Hex h)throws Exception{float[] p=point(h);long now=SystemClock.uptimeMillis();send(MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,p[0],p[1],0));MotionEvent.PointerProperties[] props={new MotionEvent.PointerProperties(),new MotionEvent.PointerProperties()};MotionEvent.PointerCoords[] coords={new MotionEvent.PointerCoords(),new MotionEvent.PointerCoords()};for(int i=0;i<2;i++){props[i].id=i;props[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i].x=p[0]+i*30;coords[i].y=p[1]+i*30;coords[i].pressure=1;coords[i].size=1;}
        send(MotionEvent.obtain(now,now+30,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,props,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0));send(MotionEvent.obtain(now,now+60,MotionEvent.ACTION_CANCEL,2,props,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0));settle();}
    void shot(String name)throws Exception{Bitmap bitmap=test.getUiAutomation().takeScreenshot();if(bitmap==null)throw new IOException("Screenshot unavailable");try(OutputStream out=new FileOutputStream(new File(directory(),name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
}
