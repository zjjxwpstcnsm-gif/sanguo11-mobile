#!/usr/bin/env python3
"""Compile-only A completed theme export; preserve every A-owned production path."""
from pathlib import Path
import hashlib,json,subprocess,shutil
ROOT=Path(__file__).resolve().parents[2]
FROZEN=Path('/Users/paopao/.codex/worktrees/2191/sanguo11-mobile/docs/handoff/20261006/session-a/THEME_FROZEN.json')
def sha(raw):return hashlib.sha256(raw).hexdigest()
def main():
 frozen=json.loads(FROZEN.read_text());assert frozen['completeSubsetFrozen']and frozen['mayReadOrCompileFinishedDependency']
 dest=ROOT/'out/session-b/readonly-theme-dependencies'
 if dest.exists():
  prior=json.loads((dest/'manifest.json').read_text())
  assert prior['sourceRevision']==frozen['sourceRevision']
  for row in prior['files']:assert sha(Path(row['generatedPath']).read_bytes())==row['afterSha256']
  print('reuse exact completed A theme dependency '+prior['sourceRevision']);return
 entries=[]
 for row in frozen['files']:
  current=ROOT/row['path'];before=sha(current.read_bytes())if current.exists()else None
  assert before==row['beforeSha256'],row['path']
  raw=Path(row['readonlyFinishedExport']).read_bytes();assert sha(raw)==row['afterSha256']
  committed=subprocess.check_output(['git','-C',frozen['sourceDirectory'],'show',frozen['sourceRevision']+':'+row['path']]);assert raw==committed
  entries.append((row,raw))
 dest.mkdir(parents=True)
 shutil.copytree(ROOT/'app/src/main/res',dest/'res')
 report=dict(sourceRevision=frozen['sourceRevision'],base=frozen['base'],files=[],scope='read-only completed dependency compilation; no A production path modification')
 for row,raw in entries:
  target=dest/'java'/Path(row['path']).name if row['path'].endswith('.java')else dest/'res'/Path(row['path']).relative_to('app/src/main/res')
  target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(raw);assert sha(target.read_bytes())==row['afterSha256']
  report['files'].append(dict(row,generatedPath=str(target)))
 (dest/'manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print('verified five A dependencies '+frozen['sourceRevision'])
if __name__=='__main__':main()
