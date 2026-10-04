#!/usr/bin/env python3
"""Execute the original FCE loader and prove every file-index -> registry/face/form mapping.

Only fopen/fseek/fread and the source byte reader are shimmed; no Wine, game or install writes.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32,UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP

EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'


def inspect(installation,output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside source required')
    exe=(installation/'san11pk.exe').read_bytes();source=(installation/'Media/face/San11Face00.fce').read_bytes()
    if hashlib.sha256(exe).hexdigest()!=EXE_SHA:raise ValueError('Original executable guard')
    _,version,start,count,images=struct.unpack_from('<5I',source)
    if version!=64 or images!=3*count or start+count>2400:raise ValueError('Source header')
    u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000])
    manager,stack,stop=0x10000000,0x20000000,0x30000000
    u.mem_map(manager,0x10000);u.mem_map(stack,0x2000);u.mem_map(stop,4096);u.mem_map(0x6fae000,0x4000)
    reads=[]
    def io(machine,address,size,user):
        sp=machine.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',machine.mem_read(sp,4))[0];consumed=0
        if address==0x70891d:value=1
        elif address==0x708462:value=0
        elif address==0x708649:
            destination,item_size,item_count,handle=struct.unpack('<4I',machine.mem_read(sp+4,16))
            if (item_size,item_count,handle)!=(1,20,1):raise ValueError('Unexamined original header IO')
            machine.mem_write(destination,source[:20]);value=20
        else:
            file_id,offset,length,destination=struct.unpack('<4I',machine.mem_read(sp+4,16))
            if file_id!=0 or offset+length>len(source):raise ValueError('Original FCE byte range')
            machine.mem_write(destination,source[offset:offset+length]);reads.append(dict(offset=offset,bytes=length,destination=hex(destination)))
            consumed=16;value=1
        machine.reg_write(UC_X86_REG_EAX,value);machine.reg_write(UC_X86_REG_ESP,sp+4+consumed);machine.reg_write(UC_X86_REG_EIP,ret)
    for address in [0x70891d,0x708462,0x708649,0x46ddf0]:u.hook_add(UC_HOOK_CODE,io,begin=address,end=address)
    u.mem_write(stack+4096,struct.pack('<III',stop,0,stop));u.reg_write(UC_X86_REG_ESP,stack+4096);u.reg_write(UC_X86_REG_ECX,manager)
    u.emu_start(0x46e360,stop,count=10000)
    if u.reg_read(UC_X86_REG_EAX)!=1 or u.reg_read(UC_X86_REG_EIP)!=stop:raise ValueError('Original FCE loader did not return success')
    if bytes(u.mem_read(0x6fae8b8+start*4,count*4))!=source[20:20+count*4]:raise ValueError('Native descriptor bytes differ')
    table=list(struct.iter_unpack('<II',source[20+count*4:20+count*4+images*8]));rows=[]
    for index,entry in enumerate(table):
        group=0 if index<count else 1+(index-count)%2
        face=start+index if index<count else start+(index-count)//2
        registry=face if group==0 else 2400+face*2+group-1
        observed=struct.unpack('<II',u.mem_read(manager+registry*8,8))
        if observed!=entry:raise ValueError('Original registry entry differs from file index '+str(index))
        rows.append(dict(fileResourceIndex=index,sourceRegistryIndex=registry,faceId=face,imageGroup=group,offset=entry[0],bytes=entry[1]))
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceFaceSha256=hashlib.sha256(source).hexdigest(),nativeChecks=len(rows)+1,
                nativeLoader='46e360..46e469',codeSha256=hashlib.sha256(exe[0x6e360:0x6e46c]).hexdigest(),reads=reads,entries=rows,
                layout='large[count], then interleaved (small1,small2)[count]; registry large=face; small=2400+2*face+form-1',
                sourceSearchStatic=['796228 media/face/San11Face00.fce first','Documents path Koei/San11 Tc/FaceData/San11Face??.fce enumeration','San11ChangeFace.fce separate CHGF path'],
                limits=['Only supplied primary FCE loaded; external user FaceData files/MOD live priority remain unverified.',
                        'Form roles (list/dialogue/duel/civil/military) still require corresponding original UI caller proof.',
                        'File IO shim copies original bytes only; no game, gameplay RNG or Wine execution.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:v for k,v in report.items() if k not in ('entries','reads','limits')}))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();inspect(a.installation,a.output)
