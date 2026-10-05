#!/usr/bin/env python3
"""Run original frames with bounded original UI numeric effect callbacks.

Explicit effects-only presentation binding, not a full headed PC GUI. Original
rule/AI/RNG functions execute unmodified. Type4 bookkeeping and type8 hand
effects are drained before their rendering boundaries.
"""
import argparse,gzip,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE,UC_HOOK_MEM_WRITE
from unicorn.x86_const import UC_X86_REG_EDI,UC_X86_REG_ESI
from inspect_pc_debate_flow import NativeDebateFlow
from inspect_pc_debate_ui_callbacks import FUNCTIONS
from audit_pc_restoration_sources import EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    exe=(installation/'san11pk.exe').read_bytes();d=NativeDebateFlow(installation,exe);w=d.world
    w.u.mem_map(0x7500000,0x200000);w.call(0x49b490,receiver=0x767cab8)
    ui=0xc200000;w.u.mem_map(ui,0x30000);calls=[];draws=[];cases=[]
    def observe_callback(u,address,size,user):calls.append(hex(address))
    def observe_random(u,access,address,size,value,user):draws.append(value&0xffffffff)
    for address in list(FUNCTIONS)+[0x5184a0]:w.u.hook_add(UC_HOOK_CODE,observe_callback,begin=address,end=address)
    w.u.hook_add(UC_HOOK_MEM_WRITE,observe_random,begin=0x8a5d44,end=0x8a5d47)
    for left in range(4):
        for right in range(4):
            w.u.mem_write(0x91ba860,struct.pack('<I',0))
            for side,temper in enumerate((left,right)):
                actor=d.people[side];w.call(0x489f10,receiver=actor)
                w.u.mem_write(actor+0xa0,struct.pack('<I',0));w.u.mem_write(actor+0x172,bytes([90 if side==0 else 82]))
                w.u.mem_write(actor+0xfc,struct.pack('<I',temper));w.u.mem_write(actor+0x124,struct.pack('<I',31<<3))
            w.u.mem_write(d.fixture,bytes(0x1000));w.call(0x51fcf0,receiver=d.fixture)
            w.u.mem_write(0x8b5214+0x28,struct.pack('<I',1))
            if w.call(0x51e220,*d.people,0,receiver=0x8b5214,count=10000000)!=1:raise ValueError('Original input rejected')
            w.u.mem_write(0x8a5d44,struct.pack('<I',23));draws.clear();calls.clear()
            if w.call(0x51fd10,receiver=d.fixture,count=10000000)!=1:raise ValueError('Original initialization failed')
            total=len(draws);initial=bytes(w.u.mem_read(d.fixture,0x1b0));initial_rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
            preferences=[]
            for side,actor in enumerate(d.people):
                preference=-1
                if w.call(0x47a630,actor):
                    other_id=w.call(0x491310,d.people[1-side],receiver=0x7201958)
                    if w.call(0x4889e0,other_id,receiver=actor):preference=0
                    elif w.call(0x488910,other_id,receiver=actor):preference=1
                preferences.append(preference)
            if struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]!=initial_rng:raise ValueError('Relationship input read consumed RNG')
            w.u.mem_write(ui,bytes(0x30000));w.u.mem_write(ui+0x10,struct.pack('<I',d.fixture));trace=[]
            for frame in range(2000):
                phase,sub=struct.unpack('<2i',w.u.mem_read(d.fixture+8,8))
                active=phase in (1,5,6,7)or phase==4 and sub in (2,7)
                w.u.mem_write(0x91ba860,struct.pack('<I',ui if active else 0))
                w.u.mem_write(ui+0x265dc,struct.pack('<I',0))
                for i in range(100):w.u.mem_write(ui+0x1febc+i*0x108,struct.pack('<i',-1))
                draws.clear();calls.clear();w.call(0x51e300,1,receiver=d.fixture,count=10000000)
                count=struct.unpack('<i',w.u.mem_read(ui+0x265dc,4))[0]
                if not 0<=count<100:raise ValueError('Original queue overflow')
                consumed=[];stage_updates=[]
                for i in range(count):
                    addr=ui+0x1febc+i*0x108;raw=bytes(w.u.mem_read(addr,0x108))
                    if struct.unpack_from('<i',raw)[0]==8:
                        w.u.reg_write(UC_X86_REG_EDI,ui);w.u.reg_write(UC_X86_REG_ESI,addr)
                        w.call(0x51cedb,stop=0x51cf3c)
                        consumed.append(list(struct.unpack_from('<i',raw,o)[0]for o in [4,0xf4]))
                    elif struct.unpack_from('<i',raw)[0]==4:
                        side=struct.unpack_from('<i',raw,4)[0];stage=struct.unpack_from('<i',raw,0x34)[0]
                        if stage not in range(4):raise ValueError('Unexamined non-health stage queue')
                        # Original type4 dispatches51c2c0; real prologue writes
                        # UI stage at51c33f. Stop before409180/rendering context.
                        w.call(0x51c2c0,side,stage,receiver=ui,stop=0x51c346)
                        stage_updates.append([side,stage])
                total+=len(draws);raw=bytes(w.u.mem_read(d.fixture,0x1b0))
                trace.append(dict(frame=frame,stateHex=raw.hex(),nativeRng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],
                                  nativeDraws=total,callbacks=list(calls),consumed=consumed,stageUpdates=stage_updates,
                                  uiStages=list(struct.unpack('<2i',w.u.mem_read(ui+0x1fcc8,8)))))
                if struct.unpack_from('<i',raw,8)[0]==9:break
            else:raise ValueError('Original effects-only model failed to terminate')
            cases.append(dict(personality=[left,right],intelligence=[90,82],talkMasks=[31,31],war=[0,0],seed=23,
                              terminalPreferences=preferences,initialStateHex=initial.hex(),initialRng=initial_rng,trace=trace))
            print(json.dumps(dict(personality=[left,right],frames=len(trace),finalNativeRng=trace[-1]['nativeRng'])),flush=True)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,frameFunction='51e300',cases=cases,
                callbackPolicy='effects-only: phase1/5/6/7 and phase4 sub2/7; other phases optional UI unbound',
                limits=['Explicit synthetic original actors IQ90/82, war0, five talks; source campaign input not certified',
                        'Original empty message context and synthetic queue; optional card widget pointer remains original zero',
                        'Original callbacks execute completely; type4 stage prologue and type8 hand removal drained before graphics/audio',
                        'Presentation/GUI scheduling, widget selection and campaign settlement excluded; not complete headed PC flow'],
                completeHeadedFlow=False,productionIntegrated=False)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps(dict(cases=16,frames=sum(len(c['trace'])for c in cases),sha256=sha(output.read_bytes()))),flush=True)
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
