#!/usr/bin/env python3
"""Decode installed message resource record zero with original4711c0 code.

Text indexing and biography-to-person bindings require separate original getter
evidence. The outputs here are quarantined decoded bytes, never generated prose.
"""
import argparse
import hashlib
import json
import struct
from pathlib import Path
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32,UC_PROT_READ,UC_PROT_EXEC
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP
from audit_pc_restoration_sources import EXE_SHA,output_guard,json_bytes,sha


def decode(exe,raw):
    if sha(exe)!=EXE_SHA:raise ValueError('Executable changed')
    if raw[:4]!=b'LS11' or len(raw)<288 or len(set(raw[16:272]))!=256:
        raise ValueError('Unreviewed LS11 header/dictionary')
    compressed,decoded,start=struct.unpack_from('>III',raw,272)
    if start!=288 or len(raw)!=compressed+start or not 0<decoded<0x200000:
        raise ValueError('Unreviewed single-record LS11 boundaries')
    u=Uc(UC_ARCH_X86,UC_MODE_32)
    u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000]);u.mem_protect(0x400000,0x500000,UC_PROT_READ|UC_PROT_EXEC)
    context,source,dictionary,output,stack,stop=0x10000000,0x11000000,0x11100000,0x12000000,0x20002000,0x30000000
    u.mem_map(context,4096);u.mem_map(source,0x100000);u.mem_write(source,raw[start:])
    u.mem_protect(source,0x100000,UC_PROT_READ)
    u.mem_map(dictionary,4096);u.mem_write(dictionary,raw[16:272]);u.mem_protect(dictionary,4096,UC_PROT_READ)
    u.mem_map(output,0x210000);u.mem_write(output,bytes([0xa5])*0x210000)
    u.mem_map(stack-8192,0x4000);u.mem_map(stop,4096)
    u.mem_write(stack,struct.pack('<6I',stop,source,compressed,dictionary,output+4096,decoded))
    u.reg_write(UC_X86_REG_ECX,context);u.reg_write(UC_X86_REG_ESP,stack)
    u.emu_start(0x4711c0,stop,count=200000000)
    if u.reg_read(UC_X86_REG_EIP)!=stop:raise ValueError('Original decoder did not return')
    if bytes(u.mem_read(output,4096))!=bytes([0xa5])*4096 or bytes(u.mem_read(output+4096+decoded,0x210000-4096-decoded))!=bytes([0xa5])*(0x210000-4096-decoded):
        raise ValueError('Original decoder escaped declared output length')
    #471090 buffers4KiB: +4 is the loaded-end pointer, +8 remaining input,
    #+14 is the actual byte cursor. +0 is a file handle (zero for memory input).
    loaded_end,remaining=struct.unpack('<II',u.mem_read(context+4,8))
    consumed=struct.unpack('<I',u.mem_read(context+0x14,4))[0]-source
    if loaded_end-source+remaining!=compressed:raise ValueError('Original input accounting changed')
    if not 0<consumed<=compressed:raise ValueError('Original input cursor escaped declared length')
    return bytes(u.mem_read(output+4096,decoded)),dict(compressedBytes=compressed,decodedBytes=decoded,
        inputOffset=start,originalInputConsumed=consumed,originalFunction='4711c0',
        codeSha256=sha(exe[0x71090:0x712db]),outputGuards=True,sourceAndDictionaryReadOnly=True)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('installation',type=Path);parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args();output_guard(args.installation,args.output)
    exe=(args.installation/'san11pk.exe').read_bytes();args.output.mkdir(parents=True,exist_ok=True);rows=[]
    for path in sorted((args.installation/'Media/msg').glob('*')):
        raw=path.read_bytes();data,proof=decode(exe,raw);target=args.output/(path.name+'.decoded.bin');target.write_bytes(data)
        row=dict(sourcePath=path.relative_to(args.installation).as_posix(),sourceSha256=sha(raw),decodedSha256=sha(data),
                 firstBytes=data[:32].hex(),**proof);rows.append(row)
        print(json.dumps(row))
    (args.output/'message-resources-native.json').write_bytes(json_bytes(dict(schema=1,sourceExecutableSha256=EXE_SHA,resources=rows,
        limits=['Original decompression only; message IDs and biography/person mapping not yet established',
                'No runtime text imported, no PC installation write, no Wine'])))
