#!/usr/bin/env python3
"""Reproduce a guarded integration in a session1 verification copy only."""
import argparse,hashlib,json,subprocess
from pathlib import Path

def apply(manifest_path,patch,target):
    root=Path(__file__).resolve().parents[2];target=target.resolve();sha=lambda b:hashlib.sha256(b).hexdigest()
    if not target.is_relative_to(root/'out/session1'):raise ValueError('Production application forbidden; use verified session1 copy')
    manifest=json.loads(manifest_path.read_bytes())
    if sha(patch.read_bytes())!=manifest['patchSha256']:raise ValueError('Integration patch changed')
    for row in manifest['changes']:
        p=target/row['path']
        if row['new']:
            if p.exists():raise ValueError('New integration path already exists '+row['path'])
        elif not p.is_file()or sha(p.read_bytes())!=row['beforeSha256']:raise ValueError('Before-image differs '+row['path'])
    directory=target.relative_to(root).as_posix()
    subprocess.run(['git','apply','--check','--directory',directory,str(patch.resolve())],cwd=root,check=True)
    subprocess.run(['git','apply','--directory',directory,str(patch.resolve())],cwd=root,check=True)
    for row in manifest['changes']:
        if sha((target/row['path']).read_bytes())!=row['sha256']:raise ValueError('Reproduced integration differs '+row['path'])
    print(json.dumps(dict(paths=len(manifest['changes']),allPostImagesByteEqual=True,productionInputsModified=False,target=str(target))))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('manifest',type=Path);p.add_argument('patch',type=Path);p.add_argument('--target',required=True,type=Path);a=p.parse_args();apply(a.manifest,a.patch,a.target)
