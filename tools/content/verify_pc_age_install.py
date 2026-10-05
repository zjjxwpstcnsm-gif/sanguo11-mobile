#!/usr/bin/env python3
"""Install and verify inherited age fixtures with external user-data restoration.

Explicit serial required. Does not clear emulator data or touch any other device.
Root is required to preserve files independently of the test runner's own restore.
"""
import argparse
import hashlib
import io
import json
import re
import subprocess
import tarfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PACKAGE = 'game.sanguo.mobile.dev'
CASES = [f'tactic-{name}-{age}' for name in ('liubei', 'guanyu', 'zhangfei', 'zhaoyun', 'zhugeliang', 'caocao') for age in ('young', 'old')]


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def members(raw):
    with tarfile.open(fileobj=io.BytesIO(raw)) as archive:
        return {m.name: archive.extractfile(m).read() for m in archive if m.isfile()}


def run(args):
    # Validate both immutable inputs before any device access or backup mutation.
    apk_hashes = {str(p): sha(p.read_bytes()) for p in (args.apk, args.test_apk)}
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    adb = [str(ROOT / 'out/toolchain/android-sdk/platform-tools/adb'), '-s', args.serial]
    def command(*parts, timeout=60, **kwargs):
        return subprocess.run(adb + list(parts), check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout, **kwargs).stdout
    if command('shell', 'id', '-u').strip() != b'0':
        raise ValueError('Root required for independent backup/restore; no installation attempted')
    command('shell', 'am', 'force-stop', PACKAGE)
    before = command('exec-out', 'tar', '-C', '/data/data/' + PACKAGE, '-cf', '-', 'files', 'shared_prefs')
    (output / 'user-before.tar').write_bytes(before)
    original = members(before)
    manifest = dict(serial=args.serial, device=command('shell', 'getprop').decode(),
                    apks=apk_hashes,
                    backup={k: dict(bytes=len(v), sha256=sha(v)) for k, v in original.items()}, requested_cases=args.cases, results=[])
    def save():
        (output / 'results.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    save()
    remote = '/data/local/tmp/pc-age-backup-' + str(int(time.time())) + '.tar'
    command('push', str(output / 'user-before.tar'), remote)
    def restore(label):
        command('shell', 'am', 'force-stop', PACKAGE)
        command('shell', 'tar', '-C', '/data/data/' + PACKAGE, '-xf', remote)
        after = command('exec-out', 'tar', '-C', '/data/data/' + PACKAGE, '-cf', '-', 'files', 'shared_prefs')
        (output / (label + '-restored.tar')).write_bytes(after)
        files = members(after)
        mismatch = [k for k, v in original.items() if files.get(k) != v]
        if mismatch:
            raise ValueError('User-data restoration mismatch: ' + repr(mismatch))
        return dict(all_original_files_byte_equal=True, added_files=sorted(set(files) - set(original)),
                    auto_sha256=sha(files['files/auto.sg11']) if 'files/auto.sg11' in files else None)
    try:
        if not args.reuse_installed:
            with (output / 'installation.txt').open('wb') as log:
                log.write(command('install', '-r', str(args.apk.resolve()), timeout=180))
                log.write(command('install', '-r', str(args.test_apk.resolve()), timeout=180))
        manifest['reused_installed'] = args.reuse_installed
        test_path = command('shell', 'pm', 'path', PACKAGE+'.test').decode().strip().removeprefix('package:')
        manifest['installed_test_sha256'] = sha(command('exec-out', 'cat', test_path, timeout=180))
        if manifest['installed_test_sha256'] != apk_hashes[str(args.test_apk)]:
            raise ValueError('Installed test APK differs from candidate')
        path = command('shell', 'pm', 'path', PACKAGE).decode().strip().removeprefix('package:')
        installed = command('exec-out', 'cat', path, timeout=180)
        manifest['installed_sha256'] = sha(installed)
        save()
        if sha(installed) != sha(args.apk.read_bytes()):
            raise ValueError('Installed main APK differs from candidate')
        for label in args.cases:
            if label not in CASES:
                raise ValueError('Unsupported age fixture: ' + label)
            print('START ' + label, flush=True)
            started = time.monotonic()
            record = dict(case=label, passed=False)
            try:
                with (output / (label + '.txt')).open('wb') as log:
                    process = subprocess.run(adb + ['shell', 'am', 'instrument', '-w', '-e', 'case', label,
                        PACKAGE + '.test/game.sanguo.mobile.PcPresentationsInstrumentation'], stdout=log, stderr=subprocess.STDOUT, timeout=args.timeout)
                result = (output / (label + '.txt')).read_text()
                record.update(exit_code=process.returncode, passed=process.returncode == 0 and 'PASS PC PRESENTATIONS' in result and 'FAIL' not in result)
            except subprocess.TimeoutExpired:
                record.update(passed=False, timeout=True)
            finally:
                record['seconds'] = round(time.monotonic() - started, 2)
                record['restoration'] = restore(label)
                manifest['results'].append(record)
                save()
            # Capture only this fixture's generated evidence, preserving all
            # existing remote reports and user files. Safe fixed fixture names.
            remote_dir = '/sdcard/Android/data/' + PACKAGE + '/files/s01'
            listing = command('shell', 'ls', remote_dir).decode().splitlines()
            evidence = output / label
            evidence.mkdir()
            for name in listing:
                if (name.startswith('pc-presentation-' + label + '-') or name in ('pc-presentations-report.txt', 'pc-presentations-failed.png')) and re.fullmatch(r'[a-zA-Z0-9_.-]+', name):
                    (evidence / name).write_bytes(command('exec-out', 'cat', remote_dir + '/' + name))
            print(json.dumps(record), flush=True)
            if args.stop_on_failure and not record['passed']:
                manifest['remaining_cases_not_run'] = args.cases[args.cases.index(label)+1:]
                save()
                break
    finally:
        manifest['final_restoration'] = restore('final')
        save()
    if not all(row['passed'] for row in manifest['results']):
        raise SystemExit(1)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--test-apk', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--cases', nargs='+', choices=CASES, default=CASES)
    parser.add_argument('--timeout', type=int, default=600)
    parser.add_argument('--stop-on-failure', action='store_true', help='Restore user files and stop remaining cases after the first failed/timeout case')
    parser.add_argument('--reuse-installed', action='store_true', help='Skip installs only after exact main and test APK readback verification')
    run(parser.parse_args())
