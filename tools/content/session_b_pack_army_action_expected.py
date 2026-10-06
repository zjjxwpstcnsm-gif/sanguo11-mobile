#!/usr/bin/env python3
"""Pack independent original16-source full AP driver evidence, never infer NPC activation."""
import argparse,json
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,sha
HEADER='# original598880/5986a0 after actualSHEX484090/date/postload, rawinitialAP0; normal owners0-41 only; source-specific identity/SHA'
def pack(folder,output):
 if output.exists():raise ValueError('Preserve previous output')
 rows=[HEADER];count=0;source_ids=set()
 for slot in range(16):
  report=json.loads((folder/('source-%02d.json'%slot)).read_text());source=report['source']
  if report['exeSha']!=EXE_SHA or source['slot']!=slot or not report['nativeRngUnchanged'] or len(report['rows'])!=47 or report['geographyContext']['verifiedRegionCells']!=40000:raise ValueError('Original source driver incomplete')
  if source['scenarioId']in source_ids or not source['scenarioId'].endswith(source['sourceSha256']):raise ValueError('Source identity differs')
  source_ids.add(source['scenarioId'])
  for native,row in enumerate(report['rows']):
   if row['nativeId']!=native:raise ValueError('Original army ordering differs')
   if not row['valid']or not 0<=row['owner']<42:continue
   if row['apBefore']!=0 or not 0<=row['apAfter']<=255:raise ValueError('Original initial driver fixture differs')
   rows.append(chr(9).join(map(str,[source['scenarioId'],source['sourceSha256'],native,row['apAfter']])));count+=1
 if count!=285 or len(source_ids)!=16:raise ValueError('Original normal-owner coverage differs')
 output.parent.mkdir(parents=True,exist_ok=True);output.write_text(chr(10).join(rows)+chr(10));print('PASS independent source AP fixture',count,'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('folder',type=Path);p.add_argument('output',type=Path);a=p.parse_args();pack(a.folder,a.output)
