#!/usr/bin/env python3
"""Integrate a completed UI commit against its prior frozen commit, preserving local work."""
import argparse,hashlib,json,subprocess
from pathlib import Path

def main(a):
    def git(*args,optional=False):
        p=subprocess.run(['git','-C',str(a.ui_repo),*args],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if p.returncode:
            if optional:return None
            raise ValueError(p.stderr.decode())
        return p.stdout
    base=git('rev-parse',a.base).decode().strip();target=git('rev-parse',a.target).decode().strip()
    paths=git('diff','--name-only','-z',base,target).decode().strip('\0').split('\0')
    sha=lambda b:hashlib.sha256(b).hexdigest() if b is not None else None
    reviewed={'docs/architecture/LEGACY_CORE_ALLOWLIST.json','docs/architecture/MODULE_RULES.md'} if a.include_architecture_review else set()
    rows=[];pending=[]
    for name in paths:
        if not name or name.startswith('/') or '..' in Path(name).parts or not (name.startswith('app/') or name.startswith('docs/uiux/') or name=='progress.md' or name in reviewed):raise ValueError('Out-of-scope path '+name)
        before=git('show',base+':'+name,optional=True);after=git('show',target+':'+name,optional=True)
        path=a.project/name;current=path.read_bytes() if path.exists() else None;final=after
        if after is None:status='unsupported_deletion'
        elif current==after:status='already_matches'
        elif name in ['progress.md','docs/architecture/MODULE_RULES.md'] and before is not None and current is not None and after.startswith(before):
            delta=after[len(before):];status='already_appended' if delta in current else 'append_only';final=current if status=='already_appended' else current+delta
        elif name=='docs/architecture/LEGACY_CORE_ALLOWLIST.json' and name in reviewed and before is not None and current is not None:
            old,new,local=json.loads(before),json.loads(after),json.loads(current)
            additions=[x for x in new if x not in old]
            if not all(isinstance(x,str) for x in old+new+local) or len(set(new))!=len(new) or [x for x in new if x not in additions]!=old or not all(x in local for x in old):
                raise ValueError('Architecture review is not strictly additive')
            if any(not x.startswith('app/src/main/java/game/sanguo/mobile/') or not x.endswith('.java') or '*' in x or '..' in Path(x).parts for x in additions):
                raise ValueError('Architecture additions must be exact UI source paths')
            merged=local+[x for x in additions if x not in local]
            status='already_appended' if merged==local else 'append_only'
            final=current if merged==local else (json.dumps(sorted(merged),indent=2)+'\n').encode()
        elif current==before:status='safe_delta'
        else:status='conflict'
        rows.append(dict(path=name,status=status,base_sha256=sha(before),target_sha256=sha(after),current_sha256=sha(current),final_sha256=sha(final)))
        pending.append((path,current,final,status))
    report=dict(base=base,target=target,files=rows,applied=False);a.output.mkdir(parents=True,exist_ok=False)
    def write(name): (a.output/name).write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    write('preflight.json')
    if any(r['status'] in ['conflict','unsupported_deletion'] for r in rows):raise ValueError('Preflight conflict; no files changed')
    if a.apply:
        # Recheck entire batch before the first write; only frozen blobs are used.
        for path,current,_,_ in pending:
            if (path.read_bytes() if path.exists() else None)!=current:raise ValueError('Concurrent change; no files changed')
        for path,_,final,status in pending:
            if status in ['safe_delta','append_only']:path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(final)
        report['applied']=True;write('applied.json')
    print(json.dumps(dict(base=base,target=target,files=len(rows),applied=report['applied'])))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ['ui-repo','project','output']:p.add_argument('--'+name,required=True,type=Path)
    for name in ['base','target']:p.add_argument('--'+name,required=True)
    p.add_argument('--include-architecture-review',action='store_true',help='After manual review, permit additive exact legacy entries and appended module review text')
    p.add_argument('--apply',action='store_true');main(p.parse_args())
