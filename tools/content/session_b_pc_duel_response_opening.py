#!/usr/bin/env python3
"""Original507e20 winning-side estimate/output chance/full RNG argument matrix."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output,model_entry=False):
    output_guard(installation,output)
    if output.exists():raise ValueError('Preserve receipt')
    d,w,source,geo,unused=prepare(installation);people=[]
    for native in [116,163,195,222,658,590,98,144,185,395,432,515,660]:
        pointer=w.call(0x490b00,native,receiver=w.root);assert w.call(0x47a600,pointer)
        vtable=struct.unpack('<I',w.u.mem_read(pointer,4))[0];virtual=struct.unpack('<I',w.u.mem_read(vtable+0x48,4))[0]
        wars=[w.call(0x50c690,pointer,injury,1,count=10000000)for injury in range(4)]
        people.append(dict(native=native,pointer=pointer,wars=wars,rawWar=w.call(0x489080,receiver=pointer)&255,age=w.call(0x488a20,receiver=pointer),virtual48=bool(w.call(virtual,receiver=pointer))))
    valid=bool(w.call(0x47a630,w.root));difficulty=struct.unpack('<i',w.u.mem_read(w.root+0x1978-0x1958,4))[0];life=struct.unpack('<i',w.u.mem_read(w.root+0x1990-0x1958,4))[0]
    baseline=bytes(w.u.mem_read(w.root,0x300000));initialRng=bytes(w.u.mem_read(0x8a5d44,4));out=d.fixture+0x7000;rows=[]
    try:
        for left in range(len(people)):
            for right in range(len(people)):
                for flags in [(0,0),(1,0),(0,1),(1,1)]:
                    for seed in [0,23,0xffffffff]:
                        w.u.mem_write(out,struct.pack('<i',-999));w.u.mem_write(0x8a5d44,struct.pack('<I',seed))
                        if model_entry:
                            w.call(0x50ab90,receiver=d.fixture)
                            for side,actor,flagValue in [(0,people[left],flags[0]),(1,people[right],flags[1])]:
                                at=d.fixture+0x24+side*0xec;w.u.mem_write(at,struct.pack('<I',actor['pointer']));w.u.mem_write(at+12,struct.pack('<i',0));w.u.mem_write(at+28,struct.pack('<i',128 if flagValue else 0));w.u.mem_write(at+0xc4,struct.pack('<i',0))
                            beforeModel=bytes(w.u.mem_read(d.fixture,0x59c));result=w.call(0x508f00,receiver=d.fixture,count=10000000);assert beforeModel==bytes(w.u.mem_read(d.fixture,0x59c))
                        else:result=w.call(0x507e20,people[left]['pointer'],people[right]['pointer'],*flags,0,0,out,count=10000000)
                        assert baseline==bytes(w.u.mem_read(w.root,0x300000))
                        rows.append(dict(left=left,right=right,leftFlag=flags[0],rightFlag=flags[1],seed=seed,result=result,chance=struct.unpack('<i',w.u.mem_read(out,4))[0],rngAfter=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]))
            print('original opening response',left,len(rows),flush=True)
        w.u.mem_write(0x8a5d44,initialRng);assert initialRng==bytes(w.u.mem_read(0x8a5d44,4))
    except Exception as error:
        output.with_suffix('.failure.json').write_text(json.dumps(dict(error=repr(error),rows=rows,completeGoal=False),indent=2)+'\n');raise
    output.write_text(json.dumps(dict(modelEntry=model_entry,exeSha=EXE_SHA,source=source,geography=geo,people=people,settingsValid=valid,rawDifficulty=difficulty,rawLifeOption=life,rows=rows,wholeWorldPureAndRngRestored=True,limits=['Fixed function argument flags are explicit boundary fixtures, not current held item assertions','Original507e20/50c690/validity/age/random functions unmodified; registry presence does not prove activation','Settings are actual serializer VM values, not certified normal startup options','Normal admission/fees/menu and APK remain required'],completeGoal=False),indent=2)+'\n');print('PASS original response opening',sha(output.read_bytes()))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('installation',type=Path);p.add_argument('--model-entry',action='store_true');p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output,a.model_entry)
