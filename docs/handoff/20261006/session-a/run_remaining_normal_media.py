#!/usr/bin/env python3
"""Strict sequential actual normal-source callers; no fabricated state or skips.

Requires a prior all-person normal/cold/every-file-restoration source run.
Each next source repeats the real menu/scenario/caller/save/reopen path with
the exact same independently installed game/test bytes. Failed source stops.
"""
import argparse
import hashlib
import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[4]
HELPER = pathlib.Path(__file__).with_name('device_session.py')


def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda: f.read(1048576), b''):
            h.update(chunk)
    return h.hexdigest()


def accepted(session, source):
    state = json.loads((session/'session.json').read_text())
    if state['root'] != str(ROOT) or state['serial'] != 'emulator-5554':
        raise ValueError('Wrong complete source/device owner')
    if state['stage'] != 'restored-verified' or not state.get('passed'):
        raise ValueError('Actual normal/cold/full restoration not accepted')
    cold = state.get('coldProcess', {})
    if not cold.get('passed') or not cold.get('differentPid'):
        raise ValueError('Actual different-PID cold reopen required')
    restore = state.get('restoration', {})
    if set(restore) != {'internal', 'external'} or not all(r.get('exactRegularFileSha') for r in restore.values()):
        raise ValueError('Complete regular file SHA restoration required')
    proof = json.loads((session/'evidence'/('source-'+str(source)+'-portrait-callers.json')).read_text())
    if not (proof['sourceIndex'] == source and proof['sourceComplete']
            and proof['allOriginalOfficerCallers'] and proof['portraitNative'] == -1
            and proof['actualNormalMenuRosterDetail'] and proof['fullSaveRngStateTokenPure']
            and proof['expectedOfficers'] > 0
            and proof['actualVisitedOfficers'] == proof['expectedOfficers']
            and len(proof['rows']) == 2*proof['expectedOfficers']):
        raise ValueError('Actual whole original normal roster/detail source proof required')
    callers = {}
    for row in proof['rows']:
        key = row['officerId']
        callers.setdefault(key, []).append(row['caller'])
    if len(callers) != proof['expectedOfficers'] or any(sorted(pair) != ['normal-detail', 'normal-roster'] for pair in callers.values()):
        raise ValueError('Exactly one actual roster/detail pair for every unique stable officer required')
    if not all(row['fullBitmapSameAs'] for row in proof['rows']):
        raise ValueError('Actual complete original pixels required for every requested caller')
    for path, digest in state['apks'].items():
        if sha(pathlib.Path(path)) != digest:
            raise ValueError('Frozen APK changed: '+path)
    return state, proof



def frozen_caller_cohort(artifacts):
    names=('FIRE_OVERRIDE_DEFAULT_BUILD90.json','PICKER_RELEASE_BUILD116.json',
           'NORMAL_VIEW_OPTION_TEST_BUILD124.json','CURRENT_NORMAL_TARGET_BUILD139.json',
           'NORMAL_PREPARATION_BUILD145.json','OVERLAY_ADMISSION_BUILD155.json',
           'EMPTY_PRESENTATION_BUILD165.json','B_LEGACY39_COMBINED_TEST_BUILD168.json',
           'LEGACY39_REGISTERED_TEST_BUILD176.json','SEARCH_OBSERVATION_TEST_BUILD239.json','MAP_FIRE_UPLOAD_BUILD269.json')
    for name in names:
        path=HELPER.with_name(name)
        if not path.is_file():continue
        build=json.loads(path.read_text())
        if build.get('buildSuccessful') and {row['path']:row['sha256'] for row in build['apks']}==artifacts:
            return name,build['sourceRevision']
    raise ValueError('No registered complete frozen caller cohort; fresh audited receipt required')


def guard_caller_source(revision, expected_main):
    protected=pathlib.Path(json.loads(HELPER.with_name('INHERITANCE.json').read_text())['source'])
    actual_main=subprocess.check_output(['git','-C',str(protected),'rev-parse','main'],text=True).strip()
    if actual_main!=expected_main:
        raise ValueError('New main requires complete inheritance before another source install')
    changed=subprocess.check_output(['git','diff','--name-only',revision,'--','app/src','app/build.gradle','core','game-api','game-runtime'],cwd=ROOT,text=True).strip()
    if changed:
        raise ValueError('Source or tracked WIP changed; fresh complete caller APK cohort required: '+changed)


