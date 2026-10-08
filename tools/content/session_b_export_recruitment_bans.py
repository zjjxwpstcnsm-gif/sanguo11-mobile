#!/usr/bin/env python3
"""Derive guarded source ban metadata only; never adopt sealed candidate code."""
import argparse,gzip,json,struct,tarfile,io
from pathlib import Path
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
ARCHIVE_SHA='635bb2f3cd3ddcdea62aad5a72981710817cc953d9f8b3b35fc06d2e3c0312d1'
DECODED_SHA='ec5d3f35d4796699a4a988c9789b16c7e15bc86da1c37fc2265061aa55b41502'
def build(installation,archive,output,receipt,check):
 output_guard(installation,output);output_guard(installation,receipt);assert sha((installation/'san11pk.exe').read_bytes())==EXE_SHA
 packed=archive.read_bytes();assert sha(packed)==ARCHIVE_SHA
 with tarfile.open(fileobj=io.BytesIO(packed)) as t:raw=gzip.decompress(t.extractfile('core/src/main/resources/pc-native-campaign/state.bin.gz').read())
 assert sha(raw)==DECODED_SHA;stream=io.BytesIO(raw)
 def integer():return struct.unpack('>i',stream.read(4))[0]
 def text():return stream.read(integer()).decode('utf8')
 assert integer()==0x504e4331 and text()==EXE_SHA;original_receipt=text();assert original_receipt=='20ca65baaec63303c680b75a3d1e19abb0931d5a26400506c13ccdf1f5f1eeea';assert integer()==16
 manifest_path=Path(__file__).resolve().parents[2]/'docs/handoff/20261004/session1/source-manifest.json';manifest_bytes=manifest_path.read_bytes();manifest={s['scenarioId']:s for s in json.loads(manifest_bytes)['scenarios']};rows=['# PDU-BAN-1 original58/59; guarded archived raw data only, no candidate code'];coverage=[]
 for index in range(16):
  ident,path,variant,source_sha=[text() for _ in range(4)];s=manifest[ident];assert (path,variant,source_sha)==(s['sourcePath'],s['sourceVariant'],s['sourceSha256']);source=(installation/path).read_bytes();assert sha(source)==source_sha and integer()==850
  for native in range(850):
   runtime_id,native_id=integer(),integer();record_sha=text();loyalty,ban,months,captive=[integer()for _ in range(4)];assert native_id==native and sha(source[17760+152*native:17760+152*(native+1)])==record_sha;assert ban==-1 and months==0 and captive==0
   rows.append('\t'.join(map(str,[index,ident,source_sha,variant,native,record_sha,ban,months])))
  assert integer()==47
  for force in range(47):
   assert integer()==force;valid=integer();assert valid in [0,1]
   for other in range(47):
    friendship,ally,relation=[integer()for _ in range(3)];assert 0<=friendship<=100 and ally in [0,1]
  coverage.append(dict(sourceIndex=index,sourceId=ident,sourceSha=source_sha,sourceVariant=variant,records=850,recordBytesComparedToReadonlyInstallation=True))
 assert not stream.read();data=('\n'.join(rows)+'\n').encode();report=dict(exeSha=EXE_SHA,archiveSha=ARCHIVE_SHA,archivedOriginalReceiptSha=original_receipt,decodedInputSha=DECODED_SHA,manifestSha=sha(manifest_bytes),resourceSha=sha(data),records=13600,coverage=coverage,limits=['Archived original data rejoined with current manifest and all original serialized record bytes; candidate production code not adopted','Read boundary bans-1/months0, not full startup events','670 mapped participants only; extra/ancient/NPC activation still separate'],completeGoal=False)
 if check:assert output.read_bytes()==data and json.loads(receipt.read_text())==report
 else:
  assert not output.exists() and not receipt.exists();output.write_bytes(data);receipt.write_text(json.dumps(report,indent=2)+'\n')
 print('PASS source ban metadata',len(rows)-1,sha(data),'check'if check else'write')
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--archive',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--receipt',type=Path,required=True);p.add_argument('--check',action='store_true');a=p.parse_args();build(a.installation,a.archive,a.output,a.receipt,a.check)
