#!/usr/bin/env python3
"""Execute original49d670 count predicate with named readonly object boundaries.

Object/scene meanings remain unknown. No rule commands/RNG, Wine or source writes.
"""
import argparse, hashlib, json, struct
from pathlib import Path
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_ESP, UC_X86_REG_EIP

EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'

def inspect(installation, output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside readonly source required')
    raw=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Original executable changed')
    u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(0x400000,0x4a0000);u.mem_write(0x400000,raw[:0x4a0000])
    objects,stack,stop,owner_method,faction=0x10000000,0x20000000,0x30000000,0x30001000,0x40002000
    u.mem_map(objects,0x5000);u.mem_map(stack,0x2000);u.mem_map(stop,0x2000)
    table=objects+0x4000;u.mem_write(table+0x40,struct.pack('<I',owner_method))
    for index in range(42):u.mem_write(objects+index*0x100,struct.pack('<I',table))
    state={};trace=[]
    def boundary(machine,address,size,user):
        sp=machine.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',machine.mem_read(sp,4))[0]
        if address==0x47a630:consumed=0;result=state['valid']
        elif address==0x490a10:
            consumed=4;index=struct.unpack('<I',machine.mem_read(sp+4,4))[0]
            if not 0<=index<42:raise ValueError('Original object range differs')
            trace.append(index);result=objects+index*0x100
        elif address==0x491270:consumed=4;result=faction
        elif address==owner_method:
            consumed=0;index=(machine.reg_read(UC_X86_REG_ECX)-objects)//0x100
            result=faction if index<state['owned'] else faction+0x100
        else:raise ValueError('Unexamined boundary')
        machine.reg_write(UC_X86_REG_EAX,result);machine.reg_write(UC_X86_REG_ESP,sp+4+consumed);machine.reg_write(UC_X86_REG_EIP,ret)
    for address in [0x47a630,0x490a10,0x491270,owner_method]:u.hook_add(UC_HOOK_CODE,boundary,begin=address,end=address)
    rows=[]
    for valid in [0,1]:
        for owned in range(43):
            state.update(valid=valid,owned=owned);trace.clear();sp=stack+0x1000
            u.mem_write(sp,struct.pack('<2I',stop,faction));u.reg_write(UC_X86_REG_ESP,sp);u.emu_start(0x49d670,stop,count=5000)
            actual=u.reg_read(UC_X86_REG_EAX);expected=int(bool(valid) and owned>=10)
            if u.reg_read(UC_X86_REG_EIP)!=stop or actual!=expected or trace!=(list(range(42)) if valid else []):raise ValueError('Original predicate differs')
            rows.append(dict(factionValidRaw=valid,matchingObjectsRaw=owned,result=actual,visitedObjectIds=list(trace)))
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(dict(sourceExecutableSha256=EXE_SHA,address='49d670',checks=len(rows),status='ORIGINAL_CODE_EXECUTED_READ_ONLY_BOUNDARIES',
        codeSha256=hashlib.sha256(raw[0x9d670:0x9d6d2]).hexdigest(),objectCount=42,threshold=10,
        limits=['Objects returned by490a10 and virtual+40 ownership identities are explicit boundaries.',
                'Object class, faction identity mapping and normal scene binding still require proof; not labelled city count or connected to gameplay.'],rows=rows),indent=2)+'\n')
    print('PASS original music raw ownership-count predicate checks=',len(rows))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
