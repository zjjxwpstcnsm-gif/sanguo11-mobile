#!/usr/bin/env python3
"""Fingerprint the read-only installation and join source-specific verified identities.

No native ID is promoted to a project ID. No installation candidate is called
active or official. Historical audit records must match the current file bytes.
"""
import argparse
import collections
import gzip
import hashlib
import json
import re
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EXE_SHA = '30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
UNKNOWN = ['courtesy_name', 'loyalty', 'parentage', 'sworn_siblings',
           'likes_dislikes', 'merit', 'biography', 'appearance_conditions',
           'location_semantics', 'skill_effect_binding', 'startup_experience',
           'startup_current_abilities', 'active_load_priority']


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + '\n').encode('utf-8')


def output_guard(installation, output):
    installation, output = installation.resolve(), output.resolve()
    if output == installation or installation in output.parents:
        raise ValueError('Output is inside the read-only PC installation')


def source_variant(path, digest):
    # Path and content both participate: identical backups are distinct variants.
    slug = re.sub('[^a-z0-9-]', '-', path.lower()).strip('-')
    return slug + '-' + sha(path.encode('utf-8'))[:12] + '-' + digest


def inspect_header(head):
    if len(head) >= 32 and head[:4] == b'\x00\x00\xfe\xff' and head[8:18] == b'KOEI%SAN11':
        return dict(format='KOEI-SAN11', native_type=struct.unpack_from('<I', head, 4)[0],
                    versions=list(struct.unpack_from('<II', head, 24)))
    for magic in (b'LS11', b'LINK', b'MZ', b'BM'):
        if head.startswith(magic):
            return dict(format=magic.decode('ascii'))
    return dict(format='unknown')


def inventory(installation):
    rows = []
    for path in sorted(installation.rglob('*'), key=lambda p: p.relative_to(installation).as_posix()):
        if path.is_symlink():
            raise ValueError('Unreviewed installation symlink: ' + str(path))
        if not path.is_file():
            continue
        digest = hashlib.sha256()
        with path.open('rb') as stream:
            head = stream.read(96)
            digest.update(head)
            for block in iter(lambda: stream.read(1024 * 1024), b''):
                digest.update(block)
        relative = path.relative_to(installation).as_posix()
        rows.append(dict(path=relative, bytes=path.stat().st_size, sha256=digest.hexdigest(),
                         **inspect_header(head)))
    return rows


def read_audits(directory):
    reports, provenance = {}, {}
    for name in ('scenario-officers', 'scenario-metadata', 'scenario-placements', 'officer-ability-sources'):
        path = directory / (name + '-native.json.gz')
        packed = path.read_bytes()
        raw = gzip.decompress(packed)
        report = json.loads(raw)
        exe = report.get('source_executable_sha256', report.get('executable_sha256'))
        if exe != EXE_SHA:
            raise ValueError('Audit executable mismatch: ' + name)
        reports[name] = report
        provenance[name] = dict(path='docs/pc-data/' + path.name, sha256=sha(packed), decoded_sha256=sha(raw))
    return reports, provenance


