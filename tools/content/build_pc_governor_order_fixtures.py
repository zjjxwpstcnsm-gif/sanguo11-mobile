#!/usr/bin/env python3
"""Export pinned original comparisons, including explicit NPC/controlled cases."""
import argparse,gzip,json
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,sha

def build(proof,expected,output):
    raw=proof.read_bytes()
    if sha(raw)!=expected:raise ValueError('Original governor proof SHA differs')
    report=json.loads(gzip.decompress(raw))
    if report['sourceExecutableSha256']!=EXE_SHA or len(report['cases'])!=1416:raise ValueError('Original governor proof incomplete')
    if output.exists():raise ValueError('Preserve previous fixture output')
    keys=['nativeId','allowed','status','commandCapacity','leadership','war','merit'];lines=['# original-sha256='+expected]
    for case in report['cases']:lines.append('\t'.join(str(int(case[p][k]))for p in ['left','right']for k in keys)+'\t'+str(int(case['earlier'])))
    output.parent.mkdir(parents=True,exist_ok=True);raw=('\n'.join(lines)+'\n').encode('utf8');output.write_bytes(raw)
    print(json.dumps(dict(cases=1416,fixtureSha256=sha(raw),originalProofSha256=expected,completeElection=False)))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('proof',type=Path);p.add_argument('--expected-sha',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();build(a.proof,a.expected_sha,a.output)
