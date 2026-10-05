#!/usr/bin/env python3
"""Freeze original human getter/selection remainder output, not Java expectations."""
import argparse,gzip,json,struct
from pathlib import Path
from audit_pc_restoration_sources import ROOT,EXE_SHA,json_bytes,sha

def build(output):
    packed=(ROOT/'docs/handoff/20261004/session1/debate-human-input-native.json.gz').read_bytes()
    if sha(packed)!='d83feb4c9ae8886844ae36e9676718eabc28a1691db83581457e3c97cd510ea7':raise ValueError('Original input oracle changed')
    report=json.loads(gzip.decompress(packed))
    if report['sourceExecutableSha256']!=EXE_SHA:raise ValueError('Original executable changed')
    output.mkdir(parents=True,exist_ok=False);rows=[]
    for row in report['cases']:
        raw=bytes.fromhex(row['afterStateHex']);side=row['side'];values=[row['case'],side,row['slot'],int(row['accepted']),struct.unpack_from('<i',raw,12)[0],struct.unpack_from('<i',raw,0x170+4*side)[0],*struct.unpack_from('<7i',raw,0x24+side*0xa0),row['nativeRng']]
        rows.append('\t'.join(map(str,values)))
    data=('\n'.join(rows)+'\n').encode();(output/'original-human-input.tsv').write_bytes(data)
    (output/'human-provenance.json').write_bytes(json_bytes(dict(sourceExecutableSha256=EXE_SHA,oraclePackedSha256=sha(packed),fixtureSha256=sha(data),cases=len(rows),completeHumanGuiCertified=False)))
    print(json.dumps(dict(cases=len(rows),sha256=sha(data),completeGoal=False)))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args();build(a.output)
