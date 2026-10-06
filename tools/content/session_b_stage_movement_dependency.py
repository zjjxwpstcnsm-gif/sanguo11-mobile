#!/usr/bin/env python3
"""Stage exact A-owned finished movement adapter for compile only, never modify MainActivity."""
import hashlib,json,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def sha(raw):return hashlib.sha256(raw).hexdigest()
def main():
 source=Path('/Users/paopao/.codex/worktrees/2191/sanguo11-mobile/docs/handoff/20261006/session-a/MOVEMENT_FROZEN.json');j=json.loads(source.read_text())
 assert j['completeSourceSubsetFrozen']and j['BMayReadOrCompileFinishedDependency']and j['minimalPatchOnlyMovementAdapter']
 assert sha((ROOT/j['path']).read_bytes())==j['beforeSha256']
 raw=Path(j['readonlyFinishedExport']).read_bytes();assert sha(raw)==j['afterSha256']
 original=subprocess.check_output(['git','show',j['base']+':'+j['path']],cwd=ROOT);assert sha(original)==j['beforeSha256']
 parent=ROOT/'out/session-b/readonly-theme-dependencies';assert (parent/'manifest.json').is_file()
 target=parent/'java/MainActivity.java';target.write_bytes(raw);assert sha(target.read_bytes())==j['afterSha256']
 (parent/'movement-manifest.json').write_text(json.dumps(dict(j,generatedPath=str(target)),ensure_ascii=False,indent=2)+'\n');print('A MainActivity compile-only adapter staged at exact SHA '+j['afterSha256'])
if __name__=='__main__':main()
