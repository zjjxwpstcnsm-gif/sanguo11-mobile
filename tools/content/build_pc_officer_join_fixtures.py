#!/usr/bin/env python3
"""Convert pinned original loyalty results without calculating expected values."""
import argparse,gzip,json
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,sha,json_bytes
def build(source,expected,output):
    packed=source.read_bytes()
    if sha(packed)!=expected:raise ValueError('Original report SHA differs')
    r=json.loads(gzip.decompress(packed))
    if r['sourceExecutableSha256']!=EXE_SHA or len(r['cases'])!=2366 or not r['fullWorldAndRngChecked']:raise ValueError('Original coverage differs')
    rows=[]
    for c in r['cases']:
        a=c['inputs'];b=c['originalFacts'];v=[a['current'],b['gap'],a['honor'],a['ambition'],b['rulerCharm'],int(a['weighted'])]
        v.extend(int(b[k])for k in ['ruler','spouse','sworn','liked','disliked','sameFather']);v.extend([int(a['sameHan']),int(a['sameBirthplace']),c['rawLoyaltyAfter']])
        rows.append('\t'.join(map(str,v)))
    output.mkdir(parents=True,exist_ok=False);p=output/'loyalty-original.tsv';p.write_text('\n'.join(rows)+'\n')
    (output/'provenance.json').write_bytes(json_bytes(dict(sourceExecutableSha256=EXE_SHA,sourceReportSha256=expected,fixtureSha256=sha(p.read_bytes()),cases=len(rows),functions=r['functions'],limits=r['limits'])))
    print(json.dumps(dict(cases=len(rows),sha256=sha(p.read_bytes()))))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path);p.add_argument('--expected-sha',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();build(a.source,a.expected_sha,a.output)
