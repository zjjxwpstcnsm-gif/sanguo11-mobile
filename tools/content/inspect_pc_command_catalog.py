#!/usr/bin/env python3
"""Read the original 44-name command lookup, without UI execution or enabled-state assumptions."""
import argparse,hashlib,json,struct
from pathlib import Path
from inspect_pc_effect_bindings import EXE_SHA
from inspect_pc_scenario_tail import NativeTailDecoder
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EIP

def inspect(exe):
 raw=exe.read_bytes()
 if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Unverified executable')
 d=NativeTailDecoder(raw);u=d.u
 guard='85c07c1983f82b7f148b0485c0f28a00'
 if bytes(u.mem_read(0x48f1a0,16)).hex()!=guard:raise ValueError('Original lookup changed')
 before=bytes(u.mem_read(0x7200000,0x300000));rows=[]
 for index in range(44):
  u.reg_write(UC_X86_REG_EAX,index);u.emu_start(0x48f1a0,0x48f1b0,count=20)
  if u.reg_read(UC_X86_REG_EIP)!=0x48f1b0:raise ValueError('Lookup boundary not reached')
  pointer=u.reg_read(UC_X86_REG_EAX);stored=struct.unpack('<I',u.mem_read(0x8af2c0+index*4,4))[0]
  if pointer!=stored:raise ValueError('Original result does not match source table')
  label=bytes(u.mem_read(pointer,128)).split(b'\0',1)[0]
  rows.append(dict(native_id=index,pointer=hex(pointer),raw_big5=label.hex(),native_label=label.decode('big5')))
 if before!=bytes(u.mem_read(0x7200000,0x300000)):raise ValueError('Lookup mutated world memory')
 return dict(schema=1,source_executable_sha256=EXE_SHA,source_path='san11pk.exe',lookup_start='0x48f1a0',lookup_end='0x48f1b0',lookup_hex=guard,table='0x8af2c0',records=rows,limits=['Names identify original command categories, not enabled commands in a scenario','Submenus, game settings, execution rules and MOD activation are not inferred','Original bounds and table lookup run; stop before presentation string allocation','PC installation read only; no Wine'])

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--exe',required=True,type=Path);p.add_argument('--output',required=True,type=Path);a=p.parse_args()
 if a.output.resolve()==a.exe.parent.resolve() or a.exe.parent.resolve() in a.output.resolve().parents:raise ValueError('Output inside read-only PC installation')
 result=inspect(a.exe);a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print('Verified original command labels:',len(result['records']))
