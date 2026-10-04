#!/usr/bin/env python3
"""Execute original presentation voice caller chains and retain raw speaker sides.

No original gameplay command, ability calculation, RNG or Wine is entered.
Only memory/actor validity and the final audio dispatch are explicit shims.
"""
import argparse
import gzip
import hashlib
import itertools
import json
from pathlib import Path
import struct
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP
from inspect_pc_voice_policy import EXE_SHA


def inspect(installation,output):
    if output.exists() or installation.resolve() in output.resolve().parents:
        raise ValueError('Fresh output outside read-only source required')
    raw=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(raw).hexdigest()!=EXE_SHA:
        raise ValueError('Changed executable')
    pe=struct.unpack_from('<I',raw,60)[0]
    base=struct.unpack_from('<I',raw,pe+52)[0]
    sections=[]
    for i in range(struct.unpack_from('<H',raw,pe+6)[0]):
        at=pe+24+struct.unpack_from('<H',raw,pe+20)[0]+40*i
        _,va,size,offset=struct.unpack_from('<4I',raw,at+8)
        sections.append((base+va,size,offset))
    def read(address,size):
        for start,length,offset in sections:
            if start<=address and address+size<=start+length:
                return raw[offset+address-start:offset+address-start+size]
        raise ValueError('Unmapped original PE range')
    u=Uc(UC_ARCH_X86,UC_MODE_32)
    spans=[]
    for start,size,offset in sections:
        low,high=start&~4095,(start+size+4095)&~4095
        if spans and low<=spans[-1][1]:spans[-1]=(spans[-1][0],max(high,spans[-1][1]))
        else:spans.append((low,high))
    for low,high in spans:u.mem_map(low,high-low)
    for start,size,offset in sections:u.mem_write(start,raw[offset:offset+size])
    context,domain,actors,stack,stop=0x10000000,0x10001000,0x10002000,0x20000000,0x30000000
    u.mem_map(context,0xc000);u.mem_map(stack,8192);u.mem_map(stop,4096)
    actor_pointers=[actors+1024*i for i in range(8)]
    records={domain+0x24+side*0xec+slot*0x40:actor_pointers[side*4+slot] for side in range(2) for slot in range(4)}
    captured=[];valid_actor=True
    def boundary(machine,address,size,user):
        sp=machine.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',machine.mem_read(sp,4))[0]
        if address==0x472070:
            pointer,length,mode=struct.unpack('<3I',machine.mem_read(sp+4,12))
            if pointer not in records or length!=0x40 or mode!=1:
                raise ValueError('Unexpected original record validation')
            result,consumed=1,0
        elif address==0x47a600:
            pointer=struct.unpack('<I',machine.mem_read(sp+4,4))[0]
            if pointer not in actor_pointers and pointer!=0:
                raise ValueError('Unexpected original speaker identity pointer')
            result,consumed=int(pointer!=0 and valid_actor),0
        elif address==0x6e96a0:
            if machine.reg_read(UC_X86_REG_ECX)!=context+0x300:
                raise ValueError('Unexpected audio availability object')
            result,consumed=1,0
        else:
            if machine.reg_read(UC_X86_REG_ECX)!=0x9119810:
                raise ValueError('Unexpected original global media manager')
            voice,volume=struct.unpack('<2I',machine.mem_read(sp+4,8))
            captured.append(dict(voiceId=voice if voice<0x80000000 else voice-0x100000000,volumeRawHex=hex(volume)))
            result,consumed=1,8
        machine.reg_write(UC_X86_REG_EAX,result)
        machine.reg_write(UC_X86_REG_ESP,sp+4+consumed);machine.reg_write(UC_X86_REG_EIP,ret)
    for address in (0x472070,0x47a600,0x4d1000,0x6e96a0):
        u.hook_add(UC_HOOK_CODE,boundary,begin=address,end=address)
    u.mem_write(0x9119810+0x34,struct.pack('<I',context+0x300))
    def call(address,args):
        captured.clear();sp=stack+4096
        u.mem_write(sp,struct.pack('<'+'I'*(len(args)+1),stop,*[x&0xffffffff for x in args]))
        u.reg_write(UC_X86_REG_ESP,sp);u.reg_write(UC_X86_REG_ECX,context)
        u.emu_start(address,stop,count=2000)
        if u.reg_read(UC_X86_REG_EIP)!=stop or u.reg_read(UC_X86_REG_ESP)!=sp+4+4*len(args):
            raise ValueError('Original caller did not return exactly')
        return list(captured)
    types=[[0,0,-1],[1,0,-1],[2,0,-1],[3,0,-1],[1,1,-1],[2,1,-1],[3,0,12],[1,0,13]]
    bases=list(struct.unpack('<71i',read(0x7f1420,284)))
    table=list(struct.unpack('<16i',read(0x7f13a0,64)))
    tactic_profiles=list(struct.unpack('<13i',read(0x838c94,52)))
    if tactic_profiles!=list(range(13)) or read(0x7f13a0,64)!=read(0x7f13e0,64):
        raise ValueError('Source caller table changed')
    def expected(profile,kind,side):
        a,b,override=types[kind]
        return bases[profile]+(override if override>=0 else table[2*(a+4*b)+side])
    tactic_rows=[]
    u.mem_write(context+4,struct.pack('<I',domain))
    for record,pointer in records.items():u.mem_write(record,struct.pack('<I',pointer))
    for tactic,side,slot,kind in itertools.product(range(13),range(2),range(4),range(8)):
        # Set the selected real record actor; unrelated actors use distinguishable types.
        for i,pointer in enumerate(actor_pointers):u.mem_write(pointer+0x100,struct.pack('<i',(kind+i-side*4-slot)%8))
        dispatch=call(0x4fd630,[side,slot,tactic])
        want=[dict(voiceId=expected(tactic_profiles[tactic],kind,side),volumeRawHex='0xbf800000')]
        if dispatch!=want:raise ValueError('Original tactic caller/speaker/profile dispatch differs')
        tactic_rows.append(dict(tacticIndexRaw=tactic,sideRaw=side,officerSlotRaw=slot,voiceTypeRaw=kind,
                                speakerRecordOffsetHex=hex(0x24+side*0xec+slot*0x40),nativeProfile=tactic_profiles[tactic],dispatch=dispatch))
    boundaries=[]
    for side,slot,tactic in itertools.product((-1,0,1,2),(-1,0,3,4),(-1,0,12,13)):
        dispatch=call(0x4fd630,[side,slot,tactic])
        if bool(dispatch)!=(side in (0,1) and 0<=slot<4 and 0<=tactic<13):
            raise ValueError('Original tactic boundary differs')
        boundaries.append(dict(sideRaw=side,officerSlotRaw=slot,tacticIndexRaw=tactic,dispatch=dispatch))
    for valid_actor in (False,True):
        dispatch=call(0x4fd630,[0,0,0])
        if bool(dispatch)!=valid_actor:raise ValueError('Actor rejection dispatch differs')
        boundaries.append(dict(actorValidRaw=int(valid_actor),dispatch=dispatch))
    generic_rows=[]
    u.mem_write(context+0x10,struct.pack('<I',domain))
    for side in (0,1):u.mem_write(domain+0x10+side*0xa0,struct.pack('<I',actor_pointers[side]))
    for index,side,kind in itertools.product(range(58),range(2),range(8)):
        for i,pointer in enumerate(actor_pointers):u.mem_write(pointer+0x100,struct.pack('<i',(kind+i-side)%8))
        dispatch=call(0x517cd0,[index,side])
        if dispatch!=[dict(voiceId=expected(index+13,kind,side),volumeRawHex='0xbf800000')]:
            raise ValueError(('Original second caller side/profile dispatch differs',index,side,kind,dispatch))
        generic_rows.append(dict(eventIndexRaw=index,sideRaw=side,voiceTypeRaw=kind,nativeProfile=index+13,
                                speakerRecordOffsetHex=hex(0x10+side*0xa0),dispatch=dispatch,status='EVENT_ROLE_UNKNOWN'))
    for index,side in itertools.product((-14,-13,-1,0,57,58),(-1,0,1,2)):
        # The original record getter has no bound. Give outside records a zero pointer,
        # retain the observed rejected side at the original downstream wrapper.
        for bad in (-1,2):u.mem_write(domain+0x10+bad*0xa0,struct.pack('<I',0))
        dispatch=call(0x517cd0,[index,side])
        if bool(dispatch)!=(0<=index+13<71 and side in (0,1)):
            raise ValueError('Second caller original raw profile/side boundary differs')
        boundaries.append(dict(eventIndexRaw=index,sideRaw=side,dispatch=dispatch))
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,policy='ORIGINAL_PRESENTATION_CALLERS_READ_ONLY_NO_RULE_RNG_NO_WINE',
                nativeChecks=len(tactic_rows)+len(generic_rows)+len(boundaries),tacticRows=tactic_rows,genericRows=generic_rows,boundaries=boundaries,
                tacticChain='503b32 ->4fd630 ->505f70 ->50c0d0 ->4d1a40 ->4d13b0 ->4cffd0 ->4d1000',
                secondChain='51d002 ->517cd0 ->51e350 ->4d1aa0 ->4d1490 ->4cffd0 ->4d1000',
                codeSha256={hex(a):hashlib.sha256(read(a,b-a)).hexdigest() for a,b in [(0x4fd630,0x4fd692),(0x505f70,0x505fa2),(0x50c0d0,0x50c118),(0x517cd0,0x517d01),(0x51e350,0x51e361),(0x503b19,0x503b37),(0x51cff5,0x51d007)]},
                established=['4fd630 forwards the same raw side0/1 both to its actual side/slot actor lookup and feedbackA selection.',
                             '4fd630 selects native profile from original13-row tactic table, here identical to raw tactic0..12.',
                             '517cd0 selects a side-specific record actor and profile13+raw event index, forwarding the same side to feedbackB.',
                             'Raw feedback cannot be globally interpreted as success/failure; caller side and actual speaker slot are essential.'],
                shims=['472070 record memory validation=1 for explicit constructed records.',
                       '47a600 explicit constructed actor validity, not original world membership.',
                       '6e96a0 audio backend availability=1 for explicit constructed media object.',
                       '4d1000 final voice/volume capture, no audio backend/decode/playback.'],
                limits=['Constructed original presentation records, not a running PC battle or source scenario.',
                        'Upstream generation of side, officerSlot and tactic/event indices still needs authoritative committed fact correlation.',
                        'Second caller event0..57 names remain unknown; negative raw indices can pass through arithmetic to lower profiles and must not be invented as mobile actions.',
                        'No normal Android voice playback or all-event/speaker acceptance.'])
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_bytes(gzip.compress((json.dumps(report,separators=(',',':'))+'\n').encode(),mtime=0))
    print(json.dumps(dict(result='PASS',nativeChecks=report['nativeChecks'],tacticRows=len(tactic_rows),secondCallerRows=len(generic_rows))))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();inspect(a.installation,a.output)
