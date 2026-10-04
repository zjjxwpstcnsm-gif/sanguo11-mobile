#!/usr/bin/env python3
"""Original comparison/winner/post-card/topic transition, bounded model fixtures.

Optional UI pointers stay zero. These outputs do not certify complete headed
rounds, fury activation, AI, deck refill, terminal selection or settlement.
"""
import argparse,gzip,itertools,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from audit_pc_restoration_sources import EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    d=NativeDebateFlow(installation,(installation/'san11pk.exe').read_bytes());w=d.world;model=d.fixture
    w.call(0x51fcf0,receiver=model);template=bytes(w.u.mem_read(model,0x1b0));cases=[]
    normal=itertools.product(range(4),range(4),range(3),range(1,15),range(1,15),(0,23))
    inputs=[(lt,rt,topic,left,right,0,0,seed)for lt,rt,topic,left,right,seed in normal]
    for lt,rt,(lf,rf),(left,right),topic in itertools.product(range(4),range(4),((1,0),(0,1),(1,1)),((1,9),(10,1),(11,9),(12,1),(13,14),(12,10)),range(3)):
        inputs.append((lt,rt,topic,left,right,lf,rf,23))
    for lt,rt,topic,left,right,lf,rf,seed in inputs:
        w.u.mem_write(model,template)
        for offset,value in [(0x168,topic),(0x16c,0),(0x170,left),(0x174,right)]:w.u.mem_write(model+offset,struct.pack('<i',value))
        for side,(temper,fury,modifier,anger)in enumerate(((lt,lf,107,50),(rt,rf,94,80))):
            for offset,value in [(4,1000),(8,anger),(12,fury),(16,modifier),(0x9c,temper)]:w.u.mem_write(model+0x10+side*0xa0+offset,struct.pack('<i',value))
        w.u.mem_write(0x8a5d44,struct.pack('<I',seed))
        w.call(0x51e960,receiver=model);w.call(0x51f9e0,receiver=model);w.call(0x51fed0,receiver=model);w.call(0x51e7d0,receiver=model)
        raw=bytes(w.u.mem_read(model,0x1b0));rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
        values=[struct.unpack_from('<i',raw,0x10+side*0xa0+offset)[0]for side in range(2)for offset in (4,8,12)]
        values += [struct.unpack_from('<i',raw,offset)[0]for offset in (0x168,0x16c,0x178)]
        cases.append([lt,rt,topic,left,right,lf,rf,seed,*values,rng])
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,cases=cases,
                sequence=['51e960','51f9e0','51fed0','51e7d0'],inputIntelligence=[90,82],inputHealth=[1000,1000],inputAnger=[50,80],
                limits=['Synthetic selected-card fixtures; includes illegal fury selections to isolate function branches',
                        'Card0 rethink excluded; original deck/input/AI/fury activation and UI callbacks not executed',
                        'Healthy inputs avoid terminal rules; no complete round or campaign outcome proof'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps(dict(cases=len(cases),sha256=sha(output.read_bytes()),completeRound=False)))
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',required=True,type=Path)
    a=p.parse_args();inspect(a.installation,a.output)
