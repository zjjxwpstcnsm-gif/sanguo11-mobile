#!/usr/bin/env python3
"""Reproduce source controller evidence; does not produce substitute effects."""
import argparse
import collections
import json
import struct
from pathlib import Path
from pc_resources import Archive, sha
from pc_effect_controllers import controller

ROOT = Path(__file__).resolve().parents[2]


def inspect(installation, catalog, output):
    source = json.loads(catalog.read_text())
    exe = (installation / 'san11pk.exe').read_bytes()
    if sha(exe) != source['executable_sha256']:
        raise ValueError('Source executable changed')
    archive = Archive(installation / source['source_archive'])
    records, unexamined_transforms = {}, []
    errors = collections.Counter()
    kinds = collections.Counter()
    interpolation = collections.Counter()
    try:
        for row in source['effects'] + source['extra_pk_effects']:
            data = archive.read(row['resource_id'])
            if sha(data) != row['sha256']:
                raise ValueError('Source effect changed')

            def add(record, owner, role):
                p = record['offset']
                n = record['bytes']
                key = (row['resource_id'], p)
                if key in records:
                    raise ValueError('Duplicate source controller ownership')
                item = dict(resource_id=row['resource_id'], effect_index=row.get('effect_index'),
                            offset=p, bytes=n, sha256=sha(data[p:p+n]),
                            owner_offset=owner, owner_role=role, runtime_binding=None)
                try:
                    item['controller'] = controller(data, p, p+n)
                    item['status'] = 'source-record-bounds-and-dispatch-only'

                    def tally(c):
                        kinds[c['controller_kind']] += 1
                        if 'interpolator' in c:
                            interpolation[c['interpolator']['type']] += 1
                        if 'nested' in c:
                            tally(c['nested'])
                    tally(item['controller'])
                except (ValueError, struct.error) as error:
                    item.update(status='unresolved', reason=str(error))
                    errors[str(error)] += 1
                records[key] = item

            def array(owner, count_offset, pointer_offset, role):
                p, n = owner['offset'], owner['bytes']
                count = struct.unpack_from('<I', data, p+count_offset)[0]
                rel = struct.unpack_from('<I', data, p+pointer_offset)[0]
                if count > 4096 or rel + count*4 > n:
                    raise ValueError('KSEF controller array boundary')
                for i in range(count):
                    offset = p+struct.unpack_from('<I', data, p+rel+i*4)[0]
                    if offset < p or offset+4 > p+n:
                        raise ValueError('KSEF controller record pointer boundary')
                    size = struct.unpack_from('<I', data, offset)[0]
                    if size < 20 or offset+size > p+n:
                        raise ValueError('KSEF controller owned record boundary')
                    add(dict(offset=offset, bytes=size), p, role)

            def walk(node):
                if node['type'] == 3:
                    for r in node['controllers']:
                        add(r, node['offset'], 'composite')
                    # These records have a different header from the root
                    # transform. Applying root +0x80/+0x84 produced 2152 false
                    # array failures in the initial diagnostic; do not do that.
                    for r in node['transforms']:
                        unexamined_transforms.append(dict(resource_id=row['resource_id'], **r,
                                                         status='distinct-transform-layout-unresolved'))
                    for child in node['children']:
                        walk(child)
                elif node['type'] in (0, 1, 2):
                    array(node, 0x118, 0x11c, 'leaf')

            for child in row['graph']['children']:
                for transform in child['transforms']:
                    array(transform, 0x80, 0x84, 'root-transform')
                for node in child['renderers']:
                    walk(node)
        result = dict(schema=1, goal_complete=False,
                      source_archive=source['source_archive'], executable_sha256=sha(exe),
                      source_catalog=catalog.relative_to(ROOT).as_posix(),
                      controller_table=dict(va='0x794cf0', count=28, stride=20,
                                            sha256=sha(exe[0x394cf0:0x394f20])),
                      interpolator_table=dict(va='0x794f28', count=12, stride=16,
                                              sha256=sha(exe[0x394f28:0x394fe8])),
                      controllers=list(records.values()), controller_kind_counts=dict(kinds),
                      interpolator_counts=dict(interpolation), unresolved_counts=dict(errors),
                      unexamined_composite_transforms=unexamined_transforms,
                      runtime_effects_added=0,
                      limits=['Controller context fields, clocks, materials and gameplay causes remain unresolved',
                              'Scalar expressions and stateful controllers are distinct formats',
                              'No sprite geometry, emission, time evolution or visual random stream is executed',
                              'Nested type31 tables and nonscalar expression programs may remain unresolved',
                              'Source decoding is not PC pixel/timing acceptance'])
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n')
        print(json.dumps(dict(controllers=len(records), parsed=sum('controller' in x for x in records.values()),
                              unresolved=dict(errors), kinds=dict(kinds), interpolators=dict(interpolation),
                              unexamined_transforms=len(unexamined_transforms), runtime_effects_added=0)))
    finally:
        archive.close()


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--catalog', type=Path, default=ROOT/'docs/pc-visual/effect-bindings-source.json')
    p.add_argument('--output', type=Path, default=ROOT/'docs/pc-visual/effect-controllers-working.json')
    args = p.parse_args()
    inspect(args.installation, args.catalog.resolve(), args.output.resolve())
