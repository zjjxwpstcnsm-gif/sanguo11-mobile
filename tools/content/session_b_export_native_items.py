#!/usr/bin/env python3
"""Exact native item definitions, independently joined against existing IDs."""
import argparse,csv,json,struct
from pathlib import Path
from audit_pc_restoration_sources import sha,EXE_SHA
def export(folder,output):
 if output.exists():raise ValueError('Preserve previous import')
 rows=list(csv.DictReader(open('core/src/main/resources/content/items.tsv'),delimiter='\t'));assert len(rows)==43;lines=['# PDU-ITEM-1 source native item definitions; exact raw-name/type/value identity join']
 for source in range(16):
  raw=(folder/('source-%02d.json'%source)).read_bytes();r=json.loads(raw);assert r['exeSha']==EXE_SHA and r['worldAndRngPure']
  for native,row in enumerate(rows):
   t=r['items'][native];b=bytes.fromhex(t['runtimeHex']);name=b[4:36].split(b'\0')[0];value=struct.unpack_from('<i',b,0x3c)[0]
   assert t['nativeId']==native and t['valid'] and name.decode('big5')==row['name'] and t['kind']==['名馬','名劍','長柄','暗器','名弓','書籍','玉璽','銅雀'].index(row['kind']) and value==int(row['value'])
   lines.append('\t'.join(map(str,[source,r['source']['scenarioId'],r['source']['sourceSha256'],native,row['id'],name.hex(),row['name'],t['kind'],value,t['runtimeSha'],sha(raw)])))
 output.write_text('\n'.join(lines)+'\n');print('PASS exact original item definition joins',len(lines)-1,'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('folder',type=Path);p.add_argument('output',type=Path);a=p.parse_args();export(a.folder,a.output)
