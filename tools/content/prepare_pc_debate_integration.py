#!/usr/bin/env python3
"""Create a complete verified development copy without changing frozen inputs."""
import argparse,hashlib,json,shutil
from pathlib import Path

def prepare(checkpoint,target):
    checkpoint=checkpoint.resolve();target=target.resolve();root=Path(__file__).resolve().parents[2]
    if not target.is_relative_to(root/'out/session1'):raise ValueError('Independent session1 directory required')
    if target.exists():raise ValueError('Preserve existing verification copy')
    manifest=json.loads((checkpoint/'manifest.json').read_bytes());source=checkpoint/'source'
    sha=lambda data:hashlib.sha256(data).hexdigest()
    for row in manifest['files']:
        p=source/row['path']
        if not p.is_file()or p.is_symlink()or sha(p.read_bytes())!=row['sha256']:raise ValueError('Checkpoint changed '+row['path'])
    target.parent.mkdir(parents=True,exist_ok=True);shutil.copytree(source,target,copy_function=shutil.copy2)
    for row in manifest['files']:
        if sha((target/row['path']).read_bytes())!=row['sha256']:raise ValueError('Copy changed '+row['path'])
    proof=dict(sourceCommit=manifest['commit'],sourceManifestSha256=sha((checkpoint/'manifest.json').read_bytes()),
               files=manifest['fileCount'],bytes=manifest['bytes'],ignoredJni=manifest['ignoredJniInputs'],
               completeInheritedPaths=True,sourcePath=str(source),verificationSourcePath=str(target),
               frozenProductionInputsModified=False,completeGoal=False)
    (target.parent/'inheritance-proof.json').write_text(json.dumps(proof,indent=2)+'\n');print(json.dumps(proof))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('checkpoint',type=Path);p.add_argument('--target',type=Path,required=True);a=p.parse_args();prepare(a.checkpoint,a.target)
