#!/usr/bin/env python3
"""Deterministic original42x42 personnel table plus87 parent references."""
import argparse,json,hashlib
from pathlib import Path
def sha(b):return hashlib.sha256(b).hexdigest()
def export(receipt,directory):
 raw=receipt.read_bytes();assert sha(raw)=='4854c0bef36fe0ba4e20ba7abdb094a26fb2b7a832f24230b8417aa5a02be29d';d=json.loads(raw);blob=bytes.fromhex(d['blobHex']);assert len(blob)==42*42+87 and sha(blob)==d['resourceSha'];directory.mkdir(parents=True,exist_ok=True)
 manifest={k:d[k]for k in ['exeSha','tableAddress','tableSha','parentAddress','parentSha','resourceSha','limits']};manifest['receiptSha']=sha(raw)
 for name,data in [('personnel-travel.bin',blob),('personnel-travel-source.json',(json.dumps(manifest,indent=2)+'\n').encode())]:
  p=directory/name
  if p.exists()and p.read_bytes()!=data:raise ValueError('Preserve conflicting output '+str(p))
  p.write_bytes(data)
 print('PASS deterministic personnel import',sha(blob))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('receipt',type=Path);p.add_argument('directory',type=Path);a=p.parse_args();export(a.receipt,a.directory)
