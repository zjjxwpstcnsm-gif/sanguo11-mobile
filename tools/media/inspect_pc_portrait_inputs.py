#!/usr/bin/env python3
"""Join a committed session1 identity request to original serializer/age/visual lookup.

Does not infer identity, choose a MOD activation policy or modify metadata/rules.
"""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import struct
from unicorn import Uc, UC_ARCH_X86, UC_MODE_32, UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_EAX,UC_X86_REG_ECX,UC_X86_REG_EDI,UC_X86_REG_ESI,UC_X86_REG_ESP,UC_X86_REG_EIP

EXE_SHA='30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb'


def sha(data):return hashlib.sha256(data).hexdigest()


def inspect(installation, requests_path, pixels_path, output, metadata_commit):
    if output.exists() or installation.resolve() in output.resolve().parents:raise ValueError('Fresh output outside source required')
    raw_requests=requests_path.read_bytes();requests=json.loads(gzip.decompress(raw_requests) if requests_path.suffix=='.gz' else raw_requests)['requests']
    pixels=json.loads(pixels_path.read_text());exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Original executable guard')
    fce=(installation/pixels['sourceFile']).read_bytes()
    if sha(fce)!=pixels['sourceSha256']:raise ValueError('Source pixel guard')
    _,_,face_start,face_count,_=struct.unpack_from('<5I',fce)
    u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(0x400000,0x500000);u.mem_write(0x400000,exe[:0x500000])
    actor,stream,stack,stop=0x10000000,0x10001000,0x20000000,0x30000000
    u.mem_map(actor,8192);u.mem_map(stack,8192);u.mem_map(stop,4096)
    u.mem_map(0x6fae000,0x4000);u.mem_write(0x6fae8b8+face_start*4,fce[20:20+face_count*4]);u.mem_map(0x7201000,8192)
    current=b'';cursor=0;reads=[];native_id=0
    def io(machine,address,size,user):
        nonlocal cursor
        sp=machine.reg_read(UC_X86_REG_ESP);ret,destination,length=struct.unpack('<III',machine.mem_read(sp,12))
        if machine.reg_read(UC_X86_REG_ECX)!=stream or cursor+length>len(current):raise ValueError('Unexamined native IO')
        machine.mem_write(destination,current[cursor:cursor+length]);reads.append((cursor,length,destination-actor));cursor+=length
        machine.reg_write(UC_X86_REG_EAX,1);machine.reg_write(UC_X86_REG_ESP,sp+12);machine.reg_write(UC_X86_REG_EIP,ret)
    u.hook_add(UC_HOOK_CODE,io,begin=0x46ff20,end=0x46ff20)
    def identity(machine,address,size,user):
        sp=machine.reg_read(UC_X86_REG_ESP);ret=struct.unpack('<I',machine.mem_read(sp,4))[0]
        machine.reg_write(UC_X86_REG_EAX,native_id);machine.reg_write(UC_X86_REG_ESP,sp+4);machine.reg_write(UC_X86_REG_EIP,ret)
    u.hook_add(UC_HOOK_CODE,identity,begin=0x4883c0,end=0x4883c0)
    u.hook_add(UC_HOOK_CODE,lambda machine,a,s,d:machine.emu_stop(),begin=0x4a672d,end=0x4a672d)
    def call(address,this):
        u.reg_write(UC_X86_REG_ECX,this);u.mem_write(stack+4096,struct.pack('<I',stop));u.reg_write(UC_X86_REG_ESP,stack+4096)
        u.emu_start(address,stop,count=2000);return u.reg_read(UC_X86_REG_EAX)
    def decode(raw):
        nonlocal current,cursor,reads
        current=raw;cursor=0;reads=[];u.mem_write(actor,bytes(4096));u.mem_write(stream,bytes(4096))
        u.mem_write(stream+8,struct.pack('<I',1));u.mem_write(stream+0x54,struct.pack('<I',22))
        first=call(0x43a8a0,stream)
        if not first and not call(0x43a8f0,stream):raise ValueError('Unexamined native serializer type')
        u.reg_write(UC_X86_REG_ESI,stream);u.reg_write(UC_X86_REG_EDI,actor);u.reg_write(UC_X86_REG_ESP,stack+4096)
        u.emu_start(0x48b7b7,0x48bb28,count=40000)
        if cursor!=152:raise ValueError('Native serializer consumed bytes changed')
        face,sex,appearance,birth,death=struct.unpack('<5i',u.mem_read(actor+0x3c,20))
        if face!=struct.unpack_from('<h',raw,53)[0] or sex!=struct.unpack_from('<b',raw,55)[0]:raise ValueError('Native signed-width source decoding')
        threshold=bytes(u.mem_read(actor+0x120,1))[0]
        field_reads=[(at,size) for at,size,dst in reads if dst==0x120]
        if len(field_reads)!=1:raise ValueError('Unexamined age input serializer')
        slot=call(0x48a5b0,actor)
        return dict(faceId=face,sexRaw=sex,birth=birth,ageThreshold=threshold,ageFieldRead=field_reads[0],dynamicSelector=131+slot)
    sources={};cache={};age_cache={};rows=[];assets={(r['faceId'],r['imageGroup']):r for r in pixels['entries']}
    for req in requests:
        name=req['sourcePath'];source=sources.setdefault(name,(installation/name).read_bytes())
        if sha(source)!=req['sourceSha256']:raise ValueError('Committed metadata source guard')
        if source[:8]!=bytes.fromhex('0000feff16000000') or len(source)!=170010:raise ValueError('Unexamined scenario source')
        native_id=req['nativeId'];raw=source[17760+152*native_id:17760+152*(native_id+1)]
        if sha(raw)!=req['recordSha256']:raise ValueError('Committed metadata record guard')
        key=sha(raw)
        if key not in cache:cache[key]=decode(raw)
        decoded=cache[key]
        if decoded['faceId']!=req['faceNativeId'] or decoded['birth']!=req['birth']:raise ValueError('Media inputs differ from metadata')
        face,birth,threshold=decoded['faceId'],decoded['birth'],decoded['ageThreshold']
        agekey=(face,birth,threshold,decoded['sexRaw'])
        if agekey not in age_cache:
            samples=[]
            for age in (threshold-1,threshold,threshold+1):
                u.mem_write(actor,bytes(4096));u.mem_write(actor+0x3c,struct.pack('<ii',face,decoded['sexRaw']))
                u.mem_write(actor+0x48,struct.pack('<i',birth));u.mem_write(actor+0x120,bytes([threshold]))
                u.mem_write(0x7201960,struct.pack('<i',birth+age-1));u.mem_write(0x7201970,struct.pack('<I',1))
                u.reg_write(UC_X86_REG_ESI,actor);u.reg_write(UC_X86_REG_ESP,stack+4096)
                u.emu_start(0x4a66ea,0x4a6738,count=1000)
                selected=struct.unpack('<i',u.mem_read(actor+0x3c,4))[0]
                expected=face+1000 if 0<=face<1000 and age>=threshold else face
                if selected!=expected:raise ValueError('Original age face transition differs')
                selector=131+call(0x48a5b0,actor)
                samples.append(dict(age=age,faceId=selected,dynamicSelector=selector,textureResource=369+selector))
            age_cache[agekey]=samples
        forms=[]
        needed={face}|{x['faceId'] for x in age_cache[agekey]}
        for selected in sorted(needed):
            for group in range(3):
                asset=assets.get((selected,group));form=dict(faceId=selected,imageGroup=group,status='FACE_OUTSIDE_SOURCE' if asset is None else asset['status'])
                if asset and asset['bytes']:form.update(asset=asset['asset'],pngSha256=asset['pngSha256'],rgbaSha256=asset['rgbaSha256'],width=asset['width'],height=asset['height'])
                forms.append(form)
        rows.append({k:req[k] for k in ['officerId','nativeId','sourceVariant','sourcePath','sourceSha256','recordSha256','identityStatus']}|dict(
            **decoded,ageBoundaries=age_cache[agekey],forms=forms,runtimeActivation='UNKNOWN',effectiveRuntimeCoverage=False))
        if len(rows)%1000==0:print('joined',len(rows),flush=True)
    report=dict(schema=1,metadataCommit=metadata_commit,metadataRequestSha256=sha(raw_requests),sourceExecutableSha256=EXE_SHA,
                sourceFaceSha256=pixels['sourceSha256'],requestCount=len(rows),identityVerifiedRequests=sum(r['officerId'] is not None for r in rows),
                uniqueNativeRecords=len(cache),nativeAgeBoundaryChecks=3*len(age_cache),effectiveRuntimeOfficerCoverage=0,
                agePolicy='Original transition only: face<1000 and year-birth+1 >= actor+120 adds1000. Existing old face does not revert.',
                entries=rows,limits=['Identity copied only from committed session1 contract; unknowns remain null.',
                                    'All three source image groups retained; normal/dialogue/duel usage still requires native caller proof.',
                                    'Installed Media candidates are not certified MOD live priority or saved runtime source identity.',
                                    'Native age slice uses original numeric inputs; no rule command, notification, save or RNG executed.'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:v for k,v in report.items() if k not in ('entries','limits')}))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--requests',type=Path,required=True);p.add_argument('--pixels',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--metadata-commit',required=True)
    a=p.parse_args();inspect(a.installation,a.requests,a.pixels,a.output,a.metadata_commit)
