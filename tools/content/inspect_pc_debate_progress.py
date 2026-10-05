#!/usr/bin/env python3
"""Original bounded reconsider, timid burst and terminal model transitions."""
import argparse,gzip,itertools,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    packed=(ROOT/'docs/handoff/20261004/session1/debate-flow-native.json.gz').read_bytes()
    if sha(packed)!='193ca13a6bb92a3c1d389934166249611536150a50733ba4fd183fe786a5fca7':raise ValueError('Original initial state oracle changed')
    initial=json.loads(gzip.decompress(packed))['cases'];d=NativeDebateFlow(installation,(installation/'san11pk.exe').read_bytes());w=d.world;model=d.fixture
    d.run([90,82],[0,0],[31,31],23)
    def load(case):
        w.u.mem_write(model,bytes.fromhex(case['initialStateHex']))
        for actor,temper in zip(d.people,case['nativePersonality']):w.u.mem_write(actor+0xfc,struct.pack('<i',temper))
    def speaker_values(raw,side):
        base=0x10+side*0xa0;offsets=[4,8,12,16]+list(range(0x14,0x30,4))+[0x30]+list(range(0x34,0x7c,4))+[0x7c]+list(range(0x80,0x94,4))+[0x94]
        return [struct.unpack_from('<i',raw,base+offset)[0]for offset in offsets]
    reconsider=[]
    for case_index,side,fury,topic,cursor,seed in itertools.product(range(16),range(2),(0,1,4),range(3),(-1,18),(0,23)):
        case=initial[case_index];load(case);w.u.mem_write(model+0x168,struct.pack('<i',topic));w.u.mem_write(model+0x1c+side*0xa0,struct.pack('<i',fury))
        if cursor>=0:w.u.mem_write(model+0x8c+side*0xa0,struct.pack('<i',cursor))
        w.u.mem_write(0x8a5d44,struct.pack('<I',seed));w.call(0x51ec50,side,receiver=model,count=10000000)
        raw=bytes(w.u.mem_read(model,0x1b0));rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
        reconsider.append([case_index,side,fury,topic,cursor,seed,*speaker_values(raw,side),struct.unpack_from('<i',raw,0x184+side*4)[0],rng])
    burst=[]
    for side,topic,seed in itertools.product(range(2),range(3),(0,23)):
        load(initial[0]);w.u.mem_write(model+0x168,struct.pack('<i',topic));w.u.mem_write(model+0x1c+side*0xa0,struct.pack('<i',1));w.u.mem_write(model+0x1a4,struct.pack('<3i',side,0,1));w.u.mem_write(0x8a5d44,struct.pack('<I',seed))
        for step in range(8):
            w.call(0x51ef30,receiver=model,count=10000000);raw=bytes(w.u.mem_read(model,0x1b0))
            values=[struct.unpack_from('<i',raw,0x10+s*0xa0+offset)[0]for s in range(2)for offset in (4,8,12)]
            values += list(struct.unpack_from('<7i',raw,0x24))+list(struct.unpack_from('<7i',raw,0xc4))+list(struct.unpack_from('<3i',raw,0x1a4))
            burst.append([side,topic,seed,step,*values,struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]])
            if values[-1]==0:break
    terminal=[]
    for leader,left_hp,right_hp,prior in itertools.product(range(2),(-100,0,1,1000),(-100,0,1,1000),(-1,0,1)):
        load(initial[0]);w.u.mem_write(model+0x16c,struct.pack('<i',leader));w.u.mem_write(model+0x14,struct.pack('<i',left_hp));w.u.mem_write(model+0xb4,struct.pack('<i',right_hp));w.u.mem_write(model+0x19c,struct.pack('<i',prior));w.u.mem_write(0x8a5d44,struct.pack('<I',23))
        result=w.call(0x51f060,receiver=model);raw=bytes(w.u.mem_read(model,0x1b0))
        terminal.append([leader,left_hp,right_hp,prior,struct.unpack_from('<i',raw,0x19c)[0],result,struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]])
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,initialOracleSha256=sha(packed),reconsider=reconsider,burst=burst,terminal=terminal,
                limits=['Original null-UI model branches only; headed burst/reconsider callback RNG not certified',
                        'Initial deck/actors from original explicit fixtures; no real campaign trigger or settlement',
                        'Psychological terminal selection only, not original external end-condition manager51f0e0'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps(dict(reconsider=len(reconsider),burst=len(burst),terminal=len(terminal),sha256=sha(output.read_bytes()),completeContest=False)))
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',required=True,type=Path)
    a=p.parse_args();inspect(a.installation,a.output)
