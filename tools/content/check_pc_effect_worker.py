#!/usr/bin/env python3
"""Verify persistent visual-child protocol against independent source execution.

Three updates and two paused redraws, complete heap/RNG and source queue order.
This host check does not establish APK installation, rendering or PC parity.
"""
import argparse
import gzip
import json
import os
from pathlib import Path
import struct
import subprocess
import shlex
import math
from check_pc_effect_shared_scene import ROOT, CENTER, STEPS, KERNEL_SHA, SCENE_SHA, reference
from pc_resources import sha


def camera():
    center = list(map(float, CENTER))
    identity = [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]
    view = identity[:]; view[12:15] = [-v for v in center]
    # Retain the original independent diagnostic fixture byte-for-byte. This
    # deliberately synthetic+c0 matrix is not a camera built by441ab0/441b80;
    # their actual+c0 is view*projection (v150 source camera check).
    diagnostic_c0 = identity[:]; diagnostic_c0[12:15] = center
    projection = [.001, 0, 0, 0, 0, .001, 0, 0, 0, 0, .001, 0, 0, 0, .5, 1]
    return center+view+diagnostic_c0+projection


def commands(values=None):
    values=camera() if values is None else values
    return struct.pack('<3I51f', 0, 0, 0, *values)+b''.join(struct.pack('<3I51f', 1 if dt is not None else 2, i+1,
        struct.unpack('<I', struct.pack('<f', float(dt) if dt is not None else 0))[0],
        *values) for i, dt in enumerate((*STEPS, None, None))) + struct.pack('<3I51f', 3, 6, 0, *values)


def native_camera(installation,distance,perspective=False):
    from pc_effect_machine import SourceEffectMachine
    m=SourceEffectMachine((installation/'san11pk.exe').read_bytes())
    try:
        for address in (0x73bc80,0x73bd20):m.call(address,0)
        c,up=m.HEAP+0x2000,m.HEAP+0x3000
        tilt,yaw=math.radians(55),math.radians(35);focus=(float(CENTER[0]),0,float(CENTER[2]))
        eye=(focus[0]+math.sin(yaw)*distance*math.cos(tilt),distance*math.sin(tilt),focus[2]+math.cos(yaw)*distance*math.cos(tilt))
        m.u.mem_write(c,struct.pack('<8f',*eye,1,*focus,1))
        m.u.mem_write(c+0x20,struct.pack('<5fI2f',0,2*math.atan(160/distance) if perspective else 0,2,20000,1080/1232,0 if perspective else 1,320*1080/1232,320))
        m.u.mem_write(up,struct.pack('<4f',0,1,0,0));m.call(0x441ab0,0,c,up);m.call(0x441b80,0,c)
        return list(struct.unpack('<51f',bytes(m.u.mem_read(c,12))+bytes(m.u.mem_read(c+0x40,64))
            +bytes(m.u.mem_read(c+0xc0,64))+bytes(m.u.mem_read(c+0x180,64))))
    finally:m.close()


def decode(data):
    if data[:16] != b'PCFXRDY1'+struct.pack('<2I', 8, 126): raise ValueError('Worker readiness header')
    frames = []; offset = 16
    while offset < len(data):
        if data[offset:offset+8] != b'PCFXFR01' or len(data)-offset < 32: raise ValueError('Worker frame header')
        serial, count, elapsed, update_ms, draw_ms, geometry_ms = struct.unpack_from('<2I4f', data, offset+8)
        if count > 32768 or len(data)-offset-32 < count*184: raise ValueError('Worker frame bounded extent')
        offset += 32; packets = []
        for _ in range(count):
            primitive, texture, operation, source, destination, depth = struct.unpack_from('<6I', data, offset)
            if primitive > 3 or texture >= 33: raise ValueError('Supported source worker packet')
            packets.append(dict(primitive=primitive, texture_index=texture, blend_operation=operation,
                blend_source=source, blend_destination=destination, depth_bits=hex(depth),
                quad_vb_hex=data[offset+24:offset+120].hex(), quad_matrix_hex=data[offset+120:offset+184].hex()))
            offset += 184
        frames.append(dict(serial=serial, elapsed=elapsed, update_ms=update_ms, draw_ms=draw_ms,
                           geometry_ms=geometry_ms, packets=packets))
    return frames


