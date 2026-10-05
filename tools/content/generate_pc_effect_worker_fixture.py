#!/usr/bin/env python3
"""Generate installed transport fixtures from independent original source calls."""
import argparse
import gzip
import json
from pathlib import Path
import struct
from check_pc_effect_shared_scene import ROOT, STEPS, reference
from check_pc_effect_worker import camera, commands
from pc_resources import sha


def generate(installation):
    frames, heap, rng = reference(installation, True, True, True, (*STEPS, None, None))
    data = bytearray(b'PCFXRDY1'+struct.pack('<2I', 8, 126)); elapsed = 0
    for i, frame in enumerate(frames):
        packets = [frame['packets'][j] for j in frame['queue_order']]
        elapsed = struct.unpack('<f', struct.pack('<f', elapsed+frame['input_dt']))[0]
        data.extend(b'PCFXFR01'+struct.pack('<2I4f', i+1, len(packets), elapsed, 0, 0, 0))
        for p in packets:
            data.extend(struct.pack('<6I', p['primitive'], p['texture_index'], p['blend_operation'],
                p['blend_source'], p['blend_destination'], int(p['depth_bits'], 16)))
            data.extend(bytes.fromhex(p['quad_vb_hex'])+bytes.fromhex(p['quad_matrix_hex']))
    output = ROOT/'app/src/androidTest/assets/pc-effects'
    output.mkdir(parents=True, exist_ok=True)
    (output/'worker-reference.bin.gz').write_bytes(gzip.compress(bytes(data), mtime=0))
    (output/'worker-camera.bin').write_bytes(struct.pack('<51f', *camera()))
    (output/'worker-reference.json').write_text(json.dumps(dict(schema=1, goal_complete=False,
        status='INDEPENDENT_SOURCE_MEMORY_FIXTURE_NOT_PC_IMAGE', frames=5, updates=3, paused_redraws=2,
        source_heap_sha256=sha(heap), source_visual_rng_sha256=sha(rng), output_sha256=sha(bytes(data)),
        source_calls='45a820/45a620/457dd0/457880/45a530/45a590/458650/442c00/442ad0',
        camera='Explicit diagnostic source51-float camera; not captured PC camera',
        runtime_effects_added=0), indent=2)+'\n')
    print(json.dumps(dict(fixture_bytes=len(data), sha256=sha(bytes(data)))), flush=True)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__);p.add_argument('installation', type=Path)
    generate(p.parse_args().installation.resolve())
