#!/usr/bin/env python3
"""Run original literal-matrix expression copy against source decoder bytes.

Does not evaluate opcode6 composites, scene transforms, emissions or the GPU.
"""
import argparse
import json
from pathlib import Path
import struct
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EAX,UC_X86_REG_EIP
from pc_resources import Archive,sha
from pc_effect_curves import matrix_literal_program
from inspect_pc_effect_bindings import EXE_SHA

ROOT=Path(__file__).resolve().parents[2]


def check(installation,catalog,output):
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Reinspect matrix expression EXE')
    rows=[r for r in json.loads(catalog.read_text())['curves']if r.get('status')=='matrix-literal-bounds-and-relative-pointer-only']
    if len(rows)!=360:raise ValueError('Reinspect literal matrix coverage')
    u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000])
    heap=0x10000000;stack=0x20000000;stop=0x30000000
    u.mem_map(heap,0x10000);u.mem_map(stack,0x10000);u.mem_map(stop,4096)
    record=heap;cursor=heap+0x1000;context=heap+0x2000;destination=heap+0x3000
    archive=Archive(installation/'Media/san11pkres.bin');cache={};samples=[]
    try:
        for row in rows:
            resource=row['resource_id']
            if resource not in cache:cache[resource]=archive.read(resource)
            data=cache[resource];at=row['offset'];raw=data[at:at+row['bytes']]
            if sha(raw)!=row['sha256']:raise ValueError('Source literal expression changed')
            decoded=matrix_literal_program(data,at,at+len(raw))
            expected=struct.pack('<16f',*decoded['matrix'])
            for context_fill in (0,0x5a):
                u.mem_write(record,raw);u.mem_write(cursor,struct.pack('<I',record+16))
                u.mem_write(context,bytes([context_fill])*512);u.mem_write(destination,b'\xa5'*64)
                u.mem_write(stack+0x8000,struct.pack('<4I',stop,cursor,context,destination))
                u.reg_write(UC_X86_REG_ESP,stack+0x8000);u.emu_start(0x4689d0,stop,count=1000)
                if u.reg_read(UC_X86_REG_EIP)!=stop or u.reg_read(UC_X86_REG_EAX)!=destination:
                    raise AssertionError('Source matrix expression return')
                actual=bytes(u.mem_read(destination,64))
                if actual!=expected:raise AssertionError(('Source literal matrix bytes',resource,at))
                if struct.unpack('<I',u.mem_read(cursor,4))[0]!=record+24:
                    raise AssertionError('Source matrix prefix cursor')
                samples.append(dict(resource_id=resource,offset=at,context_fill=context_fill,matrix_sha256=sha(actual)))
    finally:archive.close()
    result=dict(schema=1,goal_complete=False,status='PASS',executable_sha256=EXE_SHA,
        functions=['4689d0','4672d0'],records=len(rows),samples=len(samples),
        comparison='Exact source 64-byte literal matrix and prefix cursor; two unrelated context fills',
        limits=['Matrix opcode6 and scene transform/time assignment remain unexamined','No controller/emission/GPU or PC video acceptance','Original Android effects added=0'],details=samples)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:result[k]for k in ('status','records','samples')}))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path)
    p.add_argument('--catalog',type=Path,default=ROOT/'docs/pc-visual/effect-curves-working.json')
    p.add_argument('--output',type=Path,default=ROOT/'out/pc-visual/v141-effect-matrix-machine-check.json')
    a=p.parse_args();check(a.installation,a.catalog,a.output)
