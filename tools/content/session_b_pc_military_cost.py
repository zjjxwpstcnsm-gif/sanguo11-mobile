#!/usr/bin/env python3
"""Execute original military no-discount price query, preserving source files."""
import argparse,json,struct,hashlib
from pathlib import Path
from inspect_pc_scenario_tail import NativeTailDecoder
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_ECX,UC_X86_REG_EIP,UC_X86_REG_EAX
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('installation',type=Path);parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
installation=args.installation.resolve();output_guard(installation,args.output)
if args.output.exists():raise ValueError('Preserve previous report')
exe=(installation/'san11pk.exe').read_bytes();assert sha(exe)==EXE_SHA
d=NativeTailDecoder(exe);shared=(installation/'Media/scenario/Scenario.s11').read_bytes();records=d.decode_tail(shared,True)['records'];u=d.u
# Real original496d90 unit object with its constructor's exact owner default.
unit=d.root+0x169730;vtable=struct.unpack('<I',u.mem_read(unit,4))[0];getter=struct.unpack('<I',u.mem_read(vtable+0x40,4))[0]
position=0xc200000;u.mem_map(position,0x1000);u.mem_write(position,struct.pack('<hh',10,10));u.mem_map(0x6fa0000,0x200000)
def call(address,*args,receiver=None):
 u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<'+'I'*(len(args)+1),d.stop,*args))
 if receiver is not None:u.reg_write(UC_X86_REG_ECX,receiver)
 u.emu_start(address,d.stop,count=1000000);assert u.reg_read(UC_X86_REG_EIP)==d.stop;return u.reg_read(UC_X86_REG_EAX)&0xffff
owner=call(getter,receiver=unit);before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));grid=bytes(u.mem_read(0x6fa0000,0x200000));rows=[]
for native in [3,4,5,6,7,8,9,10,11,12,13,16,17,18,19,20,21,22]:
 row=next(r for r in records if r['kind']=='table_79c54' and r['native_index']==native);definition=d.root+0x79c54+native*0xd0;actor=bytes.fromhex(row['actor_hex']);base=struct.unpack_from('<H',actor,0xc4)[0]
 actual=call(0x49db10,definition,unit,position,receiver=0x799895c);assert actual==base
 assert before==bytes(u.mem_read(0x7200000,0x300000))and rng==bytes(u.mem_read(0x8a5d44,4))and grid==bytes(u.mem_read(0x6fa0000,0x200000))
 rows.append(dict(nativeId=native,name=actor[4:0xb4].split(b'\0')[0].decode('big5'),baseCost=base,originalFunctionCost=actual,recordSha=row['sha256'],hpFieldC2=struct.unpack_from('<H',actor,0xc2)[0]))
report=dict(exeSha=EXE_SHA,sharedSha=sha(shared),rows=rows,unitOriginalConstructor='496d90',unitVirtualOwnerGetter=hex(getter),defaultOwnerUnsigned16=owner,worldRngPure=True,limits=['Native constructed unit exact default owner and empty province grid are a bounded no-discount fixture, not real force/region admission','Original49db10 base branch execution only; militaryHQ80percent effect separately pending','No cost/technology/terrain/action relaxation; no PC UI/normal campaign certification'])
args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(rows,ensure_ascii=False))
