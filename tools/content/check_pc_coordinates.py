#!/usr/bin/env python3
"""Execute supplied EXE height/OBJS translation code; never write PC files.

Establishes source units before a joint scene-scale conversion. It does not
validate cameras, rasterization, contact offsets or change Android geometry.
"""
import argparse
import json
from pathlib import Path
import struct
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP
from pc_resources import Archive,objects,sha
from inspect_pc_effect_bindings import EXE_SHA

ROOT=Path(__file__).resolve().parents[2]


def check(installation,output):
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Reinspect coordinate EXE')
    constant=lambda va:struct.unpack_from('<f',exe,va-0x400000)[0]
    if constant(0x779bc4)!=5. or constant(0x74e9c8)!=.5:
        raise ValueError('Original coordinate constants changed')
    archive=Archive(installation/'Media/san11pkres.bin')
    try:raw_objects=archive.read(4805);terrain=archive.read(4793)
    finally:archive.close()
    if terrain[:8]!=b'K3ST0006' or len(terrain)!=8+1025**2*8+1024**2*8:
        raise ValueError('Terrain source bounds')
    u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000])
    heap=0x10000000;stack=0x20000000;stop=0x30000000
    u.mem_map(heap,0x1400000);u.mem_map(stack,0x10000);u.mem_map(stop,4096)
    # Original 415920 returns x87 ST0. Store that value as an IEEE float32 in
    # the caller, exactly as the terrain builder does; no arithmetic shim.
    caller=heap+0x1000;destination=heap+0x2000
    u.mem_write(caller,b'\xd9\x1d'+struct.pack('<I',destination)+b'\xe9'+struct.pack('<i',stop-(caller+11)))
    height_samples=[]
    for value in range(256):
        u.mem_write(heap+0x800000,bytes([value]));u.mem_write(stack+0x8000,struct.pack('<3I',caller,0,0))
        u.reg_write(UC_X86_REG_ECX,heap);u.reg_write(UC_X86_REG_ESP,stack+0x8000)
        u.emu_start(0x415920,stop,count=1000)
        actual=bytes(u.mem_read(destination,4));expected=struct.pack('<f',value*.5)
        if actual!=expected or u.reg_read(UC_X86_REG_EIP)!=stop:raise AssertionError('Original terrain height arithmetic')
        height_samples.append(dict(height_byte=value,PC_height=struct.unpack('<f',actual)[0]))
    placements=[]
    for row in objects(raw_objects):
        u.mem_write(heap+0x6e,struct.pack('<HH',row['x'],row['z']));u.mem_write(heap+0x73,bytes([row['height']]))
        # 41c356 is a function tail: one scratch word remains above the return.
        u.mem_write(stack+0x8000,struct.pack('<2I',0,stop));u.reg_write(UC_X86_REG_ESP,stack+0x8000);u.reg_write(UC_X86_REG_ECX,heap)
        u.emu_start(0x41c356,stop,count=1000)
        actual=bytes(u.mem_read(heap+0x40,12));expected=struct.pack('<3f',row['x']*10.,row['height']*.5,row['z']*10.)
        if actual!=expected or u.reg_read(UC_X86_REG_EIP)!=stop:raise AssertionError(('Original OBJS translation',row['slot']))
        placements.append(dict(slot=row['slot'],kind=row['model'],PC_translation=list(struct.unpack('<3f',actual))))
    max_height=max(terrain[8:8+1025**2*8:8])
    result=dict(schema=1,status='PASS',goal_complete=False,executable_sha256=EXE_SHA,
        source_resources=[dict(id=4805,sha256=sha(raw_objects)),dict(id=4793,sha256=sha(terrain))],
        functions=[dict(start='415920',sha256=sha(exe[0x15920:0x1594c])),dict(start='41c356',sha256=sha(exe[0x1c356:0x1c395]))],
        height_samples=len(height_samples),object_samples=len(placements),
        source_contract=dict(terrain_height_per_byte=.5,object_half_grid_step=10.,
            proposed_uniform_scene_scale=.05,proposed_height_per_byte=.025,
            source_terrain_max_byte=max_height,proposed_scene_max_height=max_height*.025),
        limits=['Arithmetic and object translations only; source PC camera/image comparison unavailable',
            'Current working-tree candidate uses uniform .05XYZ/.025height; installed and PC image acceptance tracked separately',
            'No rule/save changes or original Android effects added'],height_details=height_samples,objects=placements)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:result[k]for k in ('status','height_samples','object_samples','source_contract')}))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path)
    p.add_argument('--output',type=Path,default=ROOT/'out/pc-visual/v145-coordinate-machine-check.json')
    a=p.parse_args();check(a.installation,a.output)
