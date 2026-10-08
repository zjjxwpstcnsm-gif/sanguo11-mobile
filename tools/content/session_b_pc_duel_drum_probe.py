#!/usr/bin/env python3
"""Read original generic facility force getter before binding DRUM admission."""
import argparse,json,struct
from pathlib import Path
import capstone
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
from unicorn.x86_const import UC_X86_REG_EIP
def inspect(installation,output):
 output_guard(installation,output)
 if output.exists()or output.with_suffix('.failure.json').exists():raise ValueError('Preserve receipt')
 d,w,source,geo,_=prepare(installation);facility=w.call(0x490d00,100,receiver=w.root);b=bytes(w.u.mem_read(facility,0x38));vtable=struct.unpack_from('<I',b)[0];getter=struct.unpack('<I',w.u.mem_read(vtable+0x40,4))[0];code=bytes(w.u.mem_read(getter,96));dis=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));value=w.call(getter,receiver=facility)
 rows=[];registry=0x46483b4+100*128;registry_before=bytes(w.u.mem_read(registry,4));w.u.mem_write(registry,struct.pack('<I',facility));point=80|(80<<16)
 def distance(x,y):
  dq=x-80;dr=y-(x-(x&1))//2-(80-(80-(80&1))//2);return max(abs(dq),abs(dr),abs(dq+dr))
 for kind in [11,12]:
  for owner in [2,3]:
   for complete in [0,1]:
    for x in range(76,85):
     for y in range(74,87):
      gap=distance(x,y)
      if gap>4:continue
      w.u.mem_write(facility+8,struct.pack('<3i',kind,owner,800));w.u.mem_write(facility+0x14,struct.pack('<i',complete));actual_owner=w.call(getter,receiver=facility);assert actual_owner==owner,(kind,owner,actual_owner);assert w.call(0x47a630,facility)==1
      grid=0x6fb0e68+(x*200+y)*20;old=bytes(w.u.mem_read(grid,20));declared=bytearray(old);struct.pack_into('<I',declared,0,(struct.unpack_from('<I',old)[0]&~3)|2);struct.pack_into('<H',declared,8,100);w.u.mem_write(grid,bytes(declared));assert w.call(0x483b20,receiver=grid)==facility
      fixture=bytes(w.u.mem_read(0x7200000,0x300000));actual=w.call(0x4843a0,point,3,0x589a50,2,0,receiver=0x6fb0e68,count=1000000);expected=int(kind==11 and owner==2 and complete!=0 and 0<gap<=3);assert actual==expected,(kind,owner,complete,x,y,gap,actual,expected);assert fixture==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));w.u.mem_write(grid,old);rows.append([kind,owner,complete,x,y,gap,actual])
 w.u.mem_write(registry,registry_before);w.u.mem_write(0x7200000,before)
 assert before==bytes(w.u.mem_read(0x7200000,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4));facts=dict(exeSha=EXE_SHA,source=source,geography=geo,facilityPointer=facility,facilityHex=b.hex(),ownerGetter=hex(getter),getterBytes=code.hex(),getterSha=sha(code),instructions=[dict(address=hex(i.address),mnemonic=i.mnemonic,operands=i.op_str)for i in dis.disasm(code,getter)],originalOwner=str(value),valid=bool(w.call(0x47a630,facility)),drumRows=rows,registryAndGridFixture=True,limits=['Original generic constructor100; kind/owner/completion and object-registry/grid link are declared fixtures; full original4843a0 scan/589a50/getters untouched; not ordinary placement/menu'],completeGoal=False);output.write_text(json.dumps(facts,indent=2)+'\n');print('PASS original generic facility getter',hex(getter),value,sha(output.read_bytes()),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
