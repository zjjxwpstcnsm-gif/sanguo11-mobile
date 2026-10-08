#!/usr/bin/env python3
"""Deterministic five land templates from a pinned full original496570 receipt."""
import argparse,json,hashlib
from pathlib import Path
def digest(b):return hashlib.sha256(b).hexdigest()
def export(receipt,directory):
 raw=receipt.read_bytes();source_sha=digest(raw)
 if source_sha!='55adce37a51d52fd199dc8eed816a255bca4597a47fb6b185cbf6357984c02a2':raise ValueError('Original source receipt differs')
 d=json.loads(raw);rows=[]
 if d['exeSha']!='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb':raise ValueError('Original executable differs')
 for native in range(5):
  matches=[x for x in d['rows']if x['weapon']==native];templates=set(x['observed']['templateHex']for x in matches)
  if len(matches)!=24 or len(templates)!=1:raise ValueError('Original template coverage/identity differs')
  b=bytes.fromhex(next(iter(templates)));rows.append([native,b[0x55],b[0x56],digest(b)])
 resource=('# original496570 receipt '+source_sha+'\n'+''.join('\t'.join(map(str,x))+'\n'for x in rows)).encode()
 manifest=dict(exeSHA=d['exeSha'],receiptSHA=source_sha,resourceSHA=digest(resource),source=d['source'],nativeDomain=list(range(5)),rows=rows,limits=['Five land non-siege equipment templates only; full12/effects/water/transport not certified','Typed current computation not capability activation'])
 for name,data in [('unit-templates.tsv',resource),('unit-templates-source.json',(json.dumps(manifest,indent=2)+'\n').encode())]:
  p=directory/name
  if p.exists()and p.read_bytes()!=data:raise ValueError('Preserve conflicting earlier output '+str(p))
  directory.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
 print('PASS deterministic unit template export',digest(resource))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('receipt',type=Path);p.add_argument('directory',type=Path);a=p.parse_args();export(a.receipt,a.directory)
