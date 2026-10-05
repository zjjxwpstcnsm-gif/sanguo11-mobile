#!/usr/bin/env python3
"""Independent original-manager shared-scene oracle, not APK acceptance.

All126 original placements share eight roots, one arena and one visual RNG.
The oracle independently reads source resources rather than trusting the pack.
GPU boundaries only observe memory. Diagnostic camera is not a PC reference.
"""
import argparse
import gzip
import json
import os
from pathlib import Path
import struct
import subprocess

from pc_effect_machine import SourceEffectMachine, VerifiedSourceExecutable
from inspect_pc_effect_draw_packets import DrawPacketObserver
from pc_effect_gpu_observer import FinalQuadObserver
from pc_resources import Archive, sha, effects
from check_pc_effect_android_probe import KERNEL_SHA

ROOT = Path(__file__).resolve().parents[2]
SEFF_SHA = '0703ce051a133a6abd01188c90a713e27ffc95e750da7b5fe2904fa498637b2c'
SCENE_SHA = '98d2626d2842f272474cc7a3c2437e29b946d3827c8b52089741155cbc79e4ae'
STEPS = ('.0333333333', '.5', '1.0')
CENTER = ('3543.083740234375', '83.23783874511719', '1967.943359375')


def reference(installation, source_queue=False, materials=False, batch_quads=False, steps=STEPS, camera_input=None):
    verified = VerifiedSourceExecutable((installation/'san11pk.exe').read_bytes())
    archive = Archive(installation/'Media/san11pkres.bin')
    machine = SourceEffectMachine(verified)
    observer = final = None
    try:
        seff = archive.read(4792)
        if sha(seff) != SEFF_SHA: raise ValueError('Source SEFF digest')
        placements = effects(seff)
        indices = sorted({row['effect'] for row in placements})
        if indices != [8, 9, 16, 17, 18, 19, 20, 23] or len(placements) != 126:
            raise ValueError('Original scene resource boundary')
        for address in (0x73bc80, 0x73bd20): machine.call(address, 0)
        manager, slots, data = machine.HEAP+0x8000, machine.HEAP+0x9000, machine.HEAP+0x10000
        u = machine.u
        if source_queue:
            machine.call(0x45a820, manager)
            if machine.call(0x45a620, manager, 0, 4096, 244) != 1:
                raise ValueError('Original manager initialization')
            slots = machine.uint(manager+0x2e4)
            u.mem_write(manager+0x2ec, struct.pack('<I', 244))
        elif machine.call(0x45b710, 0, 0, 0, 0x400000) != 1:
            raise ValueError('Original shared arena')
        observer = DrawPacketObserver(machine, .001, tuple(map(float, CENTER)), native_queue=source_queue)
        if camera_input is not None:
            if len(camera_input)!=51:raise ValueError('Original camera51 fields')
            u.mem_write(observer.camera,struct.pack('<4f',*camera_input[:3],1))
            for offset,values in ((0x40,camera_input[3:19]),(0x80,camera_input[35:51]),
                (0xc0,camera_input[19:35]),(0x140,camera_input[19:35]),(0x180,camera_input[35:51])):
                u.mem_write(observer.camera+offset,struct.pack('<16f',*values))
        if materials:
            from pc_resources import wftx_levels
            images = wftx_levels(archive.read(124))
            if len(images) != 33 or any(x['extra_mips'] != 0 for x in images):
                raise ValueError('Reinspect original common texture library')
        final = FinalQuadObserver(machine, observer.camera, materials=materials, preserve_ring=batch_quads)
        u.mem_write(manager+0x2dc, struct.pack('<2I', 0x795190, observer.camera))
        observer.queue = manager+0x220
        if not source_queue:
            u.mem_write(manager+0x2e4, struct.pack('<3I', slots, 244, 244))
            u.mem_write(observer.queue, struct.pack('<I', observer.table))
        for i, index in enumerate(indices):
            resource = struct.unpack_from('<I', verified.data, 0x37692c+index*12+4)[0]
            raw = archive.read(resource)
            root = machine.HEAP+i*0x100
            data = (data+15)&~15
            u.mem_write(data, raw)
            machine.call(0x457c90, root)
            machine.call(0x457bd0, root, 1, manager+0x2dc)
            if machine.call(0x457dd0, root, data+20, 0, 0) != data+len(raw):
                raise ValueError('Exact original shared loader boundary')
            machine.call(0x457900, root)
            machine.call(0x45a280, manager, index, root)
            data += len(raw)
        for i, row in enumerate(placements):
            root = machine.call(0x45a270, manager, row['effect'])
            if not root: raise ValueError('Missing original shared root')
            u.mem_write(machine.STOP+0x500, seff[14+i*18:30+i*18])
            machine.call(0x413a80, 0, machine.STOP+0x600, machine.STOP+0x500)
            machine.call(0x457880, root, 1, machine.STOP+0x600)
        frames = []
        for dt in steps:
            bits = struct.unpack('<I', struct.pack('<f', float(dt) if dt is not None else 0))[0]
            if dt is not None: machine.call(0x45a530, manager, observer.camera, bits)
            observer.packets = []
            machine.call(0x45a590, manager, 0, 0, observer.camera)
            packets = []
            for packet in observer.packets:
                if source_queue:
                    packet['raw_hex'] = bytes(u.mem_read(int(packet['pointer'], 16), packet['prefix_bytes'])).hex()
                if not materials: packet.update(final.evaluate(packet) if packet['primitive'] <= 3 else {})
                packets.append(packet)
            frame = dict(input_dt=struct.unpack('<f', struct.pack('<I', bits))[0],
                source_emission_attempts=machine.attempts, source_particle_allocations=machine.successes,
                memory_allocation_count=len(machine.allocations),
                memory_allocation_bytes=sum(a['bytes'] for a in machine.allocations), packets=packets)
            if source_queue:
                lookup = {int(p['pointer'], 16): i for i, p in enumerate(packets)}
                buckets, count = machine.uint(manager+0x224), machine.uint(manager+0x22c)
                if count != 4096: raise AssertionError('Original4096 queue bins')
                order, seen = [], set()
                for i in reversed(range(count)):
                    node = machine.uint(buckets+i*4)
                    while node:
                        if node not in lookup or node in seen: raise AssertionError('Original queue linked-list boundary')
                        seen.add(node); order.append(lookup[node]); node = machine.uint(node+0xc)
                if len(order) != machine.uint(manager+0x228): raise AssertionError('Original queue accepted count')
                frame['queue_order'] = order
                if materials:
                    accepted = set(order)
                    for index, packet in enumerate(packets): packet['queue_accepted'] = index in accepted
                    if batch_quads: u.mem_write(0x8a5b64, struct.pack('<I', 0))
                    for index in order: packets[index].update(final.evaluate(packets[index]))
            frames.append(frame)
        return frames, bytes(u.mem_read(machine.HEAP, 0x1000000)), bytes(u.mem_read(0x8a5b68, 2504))
    finally:
        if final is not None: final.close()
        if observer is not None: observer.close()
        machine.close(); archive.close()


