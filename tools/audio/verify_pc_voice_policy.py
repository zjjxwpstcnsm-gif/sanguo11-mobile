#!/usr/bin/env python3
"""Compare the production Java selector to all original x86 execution vectors."""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import subprocess


def verify(report, output, java_home):
    root=Path(__file__).resolve().parents[2]
    if output.exists():
        raise ValueError('Fresh evidence directory required')
    source=json.loads(gzip.decompress(report.read_bytes()))
    rows=[]
    for row in source['rows']+source['boundaries']:
        ability=row.get('abilityRaw',[50,50,50,50])
        values=[row['profile'],row['voiceTypeRaw'],row.get('actorValidRaw',1),row.get('feedbackRaw') if row.get('feedbackRaw') is not None else -1,*ability,row['voiceId']]
        rows.append('\t'.join([row['selector'],*map(str,values)]))
    if len(rows)!=8240:
        raise ValueError('Incomplete original vectors')
    output.mkdir(parents=True)
    vectors=output/'native-vectors.tsv'
    vectors.write_text('\n'.join(rows)+'\n')
    classes=output/'classes'
    classes.mkdir()
    files=[root/'app/src/main/java/game/sanguo/mobile/PcVoicePolicy.java',root/'app/src/test/java/game/sanguo/mobile/PcVoicePolicyTest.java']
    compile_result=subprocess.run([str(java_home/'bin/javac'),'-d',str(classes),*map(str,files)],capture_output=True,text=True)
    (output/'compile.log').write_text(compile_result.stdout+compile_result.stderr)
    compile_result.check_returncode()
    result=subprocess.run([str(java_home/'bin/java'),'-cp',str(classes),'game.sanguo.mobile.PcVoicePolicyTest',str(vectors)],capture_output=True,text=True)
    (output/'test.log').write_text(result.stdout+result.stderr)
    result.check_returncode()
    if 'PASS original voice selector vectors=8240' not in result.stdout:
        raise AssertionError('Missing exact native comparison acceptance')
    evidence=dict(result='PASS',vectors=8240,nativeReportSha256=hashlib.sha256(report.read_bytes()).hexdigest(),
                  paths={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in files},
                  testLogSha256=hashlib.sha256((output/'test.log').read_bytes()).hexdigest(),
                  limits=['Host Java execution, not installed voice playback or Android event binding.'])
    (output/'acceptance.json').write_text(json.dumps(evidence,indent=2)+'\n')
    print(result.stdout.strip())


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('report',type=Path)
    p.add_argument('--output',type=Path,required=True)
    p.add_argument('--java-home',type=Path,required=True)
    a=p.parse_args()
    verify(a.report,a.output,a.java_home)
