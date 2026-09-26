#!/usr/bin/env python3
"""Execute the production doFrame body with strict owner-operation fakes.
This is a HOST control-flow regression, never Android/Surface/lifecycle acceptance.
"""
import subprocess
import tempfile
from pathlib import Path
import sys

repo = Path(__file__).resolve().parent.parent
source = Path(sys.argv[1]) if len(sys.argv) > 1 else repo / 'app/src/main/java/game/sanguo/mobile/FilamentMapView.java'
s = source.read_text()
start = s.index('    @Override public void doFrame(long time){')
end = s.index('    /** Check actual display output', start)
method = s[start:end].replace('@Override ', '')
# The real method is compiled verbatim except the inapplicable View override annotation.
head = r'''
import java.util.function.Consumer;
public final class FrameAdmissionHarness {
  long frameCallbacks,beginAttempts,beginSkipped,renderedFrames,surfaceFrames,lastFrame,waterLastTick,animationTick,lastWorkLog,gpuPreparationFrames;
  boolean queued,released,resumed=true,assetSyncPending=true,outputVerified;
  int bufferWidth=100,bufferHeight=100,assetUploadBudget,pending=7,lastMeshUploads=8,cpuCursor,cpuCount;
  long lastMeshUploadNanos=50; long[] cpuSamples=new long[240]; double callbackMillis,waterSeconds;
  int gpuCalls,uploads,failures,scheduled,cancelled,copies;
  boolean throwUpload;
  Object swap=new Object(),quality=new Object(),view=new Object(); Snapshot snapshot=new Snapshot();
  final Renderer renderer=new Renderer(); final MeshWork meshWork=new MeshWork(); final AssetWork assetWork=new AssetWork();
  final Overlay overlay=new Overlay(); final Pacer pacer=new Pacer(); final Thermal thermal=new Thermal();
  final Camera camera=new Camera(); final Lens lens=new Lens(); final Water waterMaterial=new Water();
  final Consumer<Throwable> failure=e->{failures++;};
  void gpu(){if(!renderer.begun)throw new IllegalStateException("GPU work before frame admission");gpuCalls++;}
  void acceptMeshes(Object r){} void schedule(){scheduled++;} void cancelFrame(){cancelled++;}
  void syncObjects(){gpu();assetSyncPending=false;}
  void loadVisible(){gpu();if(throwUpload)throw new IllegalStateException("upload failure");uploads++;lastMeshUploads=1;lastMeshUploadNanos=1;pending=0;}
  void animateReplay(){gpu();} void animateUnits(){gpu();} void animateEffects(){gpu();}
  void applySeason(SeasonStyle s){gpu();} void refreshPendingMeshes(){pending=7;}
  void checkSurfaceOutput(){copies++;} String startupReport(){return "host-only";}
  final class Renderer {boolean admit, begun;int renders,ends;
    boolean beginFrame(Object s,long t){begun=admit;return begun;}
    void render(Object v){gpu();renders++;}
    void endFrame(){if(!begun)throw new AssertionError("end without begin");ends++;begun=false;}
  }
  final class MeshWork {int drained;void drain(Consumer<Object> ready,Consumer<Throwable> error){drained++;}}
  final class AssetWork {int drained;boolean drain(){drained++;return true;}}
  final class Overlay {int draws;void invalidate(){draws++;}}
  static final class Pacer {boolean due(long t,int fps){return true;}}
  static final class Thermal {int fps(Object q){return 30;}}
  static final class Snapshot {int month=7;}
  static final class SeasonStyle {static SeasonStyle forMonth(int m){return new SeasonStyle();}}
  static final class Camera {double width=100,height=100,span=10,x,z;double backX(){return 0;}double cos(){return 1;}double sin(){return 1;}double rightX(){return 1;}static final class Projection {static final int ORTHO=0;}}
  final class Lens {void setProjection(int p,double... a){gpu();}void lookAt(double... a){gpu();}}
  final class Water {Water getDefaultInstance(){return this;}void setParameter(String s,float f){gpu();}}
  static final class UiMotion {static boolean enabled(){return true;}}
  static void check(boolean c,String m){if(!c)throw new AssertionError(m);}
'''
tail = r'''
  public static void main(String[] args){
    FrameAdmissionHarness h=new FrameAdmissionHarness();
    for(int i=1;i<=40;i++)h.doFrame(i*40_000_000L);
    check(h.failures==0,"rejected frames must not attempt GPU preparation");
    check(h.beginSkipped==40&&h.meshWork.drained==40&&h.assetWork.drained==40,"CPU mailboxes progress under genuine frame rejection");
    check(h.gpuCalls==0&&h.uploads==0&&h.renderer.ends==0&&h.renderedFrames==0,"no rejected-frame GPU writes or fictitious submissions");
    check(h.lastMeshUploads==0&&h.lastMeshUploadNanos==0&&h.pending>0,"rejected frame diagnostics keep real outstanding work");
    check(h.overlay.draws==40&&h.scheduled==40&&h.assetSyncPending,"Android overlay scheduling and deferred object sync survive rejection");
    h.renderer.admit=true;h.doFrame(2_000_000_000L);
    check(h.renderer.renders==1&&h.renderer.ends==1&&h.gpuPreparationFrames==1&&h.renderedFrames==1,"accepted frame prepares, renders and ends exactly once");
    check(h.uploads==1&&h.assetUploadBudget==2&&!h.assetSyncPending,"original asset budget and deferred synchronization retained");
    int calls=h.gpuCalls;h.renderer.admit=false;h.doFrame(2_040_000_000L);
    check(h.gpuCalls==calls&&h.lastMeshUploads==0,"late rejection does not reuse or conceal prior upload work");
    FrameAdmissionHarness zero=new FrameAdmissionHarness();zero.bufferWidth=0;zero.doFrame(1);
    check(zero.gpuCalls==0&&zero.beginAttempts==0&&zero.overlay.draws==1,"zero-sized surface never prepares GPU resources");
    FrameAdmissionHarness paused=new FrameAdmissionHarness();paused.resumed=false;paused.doFrame(1);
    check(paused.gpuCalls==0&&paused.meshWork.drained==0&&paused.scheduled==0,"paused owner does not advance work");
    FrameAdmissionHarness dead=new FrameAdmissionHarness();dead.released=true;dead.doFrame(1);
    check(dead.beginAttempts==0&&dead.gpuCalls==0,"released owner never enters frame");
    FrameAdmissionHarness missing=new FrameAdmissionHarness();missing.swap=null;missing.doFrame(1);
    check(missing.gpuCalls==0&&missing.meshWork.drained==0,"destroyed surface never enters frame");
    FrameAdmissionHarness error=new FrameAdmissionHarness();error.renderer.admit=true;error.throwUpload=true;error.doFrame(1);
    check(error.failures==1&&error.cancelled==1&&error.renderer.ends==1&&error.renderedFrames==0,"upload exception closes accepted frame but cannot claim submission");
    System.out.println("PASS: extracted production doFrame admission, 41 fake-driver rejections, mailbox/UI progress, zero-size, pause, release, Surface absence, exception closure (HOST ONLY)");
  }
}
'''
with tempfile.TemporaryDirectory(prefix='native-frame-admission-') as folder:
    root=Path(folder)
    (root/'FrameAdmissionHarness.java').write_text(head+method+tail)
    (root/'android/os').mkdir(parents=True)
    (root/'android/util').mkdir(parents=True)
    (root/'android/os/SystemClock.java').write_text('package android.os; public final class SystemClock {public static long uptimeMillis(){return 5000;}}')
    (root/'android/util/Log.java').write_text('package android.util; public final class Log {public static int i(String t,String s){return 0;}}')
    subprocess.run(['javac','--release','17','-d',folder]+[str(p) for p in root.rglob('*.java')],check=True)
    subprocess.run(['java','-cp',folder,'FrameAdmissionHarness'],check=True)
