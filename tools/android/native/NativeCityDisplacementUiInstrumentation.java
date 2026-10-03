package game.sanguo.mobile;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import game.sanguo.core.*;
import game.sanguo.api.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/** Real source-map cavalry ADVANCE through visible 3D controls. Named fixture,
 * ordinary saved world and target APK production rules; no alternate rules. */
public final class NativeCityDisplacementUiInstrumentation extends Instrumentation {
    private MainActivity activity;private File output;private int checks;private boolean boundaryView;private String run="native-city-displacement-r28";private final StringBuilder log=new StringBuilder();
    @Override public void onCreate(Bundle args){super.onCreate(args);if(args!=null){run=args.getString("run",run).replaceAll("[^a-zA-Z0-9_-]","_");boundaryView=Boolean.parseBoolean(args.getString("boundaryView","false"));}start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        output=new File(getTargetContext().getExternalFilesDir("rule-ui"),run);output.mkdirs();
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
        byte[] original=capture();Files.write(new File(output,"initial-capture.sg11").toPath(),original);Files.copy(new File(activity.getFilesDir(),"manual3.sg11").toPath(),new File(output,"fixture-slot3.sg11").toPath(),StandardCopyOption.REPLACE_EXISTING);byte[] fixture=Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath());check((fixture[7]&255)==35&&Arrays.equals(original,SaveCodec.encode(SaveCodec.decode(fixture))),"named source-map v35 fixture migrates through the normal app save path to exact current authority/RNG");
        World initial=world();World.Unit actor=initial.unit(1),target=initial.unit(2);check(actor!=null&&target!=null&&actor.weapon==World.Weapon.CAVALRY,"actual cavalry and defender authority exists");
        Displacement.Preview expectedPreview=initial.war.tacticPreview(1,2,War.Tactic.ADVANCE);Hex blocked=expectedPreview.blocked,stop=expectedPreview.targetPath.get(expectedPreview.targetPath.size()-1);World.City city=initial.cityAt(blocked);
        check(expectedPreview.valid()&&city!=null&&city.owner==target.owner&&SiteFootprint.cells(city).size()==7&&initial.cityAt(target.hex)==null&&initial.cityAt(stop)==null,"actual enemy-owned seven-cell city blocks second push step");
        check(expectedPreview.targetPath.size()==2,"native source-map fixture exercises partial two-step ADVANCE");
        World reference=SaveCodec.decode(original);check(reference.war.tactic(1,2,War.Tactic.ADVANCE).ok&&reference.unit(2).hex.equals(stop),"detached ordinary command control stops outside city");byte[] committed=SaveCodec.encode(reference);
        nav("地图");ui(()->activity.selectAndFocus(actor.hex));settle();MapHost host=(MapHost)field(activity,"map");waitNative(host);check(host.sourceVisuals(),"actual PC source terrain rendered by pure 3D host");
        int prior=battlePhases();text("战法");tap(awaitText("突进 ·"));tapHex(target.hex);
        Displacement.Preview shown=(Displacement.Preview)field(activity,"tacticPreview");check(shown!=null&&shown.valid()&&blocked.equals(shown.blocked)&&shown.targetPath.equals(expectedPreview.targetPath),"real map target preview stops at complete authority city footprint");
        check(Arrays.equals(original,capture())&&battlePhases()==prior,"preview preserves full authority/RNG and emits no combat sound");shot("01-source-city-seven-preview");text("取消");check(Arrays.equals(original,capture())&&battlePhases()==prior,"visible cancel leaves full state and combat sound unchanged");
        tapHex(target.hex);long revision=activity.deploymentState().revision;tap(await(v->v instanceof Button&&"执行".contentEquals(((Button)v).getText())),2);
        long deadline=SystemClock.uptimeMillis()+60000;while(host.commandEffectsActive()&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(100);check(!host.commandEffectsActive(),"actual committed battle presentation completes");
        check(activity.deploymentState().revision==revision+1&&Arrays.equals(committed,capture()),"double real battle submit commits once and matches complete rule/save/RNG control");
        check(world().unit(2).hex.equals(stop)&&world().cityAt(world().unit(2).hex)==null&&world().cityAt(world().unit(1).hex)==null,"both authoritative unit coordinates remain outside every city cell");check(battlePhases()>prior,"committed actual battle consumes a sound journal fact");verifyVisibleAuthority(host,city,committed);shot("02-source-city-seven-committed");if(boundaryView)captureBoundary(host,city,committed);
        nav("菜单");text("保存局面");tap(awaitText("槽位 3"));text("覆盖存档");check(Arrays.equals(committed,Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath())),"normal save writes exact committed city-blocked coordinates and RNG");
        World continuation=SaveCodec.decode(committed);check(continuation.nextTurn().ok,"detached normal full-turn continuation allowed");advance();check(Arrays.equals(SaveCodec.encode(continuation),capture()),"actual next-turn UI follows exact saved post-displacement state and RNG");shot("03-real-turn-after-city-block");
        nav("菜单");text("读取存档");tap(awaitText("槽位 3"));text("读取存档");deadline=SystemClock.uptimeMillis()+90000;while(!Arrays.equals(committed,capture())&&SystemClock.uptimeMillis()<deadline)settle();check(Arrays.equals(committed,capture()),"normal UI load restores complete city-blocked save after a real full turn");shot("04-source-city-seven-restored");
        nav("地图");for(int n=0;n<12&&!exitDialog();n++){sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();}check(exitDialog(),"actual Back reaches normal exit confirmation");text("取消");check(Arrays.equals(committed,capture())&&!activity.isDestroyed(),"cancel actual exit retains complete authority and activity");
        FilamentMapView retiring=(FilamentMapView)field(host,"spatial");SoundEffects sounds=((GameApplication)activity.getApplication()).sounds();sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();check(exitDialog(),"actual Back opens exit confirmation again");text("执行");deadline=SystemClock.uptimeMillis()+30000;while(!activity.isDestroyed()&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(100);check(activity.isDestroyed(),"confirmed actual exit destroys old activity");
        boolean[] released={false,false,false,false};ui(()->{try{released[0]=((GameApplication)activity.getApplication()).host().session()==null;released[1]=(Boolean)field(retiring,"released");released[2]=field(retiring,"engine")==null;released[3]=field(sounds,"pool")==null;}catch(Exception e){throw new RuntimeException(e);}});write("exit-release-observation.txt",Arrays.toString(released));check(released[0]&&released[1]&&released[2]&&released[3],"normal exit closes authority and releases renderer/audio "+Arrays.toString(released));check(Arrays.equals(committed,Files.readAllBytes(new File(getTargetContext().getFilesDir(),"auto.sg11").toPath())),"normal exit auto-save preserves full post-battle load state and RNG");
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));check(Arrays.equals(committed,capture()),"actual reopen restores complete post-battle save");host=(MapHost)field(activity,"map");waitNative(host);check(host.sourceVisuals(),"actual reopen remains source-map pure 3D");deadline=SystemClock.uptimeMillis()+30000;while(!sounds.loaded()&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(100);check(sounds.loaded()&&sounds.active(),"actual reopen reloads active sound resources");int plays=sounds.playedCount();nav("地图");check(sounds.playedCount()>plays,"real UI input after reopen is audible through SoundPool");shot("05-actual-exit-reopen");
        result.putString("stream","NATIVE CITY DISPLACEMENT UI PASS "+checks+" checks\n"+log);write("result.txt",result.getString("stream"));finish(Activity.RESULT_OK,result);
    }catch(Throwable failure){try{shot("FAIL");write("failure.txt",log+"\n"+failure);}catch(Throwable ignored){}StringWriter trace=new StringWriter();failure.printStackTrace(new PrintWriter(trace));result.putString("stream","NATIVE CITY DISPLACEMENT UI FAIL "+trace);finish(Activity.RESULT_CANCELED,result);}}
    /** Capture model selection and retain the distinct authoritative seven-cell test. */
    private void captureBoundary(MapHost host,World.City city,byte[] expected)throws Exception{
        boolean[] prior=new boolean[4];ui(()->{try{prior[0]=host.commandersShown();prior[1]=host.unitBarsShown();prior[2]=host.gridShown();prior[3]=(Boolean)field(host,"navigatorShown");}catch(Exception e){throw new RuntimeException(e);}});
        try{
            ui(()->{host.setCommandersShown(false);host.setUnitBarsShown(false);host.setGridShown(true);if(prior[3])host.toggleNavigator();activity.selectAndFocus(city.hex);});
            settle();text("收起");waitNative(host);settle();shot("02a-source-city-seven-unobscured");
            verifyCellOutline(host,city);
            check(Arrays.equals(expected,capture()),"normal city selection/grid/labels preserve full committed save/RNG");
        }finally{
            Hex actor=SaveCodec.decode(expected).unit(1).hex;
            ui(()->{host.setCommandersShown(prior[0]);host.setUnitBarsShown(prior[1]);host.setGridShown(prior[2]);if(prior[3])host.toggleNavigator();activity.selectAndFocus(actor);});settle();
        }
        check(Arrays.equals(expected,capture()),"restored display options and actor selection preserve full committed save/RNG");
    }
    private void verifyCellOutline(MapHost host,World.City city)throws Exception{
        boolean[] valid={false,false,false,false};StringBuilder report=new StringBuilder();
        ui(()->{try{
            FilamentMapView renderer=(FilamentMapView)field(host,"spatial");Object overlay=field(renderer,"overlay");
            android.graphics.Path path=(android.graphics.Path)field(overlay,"siteSelectionPath");
            valid[0]=!path.isEmpty();valid[1]=(Long)field(overlay,"siteSelectionFailures")==0;
            Set<?> cells=(Set<?>)field(overlay,"siteSelectionCells");valid[2]=cells.equals(new HashSet<>(SiteFootprint.cells(city)))&&cells.size()==7;
            RectF bounds=new RectF();path.computeBounds(bounds,true);PathMeasure measure=new PathMeasure(path,true);int contours=0;float length=0;
            do{if(measure.getLength()>0){contours++;length+=measure.getLength();}}while(measure.nextContour());
            valid[3]=contours==1;
            report.append("authority_selected_cells=").append(cells).append(" normalized_outline_contours=").append(contours).append(" perimeter_screen_px=").append(length).append(" bounds=").append(bounds).append('\n');
        }catch(Exception e){throw new RuntimeException(e);}});
        write("city-cell-outline.txt",report.toString());
        check(valid[0],"selected city has one merged cell outline instead of individual rule-cell strokes");
        check(valid[1],"real Android Path union succeeds");
        check(valid[2],"outline uses the exact seven authoritative city cells");
        check(valid[3],"the connected seven-cell selection has one exterior outline without internal shared edges");
    }
    /** Read actual source snapshot and live GPU transforms against the committed save. */
    private void verifyVisibleAuthority(MapHost host,World.City city,byte[] expected)throws Exception{
        settle();World control=SaveCodec.decode(expected);boolean[] ok={true,true,true,true,true};StringBuilder observed=new StringBuilder();
        ui(()->{try{
            FilamentMapView renderer=(FilamentMapView)field(host,"spatial");MapSceneSnapshot snap=(MapSceneSnapshot)field(renderer,"snapshot");
            Map<?,?> objects=(Map<?,?>)field(renderer,"objects");Object engine=field(renderer,"engine");Object transforms=engine.getClass().getMethod("getTransformManager").invoke(engine);
            for(int id:new int[]{1,2}){
                Object proxy=objects.get("unit:"+id);if(proxy==null){ok[0]=false;continue;}
                MapSceneSnapshot.Item item=(MapSceneSnapshot.Item)field(proxy,"item");Hex hex=control.unit(id).hex;ok[1]&=item.hex.equals(hex)&&(Boolean)field(proxy,"shown");
                int entity=(Integer)field(proxy,"entity");int instance=(Integer)transforms.getClass().getMethod("getInstance",int.class).invoke(transforms,entity);float[] matrix=new float[16];
                transforms.getClass().getMethod("getTransform",int.class,float[].class).invoke(transforms,instance,matrix);
                float x=snap.ground.grid.x(hex),z=snap.ground.grid.z(hex);ok[2]&=Math.abs(matrix[12]-x)<.002f&&Math.abs(matrix[14]-z)<.002f;
                observed.append("unit=").append(id).append(" authority=").append(hex).append(" snapshot=").append(item.hex).append(" gpu_xz=").append(matrix[12]).append(',').append(matrix[14]).append(" expected_xz=").append(x).append(',').append(z).append('\n');
            }
            MapSceneSnapshot.Item site=null;for(MapSceneSnapshot.Item item:snap.items)if(item.site!=null&&item.hex.equals(city.hex)){site=item;break;}
            ok[3]=site!=null&&new HashSet<>(site.site.cells).equals(new HashSet<>(SiteFootprint.cells(city)))&&site.site.cells.size()==7;
            Object proxy=site==null?null:objects.get(site.key);ok[4]=proxy!=null&&(Boolean)field(proxy,"shown")&&(Boolean)field(field(field(proxy,"shape"),"source"),"pcSite");
            if(site!=null)observed.append("source_city=").append(site.key).append(" authority_seven=").append(site.site.cells).append('\n');
        }catch(Exception error){throw new RuntimeException(error);}});
        write("city-visible-authority.txt",observed.toString());check(ok[0]&&ok[1],"live shown source unit snapshots use both committed authority coordinates");
        check(ok[2],"actual GPU root transforms match both committed grid coordinates within 0.002 world units");
        check(ok[3]&&ok[4],"shown original PC city mesh carries the exact authoritative seven-cell footprint");
        check(Arrays.equals(expected,capture()),"snapshot and GPU authority checks are read-only for complete save/RNG");
    }
    private boolean exitDialog(){boolean[] shown={false};ui(()->{for(View root:WindowInspector.getGlobalWindowViews())if(root.hasWindowFocus()&&find(root,v->v instanceof TextView&&((TextView)v).getText().toString().contains("退出游戏？"))!=null)shown[0]=true;});return shown[0];}
    private void waitNative(MapHost host)throws Exception{long end=SystemClock.uptimeMillis()+120000;while(field(host,"loadingCurtain")!=null&&SystemClock.uptimeMillis()<end)SystemClock.sleep(200);check(host.is3D()&&field(host,"loadingCurtain")==null,"actual 3D output ready");}
    private int battlePhases()throws Exception{int[] count={0};ui(()->{try{for(Object value:(Set<?>)field(((GameApplication)activity.getApplication()).sounds(),"heard")){String key=value.toString();if(key.startsWith("journal:")&&(key.endsWith(":action")||key.endsWith(":critical")))count[0]++;}}catch(Exception e){throw new RuntimeException(e);}});return count[0];}
    private void tapHex(Hex h)throws Exception{float[] p=screenHex(h);check(visibleMapPoint(p),"source-map target is visible outside UI overlays");boolean[] hit={false};ui(()->{try{MapHost host=(MapHost)field(activity,"map");FilamentMapView spatial=(FilamentMapView)field(host,"spatial");MapSceneSnapshot snap=(MapSceneSnapshot)field(spatial,"snapshot");int[] origin=new int[2];spatial.getLocationOnScreen(origin);hit[0]=h.equals(snap.ground.surface.pick(spatial.camera,p[0]-origin[0],p[1]-origin[1]));}catch(Exception e){throw new RuntimeException(e);}});check(hit[0],"actual ground ray picks intended authority target cell");tapPoint(p[0],p[1]);}
    private Object field(Object value,String name)throws Exception{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
    private void advance()throws Exception{nav("地图");int turn=world().turn;text("下一旬");text("执行");text("演示控制");tap(awaitText("跳过剩余演示"));long end=SystemClock.uptimeMillis()+180000;while(field(activity,"turnWork")!=null&&SystemClock.uptimeMillis()<end)settle();check(field(activity,"turnWork")==null&&world().turn==turn+1,"real full turn progresses exactly once");}
    private boolean visibleMapPoint(float[] point)throws Exception{
        boolean[] visible={false};ui(()->{try{View map=(View)field(activity,"map");int[] origin=new int[2];map.getLocationOnScreen(origin);int margin=UiTheme.dp(activity,32);Rect free=new Rect(origin[0]+margin,origin[1]+margin,origin[0]+map.getWidth()-margin,origin[1]+map.getHeight()-margin);visible[0]=free.contains((int)point[0],(int)point[1]);for(String name:new String[]{"commandDock","panelShell"}){View overlay=(View)field(activity,name);if(overlay.isShown()){overlay.getLocationOnScreen(origin);visible[0]&=!new Rect(origin[0]-margin,origin[1]-margin,origin[0]+overlay.getWidth()+margin,origin[1]+overlay.getHeight()+margin).contains((int)point[0],(int)point[1]);}}}catch(Exception e){throw new RuntimeException(e);}});return visible[0];
    }
    private float[] screenHex(Hex h)throws Exception{
        float[][] point={null};ui(()->{try{MapHost host=(MapHost)field(activity,"map");FilamentMapView spatial=(FilamentMapView)field(host,"spatial");if(spatial==null)throw new AssertionError("actual pure3D renderer required");MapSceneSnapshot snap=(MapSceneSnapshot)field(spatial,"snapshot");int[] origin=new int[2];spatial.getLocationOnScreen(origin);float x=snap.ground.grid.x(h),z=snap.ground.grid.z(h);point[0]=new float[]{origin[0]+spatial.camera.screenX(x,z,snap.ground.surface.at(h)),origin[1]+spatial.camera.screenY(x,z,snap.ground.surface.at(h))};}catch(Exception e){throw new RuntimeException(e);}});return point[0];
    }
    private void tapPoint(float x,float y){long now=SystemClock.uptimeMillis();for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent event=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(event,true);event.recycle();SystemClock.sleep(80);}settle();}
    private void nav(String name)throws Exception{description("打开功能导航");description("导航 · "+name);settle();}
    private void description(String value)throws Exception{tap(await(v->v.isClickable()&&v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith(value)));}
    private void text(String value)throws Exception{tap(await(v->v.isClickable()&&v instanceof TextView&&((TextView)v).getText().toString().startsWith(value)));}
    private View awaitText(String value)throws Exception{return await(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith(value));}
    private View tag(String value)throws Exception{return await(v->value.equals(v.getTag()));}
    private void tap(View view)throws Exception{tap(view,1);}
    private void tap(View view,int count)throws Exception{settle();Rect rect=new Rect();boolean[] visible={false};ui(()->{int[] screen=new int[2];visible[0]=view.isAttachedToWindow()&&view.hasWindowFocus()&&view.getGlobalVisibleRect(rect);view.getRootView().getLocationOnScreen(screen);rect.offset(screen[0],screen[1]);});check(visible[0]&&rect.width()>0&&rect.height()>0,"real visible touch target "+view.getClass().getSimpleName());for(int n=0;n<count;n++){long down=SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,rect.exactCenterX(),rect.exactCenterY(),0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(event,true);event.recycle();SystemClock.sleep(80);event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,rect.exactCenterX(),rect.exactCenterY(),0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(event,true);event.recycle();SystemClock.sleep(80);}settle();}
    private void settle(){SystemClock.sleep(350);waitForIdleSync();}
    private View find(View v,Predicate<View> predicate){Rect r=new Rect();if(!v.isShown()||!v.getGlobalVisibleRect(r)||r.width()<8||r.height()<8)return null;if(predicate.test(v))return v;if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++){View match=find(((ViewGroup)v).getChildAt(n),predicate);if(match!=null)return match;}return null;}
    private View await(Predicate<View> predicate)throws Exception{long end=SystemClock.uptimeMillis()+90000;while(SystemClock.uptimeMillis()<end){View[] result={null};ui(()->{List<View> roots=WindowInspector.getGlobalWindowViews();for(int n=roots.size()-1;n>=0;n--)if(roots.get(n).hasWindowFocus()&&(result[0]=find(roots.get(n),predicate))!=null)break;});if(result[0]!=null)return result[0];settle();}throw new AssertionError("visible control timed out");}
    private void ui(Runnable work){Throwable[] error={null};runOnMainSync(()->{try{work.run();}catch(Throwable failure){error[0]=failure;}});if(error[0]!=null)throw new AssertionError(error[0]);}
    private World world()throws Exception{Field field=MainActivity.class.getDeclaredField("world");field.setAccessible(true);return (World)field.get(activity);}
    private byte[] capture(){byte[][] bytes={null};ui(()->{try{bytes[0]=((GameApplication)activity.getApplication()).host().capture();}catch(Exception failure){throw new RuntimeException(failure);}});return bytes[0];}
    private void check(boolean value,String text)throws Exception{if(!value)throw new AssertionError(text);checks++;log.append("PASS ").append(text).append('\n');write("progress.txt",log.toString());}
    private void write(String name,String text)throws Exception{Files.write(new File(output,name).toPath(),text.getBytes("UTF-8"));}
    private void shot(String name)throws Exception{Bitmap bitmap=getUiAutomation().takeScreenshot();if(bitmap!=null)try(OutputStream out=new FileOutputStream(new File(output,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}}}
