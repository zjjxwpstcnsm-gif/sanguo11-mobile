#!/usr/bin/env python3
"""Stage independently checked original visual-worker inputs for APK packaging.

No PC writes/downloads. Large compiler caches stay in out; only source-derived
kernel/scene assets enter the app. Native artifacts are reproducible build output.
This stages a transport, not renderer/event completion.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
KERNEL_SHA = '2668d82c3f835b4dc043a3d28f1902b472627570c1c25ba081425c950f4f8207'
SCENE_SHA = '98d2626d2842f272474cc7a3c2437e29b946d3827c8b52089741155cbc79e4ae'
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def stage(a):
    evidence = json.loads((a.checked/'comparison.json').read_text())
    if evidence['status'] != 'PASS_ANDROID_PERSISTENT_SOURCE_WORKER': raise ValueError('Android persistent execution proof required')
    if sha(a.kernel) != KERNEL_SHA or sha(a.scene) != SCENE_SHA: raise ValueError('Pinned source input')
    builds = [json.loads((directory/'build.json').read_text()) for directory in (a.x86, a.arm64)]
    if [row['abi'] for row in builds] != ['x86_64', 'arm64-v8a'] or any(row['target'] != 'pc_effect_worker' for row in builds):
        raise ValueError('Two exact persistent worker ABI builds required')
    if sha(a.x86/'build.json') != evidence['android']['build_report_sha256']: raise ValueError('Executed Android build differs')
    sources = []
    for build in builds:
        for row in build['files']:
            if sha(ROOT/row['path']) != row['sha256']: raise ValueError('Archived build artifact differs')
        sources.append({Path(row['path']).name:row['sha256'] for row in build['files'] if '/sources/' in row['path']})
    if sources[0] != sources[1]: raise ValueError('ABI builds use different source snapshots')
    records = []
    for source, name, expected in ((a.kernel, 'source-kernel.bin', KERNEL_SHA), (a.scene, 'source-scene.bin', SCENE_SHA)):
        destination = ROOT/'app/src/main/assets/3d/pc-effects'/name
        destination.parent.mkdir(parents=True, exist_ok=True)
        if destination.exists() and sha(destination) != expected: raise ValueError('Preserve existing modified source asset: '+name)
        shutil.copyfile(source, destination)
        records.append(dict(source=str(source.relative_to(ROOT)), output=str(destination.relative_to(ROOT)), bytes=destination.stat().st_size, sha256=expected))
    for build in builds:
        for row in build['files']:
            if '/artifacts/' not in row['path']: continue
            source = ROOT/row['path']; destination = ROOT/'out/pc-native-runtime/jniLibs'/build['abi']/source.name
            destination.parent.mkdir(parents=True, exist_ok=True); shutil.copyfile(source, destination)
            records.append(dict(source=row['path'], output=str(destination.relative_to(ROOT)), abi=build['abi'], bytes=destination.stat().st_size, sha256=sha(destination)))
    report = dict(schema=1, goal_complete=False, status='SOURCE_WORKER_STAGED_MAP_GPU_BINDING_SEPARATE_VALIDATION',
        source_executable_sha256='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb',
        source_seff_resource=4792, source_ksef_resources=[133,134,141,142,143,144,145,148],
        placements=126, files=records, native_source_snapshots=sources[0],
        android_worker_evidence=str((a.checked/'comparison.json').relative_to(ROOT)),
        android_worker_evidence_sha256=sha(a.checked/'comparison.json'),
        runtime_binding='normal PC map -> PcEffectProcess private source VM -> PcMapEffects ordered quads',
        renderer_validation='validation-v152-working.json; conversion does not itself establish GPU/PC acceptance',
        map_seff_templates_bound=8, semantic_effect_acceptance_count=0,
        runtime_effects_added=0, limits=['This tool verifies/stages source transport; GPU evidence is separate', 'x86_64 emulator executed; ARM64 build only',
            'Actual MOD resolver/PC images and sustained performance pending'])
    manifest = ROOT/'docs/pc-visual/worker-source-working.json'
    manifest.write_text(json.dumps(report, indent=2)+'\n')
    release = ROOT/'tools/content/map-release-manifest.json'; current = json.loads(release.read_text())
    paths = {row['source_path'] for row in current['files']}
    for row in records:
        if row['output'].startswith('app/src/main/assets/') and row['output'] not in paths:
            current['files'].append(dict(source_path=row['output'], apk_path=row['output'].replace('app/src/main/', '', 1), sha256=row['sha256']))
    release.write_text(json.dumps(current, indent=2)+'\n')
    print(json.dumps(dict(status=report['status'], staged=len(records), runtime_effects_added=0)), flush=True)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--x86', type=Path, default=ROOT/'out/pc-visual/v150-native-x86-camera-build')
    p.add_argument('--arm64', type=Path, default=ROOT/'out/pc-visual/v150-native-arm-camera-build')
    p.add_argument('--checked', type=Path, default=ROOT/'out/pc-visual/v151-native-perspective-block-checked')
    p.add_argument('--kernel', type=Path, default=ROOT/'out/pc-visual/v148-native-probe-checked/source-kernel.bin')
    p.add_argument('--scene', type=Path, default=ROOT/'out/pc-visual/v148-native-shared-scene/source-scene.bin')
    a = p.parse_args()
    for name in ('x86', 'arm64', 'checked', 'kernel', 'scene'): setattr(a, name, getattr(a, name).resolve())
    stage(a)
