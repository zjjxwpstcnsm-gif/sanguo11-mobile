#!/usr/bin/env python3
"""Original bounded fury selection/activation and psychological-stage outputs.

Counter consumption in the original null-UI branch affects a temporary copy;
this report must be combined with the separately verified actual queue branch.
"""
import argparse,gzip,itertools,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from audit_pc_restoration_sources import EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    d=NativeDebateFlow(installation,(installation/'san11pk.exe').read_bytes());w=d.world;model=d.fixture
    trace=d.run([90,90],[0,0],[31,31],23);template=bytes.fromhex(trace['initialStateHex'])
    hands=[(0,1,2,3,10,13,14),(0,1,2,3,4,10,13),(0,1,2,3,4,10,14),(0,1,2,3,4,5,10)]
    cases=[];war_cases=[]
    for lt,rt,leader,allow,(la,ra),(lf,rf),hand,(lw,rw) in itertools.product(range(4),range(4),range(2),range(2),((99,99),(100,99),(99,100),(100,100)),((0,0),(1,0),(0,1)),hands,((0,0),(1,80),(100,90))):
        w.u.mem_write(model,template)
        for actor,war in zip(d.people,(lw,rw)):
            w.u.mem_write(actor+0x171,bytes([war]))
            if w.call(0x489080,receiver=actor)&255 != war:raise ValueError('Original current war getter differs')
        w.u.mem_write(model+0x16c,struct.pack('<i',leader))
        for side,(temper,anger,fury)in enumerate(((lt,la,lf),(rt,ra,rf))):
            base=model+0x10+side*0xa0
            for offset,value in [(4,1000),(8,anger),(12,fury),(0x9c,temper)]:w.u.mem_write(base+offset,struct.pack('<i',value))
            w.u.mem_write(base+0x14,struct.pack('<7i',*hand))
        w.u.mem_write(0x8a5d44,struct.pack('<I',23))
        counter=w.call(0x51fb60,allow,receiver=model);counter=counter if counter<0x80000000 else counter-0x100000000
        w.call(0x51ee50,receiver=model)
        raw=bytes(w.u.mem_read(model,0x1b0));values=[struct.unpack_from('<i',raw,0x10+side*0xa0+offset)[0]for side in range(2)for offset in (4,8,12)]
        values += [struct.unpack_from('<i',raw,offset)[0]for offset in (0x17c,0x1a4,0x1a8,0x1ac)]
        rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
        if rng!=23:raise ValueError('Unexpected null-UI fury RNG')
        if any(struct.unpack_from('<7i',raw,0x24+side*0xa0)!=hand for side in range(2)):raise ValueError('Original null-UI branch changed actual hand')
        if lw==rw==0:cases.append([lt,rt,leader,allow,la,ra,lf,rf,hands.index(hand),counter,*values,rng])
        else:war_cases.append([lt,rt,leader,allow,la,ra,lf,rf,hands.index(hand),lw,rw,counter,*values,rng])
    stages=[]
    for hp,prior in itertools.product(range(-100,1001),range(4)):
        w.u.mem_write(model,template)
        w.u.mem_write(model+0x14,struct.pack('<i',hp));w.u.mem_write(model+0x18c,struct.pack('<i',prior))
        result=w.call(0x51fc70,receiver=model);raw=bytes(w.u.mem_read(model,0x1b0))
        stages.append([hp,prior,struct.unpack_from('<i',raw,0x18c)[0],struct.unpack_from('<i',raw,0x194)[0],result])
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,cases=cases,warCases=war_cases,stages=stages,hands=hands,
                intelligence=[90,90],seed=23,sequence=['51fb60','51ee50'],
                limits=['Synthetic explicitly healthy actors, original optional UI pointers zero',
                        'Original null-UI counter does not consume actual hand; headed consumption verified separately',
                        'No timid burst progression, full round, AI, terminal/campaign outcome or APK integration'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps(dict(cases=len(cases),warCases=len(war_cases),stages=len(stages),sha256=sha(output.read_bytes()),completeRound=False)))
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',required=True,type=Path)
    a=p.parse_args();inspect(a.installation,a.output)
