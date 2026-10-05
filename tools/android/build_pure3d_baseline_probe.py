#!/usr/bin/env python3
"""Minimal test-only APK for the exact inherited AP APK; never packages production classes.
Avoids historic R8 shared lambda names colliding with relocated 2D test fixtures.
"""
from pathlib import Path
import subprocess,os,zipfile,json,hashlib
ROOT=Path(__file__).resolve().parents[2];AP=Path('/Users/paopao/workspace/sanguo11-mobile')
out=ROOT/'out/pure3d/baseline-probe';out.mkdir(parents=True,exist_ok=True)
sdk=ROOT/'out/toolchain/android-sdk';android=sdk/'platforms/android-35/android.jar';bt=sdk/'build-tools/35.0.0'
filament=next((ROOT/'out/pure3d/gradle-home/caches').glob('8.13/transforms/*/transformed/filament-android-1.56.0/jars/classes.jar'))
cp=[android,filament,AP/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes']+[AP/f'{m}/build/classes/java/main' for m in ('core','game-api','game-runtime')]
sources=[ROOT/f'app/src/androidTest/java/game/sanguo/mobile/{name}.java' for name in ('Pure3dInstrumentation','SceneInstrumentation','SessionProbe')]
classes=out/'classes';classes.mkdir(exist_ok=True)
def run(*args):subprocess.run(list(map(str,args)),check=True)
run('javac','-encoding','UTF-8','-source','8','-target','8','-cp',os.pathsep.join(map(str,cp)),'-d',classes,*sources)
dex=out/'dex';dex.mkdir(exist_ok=True)
run(bt/'d8','--min-api','26','--lib',android,'--output',dex,*classes.rglob('*.class'))
manifest=out/'AndroidManifest.xml';manifest.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="game.sanguo.mobile.dev.test"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="35"/><application/><instrumentation android:name="game.sanguo.mobile.Pure3dInstrumentation" android:targetPackage="game.sanguo.mobile.dev" android:functionalTest="true"/></manifest>')
unsigned=out/'unsigned.apk';run(bt/'aapt2','link','-I',android,'--manifest',manifest,'-o',unsigned)
with zipfile.ZipFile(unsigned,'a') as z:z.write(dex/'classes.dex','classes.dex')
apk=out/'baseline-probe.apk';run(bt/'apksigner','sign','--ks',ROOT/'tools/android/dev-debug.keystore','--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android','--out',apk,unsigned)
row={'source_files':{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},'classes':[str(p.relative_to(classes)) for p in classes.rglob('*.class')],'production_classes_packaged':False,'APK_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'AP_APK_sha256':'005c24e904c028a2bf8c328e1fb08eb907955b5010d33682e673eeb911779389'}
assert not any(name.endswith(('MapView.class','MainActivity.class','World.class')) for name in row['classes'])
(out/'manifest.json').write_text(json.dumps(row,indent=2));print(apk)
