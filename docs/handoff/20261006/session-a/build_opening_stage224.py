#!/usr/bin/env python3
"""Compile staged A Java/resources against SHA-verified frozen B JARs."""
from pathlib import Path
import subprocess,json,hashlib,zipfile,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a';STAGE=ROOT/'out/session-a/native-opening-stage224';OUT=STAGE/'compile';OUT.mkdir(exist_ok=True)
SDK=Path('/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk');JAVA=Path('/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home/bin/java')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
report=json.loads((DOC/'OPENING_STAGE224.json').read_text())
for x in report['artifacts']:
 if x['path'].endswith('.jar'):assert sha(STAGE/'dependencies'/Path(x['path']).name)==x['sha256']
aar=ROOT/'out/session-a/gradle-home/caches/modules-2/files-2.1/com.google.android.filament/filament-android/1.56.0/18b22658d3308afef956248e4b189a0762d7c5e4/filament-android-1.56.0.aar'
with zipfile.ZipFile(aar) as z:(OUT/'filament.jar').write_bytes(z.read('classes.jar'))
generated=OUT/'generated';generated.mkdir(exist_ok=True)
m=ET.parse(STAGE/'app/src/main/AndroidManifest.xml');m.getroot().set('package','game.sanguo.mobile');m.getroot().find('application').set('{http://schemas.android.com/apk/res/android}largeHeap','true');m.write(OUT/'AndroidManifest.xml',encoding='utf-8')
aapt=SDK/'build-tools/35.0.0/aapt2';android=SDK/'platforms/android-35/android.jar'
for command in [[str(aapt),'compile','--dir',str(STAGE/'app/src/main/res'),'-o',str(OUT/'resources.zip')],[str(aapt),'link','-I',str(android),'--manifest',str(OUT/'AndroidManifest.xml'),'--java',str(generated),'--custom-package','game.sanguo.mobile','--auto-add-overlay','-o',str(OUT/'resources-only.ap_'),str(OUT/'resources.zip')]]:subprocess.run(command,check=True)
v=dict(line.split('=',1) for line in (ROOT/'version.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
bc=generated/'game/sanguo/mobile/BuildConfig.java';bc.parent.mkdir(parents=True,exist_ok=True);bc.write_text('package game.sanguo.mobile; public final class BuildConfig {public static final boolean DEBUG=false, UNITY_ENABLED=false;public static final String APPLICATION_ID="game.sanguo.mobile.dev",SOURCE_REVISION="staged224",VERSION_NAME="'+v['versionName']+'";public static final int VERSION_CODE='+v['versionCode']+';}')
sources=sorted((STAGE/'app/src/main/java').rglob('*.java'))+sorted(generated.rglob('*.java'));(OUT/'sources.txt').write_text('\n'.join(map(str,sources))+'\n');classes=OUT/'classes';classes.mkdir(exist_ok=True)
cp=':'.join(map(str,[android,OUT/'filament.jar']+sorted((STAGE/'dependencies').glob('*.jar'))))
cmd=[str(JAVA),'-Xmx512m','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-cp',cp,'-d',str(classes),'@'+str(OUT/'sources.txt')]
with (OUT/'javac.log').open('w') as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
print((OUT/'javac.log').read_text());print('compile exit',r.returncode);raise SystemExit(r.returncode)
