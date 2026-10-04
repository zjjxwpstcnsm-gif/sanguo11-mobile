#!/usr/bin/env python3
"""Execute original music city-ratio branches with explicit readonly boundaries.

Not full threat reconstruction: building overrides and aggregation are named
inputs; source thresholds, city ownership gates and branch instructions run.
"""
import argparse,hashlib,itertools,json,struct
from pathlib import Path
from unicorn import Uc,UC_ARCH_X86,UC_MODE_32,UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP

EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'

def inspect(installation,output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside readonly source required')
    raw=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(raw).hexdigest()!=EXE_SHA:raise ValueError('Original executable changed')
    u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(0x400000,0x4a0000);u.mem_write(0x400000,raw[:0x4a0000])
    u.mem_map(0x8a0000,0x10000);u.mem_write(0x8a5d44,struct.pack('<I',0x12345678))
    data,stack,stop=0x10000000,0x20000000,0x30000000;u.mem_map(data,0x10000);u.mem_map(stack,0x3000);u.mem_map(stop,0x2000)
    faction=data+0x8000;table=data+0x9000;owner_method=stop+0x1000;u.mem_write(table+0x40,struct.pack('<I',owner_method))
    for i in range(42):u.mem_write(data+i*0x100,struct.pack('<I',table))
    state={};visited=[];aggregated=[]
    def boundary(m,address,size,user):
        sp=m.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',m.mem_read(sp,4))[0];consumed=0
        if address in [0x587e40,0x587ca0]:result=state['override']
        elif address==0x490a10:
            consumed=4;index=struct.unpack('<I',m.mem_read(sp+4,4))[0]
            if not 0<=index<42:raise ValueError('Original registry range differs')
            visited.append(index);result=data+index*0x100
        elif address==0x47a630:
            pointer=struct.unpack('<I',m.mem_read(sp+4,4))[0];result=int(pointer==data and state['valid'])
        elif address==0x491270:consumed=4;result=0
        elif address==owner_method:result=state['owner']
        elif address==0x4b5cc0:consumed=8;result=state['relationRaw']
        elif address==0x587a00:
            city,p0,p1,p2=struct.unpack('<4I',m.mem_read(sp+4,16));aggregated.append(city)
            for p,n in zip([p0,p1,p2],[state['selfSum'],state['bitSum'],state['otherSum']]):m.mem_write(p,struct.pack('<I',n))
            result=0 # Original caller does not use EAX; cdecl caller pops16.
        else:raise ValueError('Unexamined readonly boundary')
        m.reg_write(UC_X86_REG_EAX,result);m.reg_write(UC_X86_REG_ESP,sp+4+consumed);m.reg_write(UC_X86_REG_EIP,ret)
    for a in [0x587e40,0x587ca0,0x490a10,0x47a630,0x491270,owner_method,0x4b5cc0,0x587a00]:u.hook_add(UC_HOOK_CODE,boundary,begin=a,end=a)
    cases=[]
    for valid,owner,relation,self_sum,bit_sum in itertools.product([0,1],[0,1],[0,1],[0,1,9999,10000,10001,20000],[0,1,3000]):
        pivot=3*(self_sum+bit_sum)
        for other_sum in sorted({0,max(0,pivot-1),pivot,pivot+1}):
            for override in [0,1]:
                state.update(valid=valid,owner=owner,relationRaw=relation,selfSum=self_sum,bitSum=bit_sum,otherSum=other_sum,override=override)
                results=[]
                for address in [0x587f00,0x587d70]:
                    visited.clear();aggregated.clear();before=bytes(u.mem_read(data,0x10000));rng=bytes(u.mem_read(0x8a5d44,4))
                    u.reg_write(UC_X86_REG_EAX,faction);u.reg_write(UC_X86_REG_ESP,stack+0x2000);u.mem_write(stack+0x2000,struct.pack('<I',stop));u.emu_start(address,stop,count=10000)
                    value=u.reg_read(UC_X86_REG_EAX)
                    expected=int(bool(override) or bool(valid) and (owner==0 if address==0x587f00 else owner==0 or bool(relation)) and
                                 (self_sum<10000 and 3*(self_sum+bit_sum)<other_sum if address==0x587f00 else self_sum>10000 and 3*(self_sum+bit_sum)>other_sum))
                    if u.reg_read(UC_X86_REG_EIP)!=stop or value!=expected or before!=bytes(u.mem_read(data,0x10000)) or rng!=bytes(u.mem_read(0x8a5d44,4)):raise ValueError(('Original music ratio differs',hex(address),dict(state),value,expected))
                    if override and visited:raise ValueError('Original override did not short circuit')
                    results.append(dict(address=hex(address),result=value,visitedCities=list(visited),aggregateCalls=len(aggregated)))
                cases.append(dict(state,results=results))
    report=dict(sourceExecutableSha256=EXE_SHA,nativeChecks=len(cases)*2,cases=cases,
        codeSha256={hex(a):hashlib.sha256(raw[a-0x400000:a-0x400000+n]).hexdigest() for a,n in [(0x587f00,163),(0x587d70,192)]},
        evidence=['587f00 own valid city: selfSum<10000 and3*(selfSum+bitSum)<otherSum','587d70 own or directional4b5cc0 city: selfSum>10000 and3*(selfSum+bitSum)>otherSum','Both ratios are strict; selfSum10000 never satisfies either city-ratio branch','Building override takes priority and short-circuits city iteration'],
        limits=['587a00 sums and587e40/587ca0 building predicates are explicit readonly boundaries, not full normal context proof.','Relation inputs retain native direction; no inferred ally/enemy names or Android rule computation.','No normal BGM binding or playback claim; PC installation remains readonly.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print('PASS original music city-ratio predicate checks=',len(cases)*2)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
