#!/usr/bin/env python3
"""Build a separate officer acceptance APK referencing current production classes."""
import argparse
import hashlib
import json
import os
import subprocess
import zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]


def build(output):
    output=output.resolve()
    if ROOT/'out' not in output.parents:raise ValueError('Use independent out directory')
    output.mkdir(parents=True,exist_ok=False);classes=output/'classes';classes.mkdir()
    sdk=ROOT/'out/toolchain/android-sdk';tools=sdk/'build-tools/35.0.0';android=sdk/'platforms/android-35/android.jar'
    java=Path(os.environ['JAVA_HOME'])/'bin/java'
    app=ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes';classpath=output/'app-classpath.jar'
    with zipfile.ZipFile(classpath,'w',zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(app.rglob('*.class')):archive.write(path,str(path.relative_to(app)))
    modules=[ROOT/m/'build/libs'/(m+'.jar') for m in ['core','game-api','game-runtime']]
    source=ROOT/'tools/content/android/PcScenarioOpeningInstrumentation.java';contest=ROOT/'tools/content/android/PcContestInstrumentation.java'
    def run(name,command):
        with (output/(name+'.log')).open('w') as log:subprocess.run(list(map(str,command)),check=True,stdout=log,stderr=log)
    run('javac',[java.with_name('javac'),'-encoding','UTF-8','--release','17','-cp',os.pathsep.join(map(str,[android,classpath]+modules)),'-d',classes,source,contest])
    dex=output/'dex.zip';command=[java,'-cp',tools/'lib/d8.jar','com.android.tools.r8.D8','--min-api','29','--lib',android,'--output',dex]
    for jar in [classpath]+modules:command+=['--classpath',jar]
    run('d8',command+sorted(classes.rglob('*.class')))
    manifest=output/'AndroidManifest.xml';manifest.write_text('''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="game.sanguo.mobile.pcopeningprobe">
<uses-sdk android:minSdkVersion="29" android:targetSdkVersion="35" />
<application android:label="PC source opening acceptance" android:debuggable="true" />
<instrumentation android:name="game.sanguo.mobile.PcScenarioOpeningInstrumentation" android:targetPackage="game.sanguo.mobile.dev" android:functionalTest="true" />
<instrumentation android:name="game.sanguo.mobile.PcContestInstrumentation" android:targetPackage="game.sanguo.mobile.dev" android:functionalTest="true" />
</manifest>\n''')
    resources=output/'resources.apk';run('aapt2',[tools/'aapt2','link','--manifest',manifest,'-I',android,'-o',resources])
    unsigned=output/'unsigned.apk'
    with zipfile.ZipFile(unsigned,'w') as target:
        for file in [resources,dex]:
            with zipfile.ZipFile(file) as archive:
                for name in archive.namelist():target.writestr(name,archive.read(name))
    aligned=output/'aligned.apk';run('zipalign',[tools/'zipalign','-f','4',unsigned,aligned]);apk=output/'pc-opening.apk'
    run('sign',[tools/'apksigner','sign','--ks',ROOT/'tools/android/dev-debug.keystore','--ks-pass','pass:android','--key-pass','pass:android','--out',apk,aligned])
    run('verify',[tools/'apksigner','verify','--print-certs',apk])
    (output/'manifest.json').write_text(json.dumps(dict(package='game.sanguo.mobile.pcopeningprobe',runner='PcScenarioOpeningInstrumentation',sha256=hashlib.sha256(apk.read_bytes()).hexdigest(),sourceSha256=hashlib.sha256(source.read_bytes()).hexdigest(),onlyAcceptanceClasses=True),indent=2)+'\n')
    print(str(apk))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',type=Path,required=True);args=parser.parse_args();build(args.output)
