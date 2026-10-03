#!/usr/bin/env python3
"""Inspect genuine KSEF UV programs and serialized primitive defaults.

Retains exact resource/record hashes and undecoded semantics. Does not add
source effects to Android or assert PC-matched timings/materials.
"""
import argparse
import collections
import json
import struct
from pathlib import Path
from pc_resources import Archive, sha
from pc_effect_formats import graph
from pc_effect_uv import decode_uv
from inspect_pc_effect_bindings import EXE_SHA, ROOT


def inspect(installation, report):
    exe = (installation / 'san11pk.exe').read_bytes()
    if sha(exe) != EXE_SHA:
        raise ValueError('Reinspect UV dispatch for changed EXE')
    archive = Archive(installation / 'Media/san11pkres.bin')
    rows = []; kinds = collections.Counter(); selectors = collections.Counter()
    try:
        sources = [(effect,struct.unpack_from('<I',exe,0x37692c+effect*12+4)[0]) for effect in range(244)]
        sources.extend((None,resource) for resource in (4861,4862))
        for effect, resource in sources:
            data = archive.read(resource)
            pointer = struct.unpack_from('<I',exe,0x37692c+effect*12)[0]-0x400000 if effect is not None else None
            source_file = exe[pointer:exe.index(b'\0',pointer)].decode('ascii') if pointer is not None else None
            def visit(node):
                if node['type'] in (0, 1, 2):
                    owner = node['offset']; size = node['bytes']
                    default = struct.unpack_from('<I', data, owner+0x14)[0]
                    if not 0x130 <= default <= size-0x40:
                        raise ValueError('KSEF default context bounds')
                    at = owner+default
                    length = struct.unpack_from('<I', data, at)[0]
                    if length < 0x40 or default+length > size:
                        raise ValueError('KSEF default context length')
                    primitive, blend, reserved, selector, extra, queue = struct.unpack_from('<IHHHBB', data, at+0x10)
                    # 442ad0 dispatches payload+0, 442060 binds its texture
                    # from payload+8 plus the active texture-library base.
                    # Fullscreen dynamic substitution may change the selector.
                    if primitive not in range(8):
                        raise ValueError('Unexamined source primitive dispatch')
                    uv = decode_uv(data, owner, size)
                    rows.append(dict(effect_index=effect, resource_id=resource,
                                     source_file=source_file,source_sha256=sha(data), renderer_offset=owner,
                                     renderer_type=node['type'],
                                     default_context=dict(offset=at, bytes=length,
                                        sha256=sha(data[at:at+length]), primitive_kind=primitive,
                                        blend_mode_raw=blend, texture_selector_raw=selector,
                                        blend_reserved_raw=reserved,
                                        blend_source=('source-alpha','source-alpha','one','source-alpha','one')[blend] if blend < 5 else None,
                                        blend_destination='inverse-source-alpha' if blend == 0 else 'one' if blend < 5 else None,
                                        blend_operation='add' if blend < 3 else 'reverse-subtract' if blend < 5 else None,
                                        selector_extra_raw=extra, queue_marker_raw=queue,
                                        texture_binding='442060: selector plus active library base; dynamic substitutions pending',
                                        common_resource_candidate=124 if selector < 33 else None,
                                        tint_bgra=list(data[at+0x20:at+0x24]),
                                        size_xy=list(struct.unpack_from('<2f',data,at+0x24))),
                                     uv=uv, android_output=None, runtime_binding=None))
                    kinds[uv['kind'] if uv else 'absent'] += 1
                    selectors[selector] += 1
                for child in node.get('children', []):
                    visit(child)
            for child in graph(data)['children']:
                for node in child['renderers']:
                    visit(node)
        result = dict(schema=1, goal_complete=False, executable_sha256=sha(exe),
                      source_archive='Media/san11pkres.bin', source_policy='read-only',
                      sources=dict(effect_table_resources=244,extra_pk_resources=[4861,4862],
                                   extra_pk_index_binding='unresolved; no assumed effect-index correspondence'),
                      source_evidence=dict(uv_dispatch='46d670 / 7961a8, 20-byte stride',
                          uv_update='45d280 +110 -> context+50 -> rectangle+30',
                          primitive_dispatch='442ad0 -> 442bdc, payload+0',
                          texture_selector='442060 payload+8 and library+8 -> 44fd40',
                          blend_selector='442710 payload+4 -> D3D states171/19/20',
                          conversion_helper='707a74 truncates float toward zero'),
                      counts=dict(leaf_records=len(rows), uv_kinds={str(k):v for k,v in kinds.items()},
                                  texture_selectors={str(k):v for k,v in sorted(selectors.items())}),
                      records=rows,
                      limits=['No emitter/material/transforms integrated',
                              'Original controller clocks and PC video acceptance pending',
                              'Common selector candidates require active library/dynamic substitution resolution',
                              'Leaf type6 external callbacks remain unresolved; no runtime effects added'])
        report.parent.mkdir(parents=True, exist_ok=True)
        report.write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n')
        print(json.dumps(result['counts']))
    finally:
        archive.close()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    parser.add_argument('--report', type=Path, default=ROOT/'docs/pc-visual/effect-uv-working.json')
    args = parser.parse_args(); inspect(args.installation, args.report)
