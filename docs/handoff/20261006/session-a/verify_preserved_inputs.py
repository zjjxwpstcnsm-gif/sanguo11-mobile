#!/usr/bin/env python3
"""Read-only complete inherited/user/fixed-input/JNI preservation audit."""
import argparse
import hashlib
import json
import pathlib
import subprocess
import time
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[4]
OWN = pathlib.Path(__file__).resolve().parent


def sha(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1048576), b''):
            digest.update(block)
    return digest.hexdigest()


def git(base, *args):
    return subprocess.check_output(['git', '-C', str(base), *args], text=True).strip()


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--output', type=pathlib.Path, required=True)
    args = p.parse_args()
    report = dict(startedUnix=time.time(), guards=[], packages=[], wholeGoalComplete=False,
                  scope='Protected source/user files, exact168 fixed inputs in source and frozen81/90 APKs, original4/new2 JNI and Bridge/PC executable. Read-only; no B WIP/data/device mutation. No gameplay, memory budget or ARM acceptance.')
    for filename, root_key in [('SOURCE_GUARD.json', 'source'), ('ORIGINAL_GUARD.json', 'directory')]:
        guard = json.loads((OWN/filename).read_text())
        base = pathlib.Path(guard[root_key])
        assert git(base, 'rev-parse', 'HEAD') == guard['head']
        for row in guard['files']:
            path = base/row['path']
            if row.get('exists', True):
                assert path.is_file() and sha(path) == row['sha256'], str(path)
            else:
                assert not path.exists(), str(path)
        report['guards'].append(dict(root=str(base), head=guard['head'], verifiedPaths=len(guard['files'])))
    pins_path = ROOT/'tools/content/map-release-manifest.json'
    original = json.loads((OWN/'SOURCE_GUARD.json').read_text())
    expected_manifest = next(row['sha256'] for row in original['files'] if row['path'] == str(pins_path.relative_to(ROOT)))
    assert sha(pins_path) == expected_manifest
    pins = json.loads(pins_path.read_text())['files']
    assert len(pins) == 168
    for row in pins:
        assert sha(ROOT/row['source_path']) == row['sha256'], row['source_path']
    report['fixedInputsSource'] = dict(count=168, unchangedManifestSha256=expected_manifest)
    for filename in ('WATER_NORMAL384_BUILD81.json', 'FIRE_OVERRIDE_DEFAULT_BUILD90.json'):
        build = json.loads((OWN/filename).read_text())
        apk = next(row for row in build['apks'] if pathlib.Path(row['path']).name == 'app-debug.apk')
        assert sha(pathlib.Path(apk['path'])) == apk['sha256']
        with zipfile.ZipFile(apk['path']) as archive:
            for row in pins:
                assert hashlib.sha256(archive.read(row['apk_path'])).hexdigest() == row['sha256']
            for row in build['sixJniExact']:
                assert hashlib.sha256(archive.read(row['entry'])).hexdigest() == row['sha256']
        report['packages'].append(dict(receipt=filename, apk=apk, fixedPackagedInputsExact=168, sixJniExact=build['sixJniExact']))
    inheritance = json.loads((OWN/'INHERITANCE.json').read_text())
    for row in inheritance['jni']:
        assert sha(ROOT/row['path']) == row['sha256']
    for abi in ('arm64-v8a', 'x86_64'):
        path = ROOT/f'out/session-a/pc-fire-runtime/additional-jniLibs/{abi}/libpc_effect_fire_worker.so'
        expected = next(row['sha256'] for row in report['packages'][1]['sixJniExact'] if row['entry'] == f'lib/{abi}/libpc_effect_fire_worker.so')
        assert sha(path) == expected
    previous = json.loads((OWN/'PRESERVATION_CHECKPOINT85.json').read_text())
    bridge = ROOT/'app/src/main/java/game/sanguo/mobile/bridge/AndroidGameBridge.java'
    assert sha(bridge) == previous['bridgeSha256']
    exe = pathlib.Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版/san11pk.exe')
    assert sha(exe) == previous['pcExeSha256']
    report.update(bridgeSha256=sha(bridge), pcExeSha256=sha(exe),
                  main=git(ROOT, 'rev-parse', 'main'), finishedUnix=time.time())
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({key: report[key] for key in ('guards', 'fixedInputsSource', 'main')}))


if __name__ == '__main__':
    main()