def compare(actual, expected, heap, rng, actual_heap, actual_rng):
    if actual['templates'] != 8 or actual['placements'] != 126 or len(actual['frames']) != len(expected):
        raise AssertionError('Shared scene boundaries')
    checks = 3
    for i, (a, b) in enumerate(zip(actual['frames'], expected)):
        for field in ('input_dt', 'source_emission_attempts', 'source_particle_allocations',
                      'memory_allocation_count', 'memory_allocation_bytes'):
            equal = struct.pack('<f', a[field]) == struct.pack('<f', b[field]) if field == 'input_dt' else a[field] == b[field]
            if not equal: raise AssertionError((i, field, a[field], b[field]))
            checks += 1
        if len(a['packets']) != len(b['packets']): raise AssertionError((i, 'packet count'))
        checks += 1
        if 'queue_order' in b:
            if a.get('queue_order') != b['queue_order']: raise AssertionError((i, 'native queue order'))
            checks += 1
        for j, (p, q) in enumerate(zip(a['packets'], b['packets'])):
            fields = ['primitive', 'pointer', 'depth_bits', 'flags', 'prefix_bytes', 'raw_hex']
            if 'queue_accepted' in q: fields += ['queue_accepted']
            if p['primitive'] <= 3 and q.get('queue_accepted', True): fields += ['quad_vb_hex', 'quad_matrix_hex']
            if 'texture_index' in q: fields += ['texture_index', 'blend_operation', 'blend_source', 'blend_destination']
            if 'vertex_offset' in q: fields += ['vertex_offset', 'lock_flags', 'draw_first']
            for field in fields:
                if p[field] != q[field]: raise AssertionError((i, j, field))
                checks += 1
    if actual_heap != heap: raise AssertionError('Complete shared16MiB heap')
    if actual_rng != rng: raise AssertionError('Complete shared visual RNG')
    return checks+2


