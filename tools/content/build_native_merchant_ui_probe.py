#!/usr/bin/env python3
"""Build a separate signed UI acceptance APK without editing production/app tests."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import zipfile

ROOT=Path(__file__).resolve().parents[2]
PACKAGE='game.sanguo.mobile.marketprobe'


def build(output,runner='NativeMerchantUiInstrumentation'):
    package={'NativeMerchantUiInstrumentation':PACKAGE,
             'NativeCityRewardsUiInstrumentation':'game.sanguo.mobile.cityrewardsprobe',
             'NativeCityDisplacementUiInstrumentation':'game.sanguo.mobile.citydisplacementprobe'}[runner]
    output=output.resolve()
    if ROOT/'out' not in output.parents:raise ValueError('Fresh output must be inside project out/')
    output.mkdir(parents=True,exist_ok=False)
    sdk=ROOT/'out/toolchain/android-sdk';tools=sdk/'build-tools/35.0.0';android=sdk/'platforms/android-35/android.jar'
    java=Path(os.environ['JAVA_HOME'])/'bin/java';javac=java.with_name('javac')
    app=ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
    if not app.is_dir():raise ValueError('Compile actual app first')
    classpath=output/'actual-app-classpath.jar'
    with zipfile.ZipFile(classpath,'w',zipfile.ZIP_DEFLATED) as archive:
        for p in sorted(app.rglob('*.class')):archive.write(p,str(p.relative_to(app)))
    modules=[ROOT/module/'build/libs'/(module+'.jar') for module in ('core','game-api','game-runtime')]
    classes=output/'classes';classes.mkdir();source=ROOT/'tools/content/android'/(runner+'.java')
    def run(name,command):
        with (output/(name+'.log')).open('w') as log:subprocess.run(list(map(str,command)),check=True,stdout=log,stderr=log)
    run('javac',[javac,'-encoding','UTF-8','--release','17','-cp',os.pathsep.join(map(str,[android,classpath]+modules)),'-d',classes,source])
    dex=output/'dex.zip';command=[java,'-cp',tools/'lib/d8.jar','com.android.tools.r8.D8','--min-api','29','--lib',android,'--output',dex]
    for jar in [classpath]+modules:command+=['--classpath',jar]
    command+=sorted(classes.rglob('*.class'));run('d8',command)
    manifest=output/'AndroidManifest.xml';manifest.write_text(f'''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{package}">
<uses-sdk android:minSdkVersion="29" android:targetSdkVersion="35" />
<application android:label="Native merchant acceptance" android:debuggable="true" />
<instrumentation android:name="game.sanguo.mobile.{runner}" android:targetPackage="game.sanguo.mobile.dev" android:functionalTest="true" />
</manifest>\n''',encoding='utf-8')
    resources=output/'resources.apk';run('aapt2',[tools/'aapt2','link','--manifest',manifest,'-I',android,'-o',resources])
    unsigned=output/'unsigned.apk'
    with zipfile.ZipFile(unsigned,'w') as target:
        for archive in (resources,dex):
            with zipfile.ZipFile(archive) as source_zip:
                for name in source_zip.namelist():target.writestr(name,source_zip.read(name))
    aligned=output/'aligned.apk';run('zipalign',[tools/'zipalign','-f','4',unsigned,aligned])
    apk=output/'native-market-ui.apk'
    run('sign',[tools/'apksigner','sign','--ks',ROOT/'tools/android/dev-debug.keystore','--ks-pass','pass:android','--key-pass','pass:android','--out',apk,aligned])
    run('verify',[tools/'apksigner','verify','--print-certs',apk])
    result=dict(package=package,runner='game.sanguo.mobile.'+runner,apk=str(apk),bytes=apk.stat().st_size,sha256=hashlib.sha256(apk.read_bytes()).hexdigest(),source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),scope='Only separate acceptance class; production classes are classpath references and must come from installed target APK')
    (output/'manifest.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',type=Path,required=True);parser.add_argument('--runner',choices=['NativeMerchantUiInstrumentation','NativeCityRewardsUiInstrumentation','NativeCityDisplacementUiInstrumentation'],default='NativeMerchantUiInstrumentation');args=parser.parse_args();build(args.output,args.runner)
