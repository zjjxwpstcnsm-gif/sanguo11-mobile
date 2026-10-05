#!/usr/bin/env python3
"""Archive explicit installed media evidence; exclude private user backup tars.

No device/source mutations. Includes failures when explicitly requested,
raw recorded PCM, UI captures and generated authority fixtures with SHA guards.
"""
import argparse,gzip,hashlib,io,json,tarfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
def sha(raw):return hashlib.sha256(raw).hexdigest()
def archive(inputs,output):
    if output.exists():raise ValueError('Fresh evidence output required')
    paths={}
    for directory in inputs:
        directory=directory.resolve();directory.relative_to((ROOT/'out/media').resolve())
        report=json.loads((directory/'results.json').read_text())
        terminal=report.get('stage')=='finished' or bool(report.get('exception'))
        if not terminal or not report.get('restoration',{}).get('all_original_files_byte_equal'):raise ValueError('Terminal byte-restored device run required; failed attempts retain their original status')
        for path in directory.rglob('*'):
            relative=path.relative_to(directory)
            if not path.is_file() or path.is_symlink():continue
            if path.suffix=='.tar' or path.name.endswith('-preview.png'):continue
            if len(relative.parts)>1 and relative.parts[0] not in ['ui-evidence','device-evidence']:continue
            name=str(path.relative_to(ROOT.resolve()));raw=path.read_bytes()
            paths[name]=(raw,dict(path=name,bytes=len(raw),sha256=sha(raw)))
    if not paths:raise ValueError('No declared actual evidence')
    output.mkdir(parents=True);target=output/'actual-media-evidence.tar.gz'
    with target.open('wb') as file,gzip.GzipFile(filename='',mode='wb',fileobj=file,mtime=0) as compressed,tarfile.open(fileobj=compressed,mode='w|') as tar:
        for name,(raw,_) in sorted(paths.items()):
            entry=tarfile.TarInfo(name);entry.size=len(raw);entry.mode=0o644;entry.mtime=0;tar.addfile(entry,io.BytesIO(raw))
    seen=set()
    with tarfile.open(target,'r:gz') as tar:
        for entry in tar:
            raw=tar.extractfile(entry).read()
            if entry.name in seen or entry.name not in paths or raw!=paths[entry.name][0]:raise ValueError('Archive evidence differs')
            seen.add(entry.name)
    if seen!=set(paths):raise ValueError('Archive omitted evidence')
    report=dict(archive=str(target.resolve()),archiveBytes=target.stat().st_size,archiveSha256=sha(target.read_bytes()),files=[row for _,row in sorted(paths.values(),key=lambda item:item[1]['path'])],allReadbackByteEqual=True,privateUserBackupTarsExcluded=True,goalComplete=False)
    (output/'evidence-manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='files'}))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--input',type=Path,action='append',required=True);p.add_argument('--output',type=Path,required=True);args=p.parse_args();archive(args.input,args.output)
