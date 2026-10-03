#!/usr/bin/env python3
"""Run city displacement and host tests against the exact installed APK on ART.

The DEX contains only named test/fixture classes. Production rules, runtime and
resources come from the installed APK. Never install/clear app data here; use
the existing installation harness first, with an idle explicitly owned serial.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
from verify_pc_age_install import ROOT, PACKAGE, members


def sha(data):
    return hashlib.sha256(data).hexdigest()


def run(args):
    output = args.output.resolve()
    if ROOT / 'out' not in output.parents:
        raise ValueError('Output must be a fresh directory inside project out/')
    output.mkdir(parents=True, exist_ok=False)
    sdk = ROOT / 'out/toolchain/android-sdk'
    adb = [str(sdk / 'platform-tools/adb'), '-s', args.serial]

    def command(*parts, timeout=60):
        return subprocess.run(adb + list(parts), check=True, stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE, timeout=timeout).stdout

    suites = {
        'game.sanguo.core.CityDisplacementTest': ROOT / 'core/build/classes/java/test/game/sanguo/core/CityDisplacementTest.class',
        'game.sanguo.runtime.CityDisplacementSessionTest': ROOT / 'game-runtime/build/classes/java/test/game/sanguo/runtime/CityDisplacementSessionTest.class',
    }
    inputs = list(suites.values()) + [ROOT / 'core/build/classes/java/test/game/sanguo/core/DisplacementFixture.class']
    if not all(p.is_file() for p in inputs):
        raise ValueError('Compile both registered test suites before using the device')
    expected = sha(args.apk.read_bytes())
    installed = command('shell', 'pm', 'path', PACKAGE).decode().strip().removeprefix('package:')
    if not installed.startswith('/data/app/') or '\n' in installed:
        raise ValueError('Expected one installed APK')
    if sha(command('exec-out', 'cat', installed, timeout=180)) != expected:
        raise ValueError('Installed APK differs from candidate')
    if command('shell', 'id', '-u').strip() != b'0':
        raise ValueError('Root required for full external user-file verification')
    command('shell', 'am', 'force-stop', PACKAGE)
    backup = command('exec-out', 'tar', '-C', '/data/data/' + PACKAGE, '-cf', '-', 'files', 'shared_prefs')
    (output / 'user-before.tar').write_bytes(backup)
    original = members(backup)
    remote_backup = '/data/local/tmp/city-displacement-backup-' + str(time.time_ns()) + '.tar'
    command('push', str(output / 'user-before.tar'), remote_backup)
    report = dict(passed=False, serial=args.serial, apk_sha256=expected,
                  scope='Exact installed APK production rules/runtime on ART; UI gestures verified separately',
                  class_sources={str(p.relative_to(ROOT)): sha(p.read_bytes()) for p in inputs}, results=[])

    def save():
        (output / 'results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

    save()
    try:
        probe = output / 'probe.zip'
        cmd = [str(Path(os.environ['JAVA_HOME']) / 'bin/java'), '-cp', str(sdk / 'build-tools/35.0.0/lib/d8.jar'),
               'com.android.tools.r8.D8', '--min-api', '26', '--lib', str(sdk / 'platforms/android-35/android.jar')]
        for module in ('core', 'game-api', 'game-runtime'):
            cmd += ['--classpath', str(ROOT / module / 'build/libs' / (module + '.jar'))]
        cmd += ['--output', str(probe)] + [str(p) for p in inputs]
        with (output / 'd8.log').open('w') as log:
            subprocess.run(cmd, check=True, stdout=log, stderr=log)
        report['probe_sha256'] = sha(probe.read_bytes())
        remote = '/data/local/tmp/city-displacement-' + report['probe_sha256'][:20] + '.zip'
        command('push', str(probe), remote)
        command('shell', 'chmod', '644', remote)
        if sha(command('exec-out', 'cat', remote)) != report['probe_sha256']:
            raise ValueError('Probe readback differs')
        for suite in suites:
            started = time.monotonic()
            result = command('shell', 'env', 'CLASSPATH=' + installed + ':' + remote,
                             'app_process', '/system/bin', suite, timeout=180)
            (output / (suite.rsplit('.', 1)[1] + '.log')).write_bytes(result)
            if b'PASS' not in result:
                raise ValueError('Missing suite pass marker: ' + suite)
            report['results'].append(dict(suite=suite, seconds=round(time.monotonic() - started, 2), output=result.decode()))
            save()
        report['rules_passed'] = True
    finally:
        after = command('exec-out', 'tar', '-C', '/data/data/' + PACKAGE, '-cf', '-', 'files', 'shared_prefs')
        (output / 'user-after.tar').write_bytes(after)
        report['user_files_unchanged'] = members(after) == original
        if not report['user_files_unchanged']:
            command('shell', 'am', 'force-stop', PACKAGE)
            command('shell', 'tar', '-C', '/data/data/' + PACKAGE, '-xf', remote_backup)
            restored = command('exec-out', 'tar', '-C', '/data/data/' + PACKAGE, '-cf', '-', 'files', 'shared_prefs')
            (output / 'user-restored.tar').write_bytes(restored)
            report['all_original_files_restored'] = all(members(restored).get(k) == v for k, v in original.items())
        report['installed_apk_unchanged'] = sha(command('exec-out', 'cat', installed, timeout=180)) == expected
        report['passed'] = bool(report.get('rules_passed') and report['user_files_unchanged'] and report['installed_apk_unchanged'])
        save()
    print(json.dumps(report, ensure_ascii=False))
    if not report['passed']:
        raise SystemExit(1)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    run(parser.parse_args())
