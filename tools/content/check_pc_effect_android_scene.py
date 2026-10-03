#!/usr/bin/env python3
"""Execute the hash-pinned shared source scene on Android, outside the APK."""
import argparse
import gzip
import json
from pathlib import Path
import shlex
import subprocess
from check_pc_effect_shared_scene import ROOT, SCENE_SHA, KERNEL_SHA, CENTER, STEPS, compare
from pc_resources import sha


def check(a):
    build = json.loads((a.build_report/'build.json').read_text())
    if build['target'] != 'pc_effect_scene_probe': raise ValueError('Shared scene build required')
    files = {Path(r['path']).name: ROOT/r['path'] for r in build['files']}
    for row in build['files']:
        if sha((ROOT/row['path']).read_bytes()) != row['sha256']: raise ValueError('Build artifact changed')
    if sha(a.kernel.read_bytes()) != KERNEL_SHA or sha(a.scene.read_bytes()) != SCENE_SHA:
        raise ValueError('Pinned scene/source input')
    ref = json.loads((a.reference/'comparison.json').read_text())
    if ref['status'] != 'PASS_HOST_ORIGINAL_SHARED_MANAGER_FULL_STATE': raise ValueError('Independent host reference required')
    expected = json.loads((a.reference/'reference-updates.json').read_text())['frames']
    a.output.mkdir(parents=True, exist_ok=True)
    adb = [str(a.adb), '-s', a.serial]
    def run(*args): return subprocess.run(adb+list(args), capture_output=True, timeout=60, check=True).stdout
    abi = run('shell', 'getprop', 'ro.product.cpu.abilist').decode().strip()
    if build['abi'] not in abi.split(','): raise ValueError('Device ABI')
    sdk = int(run('shell', 'getprop', 'ro.build.version.sdk'))
    if sdk < build['min_api']: raise ValueError('Device API')
    remote = '/data/local/tmp/san11-pc-shared-effect-v148-'+build['abi']
    run('shell', 'mkdir', '-p', remote)
    for path, name in ((files['pc_effect_scene_probe'], 'pc_effect_scene_probe'),
                       (files['libunicorn.so'], 'libunicorn.so'),
                       (a.kernel, 'source-kernel.bin'), (a.scene, 'source-scene.bin')):
        run('push', str(path), remote+'/'+name)
        if run('exec-out', 'cat', remote+'/'+name) != path.read_bytes(): raise AssertionError('Installed bytes')
    run('shell', 'chmod', '700', remote+'/pc_effect_scene_probe')
    environment = ['env', 'LD_LIBRARY_PATH='+remote, 'PC_VM_PROBE_QUADS=1']
    if ref.get('source_queue'): environment.append('PC_VM_PROBE_SOURCE_QUEUE=1')
    if ref.get('materials'): environment.append('PC_VM_PROBE_MATERIALS=1')
    if ref.get('batch_quads'): environment.append('PC_VM_PROBE_BATCH_QUADS=1')
    command = ' '.join(map(shlex.quote, environment+[
        'PC_VM_PROBE_HEAP_PATH='+remote+'/heap.bin', 'PC_VM_PROBE_VISUAL_RNG_PATH='+remote+'/rng.bin',
        remote+'/pc_effect_scene_probe', remote+'/source-kernel.bin', remote+'/source-scene.bin', *CENTER, *STEPS]))
    try:
        execution = subprocess.run(adb+['shell', command], capture_output=True, timeout=45)
    except subprocess.TimeoutExpired as error:
        (a.output/'stdout-timeout.txt').write_bytes(error.stdout or b'')
        (a.output/'stderr-timeout.txt').write_bytes(error.stderr or b'')
        (a.output/'comparison-failed.json').write_text(json.dumps(dict(status='FAIL_ORIGINAL_45S_DEADLINE',
            goal_complete=False, runtime_effects_added=0))+'\n')
        raise
    (a.output/'updates.json').write_bytes(execution.stdout); (a.output/'stderr.txt').write_bytes(execution.stderr)
    if execution.returncode: raise RuntimeError(('Android shared source process', execution.returncode))
    actual = json.loads(execution.stdout)
    heap, rng = run('exec-out', 'cat', remote+'/heap.bin'), run('exec-out', 'cat', remote+'/rng.bin')
    (a.output/'heap.bin.gz').write_bytes(gzip.compress(heap, mtime=0)); (a.output/'rng.bin').write_bytes(rng)
    checks = compare(actual, expected, gzip.decompress((a.reference/'heap.bin.gz').read_bytes()),
                     (a.reference/'rng.bin').read_bytes(), heap, rng)
    report = dict(schema=1, goal_complete=False, status='PASS_ANDROID_ORIGINAL_SHARED_MANAGER_FULL_STATE',
        templates=8, placements=126, checks=checks, packets=sum(len(f['packets']) for f in expected), source_queue=ref.get('source_queue', False), materials=ref.get('materials', False), batch_quads=ref.get('batch_quads', False),
        abi=build['abi'], sdk=sdk, device_fingerprint=run('shell', 'getprop', 'ro.build.fingerprint').decode().strip(),
        build_report_sha256=sha((a.build_report/'build.json').read_bytes()),
        reference_report_sha256=sha((a.reference/'comparison.json').read_bytes()),
        source_kernel_sha256=KERNEL_SHA, scene_sha256=SCENE_SHA, heap_sha256=sha(heap), visual_rng_sha256=sha(rng),
        initialization_ms=actual['initialization_ms'], updates_ms=[f['native_update_ms'] for f in actual['frames']],
        draws_ms=[f['native_draw_ms'] for f in actual['frames']], runtime_effects_added=0,
        geometry_ms=[f.get('native_geometry_ms') for f in actual['frames']],
        limits=['Standalone shared VM only; no APK/renderer/events', 'Diagnostic camera/dt; no PC view/timing acceptance',
                'Memory GPU boundaries; no pixel rasterization; source_queue/materials flags identify source boundary checks', 'Emulator only; no ARM performance'])
    (a.output/'comparison.json').write_text(json.dumps(report, indent=2)+'\n'); print(json.dumps(report), flush=True)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--adb', type=Path, default=ROOT/'out/toolchain/android-sdk/platform-tools/adb')
    p.add_argument('--serial', default='emulator-5554')
    p.add_argument('--build-report', type=Path, default=ROOT/'out/pc-visual/v148-native-android-shared-scene-build')
    p.add_argument('--reference', type=Path, default=ROOT/'out/pc-visual/v148-native-shared-scene-checked')
    p.add_argument('--kernel', type=Path, default=ROOT/'out/pc-visual/v148-native-probe-checked/source-kernel.bin')
    p.add_argument('--scene', type=Path, default=ROOT/'out/pc-visual/v148-native-shared-scene/source-scene.bin')
    p.add_argument('--output', type=Path, default=ROOT/'out/pc-visual/v148-native-android-shared-scene-executed')
    a = p.parse_args()
    for field in ('adb', 'build_report', 'reference', 'kernel', 'scene', 'output'):
        setattr(a, field, getattr(a, field).resolve())
    if not a.output.is_relative_to(ROOT/'out'): p.error('Evidence remains within project out')
    check(a)
