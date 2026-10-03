#!/usr/bin/env python3
"""Crosscheck facility labels from this installation against its EXE resolver.

Only the 12-byte terminated name field of the 191-byte Scenario.s11 records is
decoded. Remaining record fields include stale bytes and are not interpreted.
No authority state, map or source installation is modified.
"""
import argparse
import json
import struct
from pathlib import Path
from pc_resources import Archive, sha

ROOT = Path(__file__).resolve().parents[2]


def inspect(installation):
    exe = (installation / 'san11pk.exe').read_bytes()
    scenario = (installation / 'Media/scenario/Scenario.s11').read_bytes()
    if scenario[:4] != b'\x00\x00\xfe\xff' or scenario[8:18] != b'KOEI%SAN11':
        raise ValueError('Scenario definition header differs')
    labels = []
    for index in range(64):
        offset = 0xb82 + index * 191
        field = scenario[offset:offset + 12]
        if len(field) != 12 or b'\0' not in field:
            raise ValueError('Unterminated facility label')
        labels.append(field.split(b'\0', 1)[0].decode('cp950'))
    if [labels[i] for i in (0, 3, 9, 24, 31, 59)] != [
            '都市', '陣', '石牆', '堤防', '市場 Lv1', '廄舍 Lv3']:
        raise ValueError('Definition layout changed; re-inspect before binding')
    # VA59f7d0 is a bounded 60-entry switch. Read its exact jump targets and
    # accept only xor al,al;ret, mov eax,constant;ret, or the default ret.
    if exe[0x19f7d0:0x19f7e6].hex() != '8b4c240433c083f93b0f874d010000ff248d30f95900':
        raise ValueError('Facility resolver changed')
    models = struct.unpack_from('<388I', exe, 0x363740)
    archive = Archive(installation / 'Media/san11pkres.bin')
    entries = []
    for index, name in enumerate(labels):
        kind = None
        target = None
        if index < 60:
            target = struct.unpack_from('<I', exe, 0x19f930 + index * 4)[0]
            code = exe[target - 0x400000:target - 0x400000 + 6]
            if code[:3] == b'\x32\xc0\xc3':
                kind = 0
            elif code[:1] == b'\xb8' and code[5] == 0xc3:
                kind = struct.unpack_from('<I', code, 1)[0]
            elif code[:1] != b'\xc3':
                raise ValueError('Unknown switch body')
        variants = []
        # City, gate and port have dedicated object creation, not this switch.
        if index >= 3 and kind is not None:
            offset = 0x37a720 + kind * 8 if kind < 46 else 0x37a890 + (kind - 62) * 8
            if not (kind < 46 or 62 <= kind < 82):
                raise ValueError('Specialized object resolver requires investigation')
            for variant, model in enumerate(struct.unpack_from('<4H', exe, offset)):
                variants.append(dict(variant=variant, model_index=model,
                    near=dict(id=models[model], sha256=sha(archive.read(models[model]))),
                    far=dict(id=models[model + 1], sha256=sha(archive.read(models[model + 1])))))
        entries.append(dict(facility_id=index, source_name=name, name_offset=0xb82 + index * 191,
            switch_target=None if target is None else hex(target), object_kind=kind,
            resolver_scope='dedicated site creation' if index < 3 else 'no model mapping' if kind is None else 'generic object',
            model_variants=variants, runtime_binding=None, validation='definition/table crosscheck; runtime and visual acceptance pending'))
    archive.close()
    report = dict(schema=1, executable_sha256=sha(exe),
        definition_file='Media/scenario/Scenario.s11', definition_sha256=sha(scenario),
        definition_names=dict(offset=0xb82, record_stride=191, field_bytes=12, encoding='cp950'),
        resolver_va='0x59f7d0', resolver_sha256=sha(exe[0x19f7d0:0x19fa20]),
        state_update_va='0x5a03e0', state_update_sha256=sha(exe[0x1a03e0:0x1a06bb]),
        completed_variant='HP < min(maxHP/2,500): 1; otherwise 0 (non-city)',
        construction_variant='3 when incomplete, category !=3, ID !=24 and HP < maxHP*0.3; otherwise 2',
        construction_factor=struct.unpack_from('<f', exe, 0x44b2c8)[0], facilities=entries,
        limits=['Names are from the supplied active Media definition, not assumed retail labels',
                'IDs25..27 return default without assigning a drawable object',
                'OBJS kind14 cliff walls are distinct from facility9 stone walls (kind28)',
                'Runtime category field and animation/effect state remain unbound'])
    (ROOT / 'docs/pc-visual/facility-bindings.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(dict(labels=len(labels), drawable_facility_mappings=sum(bool(r['model_variants']) for r in entries), dam_id=24, stone_wall_object=28)))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation', type=Path)
    inspect(parser.parse_args().installation)
