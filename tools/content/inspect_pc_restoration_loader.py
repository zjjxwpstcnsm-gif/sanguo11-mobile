#!/usr/bin/env python3
"""Execute original slot admission/path-format dispatch without filesystem calls.

The bound is the original 43aa50 function, stopping before sprintf or the cookie
check. This proves path dispatch only, never the effective launch/MOD priority.
"""
import argparse
import hashlib
import json
import struct
from pathlib import Path
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP, UC_X86_REG_EIP, UC_X86_REG_EAX
from audit_pc_restoration_sources import EXE_SHA, output_guard, json_bytes


def execute(exe, indices):
    if hashlib.sha256(exe).hexdigest()!=EXE_SHA:
        raise ValueError('Pinned executable changed')
    u=Uc(UC_ARCH_X86,UC_MODE_32)
    u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000])
    u.mem_map(0x20000000,0x4000)
    def stop(u,address,size,user):
        if address in (0x708ba2,0x707b1a):u.emu_stop()
    u.hook_add(UC_HOOK_CODE,stop)
    memory_before=bytes(u.mem_read(0x400000,0x500000))
    rows=[]
    for index in indices:
        u.mem_write(0x20000000,bytes(0x4000))
        stack=0x20002000
        u.mem_write(stack,struct.pack('<IIi',0x30000000,0x20003000,index))
        u.reg_write(UC_X86_REG_ESP,stack)
        u.emu_start(0x43aa50,0,count=1000)
        boundary=u.reg_read(UC_X86_REG_EIP)
        valid=0<=index<255
        if boundary!=(0x708ba2 if valid else 0x707b1a):
            raise ValueError('Unexpected native path-format boundary')
        row=dict(slot=index,admitted=valid,stop=hex(boundary))
        if valid:
            sp=u.reg_read(UC_X86_REG_ESP)
            ret,destination,fmt,argument=struct.unpack('<IIII',u.mem_read(sp,16))
            if ret!=0x43aa9c or argument!=index or fmt!=0x7924f8:
                raise ValueError('Original format arguments changed')
            raw=bytes(u.mem_read(fmt,80)).split(b'\0',1)[0]
            if raw!=b'media\\scenario\\Scen%03d.s11':
                raise ValueError('Original installed scenario path changed')
            row.update(formatPointer=hex(fmt),format=raw.decode('ascii'),indexArgument=argument,
                       path=(raw.decode('ascii') % argument))
        elif u.reg_read(UC_X86_REG_EAX)!=0:
            raise ValueError('Original invalid-slot rejection changed')
        if bytes(u.mem_read(0x400000,0x500000))!=memory_before:
            raise ValueError('Path dispatch mutated executable/static memory')
        rows.append(row)
    return dict(schema=1,sourceExecutableSha256=EXE_SHA,
                originalFunction='43aa50',originalCodeSha256=hashlib.sha256(exe[0x3aa50:0x3aac6]).hexdigest(),
                sourcePathTable='8a5800 ->7924f8',sourceScenarioRecords='43b9b5 ctor480830/480d50 ->4937b0',
                sharedPathTable='8a57fc ->792514;43b8a0 original shared loader ->4937b0',
                rows=rows,readOnlyExecutableAndStaticBytes=True,
                limits=['Original code executed only through format admission, no host filesystem or Wine',
                        'sprintf output is descriptive formatting of captured original arguments',
                        'Actual menu enumeration, startup overrides, external patches and events unresolved'])


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation',type=Path)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args();output_guard(args.installation,args.output)
    report=execute((args.installation/'san11pk.exe').read_bytes(),[-128,-1]+list(range(256))+[999])
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_bytes(json_bytes(report))
    print(json.dumps(dict(cases=len(report['rows']),admitted=sum(r['admitted'] for r in report['rows']),sha256=hashlib.sha256(args.output.read_bytes()).hexdigest())))
