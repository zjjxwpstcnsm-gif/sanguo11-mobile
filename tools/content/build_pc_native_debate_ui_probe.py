#!/usr/bin/env python3
"""Compile an independent real-widget native debate acceptance APK."""
import argparse,hashlib,json,os,subprocess,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def build(source_root,output):
    source_root=source_root.resolve();output=output.resolve()
    if not source_root.is_relative_to(ROOT/'out/session1')or not output.is_relative_to(ROOT/'out/session1'):raise ValueError('Use independent session1 paths')
    output.mkdir(parents=True,exist_ok=False);classes=output/'classes';classes.mkdir();sdk=ROOT/'out/toolchain/android-sdk';bt=sdk/'build-tools/35.0.0';android=sdk/'platforms/android-35/android.jar';java=Path(os.environ['JAVA_HOME'])/'bin/java'
    app=source_root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes';classpath=output/'app-classpath.jar'
    with zipfile.ZipFile(classpath,'w',zipfile.ZIP_DEFLATED)as z:
        for p in sorted(app.rglob('*.class')):z.write(p,p.relative_to(app).as_posix())
    modules=[source_root/m/'build/libs'/(m+'.jar')for m in ['core','game-api','game-runtime']]
    src=ROOT/'tools/content/android/native-debate/PcContestInstrumentation.java'
    def run(name,cmd):
        with(output/(name+'.log')).open('wb')as log:subprocess.run(list(map(str,cmd)),check=True,stdout=log,stderr=subprocess.STDOUT)
    run('javac',[java.with_name('javac'),'-encoding','UTF-8','--release','17','-cp',os.pathsep.join(map(str,[android,classpath,*modules])),'-d',classes,src])
    dex=output/'dex.zip';cmd=[java,'-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--min-api','29','--lib',android,'--output',dex]
    for p in [classpath,*modules]:cmd+=['--classpath',p]
    run('d8',cmd+sorted(classes.rglob('*.class')))
    manifest=output/'AndroidManifest.xml';manifest.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="game.sanguo.mobile.pcopeningprobe"><uses-sdk android:minSdkVersion="29" android:targetSdkVersion="35"/><application android:label="Original debate acceptance" android:debuggable="true"/><instrumentation android:name="game.sanguo.mobile.PcContestInstrumentation" android:targetPackage="game.sanguo.mobile.dev" android:functionalTest="true"/></manifest>\n')
    resources=output/'resources.apk';run('aapt2',[bt/'aapt2','link','--manifest',manifest,'-I',android,'-o',resources]);unsigned=output/'unsigned.apk'
    with zipfile.ZipFile(unsigned,'w')as z:
        for p in [resources,dex]:
            with zipfile.ZipFile(p)as origin:
                for name in origin.namelist():z.writestr(name,origin.read(name))
    aligned=output/'aligned.apk';run('zipalign',[bt/'zipalign','-f','4',unsigned,aligned]);apk=output/'pc-native-debate.apk';run('sign',[bt/'apksigner','sign','--ks',ROOT/'tools/android/dev-debug.keystore','--ks-pass','pass:android','--key-pass','pass:android','--out',apk,aligned]);run('verify',[bt/'apksigner','verify','--print-certs',apk])
    report=dict(apk=str(apk),sha256=hashlib.sha256(apk.read_bytes()).hexdigest(),sourceSha256=hashlib.sha256(src.read_bytes()).hexdigest(),sourceRoot=str(source_root),onlyAcceptanceClasses=True);(output/'manifest.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source-root',required=True,type=Path);p.add_argument('--output',required=True,type=Path);a=p.parse_args();build(a.source_root,a.output)