def join(installation, files, reports, provenance, catalog):
    by_path = {r['path']: r for r in files}
    if by_path.get('san11pk.exe', {}).get('sha256') != EXE_SHA:
        raise ValueError('Executable changed; inspect original readers again')
    officers = reports['scenario-officers']
    if officers['catalog_sha256'] != sha(catalog.read_bytes()):
        raise ValueError('Identity catalog changed')
    identity_sha = provenance['scenario-officers']['sha256']
    if reports['scenario-placements']['identity_audit_sha256'] != identity_sha:
        raise ValueError('Placement identity provenance differs')
    if reports['officer-ability-sources']['prior_audit_sha256'] != identity_sha:
        raise ValueError('Ability identity provenance differs')
    mappings = {(m['source'], m['native_index']): m for m in officers['mappings']}
    abilities = {(r['source'], r['native_index']): r for r in reports['officer-ability-sources']['records']}
    placements = {(s['path'], r['native_index']): r for s in reports['scenario-placements']['sources'] for r in s['records']}
    metadata = {s['path']: s for s in reports['scenario-metadata']['sources']}
    source_data, scenarios = {}, []
    for source in officers['sources']:
        path = source['path']
        current = by_path.get(path)
        if current is None or current['sha256'] != source['sha256'] or metadata[path]['sha256'] != source['sha256']:
            raise ValueError('Scenario source changed: ' + path)
        raw = (installation / path).read_bytes()
        if sha(raw) != source['sha256']:
            raise ValueError('Scenario changed during inventory: ' + path)
        source_data[path] = raw
        decoded = metadata[path]['decoded']
        variant = source_variant(path, current['sha256'])
        slot = int(re.search(r'scen(\d+)', path, re.I)[1])
        scenario_id = 'pc-scen%03d-%s' % (slot, current['sha256'])
        scenarios.append(dict(scenarioId=scenario_id, sourceVariant=variant, sourcePath=path,
                              sourceSha256=current['sha256'], slot=slot,
                              embeddedNativeId=decoded['native_id'], name=decoded['name'], date=decoded['date'],
                              description=decoded['description'], versions=source['versions'],
                              origin='unknown', activation='installed-candidate', loadPriority='unknown',
                              decodedBoundaries=[90, 16183, 16194, 17760, 146960, 162702, 170010],
                              unknown=['effective_override_chain', 'official_or_mod_identity', 'startup_events',
                                       'complete_initial_world', 'selectable_forces']))
    scenario_by_path = {s['sourcePath']: s for s in scenarios}
    result, requests = [], []
    for record in officers['records']:
        key = (record['source'], record['native_index'])
        if key not in mappings or key not in placements or key not in abilities:
            raise ValueError('Missing per-source join')
        mapping, placement, ability = mappings[key], placements[key], abilities[key]
        raw = source_data[key[0]][record['offset']:record['offset'] + record['bytes']]
        if len(raw) != 152 or sha(raw) != record['sha256'] or raw.hex() != record['raw_hex']:
            raise ValueError('Officer record changed: ' + str(key))
        if placement['sha256'] != record['sha256'] or ability['record_sha256'] != record['sha256']:
            raise ValueError('Record audit provenance differs')
        if placement['project_id'] != mapping['project_id'] or ability['project_id'] != mapping['project_id']:
            raise ValueError('Per-source identity disagreement')
        decoded, scenario = record['decoded'], scenario_by_path[key[0]]
        unknown = list(UNKNOWN)
        if mapping['project_id'] is None:
            unknown.append('project_identity')
        if decoded['name'] is None:
            unknown.append('name_gaiji')
        row = dict(officerId=mapping['project_id'], nativeId=key[1], sourceVariant=scenario['sourceVariant'],
                   sourcePath=key[0], sourceSha256=scenario['sourceSha256'], recordSha256=record['sha256'],
                   recordOffset=record['offset'], identityStatus=mapping['identity'], name=decoded['name'],
                   nameBytes=decoded['name_bytes'], faceNativeId=decoded['face_id'], sex=decoded['sex'],
                   birth=decoded['birth'], death=decoded['death'], appearance=decoded['appearance'],
                   sourceScenarioDate=scenario['date'], baseStats=decoded['stats'],
                   aptitudeCodes=decoded['aptitude_codes'], skillNativeId=decoded['skill_native_id'],
                   growthCodes=ability['growth_codes'], rankNativeId=ability['rank_native_id'],
                   spouseNativeId=ability['spouse_native_id'], spouseOfficerId=ability['spouse_project_id'],
                   districtNativeId=placement['district_native_index'], forceNativeId=placement['force_native_index'],
                   locationRaw=placement['location_field_9c'], statusRaw=placement['status_field_a0'],
                   sourceStatusLabel=placement['native_status_label'],
                   coverage=dict(complete=False, unknown=unknown, sourceDecodedFields=[
                       'name_bytes', 'sex', 'birth', 'death', 'appearance', 'base_stats', 'aptitudes',
                       'skill_native_id', 'growth_codes', 'rank_native_id', 'spouse_native_id',
                       'district_native_id', 'force_native_id', 'status_raw', 'location_raw']))
        result.append(row)
        requests.append({k: row[k] for k in ('officerId', 'nativeId', 'sourceVariant', 'sourcePath',
                         'sourceSha256', 'recordSha256', 'identityStatus', 'nameBytes', 'faceNativeId',
                         'birth', 'sourceScenarioDate')})
    if len(result) != 13600 or len({(r['sourceVariant'], r['nativeId']) for r in result}) != len(result):
        raise ValueError('Expected complete 16x850 distinct source slots')
    duplicates = collections.defaultdict(list)
    for scenario in scenarios:
        duplicates[scenario['embeddedNativeId']].append(scenario['scenarioId'])
    manifest = dict(schema=1, complete=False, sourceExecutableSha256=EXE_SHA, provenance=provenance,
                    installationFiles=files, installationBytes=sum(r['bytes'] for r in files), scenarios=scenarios,
                    candidateScenarioFiles=[dict(sourcePath=r['path'], sourceSha256=r['sha256'],
                        sourceVariant=source_variant(r['path'], r['sha256']), bytes=r['bytes'],
                        versions=r['versions'], scope=('primary-media-directory' if r['path'].lower().startswith('media/scenario/')
                            else 'backup-directory' if r['path'].lower().startswith('backup/') else 'attachment-directory'),
                        activation='unknown', origin='unknown', loadPriority='unknown',
                        decoded=r['path'] in scenario_by_path) for r in files if r.get('native_type')==22],
                    duplicateEmbeddedIds={str(k): v for k, v in duplicates.items() if len(v) > 1},
                    coverage=dict(sourceSlots=len(result), verifiedIdentityRecords=sum(r['officerId'] is not None for r in result),
                                  distinctVerifiedOfficers=len({r['officerId'] for r in result if r['officerId'] is not None}),
                                  completeOfficerRecords=0, candidateScenarios=len(scenarios), completeScenarios=0),
                    limits=['Installation presence does not prove activation, load priority or official identity',
                            'Source slots include placeholders and special roles; never counted as playable identities',
                            'Startup experience and current values require original initialization, not decoder zero buffers',
                            'No runtime, save, image or media asset modified'])
    return manifest, result, requests


def audit(installation, output, audit_directory=ROOT / 'docs/pc-data'):
    installation, output = installation.resolve(), output.resolve()
    output_guard(installation, output)
    reports, provenance = read_audits(audit_directory)
    files = inventory(installation)
    manifest, officers, requests = join(installation, files, reports, provenance,
                                       ROOT / 'core/src/main/resources/content/officers.tsv')
    output.mkdir(parents=True, exist_ok=True)
    outputs = {
        'officer-metadata-manifest.json.gz': gzip.compress(json_bytes(dict(schema=1, complete=False, officers=officers)), mtime=0),
        'portrait-requirements.json.gz': gzip.compress(json_bytes(dict(schema=1, requests=requests)), mtime=0),
    }
    manifest['outputs'] = {name: dict(sha256=sha(raw), bytes=len(raw), decodedSha256=sha(gzip.decompress(raw)))
                           for name, raw in outputs.items()}
    outputs['source-manifest.json'] = json_bytes(manifest)
    for name, raw in outputs.items():
        (output / name).write_bytes(raw)
    return manifest


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    report = audit(args.installation, args.output)
    print(json.dumps(dict(files=len(report['installationFiles']), bytes=report['installationBytes'],
                          coverage=report['coverage'], outputs=report['outputs'])))
