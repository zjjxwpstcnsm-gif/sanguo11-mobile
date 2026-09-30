#!/usr/bin/env python3
"""Read the delivered archive, not a filename/version claim."""
import hashlib, json, pathlib, re, sys, zipfile
apk=pathlib.Path(sys.argv[1]);source=sys.argv[2]
assert re.fullmatch('[0-9a-f]{40}',source)
with zipfile.ZipFile(apk) as z:
    assert z.testzip() is None
    assert any(source.encode() in z.read(n) for n in z.namelist() if re.fullmatch(r'classes\d*\.dex',n)), 'DEX source mismatch'
    assets=list(pathlib.Path('app/src/main/assets').rglob('*'));count=0
    for p in assets:
        if p.is_file():
            assert z.read('assets/'+p.relative_to('app/src/main/assets').as_posix())==p.read_bytes(),str(p)
            count+=1
    abis=sorted({n.split('/')[1] for n in z.namelist() if n.startswith('lib/') and n.endswith('.so')})
    assert abis==['arm64-v8a','armeabi-v7a','x86','x86_64'],abis
    assert not any(n.endswith('/libunity.so') for n in z.namelist())
print(json.dumps(dict(source_sha=source,apk_sha256=hashlib.sha256(apk.read_bytes()).hexdigest(),bytes=apk.stat().st_size,abis=abis,matched_assets=count,backend='Filament 1.56.0 OPENGL',acceptance='PARTIAL; archive identity is not runtime or art acceptance'),indent=2))
