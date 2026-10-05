#!/usr/bin/env python3
"""Complete native music selector on actual readonly fixture registries.

All music conditions/getters execute; only audio backend is captured.
Not normal Android scene binding or imported scenario-start evidence.
"""
import argparse,hashlib,itertools,json,struct,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'content'))
from inspect_pc_scenario_tail import NativeTailDecoder
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP
EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'

def inspect(installation,output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside readonly source required')
    exe=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(exe).hexdigest()!=EXE_SHA:raise ValueError('Original executable changed')
    d=NativeTailDecoder(exe);d.decode_tail((installation/'Media/scenario/Scenario.s11').read_bytes(),True);u=d.u
    u.mem_map(0x6fb0000,0x100000);u.mem_map(0x9500000,0x1000000);dispatch=[]
    def backend(m,a,size,user):
        sp=m.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',m.mem_read(sp,4))[0];dispatch.append(list(struct.unpack('<4I',m.mem_read(sp+4,16))));m.reg_write(UC_X86_REG_ESP,sp+20);m.reg_write(UC_X86_REG_EIP,ret)
    u.hook_add(UC_HOOK_CODE,backend,begin=0x4d1900,end=0x4d1900)
    def call(address,receiver,*args,eax=None):
        u.reg_write(UC_X86_REG_ECX,receiver);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<'+'I'*(len(args)+1),d.stop,*args))
        if eax is not None:u.reg_write(UC_X86_REG_EAX,eax)
        u.emu_start(address,d.stop,count=1000000)
        if u.reg_read(UC_X86_REG_EIP)!=d.stop:raise ValueError('Complete original selector/getter did not return')
        return u.reg_read(UC_X86_REG_EAX)
    forces=[d.root+0x7af8+i*0x12c for i in range(3)]
    for i,f in enumerate(forces):
        for p,n in [(f+4,i),(d.root+0xb20c+i*0x50+4,i),(d.root+0xc0bc+i*0x190+0x94,i),(d.root+0xc0bc+i*0x190+0xa0,0)]:u.mem_write(p,struct.pack('<i',n))
    table=bytes(u.mem_read(0x79c2b0,128));regions={c:next(i for i,n in enumerate(table) if n==c) for c in [0,1]};rows=[]
    for own_cities,city_owner,self_troops,other_troops,relation_bit,truce,region,season in itertools.product([0,9,10,42],[0,1],[0,9999,10000,10001],[0,1,30000,65535],[0,1],[0,1],[0,1],[0,1,2,3]):
        for f in forces:u.mem_write(f+0x50,bytes(8));u.mem_write(f+0x64,bytes(47))
        u.mem_write(forces[0]+0x50,struct.pack('<Q',relation_bit<<1));u.mem_write(forces[0]+0x64+1,bytes([truce]))
        # Exact count achieved without assigning city0 twice: other cities fill
        # the requested fixture count, preserving this case's region ownership.
        desired=own_cities;u.mem_write(d.root+0x1d8+0x38,struct.pack('<i',city_owner));remaining=max(0,desired-int(city_owner==0))
        for i in range(1,42):u.mem_write(d.root+0x1d8+i*0x248+0x38,struct.pack('<i',0 if i<=remaining else 2))
        actual_count=sum(call(0x47b2b0,d.root+0x1d8+i*0x248)==0 for i in range(42))
        for i,(owner,n) in enumerate([(0,self_troops),(1,other_troops)]):
            unit=d.root+0x169730+i*0xf4;x,y=10+i,10;u.mem_write(unit+0xc,struct.pack('<i',owner));u.mem_write(unit+0x18,struct.pack('<H',n));u.mem_write(unit+0x3c,struct.pack('<hh',x,y));u.mem_write(0x6fb0e6c+20*(x*200+y),struct.pack('<I',regions[region if i else 0]<<5))
            node=d.stream+0x100+i*0x10;payload=d.stream+0x300+i*0x10;u.mem_write(payload+8,struct.pack('<H',i));u.mem_write(node,struct.pack('<III',payload,0,d.stream+0x110 if i==0 else 0))
        u.mem_write(0x9552710,struct.pack('<I',d.stream+0x100));u.mem_write(0x96a61f0,struct.pack('<i',season))
        before=bytes(u.mem_read(0x7200000,0x300000));grid=bytes(u.mem_read(0x6fb0000,0x100000));external=bytes(u.mem_read(0x9500000,0x1000000));rng=bytes(u.mem_read(0x8a5d44,4))
        p0=call(0x587f00,d.root,eax=forces[0]);p1=call(0x587d70,d.root,eax=forces[0]);p2=call(0x587fb0,d.root,forces[0]);large=call(0x49d670,d.root,forces[0])
        if large!=int(actual_count>=10):raise ValueError('Composed native city count differs')
        dispatch.clear();call(0x5880e0,d.stream+0x800,forces[0]);expected=9 if p0 else (10 if large else 8) if p1 else (11 if large else 7) if p2 else 3+season
        if dispatch!=[[expected,1,500,0xbf800000]]:raise ValueError('Original complete music priority/dispatch differs')
        if before!=bytes(u.mem_read(0x7200000,0x300000)) or grid!=bytes(u.mem_read(0x6fb0000,0x100000)) or external!=bytes(u.mem_read(0x9500000,0x1000000)) or rng!=bytes(u.mem_read(0x8a5d44,4)):raise ValueError('Original composed selector mutated state')
        rows.append(dict(actualOwnedCities=actual_count,city0Owner=city_owner,selfTroops=self_troops,otherTroops=other_troops,queryRelationBit=relation_bit,queryTruceCounter=truce,otherRegion=region,seasonRaw=season,predicates=[p0,p1,p2],largeForceRaw=large,musicId=expected,dispatch=list(dispatch)))
    report=dict(sourceExecutableSha256=EXE_SHA,nativeChecks=len(rows),rows=rows,sourcePolicy='READ_ONLY_NO_WINE_NO_RULE_COMMANDS_OR_RNG',
        evidence='Full5880e0 + original587f00/587d70/587fb0/49d670 and their nested getters/conditions; only4d1900 audio backend observed',
        limits=['Explicit initialized fixture actor/territory/relation states; not normal scene-start or Android projection proof.','Original shared building defaults retained; damaged-building override cases are separately covered by batch16.','Project source identity/territory/calendar and ordinary scene binding still require committed fact integration.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print('PASS complete original music selector checks=',len(rows))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
