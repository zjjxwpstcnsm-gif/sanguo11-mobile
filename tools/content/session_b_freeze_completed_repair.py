#!/usr/bin/env python3
"""Freeze exact completed-parent compilation separately from all own Duel WIP.

Current uncompiled production is additionally guarded against accidental writes.
"""
import argparse
import json
import shutil
import subprocess
from pathlib import Path
from session_b_freeze_apk import ROOT, paths, sha

STAGE = 'out/session-b/completed-repair-stage-v4/'


def effective():
    return sorted(set(paths()) | {str(p.relative_to(ROOT)) for p in (ROOT / STAGE).rglob('*') if p.is_file()})


def guarded(name):
    return name.startswith(('app/src/main/', 'core/src/main/', 'game-api/src/main/', 'game-runtime/src/main/', 'out/pc-native-runtime/', 'out/session-b/readonly-theme-dependencies/', STAGE)) or name in ['app/build.gradle', 'build.gradle', 'settings.gradle', 'gradle.properties', 'version.properties']


def main(args):
    folder = ROOT / 'out/session-b' / args.label
    if args.mode == 'capture':
        folder.mkdir(exist_ok=False)
        stage = json.loads((ROOT / STAGE / 'manifest.json').read_text())
        if stage['base'] != subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip():
            raise ValueError('Completed parent changed before build')
        for row in stage['compiled']:
            if sha(ROOT / row['path']) != row['compiledSha256']:
                raise ValueError('Compiled stage differs')
        rows = [dict(path=name, bytes=(ROOT / name).stat().st_size, sha256=sha(ROOT / name)) for name in effective()]
        report = dict(head=stage['base'], files=rows, compiledSource=stage, duelWipCompiled=False, buildConfig='completed-repair.init.gradle; separate out/session-b/completed-repair-build and project cache')
        (folder / 'build-inputs.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
        print('Captured completed repair', len(rows), 'preserved/effective inputs; staged parent', stage['base'])
        return
    report = json.loads((folder / 'build-inputs.json').read_text())
    if effective() != [row['path'] for row in report['files']] or any(not (ROOT / row['path']).is_file() or sha(ROOT / row['path']) != row['sha256'] for row in report['files']):
        raise ValueError('Build input set or bytes changed')
    frozen = folder / 'frozen'
    frozen.mkdir(exist_ok=False)
    artifacts = []
    for rel in ['outputs/apk/debug/app-debug.apk', 'outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
        source = ROOT / 'out/session-b/completed-repair-build/app' / rel
        target = frozen / source.name
        shutil.copy2(source, target)
        if sha(source) != sha(target):
            raise ValueError('Artifact copy differs')
        artifacts.append(dict(path=str(target), bytes=target.stat().st_size, sha256=sha(target)))
    (frozen / 'source-guard.json').write_text(json.dumps({row['path']: row['sha256'] for row in report['files'] if guarded(row['path'])}, indent=2) + '\n')
    report.update(artifacts=artifacts, inputsUnchanged=True, compiledThemeDependencies=json.loads((ROOT / 'out/session-b/readonly-theme-dependencies/manifest.json').read_text()))
    (folder / 'frozen-report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(artifacts, indent=2))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('mode', choices=['capture', 'freeze'])
    p.add_argument('label')
    a = p.parse_args()
    if not a.label.replace('-', '').isalnum():
        raise ValueError('Unique simple label required')
    main(a)