def check(a):
    if sha(a.kernel.read_bytes()) != KERNEL_SHA or sha(a.scene.read_bytes()) != SCENE_SHA:
        raise ValueError('Pinned source input digests')
    a.output.mkdir(parents=True, exist_ok=True)
    heap_path, rng_path = a.output/'heap.bin', a.output/'rng.bin'
    env = dict(os.environ, PC_VM_PROBE_QUADS='1', PC_VM_PROBE_HEAP_PATH=str(heap_path),
               PC_VM_PROBE_VISUAL_RNG_PATH=str(rng_path))
    env.pop('PC_VM_PROBE_BLOCK_BUDGET', None); env.pop('PC_VM_PROBE_TRACE', None)
    if a.source_queue: env['PC_VM_PROBE_SOURCE_QUEUE'] = '1'
    else: env.pop('PC_VM_PROBE_SOURCE_QUEUE', None)
    if a.materials: env['PC_VM_PROBE_MATERIALS'] = '1'
    else: env.pop('PC_VM_PROBE_MATERIALS', None)
    if a.batch_quads: env['PC_VM_PROBE_BATCH_QUADS'] = '1'
    else: env.pop('PC_VM_PROBE_BATCH_QUADS', None)
    result = subprocess.run([str(a.binary), str(a.kernel), str(a.scene), *CENTER, *STEPS],
                            env=env, capture_output=True, timeout=45)
    (a.output/'updates.json').write_bytes(result.stdout)
    (a.output/'stderr.txt').write_bytes(result.stderr)
    if result.returncode: raise RuntimeError(('Shared scene process', result.returncode))
    actual = json.loads(result.stdout)
    expected, heap, rng = reference(a.installation, a.source_queue, a.materials, a.batch_quads)
    checks = compare(actual, expected, heap, rng, heap_path.read_bytes(), rng_path.read_bytes())
    (a.output/'reference-updates.json').write_text(json.dumps(dict(templates=8, placements=126, frames=expected))+'\n')
    (a.output/'heap.bin.gz').write_bytes(gzip.compress(heap, mtime=0)); heap_path.unlink()
    report = dict(schema=1, goal_complete=False, status='PASS_HOST_ORIGINAL_SHARED_MANAGER_FULL_STATE',
        checks=checks, packets=sum(len(f['packets']) for f in expected), templates=8, placements=126, source_queue=a.source_queue, materials=a.materials, batch_quads=a.batch_quads,
        source_kernel_sha256=KERNEL_SHA, scene_sha256=SCENE_SHA, binary_sha256=sha(a.binary.read_bytes()),
        heap_sha256=sha(heap), visual_rng_sha256=sha(rng), runtime_effects_added=0,
        limits=['Diagnostic manager/camera; PC runtime camera, resolver and wall-clock not verified',
                'Memory GPU boundaries only; no pixel rasterization; source_queue/materials flags identify boundary checks',
                'Standalone source execution; no APK/game events or ARM performance'])
    (a.output/'comparison.json').write_text(json.dumps(report, indent=2)+'\n')
    print(json.dumps(report), flush=True)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--binary', type=Path, required=True)
    p.add_argument('--source-queue', action='store_true', help='Execute original manager constructor and4096-bin draw queue')
    p.add_argument('--materials', action='store_true', help='Execute original texture/blend functions through442c00/442ad0')
    p.add_argument('--batch-quads', action='store_true', help='C-only bounded trampoline; compare to independent sequential source calls')
    p.add_argument('--kernel', type=Path, default=ROOT/'out/pc-visual/v148-native-probe-checked/source-kernel.bin')
    p.add_argument('--scene', type=Path, default=ROOT/'out/pc-visual/v148-native-shared-scene/source-scene.bin')
    p.add_argument('--output', type=Path, default=ROOT/'out/pc-visual/v148-native-shared-scene-checked')
    a = p.parse_args()
    if a.materials and not a.source_queue: p.error('Material evaluation requires original queue order')
    if a.batch_quads and not a.materials: p.error('Batch dispatch requires material evaluation')
    for field in ('installation', 'binary', 'kernel', 'scene', 'output'):
        setattr(a, field, getattr(a, field).resolve())
    if not a.output.is_relative_to(ROOT/'out'): p.error('Evidence must remain in project out')
    check(a)
