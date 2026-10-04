#!/usr/bin/env python3
"""Convert pinned full original recovery outputs; no expected-rule calculation."""
import argparse,gzip,json
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,sha,json_bytes
def build(source,expected,output):
    packed=source.read_bytes()
    if sha(packed)!=expected:raise ValueError('Original report SHA differs')
    r=json.loads(gzip.decompress(packed))
    if r['sourceExecutableSha256']!=EXE_SHA or len(r['sources'])!=16:raise ValueError('Original source coverage differs')
    rows=[];trajectory=0;edges=0
    for s in r['sources']:
        cases=[]
        for case in s['cases']:
            if len(case['trajectory'])!=12:raise ValueError('Original twelve-month trajectory incomplete')
            cases+=case['trajectory'];trajectory+=12
        cases+=s['admissionFixtures'];edges+=len(s['admissionFixtures'])
        for c in cases:
            b=c['before'];a=c['after'];v=[b['nativeId'],c['month'],b['injury'],int(b['originalActive']),int(b['ancientExcluded']),int(b['originalStatusAllowed']),c['seedBefore'],a['injury'],c['seedAfter'],len(c['originalDrawCalls'])]
            rows.append('\t'.join(map(str,v)))
    if trajectory!=384 or edges!=432:raise ValueError('Original fixture coverage differs')
    output.mkdir(parents=True,exist_ok=False);p=output/'recovery-original.tsv';p.write_text('\n'.join(rows)+'\n')
    (output/'provenance.json').write_bytes(json_bytes(dict(sourceExecutableSha256=EXE_SHA,sourceReportSha256=expected,fixtureSha256=sha(p.read_bytes()),trajectoryFrames=trajectory,admissionCases=edges,functions=r['functions'],limits=r['limits'])))
    print(json.dumps(dict(rows=len(rows),sha256=sha(p.read_bytes()))))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path);p.add_argument('--expected-sha',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();build(a.source,a.expected_sha,a.output)
