package game.sanguo.mobile;

import android.app.*;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import game.sanguo.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;

/** End-to-end installed APK checks. Performance uses the shipped 250/28 national map, not a fixture. */
final class Realm52Probe {
    private final Instrumentation test;private MainActivity activity;private int checks;private boolean withinBudget=true;
    private final StringBuilder report=new StringBuilder();
    Realm52Probe(Instrumentation test){this.test=test;}
    void run()throws Exception{
        World opening=ScenarioCatalog.load("heroes-250",0,12345L);require(opening.factions.length==28,"shipped 250 scenario contains 28 factions");
        try(OutputStream out=test.getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(opening));}
        test.getTargetContext().getSharedPreferences("map-display",0).edit().clear().commit();
        activity=(MainActivity)test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        require(world().cities.size()==87&&world().height==100,"official 87-site national map retained");
        header();nationalTurns();panels();openingPicker();pauseAndRecreate();critical();
        File dir=directory();try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(dir,"v052-checks.txt")),"UTF-8")){out.write((withinBudget?"REALM52 PASS ":"REALM52 PERFORMANCE FAIL ")+checks+" checks\n"+report);}
        require(withinBudget,"all six 250/28 transitions must be <=10000ms; no performance failure may publish");
    }
    private void header()throws Exception{
        TextView badge=(TextView)field(activity,"actionPointsBadge");Rect visible=new Rect();
        ui(()->{require(badge.getGlobalVisibleRect(visible)&&visible.width()>0,"action points badge is permanently visible");require(badge.getText().toString().contains("60"),"action point count is not inside a truncated title");
            World w=world();int old=w.actionPoints[w.player];w.actionPoints[w.player]=17;activity.refresh();require(badge.getText().toString().endsWith("17"),"badge updates from the actual player AP");w.actionPoints[w.player]=old;activity.refresh();});
    }
    private void nationalTurns()throws Exception{
        String[] hashes={"1ae4521c16defb8c43250e35da3cc81019112816e6271e4f84b66c38b8e0826a","2374c4549c7ab176cfda91566486fad5e69da955c153b2cfda13d8b656e7438b","da27cfd72068a454ee5b43b196c68387b5451a3a0db83e4eafa4f92e2c5dce68","032a45c1054516902ea6fea62fdfd03e686bd3981f1b0881560e3d9cd5fa0851","d3cf0274fa0ed2b00d2a1fca56d43a02979783755cbb6700f0f56051b7c32d2a","ee828a3292c29b67ef3c78f5d5b0ffb3673ef55667a8e94c8e284502d78bd079"};
        for(int n=0;n<hashes.length;n++){
            ui(()->{ClientState state=(ClientState)field(activity,"ui");state.page="map";state.panelVisible=false;activity.refresh();((MapView)field(field(activity,"map"),"flat")).fit();});settle();
            TurnWork[] work={null};long external=SystemClock.elapsedRealtime();
            ui(()->{call(activity,"advanceTurn");work[0]=(TurnWork)field(activity,"turnWork");});
            // Final acceptance measures ordinary play, not a stack-sampling session.
            await(()->!(Boolean)field(activity,"aiRunning"),45000,"national turn completes without a manual skip");test.waitForIdleSync();
            long total=(Long)field(activity,"lastTurnWallMillis"),compute=(Long)field(activity,"lastTurnComputeMillis");
            String line="PERF52 turn="+(n+1)+" totalMs="+total+" computeMs="+compute+" observedMs="+(SystemClock.elapsedRealtime()-external)+" units="+world().units.size()+" events="+work[0].playedEvents+" batches="+work[0].publishedBatches;
            report.append(line).append('\n');android.util.Log.i("Realm52Probe",line);
            withinBudget&=total>0&&total<=10000; // Defer assertion until all UI diagnostics are captured; never waive it.
            require(work[0].done&&work[0].error==null&&!work[0].waitingForPlayback,"computation completed without any animation semaphore");
            byte[] finalBytes=SaveCodec.encode(world());require(digest(finalBytes).equals(hashes[n]),"Android turn "+(n+1)+" is byte-identical to v51 rules/RNG baseline");
            require(Arrays.equals(finalBytes,readAuto()),"autosave contains the complete authoritative turn, never visual snapshots");
            require(((Button)field(activity,"nextTurn")).isEnabled(),"next-turn commands restored");
        }
        shot("01-national-after-six-turns");
    }
    private void panels()throws Exception{
        byte[] before=SaveCodec.encode(world());
        ui(()->activity.openRealmPage("factions",-1));settle();
        DataTable<?> factions=table();require(factions.list.getAdapter().getCount()==28,"faction list contains every faction");shot("02-factions");
        ui(()->factions.search.setText("曹操"));settle();require(factions.list.getAdapter().getCount()==1,"faction search filters actual rows");
        AlertDialog[] sheet={null};int side=0;for(int i=0;i<world().factions.length;i++)if(world().faction(i).contains("曹操"))side=i;final int selectedSide=side;
        ui(()->sheet[0]=new RealmUi(activity,world(),(ClientState)field(activity,"ui")).factionDetail(selectedSide,null));settle();shot("03-faction-dossier");
        for(String tab:new String[]{"收支","技巧树","能力研究"}){
            ui(()->{View button=find(sheet[0].getWindow().getDecorView(),"势力详情 · "+tab);require(button!=null,"faction tab "+tab+" exists");button.performClick();});settle();shot("04-"+tab);
        }
        ui(()->sheet[0].dismiss());
        for(String page:new String[]{"units","cities","ports","gates","officers","facilities"}){
            ui(()->activity.openRealmPage(page,-1));settle();DataTable<?> grid=table();require(grid!=null,"national "+page+" uses recycled table");
            int count=grid.list.getAdapter().getCount();if(page.equals("units"))require(count==new RealmOverview(world()).units.size(),"all factions armies and convoys listed");
            if(page.equals("ports"))require(count==world().cities.stream().filter(c->c.kind==World.SiteKind.PORT).count(),"all ports and only ports listed");
            if(page.equals("gates"))require(count==world().cities.stream().filter(c->c.kind==World.SiteKind.GATE).count(),"all gates and only gates listed");
            shot("05-"+page);
        }
        ui(()->activity.openRealmPage("units",-1));settle();DataTable<?> units=table();
        require(units.list.getAdapter().getCount()>0,"campaign produced real field armies");
        ui(()->{units.list.performItemClick(units.list.getChildAt(0),0,units.list.getAdapter().getItemId(0));});settle();
        require(((ClientState)field(activity,"ui")).page.equals("map"),"list click locates the selected real unit");
        ui(()->((Button)field(activity,"returnList")).performClick());settle();require(((ClientState)field(activity,"ui")).page.equals("units"),"return-list returns to units, not always cities");
        require(Arrays.equals(before,SaveCodec.encode(world())),"search, sort, dossier and map locating never alter the world");
        ui(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));settle();activity=(MainActivity)field(test,"current");
        ui(()->activity.openRealmPage("units",-1));settle();
        DataTable<?> landscape=table();require(landscape.list.getHeight()>=activity.dp(80)&&landscape.list.getChildCount()>=2,"landscape exposes actual scrollable unit rows, not just filters and headers");shot("06-landscape-units");
        TextView badge=(TextView)field(activity,"actionPointsBadge");require(badge.getVisibility()==View.VISIBLE,"AP badge survives landscape and list navigation");
        ui(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));settle();activity=(MainActivity)field(test,"current");
    }
    private void openingPicker()throws Exception{
        World template=ScenarioCatalog.load("heroes-250",0,12345L);byte[] before=SaveCodec.encode(template);ScenarioFactionPicker[] picker={null};
        ui(()->{picker[0]=new ScenarioFactionPicker(activity,template,i->{});picker[0].show();});settle();
        MapView preview=(MapView)field(picker[0],"map");require(preview.territoryMode()==1,"opening uses actual faction-colored national map");
        World.City target=template.cities.stream().filter(c->c.owner>0&&c.kind==World.SiteKind.CITY).findFirst().get();
        ui(()->call(picker[0],"tap",new Class[]{Hex.class},target.hex));settle();require((Integer)field(picker[0],"selected")==target.owner,"tapping a city selects its owner");
        Hex land=null;for(int q=0;q<template.width&&land==null;q++)for(int r=0;r<template.height;r++){Hex h=new Hex(q,r);int id=preview.territory().siteAt(h);World.City c=template.city(id);if(c!=null&&c.owner==0&&template.cityAt(h)==null&&template.terrain[q][r]!=World.Terrain.VOID){land=h;break;}}
        final Hex tile=land;require(tile!=null,"colored land selection target exists");ui(()->call(picker[0],"tap",new Class[]{Hex.class},tile));settle();require((Integer)field(picker[0],"selected")==0,"tapping colored territory selects its real faction");
        shot("07-opening-map-selection");require(Arrays.equals(before,SaveCodec.encode(template)),"map faction selection does not mutate the scenario template");
        ui(()->((Dialog)field(picker[0],"dialog")).dismiss());
    }
    private void pauseAndRecreate()throws Exception{
        World original=SaveCodec.decode(SaveCodec.encode(world())),expected=SaveCodec.decode(SaveCodec.encode(original));expected.nextTurn();byte[] finalBytes=SaveCodec.encode(expected);
        TurnWork[] work={null};ui(()->{call(activity,"advanceTurn");work[0]=(TurnWork)field(activity,"turnWork");work[0].pause(true);});
        await(()->work[0].done,30000,"worker finishes even while presentation is paused");require(work[0].paused&&!work[0].waitingForPlayback,"pause is presentation-only, no worker lock");
        require(Arrays.equals(finalBytes,readAuto()),"completed authoritative turn is saved even while its presentation is paused");
        ui(()->activity.recreate());settle();activity=(MainActivity)field(test,"current");require(field(activity,"turnWork")==work[0]&&work[0].paused,"rotation/recreation retains paused queue and same turn worker");
        ui(()->((TurnPlayback)field(activity,"playback")).skip());await(()->!(Boolean)field(activity,"aiRunning"),10000,"skip completes retained presentation");
        require(Arrays.equals(finalBytes,SaveCodec.encode(world())),"skip and recreation do not change any authoritative rule outcome");
    }
    private void critical()throws Exception{
        World before=Realm52Fixture.criticalWorld(true);
        ui(()->{call(activity,"activateWorld",new Class[]{World.class},before);activity.selectUnitAndFocus(1);ClientState s=(ClientState)field(activity,"ui");s.panelVisible=false;activity.refresh();((MapView)field(field(activity,"map"),"flat")).center(before.unit(1).hex);});settle();
        byte[][] resultBytes={null};ui(()->{World.Result r=SessionProbe.command(activity,Realm52Fixture::criticalCommand);require(r.ok&&r.critical!=null,"actual paid tactic produced typed critical hit");resultBytes[0]=SaveCodec.encode(SessionProbe.view(activity));});
        await(()->{Object flash=field(activity,"criticalFlash");return flash!=null&&(Long)field(flash,"began")>0;},2000,"player critical portrait reached a real rendered frame");
        criticalScreenshot();
        await(()->field(activity,"criticalFlash")==null,2500,"critical freeze automatically ends without blocking UI");require(Arrays.equals(resultBytes[0],SaveCodec.encode(world())),"cut-in does not apply combat a second time");
        World initial=Realm52Fixture.criticalWorld(true),after=SaveCodec.decode(SaveCodec.encode(initial));TurnJournal journal=new TurnJournal(after);Realm52Fixture.criticalCommand(after);journal.close();
        TurnWork work=SessionProbe.replay(test,activity,initial,after);work.after=after;work.visual=SaveCodec.decode(SaveCodec.encode(initial));work.events=journal.events();work.batchReady=true;work.done=true;work.fullReplay=true;work.summary="真实战法暴击回放";
        ui(()->{set(activity,"world",initial);set(activity,"turnWork",work);set(activity,"aiRunning",true);call(activity,"finishTurn");});
        await(()->field(field(field(activity,"map"),"flat"),"criticalHit")!=null,4000,"AI/replay pipeline displays the actual critical event");
        ui(()->((TurnPlayback)field(activity,"playback")).skip());await(()->!(Boolean)field(activity,"aiRunning"),5000,"critical cut-in respects skip");
        require(Arrays.equals(SaveCodec.encode(after),SaveCodec.encode(world())),"replayed critical preserves the exact final state");
    }
    private void criticalScreenshot()throws Exception{
        // PixelCopy reads the actual window buffer without accessibility screenshot idle
        // synchronization, which can miss a transient 680ms cut-in entirely.
        Bitmap[] frame={null};Rect[] bodyBounds={null};java.util.concurrent.CountDownLatch ready=new java.util.concurrent.CountDownLatch(1);int[] status={-1};
        ui(()->{View decor=activity.getWindow().getDecorView(),body=(View)field(activity,"body");int[] xy=new int[2];body.getLocationInWindow(xy);
            bodyBounds[0]=new Rect(xy[0],xy[1],xy[0]+body.getWidth(),xy[1]+body.getHeight());
            frame[0]=Bitmap.createBitmap(decor.getWidth(),decor.getHeight(),Bitmap.Config.ARGB_8888);
            PixelCopy.request(activity.getWindow(),frame[0],result->{status[0]=result;ready.countDown();},new Handler(Looper.getMainLooper()));});
        require(ready.await(2,java.util.concurrent.TimeUnit.SECONDS)&&status[0]==PixelCopy.SUCCESS,"native window buffer captured the transient critical frame");
        Rect rect=bodyBounds[0];int dark=0,total=0;
        for(int x=rect.left+rect.width()/10;x<rect.right-rect.width()/10;x+=17)for(int y=rect.top+rect.height()/12;y<rect.top+rect.height()/6;y+=11){
            int pixel=frame[0].getPixel(x,y);if(Color.red(pixel)<70&&Color.green(pixel)<85&&Color.blue(pixel)<100)dark++;total++;}
        try(OutputStream out=new FileOutputStream(new File(directory(),"v052-08-real-critical-cut-in.png"))){frame[0].compress(Bitmap.CompressFormat.PNG,100,out);}frame[0].recycle();
        require(total>0&&dark>total*.85,"critical overlay pixels visibly cover the map, not merely an attached empty View");
    }
    private World world()throws Exception{return (World)field(activity,"world");}
    private DataTable<?> table()throws Exception{return findTable((View)field(activity,"panelHost"));}
    private DataTable<?> findTable(View v){if(v instanceof DataTable)return (DataTable<?>)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){DataTable<?> result=findTable(((ViewGroup)v).getChildAt(i));if(result!=null)return result;}return null;}
    private View find(View root,String description){if(description.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View v=find(((ViewGroup)root).getChildAt(i),description);if(v!=null)return v;}return null;}
    interface Action{void run()throws Exception;}interface Condition{boolean ready()throws Exception;}
    private void ui(Action action){Throwable[] failure={null};test.runOnMainSync(()->{try{action.run();}catch(Throwable t){failure[0]=t;}});if(failure[0]!=null)throw new AssertionError("UI check",failure[0]);}
    private void await(Condition condition,long timeout,String message)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;boolean[] yes={false};while(SystemClock.elapsedRealtime()<end){ui(()->yes[0]=condition.ready());if(yes[0]){require(true,message);return;}SystemClock.sleep(25);}throw new AssertionError(message+" timed out");}
    private byte[] readAuto()throws Exception{try(InputStream in=test.getTargetContext().openFileInput("auto.sg11");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toByteArray();}}
    private String digest(byte[] data)throws Exception{StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(data))out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
    private File directory(){File dir=test.getTargetContext().getExternalFilesDir("smoke");if(dir==null)throw new AssertionError("missing evidence folder");dir.mkdirs();return dir;}
    private void settle(){test.waitForIdleSync();SystemClock.sleep(320);test.waitForIdleSync();}
    private void shot(String name)throws Exception{Bitmap image=test.getUiAutomation().takeScreenshot();require(image!=null,"installed screenshot "+name);try(OutputStream out=new FileOutputStream(new File(directory(),"v052-"+name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
    private void require(boolean ok,String msg){if(!ok)throw new AssertionError(msg);checks++;report.append("PASS ").append(msg).append('\n');}
    private static Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private static void set(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    private static Object call(Object target,String name)throws Exception{return call(target,name,new Class[0]);}
    private static Object call(Object target,String name,Class<?>[] types,Object... args)throws Exception{Method m=target.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(target,args);}
}
