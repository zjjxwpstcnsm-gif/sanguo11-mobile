#!/usr/bin/env python3
"""Original PE newgame option tables and instruction reference candidates."""
import argparse,json,struct
from pathlib import Path
from capstone import Cs,CS_ARCH_X86,CS_MODE_32
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve source inspection')
 raw=(installation/'san11pk.exe').read_bytes();assert sha(raw)==EXE_SHA;pe=struct.unpack_from('<I',raw,60)[0];op=struct.unpack_from('<H',raw,pe+20)[0];count=struct.unpack_from('<H',raw,pe+6)[0];base=struct.unpack_from('<I',raw,pe+52)[0];sections=[struct.unpack_from('<4I',raw,pe+24+op+40*i+8)for i in range(count)]
 def read(a,n):
  v,r,s,o=next(x for x in sections if x[1]<=a-base and a-base+n<=x[1]+x[2]);return raw[o+a-base-r:o+a-base-r+n]
 def bound(a,n):
  b=read(a,n);return dict(address=hex(a),bytes=n,sha=sha(b),instructions=[dict(address=hex(i.address),mnemonic=i.mnemonic,operands=i.op_str)for i in dis.disasm(b,a)])
 dis=Cs(CS_ARCH_X86,CS_MODE_32);dis.skipdata=True
 tables=[];refs=[]
 for a,n in [(0x8b7e50,3),(0x8b7e68,3),(0x8b7ee8,3),(0x8b7f1c,2)]:
  b=read(a,4*n);pointers=list(struct.unpack('<%dI'%n,b));labels=[]
  for p in pointers:
   text=read(p,128).split(b'\0')[0];labels.append(dict(pointer=hex(p),rawHex=text.hex(),big5=text.decode('big5')))
  tables.append(dict(address=hex(a),tableSha=sha(b),labels=labels))
  for v,r,s,o in sections:
   if r!=0x1000:continue
   part=raw[o:o+s];needle=struct.pack('<I',a-12);start=0
   while True:
    at=part.find(needle,start)
    if at<0:break
    operand=base+r+at;refs.append(dict(table=hex(a),operandAddress=hex(operand),context=bound(operand-48,160)));start=at+1
 flagRefs=[]
 for v,r,size,o in sections:
  if r!=0x1000:continue
  part=raw[o:o+size];at=0
  while True:
   at=part.find(struct.pack('<I',0x7201970),at)
   if at<0:break
   a=base+r+at;flagRefs.append(dict(operand=hex(a),context=bound(a-40,100)));at+=1
 setterRefs=[]
 for target in [0x482790,0x4827f0,0x482810,0x493f50,0x4a4170,0x4a3f80,0x4a42d0,0x4f289f,0x493ef0]:
  for v,r,size,o in sections:
   if r!=0x1000:continue
   part=raw[o:o+size]
   for at in range(len(part)-5):
    if part[at]==0xe8 and base+r+at+5+struct.unpack_from('<i',part,at+1)[0]==target:
     a=base+r+at;setterRefs.append(dict(target=hex(target),candidateCall=hex(a),context=bound(a-96,200)))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,tables=tables,refs=refs,setterRefs=setterRefs,flagRefs=flagRefs,chooserFunctions=[bound(0x546080,0x820),bound(0x5448d0,0x250)],acceptAndRootCandidates=[bound(0x544000,0x2080),bound(0x5468a0,0x700),bound(0x482000,0x1800),bound(0x547000,0x5800),bound(0x4a4000,0x650),bound(0x493f50,0x100),bound(0x4a42d0,0x700),bound(0x4f2800,0x400),bound(0x545b40,0x100),bound(0x55c000,0x1900),bound(0x483800,0x800),bound(0x493d00,0x720),bound(0x52d800,0x900),bound(0x4a4d00,0x3b0),bound(0x52dc50,0x3e0),bound(0x55b030,0x8d0),bound(0x493580,0x1c0),bound(0x43a8a0,0x90),bound(0x4dc620,0x90),bound(0x4dc7c0,0x90),bound(0x4edad0,0x60),bound(0x544900,0xd0)],limits=['Exact original static labels/pointer tables and bounded reference contexts, not UI enum mapping or default values','Raw immediate matches are candidates, require aligned function execution/chooser proof','No PC writes or imported runtime defaults']),ensure_ascii=False,indent=2)+'\n');print('PASS original option tables',len(refs),sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
