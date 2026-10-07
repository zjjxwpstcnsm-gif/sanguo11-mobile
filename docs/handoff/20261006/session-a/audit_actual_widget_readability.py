#!/usr/bin/env python3
"""Audit actual focused widget recordings, preserving APK and screenshot scope."""
import argparse
import collections
import hashlib
import json
import pathlib


def sha(path):
    value = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1048576), b''):
            value.update(block)
    return value.hexdigest()


def contrast(foreground, background):
    def luminance(argb):
        rgb = [(int(argb, 16) >> shift & 255) / 255 for shift in (16, 8, 0)]
        linear = [x / 12.92 if x <= .04045 else ((x + .055) / 1.055) ** 2.4 for x in rgb]
        return sum(a * b for a, b in zip(linear, (.2126, .7152, .0722)))
    a, b = luminance(foreground), luminance(background)
    return (max(a, b) + .05) / (min(a, b) + .05)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--session', type=pathlib.Path, required=True)
    parser.add_argument('--output', type=pathlib.Path, required=True)
    args = parser.parse_args()
    base = args.session.resolve()
    session_path = base / 'session.json'
    state = json.loads(session_path.read_text())
    assert state['stage'] == 'restored-verified' and state['passed']
    assert state['serial'] == 'emulator-5554'
    assert state['coldProcess']['passed'] and state['coldProcess']['differentPid']
    assert set(state['restoration']) == {'internal', 'external'}
    assert all(row['exactRegularFileSha'] for row in state['restoration'].values())
    assert all(sha(pathlib.Path(path)) == digest for path, digest in state['apks'].items())
    counts = collections.Counter()
    samples, findings = [], []
    minimum = None
    for path in sorted(base.rglob('*actual-text.json')):
        record = json.loads(path.read_text())
        screenshot = path.parent / record['normalScreenshot']
        assert screenshot.is_file(), screenshot
        sample = dict(path=str(path), sha256=sha(path), screenshot=str(screenshot),
                      screenshotSha256=sha(screenshot), widgetRows=0)
        counts['recordings'] += 1
        for root in record['focusedRoots']:
            assert root['actualFocusedRoot']
            for row in root['rows']:
                counts['widgetRows'] += 1
                sample['widgetRows'] += 1
                counts['enabledRows' if row['enabled'] else 'disabledRows'] += 1
                entries = [('text', row['textArgb'], row['textAlpha'], row)]
                for span in row['foregroundSpans']:
                    counts['foregroundSpans'] += 1
                    entries.append(('span', span['argb'], span['alpha'], span))
                for kind, color, alpha, entry in entries:
                    if color == 'ff000000':
                        counts['opaqueBlack'+kind.title()] += 1
                    if alpha == 0:
                        findings.append(dict(path=str(path), text=row['text'], kind=kind,
                                             argb=color, issue='transparent-foreground'))
                    if 'solidContrast' not in entry:
                        counts['unresolved'+kind.title()] += 1
                        continue
                    background = row['solidBackgroundArgb']
                    assert alpha == 255 and background.startswith('ff') and row['viewAlpha'] == 1
                    value = contrast(color, background)
                    assert abs(value - entry['solidContrast']) < 1e-9
                    counts['resolvedSolid'+kind.title()] += 1
                    minimum = value if minimum is None else min(minimum, value)
                    if not row['enabled']:
                        counts['resolvedDisabled'+kind.title()] += 1
                    if value < 4.5:
                        findings.append(dict(path=str(path), text=row['text'], kind=kind,
                                             argb=color, background=background, contrast=value,
                                             issue='solid-contrast-below-4.5'))
        samples.append(sample)
    assert samples
    result = dict(actualSession=str(base), sessionSha256=sha(session_path),
                  apks=state['apks'], counts=dict(counts), minimumResolvedSolidContrast=minimum,
                  findings=findings, recordings=samples,
                  resolvedWidgetMetadataPassed=not findings,
                  scope='Actual focused shown Android widgets in this exact installed cohort. Solid contrast recomputed from recorded drawable colors, not pixel measurement. Unresolved gradients/images/alpha, Canvas labels, offscreen/unvisited pages and rich text absent from these captures remain unaccepted. No transfer to a newer APK, full global UI or ARM acceptance.',
                  wholeGoalComplete=False)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({key: result[key] for key in ('counts', 'minimumResolvedSolidContrast', 'findings', 'resolvedWidgetMetadataPassed')}, ensure_ascii=False))


if __name__ == '__main__':
    main()
