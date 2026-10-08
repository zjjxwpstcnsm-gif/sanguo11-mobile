#!/usr/bin/env python3
"""All16 original person runtime receipts to deterministic source resource."""
import argparse,gzip,json,hashlib
from pathlib import Path
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha
def export(folder,output,index):
 if output.exists()or index.exists():raise ValueError('Preserve previous import')
 manifest=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();sources=json.loads(manifest)['scenarios'];lines=['# PDU-PERSON-1 original postloader runtime state, not effective activation'];receipts=[]
 for n,source in enumerate(sources):
  p=folder/('source-%02d.json'%n);raw=p.read_bytes();r=json.loads(raw);assert r['exeSha']==EXE_SHA and r['source']==source and r['sourceManifestSha']==sha(manifest)and r['worldAndRngUnchanged'];assert len(r['rows'])==1100
  for native,x in enumerate(r['rows']):
   assert x['nativeId']==native
   lines.append('\t'.join(map(str,[n,source['scenarioId'],source['sourceVariant'],source['sourcePath'],source['sourceSha256'],native,x['serializedRecordSha']or'-',int(x['validPerson']),int(x['validObject']),x['rawStatus'],x['physicalHealth'],x['injury'],x['rawCurrentWar'],x['nativeSkill']])))
  receipts.append(dict(sourceIndex=n,id=source['scenarioId'],sha=source['sourceSha256'],reportSha=sha(raw),registeredValid=sum(x['nativeId']<670 and x['validPerson']for x in r['rows']),nativeValid=sum(x['validPerson']for x in r['rows'])))
 data=('\n'.join(lines)+'\n').encode();packed=gzip.compress(data,mtime=0);output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(packed)
 index.write_text(json.dumps(dict(format='PDU-PERSON-1',exeSha=EXE_SHA,sourceManifestSha=sha(manifest),decodedSha=sha(data),packedSha=sha(packed),rows=len(lines)-1,sources=receipts,limits=['Postloader fields are runtime facts; physical health is not serialized scenario property','770 valid native identities includes100 ancient templates; does not activate them','Only checked stable runtime/source joins may consume fresh records; loading old saves never refills mutable health','Complete normal campaign/Save/API/APK still required'],completeGoal=False),indent=2)+'\n');print('PASS deterministic16 source person state',len(lines)-1,'SHA',sha(packed),'decoded',sha(data))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('folder',type=Path);p.add_argument('output',type=Path);p.add_argument('index',type=Path);a=p.parse_args();export(a.folder,a.output,a.index)
