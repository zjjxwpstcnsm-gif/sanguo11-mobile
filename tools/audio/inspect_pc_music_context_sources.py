#!/usr/bin/env python3
"""Prove music calendar and city-count inputs through original readonly code.

Uses installed scenarios and existing original decoders, no Wine/rules/RNG.
Does not infer scene roles or replace the unresolved music threat predicates.
"""
import argparse,hashlib,json,struct,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'content'))
from inspect_pc_scenario_domains import NativeDomainDecoder
from inspect_pc_scenario_metadata import NativeMetadataDecoder
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP,UC_X86_REG_EAX

EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'

def inspect(installation,output):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside readonly source required')
    exe=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(exe).hexdigest()!=EXE_SHA:raise ValueError('Source executable changed')
    d=NativeDomainDecoder(exe);m=NativeMetadataDecoder(exe);u=d.u;checks=0
    def call(address,receiver,*args):
        before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4))
        u.reg_write(UC_X86_REG_ECX,receiver);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<'+'I'*(len(args)+1),d.stop,*args))
        u.emu_start(address,d.stop,count=1000000)
        if u.reg_read(UC_X86_REG_EIP)!=d.stop or before!=bytes(u.mem_read(0x7200000,0x300000)) or rng!=bytes(u.mem_read(0x8a5d44,4)):raise ValueError('Original getter not pure or incomplete')
        return u.reg_read(UC_X86_REG_EAX)
    paths=sorted((p for p in (installation/'Media/scenario').iterdir() if p.name.lower().startswith('scen0') and p.suffix.lower()=='.s11'),key=lambda p:p.name.lower())
    if len(paths)!=16:raise ValueError('Expected all16 source scenarios')
    sources=[]
    for path in paths:
        raw=path.read_bytes();meta=m.decode_metadata(raw);d.decode(raw)
        u.mem_write(d.root,bytes(m.u.mem_read(m.root,0x1d8)))
        season=call(0x4825a0,d.root);checks+=1;owners=[];rows=[]
        for index in range(42):
            city=call(0x490a10,d.root,index)
            if city!=d.root+0x1d8+index*0x248:raise ValueError('City registry differs')
            table=struct.unpack('<I',u.mem_read(city,4))[0];method=struct.unpack('<I',u.mem_read(table+0x40,4))[0]
            if method!=0x47b2b0:raise ValueError('Original music city ownership method differs')
            owner=call(method,city);owners.append(owner if owner<0x80000000 else owner-(1<<32));checks+=2
        for index in range(47):
            faction=d.root+0x7af8+index*0x12c;native=call(0x491270,d.root,faction)
            if native!=index:raise ValueError('Original faction pointer/index differs')
            valid=call(0x47a630,d.root,faction);actual=call(0x49d670,d.root,faction);count=owners.count(index)
            if actual!=int(bool(valid) and count>=10):raise ValueError('Original music city-count predicate differs')
            table=struct.unpack('<I',u.mem_read(faction,4))[0];controlled=struct.unpack('<I',u.mem_read(table+0x48,4))[0]
            if controlled!=0x480fa0:raise ValueError('Original controlled-faction getter differs')
            control_raw=struct.unpack('<i',u.mem_read(faction+0x60,4))[0];control=call(controlled,faction)
            if control!=int(0<=control_raw<=7):raise ValueError('Original control-slot gate differs')
            rows.append(dict(forceNativeId=index,factionValidRaw=valid,ownedSourceCities=count,countPredicateRaw=actual,sourceCallerVirtual48Address=hex(controlled),controlSlotRaw=control_raw,controlledCallerGateRaw=control));checks+=4
        sources.append(dict(path=path.relative_to(installation).as_posix(),sourceSha256=hashlib.sha256(raw).hexdigest(),date=meta['decoded']['date'],calendarOffsetRaw=struct.unpack('<i',u.mem_read(d.root+0x5c,4))[0],seasonRaw=season,cityOwnerNativeIds=owners,factions=rows))
        print(path.name,'date',meta['decoded']['date'],'seasonRaw',season,flush=True)
    calendar=[]
    for year in [184,190,208]:
        for month in range(1,13):
            for day in [1,11,21]:
                for off,value in [(8,year),(12,month),(16,day),(0x5c,0)]:u.mem_write(d.root+off,struct.pack('<i',value))
                actual=call(0x4825a0,d.root)
                if actual!=(month-1)//3:raise ValueError('Original calendar season differs')
                calendar.append(dict(year=year,month=month,day=day,calendarOffsetRaw=0,seasonRaw=actual));checks+=1
    # Original caller57fc70..57fc8e executes the actual virtual gate. Only its
    # music backend call is observed; stop before unrelated following logic.
    faction=d.root+0x7af8;control_vectors=[];dispatch=[]
    def observe(machine,address,size,user):
        sp=machine.reg_read(UC_X86_REG_ESP);ret,arg=struct.unpack('<2I',machine.mem_read(sp,8));dispatch.append(arg)
        machine.reg_write(UC_X86_REG_ESP,sp+8);machine.reg_write(UC_X86_REG_EIP,ret)
    hook=u.hook_add(UC_HOOK_CODE,observe,begin=0x5880e0,end=0x5880e0)
    try:
        for control in [-2,-1,0,1,2,3,4,5,6,7,8,9]:
            u.mem_write(faction+0x60,struct.pack('<i',control));before=bytes(u.mem_read(0x7200000,0x300000));rng=bytes(u.mem_read(0x8a5d44,4));dispatch.clear()
            u.reg_write(UC_X86_REG_ECX,0x10000000);u.reg_write(UC_X86_REG_ESP,d.stack);u.mem_write(d.stack,struct.pack('<2I',d.stop,faction));u.emu_start(0x57fc70,0x57fc8e,count=1000)
            if u.reg_read(UC_X86_REG_EIP)!=0x57fc8e or dispatch!=([faction] if 0<=control<=7 else []) or before!=bytes(u.mem_read(0x7200000,0x300000)) or rng!=bytes(u.mem_read(0x8a5d44,4)):raise ValueError('Original caller gate changed state or dispatch')
            control_vectors.append(dict(controlSlotRaw=control,selectorCalled=bool(dispatch)));checks+=1
    finally:u.hook_del(hook)
    report=dict(sourceExecutableSha256=EXE_SHA,nativeChecks=checks,sourcePolicy='READ_ONLY_NO_WINE_NO_RULE_COMMANDS_OR_RNG',
        evidence=dict(cityRegistry='490a10: root+1d8+nativeId*248, ids0..41',ownership='city virtual40 is original47b2b0',factionIdentity='491270 returns source force index0..46',cityCount='49d670 true iff valid source faction owns at least10 of42 original cities',season='4825a0 reads root8/c/10/5c; documented zero-offset dates map (month-1)/3'),
        codeSha256={hex(a):hashlib.sha256(exe[a-0x400000:a-0x400000+n]).hexdigest() for a,n in [(0x490a10,48),(0x491270,70),(0x49d670,98),(0x4825a0,128)]},
        sources=sources,calendarVectors=calendar,controlledCallerVectors=control_vectors,
        limits=['Not normal Android BGM playback or source scene proof.','Threat predicates587f00/587d70/587fb0 still require full conditions.','Caller57fc70 gate is proven controlSlotRaw0..7; relation to ordinary Android scene transitions still requires proof.','Project player/owner ids must use approved native force mapping; never assume project ordinal equals native force index.','Calendar offset5c is not silently assumed zero outside documented dates.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n');print('PASS original music calendar/city ownership checks=',checks)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
