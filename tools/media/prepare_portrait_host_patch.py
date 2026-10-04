#!/usr/bin/env python3
"""Prepare only the sequential readonly portrait metadata binding from a completed commit. Never apply."""
import argparse
import difflib
import hashlib
import json
from pathlib import Path
import subprocess

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source-repo',type=Path,required=True);p.add_argument('--source-commit',required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    commit=subprocess.check_output(['git','-C',str(a.source_repo),'rev-parse',a.source_commit+'^{commit}'],text=True).strip();path='app/src/main/java/game/sanguo/mobile/MainActivity.java'
    before=subprocess.check_output(['git','-C',str(a.source_repo),'show',commit+':'+path]);needle='        legacyView=current.legacyView();world=legacyView.draft;\n';text=before.decode()
    if text.count(needle)!=1:raise ValueError('Completed metadata host drift')
    after=text.replace(needle,needle+'        PortraitMediaSources.bind(world,legacyView.state,current.officers());\n',1).encode()
    if a.output.exists():raise ValueError('Fresh isolated output required')
    a.output.mkdir(parents=True);candidate=a.output/'candidate'/path;candidate.parent.mkdir(parents=True,exist_ok=True);candidate.write_bytes(after)
    patch=''.join(difflib.unified_diff(before.decode().splitlines(keepends=True),after.decode().splitlines(keepends=True),fromfile='a/'+path,tofile='b/'+path)).encode();(a.output/'portrait-host.patch').write_bytes(patch)
    (a.output/'guards.json').write_text(json.dumps(dict(applied=False,sourceCommit=commit,path=path,beforeSha256=hashlib.sha256(before).hexdigest(),afterSha256=hashlib.sha256(after).hexdigest(),patchSha256=hashlib.sha256(patch).hexdigest(),
        limits=['Metadata snapshot copied through readonly additive seam; old sources missing remain unknown.', 'Apply only in sequential audited integration preserving completed metadata MainActivity and any newer changes.', 'No rule command, save, metadata mutation or RNG call.']),indent=2)+'\n');print('Prepared unapplied original portrait host patch from',commit)
