#!/usr/bin/env python3
"""Summarize actual focused widget inventories; never treat missing pixels as pass."""
import argparse
import hashlib
import json
import pathlib


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('evidence', type=pathlib.Path, nargs='+')
    parser.add_argument('--output', type=pathlib.Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise ValueError('Fresh evidence output required')
    report = {'scope': 'Actual screenshot-associated focused Android widget inventories only. Known opaque solid background contrast is metadata. Gradients, images, Canvas/map labels and screenshot pixels remain unresolved; not global Android/PC/ARM UI acceptance.',
              'files': [], 'actualShownTextRows': 0, 'knownSolidContrastRows': 0,
              'backgroundUnresolvedRows': 0, 'zeroTextAlpha': [], 'zeroViewAlpha': [],
              'knownSolidContrastBelow4_5': [], 'zeroSpanAlpha': [],
              'knownSpanContrastBelow4_5': [], 'stateCounts': {},
              'completeGlobalReadability': False, 'completeGoal': False}
    files = sorted({p.resolve() for directory in args.evidence
                    for p in directory.rglob('*-actual-text.json')})
    if not files:
        raise ValueError('Actual normal screenshot widget inventory is absent')
    for path in files:
        raw = path.read_bytes()
        data = json.loads(raw)
        screenshot = path.with_name(data['normalScreenshot'])
        if not screenshot.is_file():
            raise ValueError('Referenced actual screenshot missing: '+str(screenshot))
        rows = 0
        for window in data['focusedRoots']:
            if not window['actualFocusedRoot']:
                raise ValueError('Inventory is not an actual focused root')
            for row in window['rows']:
                rows += 1
                report['actualShownTextRows'] += 1
                reference = {'inventory': str(path), 'screenshot': str(screenshot),
                             'text': row['text'], 'visibleBounds': row['visibleBounds'],
                             'class': row['class'], 'textArgb': row['textArgb']}
                state = 'enabled='+str(row['enabled'])+'/selected='+str(row['selected'])+'/focused='+str(row['focused'])
                report['stateCounts'][state] = report['stateCounts'].get(state, 0)+1
                if row['textAlpha'] == 0:
                    report['zeroTextAlpha'].append(reference)
                if row['viewAlpha'] == 0:
                    report['zeroViewAlpha'].append(reference)
                if 'solidContrast' in row:
                    report['knownSolidContrastRows'] += 1
                    if row['solidContrast'] < 4.5:
                        report['knownSolidContrastBelow4_5'].append({**reference, 'solidContrast': row['solidContrast']})
                else:
                    report['backgroundUnresolvedRows'] += 1
                for span in row['foregroundSpans']:
                    if span['alpha'] == 0:
                        report['zeroSpanAlpha'].append({**reference, 'span': span})
                    if span.get('solidContrast', 4.5) < 4.5:
                        report['knownSpanContrastBelow4_5'].append({**reference, 'span': span})
        report['files'].append({'path': str(path), 'sha256': hashlib.sha256(raw).hexdigest(),
                                'actualScreenshot': str(screenshot),
                                'screenshotSha256': hashlib.sha256(screenshot.read_bytes()).hexdigest(),
                                'actualFocusedTextRows': rows})
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({key: report[key] for key in ['actualShownTextRows', 'knownSolidContrastRows', 'backgroundUnresolvedRows', 'stateCounts']}))
    print(json.dumps({key: len(report[key]) for key in ['zeroTextAlpha', 'zeroViewAlpha', 'knownSolidContrastBelow4_5', 'zeroSpanAlpha', 'knownSpanContrastBelow4_5']}))


if __name__ == '__main__':
    main()
