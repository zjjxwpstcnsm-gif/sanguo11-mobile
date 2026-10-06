package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import android.graphics.Rect;
import game.sanguo.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;

/** Normal source menu, deployment and map widgets. No fixture world or resource injection. */
public final class SessionBFieldworksInstrumentation extends UiUxInstrumentation {
    private MainActivity activity;private File evidence;
    private boolean coldMode;
    @Override public void onCreate(Bundle args){coldMode=args!=null&&"cold".equals(args.getString("mode"));super.onCreate(args);}
    private Object invoke(String name,Class<?>[] types,Object...args)throws Exception{
        Method method=UiUxInstrumentation.class.getDeclaredMethod(name,types);method.setAccessible(true);
        try{return method.invoke(this,args);}catch(InvocationTargetException e){Throwable cause=e.getCause();if(cause instanceof Exception)throw (Exception)cause;throw new AssertionError(cause);}
    }
    private void put(String name,Object value)throws Exception{Field f=UiUxInstrumentation.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value);}
    private void settle(){SystemClock.sleep(350);runOnMainSync(()->{});}
    private void option(String s)throws Exception{tap(await(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith(s)));}
    private void preparedOption(String s)throws Exception{tap(awaitPreparation(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith(s)));}
    private void text(String s)throws Exception{tap(await(v->v instanceof TextView&&v.isClickable()&&((TextView)v).getText().toString().startsWith(s)));}
    private void description(String s)throws Exception{tap(await(v->v.isClickable()&&v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith(s)));}
    private void nav(String s)throws Exception{description("打开功能导航");description("导航 · "+s);settle();}
    private void check(boolean b,String s)throws Exception{invoke("check",new Class<?>[]{boolean.class,String.class},b,s);}
    private View awaitPreparation(Predicate<View> p)throws Exception{
        long deadline=SystemClock.uptimeMillis()+300000;AssertionError last=null;
        do{try{return await(p);}catch(AssertionError failure){if(!failure.toString().contains("visible control timeout"))throw failure;last=failure;}}while(SystemClock.uptimeMillis()<deadline);
        throw last;
    }
    private View visible(View root,Predicate<View> p){Rect r=new Rect();if(!root.isShown()||!root.getGlobalVisibleRect(r)||r.width()<8||r.height()<8)return null;if(p.test(root))return root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View found=visible(((ViewGroup)root).getChildAt(i),p);if(found!=null)return found;}return null;}
    private View await(Predicate<View> p)throws Exception{long deadline=SystemClock.uptimeMillis()+30000;do{View[] found={null};runOnMainSync(()->{List<View> roots=WindowInspector.getGlobalWindowViews();for(int i=roots.size()-1;i>=0;i--)if(roots.get(i).hasWindowFocus()&&(found[0]=visible(roots.get(i),p))!=null)break;});if(found[0]!=null)return found[0];SystemClock.sleep(150);}while(SystemClock.uptimeMillis()<deadline);throw new AssertionError("visible control timeout");}
    private View tag(String s)throws Exception{return await(v->s.equals(v.getTag()));}
    private void pointer(float x,float y,int presses){for(int i=0;i<presses;i++){long now=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0);down.setSource(InputDevice.SOURCE_TOUCHSCREEN);sendPointerSync(down);SystemClock.sleep(80);MotionEvent up=MotionEvent.obtain(now,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,x,y,0);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{sendPointerSync(up);}catch(RuntimeException lost){/* Dismissed target window; authority checks determine outcome. */}down.recycle();up.recycle();SystemClock.sleep(120);}}
    private void tap(View v)throws Exception{tap(v,1);}
    private void tap(View v,int presses)throws Exception{Rect r=new Rect();runOnMainSync(()->{v.getGlobalVisibleRect(r);int[] at=new int[2];v.getRootView().getLocationOnScreen(at);r.offset(at[0],at[1]);});check(!r.isEmpty(),"visible real pointer target "+(v instanceof TextView?((TextView)v).getText():v.getClass().getSimpleName()));pointer(r.centerX(),r.centerY(),presses);settle();}
    private void shot(String label)throws Exception{settle();android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();check(bitmap!=null,"actual screenshot "+label);try(OutputStream out=new FileOutputStream(new File(evidence,label+".png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}}
    private byte[] capture()throws Exception{return (byte[])invoke("capture",new Class<?>[0]);}
    private Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private void pose(Hex h)throws Exception{runOnMainSync(()->{try{MapHost host=(MapHost)field(activity,"map");ClientState ui=(ClientState)field(activity,"ui");ui.page="map";ui.panelVisible=false;activity.refresh();FilamentMapView view=(FilamentMapView)field(host,"spatial");MapSceneSnapshot snap=(MapSceneSnapshot)field(view,"snapshot");view.camera.x=snap.ground.grid.x(h);view.camera.z=snap.ground.grid.z(h);view.camera.span=9;view.camera.yaw=0;view.camera.tilt=70;view.camera.clampTo(snap.ground);if((Boolean)field(host,"navigatorShown"))host.toggleNavigator();}catch(Exception e){throw new RuntimeException(e);}});settle();}
    private void tile(Hex h)throws Exception{float[] xy=(float[])invoke("screenHex",new Class<?>[]{Hex.class},h);check((Boolean)invoke("routePoint",new Class<?>[]{Hex.class,float[].class},h,xy),"exact real3D target ray "+h);pointer(xy[0],xy[1],1);settle();}
    private void advance(String label)throws Exception{
        nav("地图");int turn=SessionProbe.view(activity).turn;text("下一旬");long began=SystemClock.elapsedRealtime();text("执行");
        long deadline=began+900000,last=began;boolean background=false,skipRequested=false;
        while(field(activity,"turnWork")!=null&&SystemClock.elapsedRealtime()<deadline){
            long now=SystemClock.elapsedRealtime();if(now-last>=30000){note("turn="+label+" elapsedMs="+(now-began)+" background="+background);last=now;}
            // The real copy/computation phase can precede creation of playback controls.
            // Observe its actual readiness instead of treating a missing control as rule failure.
            if(!skipRequested&&field(activity,"playback")!=null){
                text("演示控制");option("跳过剩余演示");skipRequested=true;
            }
            if(!background&&now-began>=120000){shot("turn-"+label+"-foreground120s");sendKeyDownUpSync(KeyEvent.KEYCODE_HOME);background=true;settle();}
            settle();
        }
        if(background){runOnMainSync(()->activity.startActivity(new Intent(activity,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));settle();}
        note("turn="+label+" finalElapsedMs="+(SystemClock.elapsedRealtime()-began)+" background="+background);
        check(field(activity,"turnWork")==null&&SessionProbe.view(activity).turn==turn+1,"normal whole turn "+label+" background="+background);
    }
    private void note(String s)throws Exception{Files.write(new File(evidence,"diagnosis.txt").toPath(),(s+"\n").getBytes("UTF-8"),java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);}
    private View search(View v,Predicate<View> p){if(p.test(v))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View hit=search(((ViewGroup)v).getChildAt(i),p);if(hit!=null)return hit;}return null;}
    private void revealDescription(String name)throws Exception{
        runOnMainSync(()->{for(View root:WindowInspector.getGlobalWindowViews())if(root.hasWindowFocus()){
            View hit=search(root,v->name.equals(v.getContentDescription()==null?null:v.getContentDescription().toString()));
            if(hit!=null)hit.requestRectangleOnScreen(new android.graphics.Rect(0,0,hit.getWidth(),hit.getHeight()),true);
        }});settle();description(name);
    }
    private void unitAction(String label)throws Exception{
        description("选中对象指令 ·");text("展开");
        runOnMainSync(()->{View hit=search(activity.getWindow().getDecorView(),v->v instanceof Button&&((Button)v).getText().toString().startsWith(label));if(hit!=null)hit.requestRectangleOnScreen(new android.graphics.Rect(0,0,hit.getWidth(),hit.getHeight()),true);});settle();text(label);
    }
    private void militaryMenu()throws Exception{unitAction("设置军事设施");}
    private void selectUnit(int id)throws Exception{runOnMainSync(()->activity.selectUnitAndFocus(id));settle();}
    private int safety(World world,World.Unit builder,Hex tile){
        int nearest=999;
        for(World.Unit enemy:world.units)if(world.campaign.hostile(builder.owner,enemy.owner))nearest=Math.min(nearest,tile.distance(enemy.hex));
        for(World.City enemy:world.cities)if(world.campaign.hostile(builder.owner,enemy.owner))nearest=Math.min(nearest,SiteFootprint.distance(enemy,tile));
        return nearest;
    }
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
          flow: {
            evidence=new File(getTargetContext().getExternalFilesDir("session-b"),coldMode?"fieldworks-cold":"fieldworks");evidence.mkdirs();put("output",evidence);
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));put("activity",activity);
            await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
            if(coldMode){
                byte[] expected=Files.readAllBytes(new File(getTargetContext().getExternalFilesDir("session-b"),"fieldworks/actual-build.sg11").toPath());
                check(Arrays.equals(expected,capture()),"new process auto-resumes complete actual source/fee/construction/bothRNG");shot("01-cold-process-resume");
                nav("菜单");text("读取存档");preparedOption("槽位 3");text("读取存档");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));settle();
                check(Arrays.equals(expected,capture()),"new process ordinary manual load restores exact full campaign/bothRNG");
                result.putString("stream","PASS SESSION B FIELDWORKS COLD actual new-process auto-resume/manual-load complete world and bothRNG; ARM pending\n");break flow;
            }
            byte[] original=capture();
            nav("菜单");text("新游戏 / 选择势力");
            var source=PcScenarioCatalog.all().get(14);revealDescription("选择PC来源剧本 "+source.identity.path);
            awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("确认开局势力"));
            runOnMainSync(()->{for(View root:WindowInspector.getGlobalWindowViews()){
                View hit=search(root,v->v instanceof Button&&((Button)v).getText().toString().startsWith("曹操"));
                if(hit!=null)hit.requestRectangleOnScreen(new android.graphics.Rect(0,0,hit.getWidth(),hit.getHeight()),true);
            }});settle();text("曹操");description("确认开局势力");text("开始新局");
            awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));settle();
            World w=SessionProbe.view(activity);check(w.scenarioId.equals(source.identity.scenarioId)&&!Arrays.equals(original,capture()),"normal menu creates exact Source14 campaign");shot("01-source-new-game");
            World.City city=w.home();World.Officer leader=w.idle(city).stream().max(Comparator.comparingInt(o->o.leadership+o.war)).orElseThrow();nav("地图");description("定位己方据点 "+city.name);text("出征");
            ListView roster=(ListView)tag("deploy.officers");invoke("showRosterTag",new Class<?>[]{ListView.class,String.class},roster,"deploy.role."+leader.id);tap(tag("deploy.role."+leader.id));await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().equals("从编队移除 "+leader.name));tap(tag("deploy.tab.1"));description("枪兵 库存");
            revealDescription("兵力数量");EditText troops=(EditText)await(v->v instanceof EditText&&"兵力数量".equals(v.getContentDescription()));invoke("enter",new Class<?>[]{EditText.class,String.class},troops,"5000");sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();
            revealDescription("金钱数量");EditText money=(EditText)await(v->v instanceof EditText&&"金钱数量".equals(v.getContentDescription()));invoke("enter",new Class<?>[]{EditText.class,String.class},money,"1000");sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();tap(tag("deploy.confirm"));
            w=SessionProbe.view(activity);World.Unit u=w.unit(w.officer(leader.id).unitId);check(u!=null&&u.gold==1000&&u.troops==5000&&u.weapon==World.Weapon.SPEAR&&!u.acted,"ordinary deployment carries1000 gold/5000 spear troops and remains actionable");int id=u.id;selectUnit(id);
            note("sourceId="+source.identity.scenarioId+" unit="+u.id+" officer="+u.officerId+" owner="+u.owner+" direct="+w.districts.directUnit(id)+" acted="+u.acted+" status="+u.status+" gold="+u.gold+" origin="+u.hex+" march="+u.march);
            for(Hex h:u.hex.neighbors())note("initial target="+h+" source="+MapCoordinates.nationalSource(w,h)+" terrain="+w.terrain[h.q][h.r]+" reason="+w.fieldworks.buildError(id,War.StructureKind.CAMP,h,0));
            byte[] deployed=capture();militaryMenu();option("阵 · 金500");shot("02-initial-build-targets");
            await(v->v instanceof TextView&&((TextView)v).getText().toString().contains("SITE_DISTANCE"));
            // Current baseline has no sites at the deployment cell. Close the real information dialog.
            text("返回");check(Arrays.equals(deployed,capture()),"empty build selection preserves complete campaign/RNG");
            // Ordinary map movement must retain the unit at the seven-cell city center.
            Hex originalPosition=u.hex;selectUnit(id);pose(city.hex);text("行军");tile(city.hex);text("确认任务");settle();
            w=SessionProbe.view(activity);check(w.unit(id)!=null&&w.unit(id).hex.equals(city.hex)&&!w.unit(id).acted,"ordinary map move stops on city center without auto garrison");
            selectUnit(id);unitAction("补充携金");option(city.name+" · 金");option("200金");byte[] fundingPreview=capture();text("取消");check(Arrays.equals(fundingPreview,capture()),"real city-center funding cancel preserves full save/bothRNG");
            selectUnit(id);unitAction("补充携金");option(city.name+" · 金");option("200金");int cityGold=SessionProbe.view(activity).city(city.id).gold;long fundRevision=activity.deploymentState().revision;
            View fundExecute=await(v->v instanceof Button&&"执行".contentEquals(((Button)v).getText()));tap(fundExecute,2);settle();
            w=SessionProbe.view(activity);check(w.unit(id).gold==1200&&w.city(city.id).gold==cityGold-200&&w.unit(id).acted&&activity.deploymentState().revision==fundRevision+1,"real city-center funding transfers200/action once");shot("02a-city-center-funding");
            advance("funding-reset");
            selectUnit(id);pose(originalPosition);text("行军");tile(originalPosition);text("确认任务");settle();check(SessionProbe.view(activity).unit(id).hex.equals(originalPosition),"ordinary unit leaves city on exact normal move");
            // Original category2/3 accepts empty city-near cells; execute the actual page.
            w=SessionProbe.view(activity);Hex wallSite=w.fieldworks.sites(id,War.StructureKind.EARTH_WALL).get(0);
            check(SiteFootprint.distance(w.city(city.id),wallSite)<=2,"real wall target is inside formerly overbroad city exclusion");
            selectUnit(id);pose(wallSite);militaryMenu();option("土垒 · 金300");tile(wallSite);text("执行");settle();
            w=SessionProbe.view(activity);check(w.war.at(wallSite)!=null&&w.unit(id).gold==900&&w.unit(id).acted,"normal city-near wall build deducts300 and action");shot("02b-city-near-wall");
            advance("wall-construction");
            advance("wall-action-reset");
            w=SessionProbe.view(activity);check(w.war.at(wallSite).complete&&w.unit(id)!=null&&!w.unit(id).acted,"ordinary multi-turn wall completion/action reset");
            // Resolve a real movement endpoint from preview, then use ordinary map march/confirm.
            deployed=capture();w=SessionProbe.view(activity);u=w.unit(id);Hex endpoint=null,target=null;
            int safety=Integer.MIN_VALUE;
            final World planning=w;final World.Unit builder=u;
            List<Hex> origins=new ArrayList<>(w.orders.reachable(u).keySet());origins.sort(Comparator.comparingInt((Hex h)->-safety(planning,builder,h)).thenComparingInt(h->h.q).thenComparingInt(h->h.r));
            for(Hex origin:origins){
                // A neighboring target's distance from every threat differs by at most1.
                // This bounds unexplored cells without weakening any real move/build check.
                if(endpoint!=null&&safety>=safety(w,u,origin)+1)break;
                World copy=SaveCodec.decode(deployed);World.Unit unit=copy.unit(id);
                if(!origin.equals(unit.hex)&&!copy.move(id,origin).ok)continue;
                List<Hex> legal=copy.fieldworks.sites(id,War.StructureKind.CAMP);
                for(Hex candidate:legal){
                    int nearest=safety(w,u,candidate);
                    if(nearest>safety){safety=nearest;endpoint=origin;target=candidate;}
                }
            }
            check(endpoint!=null,"ordinary movement has a legally previewed camp cell");final Hex to=endpoint,site=target;
            note("move="+to+" build="+site+" source="+MapCoordinates.nationalSource(w,site));
            selectUnit(id);pose(to);text("行军");tile(to);text("确认任务");settle();
            w=SessionProbe.view(activity);u=w.unit(id);check(u.hex.equals(to)&&!u.acted,"real map move arrives at exact preview endpoint without spending action");
            selectUnit(id);pose(site);byte[] before=capture();militaryMenu();option("阵 · 金500");tile(site);shot("03-legal-confirm");text("取消");
            check(Arrays.equals(before,capture()),"normal construction confirmation cancel preserves fullWorld/bothRNG");text("取消选取");
            selectUnit(id);pose(site);militaryMenu();option("阵 · 金500");tile(site);
            long revision=activity.deploymentState().revision;View execute=await(v->v instanceof Button&&"执行".contentEquals(((Button)v).getText()));tap(execute,2);settle();
            w=SessionProbe.view(activity);War.Structure built=w.war.at(site);
            check(built!=null&&built.builder==id&&!built.complete&&w.unit(id).gold==400&&w.unit(id).acted,"real legal map construction debits500 once, uses action and starts construction");check(activity.deploymentState().revision==revision+1,"double execute commits once");shot("04-real-construction");
            byte[] started=capture();Files.write(new File(evidence,"actual-build.sg11").toPath(),started);
            // Actual save/load UI protects the whole campaign and both RNG streams.
            boolean existedSlot=new File(activity.getFilesDir(),"manual3.sg11").exists();
            nav("菜单");text("保存局面");option("槽位 3");if(existedSlot)text("覆盖存档");settle();
            check(Arrays.equals(started,Files.readAllBytes(new File(activity.getFilesDir(),"manual3.sg11").toPath())),"ordinary save stores exact whole campaign/bothRNG");
            for(int turn=0;turn<8&&!SessionProbe.view(activity).war.at(site).complete;turn++)advance("camp-construction-"+turn);
            w=SessionProbe.view(activity);check(w.war.at(site)!=null&&w.war.at(site).complete,"normal multi-turn camp construction completes");shot("05-completed");
            nav("菜单");text("读取存档");preparedOption("槽位 3");text("读取存档");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));settle();
            check(Arrays.equals(started,capture()),"actual load restores unfinished construction and complete world/bothRNG");
            android.app.Instrumentation.ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);runOnMainSync(activity::recreate);MainActivity resumed=(MainActivity)waitForMonitorWithTimeout(monitor,300000);removeMonitor(monitor);check(resumed!=null,"activity recreation returns a real new activity");activity=resumed;put("activity",activity);settle();check(Arrays.equals(started,capture()),"activity reopen preserves entire restored construction campaign");
            byte[] saved=capture();check(Arrays.equals(saved,SaveCodec.encode(SaveCodec.decode(saved))),"complete production save/RNG canonical roundtrip");
            result.putString("stream","PASS SESSION B FIELDWORKS normal Source14/new/deploy1000/ordinary-city-stop/fund-cancel-double/city-near-wall/multi-turn/move/empty-selection/cancel/double/camp/build/full-save-load/recreation; cold process and ARM pending\n");
          }
        }catch(Throwable failure){result.putString("stream","FAIL SESSION B FIELDWORKS "+android.util.Log.getStackTraceString(failure));try{shot("failed");}catch(Throwable ignored){}}
        finally{try{if(activity!=null)runOnMainSync(()->activity.finish());settle();}catch(Throwable failure){result.putString("stream",result.getString("stream")+"\nFAIL lifecycle "+failure);}}
        try{if(evidence!=null)Files.write(new File(evidence,"result.txt").toPath(),result.getString("stream").getBytes("UTF-8"));}catch(Exception ignored){}
        finish(Activity.RESULT_OK,result);
    }
}
