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
    parser.add_argument('--cohort-receipt', type=pathlib.Path,
                        default=HELPER.with_name('VOICE_COMBINED_BUILD72.json'))
    args = parser.parse_args()
    previous = args.previous_source11.resolve()
    baseline, _ = accepted(previous, 11)
    receipt = args.cohort_receipt.resolve()
    if receipt.parent != HELPER.parent.resolve():
        raise ValueError('Owned frozen cohort receipt required')
    build = json.loads(receipt.read_text())
    frozen = {row['path']: row['sha256'] for row in build['apks']}
    if not build['buildSuccessful'] or frozen != baseline['apks']:
        raise ValueError('Source11 must use the exact independently built frozen cohort')
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
        # Ordinary68 completed functionally but sampled only14.27MiB headroom.
        # Reproduce its normal0..6 prefix separately with the explicit existing
        # heap diagnostic. It requests GC: never substitute it for the untouched
        # ordinary68 peak or claim allocation-stack/unperturbed-budget acceptance.
        budget = json.loads(HELPER.with_name('ORDINARY384_ALL_FACTIONS_ACCEPTANCE68.json').read_text())
        diagnostic_previous = previous
        different_cohort = False
        if not budget['memoryBudgetClosed']:
            diagnostic = args.output/'ordinary-prefix-source0-to6-heap-diagnostic'
            ordinary = json.loads(HELPER.with_name('NORMAL384_COMBINED_BUILD68.json').read_text())
            ordinary_apks = {row['path']: row['sha256'] for row in ordinary['apks']}
            if not ordinary['buildSuccessful'] or not all(sha(pathlib.Path(p)) == h for p, h in ordinary_apks.items()):
                raise ValueError('Frozen ordinary diagnostic cohort changed')
            ordinary_game = next(p for p in ordinary_apks if pathlib.Path(p).name == 'app-debug.apk')
            ordinary_test = next(p for p in ordinary_apks if pathlib.Path(p).name == 'app-debug-androidTest.apk')
            report.update(activeStage='ordinary-prefix-heap-diagnostic',
                          ordinaryDiagnosticScope='Actual normal source0..6/factions prefix; Debug.dumpHprofData requestsGC, not unperturbed peak or allocation-stack acceptance',
                          ordinaryDiagnosticSession=str(diagnostic.resolve()), memoryBudgetClosed=False)
            save()
            subprocess.run([sys.executable, str(HELPER), 'reuse-backup', '--output', str(diagnostic),
                            '--previous', str(previous)], cwd=ROOT, check=True)
            with (diagnostic/'driver.log').open('w') as log:
                subprocess.run([sys.executable, str(HELPER), 'install-test', '--output', str(diagnostic),
                    '--apk', ordinary_game, '--test-apk', ordinary_test,
                    '--runner', 'SessionAMapRepairInstrumentation', '--suite', 'factions16',
                    '--begin', '0', '--end', '7', '--heap-profile', '--fresh-process-reopen'],
                    cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
            result = json.loads((diagnostic/'session.json').read_text())
            restore = result.get('restoration', {})
            if result['stage'] != 'restored-verified' or not result.get('passed') or not result.get('coldProcess', {}).get('passed'):
                raise ValueError('Actual normal diagnostic prefix/cold incomplete')
            if set(restore) != {'internal', 'external'} or not all(r['exactRegularFileSha'] for r in restore.values()):
                raise ValueError('Diagnostic complete original data restoration missing')
            if result['apks'] != ordinary_apks:
                raise ValueError('Diagnostic actual cohort differs')
            report.update(ordinaryDiagnosticWorkflowCompleted=True,
                          ordinaryDiagnosticHprofProduced=(diagnostic/'evidence/high-heap.hprof').is_file())
            save()
            diagnostic_previous = diagnostic
            different_cohort = True
            # The independently built primitive-water fix needs its own actual
            # untouched ordinary384 matrix. Do not use the diagnostic's postGC
            # numbers, the old68 functional pass or host allocation reduction.
            fixed_build = json.loads(HELPER.with_name('WATER_NORMAL384_BUILD81.json').read_text())
            fixed_apks = {r['path']: r['sha256'] for r in fixed_build['apks']}
            if not fixed_build['buildSuccessful'] or not all(sha(pathlib.Path(p)) == h for p, h in fixed_apks.items()):
                raise ValueError('Independent ordinary water-fix cohort changed')
            fixed_game = next(p for p in fixed_apks if pathlib.Path(p).name == 'app-debug.apk')
            fixed_test = next(p for p in fixed_apks if pathlib.Path(p).name == 'app-debug-androidTest.apk')
            fixed = args.output/'ordinary384-water-fix-all16'
            report.update(activeStage='ordinary384-water-fix-all16', waterFixActualSession=str(fixed.resolve()))
            save()
            subprocess.run([sys.executable, str(HELPER), 'reuse-backup', '--output', str(fixed),
                            '--previous', str(diagnostic)], cwd=ROOT, check=True)
            with (fixed/'driver.log').open('w') as log:
                subprocess.run([sys.executable, str(HELPER), 'install-test', '--output', str(fixed),
                    '--apk', fixed_game, '--test-apk', fixed_test,
                    '--runner', 'SessionAMapRepairInstrumentation', '--suite', 'factions16',
                    '--begin', '0', '--end', '16', '--fresh-process-reopen'],
                    cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
            fixed_state = json.loads((fixed/'session.json').read_text())
            fixed_restore = fixed_state.get('restoration', {})
            if fixed_state['stage'] != 'restored-verified' or not fixed_state.get('passed') or not fixed_state.get('coldProcess', {}).get('passed'):
                raise ValueError('New ordinary water-fix normal/cold acceptance incomplete')
            if fixed_state['apks'] != fixed_apks or set(fixed_restore) != {'internal', 'external'} or not all(r['exactRegularFileSha'] for r in fixed_restore.values()):
                raise ValueError('New ordinary water-fix exact install/full restoration missing')
            for source in range(16):
                p = json.loads((fixed/'evidence'/('source-'+str(source)+'-faction-previews.json')).read_text())
                if p['sourceIndex'] != source or not p['noAuthorityMutation'] or not all(r.get('actual3DVerified') for r in p['slots'] if r['enabled']):
                    raise ValueError('New ordinary water-fix actual source/faction coverage missing')
            report.update(waterFixActualNormalColdWorkflowCompleted=True, memoryBudgetClosed=False)
            save()
            diagnostic_previous = fixed
        subprocess.run([sys.executable, str(HELPER), 'reuse-backup', '--output', str(fast),
                        '--previous', str(diagnostic_previous)], cwd=ROOT, check=True)
        with (fast/'driver.log').open('w') as log:
            subprocess.run([sys.executable, str(HELPER), 'install-test', '--output', str(fast),
                '--apk', str(game), '--test-apk', str(fast_test),
                *([] if different_cohort else ['--reuse-installed']),
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
