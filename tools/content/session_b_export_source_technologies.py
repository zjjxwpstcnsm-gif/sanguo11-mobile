#!/usr/bin/env python3
"""Deterministic all16 force initial technology bits from pinned original getter."""
import argparse,json,hashlib
from pathlib import Path
def sha(b):return hashlib.sha256(b).hexdigest()
def export(receipt,directory):
 raw=receipt.read_bytes();assert sha(raw)=='23b0a9741c0966f96e746f996fbd4d6be2ca0ca949786b60b24cc8ba9ac6a0a1';d=json.loads(raw);rows=[]
 for src in d['rows']:
  s=src['source']
  for f in src['forces']:rows.append([s['scenarioId'],s['sourceSha256'],s['sourceVariant'],f['nativeId'],f['rawBits'],f['recordSha']])
 data=('# original4811e0 receipt '+sha(raw)+'\n'+''.join('\t'.join(map(str,r))+'\n'for r in rows)).encode()
 manifest=dict(exeSHA=d['exeSha'],receiptSHA=sha(raw),resourceSHA=sha(data),domainsSHA=d['domainsSha'],manifestSHA=d['manifestSha'],rows=len(rows),sources=16,forcesPerSource=47,techDomain=36,limits=d['limits'])
 directory.mkdir(parents=True,exist_ok=True)
 for name,b in [('source-technologies.tsv',data),('source-technologies-manifest.json',(json.dumps(manifest,indent=2)+'\n').encode())]:
  p=directory/name
  if p.exists()and p.read_bytes()!=b:raise ValueError('Preserve conflicting output '+str(p))
  p.write_bytes(b)
 print('PASS exact source tech import',len(rows),sha(data))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('receipt',type=Path);p.add_argument('directory',type=Path);a=p.parse_args();export(a.receipt,a.directory)
