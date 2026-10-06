#!/usr/bin/env python3
"""Stage exact completed parent plus one read-only source-cache change, excluding own governance WIP."""
import hashlib,json,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
BASE='969c518829351e84d4a1a0bba5275b8ee53fbd25'
OVERLAY='core/src/main/java/game/sanguo/core/PcScenarioPeople.java'
def sha(raw):return hashlib.sha256(raw).hexdigest()
def main():
    if subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()!=BASE:raise ValueError('Re-audit changed completed parent')
    dest=ROOT/'out/session-b/cache-core-stage';dest.mkdir(exist_ok=False)
    paths=subprocess.check_output(['git','ls-tree','-r','--name-only',BASE,'core/src/main/java'],cwd=ROOT,text=True).splitlines();rows=[]
    for logical in paths:
        if not logical.endswith('.java'):continue
        before=subprocess.check_output(['git','show',BASE+':'+logical],cwd=ROOT)
        raw=(ROOT/logical).read_bytes()if logical==OVERLAY else before
        output=dest/'java'/Path(logical).relative_to('core/src/main/java');output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(raw)
        rows.append(dict(logical=logical,path=str(output.relative_to(ROOT)),parentSha256=sha(before),compiledSha256=sha(raw),overlay=logical==OVERLAY))
    assert len(rows)==143 and sum(r['overlay']for r in rows)==1
    report=dict(base=BASE,compiled=rows,excludedWip='all other current core Java including governor strategy; pc-governor-rosters resources',actualApkAccepted=False)
    (dest/'manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print('Staged',len(rows),'exact parent core Java paths with one cache-only overlay')
if __name__=='__main__':main()
