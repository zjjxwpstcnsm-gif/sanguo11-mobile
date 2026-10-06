#!/usr/bin/env python3
"""Portable compile-only staging of six exact completed A dependency paths."""
import hashlib,io,json,shutil,tarfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
HANDOFF=ROOT/'docs/handoff/20261006/session-b'
def sha(raw):return hashlib.sha256(raw).hexdigest()
def main():
    manifest=json.loads((HANDOFF/'A_FROZEN.json').read_text())
    assert manifest['completeSourceSubsetFrozen']and manifest['compileOnly']and not manifest['productionPathsMayBeModified']
    raw=(HANDOFF/manifest['archive']).read_bytes();assert sha(raw)==manifest['archiveSha256']
    rows=manifest['files'];contents={}
    with tarfile.open(fileobj=io.BytesIO(raw),mode='r:gz')as archive:
        for entry in archive:
            assert entry.isfile()and entry.name in {r['path']for r in rows}and entry.name not in contents
            contents[entry.name]=archive.extractfile(entry).read()
    assert len(contents)==6==len(rows)
    for row in rows:
        source=ROOT/row['path'];before=sha(source.read_bytes())if source.exists()else None
        assert before==row['beforeSha256'],row['path']
        assert sha(contents[row['path']])==row['afterSha256']
    dest=ROOT/'out/session-b/readonly-theme-dependencies'
    if dest.exists():
        prior=json.loads((dest/'manifest.json').read_text());assert prior['sourceRevision']==manifest['sourceRevision']
        for row in rows:
            target=dest/'java'/Path(row['path']).name if row['path'].endswith('.java')else dest/'res'/Path(row['path']).relative_to('app/src/main/res')
            assert sha(target.read_bytes())==row['afterSha256']
        assert json.loads((dest/'movement-manifest.json').read_text())['afterSha256']==rows[-1]['afterSha256']
        print('reused six verified A compile-only files');return
    dest.mkdir(parents=True);shutil.copytree(ROOT/'app/src/main/res',dest/'res')
    theme=[]
    for row in rows:
        target=dest/'java'/Path(row['path']).name if row['path'].endswith('.java')else dest/'res'/Path(row['path']).relative_to('app/src/main/res')
        target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(contents[row['path']])
        if row is not rows[-1]:theme.append(dict(row,generatedPath=str(target)))
    report=dict(sourceRevision=manifest['sourceRevision'],base=manifest['base'],files=theme,scope='portable read-only completed dependency compilation; original A files unchanged')
    (dest/'manifest.json').write_text(json.dumps(report,indent=2)+'\n')
    (dest/'movement-manifest.json').write_text(json.dumps(dict(rows[-1],base=manifest['base'],revision=manifest['sourceRevision'],completeSourceSubsetFrozen=True,generatedPath=str(dest/'java/MainActivity.java')),indent=2)+'\n')
    print('staged six exact completed A compile-only files')
if __name__=='__main__':main()
