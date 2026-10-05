#!/usr/bin/env python3
"""Compare declared opening affiliations using verified native person identities.

This audits authored properties, not runtime appearance or full PC opening parity.
It never changes scenario data or saves and does not infer a native site from9c.
"""
import argparse
import gzip
import hashlib
import json
from collections import Counter
from pathlib import Path
from compare_pc_scenario_ownership import PAIRS, ROOT


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def compare(audit_dir, output):
    inputs={}
    reports={}
    for kind in ('officers','domains','metadata','placements'):
        path=audit_dir/('scenario-'+kind+'-native.json.gz')
        raw=path.read_bytes()
        inputs[str(path.relative_to(ROOT) if ROOT in path.parents else path)]=digest(raw)
        reports[kind]=json.loads(gzip.decompress(raw))
    officers,domains,metadata,placements=[reports[k] for k in ('officers','domains','metadata','placements')]
    if len({officers['source_executable_sha256'],domains['source_executable_sha256'],metadata['source_executable_sha256'],placements['executable_sha256']})!=1:
        raise ValueError('Native executable evidence differs')
    identity_raw=(audit_dir/'scenario-officers-native.json.gz').read_bytes()
    if placements['identity_audit_sha256']!=digest(identity_raw):
        raise ValueError('Placement identity audit is not the supplied identity mapping')
    names={(r['source'],r['native_index']):r['decoded']['name'] for r in officers['records']}
    domains_by_source={s['path']:s for s in domains['sources']}
    metadata_by_source={s['path']:s for s in metadata['sources']}
    officer_source_sha={s['path']:s['sha256'] for s in officers['sources']}
    comparisons=[]
    for source in placements['sources']:
        scenario=PAIRS.get(Path(source['path']).name.lower())
        if scenario is None:
            continue
        domain=domains_by_source[source['path']]
        meta=metadata_by_source[source['path']]
        if len({source['sha256'],domain['sha256'],meta['sha256'],officer_source_sha[source['path']]})!=1:
            raise ValueError('Scenario source evidence differs')
        path=ROOT/'core/src/main/resources/scenarios'/(scenario+'.properties')
        raw=path.read_bytes();props={}
        for line in raw.decode('utf-8').splitlines():
            if not line or line.startswith('#'):
                continue
            if '\\' in line or '=' not in line:
                raise ValueError('Unexamined property encoding')
            key,value=line.split('=',1)
            if key in props:
                raise ValueError('Duplicate property')
            props[key]=value
        if [int(props['year']),int(props['month']),1]!=meta['decoded']['date']:
            raise ValueError('Explicit date-aligned comparison changed')
        roster={}
        for i in range(int(props['officers'])):
            fields=props['officer.'+str(i)].split('|')
            if len(fields)!=9 or int(fields[0]) in roster:
                raise ValueError('Malformed/duplicate authored officer')
            roster[int(fields[0])]=fields
        force_names={r['native_index']:names.get((source['path'],r['decoded']['name_officer_native_index']))
                     for r in domain['records'] if r['kind']=='force'}
        differences=[];unknown=[];checked=0;matched_ids=set()
        for record in source['records']:
            row={k:record[k] for k in ('native_index','project_id','sha256','identity','district_native_index','force_native_index','native_status_label')}
            row['native_name']=names[(source['path'],record['native_index'])]
            person=record['project_id']
            if person is None:
                row['reason']='identity_unresolved';unknown.append(row);continue
            matched_ids.add(person)
            if person not in roster:
                row['reason']='not_in_authored_roster';differences.append(row);continue
            project=roster[person];owner=int(project[2]);native_force=record['force_native_index']
            row.update(project_name=project[1],project_owner=owner,project_city_id=int(project[3]),
                       project_faction_name=props['faction.'+str(owner)].removesuffix('军') if owner>=0 else None,
                       native_faction_name=force_names.get(native_force) if native_force>=0 else None)
            if native_force>=0 and row['native_faction_name'] is None:
                row['reason']='native_force_name_unresolved';unknown.append(row);continue
            checked+=1
            if row['project_faction_name']!=row['native_faction_name']:
                row['reason']='declared_affiliation_differs';differences.append(row)
        comparisons.append(dict(source=source['path'],source_sha256=source['sha256'],project=scenario,
            project_sha256=digest(raw),project_officers=len(roster),checked_affiliations=checked,
            differences=differences,unknown=unknown,
            difference_reasons=dict(sorted(Counter(r['reason'] for r in differences).items())),
            difference_statuses=dict(sorted(Counter(r['native_status_label'] or 'unknown' for r in differences).items())),
            unknown_reasons=dict(sorted(Counter(r['reason'] for r in unknown).items())),
            project_ids_without_verified_native_identity=sorted(roster.keys()-matched_ids)))
    if len(comparisons)!=len(PAIRS):
        raise ValueError('Missing date-aligned pair')
    report=dict(schema=1,inputs=inputs,comparisons=comparisons,limits=[
        'These authored openings retain separate identity; equal calendar dates do not establish official equivalence',
        'Compare declared affiliation only; current runtime lifecycle and PC appearance/event processing remain separate',
        'Native source name-officer labels identify forces; no integer force-ID equivalence is assumed',
        'Raw location9c and unavailable person identities are not inferred; no source or project data or save is changed'])
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps([dict(project=r['project'],checked=r['checked_affiliations'],differences=len(r['differences']),unknown=len(r['unknown'])) for r in comparisons]))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--audit-dir',type=Path,default=ROOT/'docs/pc-data')
    p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();compare(a.audit_dir.resolve(),a.output)
