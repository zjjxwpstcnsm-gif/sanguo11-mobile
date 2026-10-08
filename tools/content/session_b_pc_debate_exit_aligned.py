#!/usr/bin/env python3
"""Pinned EXE exact getters and aligned caller slices, no reconstructed GUI or rule return."""
from pathlib import Path
import argparse,struct,json
import capstone
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(pc,out):
 output_guard(pc,out);assert not out.exists();exe=(pc/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
 pe=struct.unpack_from('<I',exe,60)[0];n=struct.unpack_from('<H',exe,pe+6)[0];op=struct.unpack_from('<H',exe,pe+20)[0];base=struct.unpack_from('<I',exe,pe+52)[0];sections=[struct.unpack_from('<4I',exe,pe+24+op+40*i+8)for i in range(n)]
 def read(a,l):
  virtual,rva,stored,offset=next(s for s in sections if s[1]<=a-base and a-base+l<=s[1]+s[2]);b=exe[offset+a-base-rva:offset+a-base-rva+l];assert len(b)==l;return b
 dis=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
 def slice(a,l):
  b=read(a,l);return dict(address=hex(a),bytes=l,sha256=sha(b),hex=b.hex(),instructions=[dict(address=hex(i.address),mnemonic=i.mnemonic,operands=i.op_str)for i in dis.disasm(b,a)])
 functions=[slice(a,l)for a,l in [(0x524020,0x10),(0x524030,0x50),(0x524080,0x20),(0x523ca0,0x120),(0x51f660,0x130),(0x51f770,0xd0),(0x51e250,0x30),(0x51e260,0x40),(0x51dd10,0x2a0),(0x520010,0x30),(0x520030,0x100),(0x520130,0x300),(0x83ada8,0x100),(0x5204c0,0x900),(0x51dff0,0x120),(0x51e110,0x40),(0x5201f0,0x90),(0x5205b0,0xd0),(0x51bab0,0x70),(0x518710,0x100),(0x520050,0x30),(0x520280,0x180)]]
 calls=[];targets={0x524020,0x524030,0x524080}
 for virtual,rva,stored,offset in sections:
  if rva!=0x1000:continue
  text=exe[offset:offset+stored]
  for i in range(len(text)-5):
   if text[i]==0xe8:
    a=base+rva+i;t=a+5+struct.unpack_from('<i',text,i+1)[0]
    if t in targets:calls.append(dict(address=hex(a),target=hex(t),context=slice(a-64,136)))
 out.write_text(json.dumps(dict(exeSha=EXE_SHA,functions=functions,callCandidates=calls,limits=['Entry alignment must be checked for each bounded slice','Raw caller candidates do not certify voluntary exit or original GUI context','No runtime or save change']),indent=2)+'\n');print('PASS exact exit getter/caller evidence',len(calls),sha(out.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
