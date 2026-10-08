#!/usr/bin/env python3
"""Immutable full-input candidate and strictly B-owned overlay for A adaptation.
No acceptance claim, installation, branch reset or A-file mutation.
"""
import argparse,hashlib,json,shutil,subprocess,tarfile
from pathlib import Path
from session_b_freeze_apk import ROOT,paths,sha

FOLDER=ROOT/'out/session-b/native-candidate60'
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
def owned(name,app):
    return name.startswith(('core/','game-api/','game-runtime/','data/content/')) or app.get(name)=='B'
def bundle(file,names):
    with tarfile.open(file,'w:gz',compresslevel=6,dereference=True)as archive:
        for name in names:archive.add(ROOT/name,arcname=name,recursive=False)
def main(mode):
    if mode=='capture':
        FOLDER.mkdir(parents=True,exist_ok=False)
        names=paths();rows=[dict(path=n,bytes=(ROOT/n).stat().st_size,sha256=sha(ROOT/n))for n in names]
        app={r['path']:r['owner']for r in json.loads((ROOT/'docs/handoff/20261006/parallel-repair/APP_OWNERSHIP.json').read_text())['files']}
        changed=set(x.decode()for x in git('diff','--name-only','-z').split(b'\0')if x)
        changed.update(x.decode()for x in git('ls-files','--others','--exclude-standard','-z').split(b'\0')if x)
        overlay=[]
        for name in sorted(changed):
            if not owned(name,app):continue
            if not(ROOT/name).is_file():raise ValueError('Deletion needs explicit conflict contract: '+name)
            old=subprocess.run(['git','show','HEAD:'+name],cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
            overlay.append(dict(path=name,beforeSha256=hashlib.sha256(old.stdout).hexdigest()if old.returncode==0 else None,afterSha256=sha(ROOT/name),bytes=(ROOT/name).stat().st_size,owner='B'))
        report=dict(head=git('rev-parse','HEAD').decode().strip(),main=git('rev-parse','main').decode().strip(),branch=git('branch','--show-current').decode().strip(),files=rows,bOverlay=overlay,complete=False,candidateOnly=True,installed=False)
        (FOLDER/'build-inputs.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n')
        print('PASS captured immutable candidate',len(rows),'full inherited inputs',len(overlay),'B-owned incremental paths',sum(r['bytes']for r in rows),'bytes',flush=True)
        return
    report=json.loads((FOLDER/'build-inputs.json').read_text());names=[r['path']for r in report['files']]
    if names!=paths():raise ValueError('Input path set changed during build')
    for r in report['files']:
        if not(ROOT/r['path']).is_file()or sha(ROOT/r['path'])!=r['sha256']:raise ValueError('Input changed during build: '+r['path'])
    artifacts={
        'core.jar':'core/build/libs/core.jar','game-api.jar':'game-api/build/libs/game-api.jar','game-runtime.jar':'game-runtime/build/libs/game-runtime.jar',
        'app-debug.apk':'app/build/outputs/apk/debug/app-debug.apk','app-debug-androidTest.apk':'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'}
    frozen=FOLDER/'frozen';frozen.mkdir(exist_ok=False);records=[]
    for name,rel in artifacts.items():
        target=frozen/name;shutil.copy2(ROOT/rel,target);assert sha(ROOT/rel)==sha(target)
        records.append(dict(path=str(target),bytes=target.stat().st_size,sha256=sha(target)))
    bundle(FOLDER/'candidate-source.tar.gz',names)
    bundle(FOLDER/'b-overlay.tar.gz',[r['path']for r in report['bOverlay']])
    for r in report['files']:
        if sha(ROOT/r['path'])!=r['sha256']:raise ValueError('Input changed while archiving: '+r['path'])
    overlay=dict(sourceHead=report['head'],main=report['main'],files=report['bOverlay'],archiveSha256=sha(FOLDER/'b-overlay.tar.gz'),candidateOnly=True,complete=False,policy='A checks its own base/current paths; only registered B paths may overlay, no A WIP or frozen bridge/Unity/JNI change')
    (FOLDER/'b-overlay.json').write_text(json.dumps(overlay,indent=2,ensure_ascii=False)+'\n')
    report.update(inputsUnchanged=True,artifacts=records,sourceArchive=dict(path=str(FOLDER/'candidate-source.tar.gz'),sha256=sha(FOLDER/'candidate-source.tar.gz'),bytes=(FOLDER/'candidate-source.tar.gz').stat().st_size),overlayArchive=dict(path=str(FOLDER/'b-overlay.tar.gz'),sha256=sha(FOLDER/'b-overlay.tar.gz'),bytes=(FOLDER/'b-overlay.tar.gz').stat().st_size),overlayManifestSha=sha(FOLDER/'b-overlay.json'))
    (FOLDER/'frozen-report.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n')
    print('PASS immutable B integration candidate',json.dumps(records),flush=True)
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('mode',choices=['capture','freeze']);main(p.parse_args().mode)
