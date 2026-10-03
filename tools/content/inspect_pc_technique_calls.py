#!/usr/bin/env python3
"""Reproducible original direct-call inventory, without interpreting dynamic reward operands."""
from pathlib import Path
import argparse,hashlib,json,struct
from collections import deque
from capstone import Cs,CS_ARCH_X86,CS_MODE_32
from capstone.x86 import X86_OP_IMM
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve();source=Path('/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版').resolve()
if out==source or source in out.parents:raise ValueError('PC directory read-only')
out.mkdir(parents=True,exist_ok=False);raw=(source/'san11pk.exe').read_bytes();digest=hashlib.sha256(raw).hexdigest();assert digest=='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
pe=struct.unpack_from('<I',raw,0x3c)[0];base=struct.unpack_from('<I',raw,pe+52)[0];n=struct.unpack_from('<H',raw,pe+6)[0];opt=struct.unpack_from('<H',raw,pe+20)[0];md=Cs(CS_ARCH_X86,CS_MODE_32);md.detail=True;md.skipdata=True;rows=[];history=deque(maxlen=24)
for i in range(n):
 at=pe+24+opt+40*i
 if raw[at:at+8].rstrip(b'\0')!=b'.text':continue
 vs,va,size,offset=struct.unpack_from('<IIII',raw,at+8)
 for ins in md.disasm(raw[offset:offset+size],base+va):
  if not ins.id:history.clear();continue
  text=dict(address=hex(ins.address),bytes=ins.bytes.hex(),instruction=ins.mnemonic+' '+ins.op_str)
  if ins.mnemonic=='call' and ins.operands and ins.operands[0].type==X86_OP_IMM and ins.operands[0].imm in [0x4b6460,0x4b6580]:
   target=ins.operands[0].imm;assert ins.bytes[0]==0xe8;assert ins.address+5+struct.unpack('<i',ins.bytes[1:])[0]==target
   rows.append(dict(call=hex(ins.address),target=hex(target),raw_file_offset=offset+ins.address-base-va,context=list(history)+[text],interpretation='pending source command and dynamic operands'))
  history.append(text)
(out/'calls.json').write_text(json.dumps(dict(source_exe_sha256=digest,scope='exact original direct call inventory only; no execution/admission/runtime identity claim',count=len(rows),rows=rows),indent=2)+'\n');print('Original direct technique callsites',len(rows),flush=True)
