#!/usr/bin/env python3
"""Compare native C camera-bound packets and full state to Python source oracle.

Original scene camera provider is bound before loading. Diagnostic origin,
identity view and.001 projection only; no Android rasterization or PC reference.
"""
import argparse
import gzip
import json
import os
from pathlib import Path
import subprocess
import struct
from pc_effect_machine import SourceEffectMachine, VerifiedSourceExecutable
from inspect_pc_effect_draw_packets import DrawPacketObserver
from pc_resources import Archive, sha, effects
from check_pc_effect_android_probe import KERNEL_SHA
from pc_effect_gpu_observer import FinalQuadObserver

ROOT = Path(__file__).resolve().parents[2]


def check(a):
    if sha(a.kernel.read_bytes()) != KERNEL_SHA: raise ValueError('Source kernel digest')
    verified = VerifiedSourceExecutable((a.installation/'san11pk.exe').read_bytes())
    a.output.mkdir(parents=True, exist_ok=True)
    archive = Archive(a.installation/'Media/san11pkres.bin')
    rows = []
    try:
        jobs = [(str(r), r, None, None) for r in a.resources]
        if a.seff_slots is not None:
            source_seff = archive.read(4792)
            placements = effects(source_seff)
            jobs = []
            for slot in a.seff_slots:
                if not 0 <= slot < len(placements): raise ValueError('SEFF slot boundary')
                placement = placements[slot]
                resource = struct.unpack_from('<I', verified.data, 0x37692c+placement['effect']*12+4)[0]
                original_row = source_seff[12+slot*18:12+(slot+1)*18]
                jobs.append((f'seff-{slot}-resource-{resource}', resource, placement, original_row))
        for sample, resource, placement, original_row in jobs:
            raw = archive.read(resource)
            input_path = a.output/f'original-{sample}.ksef'
            input_path.write_bytes(raw)
            heap = a.output/f'{sample}-heap.bin'
            rng = a.output/f'{sample}-rng.bin'
            env = dict(os.environ, PC_VM_PROBE_CAMERA='1', PC_VM_PROBE_HEAP_PATH=str(heap),
                       PC_VM_PROBE_VISUAL_RNG_PATH=str(rng))
            env.pop('PC_VM_PROBE_TRACE', None)
            if a.final_quads: env['PC_VM_PROBE_QUADS'] = '1'
            else: env.pop('PC_VM_PROBE_QUADS', None)
            env.pop('PC_VM_PROBE_SEFF_PATH', None)
            if placement is not None:
                row_path = a.output/f'original-{sample}.seff'
                row_path.write_bytes(original_row)
                env['PC_VM_PROBE_SEFF_PATH'] = str(row_path)
            run = subprocess.run([str(a.binary), str(a.kernel), str(input_path),
                                  '.0333333333', '.5', '1.0'], env=env, capture_output=True, timeout=30)
            (a.output/f'{sample}-stderr.txt').write_bytes(run.stderr)
            (a.output/f'{sample}-updates.json').write_bytes(run.stdout)
            if run.returncode: raise RuntimeError(('Native camera probe', resource, run.returncode))
            result = json.loads(run.stdout)
            machine = SourceEffectMachine(verified)
            center = (0, 0, 0)
            matrix_pointer = None
            if placement is not None:
                center = (placement['x'], placement['y'], placement['z'])
                machine.u.mem_write(machine.STOP+0x500, original_row[2:])
                matrix_pointer = machine.STOP+0x600
                machine.call(0x413a80, 0, matrix_pointer, machine.STOP+0x500)
            observer = DrawPacketObserver(machine, .001, center)
            final = FinalQuadObserver(machine, observer.camera) if a.final_quads else None
            count = checks = 0
            try:
                machine.load(raw, scene_camera_provider=observer.provider)
                if matrix_pointer is None: machine.start()
                else: machine.call(0x457880, machine.instance, 1, matrix_pointer)
                if len(result['frames']) != 3: raise AssertionError('Frame count')
                for dt, frame in zip((.0333333333, .5, 1.0), result['frames']):
                    machine.update(dt)
                    snapshot = machine.snapshot()
                    if sha(bytes.fromhex(frame['root_hex'])) != snapshot['root_state_sha256']:
                        raise AssertionError((resource, 'complete176-byte root'))
                    for field in ('source_emission_attempts', 'source_particle_allocations',
                                  'memory_allocation_count', 'memory_allocation_bytes'):
                        if frame[field] != snapshot[field]: raise AssertionError((resource, field))
                    checks += 5
                    expected = observer.draw()
                    actual = frame['packets']
                    if len(actual) != len(expected): raise AssertionError((resource, 'packet count'))
                    for p, q in zip(actual, expected):
                        for field in ('primitive', 'pointer', 'depth_bits', 'flags', 'prefix_bytes', 'raw_hex'):
                            if p[field] != q[field]: raise AssertionError((resource, 'packet', count, field))
                        count += 1; checks += 6
                        if final is not None and p['primitive'] in (0, 1, 2, 3):
                            geometry = final.evaluate(q)
                            for field in ('quad_vb_hex', 'quad_matrix_hex'):
                                if p[field] != geometry[field]: raise AssertionError((resource, 'final quad', count, field))
                                checks += 1
                expected_heap = bytes(machine.u.mem_read(machine.HEAP, 0x1000000))
                expected_rng = bytes(machine.u.mem_read(0x8a5b68, 2504))
                if heap.read_bytes() != expected_heap: raise AssertionError((resource, 'complete16MiB heap after draw'))
                if rng.read_bytes() != expected_rng: raise AssertionError((resource, 'visual RNG after draw'))
                checks += 2
            finally:
                if final is not None: final.close()
                observer.close(); machine.close()
            (a.output/f'{sample}-heap.bin.gz').write_bytes(gzip.compress(expected_heap, mtime=0))
            heap.unlink()
            row = dict(sample_id=sample, resource_id=resource, source_sha256=sha(raw), packets=count, checks=checks,
                       heap_sha256=sha(expected_heap), visual_rng_sha256=sha(expected_rng))
            if placement is not None: row.update(seff_placement=placement, seff_row_sha256=sha(original_row))
            rows.append(row); print(json.dumps(row), flush=True)
    finally:
        archive.close()
    report = dict(schema=1, goal_complete=False, status='PASS_HOST_CAMERA_BOUND_NATIVE_PACKETS_FULL_STATE_EXACT',
                  final_quads=a.final_quads,
                  seff_slots=a.seff_slots,
                  resources=rows, packets=sum(r['packets'] for r in rows), checks=sum(r['checks'] for r in rows),
                  source_kernel_sha256=KERNEL_SHA, binary_sha256=sha(a.binary.read_bytes()),
                  runtime_effects_added=0, limits=['Diagnostic camera only; no original PC view or timing',
                    'GPU calls record memory only; no texture/blend/queue sorting/rasterization acceptance',
                    'No APK/event integration or ARM performance'])
    (a.output/'comparison.json').write_text(json.dumps(report, indent=2)+'\n')


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--binary', type=Path, required=True)
    p.add_argument('--kernel', type=Path, default=ROOT/'out/pc-visual/v148-native-probe-checked/source-kernel.bin')
    p.add_argument('--output', type=Path, default=ROOT/'out/pc-visual/v148-native-camera-block-budget-checked')
    p.add_argument('--resources', type=int, nargs='+', default=[125, 194, 219, 148])
    p.add_argument('--final-quads', action='store_true', help='Compare exact final source0/1/2/3 quad vertices and matrices')
    p.add_argument('--seff-slots', type=int, nargs='+', help='Use exact original18-byte rows and413a80 matrices instead of default starts')
    a = p.parse_args()
    for field in ('installation', 'binary', 'kernel', 'output'):
        setattr(a, field, getattr(a, field).resolve())
    if any(not 125 <= r <= 368 for r in a.resources): p.error('Source resource boundary125..368')
    if not a.output.is_relative_to(ROOT/'out'): p.error('Evidence remains in project out')
    check(a)
