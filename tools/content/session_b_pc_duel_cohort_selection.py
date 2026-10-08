#!/usr/bin/env python3
"""Untouched original589ac0/589ca0: explicit physical-health boundary inputs."""
import argparse,json,struct
from pathlib import Path
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output,cohorts_only=False):
    output_guard(installation,output)
    if output.exists():raise ValueError('Preserve receipt')
    d,w,source,geo,unused=prepare(installation)
    people=[]
    for native in range(670):
        p=w.call(0x490b00,native,receiver=w.root)
        if not w.call(0x47a600,p):continue
        war=w.call(0x489080,receiver=p)&255
        raw=bytes(w.u.mem_read(p,400))
        people.append(dict(native=native,pointer=p,war=war,health=raw[0x128],personality=struct.unpack_from('<i',raw,0xfc)[0],ruler=bool(w.call(0x488c00,receiver=p)),bonus=w.call(0x4faa60,p,count=10000000)))
    baseline=bytes(w.u.mem_read(w.root,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4));scores=[];cohorts=[]
    try:
        for index,person in enumerate([]if cohorts_only else people):
            p=person['pointer']
            for hp in [0,49,50,60,70,80,100]:
                w.u.mem_write(p+0x128,bytes([hp]))
                before=bytes(w.u.mem_read(w.root,0x300000))
                for flag in [0,1]:
                    result=w.call(0x589ac0,p,flag,count=10000000)
                    assert before==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
                    scores.append(dict(native=person['native'],health=hp,allowLowHealth=flag,score=result))
            w.u.mem_write(p+0x128,bytes([person['health']]))
            if index%100==0:print('original cohort score',index,len(scores),flush=True)
        assert baseline==bytes(w.u.mem_read(w.root,0x300000))
        byid={p['native']:p for p in people};unit=w.root+0x169730
        for ids in [[365,116,466],[558,14,517],[40,98,144],[248,370,432],[568,660,116]]:
            assert all(n in byid for n in ids)
            w.u.mem_write(unit+8,struct.pack('<4i',0,*ids))
            for hp in [49,50,60,70,80,100]:
                for n in ids:w.u.mem_write(byid[n]['pointer']+0x128,bytes([hp]))
                for flag in [0,1]:
                    before=bytes(w.u.mem_read(w.root,0x300000));picked=w.call(0x589ca0,unit,flag,count=10000000)
                    assert before==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
                    native=w.call(0x4883c0,receiver=picked)if picked else -1
                    cohorts.append(dict(nativeCrew=ids,health=hp,allowLowHealth=flag,validOriginalUnit=bool(w.call(0x47a630,unit)),pickedNative=native))
                for n in ids:w.u.mem_write(byid[n]['pointer']+0x128,bytes([byid[n]['health']]))
        w.u.mem_write(w.root,baseline);w.u.mem_write(0x8a5d44,rng)
        assert baseline==bytes(w.u.mem_read(w.root,0x300000))and rng==bytes(w.u.mem_read(0x8a5d44,4))
    except Exception as error:
        output.with_suffix('.failure.json').write_text(json.dumps(dict(source=source,error=repr(error),scores=scores,cohorts=cohorts,completeGoal=False),indent=2)+'\n');raise
    scoreReceipt=None
    if cohorts_only:
        scoreFile=Path('out/session-b/duel-cohort-selection-source0.json');scoreRaw=scoreFile.read_bytes();assert sha(scoreRaw)=='24ccf5097118c33c069343feadc23040188e58c96cb40fab4d4a7b766502808f';previous=json.loads(scoreRaw);assert previous['source']==source and previous['people']==people;scores=previous['scores'];scoreReceipt=sha(scoreRaw)
    output.write_text(json.dumps(dict(scoresReceipt=scoreReceipt,exeSha=EXE_SHA,source=source,geography=geo,people=people,scores=scores,cohorts=cohorts,wholeWorldAndRngRestored=True,limits=['Physical-health and mixed-owner crew values are explicit boundary fixtures','Original validity/current WAR/score/selection execute unchanged; no playable activation inference','Ordinary58b640 admission/fees/menu and APK remain separate'],completeGoal=False),indent=2)+'\n')
    print('PASS original cohort',len(scores),len(cohorts),'SHA',sha(output.read_bytes()))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('installation',type=Path);p.add_argument('--cohorts-only',action='store_true');p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output,a.cohorts_only)
