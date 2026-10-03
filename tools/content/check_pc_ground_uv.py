#!/usr/bin/env python3
"""Execute the supplied terrain UV builder, including its original corner order.

No source writes, rule state or guessed UV reference. Optional actual D3D9
stream1 captures provide a separate first-quad check for opaque ground draws.
"""
import argparse,json,struct,math
from pathlib import Path
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP
from pc_resources import Archive,sha
from import_pc_map import textures
from inspect_pc_effect_bindings import EXE_SHA

def check(source,output,capture=None):
    exe=(source/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Reinspect source terrain UV executable')
    archive=Archive(source/'Media/san11pkres.bin')
    try:
        terrain=archive.read(4793)
        colors=[archive.read(i) for i in range(4787,4791)]
        raw_palettes=[archive.read(i) for i in range(4800,4804)]
        sizes=[[im.size for im in textures(raw)] for raw in raw_palettes]
    finally:archive.close()
    if terrain[:8]!=b'K3ST0006' or len(terrain)!=8+1025**2*8+1024**2*8:raise ValueError('Terrain bounds')
    u=Uc(UC_ARCH_X86,UC_MODE_32)
    u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000])
    heap=0x10000000;stack=0x20000000;stop=0x30000000
    u.mem_map(heap,0x1400000);u.mem_map(stack,0x10000);u.mem_map(stop,4096)
    u.mem_write(heap+8,terrain[8+1025**2*8:])
    linked=heap+0x1300000;indices=linked+0x100;vertices=linked+0x200
    out_indices=linked+0x300;out_vertices=linked+0x304
    def execute(q,r,step,width,height,quarter,mask=15):
        if width%32 or height%32:raise ValueError('Original dimensions not divisible by32')
        u.mem_write(linked,struct.pack('<IHHHBx',0,q,r,0,mask))
        for dq,dr in ((0,0),(step,0),(0,step),(step,step)):
            at=(q+dq)*1025+r+dr
            u.mem_write(heap+0x800008+at*10,b'\0'+colors[quarter][8+at*3:11+at*3]+b'\0'*6)
        u.mem_write(stack+0x8000,struct.pack('<9I',stop,out_indices,out_vertices,linked,step,indices,vertices,width//32,height//32))
        u.reg_write(UC_X86_REG_ECX,heap);u.reg_write(UC_X86_REG_ESP,stack+0x8000)
        u.emu_start(0x41dcb0,stop,count=5000)
        if u.reg_read(UC_X86_REG_EIP)!=stop or bytes(u.mem_read(out_indices,8))!=struct.pack('<2I',6,4):raise AssertionError('Original UV builder completion')
        return bytes(u.mem_read(vertices,48))
    rows=[]
    for quarter,palette in enumerate(sizes):
        for index,(width,height) in enumerate(palette):
            for q,r in ((0,0),(1,3),(114,114),(372,580),(1020,1020)):
                for step in (1,2,4):
                    actual=execute(q,r,step,width,height,quarter)
                    for corner,(dq,dr) in enumerate(((0,0),(step,0),(0,step),(step,step))):
                        _,a,b=struct.unpack_from('<I2f',actual,corner*12)
                        # Original resets UV at a face's start, then adds the
                        # unwrapped step. The sampler uses WRAP; compare modulo1.
                        expected=((q+dq)*32/width,(r+dr)*32/height)
                        if any(abs((v%1)-(e%1))>1e-7 for v,e in zip((a,b),expected)):
                            raise AssertionError(('Original terrain UV',quarter,index,q,r,step,corner,a,b,expected))
                    rows.append(dict(quarter=quarter,index=index,size=[width,height],q=q,r=r,step=step,source_stream1_hex=actual.hex()))
    live=[]
    if capture:
        raw=capture.read_bytes();magic,version,_,row_bytes=struct.unpack_from('<4I',raw)
        footer,count,_,reserved=struct.unpack_from('<4I',raw,len(raw)-16)
        if (magic,version,row_bytes,footer,reserved)!=(0x31445350,3,8540,0x444e4544,0) or len(raw)!=32+count*row_bytes:raise ValueError('Live stream1 capture bounds')
        for i in range(count):
            at=16+i*row_bytes;row=struct.unpack_from('<16I',raw,at)
            if row[2]!=4 or row[3:5]!=(0,0) or row[12]!=24 or row[14:16]!=(0,0):continue
            # V3 texture description tail: first stage record after223 state words.
            state=struct.unpack_from('<256I',raw,at+4888)
            pos=4+state[1]*3+(state[2]+state[3])*4
            if state[pos:pos+2]!=(0x32585444,2):raise ValueError('Live texture metadata')
            texture=state[pos+2:pos+14]
            if texture[4]!=22:continue  # opaque original palette, not outline/grid
            width,height=texture[10:12]
            stream=struct.unpack_from('<7I',raw,at+7976)
            if stream[3]!=12 or stream[4:6]!=(0,0):raise AssertionError('Live stream1 unavailable')
            rgba=[struct.unpack_from('<I',raw,at+8028+c*12)[0]>>24 for c in range(4)]
            if rgba!=[255]*4:continue  # prefix may be unused in this indexed draw
            xyz=[struct.unpack_from('<3f',raw,at+4376+c*24) for c in range(4)]
            q=round(xyz[0][0]/5);r=round(xyz[0][2]/5);step=round((xyz[1][0]-xyz[0][0])/5)
            if step<=0:raise AssertionError('Live original ground corner order')
            actual=execute(q,r,step,width,height,0)
            observed=raw[at+8028:at+8028+48]
            if actual!=observed:raise AssertionError(('Original live stream1 mismatch',i))
            live.append(dict(draw=i,q=q,r=r,step=step,size=[width,height],original_stream1_hex=actual.hex()))
        if not live:raise AssertionError('No actual opaque ground first quad observed')
    report=dict(status='PASS_ORIGINAL_TERRAIN_UV',goal_complete=False,exe_sha256=EXE_SHA,
        functions=[dict(start='41dcb0',end='41df1e',sha256=sha(exe[0x1dcb0:0x1df1e])),dict(start='41dc00',end='41dcb0',sha256=sha(exe[0x1dc00:0x1dcb0]))],
        source_terrain_sha256=sha(terrain),palette_resources=[dict(id=4800+i,sha256=sha(raw_palettes[i]),sizes=sizes[i])for i in range(4)],
        live_capture_sha256=sha(capture.read_bytes()) if capture else None,cases=len(rows),live_quads=live,
        contract='UV=(original fine-vertex coordinate*32/original texture dimension); WRAP; source image dimensions must be retained',
        limits=['Original CPU UV and optional actual first quad only','No Android material, final raster, fog or timing acceptance'],cases_detail=rows)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(dict(status=report['status'],cases=len(rows),live_quads=len(live))))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--capture',type=Path);a=p.parse_args();check(a.installation,a.output,a.capture)
