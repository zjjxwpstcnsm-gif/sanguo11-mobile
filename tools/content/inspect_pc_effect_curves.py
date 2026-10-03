#!/usr/bin/env python3
"""Bounded read-only motion program inspection; no replacement animations."""
import argparse
import collections
import json
import struct
from pathlib import Path
from pc_resources import Archive, sha
from pc_effect_curves import scalar_program, matrix_program, UnexaminedOpcode

ROOT = Path(__file__).resolve().parents[2]


def inspect(installation, catalog, output):
    source = json.loads(catalog.read_text())
    exe = (installation / 'san11pk.exe').read_bytes()
    if sha(exe) != source['executable_sha256']:
        raise ValueError('Source executable changed')
    archive = Archive(installation / source['source_archive'])
    curves = {}
    opcodes = collections.Counter()
    unresolved = collections.Counter()
    try:
        for row in source['effects'] + source['extra_pk_effects']:
            data = archive.read(row['resource_id'])
            if sha(data) != row['sha256']:
                raise ValueError('Source effect changed')
            motions = {}

            def walk(node):
                for m in node.get('motions', []):
                    motions[m['offset']] = m
                for child in node.get('children', []):
                    walk(child)

            for child in row['graph']['children']:
                for m in child['motions']:
                    motions[m['offset']] = m
                for node in child['renderers']:
                    walk(node)
            for motion in motions.values():
                start = motion['offset']
                end = start + motion['bytes']
                for field in (0x10,0x14,0x24,0x2c,0x30,0x34,0x38,0x3c,0x40,0x44,0x48,0x4c,0x50):
                    relative = struct.unpack_from('<I', data, start + field)[0]
                    if not relative:
                        continue
                    pointer = start + relative
                    if pointer < start + 0x80 or pointer + 16 > end:
                        raise ValueError('Motion expression pointer boundary')
                    size, value_type, initialized, opcode = struct.unpack_from('<4I', data, pointer)
                    if size < 20 or pointer + size > end or initialized:
                        raise ValueError('Motion expression record boundary/state')
                    key = (row['resource_id'], pointer)
                    if key not in curves:
                        raw = data[pointer:pointer + size]
                        record = dict(resource_id=row['resource_id'], offset=pointer, bytes=size,
                                      sha256=sha(raw), value_type_raw=value_type, opcode_raw=opcode,
                                      references=[], runtime_binding=None)
                        try:
                            if value_type==2:
                                record['program']=matrix_program(data,pointer,end)
                                record['status']='matrix-literal-bounds-and-relative-pointer-only' if opcode==0 else 'matrix-rotation-expression-bounds-only'
                                curves[key]=record
                                curves[key]['references'].append(dict(motion_offset=start,field=hex(field)))
                                continue
                            parsed = scalar_program(data, pointer, end)
                            record['program'] = parsed['program']
                            record['status'] = 'scalar-expression-bounds-and-opcodes-only'

                            def tally(n):
                                if isinstance(n, dict):
                                    if 'opcode' in n:
                                        opcodes[n['opcode']] += 1
                                    for v in n.values():
                                        tally(v)
                                elif isinstance(n, list):
                                    for v in n:
                                        tally(v)
                            tally(parsed['program'])
                        except (ValueError, struct.error) as error:
                            record['status'] = 'unresolved'
                            record['reason'] = str(error)
                            unresolved[str(error)] += 1
                        curves[key] = record
                    curves[key]['references'].append(dict(motion_offset=start, field=hex(field)))
        result = dict(schema=1, goal_complete=False, executable_sha256=sha(exe),
                      source_catalog=catalog.relative_to(ROOT).as_posix(),
                      dispatch_table_va='0x7957a0', dispatch_table_entries=48,dispatch_table_sha256=sha(exe[0x3957a0:0x395860]),
                      curves=list(curves.values()), scalar_opcode_counts=dict(opcodes),
                      unresolved_counts=dict(unresolved), runtime_effects_added=0,
                      limits=['Motion fields retain raw offsets; semantic roles not inferred',
                              'Matrix literals and opcode6 rotation expression boundaries decoded; full evaluation/context/time and other unexamined table semantics remain explicit',
                              'No visual random sampling, interpolation/time or material/texture evaluation yet',
                              'Expression AST is not full source animation restoration'])
        output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
        print(json.dumps(dict(unique_curves=len(curves), scalar_programs=sum('program' in c and c['value_type_raw']==0 for c in curves.values()),
                              matrix_literals=sum(c.get('status')=='matrix-literal-bounds-and-relative-pointer-only' for c in curves.values()),
                              matrix_rotations=sum(c.get('status')=='matrix-rotation-expression-bounds-only' for c in curves.values()),
                              unresolved=sum(unresolved.values()), opcode_counts=dict(opcodes), runtime_effects_added=0)))
    finally:
        archive.close()


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--catalog', type=Path, default=ROOT/'docs/pc-visual/effect-bindings-source.json')
    p.add_argument('--output', type=Path, default=ROOT/'docs/pc-visual/effect-curves-working.json')
    args = p.parse_args()
    inspect(args.installation, args.catalog.resolve(), args.output.resolve())
