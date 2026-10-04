#!/usr/bin/env python3
"""Execute original587fb0 using real registries/territory/diplomacy, no behavior shims."""
import argparse,hashlib,itertools,json,struct,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'content'))
from inspect_pc_scenario_tail import NativeTailDecoder
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP,UC_X86_REG_EAX
EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'

def inspect(installation,output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside readonly source required')
    exe=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(exe).hexdigest()!=EXE_SHA:raise ValueError('Original executable changed')
    d=NativeTailDecoder(exe);d.decode_tail((installation/'Media/scenario/Scenario.s11').read_bytes(),True);u=d.u
    u.mem_map(0x6fb0000,0x100000);u.mem_map(0x9500000,0x1000000)
    def call(address,receiver,*args):
        u.reg_write(UC_X86_REG_ECX,receiver);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<'+'I'*(len(args)+1),d.stop,*args));u.emu_start(address,d.stop,count=1000000)
        if u.reg_read(UC_X86_REG_EIP)!=d.stop:raise ValueError('Original encounter did not return')
        return u.reg_read(UC_X86_REG_EAX)
    forces=[d.root+0x7af8+i*0x12c for i in range(4)]
    for i,f in enumerate(forces):
        for p,n in [(f+4,i),(d.root+0xb20c+i*0x50+4,i),(d.root+0xc0bc+i*0x190+0x94,i),(d.root+0xc0bc+i*0x190+0xa0,0)]:u.mem_write(p,struct.pack('<i',n))
    table=bytes(u.mem_read(0x79c2b0,128));regions={c:next(i for i,n in enumerate(table) if n==c) for c in [0,1]}
    rows=[]
    for owner,city_owner,forward_bit,forward_counter,reverse_bit,reverse_counter,extra_owner,extra_region in itertools.product([0,1],[0,1],[0,1],[0,1],[0,1],[0,1],[-1,0,1,2],[0,1]):
        for f in forces:u.mem_write(f+0x50,bytes(8));u.mem_write(f+0x64,bytes(47))
        u.mem_write(forces[0]+0x50,struct.pack('<Q',forward_bit<<1));u.mem_write(forces[0]+0x64+1,bytes([forward_counter]));u.mem_write(forces[1]+0x50,struct.pack('<Q',reverse_bit));u.mem_write(forces[1]+0x64,bytes([reverse_counter]))
        u.mem_write(d.root+0x1d8+0x38,struct.pack('<i',city_owner));u.mem_write(d.root+0x1d8+0x248+0x38,struct.pack('<i',3))
        records=[(owner,0)]+([(extra_owner,extra_region)] if extra_owner>=0 else [])
        for i,(o,region) in enumerate(records):
            unit=d.root+0x169730+i*0xf4;x,y=10+i,10
            u.mem_write(unit+0xc,struct.pack('<i',o));u.mem_write(unit+0x3c,struct.pack('<hh',x,y));u.mem_write(0x6fb0e6c+20*(x*200+y),struct.pack('<I',regions[region]<<5))
            node=d.stream+0x100+i*0x10;payload=d.stream+0x300+i*0x10
            u.mem_write(payload+8,struct.pack('<H',i));u.mem_write(node,struct.pack('<III',payload,0,d.stream+0x100+(i+1)*0x10 if i+1<len(records) else 0))
            if call(0x4955a0,unit)!=o:raise ValueError('Actual unit owner differs')
        u.mem_write(0x9552710,struct.pack('<I',d.stream+0x100))
        def relation(a,b):
            if a==b:return False
            return not (forward_bit or forward_counter) if (a,b)==(0,1) else not (reverse_bit or reverse_counter) if (a,b)==(1,0) else True
        expected=False
        for o,region in records:
            c=city_owner if region==0 else 3
            if o==0:
                other=any(r==region and relation(0,q) for q,r in records)
                expected|=relation(0,c) or (c==1 and bool(forward_bit) and other)
            else:expected|=relation(o,0) and c==0
        before=bytes(u.mem_read(0x7200000,0x300000));grid=bytes(u.mem_read(0x6fb0000,0x100000));rng=bytes(u.mem_read(0x8a5d44,4));external=bytes(u.mem_read(0x9500000,0x1000000))
        actual=call(0x587fb0,d.root,forces[0])
        if actual!=int(expected):raise ValueError(('Original encounter differs',owner,city_owner,forward_bit,forward_counter,reverse_bit,reverse_counter,extra_owner,extra_region,actual,int(expected)))
        if before!=bytes(u.mem_read(0x7200000,0x300000)) or grid!=bytes(u.mem_read(0x6fb0000,0x100000)) or rng!=bytes(u.mem_read(0x8a5d44,4)) or external!=bytes(u.mem_read(0x9500000,0x1000000)):raise ValueError('Original encounter mutated state')
        rows.append(dict(candidateOwner=owner,cityOwner=city_owner,queryToCityBit=forward_bit,queryToCityCounter=forward_counter,unitToQueryBit=reverse_bit,unitToQueryCounter=reverse_counter,extraOwner=extra_owner,extraRegion=extra_region,result=actual))
    u.mem_write(0x9552710,bytes(4));before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4))
    empty=call(0x587fb0,d.root,forces[0])
    if empty!=0 or before!=bytes(u.mem_read(0x7200000,0x300000)) or rng!=bytes(u.mem_read(0x8a5d44,4)):raise ValueError('Original empty encounter differs')
    report=dict(sourceExecutableSha256=EXE_SHA,nativeChecks=len(rows)+1,rows=rows,emptyListResult=empty,
        codeSha256=hashlib.sha256(exe[0x187fb0:0x1880df]).hexdigest(),territoryTableSha256=hashlib.sha256(table).hexdigest(),
        limits=['Explicit fixture actor/district/territory/diplomacy states; no normal Android context or scene binding claim.','No behavior/getter/condition shims; existing original decode IO/import support only.','No normal BGM playback claim, no Wine, PC source readonly.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print('PASS original music encounter predicate checks=',report['nativeChecks'])

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
