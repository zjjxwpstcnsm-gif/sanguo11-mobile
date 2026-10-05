#!/usr/bin/env python3
"""Compare two fresh conversions by all paths and bytes, including original sample retention."""
import argparse
import hashlib
import json
from pathlib import Path


def inventory(root):
    rows=[]
    for p in sorted(root.rglob('*')):
        if not p.is_file():continue
        digest=hashlib.sha256()
        with p.open('rb') as f:
            for block in iter(lambda:f.read(1024*1024),b''):digest.update(block)
        rows.append(dict(path=p.relative_to(root).as_posix(),bytes=p.stat().st_size,sha256=digest.hexdigest()))
    return rows


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('first',type=Path);p.add_argument('second',type=Path);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args()
    if a.output.exists():raise ValueError('Preserve earlier evidence')
    first,second=inventory(a.first),inventory(a.second)
    one={r['path']:r for r in first};two={r['path']:r for r in second}
    changed=[k for k in sorted(one.keys()|two.keys()) if one.get(k)!=two.get(k)]
    report=dict(schema=1,first=str(a.first.resolve()),second=str(a.second.resolve()),
                allPathsAndBytesEqual=not changed,fileCount=len(first),totalBytes=sum(r['bytes'] for r in first),mismatches=changed,
                firstGuardSha256=hashlib.sha256(json.dumps(first,sort_keys=True).encode()).hexdigest(),
                secondGuardSha256=hashlib.sha256(json.dumps(second,sort_keys=True).encode()).hexdigest(),files=first)
    a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:v for k,v in report.items() if k!='files'}))
    if changed:raise SystemExit(1)
