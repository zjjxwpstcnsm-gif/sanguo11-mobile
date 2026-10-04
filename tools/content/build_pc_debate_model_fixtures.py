#!/usr/bin/env python3
"""Freeze original complete null-UI model trace into portable state assertions."""
import argparse,gzip,json,struct
from pathlib import Path
from audit_pc_restoration_sources import ROOT,EXE_SHA,json_bytes,sha

MODEL_OFFSETS=[4,8,12,0x168,0x16c,0x170,0x174,0x178,0x17c,0x180,0x184,0x188,0x18c,0x190,0x194,0x198,0x19c,0x1a0,0x1a4,0x1a8,0x1ac]
SPEAKER_OFFSETS=[4,8,12,16]+list(range(0x14,0x30,4))+[0x30]+list(range(0x34,0x7c,4))+[0x7c]+list(range(0x80,0x94,4))+[0x94]

def build(output):
    packed=(ROOT/'docs/handoff/20261004/session1/debate-flow-native.json.gz').read_bytes()
    if sha(packed)!='193ca13a6bb92a3c1d389934166249611536150a50733ba4fd183fe786a5fca7':raise ValueError('Original complete flow oracle changed')
    report=json.loads(gzip.decompress(packed))
    if report['sourceExecutableSha256']!=EXE_SHA:raise ValueError('Original executable differs')
    output.mkdir(parents=True,exist_ok=False);rows=[]
    for case,record in enumerate(report['cases']):
        for entry in record['trace']:
            raw=bytes.fromhex(entry['stateHex']);values=[case,entry['frame'],entry['nativeRng']]
            values.extend(struct.unpack_from('<i',raw,offset)[0]for offset in MODEL_OFFSETS)
            for side in range(2):values.extend(struct.unpack_from('<i',raw,0x10+side*0xa0+offset)[0]for offset in SPEAKER_OFFSETS)
            rows.append('\t'.join(map(str,values)))
    data=('\n'.join(rows)+'\n').encode();(output/'original-model.tsv').write_bytes(data)
    (output/'model-provenance.json').write_bytes(json_bytes(dict(sourceExecutableSha256=EXE_SHA,oraclePackedSha256=sha(packed),fixtureSha256=sha(data),cases=len(report['cases']),recordedFrames=len(rows),modelFieldOffsets=MODEL_OFFSETS,speakerFieldOffsets=SPEAKER_OFFSETS,headedFlowCertified=False,campaignSettlementCertified=False)))
    print(json.dumps(dict(cases=len(report['cases']),recordedFrames=len(rows),sha256=sha(data),completeGoal=False)))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args();build(a.output)
