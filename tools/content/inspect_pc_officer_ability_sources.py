#!/usr/bin/env python3
"""Audit growth/rank/spouse source bytes with native serializer perturbations.

No gameplay import. Existing identity mappings are inherited only after their
catalog/source fingerprints are checked. Decoded zero-filled fields without
serialized input are explicitly excluded from source-data claims.
"""
import argparse,collections,gzip,hashlib,json,struct
from pathlib import Path
from inspect_pc_effect_bindings import EXE_SHA
from inspect_pc_scenario_officers import NativeOfficerDecoder

ROOT=Path(__file__).resolve().parents[2]
FIELDS={'growth':(0xd0,20),'experience':(0x12a,10),'injury':(0x15c,4),'rank':(0xa4,4),'spouse':(0x60,4)}
EXPECTED={'growth':[118,119,120,121,122],'experience':[],'injury':[],'rank':[101],'spouse':[70,71]}
def sha(data):return hashlib.sha256(data).hexdigest()
def audit(installation,prior_path,output,summary):
 installation=installation.resolve()
 for p in (output,summary):
  if p.resolve()==installation or installation in p.resolve().parents:raise ValueError('Read-only PC directory')
 exe=(installation/'san11pk.exe').read_bytes();compressed=prior_path.read_bytes();prior=json.loads(gzip.decompress(compressed))
 if sha(exe)!=EXE_SHA or prior['source_executable_sha256']!=EXE_SHA:raise ValueError('Executable provenance mismatch')
 if sha((ROOT/'core/src/main/resources/content/officers.tsv').read_bytes())!=prior['catalog_sha256']:raise ValueError('Catalog identity baseline changed')
 decoder=NativeOfficerDecoder(exe);first=prior['records'][0];raw=bytes.fromhex(first['raw_hex']);_,original=decoder.decode(raw)
 if original.hex()!=first['actor_hex']:raise ValueError('Original decoder no longer reproduces baseline actor')
 observed={name:[] for name in FIELDS}
 for offset in range(152):
  changed=bytearray(raw);changed[offset]^=1;_,actor=decoder.decode(bytes(changed))
  for name,(start,size) in FIELDS.items():
   if actor[start:start+size]!=original[start:start+size]:observed[name].append(offset)
 if observed!=EXPECTED:raise ValueError('Serializer field map changed: '+str(observed))
 # Signed byte/word width verified by executing the original decoder with extremes.
 signed_cases=0
 for offset,fmt,destination in [(118,'<b',0xd0),(101,'<b',0xa4),(70,'<h',0x60)]:
  for value in (-1,-128,0,127):
   changed=bytearray(raw);struct.pack_into(fmt,changed,offset,value);_,actor=decoder.decode(bytes(changed))
   if struct.unpack_from('<i',actor,destination)[0]!=value:raise ValueError('Native signed width differs')
   signed_cases+=1
 sources={}
 for source in prior['sources']:
  path=(installation/source['path']).resolve()
  if installation not in path.parents:raise ValueError('Source path escapes installation')
  data=path.read_bytes()
  if sha(data)!=source['sha256']:raise ValueError('Source changed: '+source['path'])
  sources[source['path']]=data
 mappings={(m['source'],m['native_index']):m for m in prior['mappings']}
 counts={name:collections.Counter() for name in ('growth','rank','spouse')};records=[]
 for row in prior['records']:
  raw=sources[row['source']][row['offset']:row['offset']+row['bytes']]
  if len(raw)!=152 or sha(raw)!=row['sha256'] or raw.hex()!=row['raw_hex']:raise ValueError('Record provenance mismatch')
  actor=bytes.fromhex(row['actor_hex']);growth=list(struct.unpack_from('<5b',raw,118));rank=struct.unpack_from('<b',raw,101)[0];spouse=struct.unpack_from('<h',raw,70)[0]
  if tuple(growth)!=struct.unpack_from('<5i',actor,0xd0) or rank!=struct.unpack_from('<i',actor,0xa4)[0] or spouse!=struct.unpack_from('<i',actor,0x60)[0]:raise ValueError('Record field map mismatch')
  mapping=mappings[(row['source'],row['native_index'])];spouse_mapping=mappings.get((row['source'],spouse))
  counts['growth'].update(growth);counts['rank'][rank]+=1;counts['spouse'][spouse]+=1
  records.append(dict(source=row['source'],native_index=row['native_index'],record_offset=row['offset'],record_sha256=row['sha256'],name=row['decoded']['name'],
   project_id=mapping['project_id'],identity=mapping['identity'],growth_codes=growth,rank_native_id=rank,spouse_native_id=spouse,
   spouse_project_id=None if spouse_mapping is None else spouse_mapping['project_id'],
   field_source_offsets=dict(growth=[row['offset']+i for i in EXPECTED['growth']],rank=row['offset']+101,spouse=row['offset']+70)))
 if len(records)!=13600 or len(sources)!=16:raise ValueError('Expected complete16x850 baseline')
 report=dict(schema=1,source_executable_sha256=EXE_SHA,prior_audit_sha256=sha(compressed),catalog_sha256=prior['catalog_sha256'],
  sources=prior['sources'],serializer=prior['serializer'],field_map=EXPECTED,
  verification=dict(perturbed_source_bytes=152,signed_edge_cases=signed_cases,records_cross_checked=len(records)),
  value_counts={k:dict(sorted(v.items())) for k,v in counts.items()},records=records,
  limits=['Read-only audit only; no runtime content or saved world rewritten',
   'Growth codes0..8 retained without assigning unverified localized curve names; full age curve effects pending',
   'Experience/injury have no serialized source bytes in this152-byte scenario path; zero-filled decoder values are not source configuration',
   'Actual constructor/startup defaults and MOD activation require separate call-chain evidence',
   'Rank IDs and spouse references retained; existing per-source verified person mappings do not imply appearance or active status',
   'No new official-scenario identity assigned from filename/date alone'])
 data=json.dumps(report,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode();packed=gzip.compress(data,mtime=0);output.write_bytes(packed)
 compact={k:v for k,v in report.items() if k not in ('records','serializer')};compact.update(output_sha256=sha(packed),decoded_sha256=sha(data),output_bytes=len(packed))
 summary.write_text(json.dumps(compact,ensure_ascii=False,indent=2)+'\n');print(json.dumps(dict(records=len(records),source_count=len(sources),output_bytes=len(packed),sha256=sha(packed))))

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for name in ('installation','prior','output','summary'):p.add_argument('--'+name,type=Path,required=True)
 a=p.parse_args();audit(a.installation,a.prior,a.output,a.summary)
