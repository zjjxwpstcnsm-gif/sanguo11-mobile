#!/usr/bin/env python3
"""Extract only pinned original reference evidence; never apply sealed candidate code."""
import argparse,hashlib,io,json,tarfile,gzip
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
ARCHIVE_SHA='0bd4af09c4b39d8af4bc488c31c7263f42b5c5f0b1a3edfdb9811ddd9b1651be'
COMPRESSED_SHA='1538f1e6dbb120600e47421ad5cf835f29af443ad666dd60b6af21e4f6e7ec9b'
RAW_SHA='a3bf182f957f9b95200aed9e50d72355174a9c96bc3d6b14a24b7040ad50e8e8'
def sha(raw):return hashlib.sha256(raw).hexdigest()
def main(a):
 archive=ROOT/'docs/handoff/20261004/session1/batch28-unfinished-candidate-delta.tar.gz'
 if sha(archive.read_bytes())!=ARCHIVE_SHA:raise ValueError('Pinned evidence archive SHA differs')
 # The source archive is read solely as an evidence container. No Java is extracted.
 wanted='core/src/main/resources/pc-native-parents/'
 blobs={}
 with tarfile.open(archive,'r:gz')as t:
  for m in t:
   name=m.name.removeprefix('./')
   if name in {wanted+'references.bin.gz',wanted+'index.txt'}:
    if not m.isfile()or name in blobs:raise ValueError('Evidence member invalid/duplicate')
    blobs[name]=t.extractfile(m).read()
 if len(blobs)!=2:raise ValueError('Original evidence members absent')
 data=blobs[wanted+'references.bin.gz'];index=blobs[wanted+'index.txt'];raw=gzip.decompress(data)
 if sha(data)!=COMPRESSED_SHA or sha(raw)!=RAW_SHA:raise ValueError('Pinned original reference SHA differs')
 if sha(raw)!=index.decode('ascii').strip():raise ValueError('Original reference fingerprint differs')
 if raw[:4]!=bytes.fromhex('504e5031'):raise ValueError('Original reference format differs')
 probe=ROOT/'out/session-b/capacity-probe/references.bin.gz'
 if probe.exists()and probe.read_bytes()!=data:raise ValueError('Previously checked10720-row evidence differs')
 dest=a.output.resolve()
 if ROOT not in dest.parents:raise ValueError('Output must stay in own inheritance')
 rel=str(dest.relative_to(ROOT))
 if rel!='core/src/main/resources/pc-command-capacity'and not rel.startswith(('out/session-b/','core/src/main/resources/pc-command-capacity/')):raise ValueError('Exact B output ownership required')
 dest.mkdir(parents=True,exist_ok=True)
 for name,b in [('references.bin.gz',data),('index.txt',index)]:
  p=dest/name
  if p.exists()and p.read_bytes()!=b:raise ValueError('Preserve prior output')
  p.write_bytes(b)
 print(json.dumps(dict(archive=str(archive.relative_to(ROOT)),archiveSha256=sha(archive.read_bytes()),compressedSha256=sha(data),rawSha256=sha(raw),rawBytes=len(raw),scope='original evidence bytes only; no sealed policy code or valid-activation claim')))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);main(p.parse_args())
