#!/usr/bin/env python3
"""Test-only current native-search acceptance: production APK and source must equal r27 exactly."""
from pathlib import Path
import hashlib,json,shutil,subprocess,argparse
p=argparse.ArgumentParser();p.add_argument("--attempt",type=int,default=1);a=p.parse_args();assert a.attempt in [1,2]
R=Path(__file__).resolve().parents[2];S=R/'out/session-b/native-opening-combined61';F=R/'out/session-b/native-opening-combined61-frozen-r27';O=R/f'out/session-b/search-debate-current-built-v{a.attempt}'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
d=json.loads((R/'out/session-b/native-opening-combined61-source-inputs.json').read_text());g=json.loads((F/'source-guard.json').read_text());assert g==json.loads((R/'out/session-b/native-opening-combined61-source-guard.json').read_text())
for r in d['files']:assert sha(S/r['path'])==r['sha256'],r['path']
for p,h in g.items():assert sha(S/p)==h,p
apk=S/'app/build/outputs/apk/debug/app-debug.apk';test=S/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk';assert sha(apk)==sha(F/'app-debug.apk')
for module in ['core','game-api','game-runtime']:assert sha(S/module/'build/libs'/f'{module}.jar')==sha(F/f'{module}.jar')
sdk=R/'out/toolchain/android-sdk/build-tools/35.0.0';m=subprocess.check_output([str(sdk/'aapt'),'dump','xmltree',str(test),'AndroidManifest.xml']).decode();assert 'game.sanguo.mobile.SessionBDebateInstrumentation'in m;assert not O.exists();O.mkdir()
for p in [apk,test]:shutil.copy2(p,O/p.name);assert sha(O/p.name)==sha(p)
for p in [R/'out/session-b/native-opening-combined61-source-inputs.json',F/'source-guard.json']:shutil.copy2(p,O/p.name)
p=R/f'out/session-b/search-debate-current-apk-guard-v{a.attempt}.json';assert not p.exists();p.write_text(json.dumps(dict(wholeGoalComplete=False,productionExactlyFrozenR27=True,noProductionChanges=True,sourceInputs=len(d['files']),apkSha256=sha(apk),testApkSha256=sha(test),fixedResources=168,originalFourAndAdditionalTwoJniExactlyFrozenR27=True),indent=2)+'\n');print('PASS all current source/r27 APK exact/no production changes',sha(apk),sha(test))
