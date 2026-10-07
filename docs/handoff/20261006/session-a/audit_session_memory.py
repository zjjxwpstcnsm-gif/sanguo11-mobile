#!/usr/bin/env python3
"""Consolidate exact-cohort completed observations; never sum independent peaks."""
import argparse
import csv
import hashlib
import json
import pathlib
import re


def sha(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1048576), b''):
            digest.update(block)
    return digest.hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--session', type=pathlib.Path, required=True)
    parser.add_argument('--output', type=pathlib.Path, required=True)
    args = parser.parse_args()
    base = args.session.resolve()
    state = json.loads((base/'session.json').read_text())
    assert state['stage'] == 'restored-verified' and state['passed']
    assert state['serial'] == 'emulator-5554'
    assert state['coldProcess']['passed'] and state['coldProcess']['differentPid']
    assert set(state['restoration']) == {'internal', 'external'}
    assert all(row['exactRegularFileSha'] for row in state['restoration'].values())
    assert all(sha(pathlib.Path(path)) == digest for path, digest in state['apks'].items())
    files, samples = [], []
    for group in ('evidence', 'cold-evidence'):
        path = base/group/'memory.csv'
        if not path.is_file():
            continue
        files.append(dict(path=str(path), sha256=sha(path)))
        for index, row in enumerate(csv.DictReader(path.open())):
            record = {key: int(value) if key != 'phase' else value for key, value in row.items()}
            assert 0 <= record['javaUsed'] <= record['javaTotal'] <= record['javaLimit']
            samples.append(dict(group=group, sampleIndex=index, **record))
    assert samples
    java = max(samples, key=lambda row: row['javaUsed'])
    native = max(samples, key=lambda row: row['nativeAllocated'])
    pss = max(samples, key=lambda row: row['totalPss'])
    timeline = base/'meminfo-timeline.txt'
    timeline_values = []
    if timeline.is_file():
        timeline_values = [int(value) for value in re.findall(r'TOTAL PSS:\s*(\d+)', timeline.read_text(errors='replace'))]
        files.append(dict(path=str(timeline), sha256=sha(timeline)))
    children, unavailable = [], []
    child_path = base/'native-workers.jsonl'
    if child_path.is_file():
        files.append(dict(path=str(child_path), sha256=sha(child_path)))
        for index, line in enumerate(child_path.read_text().splitlines()):
            row = json.loads(line)
            # Earlier manually started observer schema lacked APK bindings; its
            # explicit session/cohort provenance must be retained by the receipt.
            if row.get('apks'):
                assert row['apks'] == state['apks']
            if 'unavailable' in row:
                unavailable.append(dict(sampleIndex=index, reason=row['unavailable']))
            children.append(dict(sampleIndex=index, time=row['time'],
                sourceChildren=row['sourceChildren'],
                batchChildPssKiB=sum(item['KiB'].get('Pss', 0) for item in row['sourceChildren'])))
    child_peak = max(children, key=lambda row: row['batchChildPssKiB']) if children else None
    report = dict(actualSession=str(base), actualSessionSha256=sha(base/'session.json'),
        apks=state['apks'], normalColdFullRestorationPassed=True,
        heapProfileDiagnostic=state.get('heapProfileDiagnostic', False),
        samples=len(samples), limitsBytes=sorted({row['javaLimit'] for row in samples}),
        peakJavaSample=java, sampledJavaHeadroomAtPeakBytes=java['javaLimit']-java['javaUsed'],
        peakNativeAllocatorSampleIndependent=native, peakInstrumentationPssSampleIndependent=pss,
        peakMeminfoPssKiBIndependent=max(timeline_values) if timeline_values else None,
        sourceChildObservationBatches=len(children), sourceChildPeakBatchIndependent=child_peak,
        sourceChildReadErrors=unavailable, graphicsPssReportedValues=sorted({row['graphicsPss'] for row in samples}),
        gpu='Not established. Android reported graphicsPss, including zero, is not proof of GPU VRAM usage.',
        rawFiles=files, memoryBudgetClosed=False, wholeGoalComplete=False,
        scope='Only exact-cohort completed sampled telemetry. CSV allocator/Java/mainPSS, local meminfo and source-child smaps are independent observations. Child batch reads are sequential, not synchronized with main peaks. No sum of independent peaks, allocation stack, dominator/leak attribution, untouched peak when recording/GC present, ARM or newer APK acceptance. Budget remains open pending required full stress and device evidence.')
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({key: report[key] for key in ('samples', 'limitsBytes', 'peakJavaSample', 'sampledJavaHeadroomAtPeakBytes', 'sourceChildObservationBatches', 'memoryBudgetClosed')}))


if __name__ == '__main__':
    main()
