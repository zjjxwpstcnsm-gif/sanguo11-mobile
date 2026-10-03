#!/usr/bin/env python3
"""Extract city+7c, used by original5ca620, from verified source serializer reads."""
import argparse,gzip,hashlib,json
from collections import Counter
from pathlib import Path
from inspect_pc_effect_bindings import EXE_SHA

def sha(raw):return hashlib.sha256(raw).hexdigest()
def inspect(installation,domains):
 data=domains.read_bytes();audit=json.loads(gzip.decompress(data));exe=(installation/'san11pk.exe').read_bytes()
 if sha(exe)!=EXE_SHA or audit['source_executable_sha256']!=EXE_SHA:raise ValueError('Executable mismatch')
 sources=[];counts=Counter()
 for source in audit['sources']:
  path=(installation/source['path']).resolve()
  if installation.resolve() not in path.parents:raise ValueError('Source path escapes installation')
  raw=path.read_bytes()
  if sha(raw)!=source['sha256']:raise ValueError('Scenario source mismatch')
  records=[]
  for record in source['records']:
   if record['kind']!='city':continue
   payload=raw[record['offset']:record['offset']+record['bytes']]
   if sha(payload)!=record['sha256']:raise ValueError('City record mismatch')
   reads=[r for r in record['reads'] if r['destination']=='actor+0x7c']
   if len(reads)!=1 or reads[0]['bytes']!=1:raise ValueError('Unexpected city7c serializer mapping')
   offset=reads[0]['offset']
   if not record['offset']<=offset<record['offset']+record['bytes']:raise ValueError('Field outside source record')
   value=raw[offset]
   if bytes.fromhex(record['actor_hex'])[0x7c]!=value:raise ValueError('Decoded field mismatch')
   counts[value]+=1;records.append(dict(native_city_id=record['native_index'],identity=record['identity'],record_sha256=record['sha256'],source_offset=offset,actor_offset='0x7c',raw_value=value))
  if len(records)!=42:raise ValueError('Expected42 city records')
  sources.append(dict(path=source['path'],sha256=source['sha256'],records=records))
 if len(sources)!=16:raise ValueError('Expected16 scenario sources')
 return dict(schema=1,source_executable_sha256=EXE_SHA,domain_audit_sha256=sha(data),sources=sources,value_counts=dict(sorted(counts.items())),quote_function='0x5ca620',ability_getter='0x4890a0 -> officer+0x173',limits=['Raw scenario load field only; scenario-start and monthly update effects unknown','No runtime import or save format change','Initial source values do not establish MOD activation','390 original quote arithmetic cases verified separately; full merchant eligibility, quotas and numeric input UI not inferred'])

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for name in ['installation','domains','output']:p.add_argument('--'+name,required=True,type=Path)
 a=p.parse_args()
 if a.output.resolve()==a.installation.resolve() or a.installation.resolve() in a.output.resolve().parents:raise ValueError('Output inside read-only installation')
 result=inspect(a.installation,a.domains);a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(result['value_counts'])
