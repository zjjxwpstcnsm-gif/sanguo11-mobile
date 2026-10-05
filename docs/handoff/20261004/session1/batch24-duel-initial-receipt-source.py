#!/usr/bin/env python3
"""Execute the original full duel initializer/frame with explicit input fixtures.

The six actor input slots and AI/controller options are verification fixtures.
Normal event admission, presentation and campaign settlement remain separate.
No duel, AI, damage, phase or RNG function is replaced.
"""
import argparse,gzip,json,struct
from collections import deque
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EIP,UC_X86_REG_ESP
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from audit_pc_restoration_sources import EXE_SHA,output_guard,json_bytes,sha


def inspect(installation,output,frame_limit=2000):
    installation=installation.resolve();output_guard(installation,output)
    exe=(installation/'san11pk.exe').read_bytes();d=NativeDebateFlow(installation,exe);w=d.world
    print('Original world/platform constructed',flush=True)
    pages=load_original_data(w.u)
    shared=(installation/'Media/scenario/Scenario.s11').read_bytes();source_path='Media/scenario/Scen000.s11';raw=(installation/source_path).read_bytes()
    w.load(shared,True);print('Original shared serializer complete',flush=True)
    loaded=w.load(raw);print('Original scenario serializer complete',flush=True)
    native_ids=[116,222];actors=[w.root+0xc0bc+i*0x190 for i in native_ids]
    rows={x['native_index']:x for x in loaded['records']if x['kind']=='officer'}
    people=[]
    for native,actor in zip(native_ids,actors):
        if w.call(0x4883c0,receiver=actor)!=native:raise ValueError('Original source native identity differs')
        people.append(dict(nativeId=native,recordSha256=rows[native]['sha256'],actorHex=bytes(w.u.mem_read(actor,0x190)).hex(),originalValid=bool(w.call(0x47a600,actor))))
    input_pointer=d.fixture+0x1000
    w.call(0x50ddd0,receiver=input_pointer)
    w.u.mem_write(input_pointer,struct.pack('<6I',actors[0],0,0,actors[1],0,0))
    # Explicit two single-actor sides/styles0/AI flags1. The complete original
    # input admission copies actual source HP128 and injury15c into the manager.
    # Constructor placeholders-1 are not valid original fighter injuries.
    w.u.mem_write(input_pointer+0x20,struct.pack('<8i',0,0,0,0,2,0,1,1))
    input_hex=bytes(w.u.mem_read(input_pointer,0xd0)).hex()
    w.call(0x50ddd0,receiver=0x8b3740)
    w.u.mem_write(0x8b3740+0xcc,struct.pack('<I',1)) # explicit pending-input gate
    if w.call(0x50de30,input_pointer,receiver=0x8b3740,count=10000000)!=1:raise ValueError('Original input admission rejected')
    print('Complete original input admission complete',flush=True)
    admitted_hex=bytes(w.u.mem_read(0x8b3740,0xd0)).hex()
    w.call(0x50ab90,receiver=d.fixture);w.u.mem_write(0x8a5d44,struct.pack('<I',23))
    w.call(0x50c030,receiver=d.fixture,count=10000000)
    print('Complete original50c030 initialization complete',flush=True)
    initial=bytes(w.u.mem_read(d.fixture,0x59c));before=bytes(w.u.mem_read(0x7200000,0x300000));trace=[];tail=deque(maxlen=64)
    def track(u,address,size,user):tail.append(hex(address))
    handle=w.u.hook_add(UC_HOOK_CODE,track,begin=0x500000,end=0x51ffff)
    failure=None;terminal=False
    for frame in range(frame_limit):
        try:return_value=w.call(0x505e60,1,receiver=d.fixture,count=10000000)
        except Exception as error:
            ip=w.u.reg_read(UC_X86_REG_EIP);sp=w.u.reg_read(UC_X86_REG_ESP)
            failure=dict(frame=frame,error=repr(error),nativeIp=hex(ip),nativeSp=hex(sp),recentInstructions=list(tail),invalidMemory=w.invalid,stackHex=bytes(w.u.mem_read(sp,64)).hex())
            print(json.dumps(failure),flush=True);break
        state=bytes(w.u.mem_read(d.fixture,0x59c));head=struct.unpack_from('<5i',state,4);seed=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
        trace.append(dict(frame=frame,phase=head[0],pendingPhase=head[1],sub=head[2],header=list(head),seed=seed,returnValue=return_value,stateHex=state.hex()))
        if frame<8 or frame%100==0:print(json.dumps(dict(frame=frame,head=head,seed=seed)),flush=True)
        if head[0]==12 and return_value==1:terminal=True;break
    w.u.hook_del(handle)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sharedSha256=sha(shared),sourcePath=source_path,sourceSha256=sha(raw),originalPages=pages,people=people,
        explicitInputHex=input_hex,admittedInputHex=admitted_hex,inputAdmission='50de30 ->50d930/50d030/50d7a0 and actual source HP/injury',initializer='50ab90 ->50c030 ->50bd20',frame='505e60 original phase vtable dispatch',initialStateHex=initial.hex(),trace=trace,
        failure=failure,terminal=terminal,managerFinalHex=bytes(w.u.mem_read(0x8b3740,0xd0)).hex(),sourceWorldBeforeSha256=sha(before),sourceWorldAfterSha256=sha(bytes(w.u.mem_read(0x7200000,0x300000))),
        limits=['Actual source native116/222 at layered serializer boundary, not complete opening events',
                'Six manager slots/styles/controllers are explicit verification fixtures',
                'Original complete model executes; optional UI/controller context remains PE defaults',
                'No gameplay callback, damage, phase or RNG replacement; campaign admission/settlement not certified'],completeGoal=False)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0));print(json.dumps(dict(frames=len(trace),terminal=terminal,failure=failure,sha256=sha(output.read_bytes()))),flush=True)
    if not terminal:raise ValueError('Original full duel not yet terminal; failure retained')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--frame-limit',type=int,default=2000);a=p.parse_args();inspect(a.installation,a.output,a.frame_limit)
