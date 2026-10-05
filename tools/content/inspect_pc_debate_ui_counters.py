#!/usr/bin/env python3
"""Execute original counter queue and its bounded hand-consumption block.

Explicit synthetic UI storage and original empty message context. No graphics,
normal PC start, full queue scheduling or campaign outcome is certified here.
"""
import argparse, gzip, itertools, json, struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EDI, UC_X86_REG_ESI
from inspect_pc_debate_flow import NativeDebateFlow
from audit_pc_restoration_sources import EXE_SHA, output_guard, json_bytes, sha

def inspect(installation, output):
    installation=installation.resolve();output_guard(installation,output)
    exe=(installation/'san11pk.exe').read_bytes();d=NativeDebateFlow(installation,exe);w=d.world
    w.u.mem_map(0x7500000,0x200000);w.call(0x49b490,receiver=0x767cab8)
    ui=0xc200000;w.u.mem_map(ui,0x30000);cases=[]
    for temper, counter, side, seed in itertools.product(range(4),(-1,13,14),range(2),(0,1,23,0xffffffff)):
        w.u.mem_write(ui,bytes(0x30000));w.u.mem_write(ui+0x10,struct.pack('<I',d.fixture))
        w.u.mem_write(ui+0x1fcc8,struct.pack('<2i',-1,-1))
        for i in range(100):w.u.mem_write(ui+0x1febc+i*0x108,struct.pack('<i',-1))
        w.u.mem_write(d.fixture,bytes(0x1b0))
        for actor_side in range(2):
            actor=d.people[actor_side];w.call(0x489f10,receiver=actor)
            base=d.fixture+0x10+actor_side*0xa0
            w.u.mem_write(base,struct.pack('<I',actor));w.u.mem_write(base+0x9c,struct.pack('<i',temper))
            w.u.mem_write(base+0x30,struct.pack('<i',7))
            w.u.mem_write(base+0x14,struct.pack('<7i',0,1,2,3,10,13,14))
        initial=bytes(w.u.mem_read(d.fixture,0x1b0));w.u.mem_write(0x8a5d44,struct.pack('<I',seed))
        w.call(0x519fd0,side,counter & 0xffffffff,receiver=ui,count=10000000)
        generated=bytes(w.u.mem_read(d.fixture,0x1b0))
        if initial!=generated:raise ValueError('Counter queue generation changed model before queue execution')
        rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
        count=struct.unpack('<i',w.u.mem_read(ui+0x265dc,4))[0]
        queue=[];consumed=[]
        for i in range(count):
            address=ui+0x1febc+i*0x108;raw=bytes(w.u.mem_read(address,0x108));kind=struct.unpack_from('<i',raw)[0]
            queue.append(dict(index=i,type=kind,rawHex=raw.hex()))
            if kind!=8:continue
            # Original switch case8 at51cedb fetches the actual speaker and
            # removes its indexed card. Stop51cf3c before 3D/audio call4d0570.
            # Neither model function nor card result is hooked or substituted.
            w.u.reg_write(UC_X86_REG_EDI,ui);w.u.reg_write(UC_X86_REG_ESI,address)
            w.call(0x51cedb,stop=0x51cf3c)
            consumed.append(dict(index=i,side=struct.unpack_from('<i',raw,4)[0],slot=struct.unpack_from('<i',raw,0xf4)[0]))
        after=bytes(w.u.mem_read(d.fixture,0x1b0));final_rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
        if rng!=final_rng:raise ValueError('Bounded original hand-consumption block changed RNG')
        expected_draws=1 if counter==-1 else 2
        expected_rng=seed
        for _ in range(expected_draws):expected_rng=(expected_rng*0x6c078965+0x3039)&0xffffffff
        if rng!=expected_rng:raise ValueError('Original UI queue random draw count differs')
        expected_count=0 if counter==-1 else 1
        if len(consumed)!=expected_count:raise ValueError('Original counter consumption queue count differs')
        cases.append(dict(temper=temper,counter=counter,side=side,seed=seed,queue=queue,
                          initialStateHex=initial.hex(),afterStateHex=after.hex(),consumed=consumed,
                          nativeRng=final_rng,originalUiRandomDraws=expected_draws))
    functions=[dict(start=hex(a),endExclusive=hex(b),sha256=sha(exe[a-0x400000:b-0x400000]))
               for a,b in [(0x519fd0,0x51a494),(0x51cedb,0x51cf3c),(0x49b490,0x49b520)]]
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,functions=functions,cases=cases,
                limits=['Synthetic UI storage with empty original message context; no discovered effective message resource',
                        'Actual original519fd0 queue generation and bounded original case8 hand removal only',
                        'Queue animation/render/audio/status scheduling not executed; no complete headed PC flow proof',
                        'Original UI queue consumes RNG; future core policy must account for it before recorded presentation'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps(dict(cases=len(cases),sha256=sha(output.read_bytes()),completeHeadedFlow=False)))
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();inspect(a.installation,a.output)
