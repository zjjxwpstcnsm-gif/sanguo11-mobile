#!/usr/bin/env python3
"""Execute original numeric rewards and full recruitment-result callback chain.

Original16 layered read-boundary sources; zero player GUI context. No reward,
allegiance, loyalty, notification or RNG function is replaced.
"""
import argparse,gzip,itertools,json,struct
from pathlib import Path
from unicorn import UC_HOOK_CODE
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from session_b_pc_geography_context import load_geography
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output,postload=False,only_source=None,source_date=False,ability_change=None,geography=False):
    installation=installation.resolve();output_guard(installation,output);exe=(installation/'san11pk.exe').read_bytes()
    manifest_raw=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();shared=(installation/'Media/scenario/Scenario.s11').read_bytes();sources=[]
    for source in json.loads(manifest_raw)['scenarios']:
        if only_source and source['sourcePath']!=only_source:continue
        raw=(installation/source['sourcePath']).read_bytes()
        if sha(raw)!=source['sourceSha256']:raise ValueError('Source changed')
        w=NativeDebateFlow(installation,exe).world;pages=load_original_data(w.u)
        w.load(shared,True);loaded=w.load(raw);w.call(0x73c500);w.call(0x73ca80);w.call(0x49b490,receiver=0x767cab8)
        if source_date:
            for function,value in zip([0x4826e0,0x482700,0x482720],source['date']):w.call(function,value,receiver=w.root)
        settings=None
        if ability_change is not None:
            if not postload or not source_date:raise ValueError('Explicit ability option requires source date and postload')
            fixed=struct.unpack('<i',w.u.mem_read(w.root+0x18,4))[0]
            w.call(0x4827b0,ability_change,receiver=w.root)
            if fixed:
                w.call(0x4827b0,1,receiver=w.root);w.call(0x493f70,1,receiver=w.root);w.call(0x4827f0,3,receiver=w.root)
            effective=struct.unpack('<i',w.u.mem_read(w.root+0x28,4))[0]
            if effective!=(1 if fixed else ability_change):raise ValueError('Original ability option setter differs')
            settings=dict(requestedAbilityChange=ability_change,sourceFixedAgeFlag=fixed,effectiveGrowthDisabled=effective,
                setters=['0x4827b0','0x493f70','0x4827f0'],source='explicit verified option; source user preference remains unknown')
        geography_context=load_geography(w,installation)if geography else None
        if postload:w.call(0x493400,receiver=w.root,count=50000000)
        actors=[w.root+0xc0bc+i*0x190 for i in [116,222]];records={r['native_index']:r for r in loaded['records']if r['kind']=='officer'}
        fields=[3,4,20,21,23,24,25,26,27,28,29,30,31,32,33,34,40,41,44,45,49]
        trace=[]
        def enter(u,address,size,user):trace.append(hex(address))
        for a in [0x51dd10,0x5d3d40,0x5d3c90,0x5c4840,0x4a75a0]:w.u.hook_add(UC_HOOK_CODE,enter,begin=a,end=a)
        def people():
            rows=[];before=bytes(w.u.mem_read(0x7200000,0x300000));rng=bytes(w.u.mem_read(0x8a5d44,4))
            for native,p in zip([116,222],actors):
                b=bytes(w.u.mem_read(p,0x190));vtable=struct.unpack_from('<I',b)[0];getter=struct.unpack('<I',w.u.mem_read(vtable+0x40,4))[0]
                owner=w.call(getter,receiver=p);owner=owner if owner<0x80000000 else owner-0x100000000
                values={str(f):w.call(0x4c8720,p,f)&0xffffffff for f in fields};values={k:v if v<0x80000000 else v-0x100000000 for k,v in values.items()}
                rows.append(dict(nativeId=native,recordSha256=records[native]['sha256'],actorHex=b.hex(),owner=owner,properties=values,
                    merit=struct.unpack_from('<H',b,0xae)[0],injury=struct.unpack_from('<i',b,0x15c)[0],rawLoyalty=b[0xac],
                    experience=list(struct.unpack_from('<5H',b,0x12a)),current=list(b[0x170:0x175]),
                    rewardEligible=bool(w.call(0x47a630,p)),partyGuidance=bool(w.call(0x4a54a0,p))))
            if before!=bytes(w.u.mem_read(0x7200000,0x300000))or rng!=bytes(w.u.mem_read(0x8a5d44,4)):raise ValueError('Query mutates original authority')
            return rows
        def forces():return [w.call(0x4c4260,w.root+0x7af8+i*0x12c,16)for i in range(47)]
        def sites():
            rows=[];world_before=bytes(w.u.mem_read(0x7200000,0x300000));seed_before=bytes(w.u.mem_read(0x8a5d44,4))
            for native in range(87):
                p=w.call(0x490d00,native,receiver=w.root);values={str(f):w.call(0x4c69a0,p,f)&0xffffffff for f in [3,4,13,14]}
                rows.append(dict(nativeId=native,properties={k:v if v<0x80000000 else v-0x100000000 for k,v in values.items()}))
            if world_before!=bytes(w.u.mem_read(0x7200000,0x300000))or seed_before!=bytes(w.u.mem_read(0x8a5d44,4)):raise ValueError('Site result query mutates original state')
            return rows
        baseline=bytes(w.u.mem_read(0x7200000,0x300000));cases=[]
        for winner,outcome in itertools.product(range(2),range(4)):
            w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8b5214+0x28,struct.pack('<I',1))
            if w.call(0x51e220,*actors,0,receiver=0x8b5214)!=1:raise ValueError('Original debate input rejected')
            w.u.mem_write(0x8a5d44,struct.pack('<I',23));before=people();points_before=forces();world_before=bytes(w.u.mem_read(0x7200000,0x300000));trace.clear()
            site_before=sites()if postload else None
            w.call(0x51dd10,winner,outcome,receiver=0x8b5214,count=10000000)
            battle=people();points_battle=forces();battle_rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
            manager=bytes(w.u.mem_read(0x8b5214,0x2c));success=bool(w.call(0x51dc30,receiver=0x8b5214))
            w.call(0x5d3d40,count=10000000)
            after=people();points_after=forces();world_after=bytes(w.u.mem_read(0x7200000,0x300000));rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
            cases.append(dict(winner=winner,outcome=outcome,success=success,before=before,afterBattle=battle,after=after,
                forcesBefore=points_before,forcesAfterBattle=points_battle,forcesAfter=points_after,originalCalls=list(trace),managerReceipt=manager.hex(),
                originalRngBefore=23,originalRngAfterBattle=battle_rng,originalRngAfter=rng,
                worldBeforeSha256=sha(world_before),worldAfterSha256=sha(world_after),
                worldChangedBytes=[dict(offset=i,before=a,after=b)for i,(a,b)in enumerate(zip(world_before,world_after))if a!=b]))
            if postload:cases[-1].update(sitesBefore=site_before,sitesAfter=sites())
        entry=dict(**source,originalDataPages=pages,cases=cases)
        if geography_context is not None:entry["geographyContext"]=geography_context
        if settings is not None:entry['explicitAbilitySettings']=settings
        if postload:entry['directPostload493400']=True
        sources.append(entry);print(json.dumps(dict(source=source['sourcePath'],cases=8)),flush=True)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceManifestSha256=sha(manifest_raw),sharedSha256=sha(shared),sources=sources,
        functions=[dict(start=hex(a),endExclusive=hex(z),sha256=sha(exe[a-0x400000:z-0x400000]))for a,z in [(0x51dd10,0x51dfa5),(0x5d3d40,0x5d3d8c),(0x5d3c90,0x5d3d3d),(0x5c4840,0x5c4e6f),(0x4a75a0,0x4a77b0)]],
        limits=['Actual Shared+16 installation sources, at read boundary before complete opening events',
                'Original empty message context49b490 constructed; player GUI/controllers and script continuation not executed',
                'Complete original51dd10 and5d3d40 including recruitment/rewards/notifications; no game-function replacement',
                'Complete diplomatic result and injury recovery remain separate'],completeGoal=False)
    if postload:
        report['directPostload493400']=True
        report['limits'][0]='Actual Shared+selected installation sources after complete direct493400 postload; full opening events/settings remain separate'
    if source_date:report['explicitSourceDate']=True
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0));print(json.dumps(dict(cases=sum(len(s['cases'])for s in sources),sha256=sha(output.read_bytes()))),flush=True)
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--geography',action='store_true');p.add_argument('--postload',action='store_true');p.add_argument('--only-source');p.add_argument('--source-date',action='store_true');p.add_argument('--ability-change',type=int,choices=[0,1]);a=p.parse_args();inspect(a.installation,a.output,a.postload,a.only_source,a.source_date,a.ability_change,a.geography)
