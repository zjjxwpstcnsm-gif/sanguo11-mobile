package game.sanguo.mobile;

import android.app.Activity;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import game.sanguo.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/** Physical audit: unchanged cold probe, real pointer command chain, separate date and
 * 20+20 lifecycle cases. Authority is read only here; no scenario/command/fixture calls. */
public final class FirebaseAcceptanceInstrumentation extends NativeColdStartInstrumentation {
 private String[] names={"normalColdStartToNativeGame","pureTouchLandscapeChain","pureTouchPortraitChain","dateStateAndPixelEvidence","lifecycleTwentyPlusTwenty"};
 private boolean listing,finishing,longRun; private File evidence; private int failures;
 interface Work {void run()throws Exception;}
 @Override public void onCreate(Bundle args){listing=args!=null&&"true".equals(args.getString("log"));longRun=args!=null&&"true".equals(args.getString("auditLong"));if(longRun)names=new String[]{"normalColdStartToNativeGame","pureTouchLandscapeChain","pureTouchPortraitChain","dateStateAndPixelEvidence","lifecycleTwentyPlusTwenty","mixedGameThirtyMinutes"};super.onCreate(args);}
 private Bundle status(int i){Bundle b=new Bundle();b.putString("id","InstrumentationTestRunner");b.putString("class",getClass().getName());b.putString("test",names[i]);b.putInt("numtests",names.length);b.putInt("current",i+1);return b;}
 @Override public void onStart(){if(listing){for(int i=0;i<names.length;i++){sendStatus(1,status(i));sendStatus(0,status(i));}super.finish(Activity.RESULT_OK,new Bundle());return;}evidence=getTargetContext().getExternalFilesDir("s01");evidence.mkdirs();try{log("DEVICE model="+Build.MODEL+" device="+Build.DEVICE+" api="+Build.VERSION.SDK_INT+" supportedAbis="+Arrays.toString(Build.SUPPORTED_ABIS)+" process64="+android.os.Process.is64Bit()+" fingerprint="+Build.FINGERPRINT);}catch(Exception e){throw new RuntimeException(e);}sendStatus(1,status(0));super.onStart();}
 private void log(String s)throws Exception{Files.write(new File(evidence,"acceptance-runtime.txt").toPath(),(SystemClock.elapsedRealtime()+" "+s+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 private void end(int i,Throwable e,String msg){Bundle b=status(i);b.putString("stream",msg+"\n");if(e!=null){failures++;b.putString("stack",android.util.Log.getStackTraceString(e));}sendStatus(e==null?0:-2,b);}
 private void execute(int i,Work work){sendStatus(1,status(i));try{log("BEGIN "+names[i]);work.run();log("PASS "+names[i]);end(i,null,"PASS "+names[i]);}catch(Throwable e){try{log("FAIL "+names[i]+" "+android.util.Log.getStackTraceString(e));capture("acceptance-failure-"+i);dumpViews();}catch(Throwable ignored){}end(i,e,"FAIL "+names[i]);}}
 @Override public void finish(int code,Bundle results){
  if(finishing||listing){super.finish(code,results);return;}finishing=true;
  String msg=results==null?"":results.getString("stream","");boolean cold=code==Activity.RESULT_OK&&msg.startsWith("PASS COLD_START scoped new-game UI checks=");
  end(0,cold?null:new AssertionError(msg),msg);
  if(cold){execute(1,()->touchChain());execute(2,()->{try{orientation("竖屏");touchChain();}finally{orientation("横屏");}});execute(3,()->dateEvidence());execute(4,()->lifecycle());if(longRun)execute(5,()->longStability());}
  else for(int i=1;i<names.length;i++){Bundle b=status(i);b.putString("stream","BLOCKED: cold-start prerequisite failed\n");sendStatus(-3,b);}
  Bundle done=new Bundle();done.putString("stream","AUDIT completed failures="+failures+"; inspect pixels and scoped cases, never all-R PASS\n");super.finish(Activity.RESULT_OK,done);
 }
 private Rect bounds(View v){Rect r=new Rect();if(!v.isAttachedToWindow()||!v.isShown()||!v.isEnabled()||!v.hasWindowFocus()||!v.getLocalVisibleRect(r))return null;int[] xy=new int[2];v.getLocationOnScreen(xy);r.offset(xy[0],xy[1]);return r.width()>12&&r.height()>12?r:null;}
 private View scan(View v,Predicate<View> match){if(!v.isShown())return null;if(match.test(v)&&bounds(v)!=null)return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View hit=scan(g.getChildAt(i),match);if(hit!=null)return hit;}}return null;}
 private View now(Predicate<View> p){View[] v={null};runOnMainSync(()->{List<View> roots=WindowInspector.getGlobalWindowViews();for(int i=roots.size()-1;i>=0;i--){View r=roots.get(i);if(r.isAttachedToWindow()&&r.hasWindowFocus()&&!r.isLayoutRequested()){v[0]=scan(r,p);if(v[0]!=null)break;}}});return v[0];}
 private void pointer(float x,float y,int action,long start,long when){MotionEvent m=MotionEvent.obtain(start,when,action,x,y,0);sendPointerSync(m);m.recycle();}
 private void tap(View v)throws Exception{Rect[] r={null};runOnMainSync(()->r[0]=bounds(v));check(r[0]!=null,"touch requires visible focused control");log("TOUCH rect="+r[0]+" text="+(v instanceof TextView?((TextView)v).getText():v.getContentDescription())+" tag="+v.getTag());long t=SystemClock.uptimeMillis();pointer(r[0].exactCenterX(),r[0].exactCenterY(),0,t,t);pointer(r[0].exactCenterX(),r[0].exactCenterY(),1,t,t+80);settle();}
 private void swipe(View v,boolean horizontal)throws Exception{Rect[] a={null};runOnMainSync(()->a[0]=bounds(v));check(a[0]!=null,"scroll viewport visible");Rect r=a[0];long t=SystemClock.uptimeMillis();float x=r.exactCenterX(),y=r.exactCenterY();float x0=horizontal?r.left+r.width()*.8f:x,y0=horizontal?y:r.top+r.height()*.8f;float x1=horizontal?r.left+r.width()*.2f:x,y1=horizontal?y:r.top+r.height()*.2f;pointer(x0,y0,0,t,t);for(int i=1;i<=10;i++)pointer(x0+(x1-x0)*i/10,y0+(y1-y0)*i/10,2,t,t+i*35);pointer(x1,y1,1,t,t+380);settle();}
 private View seek(Predicate<View> p,boolean scroll)throws Exception{for(int n=0;n<14;n++){View v=now(p);if(v!=null)return v;View s=scroll?now(x->x instanceof ScrollView||x instanceof ListView):null;if(s!=null)swipe(s,false);else settle();}dumpViews();throw new AssertionError("Visible touch control not found");}
 private void text(String s)throws Exception{tap(seek(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith(s),true));}
 private void tag(String s)throws Exception{tap(seek(v->s.equals(v.getTag()),true));}
 private void describe(String s)throws Exception{tap(seek(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith(s),true));}
 private void dumpViews()throws Exception{StringBuilder b=new StringBuilder();runOnMainSync(()->{for(View root:WindowInspector.getGlobalWindowViews())dump(root,b,0);});log("UI_TREE\n"+b);}
 private void dump(View v,StringBuilder b,int depth){if(!v.isShown())return;b.append(depth).append(' ').append(v.getClass().getSimpleName()).append(' ').append(v.getTag()).append(' ').append(v instanceof TextView?((TextView)v).getText():v.getContentDescription()).append(" visible=").append(bounds(v)).append('\n');if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)dump(g.getChildAt(i),b,depth+1);}}
 private void readWorld(){runOnMainSync(()->world=SessionProbe.view(activity));}
 private void shot(String name)throws Exception{host=(MapHost)field(activity,"map");ready();capture(name+"-screen");surfaceCapture();Files.copy(new File(evidence,"surface.png").toPath(),new File(evidence,name+"-surface.png").toPath(),StandardCopyOption.REPLACE_EXISTING);log(name+" "+host.report());}
 private void closeDialogs()throws Exception{for(int i=0;i<4;i++){View r=now(v->v instanceof TextView&&Arrays.asList("返回","取消","确定").contains(((TextView)v).getText().toString()));if(r==null)break;tap(r);}}
 private void city(World.City c)throws Exception{for(int i=0;i<12;i++){View b=now(v->("定位己方据点 "+c.name).contentEquals(v.getContentDescription()==null?"":v.getContentDescription()));if(b!=null){tap(b);return;}View strip=now(v->v instanceof HorizontalScrollView);check(strip!=null,"city strip visible");swipe(strip,true);}throw new AssertionError("City not reached through touch strip: "+c.name);}
 private float[] screen(Hex h)throws Exception{float[][] p={null};runOnMainSync(()->{try{FilamentMapView f=(FilamentMapView)field(host,"spatial");MapSceneSnapshot snap=(MapSceneSnapshot)field(f,"snapshot");float x=snap.ground.grid.x(h),z=snap.ground.grid.z(h);int[] xy=new int[2];f.getLocationOnScreen(xy);float sx=f.camera.screenX(x,z),sy=f.camera.screenY(x,z,snap.ground.surface.at(h));p[0]=new float[]{xy[0]+sx,xy[1]+sy,sx,sy,f.getWidth(),f.getHeight()};}catch(Exception e){throw new RuntimeException(e);}});return p[0];}
 private boolean onMap(Hex h)throws Exception{float[] p=screen(h);return p[2]>40&&p[2]<p[4]-40&&p[3]>40&&p[3]<p[5]-135;}
 private void cell(Hex h)throws Exception{float[] p=screen(h);check(onMap(h),"map touch inside unobstructed viewport "+h);log("MAP_TOUCH cell="+h+" screen="+Arrays.toString(p));long t=SystemClock.uptimeMillis();pointer(p[0],p[1],0,t,t);pointer(p[0],p[1],1,t,t+80);settle();}
 private void unit(int id)throws Exception{World.Unit u=world.unit(id);check(u!=null,"deployed unit still exists");String officer=world.officer(u.officerId).name;View b=now(v->v.getContentDescription()!=null&&v.getContentDescription().toString().contains(officer)&&v.getContentDescription().toString().startsWith("定位"));if(b!=null)tap(b);else cell(u.hex);}
 private void nextTurn()throws Exception{closeDialogs();readWorld();int before=world.turn;text("下一旬");text("执行");long deadline=SystemClock.uptimeMillis()+120000;do{settle();readWorld();}while((world.turn==before||(Boolean)field(activity,"aiRunning"))&&SystemClock.uptimeMillis()<deadline);check(world.turn==before+1&&!(Boolean)field(activity,"aiRunning"),"one touched turn completed");log("TURN authority="+world.date()+" turn="+world.turn);}
 private void saveLoad()throws Exception{describe("打开功能导航");describe("导航 · 菜单");text("保存局面（3个槽位）");text("槽位 1");View yes=now(v->v instanceof TextView&&"执行".contentEquals(((TextView)v).getText()));if(yes!=null)tap(yes);settle();File f=new File(getTargetContext().getFilesDir(),"manual.sg11");check(f.isFile(),"manual save created through slot UI");byte[] bytes=Files.readAllBytes(f.toPath());closeDialogs();text("读取存档");text("槽位 1");text("执行");settle();readWorld();check(Arrays.equals(bytes,SaveCodec.encode(world)),"touch load authority equals saved file");shot("touch-loaded");log("MANUAL_SAVE_LOAD bytes="+bytes.length);}
 private void orientation(String name)throws Exception{closeDialogs();describe("地图工具 · ");text("屏幕方向");text(name);settle();host=(MapHost)field(activity,"map");ready();log("UI_ORIENTATION "+name);}
 private void touchChain()throws Exception{
  readWorld();World.City base=null,enemy=null;int distance=Integer.MAX_VALUE;for(World.City c:world.cities)if(c.owner==world.player&&!world.idle(c).isEmpty())for(World.City e:world.cities)if(e.owner>=0&&e.owner!=world.player&&c.hex.distance(e.hex)<distance){base=c;enemy=e;distance=c.hex.distance(e.hex);}check(base!=null,"natural deployment city exists");log("TOUCH_PLAN city="+base.name+" enemy="+enemy.name+" distance="+distance+"; read-only selection, no world mutations");city(base);text("出征");text("选用");tag("deploy.tab.1");text(World.Weapon.SWORD.label);tag("deploy.confirm");readWorld();World.Unit created=null;for(World.Unit u:world.units)if(u.owner==world.player&&(created==null||u.id>created.id))created=u;check(created!=null,"touch deployment created unit");int id=created.id;Hex initial=created.hex;shot("touch-deployed");boolean moved=false,attacked=false;
  for(int attempt=0;attempt<7;attempt++){readWorld();World.Unit u=world.unit(id);check(u!=null,"natural campaign unit survives");unit(id);Set<Hex> targets=MapSceneSnapshot.attackTargets(world,id);Hex hit=null;for(Hex h:targets)if(onMap(h)){hit=h;break;}if(hit!=null&&moved){text("攻击");cell(hit);text("执行");settle();readWorld();check(world.unit(id)==null||world.unit(id).acted,"actual attack committed through touch");attacked=true;shot("touch-attacked");break;}
   Hex dest=null;int best=u.hex.distance(enemy.hex);for(Hex h:world.orders.marchReachable(u).keySet())if(!h.equals(u.hex)&&world.cityAt(h)==null&&h.distance(enemy.hex)<best&&onMap(h)){dest=h;best=h.distance(enemy.hex);}if(dest!=null){text("行军");cell(dest);text("确认任务");readWorld();moved|=!world.unit(id).hex.equals(initial);log("MOVED unit="+id+" at="+world.unit(id).hex);shot("touch-move-"+attempt);}nextTurn();}
  check(moved,"actual movement through touch");check(attacked,"natural target attack reached through touch within seven turns");tag("reports.entry");capture("touch-battle-report");closeDialogs();nextTurn();saveLoad();
 }
 private void dateSample(String name)throws Exception{readWorld();String[] displayed={null};int[] month={0};Rect[] rect={null};runOnMainSync(()->{try{TextView v=(TextView)activity.findViewById(android.R.id.content).getRootView().findViewWithTag("hud.date");displayed[0]=v.getText().toString();rect[0]=bounds(v);month[0]=((MapSceneSnapshot)field(field(host,"spatial"),"snapshot")).month;}catch(Exception e){throw new RuntimeException(e);}});log("DATE "+name+" authority="+world.date()+" turn="+world.turn+" snapshot.month="+month[0]+" visibleText="+displayed[0]+" rect="+rect[0]+" recovery="+WindowSurfaceRecovery.report(activity.getWindow().getDecorView()));check(rect[0]!=null,"date control actually shown");check(displayed[0].replaceAll("\\s","").contains(world.date().replaceAll("\\s","")),"date widget matches authority");check(month[0]==(world.startMonth-1+world.turn/3)%12+1,"snapshot month matches authority; full date not in snapshot schema");shot(name);}
 private void dateEvidence()throws Exception{closeDialogs();describe("打开功能导航");describe("导航 · 地图");host=(MapHost)field(activity,"map");if(!host.is3D())throw new AssertionError("date requires native view");dateSample("date-before");for(int i=0;i<3;i++){nextTurn();dateSample("date-after-"+(i+1));}log("DATE pixel evidence captured; requires independent visible-pixel review");}
 private byte[] authority()throws Exception{readWorld();return SaveCodec.encode(world);}
 private String shell(String command)throws Exception{try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand(command))){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] a=new byte[8192];int n;while((n=in.read(a))!=-1)b.write(a,0,n);return b.toString("UTF-8");}}
 private void lifecycle()throws Exception{closeDialogs();host=(MapHost)field(activity,"map");byte[] before=authority();int errors=0;
  for(int i=0;i<20;i++){FilamentMapView old=(FilamentMapView)field(host,"spatial");runOnMainSync(()->host.switchMode(false));boolean retired=(Boolean)field(old,"released")&&!(Boolean)field(old,"queued")&&field(old,"engine")==null;runOnMainSync(()->host.switchMode(true));ready();boolean ok=retired&&(Integer)field(host,"activeNativeHosts")==1&&Arrays.equals(before,authority());if(!ok)errors++;log("SWITCH "+(i+1)+"/20 ok="+ok+" retired="+retired+" "+host.report());if(i==0||i==19)shot("lifecycle-switch-"+(i+1));}
  for(int i=0;i<20;i++){FilamentMapView f=(FilamentMapView)field(host,"spatial");shell("input keyevent KEYCODE_HOME");settle();settle();boolean[] focus={true},resumed={true},queued={true};long[] first={0},last={0};runOnMainSync(()->{try{focus[0]=activity.getWindow().getDecorView().hasWindowFocus();resumed[0]=(Boolean)field(f,"resumed");queued[0]=(Boolean)field(f,"queued");first[0]=(Long)field(f,"renderedFrames");}catch(Exception e){throw new RuntimeException(e);}});settle();settle();runOnMainSync(()->{try{last[0]=(Long)field(f,"renderedFrames");}catch(Exception e){throw new RuntimeException(e);}});boolean stopped=!focus[0]&&!resumed[0]&&!queued[0]&&first[0]==last[0];if(!stopped){errors++;log("HOME_DIAGNOSTIC "+shell("dumpsys activity activities"));capture("lifecycle-home-failure-"+(i+1));}log("BACKGROUND "+(i+1)+"/20 focus="+focus[0]+" resumed="+resumed[0]+" queued="+queued[0]+" frames="+first[0]+"->"+last[0]+" stopped="+stopped);shell("am start -W -f 0x10020000 -n game.sanguo.mobile.dev/game.sanguo.mobile.MainActivity");settle();ready();long resumedFrames=(Long)field(f,"renderedFrames");boolean recovered=resumedFrames>last[0]&&(Integer)field(host,"activeNativeHosts")==1&&Arrays.equals(before,authority());if(!recovered)errors++;log("FOREGROUND "+(i+1)+"/20 recovered="+recovered+" "+host.report());if(i==0||i==19)shot("lifecycle-resume-"+(i+1));}
  check(errors==0,"full 20 switches + 20 background/resume cycles failures="+errors);log("Scope excludes fault injection/delayed worker and long duration performance");
 }
 private void sampleProcess(String label)throws Exception{
  Files.write(new File(evidence,"memory-"+label+".txt").toPath(),shell("dumpsys meminfo game.sanguo.mobile.dev").getBytes("UTF-8"));
  Files.write(new File(evidence,"thermal-"+label+".txt").toPath(),shell("dumpsys thermalservice").getBytes("UTF-8"));
  Files.write(new File(evidence,"gfxinfo-"+label+".txt").toPath(),shell("dumpsys gfxinfo game.sanguo.mobile.dev framestats").getBytes("UTF-8"));
  log("PROCESS_SAMPLE "+label+"; gfxinfo is Android Window data, not Filament presented FPS/GPU duration");
 }
 private void longStability()throws Exception{
  closeDialogs();describe("打开功能导航");describe("导航 · 地图");host=(MapHost)field(activity,"map");ready();
  long started=SystemClock.elapsedRealtime(),deadline=started+30L*60*1000,nextShot=started,nextTurnAt=started+120000;int sample=0,operations=0,turns=0,errors=0;
  while(SystemClock.elapsedRealtime()<deadline){
   readWorld();List<World.City> own=new ArrayList<>();for(World.City c:world.cities)if(c.owner==world.player)own.add(c);
   check(!own.isEmpty(),"long game retains an owned site");World.City c=own.get(operations%Math.min(6,own.size()));city(c);
   boolean panel=(Boolean)field(field(activity,"ui"),"panelVisible");if(panel)tap((View)field(activity,"selectionButton"));
   // Physical screen drag across the actual Surface, no camera setter or world fixture.
   FilamentMapView view=(FilamentMapView)field(host,"spatial");Rect[] b={null};runOnMainSync(()->b[0]=bounds(view));check(b[0]!=null,"long run Surface visible");Rect r=b[0];long t=SystemClock.uptimeMillis();float x=r.exactCenterX(),y=r.top+r.height()*.40f,dx=(operations%2==0?1:-1)*Math.min(120,r.width()*.1f);
   pointer(x,y,0,t,t);for(int k=1;k<=10;k++)pointer(x+dx*k/10,y+15*k/10,2,t,t+35*k);pointer(x+dx,y+15,1,t,t+380);settle();operations++;
   long now=SystemClock.elapsedRealtime();if(now>=nextTurnAt){try{nextTurn();turns++;}catch(Throwable e){errors++;log("LONG_TURN_FAILURE "+android.util.Log.getStackTraceString(e));closeDialogs();}nextTurnAt=now+120000;}
   if(now>=nextShot){shot("long-"+sample);sampleProcess("long-"+sample);log("LONG_PROGRESS elapsedMs="+(now-started)+" touchOperations="+operations+" turns="+turns);sample++;nextShot=now+300000;}
   check(host.is3D(),"long run keeps real native renderer");SystemClock.sleep(5000);
  }
  shot("long-final");sampleProcess("long-final");log("LONG_COMPLETE durationMs="+(SystemClock.elapsedRealtime()-started)+" operations="+operations+" turns="+turns+" failures="+errors);
  check(turns>0&&errors==0,"30 minute mixed game has completed real turns and no caught operation failure");
 }

}
