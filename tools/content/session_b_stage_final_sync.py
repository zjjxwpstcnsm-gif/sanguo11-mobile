#!/usr/bin/env python3
"""Stage completed frozen sources in the clean closeout directory, never the dirty source or main."""
from pathlib import Path
import hashlib,json,tarfile,subprocess,os
R=Path(__file__).resolve().parents[2];T=Path('/Users/paopao/.codex/worktrees/rules-contest-closeout/sanguo11-mobile');F=R/'out/session-b/native-opening-combined61-frozen-r28-search-v2';O=R/'out/session-b/final-sync-audit';O.mkdir(exist_ok=False)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
assert subprocess.check_output(['git','status','--porcelain'],cwd=T)==b''
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=T).decode().strip()=='0693afc941763a7b1ab973f0b2e1841042749834'
report=json.loads((F/'frozen-report.json').read_text());assert sha(F/'combined-source.tar.gz')==report['sourceArchive']['sha256'];d=json.loads((F/'build-inputs.json').read_text());rows={r['path']:r for r in d['files']};g=json.loads((F/'source-guard.json').read_text());changes=[];extras=[]
with tarfile.open(F/'combined-source.tar.gz')as archive:
 for m in archive:
  assert m.isfile()and m.name in rows and not Path(m.name).is_absolute()and '..'not in Path(m.name).parts;row=rows[m.name];data=archive.extractfile(m).read();assert len(data)==row['bytes']and hashlib.sha256(data).hexdigest()==row['sha256'];p=T/m.name
  # Git history already contains later completed reports. Preserve those;
  # bring every production input and missing frozen dependency/tool/test.
  if m.name not in g and p.exists():continue
  before=sha(p)if p.is_file()else None
  if p.is_symlink():raise AssertionError('unexpected closeout symlink '+m.name)
  p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data);p.chmod(m.mode);assert sha(p)==row['sha256']
  if before!=row['sha256']:changes.append(dict(path=m.name,beforeSha256=before,afterSha256=row['sha256'],mode=m.mode,production=m.name in g))
 for p,h in g.items():assert sha(T/p)==h,p
 # Record commit-ready production inputs separately from ignored JNI/runtime copies.
 for p in g:
  ignored=subprocess.run(['git','check-ignore','-q',p],cwd=T).returncode==0
  if ignored:extras.append(dict(path=p,sha256=g[p],gitIgnored=True))
manifest=dict(wholeGoalComplete=False,userRequestedCloseout=True,commonMain='ef413be3653820dd6449ba7f02aa60bed5b26ef5',latestFullMain='0e7b9bc2df90249a50851baeda58c7d183ea6059',sourceCompletedCommit='0693afc941763a7b1ab973f0b2e1841042749834',aCompletedDependencies=['9ab4a3d61bd5e6db2f89ff7c787ee1481105a211','925748c6139b66a799e59de509c47151b00d8acb'],sourceArchiveSha256=report['sourceArchive']['sha256'],testedApkSha256=report['apk']['sha256'],target=str(T),productionGuard=g,changedPaths=changes,ignoredRuntimeInputs=extras,excludedCurrentPerformanceAndOtherSessionWip=True,limits=report['limits'])
(O/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n');p=T/'docs/handoff/20261006/session-b/FINAL_SYNC_MANIFEST.json';p.write_text(json.dumps(manifest,indent=2)+'\n');print('PASS completed frozen source exact production guard',len(g),'changes',len(changes),'ignored inputs',len(extras))
