#!/usr/bin/env python3
"""Verify immutable B candidate, stage A app for compilation, never install."""
from pathlib import Path
import hashlib,json,shutil,tarfile,subprocess
import argparse
ROOT=Path(__file__).resolve().parents[4]
DOC=ROOT/'docs/handoff/20261006/session-a'
B=Path('/Users/paopao/.codex/worktrees/f55b/sanguo11-mobile/out/session-b/native-candidate60')
OUT=ROOT/'out/session-a/native-opening-stage224'
def sha(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        for v in iter(lambda:f.read(1048576),b''):h.update(v)
    return h.hexdigest()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--apply-reviewed-adapter',action='store_true');args=parser.parse_args()
    assert not OUT.exists(),'Stage already exists; do not overwrite evidence'
    assert shutil.disk_usage(ROOT).free>4*1024**3
    assert sha(B/'frozen-report.json')=='a8a61bd12deb88fcdc69033d801e2c2e6a12c03f8f1869a1e838ef3e7fe1b324'
    r=json.loads((B/'frozen-report.json').read_text())
    assert sha(B/'b-overlay.json')=='85637d49ff13ac869b3141e6a398547ebec064fbfd2103eb290fd9dd866d6b6b'
    assert sha(B/'b-overlay.tar.gz')==r['overlayArchive']['sha256']
    artifacts=[]
    for x in r['artifacts']:
        p=Path(x['path']);assert p.stat().st_size==x['bytes'] and sha(p)==x['sha256'];artifacts.append(x)
    overlay={x['path']:x for x in r['bOverlay']};conflicts=[];unchanged=[]
    for path,x in overlay.items():
        assert x['owner']=='B' and not Path(path).is_absolute() and '..' not in Path(path).parts
        p=ROOT/path;actual=sha(p) if p.exists() else None
        if actual not in (x['beforeSha256'],x['afterSha256']):conflicts.append(path)
    for x in r['files']:
        path=x['path']
        if path in overlay or not path.startswith(('core/','game-api/','game-runtime/','data/content/')):continue
        p=ROOT/path
        assert p.is_file() and sha(p)==x['sha256'],path
        unchanged.append(x)
    assert not conflicts,conflicts
    with tarfile.open(B/'b-overlay.tar.gz') as t:
        assert {m.name for m in t.getmembers()}==set(overlay)
        for m in t.getmembers():
            assert m.isfile()
            content=t.extractfile(m).read();assert hashlib.sha256(content).hexdigest()==overlay[m.name]['afterSha256']
    OUT.mkdir(parents=True)
    shutil.copytree(ROOT/'app/src/main',OUT/'app/src/main')
    aBefore={}
    for name in ['MainActivity','ScenarioFactionPicker']:
        path='app/src/main/java/game/sanguo/mobile/'+name+'.java'
        aBefore[path]=sha(ROOT/path)
        p=OUT/'before'/path;p.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/path,p)
    with tarfile.open(B/'b-overlay.tar.gz') as t:
        for name in ['ContestUi','DomesticUi']:
            path='app/src/main/java/game/sanguo/mobile/'+name+'.java'
            (OUT/path).write_bytes(t.extractfile(path).read())
    deps=OUT/'dependencies';deps.mkdir()
    for x in artifacts:
        if x['path'].endswith('.jar'):shutil.copy2(x['path'],deps/Path(x['path']).name)
    report={'scope':'A-only staged app compilation against immutable candidate60 JARs and two frozen B pages; not full source inheritance, integration, APK or install',
        'root':str(ROOT),'stage':str(OUT),'aHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
        'bReportSha':sha(B/'frozen-report.json'),'overlaySha':sha(B/'b-overlay.tar.gz'),'verifiedOverlayPaths':len(overlay),
        'verifiedUnchangedBDependencies':len(unchanged),'artifacts':artifacts,'aBefore':aBefore,'conflicts':conflicts,'installed':False}
    (DOC/'OPENING_STAGE224.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    if args.apply_reviewed_adapter:
        delta=json.loads((DOC/'OPENING_ADAPTER224.json').read_text())
        assert sha(DOC/'OPENING_ADAPTER224.patch')==delta['patchSha256']
        assert aBefore==delta['aBefore']
        subprocess.run(['git','apply','--no-index',str(DOC/'OPENING_ADAPTER224.patch')],cwd=OUT,check=True)
        for x in delta['aPaths']:assert sha(OUT/x['path'])==x['afterSha256']
    print(json.dumps(report,ensure_ascii=False))
if __name__=='__main__':main()
