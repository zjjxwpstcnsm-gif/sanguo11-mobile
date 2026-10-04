#!/usr/bin/env python3
"""Run the original message interpreter on a pinned, decompressed local resource.

Only the memory-file IO interface is supplied by the host. Constructors, buffer
allocation, linked resource registry, message indexing and text opcodes execute
unaltered original x86. Active scenario resource priority remains a separate gate.
"""
import argparse
import gzip
import json
import struct
from pathlib import Path
from unicorn import UC_HOOK_CODE,UC_PROT_READ
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EAX,UC_X86_REG_EIP,UC_X86_REG_ESI,UC_X86_REG_ECX
from inspect_pc_officer_fields import NativeFields
from inspect_pc_message_resources import decode
from audit_pc_restoration_sources import EXE_SHA,output_guard,json_bytes,sha


def text_spans(raw):
    """Keep original formatting and unknown glyph bytes, with strict Big5 spans."""
    spans=[];offset=0;pending=bytearray();pending_start=0
    def flush():
        if pending:
            spans.append(dict(offset=pending_start,rawHex=pending.hex(),text=bytes(pending).decode('big5'),kind='text'))
            pending.clear()
    while offset<len(raw):
        byte=raw[offset]
        if byte==0x1b:
            flush();end=raw.find(b'x',offset+1)
            if end<0 or raw[offset+1:offset+2]!=b'[' or not raw[offset+2:end].isdigit():
                raise ValueError('Unexamined original formatting escape')
            spans.append(dict(offset=offset,rawHex=raw[offset:end+1].hex(),kind='format',text=None));offset=end+1;continue
        length=2 if 0xa1<=byte<=0xfc else 1;part=raw[offset:offset+length]
        try:part.decode('big5')
        except UnicodeDecodeError:
            flush();spans.append(dict(offset=offset,rawHex=part.hex(),kind='unknown_glyph',text=None));offset+=length;continue
        if byte<0x20 and byte not in (9,10,13):
            flush();spans.append(dict(offset=offset,rawHex=part.hex(),kind='unknown_control',text=None))
        else:
            if not pending:pending_start=offset
            pending.extend(part)
        offset+=length
    flush();return spans


class NativeMessageInterpreter(NativeFields):
    def __init__(self,exe,decoded,resource_number=2,scenario_raw=None):
        super().__init__(exe)
        if scenario_raw is not None:self.decode_units(scenario_raw)
        self.u.mem_map(0x7500000,0x200000)
        self.context=0x767cab8;self.blob=decoded;self.io_calls=[]
        self.call(0x49b490,receiver=self.context)
        self.reader=self.stream+0x6000;vtable=self.stream+0x7000;self.u.mem_map(self.reader,0x2000)
        self.methods=[self.stop+0x400+i*0x10 for i in range(5)]
        self.u.mem_write(self.reader,struct.pack('<I',vtable));self.u.mem_write(vtable,struct.pack('<5I',*self.methods))
        self.u.hook_add(UC_HOOK_CODE,self.memory_file,begin=self.methods[0],end=self.methods[-1])
        self.resource=self.context+0xa60
        if self.call(0x497e50,self.stream,resource_number,self.reader,receiver=self.resource)!=1:
            raise ValueError('Original decoded-resource load failed')
        self.call(0x497d60,self.resource,receiver=self.context)
        #497d90 first calls virtual resource discovery. The isolated fixture has
        #already loaded one explicitly chosen resource; execute the remainder
        #without pretending to have run discovery or established active priority.
        u=self.u;u.reg_write(UC_X86_REG_ESI,self.context);u.reg_write(UC_X86_REG_ESP,self.stack)
        u.mem_write(self.stack,struct.pack('<II',0,self.stop));u.emu_start(0x497da8,self.stop,count=300000)
        if u.reg_read(UC_X86_REG_EIP)!=self.stop:raise ValueError('Original bound-registry initialization did not return')
        self.data_pointer=struct.unpack('<I',u.mem_read(self.resource+0x14,4))[0]
        if bytes(u.mem_read(self.data_pointer,len(decoded)))!=decoded:raise ValueError('Original IO changed decoded bytes')
        u.mem_protect(self.data_pointer&~4095,((self.data_pointer+len(decoded)+4095)&~4095)-(self.data_pointer&~4095),UC_PROT_READ)

    def memory_file(self,u,address,size,user):
        if u.reg_read(UC_X86_REG_ECX)!=self.reader:raise ValueError('Unexamined memory-file receiver')
        sp=u.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',u.mem_read(sp,4))[0]
        index=self.methods.index(address);pop=0;value=1
        if index==1:pop=4
        elif index==3:value=len(self.blob)
        elif index==4:
            target=struct.unpack('<I',u.mem_read(sp+4,4))[0]
            if not 0x73fcab0<=target or target+len(self.blob)>0x75fcab0:raise ValueError('Original allocator escaped decoded pool')
            u.mem_write(target,self.blob);pop=4
        elif index!=2:raise ValueError('Unexamined memory-file method')
        self.io_calls.append(dict(method=index,returnAddress=hex(ret),value=value))
        u.reg_write(UC_X86_REG_EAX,value);u.reg_write(UC_X86_REG_ESP,sp+4+pop);u.reg_write(UC_X86_REG_EIP,ret)

    def render(self,message_id):
        self.call(0x497d20,self.buffer,4095,receiver=self.context)
        self.u.mem_write(self.buffer,bytes([0xa5])*4096)
        ptr=self.call(0x498a90,message_id,receiver=self.context)
        if ptr!=self.buffer:raise ValueError('Original interpreter changed output buffer')
        count=struct.unpack('<I',self.u.mem_read(self.context+0xa1c,4))[0]
        overflow=struct.unpack('<I',self.u.mem_read(self.context+0xa40,4))[0]
        if overflow or not 1<=count<=4094:raise ValueError('Original text interpreter truncated/escaped output')
        raw=bytes(self.u.mem_read(ptr,count))
        if raw[-1:]!=b'\0' or b'\0' in raw[:-1]:raise ValueError('Original output termination changed')
        if bytes(self.u.mem_read(ptr+4095,1))!=b'\xa5':raise ValueError('Output canary changed')
        if bytes(self.u.mem_read(self.data_pointer,len(self.blob)))!=self.blob:raise ValueError('Interpreter changed resource')
        return raw[:-1]


