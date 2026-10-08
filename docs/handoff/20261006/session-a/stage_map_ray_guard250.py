#!/usr/bin/env python3
"""Stage actual map-input readiness in A test helper; no live APK mutation."""
from pathlib import Path
import hashlib,json,difflib,subprocess
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';OUT=ROOT/'out/session-a/map-ray-guard250'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    assert not OUT.exists();OUT.mkdir(parents=True)
    path='app/src/androidTest/java/game/sanguo/mobile/UiUxInstrumentation.java';p=ROOT/path;before=p.read_text();after=before
    start=after.index('    private boolean routePoint(');end=after.index('    private boolean visibleMapPoint(',start);segment=after[start:end]
    assert segment.count('boolean[] matches={true}')==1
    segment=segment.replace('boolean[] matches={true}','boolean[] matches={false}')
    old='if(spatial!=null){int[] origin=new int[2];';assert segment.count(old)==1
    new='''if(spatial!=null&&field(host,"loadingCurtain")==null&&host.isAttachedToWindow()&&host.isShown()&&host.isEnabled()&&host.hasWindowFocus()
                &&spatial.isAttachedToWindow()&&!((Boolean)field(spatial,"released"))&&!((Boolean)field(spatial,"loadingCovered"))
                &&((Boolean)field(spatial,"outputVerified"))&&((Long)field(spatial,"renderedFrames"))>2
                &&((Integer)field(spatial,"pending"))==0&&!((Boolean)field(spatial,"assetSyncPending"))
                &&field(spatial,"presentationCue")==null&&spatial.presentationReady()){int[] origin=new int[2];'''
    segment=segment.replace(old,new);after=after[:start]+segment+after[end:]
    target=OUT/path;target.parent.mkdir(parents=True);target.write_text(after);patch=DOC/'MAP_RAY_GUARD250.patch';patch.write_text(''.join(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+path,tofile='b/'+path)))
    java=Path('/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home/bin/java');android=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platforms/android-35/android.jar')
    base=ROOT/'out/session-a/native-opening-stage224/compile';dependencies=ROOT/'out/session-a/native-opening-stage224/dependencies';tests=ROOT/'app/build/intermediates/javac/debugAndroidTest/compileDebugAndroidTestJavaWithJavac/classes'
    assert tests.is_dir();cp=':'.join(map(str,[android,base/'filament.jar',base/'classes',tests]+sorted(dependencies.glob('*.jar'))));classes=OUT/'classes';classes.mkdir()
    cmd=[str(java),'-Xmx512m','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-cp',cp,'-d',str(classes),str(target)]
    with (OUT/'compile.log').open('w') as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
    assert r.returncode==0,(OUT/'compile.log').read_text()[-5000:]
    assert p.read_text()==before
    replay=OUT/'patch-readback';copy=replay/path;copy.parent.mkdir(parents=True);copy.write_text(before);subprocess.run(['git','apply','--no-index',str(patch)],cwd=replay,check=True);assert sha(copy)==sha(target)
    report={'path':path,'owner':'A','beforeSha256':sha(p),'afterSha256':sha(target),'patch':str(patch),'patchSha256':sha(patch),'stagePath':str(target),'compiled':True,'compileCommand':cmd,'compileLogSha256':sha(OUT/'compile.log'),'patchReadbackExact':True,'canonicalSourceUnchanged':True,'actualInstalled':False,'scope':'Only routePoint admission in independent A test-helper stage. False if nullrenderer/loading/failure/retired/lostfocus/pending uploads/unverified output/fullscreen presentation; then same existing viewport/dock/minimap/ray logic. No rule calls, waits/retries, snapshot injection or loading bypass. Corrects evidence quality identified by actualB r8 loading screenshot248. Current239 diagnostic and B current APK unchanged, fresh test-build/install required.','wholeGoalComplete':False}
    (DOC/'MAP_RAY_GUARD250.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='compileCommand'},ensure_ascii=False))
if __name__=='__main__':main()
