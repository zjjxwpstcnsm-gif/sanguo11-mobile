#!/usr/bin/env python3
"""Read original FCE descriptors and execute original portrait lookup offline.

No Wine or installation writes. These are face IDs, not officer IDs. Actual
scenario face selection/age and MOD loader order are separate acceptance gaps.
"""
import argparse, hashlib, json, struct
from pathlib import Path
from capstone import Cs, CS_ARCH_X86, CS_MODE_32
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32
from unicorn.x86_const import UC_X86_REG_EAX, UC_X86_REG_ECX, UC_X86_REG_ESP
from inspect_pc_effect_bindings import EXE_SHA

ROOT=Path(__file__).resolve().parents[2]

def inspect(installation,output):
    exe=(installation/'san11pk.exe').read_bytes()
    if hashlib.sha256(exe).hexdigest()!=EXE_SHA:raise ValueError('Source EXE changed')
    md=Cs(CS_ARCH_X86,CS_MODE_32)
    def code(a,b):return list(md.disasm(exe[a-0x400000:b-0x400000],a))
    loader=code(0x46e360,0x46e46c)
    expected={0x46e3b2:('push','0x14'),0x46e403:('lea','eax, [edx*4 + 0x6fae8b8]'),
        0x46e40b:('shl','ebp, 2'),0x46e40f:('push','0x14'),0x46e412:('call','0x46ddf0')}
    observed={i.address:(i.mnemonic,i.op_str) for i in loader}
    if any(observed.get(a)!=v for a,v in expected.items()):raise ValueError('Original descriptor loader changed')
    source=installation/'Media/face/San11Face00.fce'
    digest=hashlib.sha256()
    with source.open('rb') as stream:
        for block in iter(lambda:stream.read(1024*1024),b''):digest.update(block)
        stream.seek(0);header=stream.read(20);magic,version,start,count,images=struct.unpack('<5I',header)
        if magic!=0x45434146 or version>100 or start+count>2400 or count==0 or images!=count*3:raise ValueError('Unexamined FCE header')
        descriptors=stream.read(count*4)
        if len(descriptors)!=count*4:raise ValueError('Truncated descriptor table')
    # Execute the unchanged480320 and48a5b0/49d730, with only their original
    # data inputs supplied. No platform imports, game rules or visual RNG.
    u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000])
    u.mem_map(0x6fae000,0x4000);u.mem_write(0x6fae8b8+start*4,descriptors)
    actor,stack,stop=0x10000000,0x20000000,0x30000000
    for base in (actor,stack,stop):u.mem_map(base,4096)
    def original_lookup(face,sex=0,direct=False):
        u.mem_write(actor+0x3c,struct.pack('<iI',face,sex));u.mem_write(stack+2048,struct.pack('<II',stop,face&0xffffffff))
        u.reg_write(UC_X86_REG_ECX,actor);u.reg_write(UC_X86_REG_ESP,stack+2048)
        u.emu_start(0x480320 if direct else 0x48a5b0,stop,count=2000);return u.reg_read(UC_X86_REG_EAX)
    rows=[]
    for local in range(count):
        face=start+local;raw=descriptors[local*4:local*4+4]
        slot=struct.unpack('<b',raw[2:3])[0] if raw[3]&2 else 39
        native=original_lookup(face,direct=True)
        if native!=slot:raise ValueError('Original lookup disagrees with descriptor '+str(face))
        selector=131+slot
        if not 0<=selector<347:raise ValueError('Dynamic selector outside original table')
        template,top=struct.unpack_from('<2I',exe,0x3774e0+selector*8)
        resource=struct.unpack_from('<I',exe,0x37692c+template*12+4)[0]
        actor_slots={str(sex):original_lookup(face,sex) for sex in (0,1)}
        expected_slots={str(sex):slot if raw[3]&2 else original_lookup(2000 if sex==0 else 2100,direct=True) for sex in (0,1)}
        if actor_slots!=expected_slots:raise ValueError('Original actor sex fallback disagrees '+str(face))
        rows.append(dict(face_id=face,descriptor_hex=raw.hex(),flags=raw[3],slot=slot,dynamic_selector=selector,actor_slots_by_sex=actor_slots,
            texture_resource=369+selector,effect_index=template,effect_resource=resource,destination_top=top,
            original_lookup_checked=True,officer_binding=None))
    # Valid unflagged actor faces go through the original sex fallback. Invalid
    # actor indices take a distinct source branch; do not conflate the two.
    fallback={str(sex):original_lookup(130,sex) for sex in (0,1)}
    invalid={str(face):original_lookup(face) for face in (-1,2400)}
    report=dict(schema=1,goal_complete=False,status='READONLY_FCE_DESCRIPTOR_LOAD_AND_ORIGINAL_LOOKUP_VERIFIED',
        source_executable_sha256=EXE_SHA,source_file='Media/face/San11Face00.fce',source_sha256=digest.hexdigest(),
        header=dict(magic='FACE',version=version,start=start,count=count,images=images),descriptor_offset=20,
        descriptor_sha256=hashlib.sha256(descriptors).hexdigest(),source_loader='46e360:20-byte header;46ddf0 reads count*4 bytes at20 into6fae8b8+start*4',
        lookup='original48a5b0 ->480320; original49d730 sex fallback; source584ed8 adds131',
        source_loader_sha256=hashlib.sha256(exe[0x6e360:0x6e46c]).hexdigest(),
        original_actor_lookup_sha256=hashlib.sha256(exe[0x8a5b0:0x8a5ed]).hexdigest(),
        original_checks=count*3+len(fallback)+len(invalid),sex_fallback_slots=fallback,invalid_actor_slots=invalid,entries=rows,
        limits=['Face IDs and content/officers.tsv sourceId are different domains; no inferred officer binding',
            'Individual actor scenario face, age change and supplied integration MOD loader precedence remain pending',
            'Only the installed Media/face/San11Face00.fce is loaded by this offline check; backups/attachments are not silently activated',
            'Original pixels and offline lookup do not establish PC fullscreen camera/timing acceptance',
            'No Wine launched; source installation remains read-only'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(dict(status=report['status'],source_sha256=report['source_sha256'],original_checks=report['original_checks'],selectors=sorted({r['dynamic_selector'] for r in rows}))))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('installation',type=Path)
    parser.add_argument('--output',type=Path,default=ROOT/'docs/pc-visual/face-descriptors-source-working.json')
    args=parser.parse_args();inspect(args.installation,args.output)
