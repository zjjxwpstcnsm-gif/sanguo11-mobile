#!/usr/bin/env python3
"""Original508890 raw-loyalty consumption, exact normal battle identities.
Declared round/raw-byte values isolate access; no getter/rule/result replacement.
"""
import argparse,json,struct
from pathlib import Path
from unicorn import UC_HOOK_MEM_READ
from unicorn.x86_const import UC_X86_REG_EIP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output):
    output_guard(installation,output)
    if output.exists():raise ValueError('Preserve source evidence')
    d,w,src,geo,_=prepare(installation)
    baseline=bytes(w.u.mem_read(0x7200000,0x300000));oldrng=bytes(w.u.mem_read(0x8a5d44,4))
    people={n:w.call(0x490b00,n,receiver=w.root)for n in [503,411,669]}
    assert all(w.call(0x47a600,p)for p in people.values())
    assert w.call(0x488c00,receiver=people[411])==0
    w.call(0x50ab90,receiver=d.fixture)
    model=bytearray(w.u.mem_read(d.fixture,0x59c))
    for side,slots in [(0,[503,None,None]),(1,[411,669,None])]:
        team=0x24+side*0xec
        struct.pack_into('<i',model,team+0xc4,0)
        for slot,n in enumerate(slots):
            if n is not None:struct.pack_into('<2i',model,team+64*slot,people[n],100)
    candidate=people[669];reads=[]
    def observe(u,access,address,size,value,user):
        if address<=candidate+0xac<address+size:reads.append(dict(pc=hex(u.reg_read(UC_X86_REG_EIP)),address=hex(address),bytes=size))
    w.u.hook_add(UC_HOOK_MEM_READ,observe)
    rows=[]
    try:
        for round_ in [0,2,4,6,15]:
            for raw in [0,96,100,120,255]:
                w.u.mem_write(0x7200000,baseline);w.u.mem_write(candidate+0xac,bytes([raw]));struct.pack_into('<i',model,0x10,round_);w.u.mem_write(d.fixture,bytes(model));w.u.mem_write(0x8a5d44,struct.pack('<I',23));before=bytes(w.u.mem_read(0x7200000,0x300000));reads.clear()
                value=w.call(0x508890,1,1,receiver=d.fixture,count=10000000)
                assert before==bytes(w.u.mem_read(0x7200000,0x300000))
                rows.append(dict(round=round_,declaredCandidateRaw=raw,originalReturn=value,originalRng=int.from_bytes(w.u.mem_read(0x8a5d44,4),'little'),rawByteReads=list(reads)))
        for round_ in [0,2,4,6,15]:
            r=[x for x in rows if x['round']==round_]
            assert all(not x['rawByteReads']for x in r)
            assert len({(x['originalReturn'],x['originalRng'])for x in r})==1
        w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,oldrng)
        assert baseline==bytes(w.u.mem_read(0x7200000,0x300000))and oldrng==bytes(w.u.mem_read(0x8a5d44,4))
        result=dict(exeSha=EXE_SHA,source=src,geography=geo,natives={'ownActive':411,'candidate':669,'opponentActive':503},ownRulerPredicate=False,cases=rows,wholeWorldAndRngRestored=True,limits=['Actual source identities from normal accepted battle; round/raw/model placement explicit VM boundary inputs','Full original508890 and relation/owner getters unchanged; raw memory read hook observes only','Non-ruler branch only, not a proof of loyalty decay/reward or other needed-raw branches','Normal deployment/full campaign/API/APK are separate acceptance'],completeGoal=False)
        output.write_text(json.dumps(result,indent=2)+'\n');print('PASS original508890 non-ruler branch25 cases/raw never consumed',sha(output.read_bytes()),flush=True)
    except Exception as e:
        output.with_suffix('.failure.json').write_text(json.dumps(dict(error=repr(e),cases=rows,pc=hex(w.u.reg_read(UC_X86_REG_EIP))),indent=2)+'\n');raise

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
