#!/usr/bin/env python3
"""Validate a phone-exported map Patch; preview or transactionally apply resource edits.
Default is dry-run. --apply never commits, pushes or changes branches. A temporary
rebuilt resource tree must load all nine scenarios and match the editor's seven
national opening fingerprints. Unrepresentable migrations stop without writing.
"""
from __future__ import annotations
import argparse, difflib, hashlib, json, os, pathlib, re, shutil, subprocess, tempfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
ADAPTER = pathlib.Path('tools/content/MapPatchRepository.java')
MAP = pathlib.Path('core/src/main/resources/maps/national-map-v056.properties')
NATIONAL = pathlib.Path('core/src/main/java/game/sanguo/core/NationalMap.java')
MANIFEST = pathlib.Path('tools/content/map-release-manifest.json')

def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def run(command: list[str], cwd: pathlib.Path) -> None:
    subprocess.run(command, cwd=cwd, check=True,
                   env=dict(os.environ, JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8'))

def compile_core(repo: pathlib.Path, classes: pathlib.Path) -> None:
    classes.mkdir(parents=True, exist_ok=True)
    files=sorted((repo/'core/src/main/java').rglob('*.java'))+[repo/ADAPTER]
    args=classes.parent/(classes.name+'-sources.txt')
    args.write_text('\n'.join(str(f) for f in files), encoding='utf-8')
    run(['javac','-encoding','UTF-8','--release','17','-d',str(classes),'@'+str(args)],repo)

def java(repo: pathlib.Path, classes: pathlib.Path, mode: str, source: pathlib.Path, out: pathlib.Path) -> None:
    run(['java','-Xmx768m','-cp',str(classes)+os.pathsep+str(repo/'core/src/main/resources'),
         'game.sanguo.core.MapPatchRepository',mode,str(source),str(repo),str(out)],repo)

def escape(value: str) -> str:
    result=value.replace('\\','\\\\').replace('\t','\\t').replace('\n','\\n').replace('\r','\\r')
    if result.startswith(' '):result='\\'+result
    return result

def edit_properties(raw: bytes, changes: dict[str, str|None]) -> bytes:
    text=raw.decode('utf-8'); pending=dict(changes); seen=set(); out=[]
    for line in text.splitlines(keepends=True):
        match=re.match(r'^([A-Za-z0-9_.-]+)=(.*?)(\r?\n)?$',line)
        if not match:
            out.append(line);continue
        key=match.group(1)
        if key in seen:raise ValueError('Duplicate property: '+key)
        seen.add(key)
        if key not in pending:out.append(line);continue
        value=pending.pop(key)
        if value is not None:out.append(key+'='+escape(value)+(match.group(3) or '\n'))
    if pending and out and not out[-1].endswith('\n'):out[-1]+='\n'
    for key,value in sorted(pending.items()):
        if value is not None:out.append(key+'='+escape(value)+'\n')
    return ''.join(out).encode('utf-8')

def replace(path: pathlib.Path, data: bytes) -> None:
    path.parent.mkdir(parents=True,exist_ok=True)
    fd,name=tempfile.mkstemp(prefix=path.name+'.map-patch-',dir=path.parent)
    try:
        with os.fdopen(fd,'wb') as out:out.write(data);out.flush();os.fsync(out.fileno())
        os.replace(name,path)
        if hasattr(os,'O_DIRECTORY'):
            directory_fd=os.open(path.parent,os.O_DIRECTORY)
            try:os.fsync(directory_fd)
            finally:os.close(directory_fd)
    finally:
        if os.path.exists(name):os.unlink(name)

def transact(repo: pathlib.Path, changes: dict[pathlib.Path, tuple[bytes|None, bytes]]) -> None:
    journal=repo/'.map-patch-transaction'
    if journal.exists():raise ValueError('Unfinished transaction: inspect .map-patch-transaction and use --recover')
    journal.mkdir(); records=[]
    try:
        for index,(path,(old,new)) in enumerate(changes.items()):
            if ((repo/path).read_bytes() if (repo/path).exists() else None)!=old:raise ValueError('Concurrent change: '+str(path))
            if old is not None:replace(journal/str(index),old)
            records.append(dict(path=str(path),backup=str(index) if old is not None else None,
                                before=digest(old) if old is not None else None,after=digest(new)))
        replace(journal/'journal.json',json.dumps(records,indent=2).encode())
        for path,(old,new) in changes.items():
            if ((repo/path).read_bytes() if (repo/path).exists() else None)!=old:raise ValueError('Concurrent change: '+str(path))
            replace(repo/path,new)
        shutil.rmtree(journal)
    except BaseException:
        if (journal/'journal.json').exists():recover(repo)
        else:shutil.rmtree(journal)
        raise

def recover(repo: pathlib.Path) -> None:
    journal=repo/'.map-patch-transaction'; records=json.loads((journal/'journal.json').read_text())
    for record in records:
        relative=pathlib.Path(record['path'])
        if relative.is_absolute() or '..' in relative.parts or not (repo/relative).resolve().is_relative_to(repo.resolve()):
            raise ValueError('Unsafe recovery path')
        backup=record['backup']
        if backup is not None and (not str(backup).isdigit() or digest((journal/backup).read_bytes())!=record['before']):
            raise ValueError('Corrupted recovery backup')
    for record in records:
        path=repo/record['path'];actual=digest(path.read_bytes()) if path.exists() else None
        if actual not in (record['before'],record['after']):raise ValueError('Recovery stopped at concurrent edit: '+str(path))
    for record in records:
        path=repo/record['path']
        if record['backup'] is None:
            if path.exists():path.unlink()
        else:replace(path,(journal/record['backup']).read_bytes())
    shutil.rmtree(journal);print('Recovered original files; no commit or push was performed.')

def main() -> None:
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('patch',nargs='?',type=pathlib.Path)
    parser.add_argument('--repo',type=pathlib.Path,default=ROOT)
    parser.add_argument('--apply',action='store_true')
    parser.add_argument('--recover',action='store_true')
    parser.add_argument('--report',type=pathlib.Path)
    args=parser.parse_args(); repo=args.repo.resolve()
    if args.recover:recover(repo);return
    if args.patch is None:parser.error('Patch JSON path required')
    if (repo/'.map-patch-transaction').exists():raise ValueError('Unfinished transaction; use --recover before a new merge')
    patch=args.patch.resolve(); raw=patch.read_bytes()
    if len(raw)>4*1024*1024:raise ValueError('Patch exceeds 4MiB')
    # Checked no-op: a repeated successful merge never creates duplicate entities.
    history=repo/'data/map/applied-editor-patches'
    if history.exists():
        for marker in sorted(history.glob('*.json')):
            record=json.loads(marker.read_text())
            if record.get('inputSha256')==digest(raw):
                for name,expected in record['outputs'].items():
                    if digest((repo/name).read_bytes())!=expected:raise ValueError('Previously applied Patch outputs were subsequently edited: '+name)
                print('Already applied; no duplicate entities or rewritten files.');return
    with tempfile.TemporaryDirectory(prefix='sanguo-map-patch-') as directory:
        temp=pathlib.Path(directory); classes=temp/'classes'; compile_core(repo,classes)
        plan_path=temp/'plan.json';java(repo,classes,'plan',patch,plan_path);plan=json.loads(plan_path.read_text())
        changes={}
        for name,entry in plan['files'].items():
            path=pathlib.Path(name);old=(repo/path).read_bytes()
            if digest(old)!=entry['beforeSha256']:raise ValueError('Source changed while validating: '+name)
            new=edit_properties(old,entry['properties'])
            if old!=new:changes[path]=(old,new)
        if MAP in changes:
            old=(repo/NATIONAL).read_bytes(); before=digest(changes[MAP][0]);after=digest(changes[MAP][1])
            needle=('SHA256="'+before+'"').encode()
            if old.count(needle)!=1:raise ValueError('National map fingerprint anchor mismatch')
            changes[NATIONAL]=(old,old.replace(needle,('SHA256="'+after+'"').encode()))
        index=pathlib.Path('core/src/main/resources/scenarios/index.txt')
        old_index=(repo/index).read_bytes(); index_text=old_index.decode()
        for path,(old,new) in list(changes.items()):
            if path.parent==index.parent and path.suffix=='.properties':
                expected=path.stem+' '+digest(old)
                if index_text.count(expected)!=1:raise ValueError('Scenario catalog fingerprint mismatch: '+str(path))
                index_text=index_text.replace(expected,path.stem+' '+digest(new))
        if index_text.encode()!=old_index:changes[index]=(old_index,index_text.encode())
        old=(repo/MANIFEST).read_bytes(); data=json.loads(old); text=old.decode()
        for item in data['files']:
            path=pathlib.Path(item['source_path'])
            if path in changes:
                before=item['sha256'];after=digest(changes[path][1])
                if digest(changes[path][0])!=before:raise ValueError('Release manifest already inconsistent: '+str(path))
                pattern=r'\{[^{}]*"source_path"\s*:\s*"'+re.escape(str(path))+r'"[^{}]*\}'
                def manifest_entry(match):
                    entry=match.group(0)
                    if entry.count('"'+before+'"')!=1:raise ValueError('Ambiguous manifest hash')
                    return entry.replace('"'+before+'"','"'+after+'"')
                text,n=re.subn(pattern,manifest_entry,text)
                if n!=1:raise ValueError('Manifest entry layout changed: '+str(path))
        if text.encode()!=old:changes[MANIFEST]=(old,text.encode())
        staged=temp/'staged';(staged/'core/src').mkdir(parents=True)
        shutil.copytree(repo/'core/src/main',staged/'core/src/main');(staged/ADAPTER).parent.mkdir(parents=True);shutil.copy2(repo/ADAPTER,staged/ADAPTER)
        for path,(_,new) in changes.items():replace(staged/path,new)
        rebuilt=temp/'rebuilt';compile_core(staged,rebuilt);java(staged,rebuilt,'verify',plan_path,temp/'verified.json')
        report=dict(mapId=plan['mapId'],revision=plan['revision'],summary=plan['summary'],
                    verified=json.loads((temp/'verified.json').read_text()),
                    files={str(p):dict(beforeSha256=digest(old),afterSha256=digest(new)) for p,(old,new) in changes.items()})
        print(plan['summary']);print('All shipped scenarios load; national openings match the editor resolver.')
        for path,(old,new) in changes.items():
            print(''.join(difflib.unified_diff(old.decode().splitlines(True),new.decode().splitlines(True),fromfile='a/'+str(path),tofile='b/'+str(path))),end='')
        if args.report:args.report.parent.mkdir(parents=True,exist_ok=True);args.report.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
        if args.apply:
            marker=pathlib.Path('data/map/applied-editor-patches')/(plan['mapId']+'-r'+str(plan['revision'])+'.json')
            if (repo/marker).exists():raise ValueError('Map ID/revision already has a different merge record')
            record=dict(inputSha256=digest(raw),patchFingerprint=plan['patchFingerprint'],baseFingerprint=plan['baseFingerprint'],outputs={str(p):digest(new) for p,(_,new) in changes.items()})
            changes[marker]=(None,(json.dumps(record,indent=2)+'\n').encode());transact(repo,changes);print('Applied. Review the Git diff; nothing was committed or pushed.')
        else:print('DRY RUN ONLY: no repository source files were modified. Use --apply after review.')

if __name__=='__main__':main()
