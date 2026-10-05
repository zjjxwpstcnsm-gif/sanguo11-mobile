#!/usr/bin/env python3
"""Sequential actual menu/new-game/play/save/load/cold flows for16 pinned sources.

Every child performs complete backup/readback restoration before the next starts.
An existing source APK guard and explicit serial lock are required. Stops on failure.
"""
import argparse,hashlib,json,subprocess,sys,time
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]

def run(args):
    args.output.mkdir(parents=True,exist_ok=args.resume)
    if args.resume:
        prior=(args.output/'results.json').read_bytes();report=json.loads(prior)
        (args.output/('resume-prior-'+str(time.time_ns())+'.json')).write_bytes(prior)
        if report['serial']!=args.serial:raise ValueError('Resume serial differs')
        expected={args.apk.name:hashlib.sha256(args.apk.read_bytes()).hexdigest(),args.test_apk.name:hashlib.sha256(args.test_apk.read_bytes()).hexdigest()}
        for row in report['rows']:
            if row['passed']:
                actual=json.loads((Path(row['evidencePath'])/'results.json').read_text())['apks']
                if actual!=expected:raise ValueError('Resume APK inputs differ from successful flow evidence')
    else:report=dict(serial=args.serial,rows=[],passed=False,completeGoal=False)
    def save(): (args.output/'results.json').write_text(json.dumps(report,indent=2)+'\n')
    save();backup=args.output/'source-0-opening/external-before.tar' if args.resume else None
    for index in range(16):
        opened=args.output/('source-'+str(index)+'-opening')
        common=[sys.executable,str(ROOT/'tools/content/verify_pc_source_opening_ui.py'),
                '--serial',args.serial,'--apk',str(args.apk),'--test-apk',str(args.test_apk),
                '--source-guard',str(args.source_guard),'--info-only','--source-index',str(index),
                '--gaiji-identity','--unique-evidence']
        for mode in ['opening','cold']:
            if any(row['sourceIndex']==index and row['mode']==mode and row['passed']for row in report['rows']):continue
            target=opened if mode=='opening' else args.output/('source-'+str(index)+'-cold')
            if target.exists():
                number=1
                while target.with_name(target.name+'-retry-'+str(number)).exists():number+=1
                target=target.with_name(target.name+'-retry-'+str(number))
            campaign=args.old_saves/(str(index)+'.sg11') if mode=='opening' else opened/'actual-source-flow-final.sg11'
            command=common+['--output',str(target),'--campaign-save',str(campaign),
                            '--source-opening' if mode=='opening' else '--source-resume']
            if backup is not None:command+=['--external-backup',str(backup)]
            if args.reuse_installed:command+=['--reuse-installed']
            started=time.monotonic()
            with (args.output/(str(index)+'-'+mode+'.log')).open('w')as log:
                result=subprocess.run(command,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
            outcome=json.loads((target/'results.json').read_text()) if (target/'results.json').exists()else {}
            report['rows'].append(dict(sourceIndex=index,mode=mode,exitCode=result.returncode,
                passed=outcome.get('passed',False),seconds=round(time.monotonic()-started,2),
                evidencePath=str(target.resolve()),restoration=outcome.get('externalRestoration'),
                actualSourceFlowSave=outcome.get('actualSourceFlowSave')));save()
            print(json.dumps(dict(sourceIndex=index,mode=mode,passed=outcome.get('passed',False))),flush=True)
            if result.returncode or not outcome.get('passed'):
                raise RuntimeError('Actual source flow failed; retain child evidence and restoration report')
            if backup is None:backup=opened/'external-before.tar'
    report['passed']=True;save();print('PASS32 actual source new-game/cold flows; full delivery remains incomplete',flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--serial',required=True)
    p.add_argument('--resume',action='store_true');p.add_argument('--reuse-installed',action='store_true')
    for name in ['apk','test-apk','source-guard','old-saves','output']:p.add_argument('--'+name,type=Path,required=True)
    run(p.parse_args())
