#!/usr/bin/env python3
"""Compare actual Java source->scene vertices to original D3DX machine code."""
import argparse
import gzip
import json
import math
from pathlib import Path
import struct
import subprocess
from pc_effect_machine import SourceEffectMachine
from pc_resources import sha

ROOT=Path(__file__).resolve().parents[2]


def check(installation,output):
    output.mkdir(parents=True,exist_ok=True)
    fixture=gzip.decompress((ROOT/'app/src/androidTest/assets/pc-effects/worker-reference.bin.gz').read_bytes())
    (output/'packets.bin').write_bytes(fixture)
    machine=SourceEffectMachine((installation/'san11pk.exe').read_bytes())
    expected=bytearray();pos=16;vertices=0;camera_fixture=bytearray()
    try:
        for address in (0x73bc80,0x73bd20):machine.call(address,0)
        matrix,vertex,dest=machine.HEAP,machine.HEAP+256,machine.HEAP+512
        while pos<len(fixture):
            count=struct.unpack_from('<I',fixture,pos+12)[0];pos+=32
            for i in range(count):
                packet=pos+i*184;machine.u.mem_write(matrix,fixture[packet+120:packet+184])
                for v in range(4):
                    xyz=struct.unpack_from('<3f',fixture,packet+24+v*24)
                    machine.u.mem_write(vertex,struct.pack('<4f',*xyz,1))
                    machine.call(0x6ad2cb,0,dest,vertex,matrix)
                    world=struct.unpack('<4f',machine.u.mem_read(dest,16))
                    if world[3]!=1:raise AssertionError('Source affine world matrix')
                    expected.extend(struct.pack('<3f',world[0]*.05-28.5-63,world[1]*.05,world[2]*.05-28.5-44.1));vertices+=1
            pos+=count*184
        if pos!=len(fixture):raise AssertionError('Whole source fixture')
        # Execute source441ab0 and441b80 from real eye/target/up/frustum
        # inputs. This independently establishes RH,+c0=view*projection and
        # +180=projection, which the earlier diagnostic packets cannot prove.
        c,up=machine.HEAP+0x2000,machine.HEAP+0x3000
        for origin in (0,63,-42):
            for tilt in (40,55,70):
                for yaw in (0,35,90,180,265,359):
                    for facing in (-1,1):
                        right=math.cos(math.radians(yaw))*facing;back=math.sin(math.radians(yaw))*facing
                        s=math.sin(math.radians(tilt));cos=math.cos(math.radians(tilt));fx,fz=43,51
                        for span,distance,perspective in [(17,300,False)]+[(span,16+(6.375+span*cos)/s,False) for span in (3,8,17,35,160)]+[(span,max(3*span,16+6.375/s),True) for span in (3,8,17,35,160)]:
                            focus=((fx+28.5+origin)/.05,0,(fz+28.5+origin*.7)/.05)
                            eye=(focus[0]+back*distance/.05*cos,distance/.05*s,focus[2]+right*distance/.05*cos)
                            gl_view=(right,-back*s,back*cos,0,0,cos,s,0,-back,-right*s,right*cos,0,
                                -right*fx+back*fz,(back*fx+right*fz)*s,-(back*fx+right*fz)*cos-distance,1)
                            aspect,near,far=1080/1232,.1,1000
                            gl_projection=(1/(span*aspect),0,0,0,0,1/span,0,0,0,0,-2/(far-near),0,0,0,-(far+near)/(far-near),1)
                            if perspective:
                                gl_projection=(distance/(span*aspect),0,0,0,0,distance/span,0,0,0,0,-(far+near)/(far-near),-1,0,0,-2*far*near/(far-near),0)
                            normalization=1/far if perspective else 1
                            machine.u.mem_write(c,bytes(0x200))
                            machine.u.mem_write(c,struct.pack('<8f',*eye,1,*focus,1))
                            machine.u.mem_write(c+0x20,struct.pack('<5fI2f',0,2*math.atan(span/distance) if perspective else 0,near/.05,far/.05,aspect,0 if perspective else 1,2*span*aspect/.05,2*span/.05))
                            machine.u.mem_write(up,struct.pack('<4f',0,1,0,0))
                            machine.call(0x441ab0,0,c,up);machine.call(0x441b80,0,c)
                            camera_fixture.extend(struct.pack('<3d16f16d',origin,origin*.7,normalization,*gl_view,*gl_projection))
                            camera_fixture.extend(bytes(machine.u.mem_read(c,12))+bytes(machine.u.mem_read(c+0x40,64))
                                +bytes(machine.u.mem_read(c+0xc0,64))+bytes(machine.u.mem_read(c+0x180,64)))
    finally:machine.close()
    (output/'source-vertices.bin').write_bytes(expected)
    (output/'source-camera.bin').write_bytes(camera_fixture)
    sources=[ROOT/'app/src/main/java/game/sanguo/mobile/PcEffectCoordinates.java',ROOT/'app/src/test/java/game/sanguo/mobile/PcEffectCoordinatesTest.java']
    subprocess.run(['javac','--release','17','-d',str(output),*[str(s)for s in sources]],check=True)
    test=subprocess.run(['java','-cp',str(output),'game.sanguo.mobile.PcEffectCoordinatesTest',str(output/'packets.bin'),str(output/'source-vertices.bin'),str(output/'source-camera.bin')],capture_output=True,text=True)
    (output/'java-check.txt').write_text(test.stdout+test.stderr)
    if test.returncode:raise RuntimeError(test.stdout+test.stderr)
    report=dict(schema=1,goal_complete=False,status='HOST_COORDINATE_BOUNDARY_PASS',vertices=vertices,
        source_function='6ad2cb selected by original CRT73bc80/73bd20',source_packets_sha256=sha(fixture),
        source_vertices_sha256=sha(bytes(expected)),java_output=test.stdout.strip(),
        source_camera_cases=1188,source_camera_sha256=sha(bytes(camera_fixture)),camera_functions=['441ab0','441b80','6ad551'],
        limits=['Actual Filament installed camera/GPU buffers still require validation','No PC lens/visual/shader/MOD acceptance'])
    (output/'comparison.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,default=ROOT/'out/pc-visual/v150-effect-coordinate-checked');a=p.parse_args();check(a.installation.resolve(),a.output.resolve())
