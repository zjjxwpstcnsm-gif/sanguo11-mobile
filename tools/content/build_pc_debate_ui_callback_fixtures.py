#!/usr/bin/env python3
"""Convert pinned original callback RNG/action observations to test fixtures."""
import argparse,gzip,json,struct
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,sha,json_bytes

PIN='294a60f0f1f66c15f967eb14a9437dfa0ac8887d4f15c85ea940ff4fea817cf7'
NAMES={'instant':'INSTANT','shout':'SHOUT','ordinary':'ORDINARY','rethink':'RETHINK',
       'calm':'CALM','guile':'GUILE','ignore':'IGNORE','counter':'COUNTER',
       'burstStrike':'BURST_STRIKE','burstEnd':'BURST_END','rash':'RASH','stages':'STAGES'}

def build(source,output):
    data=source.read_bytes()
    if sha(data)!=PIN:raise ValueError('Unpinned original UI callback observations')
    r=json.loads(gzip.decompress(data))
    if r['sourceExecutableSha256']!=EXE_SHA or len(r['cases'])!=8960:raise ValueError('Native callback coverage differs')
    lines=[]
    for c in r['cases']:
        args=c['arguments'];kind=c['callback'];card=args[1]if kind=='ordinary'else 1
        deflected=args[-1]if kind in ('ordinary','guile','ignore')else 0
        counter=args[1]if kind=='counter'else -1
        expected=[]
        for choice in c['randomQueueChoices']:
            raw=bytes.fromhex(c['queueHex'][choice['queueIndex']])
            if struct.unpack_from('<i',raw)[0]!=15 or struct.unpack_from('<i',raw,4)[0]!=choice['side']or struct.unpack_from('<i',raw,0x100)[0]!=choice['action']:raise ValueError('Observed original random action differs from actual queue')
            expected.extend([choice['side'],choice['action']])
        row=[NAMES[kind],*c['personality'],c['fury'],c['side'],c['seed'],card,deflected,counter,
             *c['previousStages'],*c['health'],len(c['rngWrites']),c['finalNativeRng'],len(c['randomQueueChoices']),*expected]
        lines.append('\t'.join(map(str,row)))
    output.mkdir(parents=True,exist_ok=False);p=output/'ui-callback-original.tsv';p.write_text('\n'.join(lines)+'\n')
    (output/'ui-callback-provenance.json').write_bytes(json_bytes(dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceReportSha256=PIN,cases=len(lines),fixtureSha256=sha(p.read_bytes()),functions=r['functions'],limits=r['limits'],productionIntegrated=False)))
    print(json.dumps(dict(cases=len(lines),sha256=sha(p.read_bytes()))))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();build(a.source,a.output)
