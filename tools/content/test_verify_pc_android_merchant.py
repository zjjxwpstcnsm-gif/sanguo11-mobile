"""Exercise the real verification harness with isolated ADB/D8 responses.

No device is contacted. Tests cover successful rules followed by truncated APK,
offline transport or changed user files, and rule failure with intact artifacts.
"""
import argparse
import contextlib
import io
import json
import subprocess
import tarfile
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest import mock
import verify_pc_android_merchant as probe


def archive(value):
    buf = io.BytesIO()
    with tarfile.open(fileobj=buf, mode='w') as tar:
        for name, raw in [('files/auto.sg11', value), ('shared_prefs/settings.xml', b'<map/>')]:
            entry = tarfile.TarInfo(name)
            entry.size = len(raw)
            tar.addfile(entry, io.BytesIO(raw))
    return buf.getvalue()


class HarnessPostVerificationTest(unittest.TestCase):
    def test_real_run_never_reports_success_after_a_failed_gate(self):
        for mode in ('success', 'truncated_apk', 'offline', 'changed_user_file', 'rule_failure'):
            with self.subTest(mode=mode), tempfile.TemporaryDirectory(dir=probe.ROOT/'out') as folder:
                base = Path(folder)
                apk = base/'candidate.apk'
                apk.write_bytes(b'complete synthetic APK for harness test')
                original = archive(b'preserved synthetic save')
                state = dict(apk_reads=0, tar_reads=0, remote_probe=None, calls=0)

                def run(command, **kwargs):
                    state['calls'] += 1
                    if command[0].endswith('/bin/java'):
                        with zipfile.ZipFile(command[command.index('--output')+1], 'w') as z:
                            z.writestr('classes.dex', b'synthetic test-only dex')
                        return subprocess.CompletedProcess(command, 0, b'', b'')
                    self.assertEqual(command[:3], [str(probe.ROOT/'out/toolchain/android-sdk/platform-tools/adb'), '-s', 'isolated-test'])
                    parts = command[3:]
                    if parts == ['shell','id','-u']:
                        data = b'0\n'
                    elif parts[:3] == ['shell','pm','path']:
                        data = b'package:/data/app/isolated/base.apk\n'
                    elif parts[:3] == ['shell','am','force-stop']:
                        data = b''
                    elif parts[:2] == ['exec-out','tar']:
                        state['tar_reads'] += 1
                        data = archive(b'changed synthetic save') if mode == 'changed_user_file' and state['tar_reads'] == 2 else original
                    elif parts[:2] == ['exec-out','cat']:
                        if parts[2] == '/data/app/isolated/base.apk':
                            state['apk_reads'] += 1
                            if state['apk_reads'] == 2 and mode == 'offline':
                                raise subprocess.CalledProcessError(1, command, b'', b'device offline')
                            data = b'partial' if state['apk_reads'] == 2 and mode == 'truncated_apk' else apk.read_bytes()
                        else:
                            data = state['remote_probe']
                    elif parts[0] == 'push':
                        state['remote_probe'] = Path(parts[1]).read_bytes()
                        data = b''
                    elif parts[:2] == ['shell','chmod']:
                        data = b''
                    elif parts[:2] == ['shell','env']:
                        name = parts[-1].split('.')[-1]
                        data = ('FAIL synthetic rule\n' if mode == 'rule_failure' else 'PASS '+name+' synthetic\n').encode()
                    else:
                        self.fail('Unexpected external command '+repr(command))
                    return subprocess.CompletedProcess(command, 0, data, b'')

                args = argparse.Namespace(serial='isolated-test', apk=apk, output=base/'result', ability_arithmetic=True)
                with mock.patch.object(probe.subprocess, 'run', side_effect=run), contextlib.redirect_stdout(io.StringIO()):
                    if mode == 'success':
                        probe.run(args)
                    else:
                        with self.assertRaises(ValueError):
                            probe.run(args)
                report = json.loads((args.output/'results.json').read_text())
                self.assertEqual(mode == 'success', report['passed'])
                self.assertEqual(mode != 'changed_user_file', report['user_files_unchanged'])
                self.assertGreater(state['calls'], 5)
                self.assertEqual(original, (args.output/'user-before.tar').read_bytes())
                if mode == 'offline':
                    self.assertEqual('CalledProcessError', report['verification_error']['type'])
                if mode == 'truncated_apk':
                    self.assertEqual(7, report['post_apk_readback_bytes'])
                    self.assertFalse(report['installed_apk_unchanged'])
                if mode not in ('rule_failure',):
                    self.assertTrue(report['rules_passed'])
        print('verification harness gates:5 isolated outcomes passed')


if __name__ == '__main__':
    unittest.main()
