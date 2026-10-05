#!/usr/bin/env python3
"""Read installed scenario names/faces with the unchanged native serializer.

Only native file IO is supplied by the harness. No Wine, installation writes,
attachment activation, gameplay changes or guessed community-ID translation.
"""
import argparse, csv, json, struct
from pathlib import Path
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ECX, UC_X86_REG_EDI, UC_X86_REG_ESI, UC_X86_REG_ESP, UC_X86_REG_EIP, UC_X86_REG_EAX
from pc_resources import sha
from inspect_pc_effect_bindings import EXE_SHA

ROOT=Path(__file__).resolve().parents[2]
WANTED={1000,1001,1002,1003,1004,2000}
BASE,STRIDE=17760,152

def inspect(installation, output):
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Reinspect changed source executable')
    faces=json.loads((ROOT/'docs/pc-visual/face-descriptors-source-working.json').read_text())
    if faces['source_executable_sha256']!=EXE_SHA:raise ValueError('Descriptor executable differs')
    fce=installation/faces['source_file']
    if sha(fce.read_bytes())!=faces['source_sha256']:raise ValueError('Descriptor source differs')
    descriptors=bytes.fromhex(''.join(r['descriptor_hex'] for r in faces['entries']))
    catalog=list(csv.DictReader((ROOT/'core/src/main/resources/content/officers.tsv').open(),delimiter='\t'))
    aliases={int(r['id']):r['alias'] for r in csv.DictReader((ROOT/'core/src/main/resources/content/aliases.tsv').open(),delimiter='\t')}
    wanted=[r for r in catalog if int(r['id']) in WANTED]
    u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000])
    actor,stream,stack,stop=0x10000000,0x10001000,0x20000000,0x30000000
    u.mem_map(actor,8192);u.mem_map(stack,8192);u.mem_map(stop,4096)
    u.mem_map(0x6fae000,0x4000);u.mem_write(0x6fae8b8,descriptors);u.mem_map(0x7201000,8192)
    current=b'';cursor=0;reads=[]
    def read_hook(machine,address,size,user):
        nonlocal cursor
        sp=machine.reg_read(UC_X86_REG_ESP)
        ret,destination,length=struct.unpack('<III',machine.mem_read(sp,12))
        if machine.reg_read(UC_X86_REG_ECX)!=stream or cursor+length>len(current):raise ValueError('Unexamined source IO')
        machine.mem_write(destination,current[cursor:cursor+length]);reads.append((cursor,length,destination-actor));cursor+=length
        machine.reg_write(UC_X86_REG_EAX,1);machine.reg_write(UC_X86_REG_ESP,sp+12);machine.reg_write(UC_X86_REG_EIP,ret)
    u.hook_add(UC_HOOK_CODE,read_hook,begin=0x46ff20,end=0x46ff20)
    def decode(raw):
        nonlocal current,cursor,reads
        current=raw;cursor=0;reads=[];u.mem_write(actor,bytes(4096));u.mem_write(stream,bytes(4096))
        u.mem_write(stream+8,struct.pack('<I',1));u.mem_write(stream+0x54,struct.pack('<I',22))
        u.reg_write(UC_X86_REG_ECX,stream);u.mem_write(stack+4096,struct.pack('<I',stop));u.reg_write(UC_X86_REG_ESP,stack+4096)
        u.emu_start(0x43a8a0,stop,count=100);first=u.reg_read(UC_X86_REG_EAX)
        if not first:
            u.reg_write(UC_X86_REG_ECX,stream);u.mem_write(stack+4096,struct.pack('<I',stop));u.reg_write(UC_X86_REG_ESP,stack+4096)
            u.emu_start(0x43a8f0,stop,count=100)
        if not (first or u.reg_read(UC_X86_REG_EAX)):raise ValueError('Source scenario type not in serializer branch')
        # Execute the unchanged full actor serializer after initialization and
        # type dispatch. Only decoded immutable portrait inputs leave this tool.
        u.reg_write(UC_X86_REG_ESI,stream);u.reg_write(UC_X86_REG_EDI,actor);u.reg_write(UC_X86_REG_ESP,stack+4096)
        u.emu_start(0x48b7b7,0x48bb28,count=40000)
        if cursor!=152:raise ValueError('Native record byte count changed: '+str(cursor))
        surname=bytes(u.mem_read(actor+4,5)).split(b'\0')[0].decode('big5')
        given=bytes(u.mem_read(actor+9,5)).split(b'\0')[0].decode('big5')
        face,sex,appearance,birth,death=struct.unpack('<5i',u.mem_read(actor+0x3c,20))
        if not any(at==53 and size==2 for at,size,dest in reads) or not any(at==55 and size==1 for at,size,dest in reads):raise ValueError('Original face/sex field IO changed')
        if face!=struct.unpack_from('<h',raw,53)[0] or sex!=struct.unpack_from('<b',raw,55)[0]:raise ValueError('Native signed face/sex result differs')
        u.reg_write(UC_X86_REG_ECX,actor);u.mem_write(stack+4096,struct.pack('<I',stop));u.reg_write(UC_X86_REG_ESP,stack+4096)
        u.emu_start(0x48a5b0,stop,count=2000);slot=u.reg_read(UC_X86_REG_EAX)
        return dict(name=surname+given,face_id=face,sex=sex,appearance=appearance,birth=birth,death=death,selector=131+slot,age_change=bytes(u.mem_read(actor+0x120,1))[0])
    sources=[];bindings={}
    paths=sorted((installation/'Media/scenario').iterdir(),key=lambda p:p.name.lower())
    for path in paths:
        if not path.name.lower().startswith('scen0') or path.suffix.lower()!='.s11':continue
        data=path.read_bytes()
        if len(data)!=170010 or data[:8]!=bytes.fromhex('0000feff16000000') or data[8:18]!=b'KOEI%SAN11':raise ValueError('Unexamined installed scenario header')
        sources.append(dict(path=path.relative_to(installation).as_posix(),sha256=sha(data)))
        for person in wanted:
            native=int(person['sourceId']);offset=BASE+STRIDE*native;raw=data[offset:offset+STRIDE];decoded=decode(raw)
            if decoded['name']!=person['name'] or decoded['birth']!=int(person['birth']):raise ValueError('Native identity does not match '+person['name'])
            if decoded['sex']!=(1 if person['gender']=='女' else 0):raise ValueError('Native gender differs')
            selector=decoded['selector'];template,top=struct.unpack_from('<2I',exe,0x3774e0+selector*8)
            if template!=115:raise ValueError('Unexamined native portrait template')
            row=dict(project_id=int(person['id']),runtime_name=aliases[int(person['id'])],native_index=native,**decoded,
                template=template,texture_resource=369+selector,scenario_records=[])
            key=int(person['id']);previous=bindings.setdefault(key,row)
            for k,v in row.items():
                if k not in ('scenario_records','appearance','death','face_id','selector','texture_resource') and previous[k]!=v:raise ValueError('Scenario-dependent identity requires explicit policy: '+k)
            previous['scenario_records'].append(dict(source=path.relative_to(installation).as_posix(),offset=offset,bytes=STRIDE,sha256=sha(raw),appearance=decoded['appearance'],death=decoded['death'],face_id=decoded['face_id'],selector=selector))
    if len(sources)!=16 or len(bindings)!=len(WANTED):raise ValueError('Incomplete installed scenario evidence')
    # Original4a66c0 changes a face by+1000 when original488a20's age
    # (year-birth+1) reaches the serialized actor byte120. Supply the registry
    # identity only; stop before408820's display notification, never emulate a
    # gameplay action or pretend this establishes a MOD's live override order.
    native_id=0
    def identity_hook(machine,address,size,user):
        sp=machine.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',machine.mem_read(sp,4))[0]
        machine.reg_write(UC_X86_REG_EAX,native_id);machine.reg_write(UC_X86_REG_ESP,sp+4);machine.reg_write(UC_X86_REG_EIP,ret)
    u.hook_add(UC_HOOK_CODE,identity_hook,begin=0x4883c0,end=0x4883c0)
    u.hook_add(UC_HOOK_CODE,lambda machine,a,s,d:machine.emu_stop(),begin=0x4a672d,end=0x4a672d)
    age_checks=0
    for row in bindings.values():
        native_id=row['native_index'];young=row['face_id']%1000;threshold=row['age_change'];variants={}
        for age in (threshold-1,threshold,threshold+1):
            year=row['birth']+age-1;u.mem_write(actor+0x3c,struct.pack('<I',young))
            u.mem_write(actor+0x48,struct.pack('<I',row['birth']));u.mem_write(actor+0x120,bytes([threshold]))
            u.mem_write(0x7201960,struct.pack('<I',year));u.mem_write(0x7201970,struct.pack('<I',1))
            u.reg_write(UC_X86_REG_ESI,actor);u.reg_write(UC_X86_REG_ESP,stack+4096)
            u.emu_start(0x4a66ea,0x4a6738,count=1000)
            selected=struct.unpack('<I',u.mem_read(actor+0x3c,4))[0]
            if selected!=young+(1000 if age>=threshold else 0):raise ValueError('Original age transition differs')
            u.reg_write(UC_X86_REG_ECX,actor);u.mem_write(stack+4096,struct.pack('<I',stop));u.reg_write(UC_X86_REG_ESP,stack+4096)
            u.emu_start(0x48a5b0,stop,count=2000);selector=131+u.reg_read(UC_X86_REG_EAX)
            variants['older' if selected>=1000 else 'younger']=dict(face_id=selected,selector=selector,texture_resource=369+selector)
            age_checks+=1
        row['variants']=variants
        if {(r['face_id'],r['selector'])for r in row['scenario_records']}!={(r['face_id'],r['selector'])for r in variants.values()}:raise ValueError('Scenario variants and original age lookup differ')
    report=dict(schema=1,goal_complete=False,status='INSTALLED_SCENARIO_IDENTITY_NATIVE_SERIALIZER_AGE_AND_LOOKUP_VERIFIED',
        source_executable_sha256=EXE_SHA,source_face_sha256=faces['source_sha256'],scenario_sources=sources,
        serializer=dict(start='48b7b7',stop='48bb28',source_type=22,consumed_bytes=152,
            face_serialized_offset=53,actor_face_offset=60,sex_serialized_offset=55,actor_sex_offset=64,
            sha256=sha(exe[0x8b7b7:0x8bb28]),io_hook='46ff20 copies original installed bytes only; original signed-width decoding unchanged'),
        checks=len(sources)*len(bindings),age_checks=age_checks,age_transition=dict(function='4a66ea..4a672d / 4a6738',age_function='488a20',age='year-birth+1',threshold='serialized actor byte120',face_change='+1000',sha256=sha(exe[0xa66ea:0xa672d])),bindings=sorted(bindings.values(),key=lambda r:r['project_id']),
        limits=['Native age transition verified offline; MOD live override order and already-customized PC faces remain pending',
            'Attachment SceCharData.s11 helped locate split names, but none of its fields are runtime inputs',
            'Community sourceId checked against actual native name/birth/sex; never assumed equivalent',
            'No Wine; no source installation writes; PC camera/timing and MOD live overrides remain unaccepted'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(dict(status=report['status'],checks=report['checks'],bindings=[{k:r[k] for k in ('runtime_name','face_id','selector')}for r in report['bindings']]),ensure_ascii=False))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,default=ROOT/'docs/pc-visual/scenario-portraits-source-working.json')
    a=p.parse_args();inspect(a.installation,a.output)
