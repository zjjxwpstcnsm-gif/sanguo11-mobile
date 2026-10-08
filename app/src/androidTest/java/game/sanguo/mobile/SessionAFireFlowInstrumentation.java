package game.sanguo.mobile;
import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.view.View;
import android.widget.*;
import game.sanguo.core.*;
import game.sanguo.api.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/** Actual menu/new/deploy/player fire/extinguish/turn expiry/save/load/cold; no World edits. */
public final class SessionAFireFlowInstrumentation extends SessionAScenePresentationInstrumentation {
 private boolean pauseProbe;
 @Override public void onCreate(Bundle args){pauseProbe="true".equals(args.getString("pauseFire","false"));super.onCreate(args);}
 private String system(String command)throws Exception{
  try(var descriptor=getUiAutomation().executeShellCommand(command);var in=new FileInputStream(descriptor.getFileDescriptor())){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[2048];for(int n;(n=in.read(buffer))!=-1;){bytes.write(buffer,0,n);if(bytes.size()>65536)throw new IOException("Unexamined diagnostic shell response extent");}return bytes.toString("UTF-8").trim();}
 }
 private double fireClock()throws Exception{double[] value={0};runOnMainSync(()->{try{MapHost host=(MapHost)field(activity,"map");FilamentMapView view=(FilamentMapView)field(host,"spatial");value[0]=(Double)field(field(view,"pcMapEffects"),"clock");}catch(Exception e){throw new RuntimeException(e);}});return value[0];}
 private void pausedFire(Hex target)throws Exception{
  byte[] before=capture();StateToken token=activity.deploymentState();
  check(getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME),"actual system Home action accepted");
  long homeDeadline=SystemClock.uptimeMillis()+10000;boolean[] background={false};
  while(SystemClock.uptimeMillis()<homeDeadline){runOnMainSync(()->{try{MapHost host=(MapHost)field(activity,"map");FilamentMapView view=(FilamentMapView)field(host,"spatial");background[0]=!(Boolean)field(host,"resumed")&&!(Boolean)field(view,"resumed")&&!activity.hasWindowFocus();}catch(Exception e){throw new RuntimeException(e);}});if(background[0])break;SystemClock.sleep(50);}
  check(background[0],"actual Home pauses Activity/map/Filament and removes window focus");SystemClock.sleep(700);double homeClock=fireClock();SystemClock.sleep(700);check(fireClock()==homeClock,"real Home stops existing native fire visual clock");
  getTargetContext().startActivity(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));nativeTarget(target,"pause-home-restored-fire",true);check(Arrays.equals(before,capture())&&token.equals(activity.deploymentState()),"real burning Home/resume entire Save/RNG/StateToken unchanged");
  String original=system("settings get global animator_duration_scale");check(original.equals("null")||original.matches("[0-9.]+"),"examined original system animation preference");boolean enabled=UiMotion.enabled();
  try{system("settings put global animator_duration_scale 0");long until=SystemClock.uptimeMillis()+10000;while(UiMotion.enabled()&&SystemClock.uptimeMillis()<until)SystemClock.sleep(100);check(!UiMotion.enabled(),"actual reduced-motion setting pauses native visual updates");nativeTarget(target,"pause-reduced-motion-real-fire",true);SystemClock.sleep(700);double paused=fireClock();SystemClock.sleep(800);check(fireClock()==paused,"real original fire clock remains frozen with reduced motion");check(Arrays.equals(before,capture())&&token.equals(activity.deploymentState()),"motion pause keeps entire Save/bothRNG/StateToken");}
  finally{system("settings put global animator_duration_scale "+(original.equals("null")?(enabled?"1":"0"):original));long until=SystemClock.uptimeMillis()+5000;while(UiMotion.enabled()!=enabled&&SystemClock.uptimeMillis()<until)SystemClock.sleep(50);if(original.equals("null"))system("settings delete global animator_duration_scale");check(original.equals(system("settings get global animator_duration_scale"))&&UiMotion.enabled()==enabled,"actual animation preference/enabled state restored exactly");}
  nativeTarget(target,"pause-resumed-original-fire",true);
 }
 private int deploy(World.City city)throws Exception {
  World w=SessionProbe.view(activity);World.Officer leader=w.idle(city).stream().filter(o->w.government.commandLimit(o.id)>=5000).max(Comparator.comparingInt(o->o.intelligence)).orElseThrow();
  note("actual deploy leader="+leader.name+" id="+leader.id+" commandLimit="+w.government.commandLimit(leader.id)+" intelligence="+leader.intelligence);
  nav("地图");description("定位己方据点 "+city.name);text("出征");ListView roster=(ListView)tag("deploy.officers");
  invoke("showRosterTag",new Class<?>[]{ListView.class,String.class},roster,"deploy.role."+leader.id);tap(tag("deploy.role."+leader.id));await(v->("从编队移除 "+leader.name).equals(v.getContentDescription()));
  tap(tag("deploy.tab.1"));World.Weapon equipment=city.equipment[World.Weapon.SPEAR.ordinal()]>=5000?World.Weapon.SPEAR:World.Weapon.SWORD;note("actual equipment="+equipment+" spearStock="+city.equipment[World.Weapon.SPEAR.ordinal()]+" troops="+city.troops);description(equipment==World.Weapon.SPEAR?"枪兵 库存":"剑兵 无需库存");revealDescription("兵力数量");EditText troops=(EditText)await(v->v instanceof EditText&&"兵力数量".equals(v.getContentDescription()));invoke("enter",new Class<?>[]{EditText.class,String.class},troops,"5000");sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);settle();tap(tag("deploy.confirm"));
  World after=SessionProbe.view(activity);World.Unit unit=after.unit(after.officer(leader.id).unitId);check(unit!=null&&!unit.acted,"normal second/player fire formation deploy");return unit.id;
 }
 private Hex fireTarget(int actor,int other){
  World w=SessionProbe.view(activity);World.Unit a=w.unit(actor),b=w.unit(other);int range=w.war.plotRange(other,War.Plot.EXTINGUISH);
  return a.hex.neighbors().stream().filter(h->w.inside(h)&&w.unitAt(h)==null&&w.war.fireAt(h)==null&&w.war.plotError(actor,h,War.Plot.FIRE)==null&&b.hex.distance(h)<=range).findFirst().orElseThrow();
 }
 private void menuOption(String prefix)throws Exception {
  boolean[] found={false};runOnMainSync(()->{for(View root:android.view.inspector.WindowInspector.getGlobalWindowViews())if(root.hasWindowFocus()){
   View hit=search(root,v->v instanceof ListView);if(hit instanceof ListView){ListView list=(ListView)hit;for(int i=0;i<list.getAdapter().getCount();i++)if(String.valueOf(list.getAdapter().getItem(i)).startsWith(prefix)){list.setSelection(i);found[0]=true;break;}}
  }});check(found[0],"real dialog scroll item "+prefix);settle();option(prefix);
 }
 private void plot(int actor,Hex target,String label,boolean cancel)throws Exception {
  selectUnit(actor);pose(target);text("计略");option(label+" ·");tile(target);text(cancel?"取消":"执行");settle();
 }
 private void nativeTarget(Hex target,String label,boolean burning)throws Exception {
  byte[] before=capture();StateToken token=activity.deploymentState();
  View[] cancel={null};runOnMainSync(()->cancel[0]=search(activity.getWindow().getDecorView(),v->v instanceof Button&&"取消".contentEquals(((Button)v).getText())));if(cancel[0]!=null)tap(cancel[0]);pose(target);
  long deadline=SystemClock.uptimeMillis()+120000;boolean[] ready={false};String[] report={""};
  while(SystemClock.uptimeMillis()<deadline){runOnMainSync(()->{try{
   MapHost host=(MapHost)field(activity,"map");FilamentMapView view=(FilamentMapView)field(host,"spatial");PcMapEffects fx=(PcMapEffects)field(view,"pcMapEffects");MapSceneSnapshot snap=(MapSceneSnapshot)field(host,"publishedSnapshot");
   boolean shown=snap!=null&&snap.fires.stream().anyMatch(f->f.hex.equals(target));
   ready[0]=snap!=null&&snap.authoritativeSceneFacts&&token.equals(snap.state)&&shown==burning&&fx!=null&&fx.fireScene&&token.equals(field(fx,"displayedFireState"))&&(Long)field(fx,"frames")>2&&((String)field(fx,"error")).isEmpty()&&(Integer)field(view,"pending")==0&&!(Boolean)field(view,"assetSyncPending")&&(Boolean)field(view,"outputVerified");report[0]=host.report();
  }catch(Exception e){throw new RuntimeException(e);}});if(ready[0])break;SystemClock.sleep(100);}
  note(label+" target="+target+" burning="+burning+" "+report[0]);check(ready[0],"current native source fire target lifecycle "+label);shot(label);
  check(Arrays.equals(before,capture())&&token.equals(activity.deploymentState()),"source fire rendering/selection preserves full Save/bothRNG/token "+label);
 }
 @Override public void onStart(){Bundle result=new Bundle();try{
  evidence=new File(getTargetContext().getExternalFilesDir("session-b"),coldMode?"fieldworks-cold":"fieldworks");evidence.mkdirs();put("output",evidence);
  activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));put("activity",activity);await(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));
  if(coldMode){
   byte[] expected=Files.readAllBytes(new File(getTargetContext().getExternalFilesDir("session-b"),"fieldworks/actual-build.sg11").toPath());check(Arrays.equals(expected,capture()),"true cold full saved fire world/bothRNG auto read");verifySceneFacts("cold-fire-restored");
   nav("菜单");text("读取存档");preparedOption("槽位 3");text("读取存档");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));settle();check(Arrays.equals(expected,capture()),"true cold ordinary manual full fire save");verifySceneFacts("cold-fire-manual");
   result.putString("stream","PASS SESSION A FIRE COLD actual new-process auto/manual fire/fullsave/RNG/native13; ARM pending\n");
  }else{
   nav("菜单");text("新游戏 / 选择势力");var source=PcScenarioCatalog.all().get(14);revealDescription("选择PC来源剧本 "+source.identity.path);awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("确认开局势力"));text("曹操");configurePcOpening(source.identity.scenarioId,0,0,0);description("确认开局势力");text("开始新局");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));settle();
   World.City city=SessionProbe.view(activity).home();int first=deploy(city);city=SessionProbe.view(activity).home();int second=deploy(city);Hex target=fireTarget(first,second);
   byte[] before=capture();StateToken token=activity.deploymentState();plot(first,target,"火计",true);check(Arrays.equals(before,capture())&&token.equals(activity.deploymentState()),"normal cancel fire preserves complete Save/bothRNG/token");
   boolean lit=false;for(int attempt=0;attempt<4&&!lit;attempt++){World w=SessionProbe.view(activity);note("player fire attempt="+attempt+" actor="+first+" target="+target+" chance="+w.war.plotChance(first,target,War.Plot.FIRE));plot(first,target,"火计",false);lit=SessionProbe.view(activity).war.fireAt(target)!=null;if(!lit)advance("fire-retry-"+attempt);}
   check(lit,"actual player normal fire success reported in B state, no injected burning");nativeTarget(target,"01-player-fire",true);if(pauseProbe)pausedFire(target);
   before=capture();token=activity.deploymentState();text("视图");menuOption("3D 画质");option("低 ·");nativeTarget(target,"02-low-quality-fire",true);check(Arrays.equals(before,capture())&&token.equals(activity.deploymentState()),"actual quality switch pure source fire");
   plot(second,target,"灭火",false);check(SessionProbe.view(activity).war.fireAt(target)==null,"actual player extinguish updates B state");nativeTarget(target,"03-player-extinguished",false);
   advance("reset-before-reignite");boolean relit=false;for(int attempt=0;attempt<4&&!relit;attempt++){plot(first,target,"火计",false);relit=SessionProbe.view(activity).war.fireAt(target)!=null;if(!relit)advance("reignite-retry-"+attempt);}
   check(relit,"same source cell real player reignite");nativeTarget(target,"04-real-reignite",true);byte[] burningSave=capture();Files.write(new File(evidence,"burning-save.sg11").toPath(),burningSave);
   File slot=new File(activity.getFilesDir(),"manual3.sg11");boolean existed=slot.isFile();nav("菜单");text("保存局面");option("槽位 3");if(existed)text("覆盖存档");long saveDeadline=SystemClock.uptimeMillis()+120000;while(SystemClock.uptimeMillis()<saveDeadline&&(!slot.isFile()||!Arrays.equals(burningSave,Files.readAllBytes(slot.toPath()))))SystemClock.sleep(100);check(slot.isFile()&&Arrays.equals(burningSave,Files.readAllBytes(slot.toPath())),"normal whole burning world manual save");
   for(int turn=0;turn<4&&SessionProbe.view(activity).war.fireAt(target)!=null;turn++)advance("real-fire-expiry-"+turn);
   check(SessionProbe.view(activity).war.fireAt(target)==null,"real B full-turn expiry removes burning");nativeTarget(target,"05-real-expired",false);
   nav("菜单");text("读取存档");preparedOption("槽位 3");text("读取存档");awaitPreparation(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("打开功能导航"));settle();check(Arrays.equals(burningSave,capture()),"normal burning load exact full Save/RNG");nativeTarget(target,"06-real-burning-readback",true);verifySceneFacts("actual-player-fire-build");
   Files.write(new File(evidence,"actual-build.sg11").toPath(),capture());
   result.putString("stream","PASS SESSION A FIRE normal Source14/deploy-two/cancel/player-fire/low-quality/player-extinguish/reignite/multiple-whole-turns/real-expiry/full-burning-save-read/native13; new process and ARM pending\n");
  }
 }catch(Throwable failure){result.putString("stream","FAIL SESSION A FIRE "+android.util.Log.getStackTraceString(failure));try{shot("failed");}catch(Throwable ignored){}}
 finally{try{if(activity!=null)runOnMainSync(()->activity.finish());settle();}catch(Throwable failure){result.putString("stream",result.getString("stream","")+"\nFAIL lifecycle "+failure);}}
 try{Files.write(new File(evidence,"result.txt").toPath(),result.getString("stream","").getBytes("UTF-8"));}catch(Exception ignored){}finish(Activity.RESULT_OK,result);
 }
}
