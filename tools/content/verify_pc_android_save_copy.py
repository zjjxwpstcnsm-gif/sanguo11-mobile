#!/usr/bin/env python3
"""Compare save copying on exact installed APKs without changing app/test UI.

Only the test probe goes into the independent dex. A SHA-pinned normal UI save
is copied to /data/local/tmp; all gameplay worlds stay in probe process memory.
User files and the full production APK must match again after the probe.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
from verify_pc_age_install import ROOT, PACKAGE, members


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def run(args):
    output = args.output.resolve()
    if ROOT/'out' not in output.parents:
        raise ValueError('Output must be inside ignored project out/')
    output.mkdir(parents=True, exist_ok=False)
    sdk = ROOT/'out/toolchain/android-sdk'
    adb = [str(sdk/'platform-tools/adb'), '-s', args.serial]

    def command(*parts, timeout=180):
        return subprocess.run(adb+list(parts), check=True, stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE, timeout=timeout).stdout

    expected = sha(args.apk.read_bytes())
    installed = command('shell', 'pm', 'path', PACKAGE).decode().strip().removeprefix('package:')
    if not installed.startswith('/data/app/') or '\n' in installed:
        raise ValueError('Expected one installed APK')
    if sha(command('exec-out', 'cat', installed)) != expected:
        raise ValueError('Installed APK mismatch')
    fixture = args.save.read_bytes()
    if sha(fixture) != '02ddb3d44d98fbebe763a82551b5cb5eb68da6e70b6087568c0a73943bde5d69':
        raise ValueError('Expected provenance-pinned normal UI190 save')
    command('shell', 'am', 'force-stop', PACKAGE)
    before = command('exec-out', 'tar', '-C', '/data/data/'+PACKAGE, '-cf', '-', 'files', 'shared_prefs')
    (output/'user-before.tar').write_bytes(before)
    report = dict(passed=False, apk_sha256=expected, fixture_sha256=sha(fixture),
                  serial=args.serial, scope='ART production classes; generated in-memory copies, not UI timing')
    try:
        java = Path(os.environ['JAVA_HOME'])/'bin/java'
        probe = output/'probe.zip'
        source = ROOT/'core/build/classes/java/test/game/sanguo/core/SaveCopyProbe.class'
        report['probe_class_sha256'] = sha(source.read_bytes())
        with (output/'d8.log').open('w') as log:
            subprocess.run([str(java), '-cp', str(sdk/'build-tools/35.0.0/lib/d8.jar'),
                            'com.android.tools.r8.D8', '--min-api', '26', '--lib',
                            str(sdk/'platforms/android-35/android.jar'), '--classpath',
                            str(ROOT/'core/build/libs/core.jar'), '--output', str(probe), str(source)],
                           stdout=log, stderr=log, check=True)
        report['probe_sha256'] = sha(probe.read_bytes())
        remote = '/data/local/tmp/save-copy-'+report['probe_sha256'][:20]+'.zip'
        remote_save = '/data/local/tmp/save-copy-'+sha(fixture)[:20]+'.sg11'
        for local, target in [(probe, remote), (args.save, remote_save)]:
            command('push', str(local), target)
            command('shell', 'chmod', '644', target)
            if command('exec-out', 'cat', target) != local.read_bytes():
                raise ValueError('Probe input readback mismatch')
        raw = command('shell', 'env', 'CLASSPATH='+installed+':'+remote, 'app_process',
                      '/system/bin', 'game.sanguo.core.SaveCopyProbe', remote_save, timeout=600)
        (output/'probe.log').write_bytes(raw)
        report['probe_output'] = raw.decode()
        report['probe_passed'] = b'PASS SaveCopyProbe' in raw
        if not report['probe_passed']:
            raise ValueError('Probe failed')
    finally:
        try:
            after = command('exec-out', 'tar', '-C', '/data/data/'+PACKAGE, '-cf', '-', 'files', 'shared_prefs')
            (output/'user-after.tar').write_bytes(after)
            report['user_files_unchanged'] = members(before) == members(after)
            apk_after = command('exec-out', 'cat', installed)
            report['post_apk_bytes'] = len(apk_after)
            report['post_apk_sha256'] = sha(apk_after)
            report['installed_apk_unchanged'] = sha(apk_after) == expected
        except Exception as error:
            report['post_verification_error'] = str(error)
        report['passed'] = bool(report.get('probe_passed') and report.get('user_files_unchanged') and report.get('installed_apk_unchanged'))
        (output/'results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    if not report['passed']:
        raise ValueError('Probe/post-verification failed')
    print(json.dumps(report, ensure_ascii=False))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--save', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    run(parser.parse_args())
