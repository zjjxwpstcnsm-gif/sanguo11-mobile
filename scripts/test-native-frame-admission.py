#!/usr/bin/env python3
"""Execute the production doFrame body with strict owner-operation fakes.
This is a HOST control-flow regression, never Android/Surface/lifecycle acceptance.
"""
import subprocess
import tempfile
from pathlib import Path
import sys
import os

repo = Path(os.environ.get('NATIVE_FRAME_TEST_REPO', Path(__file__).resolve().parent.parent))
source = Path(sys.argv[1]) if len(sys.argv) > 1 else repo / 'app/src/main/java/game/sanguo/mobile/FilamentMapView.java'
s = source.read_text()
def extract_method(text, signature):
    start = text.index(signature)
    body = text.index('{', start)
    depth = 1
    end = body + 1
    while depth:
        if text[end] == '{': depth += 1
        elif text[end] == '}': depth -= 1
        end += 1
    return text[start:end] + "\n"

# Narrow source contracts complement the executable frame/helper tests. They do
# not stand in for installed Android layout, focus, or touch acceptance.
def compact(text):
    return ''.join(text.split())

snapshot_contract=compact(extract_method(s, '    void snapshot(MapSceneSnapshot next)'))
gate='if((pendingFit||pendingInitialFocus!=null)&&(getWidth()==0||getHeight()==0)){pendingLayoutSnapshot=next;return;}'
consume='Hextarget=pendingInitialFocus;pendingInitialFocus=null;pendingFit=false;'
xfocus='camera.x=next.ground.grid.x(target);'
zfocus='camera.z=next.ground.grid.z(target);'
span='camera.span=10;'
window='terrainWindow=newSceneMesh.TerrainWindow('
submit='meshWork.submitPhased('
for needle in (gate,consume,xfocus,zfocus,span,window,submit):
    assert needle in snapshot_contract, 'initial-focus source contract missing: '+needle
assert snapshot_contract.index(gate)<snapshot_contract.index(consume)<snapshot_contract.index(xfocus)<snapshot_contract.index(zfocus)<snapshot_contract.index(span)<snapshot_contract.index(window)<snapshot_contract.index(submit), 'real layout, one-shot target and native span must precede first CPU request'
assert 'pendingInitialFocus=null;' in compact(extract_method(s,'    void release()')), 'release clears deferred focus'
resize_contract=compact(extract_method(s,'    @Override protected void onSizeChanged('))
assert 'MapSceneSnapshotnext=pendingLayoutSnapshot;pendingLayoutSnapshot=null;snapshot(next);' in resize_contract, 'first real layout consumes exactly the retained snapshot'
host=(repo/'app/src/main/java/game/sanguo/mobile/MapHost.java').read_text()
host_switch=compact(extract_method(host,'    private void switchMode(boolean use3D,boolean manual,Hex initialTarget)'))
assert host_switch.index('spatial.restoreCamera(camera)')<host_switch.index('spatial.initialFocus(initialTarget)')<host_switch.index('dirty=true;publish();'), 'atomic native target overrides saved camera before publish'
assert 'switchMode(true,true,target);' in compact(extract_method(host,'    void focusNative(Hex target)')), 'normal native focus uses atomic entry'
activity=(repo/'app/src/main/java/game/sanguo/mobile/MainActivity.java').read_text()
assert 'map.focusNative(landmarkHex(' in compact(extract_method(activity,'    private void showLandmarkPicker()')), 'normal landmark menu calls atomic entry'
print('PASS: initial-focus source contracts: real-layout deferral, one-shot clear, grid projection/span before first request, release, deferred snapshot drain and normal menu/host ordering (SOURCE CONTRACT ONLY)')

