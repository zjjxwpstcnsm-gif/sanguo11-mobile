#!/usr/bin/env python3
"""Read pinned PC contest labels and direct references; no inferred rule values.

This is an entry-point investigation, not restored combat or presentation.
Original character descriptor initialization executes; literal/xref evidence is
static and must never be promoted to active-state or numeric-rule proof.
"""
import argparse,json,struct
from pathlib import Path
from capstone import Cs,CS_ARCH_X86,CS_MODE_32
from inspect_pc_officer_fields import NativeFields
from audit_pc_restoration_sources import EXE_SHA,output_guard,json_bytes,sha

LABELS=['單挑','舌戰','鬥志','必殺技','集氣','堅守','無雙','冷靜','剛膽','莽撞','大喝','詭辯','無視','鎮靜']

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    raw=(installation/'san11pk.exe').read_bytes()
    if sha(raw)!=EXE_SHA:raise ValueError('Changed executable')
    pe=struct.unpack_from('<I',raw,60)[0];count=struct.unpack_from('<H',raw,pe+6)[0];optional=struct.unpack_from('<H',raw,pe+20)[0]
    image=struct.unpack_from('<I',raw,pe+24+28)[0];sections=[]
    for i in range(count):
        off=pe+24+optional+i*40
        virtual_bytes,rva,stored_bytes,stored_offset=struct.unpack_from('<4I',raw,off+8)
        flags=struct.unpack_from('<I',raw,off+36)[0]
        if stored_offset+stored_bytes>len(raw):raise ValueError('PE section escaped file')
        sections.append(dict(name=raw[off:off+8].split(b'\0')[0].decode('ascii'),offset=stored_offset,bytes=stored_bytes,address=image+rva,executable=bool(flags&0x20000000)))
    def section(offset):return next((s for s in sections if s['offset']<=offset<s['offset']+s['bytes']),None)
    def address(offset):
        s=section(offset);return None if s is None else s['address']+offset-s['offset']
    md=Cs(CS_ARCH_X86,CS_MODE_32);rows=[]
    for label in LABELS:
        encoded=label.encode('big5')+b'\0';start=0
        while True:
            offset=raw.find(encoded,start)
            if offset<0:break
            start=offset+1;va=address(offset)
            if va is None:continue
            references=[];probe=0;pointer=struct.pack('<I',va)
            while True:
                ref=raw.find(pointer,probe)
                if ref<0:break
                probe=ref+1;s=section(ref)
                if s is None:continue
                begin=max(s['offset'],ref-16);stop=min(s['offset']+s['bytes'],ref+20);context=raw[begin:stop]
                references.append(dict(address=hex(address(ref)),section=s['name'],executable=s['executable'],contextAddress=hex(address(begin)),contextHex=context.hex(),instructionsFromUnprovenBoundary=[dict(address=hex(i.address),hex=i.bytes.hex(),instruction=i.mnemonic+' '+i.op_str)for i in md.disasm(context,address(begin))]if s['executable']else[],provesRuleSemantics=False))
            rows.append(dict(label=label,rawHex=encoded.hex(),fileOffset=offset,address=hex(va),references=references))
    world=NativeFields(raw);before=bytes(world.u.mem_read(0x7200000,0x300000));rng=bytes(world.u.mem_read(0x8a5d44,4))
    descriptors=[d for d in world.descriptors if d['id']in [43,47,58,59,60,61,62]]
    tables=[]
    for name,table,count in [('personality',0x83e9b0,4),('duel_special_menu',0x8ad57c,6),('debate_talk_menu',0x8ad5c4,5)]:
        pointers=bytes(world.u.mem_read(table,count*4));values=[]
        for ordinal,pointer in enumerate(struct.unpack('<%dI'%count,pointers)):
            if not 0x7e0000<=pointer<0x800000:raise ValueError('Label pointer outside examined original text')
            encoded=bytes(world.u.mem_read(pointer,80)).split(b'\0')[0]
            values.append(dict(ordinal=ordinal,pointer=hex(pointer),rawHex=encoded.hex(),label=encoded.decode('big5')))
        tables.append(dict(name=name,address=hex(table),pointerBytesHex=pointers.hex(),sha256=sha(pointers),values=values,meaning='original text table only; effect and availability rules unproven'))
    if [v['label']for v in tables[0]['values']]!=['膽小','冷靜','剛膽','莽撞']:
        raise ValueError('Personality table differs')
    if before!=bytes(world.u.mem_read(0x7200000,0x300000))or rng!=bytes(world.u.mem_read(0x8a5d44,4)):raise ValueError('Descriptor inspection mutated world/RNG')
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sections=sections,labels=rows,characterDescriptors=descriptors,textTables=tables,originalDescriptorInitializer='73ca80',ruleExecution=False,completeDuelRestoration=False,completeDebateRestoration=False,limits=['Literal/direct-pointer references only; instruction start boundaries not proven','No label ordinal or personality mapping inferred','No damage/cards/anger/initiative/capture/reward/RNG rules executed','PC installation read only; no Wine; no Android/media change'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(json_bytes(report));print(json.dumps(dict(labels=len(rows),references=sum(len(x['references'])for x in rows),sha256=sha(output.read_bytes()),ruleExecution=False)))
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
