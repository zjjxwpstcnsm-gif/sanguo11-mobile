#!/usr/bin/env python3
"""Import pinned internal references with exact source/record identity guards."""
import argparse,gzip,json,struct
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,json_bytes,sha

def build(proof,expected,output):
    raw=proof.read_bytes()
    if sha(raw)!=expected:raise ValueError('Unpinned internal reference proof')
    report=json.loads(gzip.decompress(raw));sources=report['sources']
    if report['sourceExecutableSha256']!=EXE_SHA or len(sources)!=16:raise ValueError('Original source identity differs')
    data=bytearray()
    def integer(n):data.extend(struct.pack('>i',n))
    def text(s):b=s.encode('utf8');integer(len(b));data.extend(b)
    integer(0x504e5031);text(EXE_SHA);text(expected);integer(16)
    for source in sources:
        for k in ['scenarioId','sourcePath','sourceVariant','sourceSha256']:text(source[k])
        if len(source['rows'])!=1100 or not source['wholeWorldAndRngReadOnly']:raise ValueError('Incomplete original reference domain')
        integer(1100);seen=set()
        for i,row in enumerate(source['rows']):
            if row['nativeId']!=i or not -1<=row['internalFatherNativeId']<=1099:raise ValueError('Native reference outside original domain')
            officer=row['officerId']
            if officer is not None and officer>=0:
                if officer in seen or row['recordSha256']is None:raise ValueError('Runtime identity duplicate/unproved')
                seen.add(officer)
            integer(i);integer(-1 if officer is None else officer);text(row['recordSha256']or'')
            integer(row['internalFatherNativeId']);integer(int(row['originalReferenceValid']))
    output.mkdir(parents=True,exist_ok=False)
    (output/'references.bin.gz').write_bytes(gzip.compress(data,mtime=0));(output/'index.txt').write_text(sha(data)+'\n')
    lines=['# original-sha256='+expected]
    for i,source in enumerate(sources):
        rows={r['nativeId']:r for r in source['rows']}
        for case in source['cases']:
            a=rows[case['leftNativeId']]['officerId'];b=rows[case['rightNativeId']]['officerId']
            if a is not None and b is not None and a>=0 and b>=0:lines.append(f'{i}\t{a}\t{b}\t{int(case["sameFather"])}')
    fixture=('\n'.join(lines)+'\n').encode('utf8');(output/'original-fixtures.tsv').write_bytes(fixture)
    summary=dict(originalProofSha256=expected,binarySha256=sha(data),bytes=len(data),sources=16,originalObjects=17600,fixtureSha256=sha(fixture),mappedFixtureCases=len(lines)-1,oldSaveBackfill=False,completeGoal=False)
    (output/'build-report.json').write_bytes(json_bytes(summary));print(json.dumps(summary))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('proof',type=Path);p.add_argument('--expected-sha',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();build(a.proof,a.expected_sha,a.output)
