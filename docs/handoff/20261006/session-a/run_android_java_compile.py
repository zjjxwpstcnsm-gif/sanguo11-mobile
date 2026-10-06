#!/usr/bin/env python3
"""Compile actual Android production Java/resources, without Gradle or APK claims."""
import pathlib,subprocess,zipfile,json,hashlib,xml.etree.ElementTree as ET
ROOT=pathlib.Path(__file__).resolve().parents[4];OUT=ROOT/'out/session-a/android-java';OUT.mkdir(parents=True,exist_ok=True)
SDK=pathlib.Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk');android=SDK/'platforms/android-35/android.jar';aapt=SDK/'build-tools/35.0.0/aapt2'
aar=pathlib.Path('/Users/paopao/.gradle/caches/modules-2/files-2.1/com.google.android.filament/filament-android/1.56.0/18b22658d3308afef956248e4b189a0762d7c5e4/filament-android-1.56.0.aar')
with zipfile.ZipFile(aar) as z:(OUT/'filament.jar').write_bytes(z.read('classes.jar'))
generated=OUT/'generated';generated.mkdir(exist_ok=True)
manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml');manifest.getroot().set('package','game.sanguo.mobile');manifest.write(OUT/'AndroidManifest.xml',encoding='utf-8')
commands=[ [str(aapt),'compile','--dir',str(ROOT/'app/src/main/res'),'-o',str(OUT/'resources.zip')], [str(aapt),'link','-I',str(android),'--manifest',str(OUT/'AndroidManifest.xml'),'--java',str(generated),'--custom-package','game.sanguo.mobile','--auto-add-overlay','-o',str(OUT/'resources-only.ap_'),str(OUT/'resources.zip')] ]
for command in commands:subprocess.run(command,check=True)
values=dict(line.split('=',1) for line in (ROOT/'version.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
revision=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
buildconfig=generated/'game/sanguo/mobile/BuildConfig.java';buildconfig.parent.mkdir(parents=True,exist_ok=True)
buildconfig.write_text('package game.sanguo.mobile; public final class BuildConfig { public static final boolean DEBUG=false, UNITY_ENABLED=false; public static final String APPLICATION_ID="game.sanguo.mobile.dev", SOURCE_REVISION="'+revision+'", VERSION_NAME="'+values['versionName']+'"; public static final int VERSION_CODE='+values['versionCode']+';}')
sources=[]
for module in ['core','game-api','game-runtime','app']:sources+=sorted((ROOT/module/'src/main/java').rglob('*.java'))
sources+=list(generated.rglob('*.java'));(OUT/'sources.txt').write_text('\n'.join(map(str,sources))+'\n')
classes=OUT/'classes';classes.mkdir(exist_ok=True)
command=['java','-Xmx512m','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-cp',str(android)+':'+str(OUT/'filament.jar'),'-d',str(classes),'@'+str(OUT/'sources.txt')]
with (OUT/'javac.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
report={'passed':r.returncode==0,'sdk':35,'javaSources':len(sources),'sourceSha256':{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources if generated not in p.parents},'scope':'actual core/API/runtime/app Java and AAPT2 resources only; manual generated BuildConfig matches version.properties; no dex/R8/signing/install/Android runtime claim','apkBuilt':False,'installed':False,'commands':commands+[command],'log':str(OUT/'javac.log'),'output':(OUT/'javac.log').read_text(),'sourceRevision':revision}
(ROOT/'docs/handoff/20261006/session-a/ANDROID_JAVA_COMPILE.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k not in ['sourceSha256','commands','output']},ensure_ascii=False));raise SystemExit(r.returncode)
