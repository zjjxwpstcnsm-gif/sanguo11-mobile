#!/usr/bin/env python3
"""Import only cross-source invariant, identity-verified growth definitions.

Does not choose an active scenario or replace scenario-specific ability bases.
"""
import argparse,csv,gzip,hashlib,io,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def sha(b):return hashlib.sha256(b).hexdigest()
def export(installation,output,audit):
 installation=installation.resolve()
 for p in (output,audit):
  if p.resolve()==installation or installation in p.resolve().parents:raise ValueError('Read-only installation')
 src=ROOT/'docs/pc-data/officer-ability-sources-native.json.gz';raw=src.read_bytes();j=json.loads(gzip.decompress(raw))
 if sha((installation/'san11pk.exe').read_bytes())!=j['source_executable_sha256']:raise ValueError('EXE changed')
 catalog=(ROOT/'core/src/main/resources/content/officers.tsv').read_bytes()
 if sha(catalog)!=j['catalog_sha256']:raise ValueError('Identity catalog changed')
 sources={s['path']:(installation/s['path']).read_bytes() for s in j['sources']}
 for s in j['sources']:
  if sha(sources[s['path']])!=s['sha256']:raise ValueError('Source changed')
 groups={}
 for r in j['records']:
  record=sources[r['source']][r['record_offset']:r['record_offset']+152]
  if sha(record)!=r['record_sha256'] or list(record[118:123])!=r['growth_codes']:raise ValueError('Growth source changed')
  if r['project_id'] is not None:
   if r['identity'] not in ('name_birth_sex_verified','relocated_identity_verified'):raise ValueError('Unverified person bridge')
   groups.setdefault(r['project_id'],[]).append(r)
 if len(groups)!=666:raise ValueError('Expected666 verified people')
 buf=io.StringIO();writer=csv.writer(buf,delimiter='\t',lineterminator='\n');writer.writerow(['project_id','native_ids','name','growth0','growth1','growth2','growth3','growth4'])
 for pid,records in sorted(groups.items()):
  first=records[0]
  if len(records)!=16 or len({r['source'] for r in records})!=16 or any((r['name'],r['growth_codes'])!=(first['name'],first['growth_codes']) for r in records):raise ValueError('Cross-source invariant not established')
  slots=sorted({r['native_index'] for r in records})
  if len({700<=n<=799 for n in slots})!=1:raise ValueError('Special-slot behavior differs by source')
  writer.writerow([pid,','.join(map(str,slots)),first['name'],*first['growth_codes']])
 output.write_text(buf.getvalue());report=dict(schema=1,source_audit_sha256=sha(raw),catalog_sha256=j['catalog_sha256'],exe_sha256=j['source_executable_sha256'],sources=j['sources'],rows=666,verified_records=10656,output_sha256=sha(output.read_bytes()),limits=['Growth and source identity only; scenario-specific base stats, birth, rank, relations and activation are not imported here','Four unresolved catalog identities and all unverified extra slots are excluded','Authored/custom officers must not inherit native identity just because an integer ID happens to match'])
 audit.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for k in ['installation','output','audit']:p.add_argument('--'+k,type=Path,required=True)
 a=p.parse_args();export(a.installation,a.output,a.audit)
