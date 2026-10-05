#!/usr/bin/env python3
"""Run supplied EXE SENV applicator in isolated x86 memory.

Interface accessors are stubs returning camera/light memory. Imported D3DX
normalization uses a documented math shim; fog/ambient stores execute original
machine code. This is not a PC renderer/reference-video verification.
"""
import argparse
import json
import math
from pathlib import Path
import struct
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EAX, UC_X86_REG_EIP, UC_X86_REG_FPCW
from pc_resources import Archive, sha
from import_pc_environment import ROOT, environment
from inspect_pc_effect_bindings import EXE_SHA


def f32(x):
    return struct.unpack('<f', struct.pack('<f',x))[0]


def check(installation,output,source24=False,live_cameras=()):
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Reinspect SENV executable')
    archive=Archive(installation/'Media/san11pkres.bin')
    try:raw=archive.read(4799)
    finally:archive.close()
    rows=environment(raw)
    machine=Uc(UC_ARCH_X86,UC_MODE_32)
    machine.mem_map(0x400000,0x500000);machine.mem_write(0x400000,exe[:0x500000])
    record=0x10000000;camera=record+0x1000;light=record+0x2000;table=record+0x3000
    machine.mem_map(record,0x5000)
    global_renderer=0x32602b0;machine.mem_map(global_renderer&~4095,4096)
    machine.mem_write(global_renderer,struct.pack('<I',table))
    camera_accessor=table+0x100;light_accessor=table+0x120
    machine.mem_write(table,struct.pack('<2I',camera_accessor,light_accessor))
    machine.mem_write(camera_accessor,b'\xb8'+struct.pack('<I',camera)+b'\xc3')
    machine.mem_write(light_accessor,b'\xb8'+struct.pack('<I',light)+b'\xc3')
    stack=0x20000000;stop=0x30000000
    machine.mem_map(stack,0x10000);machine.mem_map(stop,0x1000)
    def normalization(uc,address,size,user):
        sp=uc.reg_read(UC_X86_REG_ESP)
        ret,dest,source=struct.unpack('<3I',uc.mem_read(sp,12))
        v=struct.unpack('<3f',uc.mem_read(source,12));length=math.sqrt(sum(x*x for x in v))
        uc.mem_write(dest,struct.pack('<3f',*[x/length for x in v]))
        uc.reg_write(UC_X86_REG_EAX,dest);uc.reg_write(UC_X86_REG_ESP,sp+12);uc.reg_write(UC_X86_REG_EIP,ret)
    machine.hook_add(UC_HOOK_CODE,normalization,begin=0x6acaad,end=0x6acaad)
    live=[]
    for specification in live_cameras:
        season,path=specification.split(':',1);data=Path(path).read_bytes()
        if len(data)!=512 or int(season)not in range(4):raise ValueError('Live camera512/season')
        near,far=struct.unpack_from('<2f',data,0x28)
        live.append(dict(season=int(season),path=path,near=near,far=far,fields=data[0x1d0:0x1de]))
    samples=[]
    for row in rows:
        cases=[(1.0,2048.0,None),(3.125,731.75,None),(0.0,1.0,None)]
        cases += [(item['near'],item['far'],item)for item in live if item['season']==row['season'] and row['variant']==0]
        for near,far,actual_live in cases:
            machine.mem_write(record,raw[row['offset']:row['offset']+40])
            machine.mem_write(camera,b'\x00'*512);machine.mem_write(light,b'\x00'*128)
            machine.mem_write(camera+0x28,struct.pack('<2f',near,far))
            machine.mem_write(stack+0x8000,struct.pack('<2I',stop,record))
            machine.reg_write(UC_X86_REG_ESP,stack+0x8000);machine.reg_write(UC_X86_REG_ECX,0)
            if source24:machine.reg_write(UC_X86_REG_FPCW,0x007f)
            machine.emu_start(0x5a2530,stop,count=10000)
            if machine.reg_read(UC_X86_REG_EIP)!=stop:raise AssertionError('SENV applicator instruction bound')
            delta=f32(far-near)
            def distance(percent):
                if source24:return f32(near+f32(f32(percent*delta)*f32(.01)))
                return f32(near+percent*delta*f32(.01))
            start=distance(row['fog_start_percent']);end=distance(row['fog_end_percent'])
            expected=struct.pack('<2f',start,end)
            actual=bytes(machine.mem_read(camera+0x1d0,8))
            if actual!=expected:raise AssertionError(('fog distance',row['season'],row['variant'],near,far,actual.hex(),expected.hex()))
            density_product=row['fog_density_percent']*f32(2.55)
            if source24:density_product=f32(density_product)
            density=min(255,int(density_product))
            color=bytes.fromhex(row['fog_bgra'][2:].zfill(8)) # A R G B
            expected_bytes=bytes((density,255,color[1],color[2],color[3],row['renderer_byte_1dd']))
            actual_bytes=bytes(machine.mem_read(camera+0x1d8,6))
            if actual_bytes!=expected_bytes:raise AssertionError(('fog color/density',row,actual_bytes.hex(),expected_bytes.hex()))
            if actual_live is not None and actual+actual_bytes!=actual_live['fields']:
                raise AssertionError(('Actual PC fog fields',actual_live['path'],(actual+actual_bytes).hex(),actual_live['fields'].hex()))
            ambient=struct.pack('<4f',*row['ambient'],1.0)
            if bytes(machine.mem_read(light+0x60,16))!=ambient:raise AssertionError('source ambient placement +60')
            source_dir=row['direction'];length=math.sqrt(sum(x*x for x in source_dir))
            for i in range(3):
                expected_dir=struct.pack('<f',-f32(source_dir[i]/length))
                if bytes(machine.mem_read(light+i*16,4))!=expected_dir:raise AssertionError('source direction normalization/negation')
            samples.append(dict(season=row['season'],variant=row['variant'],near=near,far=far,
                source_fog_fields_hex=(actual+actual_bytes).hex(),ambient_sha256=sha(ambient),
                actual_live_camera=actual_live['path']if actual_live else None))
    result=dict(schema=1,goal_complete=False,status='PASS',states=24,samples=len(samples),
        executable_sha256=EXE_SHA,source_resource=4799,source_sha256=sha(raw),
        functions=['5a2530','442e50','442ef0','4419f0','707a74'],
        comparison='Exact float32 fog start/end, byte density/color, ambient storage; normalized source direction',
        x87_control='0x007f actual-source24'if source24 else 'Unicorn default precision; historical host check',live_camera_checks=len(live),
        limits=['D3DX normalize imported function is math shim, not independent machine-code oracle','Optional actual PC camera field match is not a raster comparison','No source fog/ambient Android integration acceptance'],records=samples)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({key:result[key]for key in ('status','states','samples')}))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('installation',type=Path)
    parser.add_argument('--output',type=Path,default=ROOT/'out/pc-visual/v141-environment-machine-check.json')
    parser.add_argument('--source24',action='store_true',help='Use observed original rendering-thread x87 control0x007f')
    parser.add_argument('--live-camera',action='append',default=[],help='Source SENV season0..3:actual512bytecamera path; base variant0')
    args=parser.parse_args();check(args.installation,args.output,args.source24,args.live_camera)
