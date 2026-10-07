#!/usr/bin/env python3
"""Freeze completed real normal source evidence; never operate the device."""
import argparse
import json
import pathlib
import re
import subprocess
from run_remaining_normal_media import ROOT, accepted, frozen_caller_cohort, sha


def audit(case, source, memory_path, widget_path, helper_pid):
    state, proof = accepted(case, source)
    cohort, revision = frozen_caller_cohort(state['apks'])
    assert state.get('normalPassed') and not state.get('error')
    cold = state['coldProcess']
    assert all(cold[k]['rendererFirstSubmissionObserved'] for k in ('beforeProcessEvidence', 'afterProcessEvidence'))
    assert re.fullmatch('[0-9a-f]{64}', cold['expectedStartupSaveSha256'])
    observers = [state['videoObservation'], state['workerObservation']]
    assert all(item['exitCode'] == 0 for item in observers)
    video = state['videoObservation']
    assert video['completedOriginalParts'] > 0 and not video['captureLimitReachedBeforeRestoration']
    assert sha(pathlib.Path(video['path'])) == video['finalIndexSha256']
    terminal_pids = [helper_pid, *[item['hostPid'] for item in observers]]
    for pid in terminal_pids:
        actual = subprocess.run(['ps', '-p', str(pid), '-o', 'command='], capture_output=True, text=True)
        assert not (actual.returncode == 0 and str(case) in actual.stdout), 'Original data owner/observer still running'
    audits = [json.loads(path.read_text()) for path in (memory_path, widget_path)]
    assert all(item['actualSession'] == str(case) and item['apks'] == state['apks'] for item in audits)
    assert audits[0]['normalColdFullRestorationPassed'] and not audits[0]['memoryBudgetClosed']
    assert audits[0]['actualSessionSha256'] == audits[1]['sessionSha256'] == sha(case/'session.json')
    def checks(filename):
        text = (case/filename).read_text()
        found = re.findall(r'SESSION_A_MAP PASS (\d+) checks', text)
        assert len(found) == 1, 'Actual instrumentation PASS required'
        return int(found[0])
    return dict(actualCase=str(case), actualSessionSha256=sha(case/'session.json'),
        apks=state['apks'], cohortReceipt=cohort, frozenTestSourceRevision=revision,
        sourceIndex=source, actualOfficers=proof['actualVisitedOfficers'],
        actualRosterDetailPairs=len(proof['rows']), everyUniqueOfficerHasBothRealCallers=True,
        originalCompleteBitmapSameAsAllRows=True, fullSaveBothRngStateTokenPure=True,
        normalChecks=checks('instrumentation.txt'), coldChecks=checks('cold-instrumentation.txt'),
        coldProcess=cold, restoration=state['restoration'], videoObservation=video,
        workerObservation=state['workerObservation'], terminatedOriginalHostPids=terminal_pids,
        proofPath=str(case/'evidence'/f'source-{source}-portrait-callers.json'),
        proofSha256=sha(case/'evidence'/f'source-{source}-portrait-callers.json'),
        memoryAudit=str(memory_path), memoryAuditSha256=sha(memory_path),
        widgetAudit=str(widget_path), widgetAuditSha256=sha(widget_path),
        all16NormalCallersAccepted=False, allAgeBoundarySmallCropFullscreenVoiceAccepted=False,
        armAccepted=False, wholeGoalComplete=False,
        scope='Only this exact independently installed cohort and source: complete original roster/detail identities/current-year age/sex/source-form/full bitmap binding, complete presentation Save/allRNG/StateToken purity, normal menu/save/trueBack/newPID/both3D/every originalSHA. Does not prove original small-family crop/UI pixels/all ages/fullscreen/real voice/mapBGM/GPU/ARM or other sources.')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--session', type=pathlib.Path, required=True)
    parser.add_argument('--source', type=int, required=True)
    parser.add_argument('--helper-pid', type=int, required=True)
    parser.add_argument('--memory-audit', type=pathlib.Path, required=True)
    parser.add_argument('--widget-audit', type=pathlib.Path, required=True)
    parser.add_argument('--output', type=pathlib.Path, required=True)
    args = parser.parse_args()
    assert 0 <= args.source < 16
    case = args.session.resolve()
    output = args.output.resolve()
    assert case.is_relative_to(ROOT/'out/session-a') and output.parent == pathlib.Path(__file__).parent
    assert not output.exists(), 'Preserve previous frozen receipt'
    report = audit(case, args.source, args.memory_audit.resolve(), args.widget_audit.resolve(), args.helper_pid)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({key: report[key] for key in ('sourceIndex', 'actualOfficers', 'actualRosterDetailPairs', 'normalChecks', 'coldChecks', 'wholeGoalComplete')}))


if __name__ == '__main__':
    main()
