#!/usr/bin/env python3
"""Gate actual pending-preview pressure and remaining real media callers on source11.

No launch before source11 normal/cold/every-file restore succeeds. Each device
step uses its own full verified backup; test cohort changes are explicit installs.
"""
import argparse
import json
import pathlib
import subprocess
import sys

from run_remaining_normal_media import ROOT, HELPER, accepted, sha


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--previous-source11', type=pathlib.Path, required=True)
    parser.add_argument('--output', type=pathlib.Path, required=True)
    args = parser.parse_args()
    previous = args.previous_source11.resolve()
    baseline, _ = accepted(previous, 11)
    build = json.loads(HELPER.with_name('VOICE_COMBINED_BUILD72.json').read_text())
    frozen = {row['path']: row['sha256'] for row in build['apks']}
    if not build['buildSuccessful'] or frozen != baseline['apks']:
        raise ValueError('Source11 must use the independently built voice identity/type combination72 cohort')
    if not all(sha(pathlib.Path(path)) == digest for path, digest in frozen.items()):
        raise ValueError('Frozen combined game/test APK bytes changed')
    game = next(pathlib.Path(p) for p in frozen if pathlib.Path(p).name == 'app-debug.apk')
    fast_test = next(pathlib.Path(p) for p in frozen if pathlib.Path(p).name == 'app-debug-androidTest.apk')
    args.output.mkdir(parents=True, exist_ok=False)
    fast = args.output/'fast-preview16'
    report = {'scope': 'Actual pending-preview cancellation, real16 normal newgames/gestures/cold/restore, then same original media test cohort all remaining normal callers. Not ARM/PC fullscreen/voice/whole-goal acceptance.',
              'source11AcceptedSession': str(previous), 'fastPreviewAccepted': False,
              'remainingCallersAccepted': False, 'wholeGoalComplete': False}

    def save():
        (args.output/'batch.json').write_text(json.dumps(report, indent=2)+'\n')

    save()
    try:
        subprocess.run([sys.executable, str(HELPER), 'reuse-backup', '--output', str(fast),
                        '--previous', str(previous)], cwd=ROOT, check=True)
        with (fast/'driver.log').open('w') as log:
            subprocess.run([sys.executable, str(HELPER), 'install-test', '--output', str(fast),
                '--apk', str(game), '--test-apk', str(fast_test), '--reuse-installed',
                '--runner', 'SessionAMapRepairInstrumentation', '--suite', 'fastPreview16',
                '--begin', '0', '--end', '16', '--fresh-process-reopen'], cwd=ROOT,
                stdout=log, stderr=subprocess.STDOUT, check=True)
        state = json.loads((fast/'session.json').read_text())
        proof = json.loads((fast/'evidence/fast-preview-cancellation.json').read_text())
        if state['root'] != str(ROOT) or state['serial'] != 'emulator-5554':
            raise ValueError('Wrong exclusive source/device')
        if state['stage'] != 'restored-verified' or not state['passed'] or not state['normalPassed']:
            raise ValueError('Actual fast cancellation and normal16 incomplete')
        if not state['coldProcess']['passed'] or not state['coldProcess']['differentPid']:
            raise ValueError('Actual cold distinct process missing')
        restore = state['restoration']
        if set(restore) != {'internal', 'external'} or not all(r['exactRegularFileSha'] for r in restore.values()):
            raise ValueError('Actual full original data restore missing')
        if state['apks'] != {str(game): sha(game), str(fast_test): sha(fast_test)}:
            raise ValueError('Actual installed fast suite cohort differs')
        rows = proof['rows']
        expected = {(source, cycle) for source in range(16) for cycle in range(2)}
        actual = {(r['sourceIndex'], r['cycle']) for r in rows}
        if not proof['complete'] or proof['requestedSources'] != 16 or len(rows) != 32 or actual != expected:
            raise ValueError('All16 real pending-preview cancellation pairs missing')
        if not all(r['completeSaveRngStateTokenPure'] and r['closedWorkersAndOwners'] for r in rows):
            raise ValueError('Actual worker/owner release or complete pure state missing')
        if not any(r['actualPendingCancellation'] and (r['pendingAtObservation'] > 0 or r['assetsAtObservation'] > 0) for r in rows):
            raise ValueError('No actual unfinished CPU preparation cancellation observed')
        report.update(fastPreviewAccepted=True, fastSession=str(fast.resolve()),
                      actualPendingCancellations=sum(r['actualPendingCancellation'] for r in rows))
        save()
        # Fast-preview and all callers use the same independently built APK pair.
        # Each next source still verifies all files and installed bytes.
        remaining = args.output/'remaining-normal-callers'
        subprocess.run([sys.executable, str(HELPER.with_name('run_remaining_normal_media.py')),
            '--previous-source11', str(previous), '--output', str(remaining)], cwd=ROOT, check=True)
        media = json.loads((remaining/'batch.json').read_text())
        if not media['completeAll16NormalCallers'] or media['apks'] != baseline['apks']:
            raise ValueError('Remaining original media cohort incomplete')
        report.update(remainingCallersAccepted=True, remainingBatch=str(remaining.resolve()))
        save()
    except BaseException as error:
        report.update(error=str(error), needsActualRestorationInspection=True)
        save()
        raise
    print('Actual fast-preview and original-cohort normal callers complete; other goal requirements remain open')


if __name__ == '__main__':
    main()
