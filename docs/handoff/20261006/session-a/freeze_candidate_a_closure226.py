#!/usr/bin/env python3
"""Freeze only completed A production prerequisites plus staged two-file adapter."""
from pathlib import Path
import json,hashlib,tarfile,io,subprocess,argparse
ROOT=Path(__file__).resolve().parents[4];DOC=ROOT/'docs/handoff/20261006/session-a'
B=Path('/Users/paopao/.codex/worktrees/f55b/sanguo11-mobile/out/session-b/native-candidate60')
def sha(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        for x in iter(lambda:f.read(1048576),b''):h.update(x)
    return h.hexdigest()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--revision',type=int,choices=[226,227],default=227);args=parser.parse_args()
    OUT=ROOT/f'out/session-a/candidate-a-closure{args.revision}'
    assert not OUT.exists()
    assert not subprocess.check_output(['git','diff','--name-only','HEAD','--','app','core','game-api','game-runtime'],cwd=ROOT).strip()
    assert sha(B/'frozen-report.json')=='a8a61bd12deb88fcdc69033d801e2c2e6a12c03f8f1869a1e838ef3e7fe1b324'
    b=json.loads((B/'frozen-report.json').read_text());bf={x['path']:x for x in b['files']}
    assert sha(Path(b['sourceArchive']['path']))==b['sourceArchive']['sha256']
    a=json.loads((ROOT/'out/session-a/source-checkpoint-current225-complete/source-files.json').read_text())
    own=json.loads((DOC/'OWNERSHIP.json').read_text());app=json.loads((ROOT/'docs/handoff/20261006/parallel-repair/APP_OWNERSHIP.json').read_text())
    owners={x['path']:x['owner'] for x in app['files']};registered=set(own['paths'])
    effective={};themePrefix='out/session-b/readonly-theme-dependencies/'
    for path,x in bf.items():
        if path.startswith(themePrefix+'java/'):effective['app/src/main/java/game/sanguo/mobile/'+Path(path).name]=x
        elif path.startswith(themePrefix+'res/'):effective['app/src/main/res/'+path.split(themePrefix+'res/',1)[1]]=x
    rows=[];contents={};canonical=[]
    for x in a['files']:
        path=x['path']
        isApp=path.startswith('app/src/main/') or path in ('app/build.gradle','app/proguard-rules.pro')
        isFire=path.startswith('out/session-a/pc-fire-runtime/additional-jniLibs/')
        if not isApp and not isFire:continue
        if owners.get(path)=='B' or '/bridge/' in path:continue
        if path.endswith('.java'):assert owners.get(path)=='A' or path in registered,path
        assert sha(ROOT/path)==x['sha256'],path
        staged=ROOT/'out/session-a/native-opening-stage224'/path
        p=staged if path.endswith(('/MainActivity.java','/ScenarioFactionPicker.java')) else ROOT/path
        digest=sha(p);base=bf.get(path);compiled=effective.get(path,base)
        if base and compiled and base['sha256']==digest and compiled['sha256']==digest:continue
        rows.append(dict(path=path,owner='A',candidateSourceBeforeSha256=base['sha256'] if base else None,
                         candidateCompileBeforeSha256=compiled['sha256'] if compiled else None,canonicalCompletedSha256=x['sha256'],afterSha256=digest,bytes=p.stat().st_size))
        contents[path]=p.read_bytes();canonical.append(path)
    # Verify every candidate before byte from the immutable source tar, including
    # the separately frozen three-file theme compilation subset.
    required={x['path'] for x in bf.values() if x['path'] in canonical}
    required|={effective[path]['path'] for path in canonical if path in effective}
    verified=set()
    with tarfile.open(Path(b['sourceArchive']['path']),'r:gz') as t:
        for m in t:
            path=m.name.removeprefix('sanguo11-mobile/')
            if path not in required:continue
            assert m.isfile();raw=t.extractfile(m).read();assert hashlib.sha256(raw).hexdigest()==bf[path]['sha256'],path;verified.add(path)
    assert verified==required,(required-verified)
    OUT.mkdir(parents=True);archive=OUT/'a-production-closure.tar.gz'
    with tarfile.open(archive,'w:gz') as t:
        for path in sorted(contents):
            raw=contents[path];m=tarfile.TarInfo(path);m.size=len(raw);m.mode=0o644;m.mtime=0;t.addfile(m,io.BytesIO(raw))
    with tarfile.open(archive) as t:
        assert {m.name for m in t.getmembers()}==set(contents)
        for row in rows:assert hashlib.sha256(t.extractfile(row['path']).read()).hexdigest()==row['afterSha256']
    report={'commonBase':a['commonBase'],'completedASourceRevision':a['revision'],'completedASourceArchive':str(ROOT/'out/session-a/source-checkpoint-current225-complete/sanguo11-mobile-source.tar.gz'),
            'completedASourceArchiveSha256':sha(ROOT/'out/session-a/source-checkpoint-current225-complete/sanguo11-mobile-source.tar.gz'),
            'bCandidateReportSha256':sha(B/'frozen-report.json'),'overlayPath':str(archive),'overlaySha256':sha(archive),'paths':rows,
            'candidateBeforeVerified':len(verified),'includesBProduction':False,'includesRootGradleBridgeUnityOriginal4Jni':False,
            'original4JniPreserved':a.get('original4AndNew2Jni','see export.json'),
            'scope':'Completed A production prerequisite closure plus 224 staged Main/picker adapter; no B WIP. No new APK/install or dynamic acceptance.',
            'conflicts':['Candidate original Main and frozen theme-subset Main have distinct before SHA; require either exact before, use frozen A after only once, do not reapply old readonly-theme subset.'],'installed':False,'wholeGoalComplete':False}
    (DOC/f'CANDIDATE_A_CLOSURE{args.revision}.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'overlay':str(archive),'sha':report['overlaySha256'],'paths':len(rows),'verifiedBefore':len(verified),'files':[x['path'] for x in rows]},ensure_ascii=False))
if __name__=='__main__':main()
