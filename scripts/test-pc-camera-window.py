#!/usr/bin/env python3
"""Execute the actual camera-request method against the production worker queue.

Host control-flow proof only. Installed focus/render verification is also needed.
"""
import subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
source=(ROOT/'app/src/main/java/game/sanguo/mobile/FilamentMapView.java').read_text()
def method(signature,text=source):
    start=text.index(signature);a=text.index('{',start);depth=1;end=a+1
    while depth:
        if text[end]=='{':depth+=1
        elif text[end]=='}':depth-=1
        end+=1
    return text[start:end]
text=r'''
package game.sanguo.mobile;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
public class PcCameraWindowHarness {
 static class TerrainSurface {}
 static class MapSceneSnapshot {Ground ground=new Ground();int month=1;static class Ground{TerrainSurface surface=new TerrainSurface();}}
 static class SceneMesh {int chunkQ,chunkR;SceneMesh(){}SceneMesh(int q,int r){chunkQ=q;chunkR=r;}__TERRAIN_WINDOW__ __RETAIN_COVERAGE__}
 static class FieldAssets {} static class PcScenery {} static class PcCliffWalls {} static class Hex {} static class MeshResult {int id;MeshResult(int id){this.id=id;}}
 static class Camera {float x=0,z=135,span=5;float extentX(){return span;}float extentZ(){return span;}}
 MapSceneSnapshot snapshot=new MapSceneSnapshot();boolean released;SceneMesh.TerrainWindow terrainWindow=new SceneMesh.TerrainWindow(108,119,50,50,50);
 Camera camera=new Camera();SceneWorkQueue<MeshResult> meshWork=new SceneWorkQueue<>();List<SceneMesh> chunks=List.of(),woods=List.of();SceneMesh backdropSource;
 TerrainSurface backdropSurface;Set<Hex> woodExcluded=Set.of();FieldAssets fieldAssets=new FieldAssets();PcScenery pcScenery=new PcScenery();PcCliffWalls pcCliffWalls=new PcCliffWalls();int pending;
 static AtomicInteger builds=new AtomicInteger();static boolean rebuilt;
 static MeshResult buildMeshes(MapSceneSnapshot.Ground g,boolean changed,SceneMesh scenery,List<SceneMesh> prev,List<SceneMesh> trees,FieldAssets assets,PcScenery nativeAssets,PcCliffWalls nativeWalls,int month,Set<Hex> excluded,SceneMesh.TerrainWindow window,long at,Consumer<MeshResult> publish){rebuilt=changed;return new MeshResult(builds.incrementAndGet());}
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  SceneMesh oldA=new SceneMesh(0,0),oldB=new SceneMesh(16,0),newA=new SceneMesh(0,0),newC=new SceneMesh(0,16);
  List<SceneMesh> previous=List.of(oldA,oldB),partial=new ArrayList<>(List.of(newA,newC));
  List<SceneMesh> covered=SceneMesh.retainWindowCoverage(previous,partial);
  check(covered.size()==3&&covered.contains(newA)&&covered.contains(oldB)&&covered.contains(newC)&&!covered.contains(oldA),"partial replacement preserves missing old coverage and selects exact new key");
  partial.clear();check(covered.size()==3&&previous.size()==2,"published window owns immutable list independent of worker's next batch");
  try{covered.clear();throw new AssertionError("mutable published window");}catch(UnsupportedOperationException expected){}
  SceneMesh signed=new SceneMesh(-16,-16);check(SceneMesh.retainWindowCoverage(List.of(signed),List.of(newA)).size()==2,"signed chunk coordinates have distinct keys");
  check(SceneMesh.retainWindowCoverage(previous,List.of()).equals(previous),"empty replacement retains entire previous coverage");
  check(SceneMesh.retainWindowCoverage(List.of(),covered).equals(covered),"initial window retains every completed source mesh");
  PcCameraWindowHarness h=new PcCameraWindowHarness();CountDownLatch started=new CountDownLatch(1);
  h.meshWork.submit(()->{started.countDown();Thread.sleep(30000);return new MeshResult(-1);});check(started.await(2,TimeUnit.SECONDS),"old national window started");
  h.requestCameraWindow();check(h.terrainWindow.x==0&&h.terrainWindow.span==5,"focus replaces in-flight national demand");
  check(h.pending==1&&h.meshWork.waiting()<=1,"same bounded worker remains active");
  AtomicInteger accepted=new AtomicInteger();long end=System.nanoTime()+5_000_000_000L;
  while(h.meshWork.pending()!=0&&System.nanoTime()<end){h.meshWork.drain(r->accepted.set(r.id),e->{throw new AssertionError(e);});Thread.sleep(5);}
  check(accepted.get()==1&&rebuilt,"latest focused window completes and rebuilds missing backdrop");
  int count=builds.get();h.backdropSurface=h.snapshot.ground.surface;h.backdropSource=new SceneMesh();h.requestCameraWindow();check(builds.get()==count&&h.meshWork.pending()==0,"covered window does not churn");
  h.camera.x=60;h.requestCameraWindow();end=System.nanoTime()+5_000_000_000L;
  while(h.meshWork.pending()!=0&&System.nanoTime()<end){h.meshWork.drain(r->accepted.set(r.id),e->{throw new AssertionError(e);});Thread.sleep(5);}
  check(accepted.get()==2&&!rebuilt,"subsequent camera demand reuses matching surface backdrop");h.meshWork.close();
  System.out.println("PASS production camera request: active cancellation, latest focus, bounded queue, backdrop ownership and no covered-window churn");
 }
'''+method('    private void requestCameraWindow()')+'\n}\n'
text=text.replace('__TERRAIN_WINDOW__',method('    static final class TerrainWindow {',(ROOT/'app/src/main/java/game/sanguo/mobile/SceneMesh.java').read_text()))
text=text.replace('__RETAIN_COVERAGE__',method('    static List<SceneMesh> retainWindowCoverage(', (ROOT/'app/src/main/java/game/sanguo/mobile/SceneMesh.java').read_text()))
out=ROOT/'app/build/pc-camera-window-check';out.mkdir(parents=True,exist_ok=True)
java=out/'PcCameraWindowHarness.java';java.write_text(text)
subprocess.run(['javac','--release','17','-d',str(out),str(java),str(ROOT/'app/src/main/java/game/sanguo/mobile/SceneWorkQueue.java')],check=True)
subprocess.run(['java','-cp',str(out),'game.sanguo.mobile.PcCameraWindowHarness'],check=True)
