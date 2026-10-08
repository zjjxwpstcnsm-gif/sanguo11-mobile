#!/usr/bin/env python3
"""Bounded original actor resolver and exact PC references, no rule substitution."""
import argparse,json,struct
from pathlib import Path
import capstone
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
p=argparse.ArgumentParser();p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();output_guard(a.installation,a.output)
if a.output.exists():raise ValueError('Preserve original receipt')
raw=(a.installation/'san11pk.exe').read_bytes();assert sha(raw)==EXE_SHA
h=struct.unpack_from('<I',raw,60)[0];n=struct.unpack_from('<H',raw,h+6)[0];opt=struct.unpack_from('<H',raw,h+20)[0];base=struct.unpack_from('<I',raw,h+52)[0];sections=[struct.unpack_from('<4I',raw,h+24+opt+40*i+8)for i in range(n)]
def read(ip,length):
 v,r,s,o=next(z for z in sections if z[1]<=ip-base<z[1]+max(z[0],z[2]));return raw[o+ip-base-r:o+ip-base-r+length]
dis=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);functions=[]
for ip,length in [(0x4ad960,0x40),(0x495a40,0x70),(0x4b2380,0x460),(0x4b27f0,0x30),(0x68e610,0x120),(0x4d7d50,0x90),(0x472c40,0x80)]:
 b=read(ip,length);functions.append(dict(address=hex(ip),bytes=b.hex(),sha256=sha(b),instructions=[dict(address=hex(i.address),mnemonic=i.mnemonic,operands=i.op_str)for i in dis.disasm(b,ip)]))
a.output.write_text(json.dumps(dict(exeSha=EXE_SHA,functions=functions,limits=['Aligned starts from already observed original direct calls; bounds may include adjacent functions','Static resolver plus dynamic caller receipt; normal APK and old save adoption remain separate'],completeGoal=False),indent=2)+'\n');print('PASS bounded actor resolvers',sha(a.output.read_bytes()))
