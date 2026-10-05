#!/usr/bin/env python3
"""Extract original source personality and five talk flags, source slots distinct."""
import argparse,gzip,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from inspect_pc_scenario_domains import NativeDomainDecoder
from unicorn.x86_const import UC_X86_REG_ECX,UC_X86_REG_ESP,UC_X86_REG_EIP,UC_X86_REG_EAX
from inspect_pc_scenario_officers import NativeOfficerDecoder,BASE,STRIDE
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output);exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Changed executable')
    manifest_raw=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();manifest=json.loads(manifest_raw)
    d=NativeOfficerDecoder(exe);sources=[]
    # Reuse the already audited mapped-memory Win32 IsBadReadPtr boundary.
    # The small serializer-only decoder lacks this import because its read
    #loop did not query flags; no original flag or person rule is substituted.
    pointer_probe=d.stop+0x200
    d.u.mem_write(0x74e268,struct.pack('<I',pointer_probe))
    d.u.hook_add(UC_HOOK_CODE,lambda u,a,n,user:NativeDomainDecoder.probe_pointer(d,u,a,n,user),begin=pointer_probe,end=pointer_probe)
    for source in manifest['scenarios']:
        raw=(installation/source['sourcePath']).read_bytes()
        if sha(raw)!=source['sourceSha256']:raise ValueError('Source changed')
        versions=struct.unpack_from('<2I',raw,24);people=[]
        for native in range(850):
            offset=BASE+native*STRIDE;record=raw[offset:offset+STRIDE];person,actor=d.decode(record,native,versions)
            before=bytes(d.u.mem_read(d.actor,0x190));rng=bytes(d.u.mem_read(0x8a5d44,4));flags=[]
            for index in range(5):
                d.u.reg_write(UC_X86_REG_ECX,d.actor);d.u.reg_write(UC_X86_REG_ESP,d.stack)
                d.u.mem_write(d.stack,struct.pack('<II',d.stop,index));d.u.emu_start(0x489780,d.stop,count=10000)
                if d.u.reg_read(UC_X86_REG_EIP)!=d.stop:raise ValueError('Original flag did not return')
                value=d.u.reg_read(UC_X86_REG_EAX)
                if value not in (0,1):raise ValueError('Original flag not boolean')
                flags.append(value)
            if before!=bytes(d.u.mem_read(d.actor,0x190))or rng!=bytes(d.u.mem_read(0x8a5d44,4)):raise ValueError('Talk queries mutated actor/RNG')
            temper=struct.unpack_from('<i',actor,0xfc)[0]
            people.append(dict(nativeId=native,sourceOffset=offset,recordSha256=sha(record),nameRawHex=''.join(person['name_bytes']),birth=person['birth'],sex=person['sex'],nativePersonality=temper,nativeTalkFlags=flags,nativeTalkMask=sum(bit<<i for i,bit in enumerate(flags)),queryPure=True,projectIdentityJoined=False))
        sources.append(dict(sourcePath=source['sourcePath'],sourceSha256=source['sourceSha256'],sourceVariant=source['sourceVariant'],scenarioId=source['scenarioId'],people=people))
        print(json.dumps(dict(path=source['sourcePath'],people=len(people),historicalTalkUsers=sum(bool(p['nativeTalkMask'])for p in people[:670]))),flush=True)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceManifestSha256=sha(manifest_raw),sources=sources,originalSerializer='48b775..48bb28',originalTalkGetter='489780 ->472590 actor+124 bits3..7',platformBoundary='Existing NativeDomainDecoder.probe_pointer for mapped-memory IsBadReadPtr only',nativeTalkOrder=['大喝','詭辯','無視','鎮靜','憤怒'],nativePersonalityOrder=['膽小','冷靜','剛膽','莽撞'],androidIntegrated=False,completeRestoration=False,limits=['Separate source records; no project ID arithmetic or runtime identity assignment','Profiles do not certify full talk/duel effects or normal PC starts','Existing saves are untouched; NPC/extra activation remains unresolved'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0));print(json.dumps(dict(records=sum(len(s['people'])for s in sources),sha256=sha(output.read_bytes()))));return report
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
