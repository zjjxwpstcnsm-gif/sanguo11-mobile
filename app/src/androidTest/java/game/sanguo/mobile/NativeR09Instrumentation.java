package game.sanguo.mobile;
import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;
import java.security.MessageDigest;
import java.util.function.Function;

/** Official coalition-190 campaign, no injected map, units, sites or authority. */
public final class NativeR09Instrumentation extends SceneInstrumentation {
 private World reference;
 private File dir;
 private FilamentMapView view()throws Exception{return (FilamentMapView)field(host,"spatial");}
 private void log(String value)throws Exception{try(FileWriter out=new FileWriter(new File(dir,"r09-operations.tsv"),true)){out.write(SystemClock.uptimeMillis()+"\t"+value+"\n");}}
 private static String hash(byte[] value)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(value);StringBuilder s=new StringBuilder();for(byte b:h)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
 private void parity(String label)throws Exception{
  runOnMainSync(()->world=SessionProbe.view(activity));byte[] actual=SaveCodec.encode(world);
  check(Arrays.equals(actual,SaveCodec.encode(reference)),label+" full SaveCodec/RNG equality");log(label+"\tPASS\t"+hash(actual));
 }
 private void command(String label,Function<World,World.Result> action)throws Exception{
  World.Result expected=action.apply(reference);check(expected.ok,label+" reference: "+expected.message);
  runOnMainSync(()->{World.Result result=SessionProbe.command(activity,action::apply);check(result.ok,label+" normal session: "+result.message);activity.refresh();});parity(label);
 }
 private void turn()throws Exception{
  runOnMainSync(()->invoke("advanceTurn",new Class<?>[0]));check(reference.nextTurn().ok,"reference next turn");
  long end=SystemClock.uptimeMillis()+120000;while((Boolean)field(activity,"aiRunning")&&SystemClock.uptimeMillis()<end)settle();
  check(!(Boolean)field(activity,"aiRunning"),"normal turn completed");parity("next-turn");
 }
 private void assetsReady()throws Exception{
  ready();long end=SystemClock.uptimeMillis()+120000;boolean[] ok={false};
  while(SystemClock.uptimeMillis()<end){runOnMainSync(()->{try{FilamentMapView v=view();ok[0]=!(Boolean)field(v,"assetSyncPending")&&((SceneAssetQueue)field(v,"assetWork")).pending()==0&&((SceneWorkQueue<?>)field(v,"meshWork")).pending()==0;}catch(Exception e){throw new RuntimeException(e);}});if(ok[0])break;settle();}
  check(ok[0],"current scene queues settled");settle();
  runOnMainSync(()->{try{check(((Set<?>)field(view(),"missingAssets")).isEmpty(),"no missing assets");}catch(Exception e){throw new RuntimeException(e);}});
 }
 private void camera(Hex at,float span,float yaw)throws Exception{
  runOnMainSync(()->{try{FilamentMapView v=view();v.center(at);v.camera.span=span;v.camera.yaw=yaw;v.camera.tilt=48;}catch(Exception e){throw new RuntimeException(e);}});settle();settle();assetsReady();
 }
 private void shot(String name)throws Exception{
  assetsReady();surfaceCapture();capture(name+"-ui");
  java.nio.file.Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
  String[] report={""};runOnMainSync(()->{try{FilamentMapView v=view();report[0]="source="+BuildConfig.SOURCE_REVISION+"\nprofile="+LandscapeProfile.ID+"\nscenario=coalition-190\nplayer=5\nregion=q120..151/r68..99\ncenterWorld="+v.camera.x+","+v.camera.z+"\nspan="+v.camera.span+"\nyaw="+v.camera.yaw+"\ntilt="+v.camera.tilt+"\nquality="+host.quality()+"\ngrid="+host.gridShown()+"\nterritory="+host.territoryMode()+"\nturn="+SessionProbe.view(activity).turn+"\nUI=visible\nPC_reference=REFERENCE_MISSING\n"+host.report();}catch(Exception e){throw new RuntimeException(e);}});
  try(FileWriter out=new FileWriter(new File(dir,name+".txt"))){out.write(report[0]);}log("capture\t"+name);
 }
 private void cruise()throws Exception{
  camera(new Hex(134,84),14,0);log("continuous-camera-start");
  float[] center=new float[2];runOnMainSync(()->{try{center[0]=view().camera.x;center[1]=view().camera.z;}catch(Exception e){throw new RuntimeException(e);}});
  for(int i=0;i<=100;i++){final float t=i/100f;runOnMainSync(()->{try{FilamentMapView v=view();v.camera.x=center[0]+3*(float)Math.sin(t*Math.PI*2);v.camera.z=center[1]+2*(float)Math.cos(t*Math.PI*2);v.camera.yaw=110*t;v.camera.span=14+4*(float)Math.sin(t*Math.PI*2);}catch(Exception e){throw new RuntimeException(e);}});SystemClock.sleep(200);}
  log("continuous-camera-end");parity("camera cruise preserves state");shot("r09-cruise-end");
 }
 @Override public void onStart(){Bundle result=new Bundle();try{
  activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
  runOnMainSync(()->invoke("startScenario",new Class<?>[]{String.class,int.class},"coalition-190",5));
  long deadline=SystemClock.uptimeMillis()+90000;while(field(activity,"map")==null&&SystemClock.uptimeMillis()<deadline)settle();
  host=(MapHost)field(activity,"map");dir=getTargetContext().getExternalFilesDir("s01");dir.mkdirs();
  runOnMainSync(()->{world=SessionProbe.view(activity);host.switchMode(true);host.quality(SceneQuality.MEDIUM);host.setGridShown(false);host.setTerritoryMode(0);});reference=SaveCodec.decode(SaveCodec.encode(world));
  assetsReady();check(world.city(20015).hex.equals(new Hex(136,79)),"frozen official Luoyang coordinate");
  runOnMainSync(()->invoke("onTile",new Class<?>[]{Hex.class},new Hex(136,79)));parity("select official Luoyang");
  for(float span:new float[]{5,14,22})for(float yaw:new float[]{0,90}){camera(new Hex(136,79),span,yaw);shot("r09-luoyang-"+(int)span+"-"+(int)yaw);}
  for(Hex at:new Hex[]{new Hex(129,79),new Hex(131,90)})for(float yaw:new float[]{0,90}){camera(at,5,yaw);shot("r09-site-"+at.q+"-"+at.r+"-"+(int)yaw);}
  camera(new Hex(134,84),14,0);
  for(boolean grid:new boolean[]{false,true})for(int territory:new int[]{0,1}){
   runOnMainSync(()->{host.setGridShown(grid);host.setTerritoryMode(territory);});shot("r09-overlay-"+grid+"-"+territory);parity("overlay "+grid+"/"+territory);
  }
  for(SceneQuality quality:SceneQuality.values()){
   runOnMainSync(()->{host.setGridShown(false);host.setTerritoryMode(0);host.quality(quality);});camera(new Hex(134,84),14,0);shot("r09-quality-"+quality);parity("quality "+quality);
  }
  runOnMainSync(()->host.quality(SceneQuality.MEDIUM));cruise();
  camera(new Hex(136,79),5,0);
  command("deploy Luoyang officer2004",w->w.deploy(20015,2004,World.Weapon.SWORD,1000));
  int unit=world.units.get(world.units.size()-1).id;
  runOnMainSync(()->activity.selectUnitAndFocus(unit));shot("r09-deploy");
  command("move to official Mengjin approach129,80",w->w.move(unit,new Hex(129,80)));camera(new Hex(129,80),5,0);shot("r09-port-approach");
  byte[] state=SaveCodec.encode(world);runOnMainSync(()->host.switchMode(false));settle();parity("same campaign in 2D");capture("r09-2d-same-state");
  runOnMainSync(()->host.switchMode(true));assetsReady();parity("same campaign back in 3D");check(Arrays.equals(state,SaveCodec.encode(world)),"2D/3D no world rebuild");
  turn();camera(new Hex(129,80),5,0);shot("r09-next-turn");
  command("embark official water128,79",w->w.move(unit,new Hex(128,79)));check(world.army.water(world.unit(unit).hex),"unit really in navigable water");camera(new Hex(128,79),5,0);shot("r09-embarked");
  // Real rejected attack is logged separately; no nearby hostile unit is fabricated.
  byte[] pre=SaveCodec.encode(world);World.Result[] rejection={null};runOnMainSync(()->rejection[0]=SessionProbe.command(activity,w->w.siege(unit,20015)));
  check(!rejection[0].ok,"friendly city attack rejected");parity("rejected friendly attack");check(Arrays.equals(pre,SaveCodec.encode(world)),"rejected attack no mutation");log("successful-hostile-attack\tNOT_RUN\tno legal hostile target along this official port route");
  command("land/garrison official Mengjin20063",w->w.enter(unit,20063));check(world.unit(unit)==null,"unit garrisoned through normal rules");shot("r09-landed-port");
  // Actual application store and normal load activation, using the existing save path.
  runOnMainSync(()->invoke("save",new Class<?>[]{String.class,boolean.class},"manual",false));
  try(InputStream in=getTargetContext().openFileInput("manual.sg11")){check(Arrays.equals(SaveCodec.encode(SaveCodec.read(in)),SaveCodec.encode(world)),"actual manual save bytes");}
  runOnMainSync(()->invoke("loadSlot",new Class<?>[]{String.class},"manual"));parity("load actual manual save");camera(new Hex(129,79),5,90);shot("r09-loaded");
  try(FileOutputStream out=new FileOutputStream(new File(dir,"r09-final.sg11"))){out.write(SaveCodec.encode(world));}
  log("save-load-complete");
  result.putString("stream","PASS R09 "+checks+" installed checks; successful hostile attack/PC/ARM64 acceptance incomplete\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable e){try{capture("r09-failure");}catch(Throwable ignored){}result.putString("stream","FAIL R09 "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
