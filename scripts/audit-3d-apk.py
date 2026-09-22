#!/usr/bin/env python3
"""APK byte ledger and independent ELF / ZIP 16 KiB alignment checks."""
import argparse, collections, hashlib, json, struct, zipfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('apk',type=Path);p.add_argument('--abi');p.add_argument('--require-16k',action='store_true');a=p.parse_args()
ledger=collections.defaultdict(lambda:dict(zip_bytes=0,unpacked_bytes=0,count=0));native=[];abis=set();bad=[]
with zipfile.ZipFile(a.apk) as z:
 for e in z.infolist():
  n=e.filename
  group='dex' if n.endswith('.dex') else 'native' if n.startswith('lib/') else 'models' if n.endswith('.glb') else 'materials' if n.endswith('.filamat') else 'textures' if n.endswith(('.png','.jpg','.webp','.etc2')) else 'other'
  for key,value in [('zip_bytes',e.compress_size),('unpacked_bytes',e.file_size),('count',1)]:ledger[group][key]+=value
  if group=='native':
   abis.add(n.split('/')[1]);b=z.read(n);assert b[:4]==b'\x7fELF';endian='<' if b[5]==1 else '>';is64=b[4]==2
   phoff=struct.unpack_from(endian+('Q' if is64 else 'I'),b,32 if is64 else 28)[0]
   size,count=struct.unpack_from(endian+'HH',b,54 if is64 else 42);segments=[]
   for i in range(count):
    off=phoff+i*size;kind=struct.unpack_from(endian+'I',b,off)[0]
    if kind!=1:continue
    align=struct.unpack_from(endian+('Q' if is64 else 'I'),b,off+(48 if is64 else 28))[0]
    segments.append(align)
   with a.apk.open('rb') as f:
    f.seek(e.header_offset+26);name,extra=struct.unpack('<HH',f.read(4))
   offset=e.header_offset+30+name+extra
   elf_ok=bool(segments) and min(segments)>=16384
   zip_ok=e.compress_type!=zipfile.ZIP_STORED or offset%16384==0
   native.append(dict(path=n,load_alignment=segments,elf_16k=elf_ok,zip_16k=zip_ok,compressed=e.compress_type!=0))
   if not elf_ok or not zip_ok:bad.append(n)
size=a.apk.stat().st_size
report=dict(apk=a.apk.name,sha256=hashlib.sha256(a.apk.read_bytes()).hexdigest(),apk_bytes=size,abis=sorted(abis),ledger=ledger,native=native,target_bytes=300*1024**2,warning_bytes=400*1024**2,installed_bytes=None,cache_bytes=None,physical_device_16k_verified=False)
print(json.dumps(report,indent=2))
if a.abi and abis!={a.abi}:raise SystemExit('Unexpected APK ABI set')
if size>400*1024**2:raise SystemExit('APK exceeds 400 MiB warning budget')
if a.require_16k and bad:raise SystemExit('16 KiB alignment fails: '+', '.join(bad))
