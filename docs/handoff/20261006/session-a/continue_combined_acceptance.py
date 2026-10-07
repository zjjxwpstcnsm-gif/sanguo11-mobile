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

from run_remaining_normal_media import ROOT, HELPER, accepted


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source11', type=pathlib.Path, required=True)
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
    deadline = time.monotonic()+14460
    try:
        while time.monotonic() < deadline:
            state = json.loads((source/'session.json').read_text())
            if state['stage'] == 'restored-verified':
                break
            time.sleep(5)
        else:
            raise TimeoutError('Preceding actual run has not restored; no next device action')
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
