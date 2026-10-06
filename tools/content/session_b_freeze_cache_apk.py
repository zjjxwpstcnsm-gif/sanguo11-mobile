#!/usr/bin/env python3
"""Freeze cache-only compiled core, completed A dependency and all inherited inputs without own governor WIP consumption."""
import argparse,hashlib,json,shutil,subprocess
from pathlib import Path
from session_b_freeze_apk import paths,ROOT,sha
STAGE='out/session-b/cache-core-stage/'
def effective():
    return sorted(set(paths())|{str(p.relative_to(ROOT))for p in (ROOT/STAGE).rglob('*')if p.is_file()})
def guarded(name):
    return name.startswith(('app/src/main/','core/src/main/','game-api/src/main/','game-runtime/src/main/','out/pc-native-runtime/','out/session-b/readonly-theme-dependencies/',STAGE))or name in ['app/build.gradle','build.gradle','settings.gradle','gradle.properties','version.properties']
def main(a):
    folder=ROOT/'out/session-b'/a.label
    if a.mode=='capture':
        folder.mkdir(exist_ok=False);rows=[dict(path=p,bytes=(ROOT/p).stat().st_size,sha256=sha(ROOT/p))for p in effective()]
        stage=json.loads((ROOT/STAGE/'manifest.json').read_text())
        for row in stage['compiled']:
            if sha(ROOT/row['path'])!=row['compiledSha256']:raise ValueError('Compiled stage differs')
        report=dict(head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),files=rows,compiledCore=stage,governanceWipCompiled=False)
        (folder/'build-inputs.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print('Captured',len(rows),'effective inherited/frozen/staged inputs');return
    report=json.loads((folder/'build-inputs.json').read_text())
    if effective()!=[r['path']for r in report['files']]:raise ValueError('Input path set changed')
    if any(not(ROOT/r['path']).is_file()or sha(ROOT/r['path'])!=r['sha256']for r in report['files']):raise ValueError('Build input bytes changed')
    frozen=folder/'frozen';frozen.mkdir(exist_ok=False);artifacts=[]
    for rel in ['app/build/outputs/apk/debug/app-debug.apk','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
        p=ROOT/rel;target=frozen/p.name;shutil.copy2(p,target)
        if sha(p)!=sha(target):raise ValueError('Artifact copy differs')
        artifacts.append(dict(path=str(target),bytes=target.stat().st_size,sha256=sha(target)))
    guard={r['path']:r['sha256']for r in report['files']if guarded(r['path'])};(frozen/'source-guard.json').write_text(json.dumps(guard,indent=2)+'\n')
    report.update(artifacts=artifacts,inputsUnchanged=True,compiledThemeDependencies=json.loads((ROOT/'out/session-b/readonly-theme-dependencies/manifest.json').read_text()))
    (folder/'frozen-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(artifacts,indent=2))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('mode',choices=['capture','freeze']);p.add_argument('label');a=p.parse_args()
    if not a.label.replace('-','').isalnum():raise ValueError('Unique simple build label required')
    main(a)
