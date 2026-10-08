#!/usr/bin/env python3
"""Original serialized Root18 provenance, deterministic per-source resource."""
import argparse,json,hashlib,struct
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('input',type=Path);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();raw=a.input.read_bytes();digest=hashlib.sha256(raw).hexdigest();assert digest=='692391c85748e5da8d9ac1ae390ecf8e649ca2ba320ca4042048f929af57d3a0';j=json.loads(raw);assert len(j['rows'])==16
assert str(a.output)in ['core/src/main/resources/pc-duel/source-header-flags.tsv','out/session-b/source-header-flags-import1.tsv','out/session-b/source-header-flags-import2.tsv'];assert not a.output.exists()
lines=['# original header flags '+digest,'# executable '+j['exeSha']]
for r in j['rows']:
 f=(a.installation/r['path']).read_bytes();assert hashlib.sha256(f).hexdigest()==r['sha'];h=r['header'];assert h['offset']==16183 and h['bytes']==11 and bytes.fromhex(h['raw_hex'])==f[16183:16194]and hashlib.sha256(f[16183:16194]).hexdigest()==h['sha256'];reads=[x for x in h['reads']if x['destination']=='actor+0x18'];assert len(reads)==1 and reads[0]['offset']==16190 and reads[0]['bytes']==4
 flag=struct.unpack_from('<I',f,16190)[0];assert flag==r['flag18']and flag in [0,1];lines.append('\t'.join(map(str,[r['sourceId'],r['path'],r['sha'],j['sharedSha'],h['sha256'],flag])))
a.output.write_bytes(('\n'.join(lines)+'\n').encode('utf-8'));print(hashlib.sha256(a.output.read_bytes()).hexdigest())
