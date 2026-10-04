#!/usr/bin/env python3
"""Capture original presentation record arguments and their unchanged voice fields.

Enter after the upstream call4721d0 with an explicit raw result. Never execute
that call or rule/RNG code. Do not equate the thirteen voice-selector indices
with mobile War/Army tactic enum ordinals.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_EDX, UC_X86_REG_ESI, UC_X86_REG_EBP, UC_X86_REG_ESP, UC_X86_REG_EIP
from inspect_pc_voice_policy import EXE_SHA


def inspect(installation,output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside original source required')
    raw=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Changed executable')
    pe=struct.unpack_from('<I',raw,60)[0];sections=[]
    for i in range(struct.unpack_from('<H',raw,pe+6)[0]):
        at=pe+24+struct.unpack_from('<H',raw,pe+20)[0]+40*i
        _,va,size,offset=struct.unpack_from('<4I',raw,at+8);sections.append((0x400000+va,size,offset))
    def read(a,n):
        for start,size,offset in sections:
            if start<=a and a+n<=start+size:return raw[offset+a-start:offset+a-start+n]
        raise ValueError('Unmapped original range')
    u=Uc(UC_ARCH_X86,UC_MODE_32)
    u.mem_map(0x4fe000,8192);u.mem_write(0x4fe000,read(0x4fe000,8192))
    u.mem_map(0x503000,4096);u.mem_write(0x503000,read(0x503000,4096))
    stack,record,context=0x20000000,0x10000000,0x10003000
    u.mem_map(stack,0x4000);u.mem_map(record,0x5000);u.mem_map(0x471000,4096)
    captured=[]
    def boundary(machine,address,size,user):
        if address==0x4fe420:
            sp=machine.reg_read(UC_X86_REG_ESP);captured.append(list(struct.unpack('<30I',machine.mem_read(sp+4,120))));machine.emu_stop()
        else:
            sp=machine.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',machine.mem_read(sp,4))[0]
            # The copied vector does not select a voice; retain an explicit zero vector.
            machine.mem_write(machine.reg_read(UC_X86_REG_ECX),bytes(16))
            machine.reg_write(UC_X86_REG_ESP,sp+8);machine.reg_write(UC_X86_REG_EIP,ret)
    u.hook_add(UC_HOOK_CODE,boundary,begin=0x4fe420,end=0x4fe420)
    u.hook_add(UC_HOOK_CODE,boundary,begin=0x4719f0,end=0x4719f0)
    rows=[]
    for upstream_raw in (0,1,2):
        captured.clear();sp=stack+4096
        u.mem_write(stack,b''.join(struct.pack('<I',0x60000000+i) for i in range(4096)))
        for register,value in ((UC_X86_REG_ESP,sp),(UC_X86_REG_EAX,upstream_raw),(UC_X86_REG_ECX,context),(UC_X86_REG_EDX,0x1234),(UC_X86_REG_ESI,context),(UC_X86_REG_EBP,0)):
            u.reg_write(register,value)
        u.emu_start(0x5038e0,0x503945,count=100)
        if len(captured)!=1 or u.reg_read(UC_X86_REG_EIP)!=0x4fe420:raise ValueError('Original record call capture failed')
        args=captured[0]
        if args[9]!=(12 if upstream_raw==0 else 11):raise ValueError('Original selector argument branch differs')
        p=stack+8192;u.mem_write(p+0x18,struct.pack('<30I',*args));u.reg_write(UC_X86_REG_ESP,p);u.reg_write(UC_X86_REG_ESI,record)
        u.emu_start(0x4fe455,0x4fe4f7,count=100)
        if u.reg_read(UC_X86_REG_EIP)!=0x4fe4f7:raise ValueError('Original record field writes did not finish')
        side,slot,index=struct.unpack('<I',u.mem_read(record,4))[0],struct.unpack('<I',u.mem_read(record+4,4))[0],struct.unpack('<I',u.mem_read(record+0x98,4))[0]
        if (side,slot,index)!=(args[0],args[1],args[9]):raise ValueError('Original speaker/selector field projection differs')
        rows.append(dict(upstreamCall4721d0ResultRaw=upstream_raw,recordConstructorArgs=args,field0SideRaw=side,field4OfficerSlotRaw=slot,field98VoiceSelectorRaw=index,
                         sourceConstructorArgumentPositions=dict(side=1,officerSlot=2,voiceSelector=10)))
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,nativeChecks=6,rows=rows,
                path='5038e0..503940 ->4fe420 arguments ->4fe455..4fe4f7 ->record+0/+4/+0x98 ->503b19..503b32 ->4fd630',
                codeSha256={hex(a):hashlib.sha256(read(a,b-a)).hexdigest() for a,b in [(0x5038d8,0x503945),(0x4fe455,0x4fe4f7),(0x503b19,0x503b37)]},
                established=['Constructor argument1/2/10 becomes raw speaker side/slot/voice-selector index.',
                             'Original5038e0 branch supplies voice-selector12 for raw0 and11 for rawnonzero, independently of a tactic enum ordinal.',
                             'Legacy table/probe label tacticIndexRaw means an unlabelled13-way speech selector, not proven War/Army tacticIndex.'],
                shims=['Upstream4721d0 is not entered; explicit already-produced raw result0/1/2.',
                       '4fe420 first boundary captures its30 original arguments; allocation/list insertion not entered.',
                       '4719f0 unrelated vector copy zero-valued; all voice field stores execute unchanged.'],
                limits=['Constructed stack and partial original presentation field writes, not a PC battle or upstream rule/RNG evaluation.',
                        'Semantic meaning of4721d0 result and13 speech-selector labels still pending; no critical/success/tactic inference.',
                        'Mobile maps require committed speaker/action/outcome facts, not ordinal mapping from this constructor.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(dict(result='PASS',nativeChecks=6,voiceSelectorValues=[r['field98VoiceSelectorRaw'] for r in rows])))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();inspect(a.installation,a.output)
