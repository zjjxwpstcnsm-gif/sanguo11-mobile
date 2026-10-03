#!/usr/bin/env python3
"""Reproduce the Android source-evaluator feasibility build in ignored out.

No downloads, global installs, PC writes, APK edits or game event binding.
Dependencies must be supplied at the recorded versions/commits.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import shutil

ROOT = Path(__file__).resolve().parents[2]
UNICORN_COMMIT = '8028ec436f2d9376525352dd38ed9ed6b9f6be10'
PKGCONF_COMMIT = '0c9e506b64124d8727b68d8af0ed73739e66e2ba'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build(a):
    if subprocess.check_output(['git', '-C', str(a.unicorn), 'rev-parse', 'HEAD'], text=True).strip() != UNICORN_COMMIT:
        raise ValueError('Require the recorded unmodified Unicorn2.1.4 source')
    if subprocess.check_output(['git', '-C', str(a.pkgconf.parent), 'rev-parse', 'HEAD'], text=True).strip() != PKGCONF_COMMIT:
        raise ValueError('Require the recorded pkgconf3.0.7 source')
    if subprocess.check_output(['git', '-C', str(a.unicorn), 'diff', '--', 'CMakeLists.txt', 'qemu'], text=True):
        raise ValueError('Unrecorded Unicorn source changes')
    if 'Pkg.Revision = 27.3.13750724' not in (a.ndk/'source.properties').read_text():
        raise ValueError('Require recorded NDKr27d')
    if subprocess.check_output([str(a.pkgconf), '--version'], text=True).strip() != '3.0.7':
        raise ValueError('Require working pkgconf3.0.7')
    a.output.mkdir(parents=True, exist_ok=True)
    a.build_dir.mkdir(parents=True, exist_ok=True)
    empty = ROOT/'out/toolchain/pc-empty-pkgconfig'
    empty.mkdir(parents=True, exist_ok=True)
    # No host .pc files may contaminate Android feature discovery. Unicorn
    # embeds glib_compat and uses Android libc/pthread, not a host GLib package.
    env = dict(os.environ, PKG_CONFIG=str(a.pkgconf), PKG_CONFIG_LIBDIR=str(empty), PKG_CONFIG_PATH='')
    # Build the archived inputs, not live files that can change during a long
    # compile. Both executable sources are required by the shared CMake project.
    sources = []
    for name in ('pc_effect_vm_probe.c', 'pc_effect_scene_probe.c', 'CMakeLists.txt'):
        source = ROOT/'tools/content/native'/name
        archived = a.output/'sources'/name
        archived.parent.mkdir(parents=True, exist_ok=True)
        archived.write_bytes(source.read_bytes())
        sources.append(archived)
    commands = [
        [str(a.cmake), '-S', str(a.output/'sources'), '-B', str(a.build_dir),
         '-DPC_UNICORN_SOURCE='+str(a.unicorn),
         '-DCMAKE_TOOLCHAIN_FILE='+str(a.ndk/'build/cmake/android.toolchain.cmake'),
         '-DANDROID_ABI='+a.abi, '-DANDROID_PLATFORM=android-26', '-DCMAKE_BUILD_TYPE=Release'],
        [str(a.cmake), '--build', str(a.build_dir), '--target', a.target, '--parallel', str(a.jobs)]]
    for name, command in zip(('configure', 'build'), commands):
        with (a.output/(name+'.txt')).open('w') as log:
            run = subprocess.run(command, stdout=log, stderr=subprocess.STDOUT, env=env)
        if run.returncode:
            raise RuntimeError('Android '+name+' failed; inspect '+str(a.output/(name+'.txt')))
    binary = a.build_dir/('libpc_effect_worker.so' if a.target == 'pc_effect_worker' else a.target)
    library = a.build_dir/'unicorn/libunicorn.so'
    readelf = a.ndk/'toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-readelf'
    for path in (binary, library):
        result = subprocess.check_output([str(readelf), '-h', '-d', str(path)], text=True)
        (a.output/(path.name+'-elf.txt')).write_text(result)
    artifacts = []
    for path in (binary, library):
        archived = a.output/'artifacts'/path.name
        archived.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(path, archived)
        artifacts.append(archived)
    report = dict(schema=1, goal_complete=False, status='ANDROID_PROBE_CROSS_BUILD_ONLY',
        abi=a.abi, target=a.target, min_api=26, ndk_revision='27.3.13750724', unicorn_commit=UNICORN_COMMIT,
        pkgconf_commit=PKGCONF_COMMIT, commands=commands,
        files=[dict(path=str(p.relative_to(ROOT)), bytes=p.stat().st_size, sha256=digest(p))
               for p in (*artifacts, *sources)],
        runtime_effects_added=0, limits=['Standalone evaluator only; no APK/JNI/renderer/events',
            'Cross build does not establish Android execution or ARM performance'])
    (a.output/'build.json').write_text(json.dumps(report, indent=2)+'\n')
    print(json.dumps(report), flush=True)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--abi', choices=['x86_64', 'arm64-v8a'], default='x86_64')
    p.add_argument('--target', choices=['pc_effect_vm_probe', 'pc_effect_scene_probe', 'pc_effect_worker'], default='pc_effect_vm_probe')
    p.add_argument('--ndk', type=Path, default=ROOT/'out/toolchain/android-ndk-r27d')
    p.add_argument('--unicorn', type=Path, default=ROOT/'out/toolchain/unicorn-2.1.4')
    p.add_argument('--pkgconf', type=Path, default=ROOT/'out/toolchain/pkgconf-3.0.7/pkgconf-lite')
    p.add_argument('--cmake', type=Path, default=ROOT/'out/toolchain/pc-native-build/cmake/data/bin/cmake')
    p.add_argument('--build-dir', type=Path)
    p.add_argument('--output', type=Path)
    p.add_argument('--jobs', type=int, default=2)
    a = p.parse_args()
    if not 1 <= a.jobs <= 8: p.error('Bounded build parallelism1..8')
    for field in ('ndk', 'unicorn', 'pkgconf', 'cmake'):
        setattr(a, field, getattr(a, field).resolve())
    a.output = (a.output or ROOT/('out/pc-visual/v148-native-android-'+a.abi)).resolve()
    a.build_dir = (a.build_dir or a.output/'build').resolve()
    if not a.build_dir.is_relative_to(ROOT/'out') or not a.output.is_relative_to(ROOT/'out'):
        p.error('Build/evidence output must remain within the project ignored out directory')
    build(a)
