#!/usr/bin/env python3
"""Original music regional sums/presence, actual registries/getters, no behavior shims.

Only original decode IO/import boundaries from NativeTailDecoder are used.
Explicit offline fixture data is not a reconstructed normal Android scene.
"""
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
        if u.reg_read(UC_X86_REG_EIP)!=d.stop:raise ValueError('Original music read did not return')
        return u.reg_read(UC_X86_REG_EAX)
    forces=[d.root+0x7af8+i*0x12c for i in range(4)]
    for i,force in enumerate(forces):
        district=d.root+0xb20c+i*0x50;officer=d.root+0xc0bc+i*0x190
        for p,n in [(force+4,i),(district+4,i),(officer+0x94,i),(officer+0xa0,0)]:u.mem_write(p,struct.pack('<i',n))
        u.mem_write(force+0x50,bytes(8));u.mem_write(force+0x64,bytes(47))
    city=d.root+0x1d8;u.mem_write(city+0x38,struct.pack('<i',0))
    table=bytes(u.mem_read(0x79c2b0,128));regions={c:next(i for i,n in enumerate(table) if n==c) for c in [0,1]}
    points=[(10,10),(150,150),(11,10),(12,10),(13,10)];out=d.stream+0x600;rows=[]
    for query_bit,query_counter,reverse_bit,reverse_counter,which_region,troop in itertools.product([0,1],[0,1],[0,1],[0,1],[0,1],[0,1,9999,10000,65535]):
        for force in forces:u.mem_write(force+0x50,bytes(8));u.mem_write(force+0x64,bytes(47))
        # Query force0 relation to force1 and reverse direction are distinct.
        u.mem_write(forces[0]+0x50,struct.pack('<Q',query_bit<<1));u.mem_write(forces[0]+0x64+1,bytes([query_counter]));u.mem_write(forces[1]+0x50,struct.pack('<Q',reverse_bit));u.mem_write(forces[1]+0x64,bytes([reverse_counter]))
        # Force3 is neutral via source truce counter; force2 is other relation.
        u.mem_write(forces[0]+0x64+3,b'\x01');u.mem_write(forces[3]+0x64,b'\x01')
        owners=[0,1,2,3,1];counts=[1234,troop,2345,3456,7]
        for i,((x,y),owner,n) in enumerate(zip(points,owners,counts)):
            unit=d.root+0x169730+i*0xf4;u.mem_write(unit+0xc,struct.pack('<i',owner));u.mem_write(unit+0x18,struct.pack('<H',n));u.mem_write(unit+0x3c,struct.pack('<hh',x,y))
            region=which_region if i in [1,4] else 0;u.mem_write(0x6fb0e6c+20*(x*200+y),struct.pack('<I',regions[region]<<5))
            node=d.stream+0x100+i*0x10;payload=d.stream+0x300+i*0x10
            u.mem_write(payload+8,struct.pack('<H',i));u.mem_write(node,struct.pack('<III',payload,0,d.stream+0x100+(i+1)*0x10 if i<4 else 0))
            if call(0x47a630,d.root,unit)!=1 or call(0x4955a0,unit)!=owner or call(0x496010,unit)!=n:raise ValueError('Actual original unit fixture getter differs')
        u.mem_write(0x9552710,struct.pack('<I',d.stream+0x100))
        before=bytes(u.mem_read(0x7200000,0x300000));grid=bytes(u.mem_read(0x6fb0000,0x100000));rng=bytes(u.mem_read(0x8a5d44,4));external=bytes(u.mem_read(0x9500000,0x1000000))
        call(0x587a00,forces[0],city,out,out+4,out+8);actual=list(struct.unpack('<3I',u.mem_read(out,12)))
        included=troop+7 if which_region==0 else 0;expected=[1234,included if query_bit else 0,2345+(included if not query_bit and not query_counter else 0)]
        same=call(0x587c00,d.root,forces[0],city);other=call(0x587b50,d.root,forces[0],city)
        # Same presence sees own source force. Other presence also sees force2,
        # so both must remain true even when force1 is outside this region.
        if actual!=expected or same!=1 or other!=1:raise ValueError(('Original regional aggregation differs',actual,expected,same,other))
        if before!=bytes(u.mem_read(0x7200000,0x300000)) or grid!=bytes(u.mem_read(0x6fb0000,0x100000)) or rng!=bytes(u.mem_read(0x8a5d44,4)) or external!=bytes(u.mem_read(0x9500000,0x1000000)):raise ValueError('Original regional inspection mutated state')
        rows.append(dict(queryRelationBit=query_bit,queryTruceCounter=query_counter,reverseRelationBit=reverse_bit,reverseTruceCounter=reverse_counter,force1RegionNativeCity=which_region,force1Troops=troop,aggregate=actual,samePresenceRaw=same,otherPresenceRaw=other))
    presence=[]
    for owner,which_region,forward_bit,forward_counter,reverse_bit,reverse_counter in itertools.product(range(4),[0,1],[0,1],[0,1],[0,1],[0,1]):
        for force in forces:u.mem_write(force+0x50,bytes(8));u.mem_write(force+0x64,bytes(47))
        u.mem_write(forces[0]+0x50,struct.pack('<Q',forward_bit<<owner));u.mem_write(forces[0]+0x64+owner,bytes([forward_counter]));u.mem_write(forces[owner]+0x50,struct.pack('<Q',reverse_bit));u.mem_write(forces[owner]+0x64,bytes([reverse_counter]))
        unit=d.root+0x169730;u.mem_write(unit+0xc,struct.pack('<i',owner));u.mem_write(unit+0x3c,struct.pack('<hh',150,150));u.mem_write(0x6fb0e6c+20*(150*200+150),struct.pack('<I',regions[which_region]<<5))
        u.mem_write(d.stream+0x300+8,struct.pack('<H',0));u.mem_write(d.stream+0x100,struct.pack('<III',d.stream+0x300,0,0));u.mem_write(0x9552710,struct.pack('<I',d.stream+0x100))
        before=bytes(u.mem_read(0x7200000,0x300000));grid=bytes(u.mem_read(0x6fb0000,0x100000));rng=bytes(u.mem_read(0x8a5d44,4))
        same=call(0x587c00,d.root,forces[0],city);other=call(0x587b50,d.root,forces[0],city)
        expected_same=int(which_region==0 and owner==0);expected_other=int(which_region==0 and owner!=0 and not forward_bit and not forward_counter)
        if same!=expected_same or other!=expected_other:raise ValueError(('Original exclusive presence/direction differs',owner,which_region,forward_bit,forward_counter,reverse_bit,reverse_counter,same,other,expected_same,expected_other))
        if before!=bytes(u.mem_read(0x7200000,0x300000)) or grid!=bytes(u.mem_read(0x6fb0000,0x100000)) or rng!=bytes(u.mem_read(0x8a5d44,4)):raise ValueError('Original exclusive presence mutated state')
        presence.append(dict(unitOwnerNativeId=owner,unitRegionNativeCity=which_region,queryToUnitRelationBit=forward_bit,queryToUnitTruceCounter=forward_counter,unitToQueryRelationBit=reverse_bit,unitToQueryTruceCounter=reverse_counter,samePresenceRaw=same,otherPresenceRaw=other))
    report=dict(sourceExecutableSha256=EXE_SHA,nativeChecks=len(rows)*3+len(presence)*2,rows=rows,exclusivePresence=presence,sourcePolicy='READ_ONLY_NO_WINE_NO_RULE_COMMANDS_OR_RNG',
        nativeMethods=['587a00','587b50','587c00','56dba0','496030','4955a0','496010','4839f0','4811b0','4b5cc0'],territoryByteTableSha256=hashlib.sha256(table).hexdigest(),
        evidence=['Original unit list payload uses56dba0 index+8 and registry stridef4','Virtual3c returns unit+3c signed-short coordinates; original map region uses20*(x*200+y), bits5..11 and original4839f0 byte table','Aggregation and other presence587b50 use queried force to unit owner; reverse input variations do not change their result','587c00 requires same queried force identity in the original city region','A far-away unit150,150 in same original city region is included; no hex-distance/visible-map approximation'],
        limits=['Explicit fixture registries/coordinates/relation states, not normal scenario-start or Android context reconstruction.','Original static region table is preserved; linking Android terrain regions/native ownership still requires approved committed facts.','No ordinary BGM playback claim or rule/metadata changes.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print('PASS original music region aggregation/presence checks=',report['nativeChecks'])

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
