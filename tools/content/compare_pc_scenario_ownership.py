#!/usr/bin/env python3
"""Compare explicit date-aligned project openings with native site ownership evidence.

This does not rename, replace or declare a custom opening equivalent to a PC one.
Unknown actor glyphs stay unknown rather than becoming assumed faction identities.
"""
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PAIRS = {'scen000.s11': 'huangjin-184', 'scen001.s11': 'coalition-190',
         'scen002.s11': 'warlords-194', 'scen003.s11': 'guandu-200', 'scen004.s11': 'chibi-207'}


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def compare(officers_path, domains_path, metadata_path, output):
    officers, domains, metadata = [json.loads(p.read_text()) for p in (officers_path, domains_path, metadata_path)]
    if len({r['source_executable_sha256'] for r in (officers, domains, metadata)}) != 1:
        raise ValueError('Mismatched executable evidence')
    officer_sources = {s['path']: s['sha256'] for s in officers['sources']}
    meta = {s['path']: s for s in metadata['sources']}
    names = {(r['source'], r['native_index']): r['decoded']['name'] for r in officers['records']}
    rows = []
    for source in domains['sources']:
        key = Path(source['path']).name.lower()
        if key not in PAIRS:
            continue
        if not officer_sources[source['path']] == source['sha256'] == meta[source['path']]['sha256']:
            raise ValueError('Mismatched scenario evidence')
        path = ROOT / 'core/src/main/resources/scenarios' / (PAIRS[key] + '.properties')
        raw = path.read_bytes()
        properties = {}
        for line in raw.decode('utf-8').splitlines():
            if not line or line.startswith('#'):
                continue
            if '\\' in line or '=' not in line:
                raise ValueError('Unexamined property encoding')
            field, value = line.split('=', 1)
            if field in properties:
                raise ValueError('Duplicate property')
            properties[field] = value
        if [int(properties['year']), int(properties['month']), 1] != meta[source['path']]['decoded']['date']:
            raise ValueError('Comparison pair date changed; review explicitly')
        sites = {int(value.split('|')[0]): value.split('|') for field, value in properties.items() if field.startswith('city.') and field[5:].isdigit()}
        forces = {r['native_index']: r['decoded']['name_officer_native_index'] for r in source['records'] if r['kind'] == 'force'}
        differences, unknown = [], []
        checked = 0
        for native in source['records']:
            if native['kind'] not in ('city', 'gate', 'port'):
                continue
            site = sites[native['identity']['project_id']]
            owner = int(site[4])
            faction = properties['faction.' + str(owner)] if owner >= 0 else None
            project_name = faction.removesuffix('军') if faction is not None else None
            force = native['decoded']['force_native_index']
            person = forces[force] if force >= 0 else None
            native_name = names.get((source['path'], person)) if person is not None else None
            row = dict(project_site_id=int(site[0]), site_name=native['identity']['source_name'],
                       kind=native['kind'], native_index=native['native_index'],
                       native_district=native['decoded']['district_native_index'], native_force=force,
                       native_name_officer=person, native_faction_name=native_name,
                       project_faction=owner, project_faction_name=project_name)
            if force >= 0 and native_name is None:
                unknown.append(row)
                continue
            checked += 1
            if project_name != native_name:
                differences.append(row)
        rows.append(dict(source=source['path'], source_sha256=source['sha256'], source_name=meta[source['path']]['decoded']['name'],
                         project_id=properties['id'], project_name=properties['name'], project_sha256=sha(raw),
                         scope='explicit date-aligned comparison; openings retain independent identity',
                         checked=checked, differences=differences, unknown=unknown))
    if len(rows) != len(PAIRS):
        raise ValueError('Missing comparison pair')
    report = dict(schema=1, inputs={p.name: sha(p.read_bytes()) for p in (officers_path, domains_path, metadata_path)},
                  source_executable_sha256=domains['source_executable_sha256'], comparisons=rows,
                  limits=['Force identity compared through its source name-officer lookup; no inferred project faction ID conversion',
                          'heroes-250 and three sandbox maps have no declared one-to-one PC opening comparison',
                          'Resource stocks, officers, armies and facility differences remain separate work',
                          'No runtime data or user save changed'])
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps([dict(project=r['project_id'], checked=r['checked'], differences=len(r['differences']), unknown=len(r['unknown'])) for r in rows]))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--officers', type=Path, required=True)
    parser.add_argument('--domains', type=Path, required=True)
    parser.add_argument('--metadata', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    compare(args.officers, args.domains, args.metadata, args.output)
