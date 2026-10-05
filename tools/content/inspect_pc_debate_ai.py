#!/usr/bin/env python3
"""Original AI priority tables and native choice/RNG with explicit model inputs."""
import argparse,gzip,itertools,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from audit_pc_restoration_sources import EXE_SHA,output_guard,json_bytes,sha

TABLES=[(0x8b42c8,11),(0x8b4350,12),(0x8b43e0,16),(0x8b44a0,3)]
HANDS=[(0,1,2,3,10,13,14),(0,1,4,7,10,11,12),(0,2,3,5,6,8,9)]

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    d=NativeDebateFlow(installation,(installation/'san11pk.exe').read_bytes());w=d.world;model=d.fixture;context=model+0x150
    w.call(0x51fcf0,receiver=model);template=bytes(w.u.mem_read(model,0x1b0));tables=[]
    for address,count in TABLES:
        raw=bytes(w.u.mem_read(address,count*12));tables.append(dict(address=hex(address),sha256=sha(raw),entries=[list(struct.unpack_from('<3I',raw,i*12))for i in range(count)]))
    for actor in d.people:
        w.call(0x489f10,receiver=actor);w.u.mem_write(actor+0xa0,struct.pack('<I',0))
    cases=[]
    for iq,temper,other_temper,(own_fury,other_fury),topic,other_card,hand_index in itertools.product((20,59,60,70,80,90,100),range(4),range(4),((0,0),(4,0),(0,4)),range(3),(-1,3,6,10,12),range(3)):
        w.u.mem_write(model,template);slots=4 if iq<70 else 5 if iq<80 else 6 if iq<90 else 7
        hand=HANDS[hand_index][:slots]+(-1,)*(7-slots)
        for side,(actor,personality,fury,current_iq)in enumerate(zip(d.people,(temper,other_temper),(own_fury,other_fury),(iq,90))):
            w.u.mem_write(actor+0x172,bytes([current_iq]));base=model+0x10+side*0xa0
            for offset,value in [(0,actor),(4,750),(8,85),(12,fury),(16,100),(0x30,slots if side==0 else 7),(0x9c,personality)]:w.u.mem_write(base+offset,struct.pack('<I',value))
            w.u.mem_write(base+0x14,struct.pack('<7i',*(hand if side==0 else HANDS[0])))
        for offset,value in [(0x168,topic),(0x170,-1),(0x174,other_card),(0x184,1),(0x188,1)]:w.u.mem_write(model+offset,struct.pack('<i',value))
        w.call(0x5166d0,model,0,receiver=context)
        before=bytes(w.u.mem_read(model,0x1b0));world_before=sha(bytes(w.u.mem_read(0x7200000,0x300000)))
        w.u.mem_write(0x8a5d44,struct.pack('<I',23));choice=w.call(0x517600,receiver=context,count=10000000)
        choice=choice if choice<0x80000000 else choice-0x100000000
        if before!=bytes(w.u.mem_read(model,0x1b0)) or world_before!=sha(bytes(w.u.mem_read(0x7200000,0x300000))):raise ValueError('Native AI mutated model/world instead of returning choice')
        if not 0<=choice<slots or w.call(0x51e3c0,0,hand[choice],receiver=model)!=1:raise ValueError('Native AI returned an unavailable card')
        rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
        cases.append([iq,temper,other_temper,own_fury,other_fury,topic,other_card,hand_index,choice,rng])
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,tables=tables,hands=HANDS,cases=cases,seed=23,
                health=[750,750],anger=[85,85],reconsiderAvailable=[True,True],
                limits=['Explicit synthetic actor identity/relationships, not real source battle start',
                        'Choice returns slot; original RNG consumes its complete original heuristic/shuffle calls',
                        'No model/world mutation, AI rule interception, full headed rounds, campaign settlement or APK integration'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps(dict(cases=len(cases),sha256=sha(output.read_bytes()),completeContest=False)))
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',required=True,type=Path)
    a=p.parse_args();inspect(a.installation,a.output)
