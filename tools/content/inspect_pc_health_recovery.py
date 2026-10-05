#!/usr/bin/env python3
"""Execute complete original580e70 under explicit date/health/lifetime fixtures.

Original source people and serializer are retained. Lifetime mode3 and dates
are explicit verification inputs, not certified original new-game settings.
"""
import argparse,gzip,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_ECX
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,sha,json_bytes

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output);exe=(installation/'san11pk.exe').read_bytes()
    manifest_raw=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();shared=(installation/'Media/scenario/Scenario.s11').read_bytes();sources=[]
    for source in json.loads(manifest_raw)['scenarios']:
        raw=(installation/source['sourcePath']).read_bytes()
        if sha(raw)!=source['sourceSha256']:raise ValueError('Source changed')
        w=NativeDebateFlow(installation,exe).world;pages=load_original_data(w.u);w.load(shared,True);w.load(raw);w.call(0x49b490,receiver=0x767cab8)
        # Explicit original lifetime option3: no healthy-age illness branch.
        w.u.mem_write(w.root+0x38,struct.pack('<i',3));w.u.mem_write(w.root+0x5c,bytes(4))
        baseline=bytes(w.u.mem_read(0x7200000,0x300000));rng_calls=[]
        def draw(u,address,size,user):
            ret,chance=struct.unpack('<II',u.mem_read(u.reg_read(UC_X86_REG_ESP),8));rng_calls.append(dict(function=hex(address),returnAddress=hex(ret),chance=chance))
        w.u.hook_add(UC_HOOK_CODE,draw,begin=0x4721d0,end=0x4721d0)
        def record(native):
            p=w.root+0xc0bc+native*0x190;b=bytes(w.u.mem_read(p,0x190));status=struct.unpack_from('<i',b,0xa0)[0]
            return dict(nativeId=native,status=status,originalActive=bool(w.call(0x47a630,p)),ancientExcluded=bool(w.call(0x489ce0,receiver=p)),
                originalStatusAllowed=bool(w.call(0x489fe0,0x3f,receiver=p)),injury=struct.unpack_from('<i',b,0x15c)[0],
                current=list(b[0x170:0x175]),actorHex=b.hex())
        cases=[]
        for native in [116,101]:
            w.u.mem_write(0x7200000,baseline);p=w.root+0xc0bc+native*0x190;w.call(0x48a8e0,3,receiver=p);w.u.mem_write(0x8a5d44,struct.pack('<I',23));trajectory=[]
            for month in range(1,13):
                for f,v in [(0x4826e0,184),(0x482700,month),(0x482720,1)]:w.call(f,v,receiver=w.root)
                before=record(native);world_before=bytes(w.u.mem_read(0x7200000,0x300000));seed=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0];rng_calls.clear()
                w.call(0x580e70,count=10000000);after=record(native);world_after=bytes(w.u.mem_read(0x7200000,0x300000));next_seed=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
                trajectory.append(dict(month=month,originalMonth=w.call(0x4824f0,receiver=w.root),originalMonthStart=bool(w.call(0x482680,receiver=w.root)),
                    before=before,after=after,seedBefore=seed,seedAfter=next_seed,originalDrawCalls=list(rng_calls),
                    worldBeforeSha256=sha(world_before),worldAfterSha256=sha(world_after),
                    worldChangedBytes=[dict(offset=i,before=a,after=b)for i,(a,b)in enumerate(zip(world_before,world_after))if a!=b]))
            cases.append(dict(nativeId=native,lifetimeMode=3,year=184,trajectory=trajectory))
        # Original admission states and invalid/ancient/native parity boundaries.
        edges=[]
        if source['sourcePath']=='Media/scenario/Scen000.s11':
            for native in [116,101,700,800,850,1000]:
                for status in range(9):
                    for injury in [-1,0,1,3]:
                        for month in [1,2]:
                            w.u.mem_write(0x7200000,baseline);p=w.root+0xc0bc+native*0x190
                            # Only the selected source object's explicit fixture
                            # status/health is changed. Native identity is never hooked.
                            w.u.mem_write(p+0xa0,struct.pack('<i',status));w.call(0x48a8e0,injury&0xffffffff,receiver=p)
                            for f,v in [(0x4826e0,184),(0x482700,month),(0x482720,1)]:w.call(f,v,receiver=w.root)
                            before=record(native);w.u.mem_write(0x8a5d44,struct.pack('<I',23));rng_calls.clear();w.call(0x580e70,count=10000000);after=record(native)
                            edges.append(dict(month=month,before=before,after=after,seedBefore=23,seedAfter=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],originalDrawCalls=list(rng_calls)))
        sources.append(dict(**source,originalDataPages=pages,cases=cases,admissionFixtures=edges));print(json.dumps(dict(source=source['sourcePath'],frames=24,admissionFixtures=len(edges))),flush=True)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceManifestSha256=sha(manifest_raw),sharedSha256=sha(shared),sources=sources,
        functions=[dict(start=hex(a),endExclusive=hex(z),sha256=sha(exe[a-0x400000:z-0x400000]))for a,z in [(0x580e70,0x581090),(0x482680,0x4826bc),(0x489ce0,0x489d02),(0x489fe0,0x489ff7),(0x48a8e0,0x48a8fe)]],
        limits=['Explicit lifetime mode3, year184/month1..12/day1 and selected injury/status memory fixtures',
                'Complete original580e70 executes, not only a setter or selected recovery branch',
                'Original590c30 caller month-start/lifecycle gate inspected; complete GUI/month dispatcher separate',
                'Healthy-age illness and other lifetime settings are not claimed by this mode3 report'],completeGoal=False)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0));print(json.dumps(dict(trajectoryFrames=384,admissionCases=432,sha256=sha(output.read_bytes()))),flush=True)
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
