#!/usr/bin/env python3
"""Import pinned original runtime fields/force relationships, without ID arithmetic."""
import argparse,gzip,json,struct
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,json_bytes,sha

def build(proof,expected,output):
    raw=proof.read_bytes()
    if sha(raw)!=expected:raise ValueError('Unpinned original runtime-state proof')
    report=json.loads(gzip.decompress(raw));sources=report['sources']
    if report['sourceExecutableSha256']!=EXE_SHA or len(sources)!=16:raise ValueError('Original executable/source count differs')
    data=bytearray()
    def integer(n):data.extend(struct.pack('>i',n))
    def text(v):b=v.encode('utf8');integer(len(b));data.extend(b)
    integer(0x504e4331);text(EXE_SHA);text(expected);integer(16)
    for s in sources:
        for key in ['scenarioId','sourcePath','sourceVariant','sourceSha256']:text(s[key])
        rows=s['rows'];forces=s['forces']
        if len(rows)!=850 or len(forces)!=47:raise ValueError('Incomplete original state source')
        integer(850)
        for i,row in enumerate(rows):
            if row['nativeId']!=i:raise ValueError('Original native state order differs')
            integer(-1 if row['officerId']is None else row['officerId']);integer(i);text(row['recordSha256'])
            integer(row['rawLoyalty']);integer(row['banRulerNativeId']);integer(row['banMonths']);integer(row['captiveMonths'])
        integer(47)
        for i,f in enumerate(forces):
            if f['nativeId']!=i or len(f['relations'])!=47:raise ValueError('Original force relationships incomplete')
            integer(i);integer(int(f['allowed']))
            for j,rel in enumerate(f['relations']):
                if rel['nativeId']!=j or rel['friendship']<0 or rel['friendship']>100:raise ValueError('Original relationship input invalid')
                integer(rel['friendship']);integer(int(rel['allied']));integer(rel['relation'])
    output.mkdir(parents=True,exist_ok=False);(output/'state.bin.gz').write_bytes(gzip.compress(data,mtime=0));(output/'index.txt').write_text(sha(data)+'\n')
    summary=dict(originalProofSha256=expected,binarySha256=sha(data),bytes=len(data),sourceCount=16,originalRows=13600,forcePairs=16*47*47,oldSaveBackfill=False,completeGoal=False)
    (output/'build-report.json').write_bytes(json_bytes(summary));print(json.dumps(summary))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('proof',type=Path);p.add_argument('--expected-sha',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();build(a.proof,a.expected_sha,a.output)
