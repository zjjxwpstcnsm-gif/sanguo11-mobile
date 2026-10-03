#!/usr/bin/env python3
"""Run production buildMeshes ordering with bounded worker and gated CPU terrain.

Geometry/Android dependencies are surrogates. This tests source publication,
old coverage and incomplete-task semantics; it is not installed GPU evidence.
"""
import subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def method(signature,path):
    text=path.read_text();start=text.index(signature);at=text.index('{',start);depth=1;end=at+1
    while depth:
        if text[end]=='{':depth+=1
        elif text[end]=='}':depth-=1
        end+=1
    return text[start:end]

source=method('    private static MeshResult buildMeshes(',ROOT/'app/src/main/java/game/sanguo/mobile/FilamentMapView.java')
merge=method('    static List<SceneMesh> retainWindowCoverage(',ROOT/'app/src/main/java/game/sanguo/mobile/SceneMesh.java')
harness=r'''
package game.sanguo.mobile;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
public class PcSourceDeliveryHarness {
 static class TerrainSurface{}
 static class MapSceneSnapshot{static class Ground{Object pcMap=new Object();TerrainSurface surface=new TerrainSurface();}}
 static class Hex{}static class FieldAssets{}
 static class SceneMesh{
  int chunkQ,chunkR;SceneMesh(int q){chunkQ=q;}
  static class TerrainWindow{}
  static class BuildStats{long geometryNanos,heightNanos,blendNanos,shoreNanos;int sharedSamples,timedFieldSamples,builtChunks;Runnable progress;Consumer<List<SceneMesh>> firstLoadBatch;}
  static CountDownLatch terrainStarted,terrainAllowed;
  static SceneMesh newGround=new SceneMesh(0),tree=new SceneMesh(1),wall=new SceneMesh(2);
  static SceneMesh backdrop(MapSceneSnapshot.Ground g){return new SceneMesh(3);}
  static List<SceneMesh> ground(MapSceneSnapshot.Ground g,List<SceneMesh> old,TerrainWindow w,BuildStats stats)throws Exception{
   terrainStarted.countDown();if(!terrainAllowed.await(3,TimeUnit.SECONDS))throw new AssertionError("gated terrain timeout");
   stats.firstLoadBatch.accept(List.of(newGround));return List.of(newGround);
  }
  __MERGE__
 }
 static class PcScenery{List<SceneMesh> buildWindow(MapSceneSnapshot.Ground g,Set<Hex> e,List<SceneMesh> old,SceneMesh.TerrainWindow w,int m){return List.of(SceneMesh.tree);}}
 static class PcCliffWalls{List<SceneMesh> buildWindow(MapSceneSnapshot.Ground g,List<SceneMesh> old,SceneMesh.TerrainWindow w,int m){return List.of(SceneMesh.wall);}}
 static class Vegetation{static List<SceneMesh> buildWindow(MapSceneSnapshot.Ground g,Set<Hex> e,List<SceneMesh> old,FieldAssets a,SceneMesh.TerrainWindow w){return List.of(SceneMesh.tree);}}
 static class MeshResult{final SceneMesh scenery;final List<SceneMesh> ground,trees;MeshResult(SceneMesh s,List<SceneMesh> g,List<SceneMesh> t,TerrainSurface surface){scenery=s;ground=g;trees=t;}}
 static long runtimeCounter(String key){return 0;}
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
 public static void main(String[] args)throws Exception{
  for(boolean nativeMap:new boolean[]{true,false}){
   MapSceneSnapshot.Ground g=new MapSceneSnapshot.Ground();if(!nativeMap)g.pcMap=null;
   SceneMesh.terrainStarted=new CountDownLatch(1);SceneMesh.terrainAllowed=new CountDownLatch(1);
   SceneMesh old=new SceneMesh(0),unbuilt=new SceneMesh(16);List<SceneMesh> previous=List.of(old,unbuilt);
   SceneWorkQueue<MeshResult> queue=new SceneWorkQueue<>();List<MeshResult> results=new ArrayList<>();
   queue.submitPhased(publish->buildMeshes(g,true,null,previous,List.of(),new FieldAssets(),new PcScenery(),new PcCliffWalls(),1,Set.of(),new SceneMesh.TerrainWindow(),System.nanoTime(),publish));
   try{
    check(SceneMesh.terrainStarted.await(2,TimeUnit.SECONDS),"terrain stage reached without owner join");
    queue.drain(results::add,e->{throw new AssertionError(e);});
    check(queue.pending()==1,"early scenery never completes terrain task or fabricates ready");
    check(nativeMap?results.size()==1&&results.get(0).trees.equals(List.of(SceneMesh.tree,SceneMesh.wall))&&results.get(0).ground.equals(previous):results.isEmpty(),"source scenery before gated terrain; legacy remains ground first");
    SceneMesh.terrainAllowed.countDown();long end=System.nanoTime()+3_000_000_000L;
    while(queue.pending()!=0&&System.nanoTime()<end){queue.drain(results::add,e->{throw new AssertionError(e);});Thread.sleep(2);}
    check(queue.pending()==0,"all source stages finish through same bounded mailbox");
    MeshResult partial=results.get(nativeMap?1:0);check(partial.ground.contains(SceneMesh.newGround)&&partial.ground.contains(unbuilt)&&!partial.ground.contains(old),"partial delivers exact new key and retains missing old coverage");
    MeshResult last=results.get(results.size()-1);check(last.ground.equals(List.of(SceneMesh.newGround)),"final complete window drops stale CPU coverage");
    check(nativeMap?last.trees.equals(List.of(SceneMesh.tree,SceneMesh.wall)):last.trees.equals(List.of(SceneMesh.tree)),"exact source objects/walls retained; legacy compatible");
   }finally{SceneMesh.terrainAllowed.countDown();queue.close();}
  }
  System.out.println("PASS production source/legacy delivery: early source objects, incomplete-task gate, partial coverage and final window (HOST ONLY)");
 }
 __BUILD__
}
'''.replace('__MERGE__',merge).replace('__BUILD__',source)
out=ROOT/'app/build/pc-source-delivery-check';out.mkdir(parents=True,exist_ok=True)
java=out/'PcSourceDeliveryHarness.java';java.write_text(harness)
debug=out/'android/os/Debug.java';debug.parent.mkdir(parents=True,exist_ok=True);debug.write_text('package android.os;public class Debug{public static long threadCpuTimeNanos(){return System.nanoTime();}}')
log=out/'android/util/Log.java';log.parent.mkdir(parents=True,exist_ok=True);log.write_text('package android.util;public class Log{public static int i(String t,String m){return 0;}}')
subprocess.run(['javac','--release','17','-d',str(out),str(java),str(debug),str(log),str(ROOT/'app/src/main/java/game/sanguo/mobile/SceneWorkQueue.java')],check=True)
subprocess.run(['java','-cp',str(out),'game.sanguo.mobile.PcSourceDeliveryHarness'],check=True)
