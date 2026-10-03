#!/usr/bin/env python3
"""Compare original quad VB machine-code writes with decoded source geometry.

Device/buffer calls record memory only. Does not run the game, controllers,
emission or source GPU, and does not establish an Android effect binding.
"""
import argparse
import json
from pathlib import Path
import struct
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32,UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EAX,UC_X86_REG_EIP,UC_X86_REG_ESI
from pc_resources import Archive,sha
from pc_effect_primitives import quad
from inspect_pc_effect_bindings import EXE_SHA

ROOT=Path(__file__).resolve().parents[2]


def check(installation,catalog,output,runtime_packets=None):
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Reinspect effect quad source EXE')
    if runtime_packets is None:
        rows=[r for r in json.loads(catalog.read_text())['records']if r['default_context']['primitive_kind']==2]
    else:
        report=json.loads(runtime_packets.read_text())
        if report['executable_sha256']!=EXE_SHA:raise ValueError('Evaluated packet executable mismatch')
        rows=[]
        for resource in report['resources']:
            for frame_index,frame in enumerate(resource['frames']):
                for packet_index,packet in enumerate(frame['packets']):
                    if packet['primitive']!=2:continue
                    payload=bytes.fromhex(packet['raw_hex'])
                    if len(payload)!=0x70 or sha(payload)!=packet['sha256']:raise ValueError('Evaluated source quad packet boundary/hash')
                    rows.append(dict(resource_id=resource['resource_id'],renderer_offset=None,
                        source_payload=payload,frame_index=frame_index,packet_index=packet_index))
    if not rows:raise ValueError('No source primitive2 records')
    u=Uc(UC_ARCH_X86,UC_MODE_32)
    u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000])
    u.mem_map(0x6ed0000,0x10000)
    heap=0x10000000;stack=0x20000000;stop=0x30000000
    u.mem_map(heap,0x10000);u.mem_map(stack,0x10000);u.mem_map(stop,4096)
    device=heap;buffer=heap+0x1000;device_table=heap+0x2000;buffer_table=heap+0x3000
    payload=heap+0x4000;vertices=heap+0x5000
    u.mem_write(device,struct.pack('<I',device_table));u.mem_write(buffer,struct.pack('<I',buffer_table))
    u.mem_write(0x6ed6f74,struct.pack('<I',device));u.mem_write(0x6ed37dc,struct.pack('<I',buffer))
    unlock=heap+0x6000;draw=heap+0x6020
    u.mem_write(buffer_table+0x30,struct.pack('<I',unlock));u.mem_write(device_table+0x144,struct.pack('<I',draw))
    matrix_uploads=[];draws=[];locks=[]
    def hook(uc,address,size,user):
        if address not in (0x44d560,0x44d160,unlock,draw):return
        sp=uc.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',uc.mem_read(sp,4))[0];pop=0
        if address==0x44d560:
            dev,index,data=struct.unpack('<3I',uc.mem_read(sp+4,12));assert dev==device and index==30
            matrix_uploads.append(bytes(uc.mem_read(data,64)))
        elif address==0x44d160:
            vb,offset,size,destination,flags=struct.unpack('<5I',uc.mem_read(sp+4,20));assert vb==buffer and size==96
            locks.append([offset,size,flags]);uc.mem_write(destination,struct.pack('<I',vertices))
        elif address==unlock:pop=4
        elif address==draw:
            dev,primitive,first,count=struct.unpack('<4I',uc.mem_read(sp+4,16));assert dev==device
            draws.append([primitive,first,count]);pop=16
        uc.reg_write(UC_X86_REG_EAX,0);uc.reg_write(UC_X86_REG_ESP,sp+4+pop);uc.reg_write(UC_X86_REG_EIP,ret)
    u.hook_add(UC_HOOK_CODE,hook)
    archive=Archive(installation/'Media/san11pkres.bin');samples=[];cache={}
    try:
        for row in rows:
            resource=row['resource_id']
            if runtime_packets is None:
                if resource not in cache:cache[resource]=archive.read(resource)
                raw=cache[resource]
                at=row['default_context']['offset']+0x10;source=raw[at:at+0x70]
            else:source=row['source_payload']
            if len(source)!=0x70:raise ValueError('Source quad payload bounds')
            width,height=struct.unpack_from('<2f',source,0x14);color=struct.unpack_from('<I',source,0x10)[0]
            rectangle=struct.unpack_from('<4f',source,0x20);matrix=source[0x30:0x70]
            for offset in (0,0xbfa1):
                u.mem_write(payload,source);u.mem_write(vertices,b'\xa5'*96);u.mem_write(0x8a5b64,struct.pack('<I',offset))
                u.mem_write(stack+0x8000,struct.pack('<I',stop));u.reg_write(UC_X86_REG_ESP,stack+0x8000)
                u.reg_write(UC_X86_REG_ESI,payload);u.reg_write(UC_X86_REG_EAX,payload+0x30)
                matrix_uploads.clear();draws.clear();locks.clear();u.emu_start(0x442230,stop,count=10000)
                if u.reg_read(UC_X86_REG_EIP)!=stop:raise AssertionError('Source quad instruction bound')
                actual=bytes(u.mem_read(vertices,96));expected=quad(width,height,color,rectangle)
                if actual!=expected:raise AssertionError(('Source quad VB',resource,row['renderer_offset'],actual.hex(),expected.hex()))
                if matrix_uploads!=[matrix] or draws!=[[5,0,2]]:raise AssertionError('Source matrix and triangle strip draw')
                if locks!=[[0,96,0x1000 if offset==0 else 0x2000]]:raise AssertionError('Source dynamic VB reset flags')
                samples.append(dict(resource_id=resource,renderer_offset=row['renderer_offset'],buffer_reset=offset!=0,
                    vb_sha256=sha(actual),matrix_sha256=sha(matrix),
                    **({}if runtime_packets is None else dict(frame_index=row['frame_index'],packet_index=row['packet_index']))))
    finally:archive.close()
    result=dict(schema=1,goal_complete=False,status='PASS',executable_sha256=EXE_SHA,
        function='442230',function_sha256=sha(exe[0x42230:0x423ab]),leaf_records=len(rows),samples=len(samples),
        comparison='Exact 96-byte source VB for primitive2, original BGRA and UV, matrix upload, triangle strip and dynamic buffer reset',
        limits=['Device/buffer/constant upload calls are memory-recording stubs','No controller/emission/scene transform/PC rasterization acceptance',
            'Primitive0/1/3/4/5/6/7 not verified by this tool','Original Android effects added=0'],records=samples)
    if runtime_packets is not None:
        result.update(evaluated_packet_reference=str(runtime_packets.resolve().relative_to(ROOT)),
            evaluated_packet_reference_sha256=sha(runtime_packets.read_bytes()),
            comparison='Exact original96-byte quad vertices/matrix/strip from native-emitted/time-updated draw packets; explicit diagnostic camera')
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:result[k]for k in ('status','leaf_records','samples')}))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path)
    p.add_argument('--catalog',type=Path,default=ROOT/'docs/pc-visual/effect-uv-working.json')
    p.add_argument('--output',type=Path,default=ROOT/'out/pc-visual/v141-effect-quad-machine-check.json')
    p.add_argument('--runtime-packets',type=Path,help='Native evaluated packet report; default remains original887 serialized records')
    a=p.parse_args();check(a.installation,a.catalog,a.output,a.runtime_packets)
