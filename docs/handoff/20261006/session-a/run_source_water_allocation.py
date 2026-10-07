#!/usr/bin/env python3
"""Measure exact old/new water producer output and actual host allocation bytes."""
import argparse
import hashlib
import json
import pathlib
import subprocess

ROOT=pathlib.Path(__file__).resolve().parents[4]
OWN=pathlib.Path(__file__).resolve().parent


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--output',type=pathlib.Path,required=True)
    p.add_argument('--java',required=True)
    p.add_argument('--javac',required=True)
    p.add_argument('--common-classes',type=pathlib.Path,
                   default=ROOT/'out/session-a/grid-memory-parity/common')
    p.add_argument('--core-classes',type=pathlib.Path,
                   default=ROOT/'out/session-a/military-repair-host65/classes')
    args=p.parse_args()
    out=args.output.resolve()
    out.mkdir(parents=True,exist_ok=False)
    common=args.common_classes.resolve()
    core=args.core_classes.resolve()
    results={}
    for stage in ['baseline','current']:
        folder=out/stage;folder.mkdir()
        if stage=='baseline':
            source=folder/'SceneMesh.java'
            source.write_bytes((OWN/'source-water-baseline80.java.txt').read_bytes())
        else:source=ROOT/'app/src/main/java/game/sanguo/mobile/SceneMesh.java'
        subprocess.run([args.javac,'--release','17','-encoding','UTF-8','-cp',str(core)+':'+str(common),
            '-d',str(folder),str(source),str(OWN/'SourceWaterAllocationProbe.java')],check=True)
        log=folder/'producer.tsv'
        with log.open('w') as stream:
            subprocess.run([args.java,'-Xmx384m','-cp',str(folder)+':'+str(core)+':'+str(common)+':'+str(ROOT/'core/src/main/resources'),
                'game.sanguo.mobile.SourceWaterAllocationProbe'],stdout=stream,check=True)
        rows=[line.split('\t') for line in log.read_text().splitlines()]
        if len(rows)!=16:raise ValueError('All16 source producer results required')
        results[stage]={'sourceSha256':hashlib.sha256(source.read_bytes()).hexdigest(),
            'logSha256':hashlib.sha256(log.read_bytes()).hexdigest(),'rows':rows}
    baseline,current=results['baseline']['rows'],results['current']['rows']
    for a,b in zip(baseline,current):
        if a[:4]!=b[:4] or a[5]!=b[5]:raise ValueError('Exact original water vertices/index/attributes/metadata differ')
        if int(b[4])>=int(a[4]):raise ValueError('Measured host producer allocations not reduced')
    report={'passed':True,'sourceCount':16,'results':results,
        'baselineHostAllocatedBytes':sum(int(r[4]) for r in baseline),
        'currentHostAllocatedBytes':sum(int(r[4]) for r in current),
        'exactFinalFloatBitsIndicesAttributesMetadata':True,'fullSaveRngPure':True,
        'scope':'Actual host producer allocation counter under384MiB and all16 original-source geometry regions; not Android normal flow, actual Source6 peak improvement, native/GPU or ARM acceptance',
        'actualAndroidAccepted':False,'wholeGoalComplete':False}
    (out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k!='results'}))


if __name__=='__main__':main()
