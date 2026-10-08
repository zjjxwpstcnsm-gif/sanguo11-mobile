#!/usr/bin/env python3
"""Real source index encoder plus owner-lifetime tests and actual Android compile."""
from pathlib import Path
import subprocess,json,time
from read_session_state import read_session_state
from run_remaining_normal_media import sha
ROOT=Path(__file__).resolve().parents[4];DOC=Path(__file__).resolve().parent;OUT=ROOT/'out/session-a/gpu-index-probe325';JAVA=Path('/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home/bin')
def main():
 assert not OUT.exists();OUT.mkdir(parents=True);base=ROOT/'out/session-a/mesh-allocation-build316/source';delta=read_session_state(DOC/'GPU_INDEX_DELTA324.json');sources={r['path']:Path(r['stagedPath']) for r in delta['paths']}
 for row in delta['paths']:assert sha(sources[row['path']])==row['afterSha256']
 common=ROOT/'out/session-a/prepare-allocation313/classes';cp=[common,*[base/n/'build/classes/java/main' for n in ['core','game-api','game-runtime']],base/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes',base/'core/src/main/resources',base/'core/src/test/resources'];classes=OUT/'host';classes.mkdir()
 hostSources=[sources['app/src/main/java/game/sanguo/mobile/SceneIndexLeasePool.java'],base/'app/src/main/java/game/sanguo/mobile/MeshIndexBuffer.java',DOC/'SceneIndexLeaseProbe325.java',base/'app/src/main/java/game/sanguo/mobile/SceneMesh.java']
 subprocess.run([str(JAVA/'javac'),'--release','17','-cp',':'.join(map(str,cp)),'-d',str(classes),*map(str,hostSources)],check=True)
 with (OUT/'host.log').open('w') as f:subprocess.run([str(JAVA/'java'),'-Xmx384m','-cp',':'.join(map(str,[classes,*cp])),'game.sanguo.mobile.SceneIndexLeaseProbe325'],stdout=f,stderr=subprocess.STDOUT,check=True)
 androidCp=[Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk/platforms/android-35/android.jar'),ROOT/'out/session-a/native-opening-stage224/compile/filament.jar',*cp];android=OUT/'android';android.mkdir()
 with (OUT/'android-compile.log').open('w') as f:subprocess.run([str(JAVA/'javac'),'--release','17','-cp',':'.join(map(str,androidCp)),'-d',str(android),*map(str,sources.values())],stdout=f,stderr=subprocess.STDOUT,check=True)
 report={'hostChecksPassed':True,'actualAndroidSourceCompiled':True,'hostLog':(OUT/'host.log').read_text(),'hostLogSha256':sha(OUT/'host.log'),'androidCompileLogSha256':sha(OUT/'android-compile.log'),'deltaSha256':sha(DOC/'GPU_INDEX_DELTA324.json'),'installed':False,'actualGpuIndexMemoryOrTimingMeasured':False,'scope':'Exact actual source14 immutable index arrays, unchanged lossless encoder and all original indices validated, encoded streams byte-equal per mesh; bounded16 registry spill/lifetime/format/range/factory/thread/engine separation/last owner behavior. Actual Java compilation against frozen316 Android/Filament. Mock factory checks do not execute GPU upload or device lifetime/native allocator. Full newAPK installation/normal retry/zoom/cold required.','wholeGoalComplete':False}
 (DOC/'GPU_INDEX_PROBE325.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report),flush=True)
if __name__=='__main__':main()
