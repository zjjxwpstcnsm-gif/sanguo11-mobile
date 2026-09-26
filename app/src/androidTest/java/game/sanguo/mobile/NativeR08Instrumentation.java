package game.sanguo.mobile;
import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;

/** Production Activity/MapHost, official sites/forest, explicitly labeled unit fixture. */
public final class NativeR08Instrumentation extends SceneInstrumentation {
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
   shot("r08-"+name+"-"+(int)span+"-"+(int)yaw,scenario);
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
  shot("r08-"+kind+"-construction","coalition-190; normal build command");
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
  MapSceneSnapshot.Item gate=snapshot.items.stream().filter(i->i.site!=null&&i.kind==2).findFirst().orElseThrow();
  views(gate.hex,"valley-gate","coalition-190 official; "+gate.label+"; "+gate.hex);
  Hex forestRoad=null;int bestForest=-1;
  for(int r=4;r<snapshot.ground.height-4;r++)for(int q=4;q<snapshot.ground.width-4;q++){
   Hex h=new Hex(q,r);if(!Vegetation.path(snapshot.ground,h))continue;
   int forest=0;boolean edge=false;
   for(Hex n:h.neighbors())if(snapshot.ground.valid(n)&&snapshot.ground.terrain[n.r*snapshot.ground.width+n.q]==World.Terrain.FOREST.ordinal())edge=true;
   if(!edge)continue;
   for(int rr=r-4;rr<=r+4;rr++)for(int qq=q-4;qq<=q+4;qq++)if(snapshot.ground.terrain[rr*snapshot.ground.width+qq]==World.Terrain.FOREST.ordinal())forest++;
   if(forest>bestForest){forestRoad=h;bestForest=forest;}
  }
  check(forestRoad!=null,"official forest-road region exists");views(forestRoad,"forest-road","coalition-190 official; "+forestRoad);
  // Compare cached production placement after a camera trip; no second World/session.
  final Hex focus=forestRoad;
  Map<Long,Long> beforeMeshes=new HashMap<>();
  runOnMainSync(()->{try{for(SceneMesh m:(List<SceneMesh>)field(view(),"woods"))beforeMeshes.put(((long)m.chunkR<<32)|m.chunkQ,m.fingerprint);}catch(Exception e){throw new RuntimeException(e);}});
  for(Hex at:new Hex[]{gate.hex,focus}){
   runOnMainSync(()->{try{FilamentMapView v=view();v.center(at);v.camera.span=60;v.camera.yaw=90;}catch(Exception e){throw new RuntimeException(e);}});
   shot(at.equals(focus)?"r08-return-forest":"r08-return-valley","coalition-190 official; "+at);
  }
  runOnMainSync(()->{try{for(SceneMesh m:(List<SceneMesh>)field(view(),"woods")){Long hash=beforeMeshes.get(((long)m.chunkR<<32)|m.chunkQ);if(hash!=null)check(hash==m.fingerprint,"camera travel preserves forest fingerprint");}}catch(Exception e){throw new RuntimeException(e);}});
  references();runOnMainSync(()->world=SessionProbe.view(activity));check(Arrays.equals(before,SaveCodec.encode(world)),"site cameras preserve authority");
  runOnMainSync(()->{try{view().setGridShown(true);}catch(Exception e){throw new RuntimeException(e);}});shot("r08-gate-grid","coalition-190 official gate");
  commandFlow();shot("r08-command-load","coalition-190 official commands/save/load");
  for(Domestic.Kind kind:new Domestic.Kind[]{Domestic.Kind.FARM}){
   Hex h=buildFacility(kind);views(h,kind.name(),"coalition-190; actual facility built through GameSession");
  }
  references();
  for(int i=0;i<3;i++){
   FilamentMapView old=view();runOnMainSync(()->{host.switchMode(false);host.switchMode(true);});assetsReady();
   runOnMainSync(()->{try{check(((SceneAssetQueue)field(old,"assetWork")).bytes()==0,"released CPU references");check(((Map<?,?>)field(old,"shapes")).isEmpty(),"released GPU cache");}catch(Exception e){throw new RuntimeException(e);}});
  }
  shot("r08-recreated","coalition-190 after construction and three recreations");
  result.putString("stream","PASS R08 "+checks+" installed checks; official site/facility Surface captures; ARM64/art acceptance separate\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable e){try{capture("r08-failure");}catch(Throwable ignored){}result.putString("stream","FAIL R08 "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
