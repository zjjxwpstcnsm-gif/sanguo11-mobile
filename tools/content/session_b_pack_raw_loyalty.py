#!/usr/bin/env python3
"""Deterministic16 distinct original raw/getter facts; no playable roster inference."""
import argparse,gzip,json
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,sha
def pack(folder,output):
 if output.exists():raise ValueError('Preserve earlier import')
 lines=['# original byteAC versus property23; constructed/valid does not prove activation; EXE '+EXE_SHA,'# sourceId\tsourceSha\tnativeId\trecordShaOrUnknown\trawLoyalty\tgetterLoyalty\tstatus\thome\tvalidPerson\tvalidObject'];sources=set();count=0
 for slot in range(16):
  report=json.loads((folder/('source-%02d.json'%slot)).read_text());src=report['source'];assert report['exeSha']==EXE_SHA and report['wholeWorldAndRngUnchanged'] and len(report['rows'])==1100 and src['slot']==slot and src['scenarioId']not in sources and src['scenarioId'].endswith(src['sourceSha256']);sources.add(src['scenarioId'])
  assert report['geography']['verifiedRegionCells']==40000
  for native,row in enumerate(report['rows']):
   assert row['nativeId']==native and 0<=row['rawLoyalty']<=255
   record=row['serializedRecordSha']or'?'
   if record!='?'and(len(record)!=64 or any(c not in'0123456789abcdef'for c in record)):raise ValueError('Original record SHA invalid')
   values=[src['scenarioId'],src['sourceSha256'],native,record,row['rawLoyalty'],row['getterLoyalty'],row['rawStatus'],row['rawHome'],int(row['validPerson']),int(row['validObject'])];lines.append('\t'.join(map(str,values)));count+=1
 assert len(sources)==16 and count==17600
 raw=('\n'.join(lines)+'\n').encode();packed=gzip.compress(raw,mtime=0);output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(packed);print('PASS original domains',count,'packedSHA',sha(packed),'rawSHA',sha(raw),'bytes',len(packed),'not effective people coverage')
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('folder',type=Path);p.add_argument('output',type=Path);a=p.parse_args();pack(a.folder,a.output)