def audit(installation,output):
    output_guard(installation,output);exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Executable provenance changed')
    path=installation/'Media/msg/S11MSG02.s11';source=path.read_bytes();decoded,proof=decode(exe,source)
    count=struct.unpack_from('<H',decoded)[0];offsets=struct.unpack_from('<%dI'%count,decoded,2)
    if count!=2428 or not all(2+count*4<=v<len(decoded) for v in offsets) or tuple(sorted(offsets))!=offsets:
        raise ValueError('Unexamined original message table')
    native=NativeMessageInterpreter(exe,decoded);rng=bytes(native.u.mem_read(0x8a5d44,4));rows=[]
    for index in range(775):
        start=offsets[index];end=offsets[index+1];raw=native.render(10000+index);spans=text_spans(raw)
        rows.append(dict(messageId=10000+index,resourceIndex=index,sourceOffset=start,sourceBytes=end-start,
            sourceRawHex=decoded[start:end].hex(),renderedRawHex=raw.hex(),renderedSha256=sha(raw),spans=spans,
            unknownGlyphs=sorted({s['rawHex'] for s in spans if s['kind']=='unknown_glyph'}),
            unknownControls=sorted({s['rawHex'] for s in spans if s['kind']=='unknown_control'})))
    if rng!=bytes(native.u.mem_read(0x8a5d44,4)):raise ValueError('Message interpreter consumed gameplay RNG')
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,resourcePath='Media/msg/S11MSG02.s11',resourceSha256=sha(source),
        decodedSha256=sha(decoded),decoder=proof,interpreter='498a90 ->4976a0/4988e0',
        resourceLoad='497e50 memory-file IO; original allocator/constructor/497d60 registry;497da8 bound-registry initialization',
        ioCalls=native.io_calls,rngUnchanged=True,resourceReadOnly=True,outputCanary=True,messages=rows,
        limits=['Resource discovery/active priority has not been executed',
            'Message selectors must be joined by source identity, never project ID arithmetic',
            'Gaiji/format bytes retained; unknown glyphs have no invented Unicode value',
            'This report does not establish that every numbered message is a valid officer biography'])
    output.parent.mkdir(parents=True,exist_ok=True);payload=json_bytes(report);output.write_bytes(gzip.compress(payload,mtime=0))
    summary={k:v for k,v in report.items() if k!='messages'}
    summary.update(messages=len(rows),messagesWithUnknownGlyphs=sum(bool(r['unknownGlyphs']) for r in rows),
        messagesWithUnknownControls=sum(bool(r['unknownControls']) for r in rows),packedSha256=sha(output.read_bytes()),decodedReportSha256=sha(payload))
    output.with_suffix('.summary.json').write_bytes(json_bytes(summary));return report


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();audit(a.installation,a.output)
