#!/usr/bin/env python3
"""Deterministic source0 initialized city neighbors; no all-source claim."""
import argparse,json,struct,hashlib
from pathlib import Path
def sha(b):return hashlib.sha256(b).hexdigest()
def export(receipt,directory):
 raw=receipt.read_bytes();assert sha(raw)=='955258479e3fea376fa87ed6d98cb00cc3a22557d820d9184cb04fa4f6b506cf';r=json.loads(raw);blob=struct.pack('<252i',*[n for row in r['neighbors']for n in row]);assert sha(blob)==r['neighborBytesSha'];directory.mkdir(parents=True,exist_ok=True)
 manifest=dict(exeSha=r['exeSha'],source=r['source'],receiptSha=sha(raw),resourceSha=sha(blob),limits=r['limits'])
 for name,data in [('personnel-neighbors.bin',blob),('personnel-neighbors-source.json',(json.dumps(manifest,indent=2)+'\n').encode())]:
  p=directory/name
  if p.exists()and p.read_bytes()!=data:raise ValueError('Preserve conflicting output '+str(p))
  p.write_bytes(data)
 print('PASS deterministic source0 personnel neighbors',sha(blob))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('receipt',type=Path);p.add_argument('directory',type=Path);a=p.parse_args();export(a.receipt,a.directory)
