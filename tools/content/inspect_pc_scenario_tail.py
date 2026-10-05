#!/usr/bin/env python3
"""Execute original scenario tail-table serializers; retain unproven semantics as raw records."""
import argparse
import json
import struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP, UC_X86_REG_EBX, UC_X86_REG_EBP
from inspect_pc_scenario_domains import NativeDomainDecoder, GROUPS, sha

# Original492db0 constructor arrays, in49391c..493b06 serializer order.
# Stable offset-based table names deliberately do not guess gameplay semantics.
TABLES=(
    (0x7984c,0x48,12,0x47a1e0),(0x79bac,0x1c,6,0x48df30),
    (0x79c54,0xd0,64,0x47ac70),(0x7d054,0xa0,12,0x47eca0),
    (0x7d7d4,0x34,10,0x494c60),(0x7d9dc,0x3c,81,0x48e0a0),
    (0x7ecd8,0x68,84,0x485050),(0x80ef8,0x6c,100,0x4944a0),
    (0x83928,0x6c,36,0x494a10),(0x84858,0x44,32,0x4946f0),
    (0x850d8,0x20,32,0x485310),(0x854d8,0x10,400,0x485600),
    (0x86dd8,0x6c,98,0x494e40),(0x169730,0xf4,1000,0x496d90),
)


class NativeTailDecoder(NativeDomainDecoder):
    def __init__(self,exe):
        super().__init__(exe)
        self.hooked={g[-1] for g in GROUPS}
        self.reading_prefix=False
        # Reused Unicorn translation blocks may cross a former emu_start end.
        # Explicit code-boundary stops prevent an uninitialized next table from
        # executing during a subsequent prefix read. No source rule is replaced.
        self.u.hook_add(UC_HOOK_CODE,self.boundary,begin=0x49391c,end=0x49391c)
        self.u.hook_add(UC_HOOK_CODE,self.boundary,begin=0x493b06,end=0x493b06)

    def boundary(self,u,address,size,user):
        if address==(0x49391c if self.reading_prefix else 0x493b06):
            u.emu_stop()

    def decode_tail(self,raw,shared=False):
        self.reading_prefix=True
        try:prefix=super().decode(raw,shared)
        finally:self.reading_prefix=False
        u=self.u
        for offset,stride,count,ctor in TABLES:
            for index in range(count):
                pointer=self.root+offset+stride*index
                u.reg_write(UC_X86_REG_ECX,pointer);u.reg_write(UC_X86_REG_ESP,self.stack)
                u.mem_write(self.stack,struct.pack('<I',self.stop))
                u.emu_start(ctor,self.stop,count=100000)
                if u.reg_read(UC_X86_REG_EIP)!=self.stop:
                    raise ValueError('Tail constructor failed '+hex(ctor))
                vtable=struct.unpack('<I',u.mem_read(pointer,4))[0]
                serializer=struct.unpack('<I',u.mem_read(vtable+0x30,4))[0]
                if not 0x400000<=serializer<0x740000:
                    raise ValueError('Unexpected tail serializer '+hex(serializer))
                self.addresses[pointer]=(f'table_{offset:x}',index,stride,serializer)
                if serializer not in self.hooked:
                    u.hook_add(UC_HOOK_CODE,self.enter,begin=serializer,end=serializer);self.hooked.add(serializer)
        start=self.cursor;self.records=[]
        u.reg_write(UC_X86_REG_EBX,self.root);u.reg_write(UC_X86_REG_EBP,self.stream);u.reg_write(UC_X86_REG_ESP,self.stack)
        u.emu_start(0x49391c,0x493b06,count=20000000)
        if u.reg_read(UC_X86_REG_EIP)!=0x493b06:
            raise ValueError('Original tail loop did not finish')
        self.finish_record()
        expected=[(f'table_{offset:x}',i) for offset,stride,count,ctor in TABLES for i in range(count)]
        if [(r['kind'],r['native_index']) for r in self.records]!=expected:
            raise ValueError('Tail record count/order changed')
        for row in self.records:
            row['actor_hex']=bytes(u.mem_read(row['actor_address'],row['actor_stride'])).hex()
        return dict(start=start,end=self.cursor,versions=prefix['versions'],records=self.records)


def audit(installation,output):
    installation=installation.resolve();output=output.resolve()
    if output==installation or installation in output.parents:
        raise ValueError('Output must not be inside read-only PC installation')
    exe=(installation/'san11pk.exe').read_bytes();decoder=NativeTailDecoder(exe)
    paths=[installation/'Media/scenario/Scenario.s11']+sorted((p for p in (installation/'Media/scenario').iterdir() if p.name.lower().startswith('scen0') and p.suffix.lower()=='.s11'),key=lambda p:p.name.lower())
    if len(paths)!=17:raise ValueError('Expected16 candidates and shared file')
    sources=[]
    for path in paths:
        raw=path.read_bytes();source=dict(path=path.relative_to(installation).as_posix(),bytes=len(raw),sha256=sha(raw),**decoder.decode_tail(raw,path.name=='Scenario.s11'))
        sources.append(source);print(json.dumps(dict(path=source['path'],start=source['start'],end=source['end'],records=len(source['records']))),flush=True)
    report=dict(schema=1,source_executable_sha256=sha(exe),sources=sources,evidence=dict(constructors='492db0..49309d',loop=['49391c','493b06'],io='original46ff20 source bytes only'),limits=['Offset-based table names are not semantic mappings','Post493b06 fixups and trailing objects have not executed','No runtime content imported; no Wine; PC directory read only'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    return report


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();audit(a.installation,a.output)