def main():
    parser = argparse.ArgumentParser()
    anchors = parser.add_mutually_exclusive_group(required=True)
    anchors.add_argument('--previous-source11', type=pathlib.Path)
    anchors.add_argument('--previous-completed', type=pathlib.Path)
    parser.add_argument('--initial-source', type=int, choices=range(16),
                        help='Required with --previous-completed; only this actually completed source is credited')
    parser.add_argument('--output', type=pathlib.Path, required=True)
    parser.add_argument('--test-only-update-first', action='store_true',
                        help='Restore the frozen test cohort through the first remaining source with complete backup and actual test APK installation')
    args = parser.parse_args()
    if args.previous_source11 is not None:
        if args.initial_source not in (None, 11):
            parser.error('--previous-source11 requires source11')
        initial_source = 11
        previous = args.previous_source11.resolve()
    else:
        if args.initial_source is None:
            parser.error('--previous-completed requires --initial-source')
        initial_source = args.initial_source
        previous = args.previous_completed.resolve()
    baseline, _ = accepted(previous, initial_source)
    artifacts = baseline['apks']
    cohort_receipt,source_revision=frozen_caller_cohort(artifacts)
    expected_main=json.loads(HELPER.with_name('INHERITANCE.json').read_text())['main']
    guard_caller_source(source_revision,expected_main)
    game = next(pathlib.Path(p) for p in artifacts if pathlib.Path(p).name == 'app-debug.apk')
    test = next(pathlib.Path(p) for p in artifacts if pathlib.Path(p).name == 'app-debug-androidTest.apk')
    args.output.mkdir(parents=True, exist_ok=False)
    report = {'scope': 'Real menu/normal original identity roster-detail/current-year pixels/fullSaveRNGStateToken/save/newPID and every original file restoration for each source. Not fullscreen/allage/voice/originalPC timing/GPU/ARM or whole goal completion.',
              'device': 'emulator-5554', 'apks': artifacts,
              'cohortReceipt':cohort_receipt,'sourceRevision':source_revision,'expectedMain':expected_main,
              'initialSource': initial_source, 'initialCompletedSession': str(previous),
              'completedSources': [initial_source],
              'sessions': {str(initial_source): str(previous)}, 'completeAll16NormalCallers': False,
              'wholeGoalComplete': False, 'testOnlyUpdateFirst': args.test_only_update_first}
    record = args.output/'batch.json'

    def save():
        record.write_text(json.dumps(report, indent=2)+'\n')

    save()
    for index, source in enumerate(s for s in range(16) if s != initial_source):
        target = args.output/('source-'+str(source).zfill(2))
        report['activeSource'] = source
        save()
        print('Actual normal source '+str(source)+' starting, previous all-files restoration verified', flush=True)
        try:
            guard_caller_source(source_revision,expected_main)
            subprocess.run([sys.executable, str(HELPER), 'reuse-backup',
                            '--output', str(target), '--previous', str(previous)], cwd=ROOT, check=True)
            try:
                guard_caller_source(source_revision,expected_main)
            except BaseException:
                # No installation began. Release this own verified backup lock
                # through the same complete SHA restoration path, never clearing data.
                subprocess.run([sys.executable,str(HELPER),'restore','--output',str(target)],cwd=ROOT,check=True)
                report['backupOnlyRestoredAfterSourceGuardRejection']=str(target.resolve())
                raise
            with (target/'driver.log').open('w') as log:
                result = subprocess.run([sys.executable, str(HELPER), 'install-test',
                    '--output', str(target), '--apk', str(game), '--test-apk', str(test),
                    *(['--test-only-update'] if args.test_only_update_first and index == 0 else ['--reuse-installed']),
                    '--observe-workers', '--runner', 'SessionAMapRepairInstrumentation',
                    '--suite', 'mediaAll16', '--begin', str(source), '--end', str(source+1),
                    '--fresh-process-reopen'], cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
            actual, proof = accepted(target, source)
            if result.returncode or actual['apks'] != artifacts:
                raise ValueError('Failed real source or changed installed APK cohort')
        except BaseException as error:
            report['stoppedSource'] = source
            report['error'] = str(error)
            # Never claim cleanup from this exception. The helper has its own
            # complete restoration receipt; inspect it before any new device use.
            report['needsActualRestorationInspection'] = True
            save()
            raise
        report['completedSources'].append(source)
        report['sessions'][str(source)] = str(target.resolve())
        report['lastActualCallerCount'] = proof['actualVisitedOfficers']
        save()
        print('Actual source '+str(source)+' normal all'+str(proof['actualVisitedOfficers'])+
              ' caller pairs/cold/fulloriginalSHA restoration accepted', flush=True)
        previous = target.resolve()
    report.pop('activeSource', None)
    report['completeAll16NormalCallers'] = sorted(report['completedSources']) == list(range(16))
    save()
    print('Actual normal all16 caller batch complete; other media/ARM/whole goal remain open', flush=True)


if __name__ == '__main__':
    main()
