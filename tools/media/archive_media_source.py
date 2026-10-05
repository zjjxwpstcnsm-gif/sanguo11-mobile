#!/usr/bin/env python3
"""Freeze all current tracked source plus the four SHA-guarded ignored JNI inputs.

Uses NUL-delimited Git paths so CJK filenames are never quoted or duplicated.
No reset, cleanup, source copy from old HEAD, PC-install writes or user save access.
"""
import argparse
import gzip
import hashlib
import io
import json
import os
from pathlib import Path
import subprocess
import tarfile


def digest(data):
    return hashlib.sha256(data).hexdigest()


def freeze(output, inputs, apk):
    root=Path(__file__).resolve().parents[2]
    if output.exists():
        raise ValueError('Fresh delivery directory required')
    def git(*args):
        return subprocess.check_output(['git',*args],cwd=root)
    if git('status','--porcelain','-z'):
        raise ValueError('Commit the complete intended source batch before archiving')
    head=git('rev-parse','HEAD').decode().strip()
    tracked=[os.fsdecode(x) for x in git('ls-files','-z').split(b'\0') if x]
    fixed=json.loads(inputs.read_text())['ignoredFixedInputs']
    if len(fixed)!=4 or {row['path'] for row in fixed}!={
        'out/pc-native-runtime/jniLibs/'+abi+'/'+lib
        for abi in ('arm64-v8a','x86_64') for lib in ('libpc_effect_worker.so','libunicorn.so')}:
        raise ValueError('Expected all four fixed JNI inputs')
    for row in fixed:
        raw=(root/row['path']).read_bytes()
        if len(raw)!=row['bytes'] or digest(raw)!=row['sha256']:
            raise ValueError('Inherited JNI SHA mismatch: '+row['path'])
        if git('check-ignore','--',row['path']).decode().strip()!=row['path']:
            raise ValueError('Fixed JNI input ignore status changed')
    paths=sorted(set(tracked)|{row['path'] for row in fixed})
    if len(paths)!=len(tracked)+4:
        raise ValueError('Tracked/ignored overlap or duplicate Git paths')
    output.mkdir(parents=True)
    target=output/'sanguo11-portrait-audio-source.tar.gz'
    source_rows=[]
    with target.open('wb') as f, gzip.GzipFile(filename='',mode='wb',fileobj=f,mtime=0,compresslevel=6) as zipped:
        with tarfile.open(fileobj=zipped,mode='w|',format=tarfile.PAX_FORMAT) as tar:
            for relative in paths:
                p=root/relative
                if p.is_symlink() or not p.is_file() or Path(relative).is_absolute() or '..' in Path(relative).parts:
                    raise ValueError('Unexamined source file: '+relative)
                raw=p.read_bytes()
                entry=tarfile.TarInfo(relative)
                entry.size=len(raw);entry.mtime=0;entry.uid=entry.gid=0;entry.uname=entry.gname=''
                entry.mode=0o755 if p.stat().st_mode&0o111 else 0o644
                tar.addfile(entry,io.BytesIO(raw))
                source_rows.append(dict(path=relative,bytes=len(raw),sha256=digest(raw)))
    seen=set()
    expected={row['path']:row for row in source_rows}
    with tarfile.open(target,'r:gz') as tar:
        for entry in tar:
            if entry.name in seen or entry.name not in expected or not entry.isfile():
                raise ValueError('Unexpected/duplicate archived path')
            seen.add(entry.name)
            raw=tar.extractfile(entry).read()
            row=expected[entry.name]
            if len(raw)!=row['bytes'] or digest(raw)!=row['sha256'] or raw!=(root/entry.name).read_bytes():
                raise ValueError('Archived source differs: '+entry.name)
    if seen!=set(paths) or head!=git('rev-parse','HEAD').decode().strip() or git('status','--porcelain','-z'):
        raise ValueError('Source changed during freeze')
    manifest=dict(sourceCommit=head,archive=str(target.resolve()),archiveSha256=digest(target.read_bytes()),
                  archiveBytes=target.stat().st_size,sourceFiles=len(paths),sourceEntriesByteEqual=True,
                  allTrackedFilesIncluded=True,allFourIgnoredJniIncluded=True,ignoredFixedInputs=fixed,
                  mainApkSha256=digest(apk.read_bytes()),goalComplete=False)
    (output/'source-files.json').write_text(json.dumps(source_rows,ensure_ascii=False,indent=2)+'\n')
    (output/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print(json.dumps(manifest))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output',type=Path,required=True)
    p.add_argument('--fixed-input-manifest',type=Path,required=True)
    p.add_argument('--apk',type=Path,required=True)
    a=p.parse_args();freeze(a.output,a.fixed_input_manifest,a.apk)
