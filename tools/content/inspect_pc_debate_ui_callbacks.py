#!/usr/bin/env python3
"""Run twelve original debate UI queue callbacks, with observed original RNG.

Synthetic queue storage and empty original message context are explicit inputs.
Rendering/audio/scheduling are excluded. Original model and RNG are not hooked.
"""
import argparse, gzip, itertools, json, struct
from pathlib import Path
from unicorn import UC_HOOK_CODE, UC_HOOK_MEM_WRITE
from unicorn.x86_const import UC_X86_REG_ESP, UC_X86_REG_EDI, UC_X86_REG_ESI
from inspect_pc_debate_flow import NativeDebateFlow
from audit_pc_restoration_sources import EXE_SHA, output_guard, json_bytes, sha

FUNCTIONS = {0x519150:0x5193ec,0x5193f0:0x5195c0,0x5195c0:0x5198d0,
             0x5198d0:0x5199e0,0x5199e0:0x519b90,0x519b90:0x519e00,
             0x519e00:0x519fd0,0x519fd0:0x51a4a0,0x51a4a0:0x51a730,
             0x51a730:0x51a870,0x51a870:0x51aa70,0x51aa70:0x51ae60}

def variants(side):
    result=[('instant',0x519150,[side]),('shout',0x5193f0,[side,80,15]),
            ('rethink',0x5198d0,[side]),('calm',0x5199e0,[side,15]),
            ('rash',0x51a870,[side,280,18]),('stages',0x51aa70,[])]
    for card,deflected in itertools.product(range(1,10),range(2)):
        result.append(('ordinary',0x5195c0,[side,card,80,15,deflected]))
    for deflected in range(2):
        result.extend([('guile',0x519b90,[side,15,deflected]),
                       ('ignore',0x519e00,[side,15,deflected])])
    for counter in (-1,13,14):result.append(('counter',0x519fd0,[side,counter]))
    for count in (0,5):
        result.extend([('burstStrike',0x51a4a0,[side,1,80,15,count]),
                       ('burstEnd',0x51a730,[side,int(count>0)])])
    return result

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    exe=(installation/'san11pk.exe').read_bytes();d=NativeDebateFlow(installation,exe);w=d.world
    w.u.mem_map(0x7500000,0x200000);w.call(0x49b490,receiver=0x767cab8)
    ui=0xc200000;w.u.mem_map(ui,0x30000);observed=[];calls=[];choices=[];cases=[];pending=[]
    def rng_write(u,access,address,size,value,user):
        if address!=0x8a5d44 or size!=4:raise ValueError('Unexpected original RNG write')
        observed.append(value&0xffffffff)
    def percent_entry(u,address,size,user):
        esp=u.reg_read(UC_X86_REG_ESP);ret,chance=struct.unpack('<2I',u.mem_read(esp,8))
        calls.append(dict(returnAddress=hex(ret),chance=chance))
        if pending:raise ValueError('Previous original percent choice not queued')
        pending.append(len(calls)-1)
    def action_write(u,access,address,size,value,user):
        offset=address-ui-0x1febc
        if size!=4 or offset<0 or offset//0x108>=100 or offset%0x108!=0x100 or not pending:return
        entry=ui+0x1febc+(offset//0x108)*0x108
        choices.append(dict(percentCallIndex=pending.pop(),queueIndex=offset//0x108,
                            side=struct.unpack('<i',u.mem_read(entry+4,4))[0],action=value))
    w.u.hook_add(UC_HOOK_MEM_WRITE,rng_write,begin=0x8a5d44,end=0x8a5d47)
    w.u.hook_add(UC_HOOK_CODE,percent_entry,begin=0x4721d0,end=0x4721d0)
    w.u.hook_add(UC_HOOK_MEM_WRITE,action_write,begin=ui+0x1febc,end=ui+0x265db)
    for left,right,side,seed,fury in itertools.product(range(4),range(4),range(2),(0,1,23,0xffffffff),(0,4)):
        for name,fn,args in variants(side):
            w.u.mem_write(ui,bytes(0x30000));w.u.mem_write(ui+0x10,struct.pack('<I',d.fixture))
            # Existing stages equal one side only: stage callback takes both
            # no-change and change branches when the observer side flips.
            stages=[1,1] if side==0 else [-1,-1]
            w.u.mem_write(ui+0x1fcc8,struct.pack('<2i',*stages))
            for i in range(100):w.u.mem_write(ui+0x1febc+i*0x108,struct.pack('<i',-1))
            w.u.mem_write(d.fixture,bytes(0x1b0))
            for actor_side,temper in enumerate((left,right)):
                actor=d.people[actor_side];w.call(0x489f10,receiver=actor)
                base=d.fixture+0x10+actor_side*0xa0
                w.u.mem_write(base,struct.pack('<4i',actor,750,85,fury))
                w.u.mem_write(base+0x14,struct.pack('<7i',0,1,2,3,10,13,14))
                w.u.mem_write(base+0x30,struct.pack('<i',7));w.u.mem_write(base+0x9c,struct.pack('<i',temper))
            initial=bytes(w.u.mem_read(d.fixture,0x1b0))
            w.u.mem_write(0x8a5d44,struct.pack('<I',seed));observed.clear();calls.clear();choices.clear();pending.clear()
            w.call(fn,*[a&0xffffffff for a in args],receiver=ui,count=10000000)
            if bytes(w.u.mem_read(d.fixture,0x1b0))!=initial:raise ValueError('Callback mutated model before queue execution')
            if len(observed)!=len(calls)or any(x['chance']!=50 for x in calls):raise ValueError('Unexamined callback RNG protocol')
            if pending or len(choices)!=len(calls):raise ValueError('Original random queue choices incomplete')
            expected=seed
            for value in observed:
                expected=(expected*0x6c078965+0x3039)&0xffffffff
                if value!=expected:raise ValueError('Original callback RNG transition differs')
            count=struct.unpack('<i',w.u.mem_read(ui+0x265dc,4))[0]
            if not 0<=count<100:raise ValueError('Queue wrapped or invalid')
            queue=[];consumed=[]
            for i in range(count):
                address=ui+0x1febc+i*0x108;raw=bytes(w.u.mem_read(address,0x108))
                kind=struct.unpack_from('<i',raw)[0];queue.append(raw.hex())
                if kind==8:
                    w.u.reg_write(UC_X86_REG_EDI,ui);w.u.reg_write(UC_X86_REG_ESI,address)
                    w.call(0x51cedb,stop=0x51cf3c)
                    consumed.append(dict(side=struct.unpack_from('<i',raw,4)[0],slot=struct.unpack_from('<i',raw,0xf4)[0]))
            final_rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
            if final_rng!=expected:raise ValueError('Queue card removal consumed RNG')
            cases.append(dict(callback=name,function=hex(fn),arguments=args,personality=[left,right],
                              fury=fury,side=side,seed=seed,previousStages=stages,health=[750,750],
                              initialStateHex=initial.hex(),afterStateHex=bytes(w.u.mem_read(d.fixture,0x1b0)).hex(),
                              queueHex=queue,consumed=consumed,percentCalls=list(calls),
                              rngWrites=list(observed),randomQueueChoices=list(choices),finalNativeRng=final_rng,
                              uiStagesAfter=list(struct.unpack('<2i',w.u.mem_read(ui+0x1fcc8,8)))))
        if left==right and side==1 and seed==0xffffffff and fury==4:
            print(json.dumps(dict(personality=[left,right],cases=len(cases))),flush=True)
    # Synthetic actors are deliberately reinitialized above; prove callback
    # source world purity separately from those fixture writes by per-case
    # model checks. This is not a campaign/world purity assertion.
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,
                functions=[dict(start=hex(a),endExclusive=hex(b),sha256=sha(exe[a-0x400000:b-0x400000]))for a,b in FUNCTIONS.items()],
                cases=cases,limits=['Synthetic model actors/queue storage; original empty message context',
                'Twelve original queue callbacks executed completely; RNG writes and percent call sites observed only',
                'Only original queue type8 numeric hand removal executed before graphics/audio boundary',
                'Other queue rendering/scheduling and complete headed GUI/campaign settlement excluded'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps(dict(cases=len(cases),sha256=sha(output.read_bytes()),completeHeadedFlow=False)),flush=True)
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();inspect(a.installation,a.output)
