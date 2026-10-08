#!/usr/bin/env python3
"""Pinned readonly ruler death/collapse instruction slices; no gameplay proof."""
import argparse,json,struct
from pathlib import Path
from capstone import Cs,CS_ARCH_X86,CS_MODE_32
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve source evidence')
 raw=(installation/'san11pk.exe').read_bytes();assert sha(raw)==EXE_SHA;p=struct.unpack_from('<I',raw,60)[0];op=struct.unpack_from('<H',raw,p+20)[0];n=struct.unpack_from('<H',raw,p+6)[0];base=struct.unpack_from('<I',raw,p+52)[0];sections=[struct.unpack_from('<4I',raw,p+24+op+40*i+8)for i in range(n)]
 def read(a,n):
  v,r,s,o=next(x for x in sections if x[1]<=a-base and a-base+n<=x[1]+x[2]);return raw[o+a-base-r:o+a-base-r+n]
 cs=Cs(CS_ARCH_X86,CS_MODE_32);functions=[]
 for a,n in [(0x4acbe0,0x340),(0x4b80a0,0xe20),(0x4b9080,0x410),(0x4a2cb0,0x2d0),(0x4aa680,0x510),(0x4acae0,0x100),(0x4cf480,0x80),(0x73d5b0,0x20),(0x4f52d0,0x110),(0x4f6330,0x160),(0x73bdd0,0x20),(0x477810,0x140),(0x475740,0x80),(0x4765c0,6),(0x4b7e70,0x210),(0x4b78f0,0x3c0),(0x4b5430,0x30),(0x4b5460,0x3f0),(0x4b5410,0x20),(0x48bd80,0x70),(0x48bc40,0x30),(0x48bc70,0x60),(0x4a75a0,0x390),(0x4a5af0,0x110),(0x4bbaa0,0x250),(0x4813e0,0x40),(0x481570,0x60),(0x4ab9a0,0x200),(0x48a7a0,0x20),(0x489970,0x50),(0x48a860,0x50),(0x4bd3b0,0x420),(0x4a0940,0x130),(0x4b3a20,0x150),(0x489730,0x50),(0x4a33b0,0xc0),(0x4a32f0,0xc0),(0x4a31e0,0xc0),(0x489e70,0x50),(0x489df0,0x80),(0x4cdf40,0x90),(0x484de0,0x40),(0x4a5bc0,0x120),(0x4ab770,0x230),(0x48d9a0,0x40),(0x4cf500,0x100),(0x4cf060,0x100),(0x47a600,0x30),(0x47a630,0x30),(0x4883f0,0x40),(0x488430,0x70)]:
  b=read(a,n);functions.append(dict(address=hex(a),boundedBytes=n,sha256=sha(b),instructions=[dict(address=hex(i.address),mnemonic=i.mnemonic,operands=i.op_str)for i in cs.disasm(b,a)]))
 preference=read(0x8a69a8,68);preferences=list(struct.unpack('<17I',preference))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,functions=functions,preferenceNativeIds=preferences,preferenceSha=sha(preference),limits=['Bounded slices retain true addresses/SHA, not inferred function ends','Only dynamic full original callbacks can prove successor/collapse behavior']),indent=2)+'\n');print('PASS ruler death source',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
