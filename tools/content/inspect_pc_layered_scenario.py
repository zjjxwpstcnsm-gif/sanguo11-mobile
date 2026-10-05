#!/usr/bin/env python3
"""Execute one native world constructor and layered Shared/scenario loader.

No Wine or PC writes. Original4937b0 reading is stopped before postload until
each required external context is established. Retains every actual IO request.
"""
import argparse
import gzip
import json
import struct
from pathlib import Path
from unicorn import UC_HOOK_CODE,UC_HOOK_MEM_INVALID
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP
from inspect_pc_scenario_domains import NativeDomainDecoder,GROUPS,scenario_header
from inspect_pc_scenario_tail import TABLES
from audit_pc_restoration_sources import EXE_SHA,output_guard,json_bytes,sha


class NativeLayeredWorld(NativeDomainDecoder):
    def __init__(self,exe):
        super().__init__(exe);self.exe=exe;self.loading=False;self.phase=None;self.invalid=[];self.stops={0x493bce,0x480284}
        self.meta=self.stream+0x1000;self.u.mem_map(self.meta,0x5000)
        self.u.mem_map(0x6fa0000,0x200000)
        self.u.hook_add(UC_HOOK_MEM_INVALID,self.invalid_memory)
        self.u.hook_add(UC_HOOK_CODE,self.boundary,begin=0x493bce,end=0x493bce)
        self.u.hook_add(UC_HOOK_CODE,self.boundary,begin=0x480284,end=0x480284)
        self.call(0x492db0,receiver=self.root,count=10000000)
        self.call(0x480830,receiver=self.meta)
        self.register('world_header',0,self.root,0x1d8,0x483120)
        self.register('scenario_metadata',0,self.meta,0x3f6a,0x480d50)
        for kind,offset,stride,count,ctor,serializer in GROUPS:
            for i in range(count):self.register(kind,i,self.root+offset+i*stride,stride,serializer)
        for offset,stride,count,ctor in TABLES:
            for i in range(count):self.register_vtable('table_%x'%offset,i,self.root+offset+i*stride,stride)
        for kind,offset,stride,count in [('officer',0xc0bc,0x190,1100),('grid',0x89730,0x38,16384),('table_1bd9ac',0x1bd9ac,0x36e28,1)]:
            for i in range(count):self.register_vtable(kind,i,self.root+offset+i*stride,stride)
        self.u.hook_add(UC_HOOK_CODE,self.enter_direct,begin=0x47abd0,end=0x47abd0)
        self.u.hook_add(UC_HOOK_CODE,self.enter_direct,begin=0x47ac20,end=0x47ac20)
        self.u.hook_add(UC_HOOK_CODE,self.enter_direct,begin=0x479680,end=0x479680)
        self.u.hook_add(UC_HOOK_CODE,self.enter_direct,begin=0x484960,end=0x484960)
        self.hooked={g[-1] for g in GROUPS}
        for row in self.addresses.values():
            function=row[-1]
            if function not in self.hooked:
                self.u.hook_add(UC_HOOK_CODE,self.enter,begin=function,end=function);self.hooked.add(function)

    def register(self,kind,index,actor,stride,function):self.addresses[actor]=(kind,index,stride,function)

    def register_vtable(self,kind,index,actor,stride):
        vtable=struct.unpack('<I',self.u.mem_read(actor,4))[0]
        function=struct.unpack('<I',self.u.mem_read(vtable+0x30,4))[0]
        if not 0x400000<=function<0x740000:raise ValueError('Original constructor has no examined serializer: '+kind)
        self.register(kind,index,actor,stride,function)

    def invalid_memory(self,u,access,address,size,value,user):
        self.invalid.append(dict(pc=hex(u.reg_read(UC_X86_REG_EIP)),access=access,address=hex(address),bytes=size));return False

    def boundary(self,u,address,size,user):
        if self.loading and address in self.stops:u.emu_stop()

    def call(self,function,*args,receiver=None,count=1000000,stop=None):
        u=self.u;u.reg_write(UC_X86_REG_ESP,self.stack)
        u.mem_write(self.stack,struct.pack('<%dI'%(len(args)+1),self.stop,*args))
        if receiver is not None:u.reg_write(UC_X86_REG_ECX,receiver)
        u.emu_start(function,self.stop if stop is None else stop,count=count)
        expected=self.stop if stop is None else stop
        if u.reg_read(UC_X86_REG_EIP)!=expected:raise ValueError('Native call boundary mismatch '+hex(function)+' at '+hex(u.reg_read(UC_X86_REG_EIP)))
        return u.reg_read(UC_X86_REG_EAX)

    def enter(self,u,address,size,user):
        if not self.loading:return
        super().enter(u,address,size,user)

    def enter_direct(self,u,address,size,user):
        if not self.loading:return
        actor=u.reg_read(UC_X86_REG_ECX)
        definitions={0x47abd0:('table_1a5050',self.root+0x1a5050,0x247,150),
                     0x47ac20:('table_1ba5ea',self.root+0x1ba5ea,0x109,50),
                     0x479680:('table_1f47d4',self.root+0x1f47d4,0x53a0,1),
                     0x484960:('global_6fb0e68',0x6fb0e68,0x80,1)}
        kind,base,stride,count=definitions[address];index=(actor-base)//stride
        if not 0<=index<count or actor!=base+index*stride:raise ValueError('Unexamined direct serializer receiver')
        self.register(kind,index,actor,stride,address);self.enter(u,address,size,user)

    def load(self,raw,shared=False,shared_postload=False):
        if shared_postload and not shared:raise ValueError('Regular scenario postload requires examined opening context')
        versions=scenario_header(raw,shared);self.raw=raw;self.cursor=90;self.records=[];self.invalid=[]
        self.u.mem_write(self.stream,bytes(4096));self.u.mem_write(self.stream+8,struct.pack('<I',1))
        self.u.mem_write(self.stream+0x54,struct.pack('<III',24 if shared else 22,*versions))
        self.loading=True
        if shared_postload:self.stops.discard(0x493bce)
        try:
            if not shared:self.call(0x480d50,self.stream,receiver=self.meta)
            self.call(0x4937b0,self.stream,receiver=self.root,count=40000000,
                      stop=self.stop if shared_postload else 0x493bce if shared else 0x480284)
        finally:self.loading=False;self.stops.add(0x493bce)
        self.finish_record()
        if self.cursor!=len(raw):raise ValueError('Original layered read did not consume complete file: '+str(self.cursor))
        return dict(bytes=len(raw),sourceSha256=sha(raw),readEnd=self.cursor,records=self.records,
                    worldSha256=sha(bytes(self.u.mem_read(0x7200000,0x300000))),
                    sharedPostloadExecuted=shared_postload,
                    stopBefore=None if shared_postload else '493bce postload dispatch' if shared else '480284 ->679cb0 event resource discovery',invalidMemory=self.invalid)


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--source',default='Media/scenario/Scen000.s11');p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();output_guard(a.installation,a.output);world=NativeLayeredWorld((a.installation/'san11pk.exe').read_bytes())
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,completeStartup=False,
        shared=world.load((a.installation/'Media/scenario/Scenario.s11').read_bytes(),True),
        scenario=world.load((a.installation/a.source).read_bytes()),sourcePath=a.source,
        limits=['Read boundary only; menu settings, complete postload and opening events still pending','No runtime content import; PC installation read only'])
    a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps({k:v for k,v in report.items() if k not in ('shared','scenario')}))