method = extract_method(s, '    @Override public void doFrame(long time){').replace('@Override ', '')
method += extract_method(s, '    private void selectObjectLods()')
site=(repo/'app/src/main/java/game/sanguo/mobile/SiteVisual.java').read_text()
a=site.index('    static int lod(');b=site.index('    static SceneMesh fallback',a)
selectors='static final class SiteVisual {'+site[a:b]+'}\n'
unit=(repo/'app/src/main/java/game/sanguo/mobile/UnitLod.java').read_text()
a=unit.index('    static int select(');b=unit.index('    static int idleFrame',a)
selectors+='static final class UnitLod {'+unit[a:b]+'}\n'
# The real method is compiled verbatim except the inapplicable View override annotation.
head = r'''
import java.util.function.Consumer;
public final class FrameAdmissionHarness {
  long frameCallbacks,beginAttempts,beginSkipped,renderedFrames,surfaceFrames,lastFrame,waterLastTick,animationTick,lastWorkLog,gpuPreparationFrames;
  boolean queued,released,resumed=true,assetSyncPending=true,outputVerified,terrainVisibilityDirty;
  int bufferWidth=100,bufferHeight=100,assetUploadBudget,pending=7,lastMeshUploads=8,cpuCursor,cpuCount;
  long lastMeshUploadNanos=50; long[] cpuSamples=new long[240]; double callbackMillis,waterSeconds;
  int gpuCalls,uploads,failures,scheduled,cancelled,copies,cpuWindowChecks,windowRequests,windowVersion=1,requestedWindowVersion,resizeCalls;
  boolean cpuWindowPending;
  final Metrics frameMetrics=new Metrics();
  int siteLod=1,unitLod=1,firstSite=-1,firstUnit=-1,syncs;
  boolean throwUpload;
  Object swap=new Object(),view=new Object(); Quality quality=new Quality(); Snapshot snapshot=new Snapshot();
  final Renderer renderer=new Renderer(); final MeshWork meshWork=new MeshWork(); final AssetWork assetWork=new AssetWork();
  final Overlay overlay=new Overlay(); final Pacer pacer=new Pacer(); final Thermal thermal=new Thermal();
  final Camera camera=new Camera(); final Lens lens=new Lens(); final Water waterMaterial=new Water(),vegetationMaterial=new Water();
  final Consumer<Throwable> failure=e->{failures++;};
  void gpu(){if(!renderer.begun)throw new IllegalStateException("GPU work before frame admission");gpuCalls++;}
  void acceptMeshes(Object r){}
  void requestCameraWindow(){
    check(!renderer.begun,"CPU window request must precede GPU frame admission");
    check(meshWork.drained>cpuWindowChecks,"CPU window request follows terrain result drain");
    cpuWindowChecks++;
    if(!cpuWindowPending&&requestedWindowVersion!=windowVersion){
      requestedWindowVersion=windowVersion;cpuWindowPending=true;windowRequests++;
    }
  }
  void drainTerrainResults(){meshWork.drain(this::acceptMeshes,failure);}
  void drainLandscapeResults(){}
  void resizeSurface(){resizeCalls++;}
  void schedule(){scheduled++;} void cancelFrame(){cancelled++;}
  void syncObjects(){gpu();if(syncs++==0){firstSite=siteLod;firstUnit=unitLod;}assetSyncPending=false;}
  void loadVisible(){gpu();if(throwUpload)throw new IllegalStateException("upload failure");uploads++;lastMeshUploads=1;lastMeshUploadNanos=1;pending=0;}
  void animateReplay(){gpu();} void animateUnits(){gpu();} void animateEffects(){gpu();}
  void applySeason(SeasonStyle s){gpu();} void refreshPendingMeshes(){pending=7;}
  void checkSurfaceOutput(){copies++;} String startupReport(){return "host-only";}
  final class Renderer {boolean admit, begun;int renders,ends;
    boolean beginFrame(Object s,long t){
      check(cpuWindowChecks==beginAttempts,"CPU window demand checked before every eligible beginFrame");
      begun=admit;return begun;
    }
    void render(Object v){gpu();renders++;}
    void endFrame(){if(!begun)throw new AssertionError("end without begin");ends++;begun=false;}
  }
  final class MeshWork {int drained;void drain(Consumer<Object> ready,Consumer<Throwable> error){drained++;}}
  final class AssetWork {int drained;boolean changed=true;boolean drain(){drained++;boolean result=changed;changed=false;return result;}}
  final class Overlay {int draws;void invalidate(){draws++;}}
  static final class Pacer {boolean admit=true;int resets;void reset(){resets++;}boolean due(long t,int fps){return admit;}}
  static final class Thermal {boolean changed;boolean tick(long t){boolean value=changed;changed=false;return value;}int fps(Object q){return 30;}}
  static final class Metrics {int samples;void record(long a,long b,long c,long d,long e,boolean accepted){samples++;}}
  static final class Snapshot {int month=7;}
  static final class SeasonStyle {static SeasonStyle forMonth(int m){return new SeasonStyle();}}
  static final class Quality {int minSiteLod,minUnitLod;}
  static final class Camera {float span=86.256714f;double width=100,height=100,x,z;double backX(){return 0;}double cos(){return 1;}double sin(){return 1;}double rightX(){return 1;}static final class Projection {static final int ORTHO=0;}}
  final class Lens {void setProjection(int p,double... a){gpu();}void lookAt(double... a){gpu();}}
  final class Water {Water getDefaultInstance(){return this;}void setParameter(String s,float f){gpu();}}
  static final class UiMotion {static boolean enabled(){return true;}}
  static final class WindowSurfaceRecovery {static void changed(Object view){}}
  static void check(boolean c,String m){if(!c)throw new AssertionError(m);}
'''
tail = r'''
  public static void main(String[] args){
    FrameAdmissionHarness h=new FrameAdmissionHarness();
    for(int i=1;i<=40;i++)h.doFrame(i*40_000_000L);
    check(h.failures==0,"rejected frames must not attempt GPU preparation");
    check(h.beginSkipped==40&&h.meshWork.drained==40&&h.assetWork.drained==40,"CPU mailboxes progress under genuine frame rejection");
    check(h.gpuCalls==0&&h.uploads==0&&h.renderer.ends==0&&h.renderedFrames==0,"no rejected-frame GPU writes or fictitious submissions");
    check(h.cpuWindowChecks==40&&h.windowRequests==1,"rejected admission still starts CPU window work without duplicating in-flight demand");
    check(h.lastMeshUploads==0&&h.lastMeshUploadNanos==0&&h.pending>0,"rejected frame diagnostics keep real outstanding work");
    check(h.overlay.draws==40&&h.scheduled==40&&h.assetSyncPending,"Android overlay scheduling and deferred object sync survive rejection");
    h.windowVersion=2;h.cpuWindowPending=false;h.doFrame(1_800_000_000L);
    check(h.windowRequests==2&&h.requestedWindowVersion==2&&h.gpuCalls==0&&h.renderedFrames==0,"changed-camera CPU request starts before another rejected beginFrame");
    h.doFrame(1_840_000_000L);
    check(h.windowRequests==2&&h.cpuWindowChecks==42,"same in-flight window remains bounded across repeated rejection");
    h.renderer.admit=true;h.doFrame(2_000_000_000L);
    check(h.renderer.renders==1&&h.renderer.ends==1&&h.gpuPreparationFrames==1&&h.renderedFrames==1,"accepted frame prepares, renders and ends exactly once");
    check(h.uploads==1&&h.assetUploadBudget==2&&!h.assetSyncPending,"original asset budget and deferred synchronization retained");
    check(h.firstSite==2&&h.firstUnit==2,"FIRST_ASSET_LOD: national first request must use far site and unit assets");
    h.camera.span=45;h.doFrame(2_010_000_000L);
    check(h.siteLod==2&&h.unitLod==2&&h.syncs==1,"national hysteresis does not requeue unchanged assets");
    h.camera.span=18;h.doFrame(2_020_000_000L);
    check(h.siteLod==1&&h.unitLod==1&&h.syncs==2,"zoom selects both LODs before one synchronization");
    FrameAdmissionHarness near=new FrameAdmissionHarness();near.camera.span=6;near.renderer.admit=true;near.doFrame(1);
    check(near.firstSite==0&&near.firstUnit==0,"fresh close view requests close assets on its first synchronization");
    FrameAdmissionHarness low=new FrameAdmissionHarness();low.camera.span=6;low.quality.minSiteLod=2;low.quality.minUnitLod=2;low.renderer.admit=true;low.doFrame(1);
    check(low.firstSite==2&&low.firstUnit==2,"quality minimums are retained before requests");
    int calls=h.gpuCalls;h.renderer.admit=false;h.doFrame(2_040_000_000L);
    check(h.gpuCalls==calls&&h.lastMeshUploads==0,"late rejection does not reuse or conceal prior upload work");
    FrameAdmissionHarness zero=new FrameAdmissionHarness();zero.bufferWidth=0;zero.doFrame(1);
    check(zero.gpuCalls==0&&zero.beginAttempts==0&&zero.overlay.draws==1,"zero-sized surface never prepares GPU resources");
    FrameAdmissionHarness paused=new FrameAdmissionHarness();paused.resumed=false;paused.doFrame(1);
    check(paused.gpuCalls==0&&paused.meshWork.drained==0&&paused.scheduled==0&&paused.cpuWindowChecks==0,"paused owner does not advance work");
    FrameAdmissionHarness dead=new FrameAdmissionHarness();dead.released=true;dead.doFrame(1);
    check(dead.beginAttempts==0&&dead.gpuCalls==0&&dead.cpuWindowChecks==0,"released owner never enters frame");
    FrameAdmissionHarness missing=new FrameAdmissionHarness();missing.swap=null;missing.doFrame(1);
    check(missing.gpuCalls==0&&missing.meshWork.drained==0&&missing.cpuWindowChecks==0,"destroyed surface never enters frame");
    FrameAdmissionHarness paced=new FrameAdmissionHarness();paced.pacer.admit=false;paced.doFrame(1);
    check(paced.beginAttempts==0&&paced.cpuWindowChecks==0&&paced.scheduled==1,"paced callback preserves one next opportunity without advancing owner work");
    FrameAdmissionHarness heated=new FrameAdmissionHarness();heated.thermal.changed=true;heated.doFrame(1);
    check(heated.terrainVisibilityDirty&&heated.resizeCalls==1&&heated.pacer.resets==1&&heated.gpuCalls==0,"thermal transition retains resize and pacing behavior before a rejected frame");
    FrameAdmissionHarness error=new FrameAdmissionHarness();error.renderer.admit=true;error.throwUpload=true;error.doFrame(1);
    check(error.failures==1&&error.cancelled==1&&error.renderer.ends==1&&error.renderedFrames==0,"upload exception closes accepted frame but cannot claim submission");
    System.out.println("PASS: extracted production doFrame admission, 43 fake-driver rejections, changed-camera CPU window demand, mailbox/UI progress, zero-size, pacing, thermal, pause, release, Surface absence, exception closure (HOST ONLY; fake worker tests call ordering, not real queue/driver throughput)");
  }
}
'''
# Also compile the actual CPU-only window helper with the production coverage policy.
# This verifies its one-in-flight bound and camera/LOD request semantics separately
# from the doFrame fake, which intentionally tests only placement relative to admission.
window_source=(repo/'app/src/main/java/game/sanguo/mobile/SceneMesh.java').read_text()
window_type=extract_method(window_source, '    static final class TerrainWindow {')
window_method=extract_method(s, '    private void requestCameraWindow()')
window_head=r'''
import java.util.*;
import java.util.function.*;
public final class CameraWindowHarness {
  boolean released,pendingFit; Hex pendingInitialFocus; int pending;
  final Camera camera=new Camera();
  MapSceneSnapshot snapshot=new MapSceneSnapshot();
  SceneMesh.TerrainWindow terrainWindow;
  List<SceneMesh> chunks=List.of(new SceneMesh()),woods=List.of(new SceneMesh());
  SceneMesh backdropSource=new SceneMesh();
  Set<Hex> woodExcluded=Set.of(new Hex()); FieldAssets fieldAssets=new FieldAssets();
  final MeshWork meshWork=new MeshWork();
  MapSceneSnapshot.Ground builtGround; SceneMesh.TerrainWindow builtWindow;
  List<SceneMesh> builtChunks,builtWoods; SceneMesh builtBackdrop; Set<Hex> builtExcluded;
  FieldAssets builtAssets;
  static final class MapSceneSnapshot {final Ground ground=new Ground();static final class Ground {final Grid grid=new Grid();}}
  static final class Grid {float x(Hex h){return h.q;}float z(Hex h){return h.r+(h.q%2)*.5f;}}
  static final class Hex {final int q,r;Hex(){this(0,0);}Hex(int q,int r){this.q=q;this.r=r;}} static final class FieldAssets {}
  static final class Camera {float x=31,z=183.5f,span=10;float extentX(){return span;}float extentZ(){return span*1.2f;}}
  final class MeshWork {int count,inFlight;Function<Consumer<Object>,Object> work;
    int pending(){return inFlight;}
    void submitPhased(Function<Consumer<Object>,Object> next){check(inFlight==0,"never duplicate an in-flight CPU request");work=next;inFlight=1;count++;}
    void complete(){check(work!=null,"fake worker has captured a request");work.apply(x->{});work=null;inFlight=0;}
  }
  Object buildMeshes(MapSceneSnapshot.Ground ground,boolean changed,SceneMesh scenery,List<SceneMesh> previous,List<SceneMesh> trees,FieldAssets assets,Set<Hex> excluded,SceneMesh.TerrainWindow window,long queuedAt,Consumer<Object> publish){
    check(!changed,"camera-only window request retains terrain identity");
    builtGround=ground;builtWindow=window;builtChunks=previous;builtWoods=trees;builtBackdrop=scenery;builtExcluded=excluded;builtAssets=assets;return new Object();
  }
  static void check(boolean c,String m){if(!c)throw new AssertionError(m);}
'''
window_tail=r'''
  public static void main(String[] args){
    CameraWindowHarness h=new CameraWindowHarness();
    MapSceneSnapshot.Ground original=h.snapshot.ground;
    h.requestCameraWindow();
    check(h.meshWork.count==1&&h.pending==1,"uncovered camera schedules real helper demand immediately");
    check(h.terrainWindow.x==31&&h.terrainWindow.z==183.5f&&h.terrainWindow.span==10,"request uses current target camera and close LOD");
    for(int i=0;i<40;i++)h.requestCameraWindow();
    check(h.meshWork.count==1,"repeated rejected frame opportunities retain one in-flight request");
    h.camera.x=147;h.camera.z=59.5f;h.requestCameraWindow();
    check(h.meshWork.count==1,"camera change does not interrupt active immutable work");
    h.meshWork.complete();
    check(h.builtGround==original&&h.builtWindow.x==31&&h.builtChunks==h.chunks&&h.builtWoods==h.woods&&h.builtBackdrop==h.backdropSource&&h.builtExcluded==h.woodExcluded&&h.builtAssets==h.fieldAssets,"worker captures existing immutable geometry and resources");
    h.requestCameraWindow();
    check(h.meshWork.count==2&&h.terrainWindow.x==147&&h.terrainWindow.z==59.5f,"completed obsolete window schedules latest camera without GPU admission");
    h.meshWork.complete();h.requestCameraWindow();
    check(h.meshWork.count==2,"covered completed view does not requeue");
    h.camera.span=15;h.requestCameraWindow();
    check(h.meshWork.count==3&&SceneMesh.TerrainWindow.level(h.terrainWindow.span)==1,"crossing ordinary LOD threshold requests replacement");
    h.meshWork.complete();h.camera.x+=1;h.requestCameraWindow();
    check(h.meshWork.count==3,"small covered camera movement reuses prefetch margin");
    CameraWindowHarness empty=new CameraWindowHarness();empty.snapshot=null;empty.requestCameraWindow();
    check(empty.meshWork.count==0,"no immutable snapshot means no worker request");
    CameraWindowHarness dead=new CameraWindowHarness();dead.released=true;dead.requestCameraWindow();
    check(dead.meshWork.count==0,"released owner cannot schedule worker");
    CameraWindowHarness deferred=new CameraWindowHarness();deferred.snapshot=null;deferred.pendingFit=true;
    Hex first=new Hex(31,183),latest=new Hex(147,59);
    float previousX=deferred.camera.x;deferred.initialFocus(first);deferred.focus(latest);
    check(deferred.pendingInitialFocus==latest&&!deferred.pendingFit&&deferred.camera.x==previousX,"latest target before layout replaces pending initial focus without moving an unbound camera");
    deferred.focus(null);check(deferred.pendingInitialFocus==latest,"null focus cannot discard latest deferred target");
    CameraWindowHarness bound=new CameraWindowHarness();bound.camera.span=15;bound.focus(latest);
    check(bound.camera.x==147&&bound.camera.z==59.5f&&bound.camera.span==10,"loaded focus uses immutable ground projection and ordinary span");
    System.out.println("PASS: extracted focus methods: latest target before layout, null preservation and normal loaded focus (HOST ONLY)");
    System.out.println("PASS: extracted requestCameraWindow and production TerrainWindow coverage: initial focus, in-flight bound, immutable capture, changed camera, LOD replacement, covered reuse, absent/released snapshot (HOST ONLY)");
  }
}
'''
window_program=window_head+'static final class SceneMesh {'+window_type+'}\n'+window_method+extract_method(s,'    void initialFocus(Hex h)')+extract_method(s,'    void focus(Hex h)')+window_tail

with tempfile.TemporaryDirectory(prefix='native-frame-admission-') as folder:
    root=Path(folder)
    (root/'FrameAdmissionHarness.java').write_text(head+selectors+method+tail)
    (root/'CameraWindowHarness.java').write_text(window_program)
    (root/'android/os').mkdir(parents=True)
    (root/'android/util').mkdir(parents=True)
    (root/'android/os/Debug.java').write_text('package android.os; public final class Debug {public static long threadCpuTimeNanos(){return System.nanoTime();}}')
    (root/'android/os/Trace.java').write_text('package android.os; public final class Trace {public static void beginSection(String s){} public static void endSection(){}}')
    (root/'android/os/SystemClock.java').write_text('package android.os; public final class SystemClock {public static long uptimeMillis(){return 5000;}}')
    (root/'android/util/Log.java').write_text('package android.util; public final class Log {public static int i(String t,String s){return 0;}}')
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-d',folder]+[str(p) for p in root.rglob('*.java')],check=True)
    subprocess.run(['java','-cp',folder,'FrameAdmissionHarness'],check=True)
    subprocess.run(['java','-cp',folder,'CameraWindowHarness'],check=True)
