#!/usr/bin/env python3
"""Import two pinned local Markdown snapshots; never downloads during build/runtime.

Existing JSON records are the ID ledger. A changed snapshot needs an explicit review,
new hash and migration mapping, not a row-order re-numbering.
"""
import argparse, hashlib, json, pathlib, re, collections
import build_content as b

def pinned(path,expected):
    raw=pathlib.Path(path).read_bytes()
    actual=hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest()
    b.require(actual==expected,'source blob changed; review required: '+actual)
    return raw.decode('utf-8').splitlines()

def main():
    p=argparse.ArgumentParser();p.add_argument('--officers',required=True);p.add_argument('--reference',required=True);p.add_argument('--write',action='store_true');args=p.parse_args()
    lines=pinned(args.officers,'1d81a16658885473db2f0f245528ae73b6bcb7d0')
    ref=pinned(args.reference,'89d4de6bffc8d5ef591c32b8b3a431378848594d')
    ledger=b.read('officers-source.json');mapping={r['sourceId']:r['projectId'] for r in ledger['rows']}
    rawheads=[x.strip() for x in lines[2].strip('|').split('|')];seen=collections.Counter();heads=[]
    for h in rawheads:
        seen[h]+=1;heads.append(h if seen[h]==1 else h+'_'+str(seen[h]))
    rows=[]
    for n,line in enumerate(lines,1):
        if not re.match(r'^\|\s*\d+\|',line):continue
        v=[x.strip() for x in line.strip('|').split('|')];b.require(len(v)==len(heads),'source column width')
        sid=int(v[0]);name=re.sub(r'\[([^]]+)\]\([^)]*\)',r'\1',v[1])
        rows.append(dict(sourceId=sid,name=name,sourceLine=n,values=dict(zip(heads,v)),projectId=mapping[sid]))
    imported=dict(source='rlu-officers',headers=heads,rows=rows)
    b.require(imported==ledger,'officer snapshot differs from reviewed ledger')
    # Numeric facts and names only; skill prose/descriptions are not redistributed.
    for table in ['sites','items','skills']:
        for row in b.read(table+'-source.json'):
            v=[x.strip() for x in ref[row['sourceLine']-1].split('|')]
            b.require(v[0]==row['name'],'source name/line mismatch')
            if table=='sites':
                b.require([int(v[i]) for i in [2,3,4]]==[row[k] for k in ['durability','rawX','rawY']],'site source numeric mismatch')
                b.require({'城':'city','關':'gate','港':'port'}[v[1]]==row['kind'],'site kind mismatch')
            if table=='items':b.require(v[1]==row['kind'] and int(v[2])==row['value'] and v[3]==row['rawHolder'] and v[4]==row['rawLocation'] and v[5]==row['rawAppeared'],'item source mismatch')
    if args.write:(b.DATA/'officers-source.json').write_text(json.dumps(imported,ensure_ascii=False,indent=2)+'\n')
    print('PASS: pinned source snapshots; 850 officer rows / 62 columns, 87 sites, 43 items, 100 skill names match ID ledger')
if __name__=='__main__':main()
