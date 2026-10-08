#!/usr/bin/env python3
"""Strict original48d3b0 matrix import with each source preserved."""
import argparse,json,hashlib
from pathlib import Path
def sha(b):return hashlib.sha256(b).hexdigest()
def export(source,output,index):
 if output.exists()or index.exists():raise ValueError('Preserve import')
 raw=source.read_bytes();r=json.loads(raw);assert r['exeSha']=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'and r['rows']==670 and r['columns']==670 and r['wholeWorldAndRngPure']and not r['completeGoal'];matrix=b''.join(bytes.fromhex(row)for row in r['rowHex']);assert len(matrix)==670*670 and all(b in [0,1,2,3,255]for b in matrix);output.write_bytes(matrix);index.write_text(json.dumps(dict(source=r['source'],exeSha=r['exeSha'],receiptSha=sha(raw),matrixSha=sha(matrix),rows=670,columns=670,limits=['Initial original family graph only; current spouse join belongs to rules','Not670effective activation, not normal campaign/menus/APK proof'],completeGoal=False),indent=2)+'\n');print('PASS original matrix import',sha(matrix),'receipt',sha(raw))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('source',type=Path);p.add_argument('output',type=Path);p.add_argument('index',type=Path);a=p.parse_args();export(a.source,a.output,a.index)
