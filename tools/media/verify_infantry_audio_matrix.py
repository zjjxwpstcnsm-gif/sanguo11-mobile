#!/usr/bin/env python3
"""Serial installed nine-tactic49/78 matrix with exact restore and real PCM.

Every output is fresh. Stop at the first UI, restoration or waveform failure;
no automatic retries or waveform repairs. Media test APK must already contain
the explicit sourceTactic acceptance and frozen app/test SHA are checked each run.
"""
import argparse,json,os,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]

def verify(args):
    if args.output.exists():raise ValueError('Fresh matrix output required')
    if not args.prefix.replace('-','').replace('_','').isalnum():raise ValueError('Simple run prefix required')
    args.output.mkdir(parents=True);rows=[]
    catalog=json.load(open(ROOT/'app/src/main/assets/audio/pc/tactic-event-bindings.json'))
    if len(catalog['entries']) not in (9,12):raise ValueError('Explicit source infantry/cavalry joins required')
    entries=[e for e in catalog['entries'] if e['projectTacticEnum'] in args.tactic] if args.tactic else catalog['entries']
    if args.tactic and set(args.tactic)!={e['projectTacticEnum'] for e in entries}:raise ValueError('Selected source tactic missing')
    for entry in entries:
        tactic=entry['projectTacticEnum']
        for sound in [49,78]:
            run=args.prefix+'-'+tactic.lower()+'-'+str(sound);out=args.output/run;print('START',run,flush=True)
            command=[sys.executable,str(ROOT/'tools/media/verify_media_install.py'),'--apk',str(args.apk.resolve()),'--test-apk',str(args.test_apk.resolve()),'--output',str(out.resolve()),'--runner','UiUxInstrumentation','--argument','suite=criticalAudio','--argument','sourceTactic='+tactic,'--argument','sourceTacticSound='+str(sound),'--argument','run='+run,'--pass-marker','UIUX PASS','--timeout','240','--reuse-installed','--wave',str(args.wave.resolve()),'--capture-only']
            if args.device_sha_readback:command.append('--device-sha-readback')
            process=subprocess.run(command,capture_output=True,text=True,cwd=ROOT);(out/'orchestrator.log').write_text(process.stdout+process.stderr);process.check_returncode()
            adb=ROOT/'out/toolchain/android-sdk/platform-tools/adb'
            subprocess.run([str(adb),'-s','emulator-5582','pull','/sdcard/Android/data/game.sanguo.mobile.dev/files/uiux/'+run,str(out/'ui-evidence')],check=True,capture_output=True)
            command=[str(args.media_python.resolve()),str(ROOT/'tools/audio/check_pc_spear_tactic_mix.py'),str(out/'captured-window.wav'),'--flow',str(out/'results.json'),'--facts',str(out/'ui-evidence/critical-facts.json'),'--output',str(out/'original-mix.json')]
            process=subprocess.run(command,capture_output=True,text=True,cwd=ROOT,env=dict(os.environ,OPENBLAS_NUM_THREADS='1'));(out/'pcm-check.log').write_text(process.stdout+process.stderr);process.check_returncode()
            flow=json.load(open(out/'results.json'));mix=json.load(open(out/'original-mix.json'));row=dict(run=run,tactic=tactic,nativeTacticId=entry['nativeTacticId'],nativeSoundId=sound,seconds=flow['seconds'],installedSha256=flow['installed_sha256'],installedTestSha256=flow['installed_test_sha256'],restoredByteEqual=flow['restoration']['all_original_files_byte_equal'],jointCorrelation=mix['jointCorrelation'],independentCorrelations=mix['independentCorrelations']);rows.append(row);print('PASS',json.dumps(row),flush=True)
            (args.output/'matrix.json').write_text(json.dumps(dict(rows=rows,expectedCases=2*len(entries),complete=len(rows)==2*len(entries),scope='Explicit prepared combat fixture, visible normal command, native hit branches only; not PC timing, native58, normal voice or full audio restoration'),indent=2)+'\n')
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for key in ['apk','test-apk','output','wave','media-python']:p.add_argument('--'+key,type=Path,required=True)
    p.add_argument('--prefix',required=True);p.add_argument('--device-sha-readback',action='store_true');p.add_argument('--tactic',action='append',default=[]);verify(p.parse_args())
