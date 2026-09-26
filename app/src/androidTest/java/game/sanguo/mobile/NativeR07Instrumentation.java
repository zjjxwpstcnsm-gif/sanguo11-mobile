package game.sanguo.mobile;
import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;

/** Production Activity/MapHost, official sites/forest, explicitly labeled unit fixture. */
public final class NativeR07Instrumentation extends SceneInstrumentation {
 private FilamentMapView view()throws Exception{return (FilamentMapView)field(host,"spatial");}
 private void assetsReady()throws Exception{
  ready();long end=SystemClock.uptimeMillis()+120000;boolean[] done={false};
  while(SystemClock.uptimeMillis()<end){runOnMainSync(()->{try{FilamentMapView v=view();done[0]=!(Boolean)field(v,"assetSyncPending")&&((SceneAssetQueue)field(v,"assetWork")).pending()==0;}catch(Exception e){throw new RuntimeException(e);}});if(done[0])break;settle();}
  check(done[0],"bounded asset queue settled");check(host.is3D(),"production native active");
  runOnMainSync(()->{try{check(((Set<?>)field(view(),"missingAssets")).isEmpty(),"no runtime resource fallback");}catch(Exception e){throw new RuntimeException(e);}});
 }
 private void shot(String name,String scenario)throws Exception{
  settle();settle();assetsReady();surfaceCapture();capture(name+"-ui");File dir=getTargetContext().getExternalFilesDir("s01");
  java.nio.file.Files.copy(new File(dir,"surface.png").toPath(),new File(dir,name+"-surface.png").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
  String[] report={""};runOnMainSync(()->report[0]=host.report());
  try(FileWriter out=new FileWriter(new File(dir,name+".txt"))){out.write("scenario="+scenario+"\n"+report[0]);}
 }
 private void views(Hex at,String name,String scenario)throws Exception{
  for(float span:new float[]{5,24,60})for(float yaw:new float[]{0,90}){
   runOnMainSync(()->{try{FilamentMapView v=view();v.center(at);v.camera.span=span;v.camera.yaw=yaw;v.setGridShown(false);}catch(Exception e){throw new RuntimeException(e);}});
   shot("r07-"+name+"-"+(int)span+"-"+(int)yaw,scenario);
   final int expected=span<15?0:span>48?2:1;
   runOnMainSync(()->{try{check((Integer)field(view(),"siteLod")==expected,"actual near/mid/far site LOD");}catch(Exception e){throw new RuntimeException(e);}});
  }
 }
 private void references()throws Exception{
  runOnMainSync(()->{try{
   FilamentMapView v=view();Map<?,?> objects=(Map<?,?>)field(v,"objects"),shapes=(Map<?,?>)field(v,"shapes");Map<Object,Integer> actual=new IdentityHashMap<>();
   for(Object p:objects.values())for(String member:new String[]{"shape","flagShape","baseShape","stateShape"}){Object g=field(p,member);if(g!=null)actual.put(g,actual.getOrDefault(g,0)+1);}
   boolean shared=false;for(Object g:shapes.values()){int expected=actual.getOrDefault(g,0);check((Integer)field(g,"references")==expected,"GPU live reference ownership");if(expected>1)shared=true;}
   check(shared,"multiple instances share actual GPU buffers, not claimed instancing");
  }catch(Exception e){throw new RuntimeException(e);}});
 }
 private Hex buildFacility(Domestic.Kind kind)throws Exception{
  runOnMainSync(()->world=SessionProbe.view(activity));
  int cityId=-1,officerId=-1;Hex plot=null;
  outer:for(World.City city:world.cities)if(city.owner==world.player)
   for(World.Officer officer:world.idle(city))for(Hex h:world.domestic.buildSites(city.id)){
    World probe=SaveCodec.decode(SaveCodec.encode(world));
    if(probe.domestic.build(city.id,officer.id,kind,h).ok){cityId=city.id;officerId=officer.id;plot=h;break outer;}
   }
  check(plot!=null,"legal ordinary construction "+kind);
  final int c=cityId,o=officerId;final Hex h=plot;
  World reference=SaveCodec.decode(SaveCodec.encode(world));check(reference.domestic.build(c,o,kind,h).ok,"reference facility build");
  runOnMainSync(()->{check(SessionProbe.command(activity,w->w.domestic.build(c,o,kind,h)).ok,"normal GameSession facility build");activity.refresh();world=SessionProbe.view(activity);});
  check(Arrays.equals(SaveCodec.encode(reference),SaveCodec.encode(world)),"facility command parity");
  runOnMainSync(()->{try{view().center(h);view().camera.span=5;}catch(Exception e){throw new RuntimeException(e);}});
  shot("r07-"+kind+"-construction","coalition-190; normal build command");
  for(int turn=0;turn<4&&world.domestic.at(h).remaining>0;turn++){
   runOnMainSync(()->invoke("advanceTurn",new Class<?>[0]));check(reference.nextTurn().ok,"reference construction turn");
   long deadline=SystemClock.uptimeMillis()+120000;while((Boolean)field(activity,"aiRunning")&&SystemClock.uptimeMillis()<deadline)settle();
   check(!(Boolean)field(activity,"aiRunning"),"construction turn finished");
   runOnMainSync(()->world=SessionProbe.view(activity));check(Arrays.equals(SaveCodec.encode(reference),SaveCodec.encode(world)),"construction full-turn parity");
  }
  check(world.domestic.at(h)!=null&&world.domestic.at(h).remaining==0,"ordinary facility complete");return h;
 }
 @Override public void onStart(){Bundle result=new Bundle();try{
  for(String family:new String[]{"sites","field"})for(String file:getTargetContext().getAssets().list("3d/"+family))if(file.endsWith(".glb"))try(InputStream in=getTargetContext().getAssets().open("3d/"+family+"/"+file)){check(SiteGlb.read(in).indices.length>0,"installed GLB subset");}
  activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
  runOnMainSync(()->invoke("startScenario",new Class<?>[]{String.class,int.class},"coalition-190",0));
  long end=SystemClock.uptimeMillis()+90000;while(field(activity,"map")==null&&SystemClock.uptimeMillis()<end)settle();
  host=(MapHost)field(activity,"map");runOnMainSync(()->{world=SessionProbe.view(activity);host.switchMode(true);host.quality(SceneQuality.MEDIUM);});assetsReady();byte[] before=SaveCodec.encode(world);
  MapSceneSnapshot snapshot=(MapSceneSnapshot)field(view(),"snapshot");
  for(String family:new String[]{"city0","city1","city2","port","gate"}){
   MapSceneSnapshot.Item item=snapshot.items.stream().filter(i->i.site!=null&&i.site.model.equals(family)).findFirst().orElseThrow();
   views(item.hex,family+"-"+item.key.replace(':','-'),"coalition-190 official; "+item.label+"; "+item.hex);
  }
  // Find the most compact real city/port/gate trio from the official projection.
  MapSceneSnapshot.Item[] trio=null;float best=Float.MAX_VALUE;
  GridWorldTransform grid=snapshot.ground.grid;
  for(MapSceneSnapshot.Item city:snapshot.items)if(city.site!=null&&city.kind==0)
   for(MapSceneSnapshot.Item port:snapshot.items)if(port.site!=null&&port.kind==1)
    for(MapSceneSnapshot.Item gate:snapshot.items)if(gate.site!=null&&gate.kind==2){
     float minX=Math.min(grid.x(city.hex),Math.min(grid.x(port.hex),grid.x(gate.hex))),maxX=Math.max(grid.x(city.hex),Math.max(grid.x(port.hex),grid.x(gate.hex)));
     float minZ=Math.min(grid.z(city.hex),Math.min(grid.z(port.hex),grid.z(gate.hex))),maxZ=Math.max(grid.z(city.hex),Math.max(grid.z(port.hex),grid.z(gate.hex)));
     float extent=Math.max(maxX-minX,maxZ-minZ);
     if(extent<best){best=extent;trio=new MapSceneSnapshot.Item[]{city,port,gate};}
    }
  check(trio!=null,"official city/port/gate region");
  final MapSceneSnapshot.Item[] region=trio;final float span=Math.max(12,best*2+4);
  runOnMainSync(()->{try{FilamentMapView v=view();v.camera.x=(grid.x(region[0].hex)+grid.x(region[1].hex)+grid.x(region[2].hex))/3;v.camera.z=(grid.z(region[0].hex)+grid.z(region[1].hex)+grid.z(region[2].hex))/3;v.camera.span=span;v.camera.yaw=0;}catch(Exception e){throw new RuntimeException(e);}});
  shot("r07-official-trio","coalition-190 official: "+region[0].label+" / "+region[1].label+" / "+region[2].label);
  references();runOnMainSync(()->world=SessionProbe.view(activity));check(Arrays.equals(before,SaveCodec.encode(world)),"site cameras preserve authority");
  runOnMainSync(()->{try{view().setGridShown(true);}catch(Exception e){throw new RuntimeException(e);}});shot("r07-gate-grid","coalition-190 official gate");
  commandFlow();shot("r07-command-load","coalition-190 official commands/save/load");
  for(Domestic.Kind kind:new Domestic.Kind[]{Domestic.Kind.FARM,Domestic.Kind.MARKET}){
   Hex h=buildFacility(kind);views(h,kind.name(),"coalition-190; actual facility built through GameSession");
  }
  references();
  for(int i=0;i<3;i++){
   FilamentMapView old=view();runOnMainSync(()->{host.switchMode(false);host.switchMode(true);});assetsReady();
   runOnMainSync(()->{try{check(((SceneAssetQueue)field(old,"assetWork")).bytes()==0,"released CPU references");check(((Map<?,?>)field(old,"shapes")).isEmpty(),"released GPU cache");}catch(Exception e){throw new RuntimeException(e);}});
  }
  shot("r07-recreated","coalition-190 after construction and three recreations");
  result.putString("stream","PASS R07 "+checks+" installed checks; official site/facility Surface captures; ARM64/art acceptance separate\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable e){try{capture("r07-failure");}catch(Throwable ignored){}result.putString("stream","FAIL R07 "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
