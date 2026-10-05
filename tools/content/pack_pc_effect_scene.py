#!/usr/bin/env python3
"""Package exact SEFF rows and their eight original KSEF templates for a shared VM.

Readonly supplied archive/EXE. The small container preserves all source bytes;
it is an investigation input, not an APK/runtime event integration.
"""
import argparse
import json
from pathlib import Path
import struct
from pc_resources import Archive, effects, sha
from pc_effect_machine import VerifiedSourceExecutable

ROOT = Path(__file__).resolve().parents[2]


def pack(installation, output):
    verified = VerifiedSourceExecutable((installation/'san11pk.exe').read_bytes())
    archive = Archive(installation/'Media/san11pkres.bin')
    try:
        seff = archive.read(4792)
        placements = effects(seff)
        ids = sorted({p['effect'] for p in placements})
        if ids != [8, 9, 16, 17, 18, 19, 20, 23] or len(placements) != 126:
            raise ValueError('Reinspect the original map effect coverage')
        entries, body = [], bytearray()
        for effect in ids:
            resource = struct.unpack_from('<I', verified.data, 0x37692c+effect*12+4)[0]
            raw = archive.read(resource)
            if not raw.startswith(b'KSEF0131') or not 20 <= len(raw) <= 0xe0000:
                raise ValueError('Original KSEF boundary')
            body.extend(struct.pack('<3I', effect, resource, len(raw))+bytes.fromhex(sha(raw))+raw)
            entries.append(dict(effect_index=effect, resource_id=resource, bytes=len(raw), sha256=sha(raw)))
        header = b'PCFXSC01'+struct.pack('<4I', len(entries), len(placements), len(body), len(seff))+bytes.fromhex(sha(seff))+bytes(8)
        if len(header) != 64: raise AssertionError('Scene container header')
        packed = header+body+seff
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_bytes(packed)
        report = dict(schema=1, goal_complete=False, status='EXACT_SOURCE_SHARED_SCENE_INPUT_NOT_APK',
            source_archive=str(installation/'Media/san11pkres.bin'), source_executable_sha256=sha(verified.data),
            source_seff_resource_id=4792, source_seff_sha256=sha(seff), source_seff_bytes=len(seff),
            templates=entries, placement_count=len(placements), output=str(output.relative_to(ROOT)),
            bytes=len(packed), sha256=sha(packed), source_instruction_changes=0, runtime_effects_added=0,
            limits=['Original archive bytes; loose/MOD active resolver still unproven',
                'Source manager/shared arena/controller test input only; no game renderer/event binding'])
        output.with_suffix(output.suffix+'.json').write_text(json.dumps(report, indent=2)+'\n')
        print(json.dumps(report), flush=True)
    finally:
        archive.close()


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--output', type=Path, default=ROOT/'out/pc-visual/v148-native-shared-scene/source-scene.bin')
    a = p.parse_args()
    output = a.output.resolve()
    if not output.is_relative_to(ROOT/'out'): p.error('Investigation input remains in ignored project out')
    pack(a.installation.resolve(), output)
