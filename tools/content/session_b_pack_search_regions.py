#!/usr/bin/env python3
"""Keep original concretecity property3 separate from genericbuilding property3."""
import argparse,gzip,json
from pathlib import Path
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha,output_guard
def pack(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier import')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
 report=(ROOT/'docs/handoff/20261004/session1/layered-scenario-fields.json.gz').read_bytes();d=json.loads(gzip.decompress(report));assert d['sourceExecutableSha256']==EXE_SHA
 manifest=json.loads((ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_text());sources={s['path']:s for s in d['sources']}
 domains=json.loads(gzip.decompress((ROOT/'docs/pc-data/scenario-domains-native.json.gz').read_bytes()));mapping={r['native_index']:r['identity']['project_id']for r in domains['shared_source']['records']if r['kind']=='city'}
 assert len(mapping)==42 and len(sources)==16
 descriptor=next(x for x in d['descriptors']['city']if x['id']==3);assert descriptor['name']=='所屬州'
 rows=['# concretecity3 所屬州 getter4c0c30 offsets24..27; genericbuilding3 remains sitekind; EXE '+EXE_SHA,'# layered report SHA '+sha(report),'# sourceId\tsourceSha\tnativeCity\tstableCity\tregion; original initialized context; serialized origin not proven']
 for entry in manifest['scenarios']:
  s=sources[entry['sourcePath']];assert s['sha256']==entry['sourceSha256']==sha((installation/entry['sourcePath']).read_bytes()) and s['rngUnchanged'] and s['nativeWorldReadOnly'] and len(s['domains']['city'])==42
  for native,row in enumerate(s['domains']['city']):
   p=row['properties']['3'];assert p['getter']=='0x4c0c30' and set([24,25,26,27])<=set(p['actorReadOffsets']) and 0<=p['value']<12
   rows.append('\t'.join(map(str,[entry['scenarioId'],entry['sourceSha256'],native,mapping[native],p['value']])))
 output.parent.mkdir(parents=True,exist_ok=True);output.write_text('\n'.join(rows)+'\n');print('PASS',len(rows)-3,'source-city rows SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('output',type=Path);a=p.parse_args();pack(a.installation,a.output)
