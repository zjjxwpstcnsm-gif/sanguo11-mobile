#!/usr/bin/env python3
"""Sequential same-APK tests on one exclusive emulator, with bytes-preserving restores."""
import argparse, json, subprocess, sys, time
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--serial',required=True)
    p.add_argument('--apk',type=Path,required=True)
    p.add_argument('--test-apk',type=Path,required=True)
    p.add_argument('--output',type=Path,required=True)
    p.add_argument('--suites',nargs='+',default=['terrain','audio','opening','march','combat','mapEdges','mapNative','pure3dLifecycle','longRun'])
    p.add_argument('--reuse-slot-sha',required=True)
    p.add_argument('--long-turns',type=int,default=24)
    p.add_argument('--record',nargs='*',default=['march'])
    a=p.parse_args()
    a.output.mkdir(parents=True,exist_ok=False)
    adb=[str(ROOT/'out/toolchain/android-sdk/platform-tools/adb'),'-s',a.serial]
    def device(*parts):return subprocess.run(adb+list(parts),check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=30).stdout
    reports=[]
    for suite in a.suites:
        if not suite.isalnum():raise ValueError('Simple suite names only')
        destination=a.output/suite
        remote='/sdcard/pure3d-'+suite+'.mp4'
        pid=None
        if suite in a.record:
            pid=device('shell',f'screenrecord --bit-rate 4000000 --time-limit 180 {remote} >/sdcard/pure3d-record.log 2>&1 & echo $!').decode().strip()
            if not pid.isdigit():raise ValueError('Cannot identify own screenrecord process')
        arguments=[sys.executable,str(ROOT/'tools/android/verify_pure3d_install.py'),'--serial',a.serial,'--apk',str(a.apk.resolve()),'--test-apk',str(a.test_apk.resolve()),'--output',str(destination),'--runner','UiUxInstrumentation','--argument','suite='+suite,'--argument','run=final'+suite,'--pass-marker','UIUX PASS']
        if suite=='opening':arguments+=['--argument','reuseSlotSha='+a.reuse_slot_sha]
        if suite=='longRun':arguments+=['--argument','rounds='+str(a.long_turns)]
        started=time.monotonic()
        try:
            with (a.output/(suite+'.log')).open('wb') as log:
                result=subprocess.run(arguments,stdout=log,stderr=subprocess.STDOUT)
        finally:
            if pid:
                # Stop only the recorder started by this run; never interrupt the app/emulator.
                subprocess.run(adb+['shell','kill','-2',pid],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
                deadline=time.monotonic()+20
                while time.monotonic()<deadline:
                    process=subprocess.run(adb+['shell','kill','-0',pid],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
                    if process.returncode:break
                    time.sleep(.25)
                subprocess.run(adb+['pull',remote,str(a.output/(suite+'.mp4'))],check=True,stdout=subprocess.DEVNULL)
        destination.mkdir(exist_ok=True)
        with (destination/'renderer-audio-logcat.txt').open('wb') as log:
            subprocess.run(adb+['logcat','-d','-t','6000','-s','Sanguo3D','SceneTiming','SceneLifecycle','UiSave','GameAudio','AndroidRuntime'],check=True,stdout=log)
        with (destination/'pull.log').open('wb') as log:
            subprocess.run(adb+['pull','/sdcard/Android/data/game.sanguo.mobile.dev/files/uiux/final'+suite,str(destination/'evidence')],stdout=log,stderr=subprocess.STDOUT)
        report=json.loads((destination/'results.json').read_text()) if (destination/'results.json').exists() else {}
        reports.append(dict(suite=suite,passed=result.returncode==0 and report.get('passed',False),wall_seconds=round(time.monotonic()-started,2),result=report))
        (a.output/'summary.json').write_text(json.dumps(reports,ensure_ascii=False,indent=2)+'\n')
        print(suite,'PASS' if reports[-1]['passed'] else 'FAIL',flush=True)
    return 0 if all(r['passed'] for r in reports) else 1

if __name__=='__main__':sys.exit(main())
