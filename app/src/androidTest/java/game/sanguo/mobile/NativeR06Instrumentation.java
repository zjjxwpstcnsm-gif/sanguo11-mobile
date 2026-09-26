package game.sanguo.mobile;
import android.app.Activity;
import android.content.Intent;
import android.os.*;
import game.sanguo.core.*;
import java.io.*;
import java.util.*;

/** Production Activity/MapHost, official sites/forest, explicitly labeled unit fixture. */
public final class NativeR06Instrumentation extends SceneInstrumentation {
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
  for(float span:new float[]{5,12,28})for(float yaw:new float[]{0,90}){
   runOnMainSync(()->{try{FilamentMapView v=view();v.center(at);v.camera.span=span;v.camera.yaw=yaw;v.setGridShown(false);}catch(Exception e){throw new RuntimeException(e);}});
   shot("r06-"+name+"-"+(int)span+"-"+(int)yaw,scenario);
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
 @Override public void onStart(){Bundle result=new Bundle();try{
  // Android's actual JSON implementation and every bundled GLB, not host-parser inference.
  for(String family:new String[]{"sites","field"})for(String file:getTargetContext().getAssets().list("3d/"+family))if(file.endsWith(".glb"))try(InputStream in=getTargetContext().getAssets().open("3d/"+family+"/"+file)){check(SiteGlb.read(in).indices.length>0,"installed GLB subset");}
  activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
  runOnMainSync(()->invoke("startScenario",new Class<?>[]{String.class,int.class},"coalition-190",0));
  long end=SystemClock.uptimeMillis()+90000;while(field(activity,"map")==null&&SystemClock.uptimeMillis()<end)settle();
  host=(MapHost)field(activity,"map");runOnMainSync(()->{world=SessionProbe.view(activity);host.switchMode(true);host.quality(SceneQuality.MEDIUM);});assetsReady();byte[] before=SaveCodec.encode(world);
  World.City gate=world.cities.stream().filter(c->c.kind==World.SiteKind.GATE).findFirst().orElseThrow();views(gate.hex,"gate","coalition-190 official");
  MapSceneSnapshot snapshot=(MapSceneSnapshot)field(view(),"snapshot");Set<Hex> excluded=Vegetation.exclusions(snapshot);Hex forest=null;
  for(int q=0;q<world.width&&forest==null;q++)for(int r=0;r<world.height;r++){Hex h=new Hex(q,r);if(world.terrain[q][r]==World.Terrain.FOREST&&Vegetation.eligible(snapshot.ground,excluded,h)){forest=h;break;}}
  check(forest!=null,"official forest available");views(forest,"tree","coalition-190 official");references();
  runOnMainSync(()->world=SessionProbe.view(activity));check(Arrays.equals(before,SaveCodec.encode(world)),"camera and asset decode preserve full authority");
  commandFlow();shot("r06-official-command-load","coalition-190 official");
  World fixture=SaveCodec.decode(SaveCodec.encode(FieldSceneFixture.create(10,true,false)));World.Unit catapult=fixture.units.stream().filter(u->u.weapon==World.Weapon.CATAPULT).findFirst().orElseThrow();
  runOnMainSync(()->{SessionProbe.install(activity,fixture);activity.refresh();world=SessionProbe.view(activity);});assetsReady();byte[] fixtureBefore=SaveCodec.encode(world);
  views(catapult.hex,"catapult","explicit FieldSceneFixture 40x40/10 units; not official map");references();
  FieldAssets cpu=new FieldAssets(name->getTargetContext().getAssets().open("3d/field/"+name));
  check(!Arrays.equals(cpu.pose("unit-CATAPULT-lod0","attack",0,1).vertices,cpu.pose("unit-CATAPULT-lod0","attack",6,1).vertices),"installed lever pose changes");
  // Installed renderer uses actual rigid pose frames; these shots document demonstration poses,
  // not an invented gameplay attack or animation acceptance.
  runOnMainSync(()->{try{view().center(catapult.hex);view().camera.span=3;view().camera.yaw=0;view().setGridShown(true);}catch(Exception e){throw new RuntimeException(e);}});
  shot("r06-catapult-grid","explicit fixture; idle runtime animation");
  runOnMainSync(()->world=SessionProbe.view(activity));check(Arrays.equals(fixtureBefore,SaveCodec.encode(world)),"fixture cameras and animations leave authority unchanged");
  for(int i=0;i<3;i++){
   FilamentMapView old=view();runOnMainSync(()->{host.switchMode(false);host.switchMode(true);});assetsReady();
   runOnMainSync(()->{try{check(((SceneAssetQueue)field(old,"assetWork")).bytes()==0,"released CPU decoded references");check(((Map<?,?>)field(old,"shapes")).isEmpty(),"released GPU cache");}catch(Exception e){throw new RuntimeException(e);}});
  }
  shot("r06-recreated","explicit fixture after three 2D/3D recreations");
  result.putString("stream","PASS R06 "+checks+" installed checks; actual APK surfaces, software emulator; art/ARM64 acceptance separate\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable e){try{capture("r06-failure");}catch(Throwable ignored){}result.putString("stream","FAIL R06 "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