def check(a):
    if sha(a.kernel.read_bytes()) != KERNEL_SHA or sha(a.scene.read_bytes()) != SCENE_SHA:
        raise ValueError('Pinned source input')
    a.output.mkdir(parents=True, exist_ok=True)
    heap_path, rng_path = a.output/'heap.bin', a.output/'rng.bin'
    values=native_camera(a.installation,a.native_camera_distance,a.native_perspective) if a.native_camera_distance is not None else None
    center=tuple(str(v)for v in values[:3]) if values is not None else CENTER
    inputs = commands(values); (a.output/'commands.bin').write_bytes(inputs)
    env = dict(os.environ, PC_VM_PROBE_HEAP_PATH=str(heap_path), PC_VM_PROBE_VISUAL_RNG_PATH=str(rng_path))
    env.pop('PC_VM_PROBE_TRACE', None); env.pop('PC_VM_PROBE_BLOCK_BUDGET', None)
    if a.block_budget:env['PC_VM_PROBE_BLOCK_BUDGET']='1'
    android = None
    if a.android_build:
        build = json.loads((a.android_build/'build.json').read_text())
        if build['target'] != 'pc_effect_worker': raise ValueError('Persistent source worker build required')
        files = {Path(row['path']).name:ROOT/row['path'] for row in build['files']}
        for row in build['files']:
            if sha((ROOT/row['path']).read_bytes()) != row['sha256']: raise ValueError('Archived worker changed')
        adb = [str(a.adb), '-s', a.serial]
        def run(*args): return subprocess.run(adb+list(args), capture_output=True, timeout=60, check=True).stdout
        abi = run('shell', 'getprop', 'ro.product.cpu.abilist').decode().strip()
        if build['abi'] not in abi.split(','): raise ValueError('Device ABI')
        # Independent checks may use different camera fixtures. Their dump
        # files must never share a directory or overwrite another run's state.
        remote = '/data/local/tmp/san11-pc-persistent-'+build['abi']+'-'+sha(str(a.output).encode())[:16]
        run('shell', 'mkdir', '-p', remote)
        for path, name in ((files['libpc_effect_worker.so'], 'libpc_effect_worker.so'), (files['libunicorn.so'], 'libunicorn.so'),
                           (a.kernel, 'source-kernel.bin'), (a.scene, 'source-scene.bin')):
            run('push', str(path), remote+'/'+name)
            if run('exec-out', 'cat', remote+'/'+name) != path.read_bytes(): raise AssertionError('Installed worker bytes')
        run('shell', 'chmod', '700', remote+'/libpc_effect_worker.so')
        command = ' '.join(map(shlex.quote, ['env', 'LD_LIBRARY_PATH='+remote,*(['PC_VM_PROBE_BLOCK_BUDGET=1']if a.block_budget else []),
            'PC_VM_PROBE_HEAP_PATH='+remote+'/heap.bin', 'PC_VM_PROBE_VISUAL_RNG_PATH='+remote+'/rng.bin',
            remote+'/libpc_effect_worker.so', remote+'/source-kernel.bin', remote+'/source-scene.bin', *center, '--stream']))
        binary_command = adb+['shell', '-T', command]
        android = dict(abi=build['abi'], sdk=int(run('shell', 'getprop', 'ro.build.version.sdk')),
            fingerprint=run('shell', 'getprop', 'ro.build.fingerprint').decode().strip(),
            build_report_sha256=sha((a.android_build/'build.json').read_bytes()))
        binary_sha = sha(files['libpc_effect_worker.so'].read_bytes())
    else:
        binary_command = [str(a.binary), str(a.kernel), str(a.scene), *center, '--stream']
        binary_sha = sha(a.binary.read_bytes())
    try:
        result = subprocess.run(binary_command, input=inputs, env=env, capture_output=True, timeout=45)
    except subprocess.TimeoutExpired as failure:
        (a.output/'stdout-timeout.bin').write_bytes(failure.stdout or b'')
        (a.output/'stderr-timeout.txt').write_bytes(failure.stderr or b'')
        (a.output/'comparison-failed.json').write_text(json.dumps(dict(goal_complete=False,
            status='FAIL_ORIGINAL_45S_WORKER_DEADLINE', runtime_effects_added=0))+'\n')
        raise
    (a.output/'stdout.bin').write_bytes(result.stdout); (a.output/'stderr.txt').write_bytes(result.stderr)
    if result.returncode: raise RuntimeError(('Worker exit', result.returncode))
    if android:
        heap_path.write_bytes(run('exec-out', 'cat', remote+'/heap.bin'))
        rng_path.write_bytes(run('exec-out', 'cat', remote+'/rng.bin'))
    frames = decode(result.stdout)
    expected, heap, rng = reference(a.installation, True, True, True, (*STEPS, None, None),values)
    if len(frames) != 5: raise AssertionError('Persistent update/paused frame count')
    checks = 1; elapsed = 0
    for i, (frame, ref) in enumerate(zip(frames, expected)):
        if frame['serial'] != i+1: raise AssertionError('Worker serial order')
        elapsed = struct.unpack('<f', struct.pack('<f', elapsed+ref['input_dt']))[0]
        if frame['elapsed'] != elapsed: raise AssertionError('Source clock/pause')
        checks += 2
        packets = [ref['packets'][index] for index in ref['queue_order']]
        if len(frame['packets']) != len(packets): raise AssertionError('Accepted source queue packet count')
        checks += 1
        for p, q in zip(frame['packets'], packets):
            for field in p:
                if p[field] != q[field]: raise AssertionError((i, field))
                checks += 1
    if heap_path.read_bytes() != heap or rng_path.read_bytes() != rng:
        raise AssertionError('Persistent complete heap/RNG after paused redraw')
    checks += 2
    (a.output/'heap.bin.gz').write_bytes(gzip.compress(heap, mtime=0)); heap_path.unlink()
    (a.output/'frames.json').write_text(json.dumps(frames)+'\n')
    report = dict(schema=1, goal_complete=False, status='PASS_ANDROID_PERSISTENT_SOURCE_WORKER' if android else 'PASS_HOST_PERSISTENT_SOURCE_WORKER', checks=checks,
        frames=5, updates=3, paused_redraws=2, packets=sum(len(f['packets']) for f in frames),
        binary_sha256=binary_sha, kernel_sha256=KERNEL_SHA, scene_sha256=SCENE_SHA, android=android,
        heap_sha256=sha(heap), visual_rng_sha256=sha(rng), runtime_effects_added=0,
        camera_input='original441ab0/441b80' if values is not None else 'legacy explicit diagnostic',
        source_camera_distance=a.native_camera_distance,
        native_perspective=a.native_perspective,
        budget='conservative5M-executed-block-bytes' if a.block_budget else 'Unicorn5M-instructions',
        limits=['Standalone child protocol only; no APK or pixel output', 'Diagnostic camera and dt; no PC/ARM acceptance'])
    (a.output/'comparison.json').write_text(json.dumps(report, indent=2)+'\n'); print(json.dumps(report), flush=True)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation', type=Path)
    p.add_argument('--binary', type=Path)
    p.add_argument('--android-build', type=Path)
    p.add_argument('--adb', type=Path, default=ROOT/'out/toolchain/android-sdk/platform-tools/adb')
    p.add_argument('--serial', default='emulator-5554')
    p.add_argument('--native-camera-distance',type=float,help='Use original RH camera construction, source units; keeps original diagnostic fixture by default')
    p.add_argument('--native-perspective',action='store_true',help='Use original441b80 normalized perspective, required by map depth queue')
    p.add_argument('--block-budget',action='store_true',help='Profile existing conservative5M block-byte cap, same original5s watchdog')
    p.add_argument('--kernel', type=Path, default=ROOT/'out/pc-visual/v148-native-probe-checked/source-kernel.bin')
    p.add_argument('--scene', type=Path, default=ROOT/'out/pc-visual/v148-native-shared-scene/source-scene.bin')
    p.add_argument('--output', type=Path, default=ROOT/'out/pc-visual/v148-native-worker-checked')
    a = p.parse_args()
    if bool(a.binary) == bool(a.android_build): p.error('Choose one host binary or Android build')
    if a.native_camera_distance is not None and (not math.isfinite(a.native_camera_distance)or a.native_camera_distance<=0):p.error('Positive finite source camera distance')
    if a.native_perspective and a.native_camera_distance is None:p.error('Original perspective requires source camera distance')
    for field in ('installation', 'binary', 'kernel', 'scene', 'output', 'android_build', 'adb'):
        if getattr(a, field) is not None: setattr(a, field, getattr(a, field).resolve())
    if not a.output.is_relative_to(ROOT/'out'): p.error('Evidence within project out')
    check(a)
