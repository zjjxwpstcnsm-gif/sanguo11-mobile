#!/usr/bin/env python3
"""Execute original force friendship and complete monthly officer relation helper.

Explicit numeric/status fixtures; no rules, RNG or relationship getters replaced.
Not proof of complete PC startup events or Android settlement integration.
"""
import argparse,gzip,itertools,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from audit_pc_restoration_sources import EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    exe=(installation/'san11pk.exe').read_bytes();w=NativeDebateFlow(installation,exe).world
    pages=load_original_data(w.u);shared=(installation/'Media/scenario/Scenario.s11').read_bytes();raw=(installation/'Media/scenario/Scen000.s11').read_bytes()
    w.load(shared,True);w.load(raw);w.call(0x73c500);w.call(0x73ca80)
    baseline=bytes(w.u.mem_read(0x7200000,0x300000));forces=[i for i in range(42)if w.call(0x47a630,w.root+0x7af8+i*0x12c)]
    if len(forces)<2:raise ValueError('Two actual original active forces required')
    a,b=forces[:2];pa,pb=[w.root+0x7af8+i*0x12c for i in [a,b]];friendship=[]
    for before,reverse,delta in itertools.product([0,1,25,50,99,100],[0,100],[-101,-50,-1,0,1,50,101]):
        w.u.mem_write(0x7200000,baseline);w.call(0x481ac0,b,before,receiver=pa);w.call(0x481ac0,a,reverse,receiver=pb)
        world_before=bytes(w.u.mem_read(0x7200000,0x300000));w.u.mem_write(0x8a5d44,struct.pack('<I',23))
        result=w.call(0x4b5f90,a,b,delta&0xffffffff);result=result if result<0x80000000 else result-0x100000000
        left=w.call(0x4814e0,b,receiver=pa)&255;right=w.call(0x4814e0,a,receiver=pb)&255
        expected=max(0,min(100,before+delta));world_after=bytes(w.u.mem_read(0x7200000,0x300000));patched=bytearray(world_before)
        for actor,target in [(pa,b),(pb,a)]:patched[actor-0x7200000+0xc+target]=expected
        seed=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
        if left!=expected or right!=expected or result!=expected-before or bytes(patched)!=world_after or seed!=23:raise ValueError('Original friendship semantics/side effects differ')
        friendship.append(dict(before=before,reverseBefore=reverse,delta=delta,leftAfter=left,rightAfter=right,returnedDelta=result,seedBefore=23,seedAfter=seed,fullWorldChecked=True))
    actor=w.root+0xc0bc+222*0x190;target=w.root+0xc0bc+116*0x190;monthly=[]
    def state(p):
        blob=bytes(w.u.mem_read(p,0x190));return dict(status=struct.unpack_from('<i',blob,0xa0)[0],allowed=bool(w.call(0x47a630,p)),rawLoyalty=blob[0xac],hateNativeId=struct.unpack_from('<i',blob,0x164)[0],hateMonths=blob[0x168],captiveCounter=blob[0x169])
    for status,other_status,months in itertools.product(range(9),[3,6,7,8],[1,2,6,255]):
        w.u.mem_write(0x7200000,baseline)
        for p,s in [(actor,status),(target,other_status)]:w.u.mem_write(p+0xa0,struct.pack('<i',s))
        # Explicit saved-field fixture even when inactive. Setter admission itself
        # is tested by the complete original4a72e0 ->4a56c0 chain below.
        w.u.mem_write(actor+0x164,struct.pack('<i',116));w.u.mem_write(actor+0x168,bytes([months]))
        before=state(actor);other=state(target);world_before=bytes(w.u.mem_read(0x7200000,0x300000));w.u.mem_write(0x8a5d44,struct.pack('<I',23))
        w.call(0x58bb30,count=10000000);after=state(actor);world_after=bytes(w.u.mem_read(0x7200000,0x300000));seed=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
        if seed!=23:raise ValueError('Original monthly relation helper unexpectedly consumes RNG')
        monthly.append(dict(before=before,targetBefore=other,after=after,seedBefore=23,seedAfter=seed,worldBeforeSha256=sha(world_before),worldAfterSha256=sha(world_after),changedBytes=[dict(offset=i,before=x,after=y)for i,(x,y)in enumerate(zip(world_before,world_after))if x!=y]))
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourcePath='Media/scenario/Scen000.s11',sourceSha256=sha(raw),sharedSha256=sha(shared),originalDataPages=pages,forceIds=[a,b],friendshipCases=friendship,monthlyCases=monthly,
        functions=[dict(start=hex(a),endExclusive=hex(z),sha256=sha(exe[a-0x400000:z-0x400000]))for a,z in [(0x4b5f90,0x4b6025),(0x58bb30,0x58bb89),(0x4a72e0,0x4a733b),(0x4a56c0,0x4a5715)]],
        limits=['Source0 real layered records plus explicit friendship/status/month counters', 'Complete monthly helper also increments a status5 captive byte169 counter; this is separate from raw loyalty byteac','Caller timing separately traced to original month-start branch; full startup/GUI not executed','No Android settlement conclusion implied'],completeGoal=False)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0));print(json.dumps(dict(friendshipCases=len(friendship),monthlyCases=len(monthly),sha256=sha(output.read_bytes()))),flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
