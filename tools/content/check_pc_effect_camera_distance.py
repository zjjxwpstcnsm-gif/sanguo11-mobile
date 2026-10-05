#!/usr/bin/env python3
"""Run source SEFF/KSEF against source-built RH cameras at several distances.

Investigates original visibility conditions; no guessed change to source code,
no PC screenshot or Android pixel acceptance. Supplied installation is readonly.
"""
import argparse
import json
import math
from pathlib import Path
import struct
from pc_resources import Archive, effects, sha
from pc_effect_machine import SourceEffectMachine, VerifiedSourceExecutable
from inspect_pc_effect_draw_packets import DrawPacketObserver

ROOT=Path(__file__).resolve().parents[2]


def check(installation,output):
    exe=VerifiedSourceExecutable((installation/'san11pk.exe').read_bytes());a=Archive(installation/'Media/san11pkres.bin');results=[]
    try:
        rows=effects(a.read(4792))
        for slot in (0,6,40):
            row=rows[slot];resource=struct.unpack_from('<I',exe.data,0x37692c+row['effect']*12+4)[0];raw=a.read(resource)
            for distance in (100,300,600,1000,2000,6000):
                m=SourceEffectMachine(exe);observer=None
                try:
                    for function in (0x73bc80,0x73bd20):m.call(function,0)
                    observer=DrawPacketObserver(m,.001,center=(row['x'],0,row['z']))
                    c=observer.camera;up=observer.base+0xe000
                    tilt,yaw=math.radians(55),math.radians(35);back,right=math.sin(yaw),math.cos(yaw)
                    focus=(row['x'],0,row['z']);eye=(focus[0]+back*distance*math.cos(tilt),distance*math.sin(tilt),focus[2]+right*distance*math.cos(tilt))
                    m.u.mem_write(c,struct.pack('<8f',*eye,1,*focus,1))
                    m.u.mem_write(c+0x20,struct.pack('<5fI2f',0,0,2,20000,1080/1232,1,320*1080/1232,320))
                    m.u.mem_write(up,struct.pack('<4f',0,1,0,0));m.call(0x441ab0,0,c,up);m.call(0x441b80,0,c)
                    m.u.mem_write(c+0x140,bytes(m.u.mem_read(c+0xc0,64)))
                    # Preserve original camera before the load-time provider is cached.
                    m.load(raw,scene_camera_provider=observer.provider)
                    m.u.mem_write(m.STOP+0x500,struct.pack('<4f',row['x'],row['y'],row['z'],row['yaw']))
                    m.call(0x413a80,0,m.STOP+0x600,m.STOP+0x500);m.start(bytes(m.u.mem_read(m.STOP+0x600,64)))
                    samples=[]
                    for dt in (.0333333333,.5,1,1,1):
                        m.update(dt);packets=observer.draw()
                        samples.append(dict(dt=dt,attempts=m.attempts,allocations=m.successes,packets=len(packets),
                            depths=[struct.unpack('<f',struct.pack('<I',int(p['depth_bits'],16)))[0] for p in packets]))
                    r=dict(seff_slot=slot,effect_index=row['effect'],resource=resource,source_distance=distance,samples=samples)
                    results.append(r);print(json.dumps(r),flush=True)
                finally:
                    if observer is not None:observer.close()
                    m.close()
    finally:a.close()
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(dict(schema=1,goal_complete=False,status='ORIGINAL_SOURCE_CAMERA_DISTANCE_INVESTIGATION',source_executable_sha256=sha(exe.data),source_functions=['441ab0','441b80','457dd0','457880','457a20','457b00'],cases=results,limits=['No gameplay/PC camera calibration or image acceptance']),indent=2)+'\n')


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,default=ROOT/'out/pc-visual/v150-camera-distance-source.json');args=p.parse_args();check(args.installation.resolve(),args.output.resolve())
