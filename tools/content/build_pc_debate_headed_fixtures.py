#!/usr/bin/env python3
"""Freeze original bounded frame effects and callback order as portable fixtures."""
import argparse,gzip,json,struct
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,json_bytes,sha
from build_pc_debate_model_fixtures import MODEL_OFFSETS,SPEAKER_OFFSETS
PIN='673b0e1f74467f0d99adbe45499a26138dd8a7e55cc564dd30af98cf87624aab'

def build(source,output):
    data=source.read_bytes()
    if sha(data)!=PIN:raise ValueError('Unpinned bounded headed oracle')
    r=json.loads(gzip.decompress(data))
    if r['sourceExecutableSha256']!=EXE_SHA or len(r['cases'])!=16:raise ValueError('Original source/cases differ')
    output.mkdir(parents=True,exist_ok=False);rows=[]
    for case,c in enumerate(r['cases']):
        for entry in c['trace']:
            raw=bytes.fromhex(entry['stateHex']);v=[case,entry['frame'],entry['nativeRng'],entry['nativeDraws'],*entry['uiStages']]
            v.extend(struct.unpack_from('<i',raw,o)[0]for o in MODEL_OFFSETS)
            for side in range(2):v.extend(struct.unpack_from('<i',raw,0x10+side*0xa0+o)[0]for o in SPEAKER_OFFSETS)
            v.extend([len(entry['callbacks']),*[int(x,16)for x in entry['callbacks']]])
            v.extend(c['terminalPreferences'])
            rows.append('\t'.join(map(str,v)))
    p=output/'headed-original.tsv';p.write_text('\n'.join(rows)+'\n')
    (output/'headed-provenance.json').write_bytes(json_bytes(dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceReportSha256=PIN,fixtureSha256=sha(p.read_bytes()),cases=16,frames=len(rows),callbackPolicy=r['callbackPolicy'],limits=r['limits'],modelOffsets=MODEL_OFFSETS,speakerOffsets=SPEAKER_OFFSETS,completeHeadedFlow=False)))
    print(json.dumps(dict(cases=16,frames=len(rows),sha256=sha(p.read_bytes()))))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();build(a.source,a.output)
