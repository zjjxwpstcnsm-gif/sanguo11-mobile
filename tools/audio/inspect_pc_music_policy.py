#!/usr/bin/env python3
"""Execute original music selector and switch policy; preserve unresolved predicate/scene meaning.

Only named readonly predicate results and audio backend boundaries are shimmed.
No rule command, rule RNG, Wine or writes to the supplied installation.
"""
import argparse
import hashlib
import itertools
import json
from pathlib import Path
import struct
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP

EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'
def inspect(installation,output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside readonly source required')
    raw=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Source executable changed')
    pe=struct.unpack_from('<I',raw,60)[0];base=struct.unpack_from('<I',raw,pe+52)[0]
    count=struct.unpack_from('<H',raw,pe+6)[0];optional=struct.unpack_from('<H',raw,pe+20)[0];sections=[]
    for index in range(count):
        at=pe+24+optional+40*index;virtual,address,size,offset=struct.unpack_from('<IIII',raw,at+8)
        sections.append((base+address,size,offset))
    def read(address,size):
        for start,length,offset in sections:
            if start<=address and address+size<=start+length:return raw[offset+address-start:offset+address-start+size]
        raise ValueError('Unmapped original PE range')
    u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(0x400000,0x4a0000);u.mem_write(0x400000,raw[:0x4a0000])
    u.mem_map(0x9600000,0xb0000);manager,stack,stop=0x10000000,0x20000000,0x30000000
    u.mem_map(manager,0x4000);u.mem_map(stack,0x2000);u.mem_map(stop,0x1000)
    values={};calls=[]
    def returned(machine,address,size,user):
        sp=machine.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',machine.mem_read(sp,4))[0]
        if address==0x47a630:consumed=0;result=values[address]
        elif address==0x49d670:consumed=4;result=values[address]
        elif address in [0x587f00,0x587d70]:consumed=0;result=values[address]
        elif address==0x587fb0:consumed=0;result=values[address]
        elif address==0x4d1900:
            args=struct.unpack('<4I',machine.mem_read(sp+4,16));calls.append(dict(address=hex(address),args=list(args)));consumed=16;result=1
        elif address==0x4cf9b0:
            args=struct.unpack('<4I',machine.mem_read(sp+4,16));calls.append(dict(address=hex(address),args=list(args)));consumed=16;result=1
            machine.mem_write(machine.reg_read(UC_X86_REG_ECX)+0x28,struct.pack('<I',args[0]))
        elif address==0x4cfa50:consumed=4;calls.append(dict(address=hex(address),args=[struct.unpack('<I',machine.mem_read(sp+4,4))[0]]));result=1
        elif address==0x6e9250:consumed=4;calls.append(dict(address=hex(address),args=[struct.unpack('<I',machine.mem_read(sp+4,4))[0]]));result=0
        elif address==0x6e9270:consumed=8;calls.append(dict(address=hex(address),args=list(struct.unpack('<2I',machine.mem_read(sp+4,8)))));result=0
        else:raise ValueError('Unexamined shim')
        machine.reg_write(UC_X86_REG_EAX,result);machine.reg_write(UC_X86_REG_ESP,sp+4+consumed);machine.reg_write(UC_X86_REG_EIP,ret)
    addresses=[0x47a630,0x49d670,0x587f00,0x587d70,0x587fb0,0x4d1900,0x4cf9b0,0x4cfa50,0x6e9250,0x6e9270]
    for address in addresses:u.hook_add(UC_HOOK_CODE,returned,begin=address,end=address)
    def call(address,args):
        calls.clear();sp=stack+4096;u.mem_write(sp,struct.pack('<'+'I'*(len(args)+1),stop,*[x&0xffffffff for x in args]))
        u.reg_write(UC_X86_REG_ESP,sp);u.reg_write(UC_X86_REG_ECX,manager);u.emu_start(address,stop,count=20000)
        if u.reg_read(UC_X86_REG_EIP)!=stop:raise ValueError('Native execution did not return')
        return u.reg_read(UC_X86_REG_EAX),list(calls)
    rows=[];checks=0
    for valid,p0,p1,p2,p3,season in itertools.product([0,1],[0,1],[0,1],[0,1],[0,1],[-1,0,1,2,3,4]):
        values.update({0x47a630:valid,0x587f00:p0,0x587d70:p1,0x587fb0:p2,0x49d670:p3})
        u.mem_write(0x96a61f0,struct.pack('<i',season));_,dispatch=call(0x5880e0,[manager+0x3000])
        expected=9 if p0 else (10 if p3 else 8) if p1 else (11 if p3 else 7) if p2 else 3+season if 0<=season<=3 else 7
        if valid:
            if dispatch!=[dict(address='0x4d1900',args=[expected,1,500,0xbf800000])]:raise ValueError('Original branch/gain/fade differs')
        elif dispatch:raise ValueError('Invalid source faction dispatched music')
        rows.append(dict(factionValidRaw=valid,predicate587f00=p0,predicate587d70=p1,predicate587fb0=p2,predicate49d670=p3,seasonRaw=season,
            musicId=expected if valid else None,dispatch=dispatch,status='ORIGINAL_SELECTOR_EXECUTED_PREDICATE_MEANINGS_PENDING'));checks+=1
    switches=[]
    for previous,next_id,a,b in itertools.product([-1,3,12],[-2,-1,0,3,12,29,30],[0,1,500],[0,1]):
        u.mem_write(manager+0x20,struct.pack('<I',manager+0x2000));u.mem_write(manager+0x28,struct.pack('<i',previous))
        result,dispatch=call(0x4cfc20,[next_id,1,0x3f800000,a,b]);valid=0<=next_id<30
        expected=[dict(address='0x6e9250',args=[500])] if not valid else [dict(address='0x6e9270',args=[0x3f800000,500])] if previous==next_id else (
            ([] if a and b else [dict(address='0x4cfa50',args=[500])])+[dict(address='0x4cf9b0',args=[next_id,1,0x3f800000,a])])
        if dispatch!=expected or result!=(1 if valid or next_id==-1 else 0):raise ValueError(('Original switch differs',previous,next_id,a,b,dispatch,expected,result))
        switches.append(dict(previous=previous,next=next_id,fadeMillisecondsRaw=a,argument5ModeRaw=b,result=result,dispatch=dispatch));checks+=1
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourcePolicy='READ_ONLY_NO_WINE_NO_RULE_RNG',nativeChecks=checks,
        selector=dict(address='5880e0',codeSha256=hashlib.sha256(read(0x5880e0,0xc4)).hexdigest(),
            jumpTable=list(struct.unpack('<4I',read(0x5881a0,16))),rows=rows),
        switchPolicy=dict(address='4cfc20',codeSha256=hashlib.sha256(read(0x4cfc20,0xae)).hexdigest(),rows=switches),
        sourceCallerEvidence=[dict(call='57fc89',condition='source faction virtual+48 nonzero'),dict(call='57fd67',condition='source faction virtual+48 nonzero')],
        limits=['Predicate return values are explicit shims, not reconstructed gameplay conditions.',
            'Season raw0..3 maps to music3..6; season labels/month derivation and normal map scene identity still require proof.',
            'Selector emits default volume sentinel-1, repeat1 and500ms fade; backend captured rather than played.',
            'Unresolved scene call6907a4 maps source flag9c57074==1 to16 and other values to12; scene role not proven, do not label it menu or choose a mobile track from this alone.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print('PASS original music selector/switch checks=',checks)
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();inspect(a.installation,a.output)
