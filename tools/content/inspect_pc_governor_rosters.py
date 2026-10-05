#!/usr/bin/env python3
"""Execute full original4bca30 roster election on each actual owned site.

Each site uses the same postload baseline, independently. Identity pointers
are checked by original4883c0; no project ID is synthesized from a slot.
"""
import argparse,gzip,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output,source_index):
    installation=installation.resolve();output_guard(installation,output)
    if output.exists():raise ValueError('Preserve previous report')
    exe=(installation/'san11pk.exe').read_bytes();manifest_raw=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes()
    source=json.loads(manifest_raw)['scenarios'][source_index];raw=(installation/source['sourcePath']).read_bytes()
    if sha(exe)!=EXE_SHA or sha(raw)!=source['sourceSha256']:raise ValueError('Original provenance differs')
    shared=(installation/'Media/scenario/Scenario.s11').read_bytes();w=NativeDebateFlow(installation,exe).world;load_original_data(w.u)
    w.load(shared,True);loaded=w.load(raw);w.call(0x73c500)
    for f,v in zip([0x4826e0,0x482700,0x482720],source['date']):w.call(f,v,receiver=w.root)
    fixed=struct.unpack('<i',w.u.mem_read(w.root+0x18,4))[0];w.call(0x4827b0,1 if fixed else 0,receiver=w.root)
    if fixed:w.call(0x493f70,1,receiver=w.root);w.call(0x4827f0,3,receiver=w.root)
    w.call(0x493400,receiver=w.root,count=50000000)
    pointers={};people=[]
    for i in range(1100):
        p=w.root+0xc0bc+i*0x190
        if w.call(0x4883c0,receiver=p)!=i:raise ValueError('Original pointer identity differs')
        pointers[p]=i;b=bytes(w.u.mem_read(p,0x190));table=struct.unpack_from('<I',b)[0]
        getter=struct.unpack('<I',w.u.mem_read(table+0x44,4))[0];army=w.call(getter,receiver=p)&0xffffffff
        people.append(dict(nativeId=i,allowed=bool(w.call(0x47a630,p)),location=struct.unpack_from('<i',b,0x98)[0],status=struct.unpack_from('<i',b,0xa0)[0],army=army if army<0x80000000 else army-0x100000000,
            resident=bool(w.call(0x489730,receiver=p)),commandCapacity=w.call(0x48a4f0,receiver=p)&65535,leadership=b[0x170],war=b[0x171],merit=struct.unpack_from('<H',b,0xae)[0]))
    baseline=bytes(w.u.mem_read(0x7200000,0x300000));seed=bytes(w.u.mem_read(0x8a5d44,4));calls=[];sites=[]
    def appoint(u,address,size,user):
        _,building,person=struct.unpack('<3I',u.mem_read(u.reg_read(UC_X86_REG_ESP),12))
        if person!=0 and person not in pointers:raise ValueError('Original appointment pointer outside verified domain')
        calls.append(dict(buildingPointer=hex(building),officerNativeId=pointers[person]if person else None))
    hook=w.u.hook_add(UC_HOOK_CODE,appoint,begin=0x4b3a20,end=0x4b3a20)
    try:
        for i in range(87):
            w.u.mem_write(0x7200000,baseline);p=w.call(0x490d00,i,receiver=w.root)
            if not w.call(0x47a630,p):continue
            def props():
                result={str(f):w.call(0x4c69a0,p,f)&0xffffffff for f in [3,4,13,14]}
                return {k:v if v<0x80000000 else v-0x100000000 for k,v in result.items()}
            before=props();calls.clear();w.call(0x4bca30,p,1,receiver=0x799895c,count=10000000);after=props()
            changed=bytes(w.u.mem_read(0x7200000,0x300000))
            if seed!=bytes(w.u.mem_read(0x8a5d44,4)):raise ValueError('Original governor election consumes RNG')
            sites.append(dict(nativeId=i,before=before,after=after,appointments=list(calls),
                changedBytes=[dict(offset=j,before=a,after=b)for j,(a,b)in enumerate(zip(baseline,changed))if a!=b]))
            print(json.dumps(dict(site=i,before=before['14'],after=after['14'],appointments=calls)),flush=True)
    finally:w.u.hook_del(hook)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceManifestSha256=sha(manifest_raw),sharedSha256=sha(shared),source=source,people=people,sites=sites,
        originalFunction=dict(start='0x4bca30',guardEndExclusive='0x4bccc4',sha256=sha(exe[0xbca30:0xbccc4])),
        selectedAbilityChange=0,effectiveGrowthDisabled=1 if fixed else 0,
        limits=['Each original site election starts from independent493400 baseline, not a full player command/event sequence',
                'Native domain includes unknown/constructed NPC objects; runtime identity join remains separate',
                'Original actor residency and command capacity observed; Android activity/mission policies are not inferred',
                'All87 sites considered; original47a630 controls which site objects admit election'],completeGoal=False)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0));print(json.dumps(dict(sites=len(sites),sha256=sha(output.read_bytes()))),flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--source-index',type=int,choices=range(16),default=0);a=p.parse_args();inspect(a.installation,a.output,a.source_index)
