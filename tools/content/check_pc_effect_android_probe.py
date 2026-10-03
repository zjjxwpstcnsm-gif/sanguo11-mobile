#!/usr/bin/env python3
"""Run the bounded original evaluator on Android and compare complete state.

Standalone CLI in a task-specific /data/local/tmp directory. Does not touch
the game package, user save, gameplay RNG or renderer. This is not acceptance
of event-triggered effects, PC visual timing or ARM performance.
"""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import shlex
import subprocess

ROOT = Path(__file__).resolve().parents[2]
KERNEL_SHA = '2668d82c3f835b4dc043a3d28f1902b472627570c1c25ba081425c950f4f8207'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def check(a):
    build = json.loads((a.build_report/'build.json').read_text())
    if sha(a.kernel.read_bytes()) != KERNEL_SHA:
        raise ValueError('Exact source kernel digest required before execution')
    files = {Path(r['path']).name: ROOT/r['path'] for r in build['files']}
    for r in build['files']:
        if sha((ROOT/r['path']).read_bytes()) != r['sha256']:
            raise ValueError('Build artifact changed: '+r['path'])
    a.output.mkdir(parents=True, exist_ok=True)
    adb = [str(a.adb)] + (['-s', a.serial] if a.serial else [])

    def run(*args, timeout=60):
        return subprocess.run(adb+list(args), capture_output=True, timeout=timeout, check=True)

    abi = run('shell', 'getprop', 'ro.product.cpu.abilist').stdout.decode().strip()
    if build['abi'] not in abi.split(','):
        raise ValueError('Device ABI differs from the cross build')
    sdk = int(run('shell', 'getprop', 'ro.build.version.sdk').stdout)
    if sdk < build['min_api']: raise ValueError('Device API below probe minimum')
    remote = '/data/local/tmp/san11-pc-effect-v148-'+build['abi']
    run('shell', 'mkdir', '-p', remote)
    inputs = [(files['pc_effect_vm_probe'], 'pc_effect_vm_probe'), (files['libunicorn.so'], 'libunicorn.so'),
              (a.kernel, 'source-kernel.bin')]
    resources = json.loads((a.reference/'comparison.json').read_text())['resources']
    for row in resources:
        sample = row.get('sample_id', str(row['resource_id']))
        path = a.reference/('original-'+sample+'.ksef')
        if sha(path.read_bytes()) != row['source_sha256']: raise ValueError('Original KSEF changed')
        inputs.append((path, path.name))
        if 'seff_row_sha256' in row:
            path = a.reference/('original-'+sample+'.seff')
            if sha(path.read_bytes()) != row['seff_row_sha256']: raise ValueError('Original SEFF row changed')
            inputs.append((path, path.name))
    for path, name in inputs:
        run('push', str(path), remote+'/'+name)
        installed = run('exec-out', 'cat', remote+'/'+name).stdout
        if installed != path.read_bytes(): raise AssertionError('Installed probe bytes differ: '+path.name)
    run('shell', 'chmod', '700', remote+'/pc_effect_vm_probe')
    results = []
    for row in resources:
        resource = row['resource_id']
        sample = row.get('sample_id', str(resource))
        heap_remote = remote+'/'+sample+'-heap.bin'
        rng_remote = remote+'/'+sample+'-rng.bin'
        environment = ['env', 'LD_LIBRARY_PATH='+remote, 'PC_VM_PROBE_HEAP_PATH='+heap_remote,
                       'PC_VM_PROBE_VISUAL_RNG_PATH='+rng_remote]
        if a.camera_bound: environment.append('PC_VM_PROBE_CAMERA=1')
        if a.final_quads: environment.append('PC_VM_PROBE_QUADS=1')
        if 'seff_row_sha256' in row: environment.append('PC_VM_PROBE_SEFF_PATH='+remote+'/original-'+sample+'.seff')
        command = ' '.join(shlex.quote(v) for v in environment + [
            remote+'/pc_effect_vm_probe', remote+'/source-kernel.bin',
            remote+'/original-'+sample+'.ksef', '.0333333333', '.5', '1.0'])
        try:
            executed = subprocess.run(adb+['shell', command], capture_output=True, timeout=45)
        except subprocess.TimeoutExpired as error:
            (a.output/(sample+'-stdout-timeout.txt')).write_bytes(error.stdout or b'')
            (a.output/(sample+'-stderr-timeout.txt')).write_bytes(error.stderr or b'')
            failure = dict(schema=1, goal_complete=False, status='FAIL_ANDROID_PROBE_PROCESS_45S',
                resource_id=resource, sample_id=sample, completed_resources=results, abi=build['abi'], sdk=sdk,
                runtime_effects_added=0, failure='Original45s diagnostic process limit exceeded',
                limits=['Completed resource state checks do not establish whole probe pass',
                    'No APK/render/event/PC/ARM performance acceptance'])
            (a.output/'comparison-failed.json').write_text(json.dumps(failure, indent=2)+'\n')
            raise
        (a.output/(sample+'-stdout.txt')).write_bytes(executed.stdout)
        (a.output/(sample+'-stderr.txt')).write_bytes(executed.stderr)
        if executed.returncode: raise RuntimeError('Android source execution failed '+str(resource))
        result = json.loads(executed.stdout)
        expected = json.loads((a.reference/(sample+('-updates.json' if a.camera_bound else '-fragment-v2-updates.json'))).read_text())
        if len(result['frames']) != len(expected['frames']): raise AssertionError('Frame count')
        checks = 0
        for actual, reference in zip(result['frames'], expected['frames']):
            for field in ('root_hex', 'source_emission_attempts', 'source_particle_allocations',
                          'memory_allocation_count', 'memory_allocation_bytes'):
                if actual[field] != reference[field]: raise AssertionError((resource, field))
                checks += 1
            if a.camera_bound:
                if len(actual['packets']) != len(reference['packets']): raise AssertionError((resource, 'packet count'))
                for p, q in zip(actual['packets'], reference['packets']):
                    fields = ('primitive', 'pointer', 'depth_bits', 'flags', 'prefix_bytes', 'raw_hex')
                    if a.final_quads and p['primitive'] in (0, 1, 2, 3): fields += ('quad_vb_hex', 'quad_matrix_hex')
                    for field in fields:
                        if p[field] != q[field]: raise AssertionError((resource, 'packet', field))
                        checks += 1
        heap = run('exec-out', 'cat', heap_remote).stdout
        rng = run('exec-out', 'cat', rng_remote).stdout
        (a.output/(sample+'-heap.bin.gz')).write_bytes(gzip.compress(heap, mtime=0))
        (a.output/(sample+'-visual-rng.bin')).write_bytes(rng)
        heap_name = sample+('-heap.bin.gz' if a.camera_bound else '-native-heap.bin.gz')
        if heap != gzip.decompress((a.reference/heap_name).read_bytes()):
            raise AssertionError((resource, 'complete16MiB heap'))
        rng_name = sample+('-rng.bin' if a.camera_bound else '-native-visual-rng.bin')
        if rng != (a.reference/rng_name).read_bytes():
            raise AssertionError((resource, 'complete2504-byte visual RNG'))
        checks += 2
        item = dict(resource_id=resource, sample_id=sample, checks=checks, heap_bytes=len(heap), heap_sha256=sha(heap),
            visual_rng_sha256=sha(rng), initialization_ms=result['initialization_ms'],
            updates_ms=[f['native_update_ms'] for f in result['frames']])
        results.append(item)
        print(json.dumps(item), flush=True)
    status = 'PASS_ANDROID_CAMERA_BOUND_PACKETS_HEAP_VISUAL_RNG_EXACT' if a.camera_bound else 'PASS_ANDROID_STANDALONE_HEAP_VISUAL_RNG_EXACT'
    report = dict(schema=1, goal_complete=False, status=status, camera_bound=a.camera_bound, final_quads=a.final_quads,
        abi=build['abi'], device_abis=abi, sdk=sdk,
        device_fingerprint=run('shell', 'getprop', 'ro.build.fingerprint').stdout.decode().strip(),
        checks=sum(r['checks'] for r in results), resources=results, kernel_sha256=KERNEL_SHA,
        build_report_sha256=sha((a.build_report/'build.json').read_bytes()), runtime_effects_added=0,
        remote_probe_directory=remote, limits=[
            'Standalone process only; no JNI/APK/renderer/game event integration',
            'Full state matches host source oracle, not captured PC images or timing',
            'Diagnostic three time inputs; no sustained frame rate claim',
            'Emulator evidence does not establish ARM device performance'])
    (a.output/'comparison.json').write_text(json.dumps(report, indent=2)+'\n')
    print(json.dumps(dict(status=report['status'], checks=report['checks'])), flush=True)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--adb', type=Path, default=ROOT/'out/toolchain/android-sdk/platform-tools/adb')
    p.add_argument('--serial')
    p.add_argument('--build-report', type=Path, default=ROOT/'out/pc-visual/v148-native-android-x86_64')
    p.add_argument('--reference', type=Path, default=ROOT/'out/pc-visual/v148-native-probe-checked')
    p.add_argument('--kernel', type=Path, default=ROOT/'out/pc-visual/v148-native-probe-checked/source-kernel.bin')
    p.add_argument('--camera-bound', action='store_true', help='Compare camera-bound packets to the independently checked host draw report')
    p.add_argument('--final-quads', action='store_true', help='Compare source0/1/2/3 final96-byte vertices and64-byte matrices')
    p.add_argument('--output', type=Path, default=ROOT/'out/pc-visual/v148-native-android-executed')
    a = p.parse_args()
    if a.final_quads and not a.camera_bound: p.error('Final quads require camera-bound reference')
    for field in ('adb', 'build_report', 'reference', 'kernel', 'output'):
        setattr(a, field, getattr(a, field).resolve())
    if not a.output.is_relative_to(ROOT/'out'): p.error('Evidence output must remain in project out')
    check(a)
