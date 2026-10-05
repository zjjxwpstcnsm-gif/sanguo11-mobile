#!/usr/bin/env python3
"""Pack native audit reports deterministically, retaining provenance and unknowns."""
import argparse
import gzip
import hashlib
import json
from pathlib import Path


def pack(source, destination, kind):
    raw = source.read_bytes()
    report = json.loads(raw)
    destination.mkdir(parents=True, exist_ok=True)
    packed = gzip.compress(raw, compresslevel=9, mtime=0)
    name = 'scenario-' + kind + '-native.json.gz'
    (destination / name).write_bytes(packed)
    if kind == 'officers':
        summary = {k: v for k, v in report.items() if k not in ('records', 'mappings')}
        summary.update(records=len(report['records']),
                       identity_verified=sum(row['project_id'] is not None for row in report['mappings']),
                       outside_project_catalog=sum(row['identity'] == 'no_project_catalog_identity' for row in report['mappings']),
                       relocations=[row for row in report['mappings'] if row['identity'] == 'relocated_identity_verified'],
                       quarantined=[row for row in report['mappings'] if row['identity'] == 'quarantined'])
    elif kind == 'placements':
        summary = {k: v for k, v in report.items() if k != 'sources'}
        summary['sources'] = []
        for source in report['sources']:
            rows=source['records']
            item={k:v for k,v in source.items() if k!='records'}
            item.update(records=len(rows),project_id_mapped=sum(r['project_id'] is not None for r in rows),
                        assigned_force=sum(r['force_native_index']>=0 for r in rows),
                        raw_status_counts={str(code):sum(r['status_field_a0']==code for r in rows) for code in sorted({r['status_field_a0'] for r in rows})})
            summary['sources'].append(item)
        summary['records'] = sum(len(source['records']) for source in report['sources'])
    elif kind == 'units':
        summary = {k: v for k, v in report.items() if k != 'sources'}
        summary['sources'] = [{k: v for k, v in source.items() if k != 'units'} for source in report['sources']]
        summary['records'] = sum(source['unit_count'] for source in report['sources'])
        summary['valid_unit_count'] = sum(source['valid_unit_count'] for source in report['sources'])
    elif kind == 'metadata':
        summary = {k: v for k, v in report.items() if k != 'sources'}
        summary['sources'] = [{k: v for k, v in source.items() if k != 'records'} for source in report['sources']]
        summary['records'] = len(report['sources'])
    else:
        summary = {k: v for k, v in report.items() if k not in ('sources', 'shared_source')}
        if 'shared_source' in report:
            source = report['shared_source']
            summary['shared_source'] = {k: v for k, v in source.items() if k != 'records'}
            summary['shared_source']['sites'] = [{k: row[k] for k in ('kind', 'native_index', 'offset', 'bytes', 'identity')}
                                                  for row in source['records'] if 'identity' in row]
        summary['sources'] = []
        for source in report['sources']:
            item = {k: v for k, v in source.items() if k != 'records'}
            item['tables'] = []
            for kind_name in dict.fromkeys(row['kind'] for row in source['records']):
                rows = [row for row in source['records'] if row['kind'] == kind_name]
                item['tables'].append(dict(kind=kind_name, count=len(rows), start=rows[0]['offset'],
                                            end=rows[-1]['offset'] + rows[-1]['bytes'],
                                            record_widths=sorted(set(row['bytes'] for row in rows))))
            summary['sources'].append(item)
        summary['records'] = sum(len(s['records']) for s in report['sources'])
    summary['full_report'] = dict(path=name, sha256=hashlib.sha256(packed).hexdigest(),
                                  decoded_sha256=hashlib.sha256(raw).hexdigest())
    (destination / ('scenario-' + kind + '-audit.json')).write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(dict(kind=kind, records=summary['records'], **summary['full_report'])))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--officers', type=Path, required=True)
    parser.add_argument('--domains', type=Path, required=True)
    parser.add_argument('--metadata', type=Path)
    parser.add_argument('--tail', type=Path)
    parser.add_argument('--units', type=Path)
    parser.add_argument('--placements', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    pack(args.officers, args.output, 'officers')
    pack(args.domains, args.output, 'domains')
    if args.metadata:
        pack(args.metadata, args.output, 'metadata')

    if args.tail:
        pack(args.tail, args.output, 'tail')
    if args.units:
        pack(args.units, args.output, 'units')
    if args.placements:
        pack(args.placements, args.output, 'placements')
