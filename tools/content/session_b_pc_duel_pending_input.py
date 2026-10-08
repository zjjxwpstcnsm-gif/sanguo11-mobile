#!/usr/bin/env python3
"""Original pending-input query and phase5 transitions with declared UI storage."""
import argparse,json,struct
from pathlib import Path
import capstone
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists():raise ValueError('Preserve receipt')
 d,w,source,geo,_=prepare(installation);md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);functions=[]
 for a,n in [(0x5116f0,0x40),(0x511700,0x30),(0x511730,0x100),(0x509fa0,0x1c3),(0x515f90,0x90)]:
  b=bytes(w.u.mem_read(a,n));functions.append(dict(address=hex(a),sha256=sha(b),instructions=[dict(address=hex(i.address),mnemonic=i.mnemonic,operands=i.op_str)for i in md.disasm(b,a)]))
 view=0xc600000;w.u.mem_map(view,0x5000);rows=[];world=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4))
 for value in range(6):
  w.u.mem_write(view+0x3ccc,struct.pack('<i',value));actual=w.call(0x5116f0,receiver=view);rows.append(dict(storage3ccc=value,original5116f0=actual))
 frames=[]
 for pending in [0,1,2]:
  for ready in [0,1]:
   w.call(0x50ab90,receiver=d.fixture);before=bytearray(w.u.mem_read(d.fixture,0x59c));struct.pack_into('<3i',before,4,5,-1,2);struct.pack_into('<i',before,0x14,pending);struct.pack_into('<I',before,0x228,view);w.u.mem_write(d.fixture,bytes(before));w.u.mem_write(view+0x3ccc,struct.pack('<i',ready));result=w.call(0x509fa0,1,receiver=d.fixture,count=10000000);after=bytes(w.u.mem_read(d.fixture,0x59c));frames.append(dict(pending=pending,ready=ready,beforeHex=before.hex(),afterHex=after.hex(),result=result,nativeRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]));assert struct.unpack_from('<i',after,0x14)[0]==(0 if pending==1 and ready else pending)
 assert world==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
 output.write_text(json.dumps(dict(exeSha=EXE_SHA,source=source,functions=functions,queries=rows,frames=frames,worldAndRngPure=True,limits=['Declared UI storage/phase5 sub2, no GUI execution','No damage/AI/RNG rule replacement or fabricated terminal']),indent=2)+'\n');print('PASS original pending queries',rows,'frames',len(frames),sha(output.read_bytes()))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
