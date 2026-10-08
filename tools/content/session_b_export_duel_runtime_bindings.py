#!/usr/bin/env python3
"""Deterministic16-source native person references/item identities import."""
import argparse,gzip,json
from pathlib import Path
from audit_pc_restoration_sources import ROOT,EXE_SHA,sha
def export(folder,output,index):
 if output.exists()or index.exists():raise ValueError('Preserve earlier import')
 manifest=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();sources=json.loads(manifest)['scenarios'];lines=['# PDU-BINDING-1 postloader references/items, not activation'];receipts=[]
 for n,source in enumerate(sources):
  raw=(folder/('source-%02d.json'%n)).read_bytes();r=json.loads(raw);assert r['exeSha']==EXE_SHA and r['source']==source and r['worldAndRngPure'] and len(r['people'])==670 and len(r['items'])==100
  prefix=[n,source['scenarioId'],source['sourceSha256'],source['sourceVariant']]
  for i,p in enumerate(r['people']):
   assert p['nativeId']==i and p['valid']
   lines.append('\t'.join(map(str,prefix+['P',i,p['internalFather'],p['publicFather'],p['publicMother'],p['spouse'],p['swornGroup'],p['birthplace'],p['rawLoyalty'],p['originalTreasureBonus']])))
  for i,t in enumerate(r['items']):
   assert t['nativeId']==i
   lines.append('\t'.join(map(str,prefix+['I',i,int(t['valid']),t['kind'],t['owner'],t['runtimeSha'],t['runtimeHex']])))
  receipts.append(dict(source=n,id=source['scenarioId'],sha=source['sourceSha256'],reportSha=sha(raw)))
 data=('\n'.join(lines)+'\n').encode();packed=gzip.compress(data,mtime=0);output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(packed);index.write_text(json.dumps(dict(format='PDU-BINDING-1',exeSha=EXE_SHA,manifestSha=sha(manifest),decodedSha=sha(data),packedSha=sha(packed),rows=len(lines)-1,sources=receipts,limits=['Post493400 source runtime, full opening events unknown','Original native item IDs/owners never project names/IDs','Source validity does not prove active character','Fresh saved facts only; old saves never filled'],completeGoal=False),indent=2)+'\n');print('PASS original runtime bindings import',len(lines)-1,sha(packed),sha(data))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('folder',type=Path);p.add_argument('output',type=Path);p.add_argument('index',type=Path);a=p.parse_args();export(a.folder,a.output,a.index)
