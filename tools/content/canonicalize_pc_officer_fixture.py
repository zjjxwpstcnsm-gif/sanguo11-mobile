#!/usr/bin/env python3
"""Canonicalize a test-owned fixture with actual installed APK codec on ART.

Only /data/local/tmp is written. Never touch app files, slots or preferences.
"""
import argparse
import hashlib
import json
import os
import subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]


def run(args):
    args.output.mkdir(parents=True,exist_ok=False)
    owner=json.loads(Path('/tmp/sanguo11-emulator-5554.lock/owner.json').read_text())
    if args.serial!='emulator-5554' or owner['branch']!='codex/scenario-officer-restoration':
        raise ValueError('Own exclusive5554 required')
    adb=[str(ROOT/'out/toolchain/android-sdk/platform-tools/adb'),'-s',args.serial]
    def command(*parts):
        result=subprocess.run(adb+list(parts),stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=180)
        if result.returncode:
            (args.output/'command-failure.log').write_bytes(result.stdout+b'\n'+result.stderr)
            result.check_returncode()
        return result.stdout
    target=command('shell','pm','path','game.sanguo.mobile.dev').decode().strip().split('package:',1)[1]
    installed=hashlib.sha256(command('exec-out','cat',target)).hexdigest()
    if installed!=hashlib.sha256(args.apk.read_bytes()).hexdigest():raise ValueError('Wrong installed APK')
    tools=ROOT/'out/toolchain/android-sdk/build-tools/35.0.0';android=ROOT/'out/toolchain/android-sdk/platforms/android-35/android.jar'
    java=Path(os.environ['JAVA_HOME'])/'bin/java';javac=java.with_name('javac');classes=args.output/'classes';classes.mkdir()
    source=ROOT/'tools/content/ArtOfficerFixtureCanonicalizer.java'
    subprocess.run([str(javac),'--release','17','-encoding','UTF-8','-cp',str(ROOT/'core/build/libs/core.jar'),'-d',str(classes),str(source)],check=True)
    dex=args.output/'canonicalizer.zip'
    subprocess.run([str(java),'-cp',str(tools/'lib/d8.jar'),'com.android.tools.r8.D8','--min-api','29','--lib',str(android),'--classpath',str(ROOT/'core/build/libs/core.jar'),'--output',str(dex),str(classes/'ArtOfficerFixtureCanonicalizer.class')],check=True)
    prefix='/data/local/tmp/session1-officer-'+args.output.name
    command('push',str(dex),prefix+'.zip');command('push',str(args.input),prefix+'.input.sg11')
    log=command('shell','env','CLASSPATH='+target+':'+prefix+'.zip','app_process','/system/bin','ArtOfficerFixtureCanonicalizer',prefix+'.input.sg11',prefix+'.art.sg11')
    (args.output/'art.log').write_bytes(log)
    if b'PASS ART fixture canonicalized' not in log:raise ValueError('Actual codec did not return')
    raw=command('exec-out','cat',prefix+'.art.sg11');(args.output/'normal190-art.sg11').write_bytes(raw)
    original=args.input.read_bytes()
    report=dict(serial=args.serial,installedApkSha256=installed,sourceSha256=hashlib.sha256(source.read_bytes()).hexdigest(),
        originalFixtureSha256=hashlib.sha256(original).hexdigest(),artFixtureSha256=hashlib.sha256(raw).hexdigest(),
        byteDifferences=[i for i,(a,b) in enumerate(zip(original,raw)) if a!=b],originalBytes=len(original),artBytes=len(raw),
        stableArtRoundtrip=True,userDataTouched=False,productionClassesFromInstalledApk=True)
    (args.output/'provenance.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--serial',required=True)
    for name in ('apk','input','output'):parser.add_argument('--'+name,type=Path,required=True)
    run(parser.parse_args())
