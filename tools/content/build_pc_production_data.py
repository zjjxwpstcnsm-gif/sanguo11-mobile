#!/usr/bin/env python3
"""Reproduce original shared prices and16-source city flag bindings. PC read only.
No online tables, current GUI, official/MOD identity inference or save mutation.
"""
import argparse,csv,hashlib,json,struct
from pathlib import Path
from inspect_pc_scenario_tail import NativeTailDecoder
from inspect_pc_scenario_domains import NativeDomainDecoder

def sha(raw):return hashlib.sha256(raw).hexdigest()
def build(installation,output):
 installation=installation.resolve();output=output.resolve()
 if output==installation or installation in output.parents:raise ValueError('Output must not be inside PC installation')
 exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb','Executable differs from verified source';shared=(installation/'Media/scenario/Scenario.s11').read_bytes();tail=NativeTailDecoder(exe);decoded=tail.decode_tail(shared,True);prices=[]
 for row in decoded['records']:
  if row['kind']!='table_7d054':continue
  value=bytes.fromhex(row['actor_hex']);reads=[r for r in row['reads'] if r['destination'] in ('actor+0x98','actor+0x99')];assert len(reads)==1 and reads[0]['bytes']==2
  offset=reads[0]['offset'];gold=struct.unpack_from('<H',shared,offset)[0];assert gold==struct.unpack_from('<H',value,0x98)[0]
  prices.append(dict(native_id=row['native_index'],gold=gold,shared_offset=offset,name=value[4:9].split(b'\0',1)[0].decode('big5')))
 assert [r['native_id'] for r in prices]==list(range(12))
 domain=NativeDomainDecoder(exe);shared_domain=domain.decode(shared,True);catalog=Path(__file__).resolve().parents[2]/'core/src/main/resources/content/sites.tsv';sites=list(csv.DictReader(catalog.open(),delimiter='\t'));identities={}
 for row in shared_domain['records']:
  if row['kind']!='city':continue
  name=bytes.fromhex(row['actor_hex'])[4:9].split(b'\0',1)[0].decode('big5');matches=[s for s in sites if s['kind']=='city' and s['name']==name];assert len(matches)==1,(name,matches);identities[row['native_index']]=dict(project_id=int(matches[0]['id']),name=name)
 assert len(identities)==42
 sources=[];bindings={};paths=sorted((p for p in (installation/'Media/scenario').iterdir() if p.name.lower().startswith('scen0') and p.suffix.lower()=='.s11'),key=lambda p:p.name.lower());assert len(paths)==16
 for path in paths:
  raw=path.read_bytes();decoded=domain.decode(raw);rows=[]
  for row in decoded['records']:
   if row['kind']!='city':continue
   reads=[r for r in row['reads'] if r['destination'] in ['actor+0x'+format(i,'x') for i in range(0x86,0x8c)]];assert len(reads)==6 and all(r['bytes']==1 for r in reads)
   assert [r['destination'] for r in reads]==['actor+0x'+format(i,'x') for i in range(0x86,0x8c)]
   flags=bytes(raw[r['offset']] for r in reads);assert flags==bytes.fromhex(row['actor_hex'])[0x86:0x8c];identity=identities[row['native_index']];old=bindings.setdefault(identity['project_id'],flags);assert old==flags,'Cross-source flag conflict; do not silently pick a source'
   rows.append(dict(native_id=row['native_index'],**identity,flags=list(flags),source_offsets=[r['offset'] for r in reads]))
  assert len(rows)==42;sources.append(dict(path=path.relative_to(installation).as_posix(),sha256=sha(raw),cities=rows));print('DECODED',path.name,'42cities',flush=True)
 output.mkdir(parents=True,exist_ok=False);price_bytes=('native_id\tgold\n'+''.join(str(r['native_id'])+'\t'+str(r['gold'])+'\n' for r in prices)).encode();flag_bytes=('city_id\tflags\n'+''.join(str(city)+'\t'+','.join(map(str,flags))+'\n' for city,flags in sorted(bindings.items()))).encode();(output/'pc-production-prices.tsv').write_bytes(price_bytes);(output/'pc-city-production-flags.tsv').write_bytes(flag_bytes)
 report=dict(schema=1,source_executable_sha256=sha(exe),shared_sha256=sha(shared),site_catalog_sha256=sha(catalog.read_bytes()),prices=prices,sources=sources,outputs={'pc-production-prices.tsv':sha(price_bytes),'pc-city-production-flags.tsv':sha(flag_bytes)},scope='Original serializers;12shared prices672candidate city flag records,42exactname/kind identities. Category label/effective officialMOD/complete openings not inferred')
 (output/'provenance.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');return report
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',required=True,type=Path);a=p.parse_args();report=build(a.installation,a.output);print('PASS original reproducible production data',report['outputs'])
