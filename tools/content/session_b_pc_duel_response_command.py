#!/usr/bin/env python3
"""Original58b400 selection on declared original units with computed caches.
Does not claim ordinary deployment/menu, player picker or command fee closure.
"""
import argparse,json,struct
from pathlib import Path
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_EIP,UC_X86_REG_ESP
from session_b_pc_duel_crew import prepare
from audit_pc_restoration_sources import EXE_SHA,sha,output_guard

def inspect(installation,output,terrain_refresh=False):
    output_guard(installation,output)
    if output.exists():raise ValueError('Preserve receipt')
    path=Path('out/session-b/duel-unit-context-source0-v4.json');raw=path.read_bytes();assert sha(raw)=='49c1ed7a9e7453e9834ef58d08f2c8f7f906a278b5d1b43bc3e60328f5592c38';context=json.loads(raw)
    d,w,source,geo,unused=prepare(installation);assert source==context['source'];baseline=bytes(w.u.mem_read(w.root,0x300000));initialRng=bytes(w.u.mem_read(0x8a5d44,4));units=[];current=[]
    try:
        for unit,case in zip(context['units'],context['cases']):
            pointer=unit['pointer'];w.u.mem_write(pointer,bytes.fromhex(case['afterHex']));w.u.mem_write(pointer+0x3c,struct.pack('<hh',80+unit['index'],80));w.u.mem_write(pointer+0x24,struct.pack('<i',0))
            w.call(0x4962b0,0,0,5000,receiver=pointer);w.call(0x496250,997,receiver=pointer);w.call(0x496280,17000,receiver=pointer)
            w.u.mem_write(pointer+0x18,struct.pack('<H',5000));w.u.mem_write(pointer+0x1a,bytes([100]))
            w.u.reg_write(UC_X86_REG_EAX,pointer);location=w.call(0x4a7530)
            for person in case['declaredCrew']:
                w.call(0x4a0cb0,person['pointer'],location,receiver=0x799895c,count=10000000)
                for injury in range(4):w.call(0x50c690,person['pointer'],injury,1,count=10000000)
            w.call(0x496f40 if terrain_refresh else 0x496dd0,receiver=pointer,count=10000000)
            units.append(pointer);current.append(dict(index=unit['index'],pointer=pointer,crew=case['declaredCrew'],originalHuman=w.call(0x47a6d0,receiver=pointer),unitHex=bytes(w.u.mem_read(pointer,244)).hex()))
        before=bytes(w.u.mem_read(w.root,0x300000));rows=[];flag=d.fixture+0x7000;position=d.fixture+0x7100
        for direction in range(2):
            for seed in [1,2,3,7,23,24,99,0xffffffff]:
                w.u.mem_write(0x8a5d44,struct.pack('<I',seed));w.u.mem_write(flag,bytes(4));w.u.mem_write(position,bytes(w.u.mem_read(units[1-direction]+0x3c,4)))
                first=w.call(0x58b400,units[direction],units[1-direction],current[direction]['originalHuman'],0xffffffff,position,flag,1,0,count=10000000)
                firstRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];counterFirst=struct.unpack('<i',w.u.mem_read(flag,4))[0]
                w.u.mem_write(flag,bytes(4));second=w.call(0x58b400,units[1-direction],units[direction],current[1-direction]['originalHuman'],0xffffffff,position,flag,0,first,count=10000000)
                after=bytes(w.u.mem_read(w.root,0x300000))
                assert before==after
                rows.append(dict(direction=direction,seed=seed,firstNative=w.call(0x4883c0,receiver=first)if first else -1,firstRng=firstRng,firstCounter=counterFirst,secondNative=w.call(0x4883c0,receiver=second)if second else -1,secondRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],secondCounter=struct.unpack('<i',w.u.mem_read(flag,4))[0],wholeWorldPure=True))
                print('original response',rows[-1],flush=True)
        w.u.mem_write(w.root,baseline);w.u.mem_write(0x8a5d44,initialRng);assert baseline==bytes(w.u.mem_read(w.root,0x300000))and initialRng==bytes(w.u.mem_read(0x8a5d44,4))
    except Exception as error:
        sp=w.u.reg_read(UC_X86_REG_ESP);output.with_suffix('.failure.json').write_text(json.dumps(dict(source=source,error=repr(error),ip=hex(w.u.reg_read(UC_X86_REG_EIP)),stackHex=bytes(w.u.mem_read(sp,64)).hex(),units=current,completeGoal=False),indent=2)+'\n');raise
    output.write_text(json.dumps(dict(correctedTargetAndCounterArgumentPositions=True,terrainRefresh=terrain_refresh,exeSha=EXE_SHA,source=source,geography=geo,contextSha=sha(raw),units=current,rows=rows,wholeWorldAndRngRestored=True,limits=['Declared original constructors/person-unit links/category0/resource/status0/equipment0 inputs','Original496dd0 computes actual unit caches; selection/rule/RNG code unchanged','No ordinary deployment/menu/human selection/fee/APK proof'],completeGoal=False),indent=2)+'\n');print('PASS original response',sha(output.read_bytes()))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('installation',type=Path);p.add_argument('--terrain-refresh',action='store_true');p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output,a.terrain_refresh)
