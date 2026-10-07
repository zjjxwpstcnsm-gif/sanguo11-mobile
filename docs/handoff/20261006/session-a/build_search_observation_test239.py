#!/usr/bin/env python3
"""Build an observation-only test APK with unchanged frozen165 game."""
from pathlib import Path
import json,hashlib,subprocess,os,zipfile,shutil,time
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    assert not subprocess.check_output(['git','status','--porcelain'],cwd=ROOT).strip(),'Commit owned source first'
    head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    baseline=json.loads((DOC/'LEGACY39_REGISTERED_TEST_BUILD176.json').read_text());game=next(x for x in baseline['apks'] if Path(x['path']).name=='app-debug.apk')
    assert sha(Path(game['path']))==game['sha256']
    changed=subprocess.check_output(['git','diff','--name-only',baseline['gameSourceRevision'],'--','app/src/main','app/build.gradle','core/src/main','game-api/src/main','game-runtime/src/main'],cwd=ROOT,text=True).strip();assert not changed,changed
    out=ROOT/f'out/session-a/apk-{head[:8]}-search-observation-test239';assert not out.exists();out.mkdir(parents=True)
    env=os.environ.copy();env['JAVA_HOME']='/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home';env['GRADLE_USER_HOME']=str(ROOT/'out/session-a/gradle-home');env['ANDROID_HOME']='/Users/paopao/workspace/sanguo11-mobile/out/toolchain/android-sdk'
    gradle='/Users/paopao/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13/bin/gradle'
    command=[gradle,'--offline','--no-daemon',':app:assembleDebugAndroidTest'];began=time.monotonic()
    with (out/'build.log').open('w') as f:r=subprocess.run(command,cwd=ROOT,env=env,stdout=f,stderr=subprocess.STDOUT)
    assert r.returncode==0,(out/'build.log').read_text()[-6000:]
    assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()==head
    test=out/'app-debug-androidTest.apk';shutil.copy2(ROOT/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk',test)
    sdk=Path(env['ANDROID_HOME']);aapt=sdk/'build-tools/35.0.0/aapt';signer=sdk/'build-tools/35.0.0/apksigner'
    manifest=subprocess.check_output([str(aapt),'dump','xmltree',str(test),'AndroidManifest.xml'],text=True)
    assert 'SessionAMapRepairInstrumentation' in manifest and 'SessionBLegacy39Instrumentation' in manifest
    signatures=[]
    for apk in [Path(game['path']),test]:
        output=subprocess.check_output([str(signer),'verify','--print-certs',str(apk)],text=True,env=env);(out/(apk.name+'.signature.txt')).write_text(output);signatures.append(output)
    digest=lambda s:next(x for x in s.splitlines() if x.startswith('Signer #1 certificate SHA-256 digest:'))
    assert digest(signatures[0])==digest(signatures[1])
    pins=json.loads((ROOT/'tools/content/map-release-manifest.json').read_text())['files'];assert len(pins)==168
    with zipfile.ZipFile(game['path']) as z:
        for x in pins:assert hashlib.sha256(z.read(x['apk_path'])).hexdigest()==x['sha256']
        for x in baseline['sixJniExact']:assert hashlib.sha256(z.read(x['entry'])).hexdigest()==x['sha256']
    with zipfile.ZipFile(test) as z:
        fixtures=[x for x in z.namelist() if x.endswith('.sg11') and hashlib.sha256(z.read(x)).hexdigest()==baseline['genuine39FixtureSha256']]
        assert len(fixtures)==1,fixtures

    report={'buildSuccessful':True,'sourceRevision':head,'gameSourceRevision':baseline['gameSourceRevision'],'testSourceRevision':head,'gameLargeHeap':True,'apks':[game,{'path':str(test),'bytes':test.stat().st_size,'sha256':sha(test)}],'sixJniExact':baseline['sixJniExact'],'fixedPackagedInputsExact':168,'legacy39RunnerManifestRegistered':True,'genuine39FixtureSha256':baseline['genuine39FixtureSha256'],'fixturePackagedByteExact':True,'productionRawSourcesEqualTo165':True,'buildCommand':command,'wallSeconds':time.monotonic()-began,'buildLogSha256':sha(out/'build.log'),'newTestObservationOnly':True,'defaultNormalPredicateUnchanged':True,'searchDiagnosticFlagDefaultFalse':True,'actualInstalled':False,'scope':'Fresh test-only observation APK, same exact frozen165 game. New optional bounded search-window/focus/layout observations on failure; no waits/retries/predicate or application production changes. Old176 Source0/1/2/3/11 scores do not transfer to new test cohort; fresh normal diagnostic/complete backup/install/cold/restoration required.','wholeGoalComplete':False}
    (DOC/'SEARCH_OBSERVATION_TEST_BUILD239.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:report[k] for k in ['apks','wallSeconds','productionRawSourcesEqualTo165','actualInstalled']}))
if __name__=='__main__':main()
