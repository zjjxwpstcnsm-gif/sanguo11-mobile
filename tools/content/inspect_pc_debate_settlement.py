#!/usr/bin/env python3
"""Execute original debate settlement on actual layered source people/forces.

Read-boundary world, original empty message context and zero UI are explicit.
No source writes, Wine, substituted settlement/XP/merit/RNG functions.
"""
import argparse,gzip,itertools,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output);exe=(installation/'san11pk.exe').read_bytes()
    manifest_raw=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();manifest=json.loads(manifest_raw)
    shared=(installation/'Media/scenario/Scenario.s11').read_bytes();reports=[]
    for source in manifest['scenarios']:
        raw=(installation/source['sourcePath']).read_bytes()
        if sha(raw)!=source['sourceSha256']:raise ValueError('Source scenario changed')
        d=NativeDebateFlow(installation,exe);w=d.world;w.load(shared,True);loaded=w.load(raw)
        w.call(0x73c500) # Original force property descriptor/getter initializer.
        w.u.mem_map(0x7500000,0x200000);w.call(0x49b490,receiver=0x767cab8)
        actors=[w.root+0xc0bc+i*0x190 for i in (116,222)]
        for actor,native in zip(actors,(116,222)):
            if w.call(0x4883c0,receiver=actor)!=native or not w.call(0x47a600,actor):raise ValueError('Actual source actor identity/validity differs')
        records={r['native_index']:r for r in loaded['records']if r['kind']=='officer'}
        baseline=bytes(w.u.mem_read(w.root,0x300000));cases=[]
        def people():
            result=[]
            for actor,native in zip(actors,(116,222)):
                b=bytes(w.u.mem_read(actor,0x190));vtable=struct.unpack_from('<I',b)[0];owner_getter=struct.unpack('<I',w.u.mem_read(vtable+0x40,4))[0];owner=w.call(owner_getter,receiver=actor);owner=owner if owner<0x80000000 else owner-0x100000000
                result.append(dict(nativeId=native,recordSha256=records[native]['sha256'],
                    rewardEligible=bool(w.call(0x47a630,actor)),partyGuidance=bool(w.call(0x4a54a0,actor)),
                    actorHex=b.hex(),merit=struct.unpack_from('<H',b,0xae)[0],
                    intelligenceExperience=w.call(0x489180,2,receiver=actor)&0xffff,
                    injury=struct.unpack_from('<i',b,0x15c)[0],owner=owner,ownerGetter=hex(owner_getter)))
            return result
        def forces():return[w.call(0x4c4260,w.root+0x7af8+i*0x12c,16)for i in range(47)]
        for winner,outcome,boundary in itertools.product(range(2),range(4),range(3)):
            w.u.mem_write(w.root,baseline)
            # Explicit boundary fixtures use original setters; never alter a
            # source record or fabricate its decoded identity.
            if boundary:
                for actor in actors:
                    w.call(0x48a7a0,59995,receiver=actor);w.call(0x48a810,2,2995,receiver=actor)
                    w.call(0x48a8e0,2 if boundary==1 else 3,receiver=actor)
            w.u.mem_write(0x8b5214+0x28,struct.pack('<I',1))
            if w.call(0x51e220,*actors,0,receiver=0x8b5214)!=1:raise ValueError('Original source debate input rejected')
            w.u.mem_write(0x8a5d44,struct.pack('<I',23));before=people();before_forces=forces()
            world_before=bytes(w.u.mem_read(w.root,0x300000))
            try:w.call(0x51dd10,winner,outcome,receiver=0x8b5214,count=10000000)
            except Exception as e:raise ValueError('Original settlement failed '+source['sourcePath']+' '+str((winner,outcome,boundary))+' '+repr(w.invalid))from e
            after=people();after_forces=forces();world_after=bytes(w.u.mem_read(w.root,0x300000))
            rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
            if rng!=23:raise ValueError('Original source settlement unexpected RNG draws')
            changed=[dict(offset=i,before=old,after=new)for i,(old,new)in enumerate(zip(world_before,world_after))if old!=new]
            cases.append(dict(winner=winner,outcome=outcome,boundary=boundary,before=before,after=after,
                              forcesBefore=before_forces,forcesAfter=after_forces,
                              originalRngBefore=23,originalRngAfter=rng,worldBeforeSha256=sha(world_before),
                              worldAfterSha256=sha(world_after),worldChangedBytes=changed,
                              receipt=bytes(w.u.mem_read(0x8b5214,0x2c)).hex()))
        reports.append(dict(scenarioId=source['scenarioId'],sourceVariant=source['sourceVariant'],sourcePath=source['sourcePath'],sourceSha256=source['sourceSha256'],cases=cases))
        print(json.dumps(dict(source=source['sourcePath'],cases=len(cases))),flush=True)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceManifestSha256=sha(manifest_raw),sharedSha256=sha(shared),sources=reports,
                functions=[dict(start=hex(a),endExclusive=hex(b),sha256=sha(exe[a-0x400000:b-0x400000]))for a,b in[(0x51dd10,0x51dfa5),(0x4a70d0,0x4a7270),(0x4a6d50,0x4a6db0),(0x4a5690,0x4a56b2),(0x4b6580,0x4b65d3)]],
                limits=['Actual installation source actors116/222 and forces, before complete opening events',
                        'Boundary fixtures use original XP/merit/injury setters; no invented project identity',
                        'Original UI pointer remains zero; source message context empty; no normal GUI proof',
                        'Recruitment ownership/diplomatic event callback/ordinary party guidance and injury recovery still separate',
                        'Full original51dd10 executed; original setters and RNG not substituted'],completeGoal=False)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0));print(json.dumps(dict(cases=sum(len(s['cases'])for s in reports),sha256=sha(output.read_bytes()))),flush=True);return report
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
