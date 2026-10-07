#!/usr/bin/env python3
"""Continue the current exact cohort only after actual source11 acceptance.

Waits read-only for complete restoration. A failed preceding run causes no
device actions. The next controller repeats independent full backups and all
normal UI assertions, and refuses any changed frozen game/test APK bytes.
"""
import argparse
import json
import pathlib
import subprocess
import sys
import time

from run_remaining_normal_media import ROOT, HELPER, accepted, sha


def wait_restored(session):
    deadline = time.monotonic()+14460
    while time.monotonic() < deadline:
        state = json.loads((session/'session.json').read_text())
        if state['stage'] == 'restored-verified':
            return state
        time.sleep(5)
    raise TimeoutError('Preceding actual run has not restored; no next device action')


def ordinary_accepted(session):
    state = wait_restored(session)
    if state['root'] != str(ROOT) or state['serial'] != 'emulator-5554':
        raise ValueError('Wrong ordinary source/device')
    cold = state.get('coldProcess', {})
    restored = state.get('restoration', {})
    if not state.get('passed') or not state.get('normalPassed'):
        raise ValueError('Ordinary normal acceptance failed; no default install')
    if not cold.get('passed') or not cold.get('differentPid'):
        raise ValueError('Ordinary actual distinct-process acceptance missing')
    if set(restored) != {'internal', 'external'} or not all(r['exactRegularFileSha'] for r in restored.values()):
        raise ValueError('Ordinary every-file restoration missing')
    frozen = json.loads(HELPER.with_name('NORMAL384_COMBINED_BUILD68.json').read_text())
    expected = {r['path']: r['sha256'] for r in frozen['apks']}
    if state['apks'] != expected or not all(sha(pathlib.Path(p)) == h for p, h in expected.items()):
        raise ValueError('Ordinary actual installed/frozen APK cohort mismatch')
    for source in range(16):
        proof = json.loads((session/'evidence'/('source-'+str(source)+'-faction-previews.json')).read_text())
        if proof['sourceIndex'] != source or not proof['noAuthorityMutation']:
            raise ValueError('Ordinary complete source/faction pure evidence missing')
        if proof['actualEnabledCount'] <= 0 or proof['actualEnabledCount'] != sum(r['enabled'] for r in proof['slots']):
            raise ValueError('Ordinary actual enabled faction preview coverage missing')
        if 'SESSION_A source '+str(source)+' normal flow complete' not in state['testOutput']:
            raise ValueError('Ordinary actual source normal workflow missing')
    return state


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source11', type=pathlib.Path, required=True)
    parser.add_argument('--preceding-ordinary', type=pathlib.Path,
                        help='Require current complete ordinary68 matrix before fresh default69 source11 install')
    parser.add_argument('--output', type=pathlib.Path, required=True)
    args = parser.parse_args()
    source = args.source11.resolve()
    args.output.mkdir(parents=True, exist_ok=False)
    record = args.output/'continuation.json'
    report = {'source11': str(source), 'stage': 'waiting-source11-restoration',
              'scope': 'Current cohort real source11 then fast-preview16 and remaining15 actual callers; not ARM or whole-goal acceptance',
              'wholeGoalComplete': False}

    def save():
        record.write_text(json.dumps(report, indent=2)+'\n')

    save()
    try:
        if args.preceding_ordinary:
            previous = args.preceding_ordinary.resolve()
            report.update(stage='waiting-ordinary68-restoration', precedingOrdinary=str(previous))
            save()
            ordinary_accepted(previous)
            if pathlib.Path('/tmp/sanguo11-emulator-5554-session-a.lock').exists():
                raise ValueError('5554 remains owned; no competing default install')
            frozen = json.loads(HELPER.with_name('MEDIA_VIEWPORT_TEST_BUILD69.json').read_text())
            if not frozen['buildSuccessful']:
                raise ValueError('Independent viewport69 build missing')
            artifacts = {r['path']: r['sha256'] for r in frozen['apks']}
            if not all(sha(pathlib.Path(p)) == h for p, h in artifacts.items()):
                raise ValueError('Frozen default69 cohort changed')
            game = next(p for p in artifacts if pathlib.Path(p).name == 'app-debug.apk')
            test = next(p for p in artifacts if pathlib.Path(p).name == 'app-debug-androidTest.apk')
            report.update(stage='ordinary-accepted-fresh-default-source11', apks=artifacts)
            save()
            subprocess.run([sys.executable, str(HELPER), 'reuse-backup',
                '--output', str(source), '--previous', str(previous)], cwd=ROOT, check=True)
            with (source/'driver.log').open('w') as log:
                subprocess.run([sys.executable, str(HELPER), 'install-test',
                    '--output', str(source), '--apk', game, '--test-apk', test,
                    '--runner', 'SessionAMapRepairInstrumentation', '--suite', 'mediaAll16',
                    '--begin', '11', '--end', '12', '--fresh-process-reopen'],
                    cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
        wait_restored(source)
        baseline, _ = accepted(source, 11)
        lock = pathlib.Path('/tmp/sanguo11-emulator-5554-session-a.lock')
        if lock.exists():
            raise ValueError('5554 remains owned; no competing next acceptance')
        report.update(stage='preceding-accepted-starting-next', apks=baseline['apks'])
        save()
        with (args.output/'driver.log').open('w') as log:
            subprocess.run([sys.executable,
                str(HELPER.with_name('run_fast_preview_and_remaining_media.py')),
                '--previous-source11', str(source),
                '--output', str(args.output/'actual-fast-and-callers')],
                cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
        report['stage'] = 'controller-completed-other-goal-items-open'
        save()
    except BaseException as error:
        report.update(stage='stopped', error=repr(error),
                      requireActualCurrentDeviceRestorationInspection=True)
        save()
        raise


if __name__ == '__main__':
    main()
