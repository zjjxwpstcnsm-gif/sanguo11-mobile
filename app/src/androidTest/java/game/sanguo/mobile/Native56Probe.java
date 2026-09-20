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
import java.util.*;

/** Installed APK, actual shipped scenarios, real controls, actual window screenshots.
 * Only the attack setup relocates an already-deployed army; terrain/sites never substituted. */
final class Native56Probe {
    private final Instrumentation test;private MainActivity activity;private int checks;
    private final StringBuilder report=new StringBuilder();
    Native56Probe(Instrumentation t){test=t;}
    void run()throws Exception{
        try{
            launch(ScenarioCatalog.load("coalition-190",5,56L));
            geography();sortieAndTransit();edgeAttack();
            report.append("Build source: "+BuildConfig.SOURCE_REVISION+"; version "+BuildConfig.VERSION_NAME+"/"+BuildConfig.VERSION_CODE+"\n");
            report.append("No physical ARM device was used; API29 x86_64 emulator. Attack fixture arranges an army at the real Luoyang edge, without editing map or ownership.\n");
        }finally{File dir=test.getTargetContext().getExternalFilesDir("smoke");dir.mkdirs();try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(dir,"v056-checks.txt")),"UTF-8")){out.write("Installed checks: "+checks+"\n"+report);}}
    }
    private void launch(World initial)throws Exception{
        if(activity!=null){ui(activity::finish);settle();}
        try(OutputStream out=test.getTargetContext().openFileOutput("auto.sg11",0)){out.write(SaveCodec.encode(initial));}
        test.getTargetContext().getSharedPreferences("map-display",0).edit().clear().putBoolean("navigator",true).commit();
        activity=(MainActivity)test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
        require(world().scenarioId.equals(initial.scenarioId),"official scenario loaded, no substitute map");
        ui(()->{page();map().fit();});settle();
    }
    private World world()throws Exception{return (World)field(activity,"world");}
    private MapView map()throws Exception{return (MapView)field(activity,"map");}
    private void page()throws Exception{ClientState state=(ClientState)field(activity,"ui");state.page="map";state.panelVisible=false;state.panelExpanded=false;activity.refresh();}
    private void focus(Hex h,float scale)throws Exception{ui(()->{page();map().focus(h);MapCamera camera=(MapCamera)field(map(),"camera");camera.zoom(scale,map().getWidth()/2f,map().getHeight()/2f);map().center(h);});settle();}
    private void geography()throws Exception{
        World w=world();require(w.sourceColumns()==200&&w.sourceRows()==200&&w.mapRevision==56,"installed APK really loads native200/revision56");
        require(w.cities.size()==87&&CityAtlas.bytes()>0,"87 sites and decoded new city atlas");byte[] before=SaveCodec.encode(w);
        shot("01-FAR-national");
        int[] ids={20015,20017,20039,20029,20022,20000,20041,20044,20065};
        for(int id:ids){World.City site=w.city(id);focus(site.hex,3.4f);shot("NEAR-site-"+id);focus(site.hex,.9f);shot("MID-site-"+id);}
        World.City city=w.city(20017);focus(city.hex,3.4f);
        for(Hex h:SiteFootprint.cells(city)){
            ui(this::page);settle();tap(h);Hex selected=(Hex)field(activity,"selected");
            require(world().cityAt(selected)==city,"actual touch selects city entity from each of its seven cells "+h);
        }
        focus(city.hex,3.4f);shot("02-seven-selected");
        World.City gate=w.city(20044);focus(gate.hex,3.4f);Hex adjacent=gate.hex.neighbors().get(0);ui(this::page);tap(adjacent);
        require(((Hex)field(activity,"selected")).equals(adjacent)&&world().cityAt(adjacent)!=gate,"one-cell gate art does not steal neighbor touches");
        focus(city.hex,3.4f);ui(this::page);settle();
        RectF mini=(RectF)field(map(),"miniRect");MapCamera camera=(MapCamera)field(map(),"camera");float previousX=camera.centerX();
        tapPoint(mini.left+mini.width()*.82f,mini.top+mini.height()*.67f);
        require(Math.abs(camera.centerX()-previousX)>500,"actual minimap touch jumps across native national map");shot("03-minimap-jump");
        for(int i=0;i<4;i++){ui(()->{MapCamera c=(MapCamera)field(map(),"camera");c.pan(100,-80);map().invalidate();});settle();}
        require(Arrays.equals(before,SaveCodec.encode(world())),"map gestures and selections do not mutate game rules");
        focus(city.hex,3.4f);sampleFrames(false);sampleFrames(true);
        report.append("Atlas decoded bytes: ").append(CityAtlas.bytes()).append("; overview cache bytes: ").append(map().overviewBytes()).append("\n");
    }
    private void sampleFrames(boolean zoom)throws Exception{
        java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);long[] times=new long[50],cpu=new long[50];
        ui(()->android.view.Choreographer.getInstance().postFrameCallback(new android.view.Choreographer.FrameCallback(){
            int index;
            public void doFrame(long ns){try{
                times[index]=ns;cpu[index]=map().drawNanos();MapCamera c=(MapCamera)field(map(),"camera");
                if(zoom)c.zoom(1.3f+(float)Math.sin(index*.13f)*.45f,map().getWidth()/2f,map().getHeight()/2f);else c.pan(index<25?-12:12,5);
                map().invalidate();if(++index<times.length)android.view.Choreographer.getInstance().postFrameCallback(this);else done.countDown();
            }catch(Exception error){report.append(error).append('\n');done.countDown();}}
        }));
        require(done.await(30,java.util.concurrent.TimeUnit.SECONDS)&&times[times.length-1]>0,"installed frame sampler completed all frames");
        double[] intervals=new double[times.length-1];for(int i=1;i<times.length;i++)intervals[i-1]=(times[i]-times[i-1])/1e6;
        Arrays.sort(intervals);Arrays.sort(cpu);report.append(zoom?"ZOOM":"PAN").append(" emulator frame interval p50_ms=").append(intervals[24]).append(" p95_ms=").append(intervals[46]).append(" CPU onDraw p95_ms=").append(cpu[47]/1e6).append('\n');
    }
    private void tapPoint(float x,float y)throws Exception{
        int[] loc=new int[2];map().getLocationOnScreen(loc);long t=SystemClock.uptimeMillis();
        MotionEvent down=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,x+loc[0],y+loc[1],0),up=MotionEvent.obtain(t,t+80,MotionEvent.ACTION_UP,x+loc[0],y+loc[1],0);
        test.sendPointerSync(down);test.sendPointerSync(up);down.recycle();up.recycle();settle();
    }
    private void sortieAndTransit()throws Exception{
        World w=world();World.City c=w.city(20017);ui(()->new DeployWizard(activity,w,null,DeployWizard.start(activity,c,false)).show());settle();
        ListView list=(ListView)tag("deploy.officers");require(list.getAdapter().getCount()>=1,"real ChangAn has an available officer");long id=list.getAdapter().getItemId(0);
        ui(()->list.setSelection(0));settle();clickTag("deploy.officer."+id);clickTag("deploy.tab.1");clickTag("choice.SPEAR");
        QuantityControl troops=(QuantityControl)tag("deploy.troops"),food=(QuantityControl)tag("deploy.food");ui(()->{troops.set(3000);food.set(12000);});clickTag("deploy.tab.2");shot("04-real-sortie-wizard");
        int prior=c.troops;ui(()->dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick());settle();World.Unit u=w.unit(w.officer((int)id).unitId);require(u!=null&&c.troops==prior-3000,"actual wizard deploys exactly one army");
        require(SiteFootprint.distance(c,u.hex)==1&&!SiteFootprint.contains(c,u.hex),"spawn is reachable city exterior");final int unitId=u.id;
        ui(()->{activity.selectUnitAndFocus(unitId);page();});settle();focus(c.hex,1.55f);clickText("行军");tap(c.hex);
        MarchOrders.Plan toCenter=(MarchOrders.Plan)field(activity,"pendingMarch");require(toCenter!=null&&toCenter.valid()&&toCenter.order.intent==MarchOrders.Intent.MOVE,"marching onto city is explicit MOVE, not inferred entry");
        int cost=toCenter.cost,stock=c.troops;clickText("确认任务");u=world().unit(unitId);
        require(u!=null&&u.hex.equals(c.hex)&&u.movementSpent==cost&&c.troops==stock,"army visibly occupies city center, pays each step, not garrisoned");shot("05-army-on-city");
        World restored=SaveCodec.decode(readAuto());require(restored.unit(unitId)!=null&&restored.unit(unitId).hex.equals(c.hex),"actual automatic save contains city-standing field army");
        Hex destination=new Hex(c.hex.q-3,c.hex.r);clickText("行军");tap(destination);MarchOrders.Plan across=(MarchOrders.Plan)field(activity,"pendingMarch");require(across!=null&&across.valid(),"far-side route valid");shot("06-through-city-route");clickText("确认任务");
        u=world().unit(unitId);require(u!=null&&(u.march==null||u.march.intent==MarchOrders.Intent.MOVE),"paid cross-city MOVE completes or persists without garrison");
        int turn=world().turn;clickText("下一旬  →");ui(()->dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        long deadline=SystemClock.uptimeMillis()+120000;while(SystemClock.uptimeMillis()<deadline){settle();if(!(Boolean)field(activity,"aiRunning")&&world().turn>turn)break;}
        require(!(Boolean)field(activity,"aiRunning")&&world().turn==turn+1,"actual Next Turn UI completed one turn");
        u=world().unit(unitId);require(u!=null&&u.hex.equals(destination)&&u.march==null,"automatic continuation exits city to remote destination");shot("07-after-next-turn");
        ui(()->{activity.selectUnitAndFocus(unitId);page();});settle();focus(c.hex,1.55f);clickText("进驻");tap(new Hex(c.hex.q-1,c.hex.r));
        MarchOrders.Plan entry=(MarchOrders.Plan)field(activity,"pendingMarch");require(entry!=null&&entry.valid()&&entry.order.intent==MarchOrders.Intent.GARRISON,"explicit enter control targets city entity from outer tile");
        shot("08-garrison-edge-preview");clickText("确认任务");require(world().unit(unitId)==null&&world().officer((int)id).cityId==c.id,"actual UI garrison completes at perimeter");
        restored=SaveCodec.decode(readAuto());require(restored.unit(unitId)==null&&restored.officer((int)id).cityId==c.id,"actual file save/read retains completed entry");shot("09-garrison-complete");
        ui(()->activity.recreate());settle();activity=(MainActivity)field(test,"current");require(world().unit(unitId)==null&&world().officer((int)id).cityId==c.id,"Activity recreation reads correct saved unit/resource ownership");
    }
    private void edgeAttack()throws Exception{
        World attack=ScenarioCatalog.load("heroes-250",5,56L);World.City source=attack.city(20017),target=attack.city(20015);World.Officer o=attack.idle(source).get(0);
        World.Result deployed=attack.army.deploy(source.id,o.id,new int[0],World.Weapon.SPEAR,Army.Ship.BOAT,3000,12000,0);require(deployed.ok,"attack fixture uses real deployment");World.Unit u=attack.unit(o.unitId);u.hex=new Hex(target.hex.q-2,target.hex.r);
        require(attack.cityAt(u.hex)==null&&attack.cost(u.hex,u.weapon)>0&&attack.campaign.hostile(u.owner,target.owner),"attack setup at real exterior, unchanged terrain and allegiance");
        launch(attack);int unitId=u.id;ui(()->{activity.selectUnitAndFocus(unitId);page();});settle();focus(target.hex,1.55f);clickText("攻击");Hex rim=new Hex(target.hex.q-1,target.hex.r);tap(rim);
        Displacement.Preview preview=(Displacement.Preview)field(activity,"tacticPreview");require(preview!=null&&preview.valid()&&preview.riskHexes.contains(rim),"actual edge attack preview uses touched legal hit cell");shot("10-luoyang-edge-attack-preview");
        int hp=world().city(target.id).defense;clickText("执行");require(world().city(target.id).defense<hp&&world().unit(unitId).acted,"actual attack confirmation damages city once");shot("11-luoyang-actual-hit");
        require(world().reports.size()>0,"combat recorded by persistent report system");SaveCodec.decode(readAuto());
    }
    private byte[] readAuto()throws Exception{try(InputStream in=test.getTargetContext().openFileInput("auto.sg11")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[16384];for(int n;(n=in.read(buf))>=0;)out.write(buf,0,n);return out.toByteArray();}}
    private AlertDialog dialog()throws Exception{return (AlertDialog)field(activity,"confirmationDialog");}
    private View tag(String name)throws Exception{View v=dialog().getWindow().getDecorView().findViewWithTag(name);require(v!=null,"control exists "+name);return v;}
    private void clickTag(String name)throws Exception{View v=tag(name);ui(v::performClick);settle();}
    private View text(View root,String label){if(root instanceof Button&&((Button)root).getText().toString().equals(label)&&root.getVisibility()==View.VISIBLE)return root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View v=text(group.getChildAt(i),label);if(v!=null)return v;}}return null;}
    private void clickText(String label)throws Exception{View v=text(activity.getWindow().getDecorView(),label);require(v!=null,"visible actual button "+label);ui(v::performClick);settle();}
    private void tap(Hex h)throws Exception{
        MapView map=map();MapCamera camera=(MapCamera)field(map,"camera");float x=((Number)invoke(map,"x",new Class[]{Hex.class},h)).floatValue()*camera.scale+camera.x,y=((Number)invoke(map,"y",new Class[]{Hex.class},h)).floatValue()*camera.scale+camera.y;int[] loc=new int[2];map.getLocationOnScreen(loc);
        require(x>=0&&x<map.getWidth()&&y>=0&&y<map.getHeight(),"touch target inside actual viewport "+h);
        long t=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,x+loc[0],y+loc[1],0),up=MotionEvent.obtain(t,t+70,MotionEvent.ACTION_UP,x+loc[0],y+loc[1],0);test.sendPointerSync(down);test.sendPointerSync(up);down.recycle();up.recycle();settle();
    }
    private interface Checked{void run()throws Exception;}
    private void ui(Checked action)throws Exception{Throwable[] error={null};test.runOnMainSync(()->{try{action.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError(error[0]);}
    private void settle(){test.waitForIdleSync();SystemClock.sleep(450);test.waitForIdleSync();}
    private static Object field(Object obj,String name)throws Exception{Class<?> t=obj.getClass();while(t!=null){try{Field f=t.getDeclaredField(name);f.setAccessible(true);return f.get(obj);}catch(NoSuchFieldException e){t=t.getSuperclass();}}throw new NoSuchFieldException(name);}
    private static Object invoke(Object obj,String method,Class<?>[] types,Object... args)throws Exception{Method m=obj.getClass().getDeclaredMethod(method,types);m.setAccessible(true);return m.invoke(obj,args);}
    private void require(boolean condition,String message){checks++;report.append(checks).append(condition?" PASS ":" FAIL ").append(message).append('\n');if(!condition)throw new AssertionError(message);}
    private void shot(String name)throws Exception{File dir=test.getTargetContext().getExternalFilesDir("smoke");dir.mkdirs();Bitmap b=test.getUiAutomation().takeScreenshot();require(b!=null,"actual screenshot "+name);try(OutputStream out=new FileOutputStream(new File(dir,"v056-"+name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
}
