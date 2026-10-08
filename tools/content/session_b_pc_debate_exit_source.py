#!/usr/bin/env python3
"""Read-only original debate exit/terminal callers; candidates are not rules."""
import argparse,json,struct
from pathlib import Path
import capstone
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve earlier source receipt')
 raw=(installation/'san11pk.exe').read_bytes();assert sha(raw)==EXE_SHA
 pe=struct.unpack_from('<I',raw,60)[0];count=struct.unpack_from('<H',raw,pe+6)[0];optional=struct.unpack_from('<H',raw,pe+20)[0];base=struct.unpack_from('<I',raw,pe+52)[0]
 sections=[struct.unpack_from('<4I',raw,pe+24+optional+40*i+8)for i in range(count)]
 def read(address,length):
  virtual,rva,stored,offset=next(s for s in sections if s[1]<=address-base<s[1]+max(s[0],s[2]))
  data=raw[offset+address-base-rva:offset+address-base-rva+length]
  assert len(data)==length;return data
 dis=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
 def bounded(address,length):
  data=read(address,length)
  return dict(address=hex(address),length=length,sha256=sha(data),hex=data.hex(),instructions=[dict(address=hex(i.address),mnemonic=i.mnemonic,operands=i.op_str)for i in dis.disasm(data,address)])
 targets={0x51dd10,0x51dc30,0x51e220,0x51e300,0x523dc0,0x51fcf0,0x51d890,0x51e4f0}
 calls=[]
 for virtual,rva,stored,offset in sections:
  if rva!=0x1000:continue
  text=raw[offset:offset+stored]
  for i in range(len(text)-5):
   if text[i]!=0xe8:continue
   address=base+rva+i;target=address+5+struct.unpack_from('<i',text,i+1)[0]
   if target in targets:calls.append(dict(address=hex(address),target=hex(target),context=bounded(address-80,176)))
 functions=[bounded(a,n)for a,n in [(0x51dc30,0xe0),(0x51dd10,0x2a0),(0x51e220,0xe0),(0x523dc0,0x100),(0x51e300,0x100),(0x51e3c0,0x200),(0x520000,0x900),(0x523d80,0x900),(0x51f6e0,0x1a0),(0x523ca0,0xe0),(0x520450,0x70),(0x523fa0,0x3e),(0x51f750,0xe7)]]
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,calls=calls,functions=functions,limits=['Raw direct-call candidates require aligned containing-function and actual execution proof','No original GUI/exit/campaign or APK completion claim'],completeGoal=False),indent=2)+'\n')
 print('PASS original debate source scan',len(calls),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
