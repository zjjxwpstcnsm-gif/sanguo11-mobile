#!/usr/bin/env python3
"""Convert pinned original complete settlement outputs, without computing them."""
import argparse,gzip,json
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,sha,json_bytes
def build(source,expected,output):
    packed=source.read_bytes()
    if sha(packed)!=expected:raise ValueError('Original report SHA differs')
    r=json.loads(gzip.decompress(packed))
    if r['sourceExecutableSha256']!=EXE_SHA or len(r['sources'])!=16:raise ValueError('Original source coverage differs')
    output.mkdir(parents=True,exist_ok=False);rows=[]
    for index,s in enumerate(r['sources']):
        if len(s['cases'])!=24:raise ValueError('Original settlement case coverage differs')
        for c in s['cases']:
            v=[index,c['winner'],c['outcome'],c['boundary']]
            for values in [c['before'],c['after']]:
                for field in ['merit','intelligenceExperience','injury','owner']:v.extend(p[field]for p in values)
            v.extend(c['forcesBefore']);v.extend(c['forcesAfter']);rows.append('\t'.join(map(str,v)))
            rows[-1]+='\t'+'\t'.join(str(int(p[key]))for key in ['partyGuidance','rewardEligible']for p in c['before'])
    p=output/'settlement-original.tsv';p.write_text('\n'.join(rows)+'\n')
    (output/'settlement-provenance.json').write_bytes(json_bytes(dict(sourceExecutableSha256=EXE_SHA,sourceReportSha256=expected,fixtureSha256=sha(p.read_bytes()),cases=len(rows),sources=16,limits=r['limits'],functions=r['functions'])))
    print(json.dumps(dict(cases=len(rows),sha256=sha(p.read_bytes()))))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path);p.add_argument('--expected-sha',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();build(a.source,a.expected_sha,a.output)
