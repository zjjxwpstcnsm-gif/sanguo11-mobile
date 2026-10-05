#!/usr/bin/env python3
"""Execute original settlement with explicit unit-guidance memory fixtures.

Shared/Scen000 identities, skills and active states remain source values.
Fixture unit crews/location are not asserted to be original opening units.
"""
import argparse,gzip,itertools,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha

CONTEXTS=[('guide-leader',116,222,[(9,116,-1)], [87,0],[True,False]),
          ('guide-deputy-one',116,222,[(116,9,-1)],[87,0],[True,False]),
          ('guide-deputy-two',116,222,[(116,-1,9)],[87,0],[True,False]),
          ('self-guide-only',9,222,[(9,-1,-1)],[87,0],[False,False]),
          ('inactive-companion',116,222,[(116,367,-1)],[87,0],[False,False]),
          ('nonunit-recipient',116,222,[(116,9,-1)],[0,0],[False,False]),
          ('other-skill-companion',116,222,[(116,222,-1)],[87,0],[False,False]),
          ('both-guided',116,222,[(116,9,-1),(222,669,-1)],[87,88],[True,True])]

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    manifest_raw=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes()
    source=next(s for s in json.loads(manifest_raw)['scenarios'] if s['sourcePath']=='Media/scenario/Scen000.s11')
    raw=(installation/source['sourcePath']).read_bytes();shared=(installation/'Media/scenario/Scenario.s11').read_bytes()
    if sha(raw)!=source['sourceSha256']:raise ValueError('Source changed')
    d=NativeDebateFlow(installation,(installation/'san11pk.exe').read_bytes());w=d.world
    w.load(shared,True);loaded=w.load(raw);w.call(0x73c500)
    w.u.mem_map(0x7500000,0x200000);w.call(0x49b490,receiver=0x767cab8)
    records={r['native_index']:r for r in loaded['records'] if r['kind']=='officer'}
    actor=lambda native:w.root+0xc0bc+native*0x190
    def people(ids):
        rows=[]
        for native in ids:
            p=actor(native);b=bytes(w.u.mem_read(p,0x190));vtable=struct.unpack_from('<I',b)[0]
            getter=struct.unpack('<I',w.u.mem_read(vtable+0x40,4))[0];owner=w.call(getter,receiver=p);owner=owner if owner<0x80000000 else owner-0x100000000
            rows.append(dict(nativeId=native,recordSha256=records[native]['sha256'],actorHex=b.hex(),
                rewardEligible=bool(w.call(0x47a630,p)),partyGuidance=bool(w.call(0x4a54a0,p)),
                merit=struct.unpack_from('<H',b,0xae)[0],intelligenceExperience=w.call(0x489180,2,receiver=p)&0xffff,
                injury=struct.unpack_from('<i',b,0x15c)[0],owner=owner,ownerGetter=hex(getter)))
        return rows
    def forces():return [w.call(0x4c4260,w.root+0x7af8+i*0x12c,16) for i in range(47)]
    baseline=bytes(w.u.mem_read(0x7200000,0x300000));cases=[]
    companions=[dict(nativeId=i,recordSha256=records[i]['sha256'],eligible=bool(w.call(0x47a630,actor(i))),
                     originalSkill84=bool(w.call(0x4890f0,84,receiver=actor(i)))) for i in [9,367,669,116,222]]
    for context,left,right,crews,locations,expected in CONTEXTS:
        for winner,outcome,boundary in itertools.product(range(2),range(4),range(3)):
            w.u.mem_write(0x7200000,baseline);ids=[left,right]
            for index,crew in enumerate(crews):w.u.mem_write(w.root+0x169730+index*0xf4+0xc,struct.pack('<3i',*crew))
            for native,location in zip(ids,locations):w.u.mem_write(actor(native)+0x9c,struct.pack('<i',location))
            if boundary:
                for native in ids:
                    w.call(0x48a7a0,59995,receiver=actor(native));w.call(0x48a810,2,2995,receiver=actor(native));w.call(0x48a8e0,2 if boundary==1 else 3,receiver=actor(native))
            actual=[bool(w.call(0x4a54a0,actor(i))) for i in ids]
            if actual!=expected:raise ValueError('Original guidance differs '+context+' '+str(actual))
            w.u.mem_write(0x8b5214+0x28,struct.pack('<I',1))
            if w.call(0x51e220,*[actor(i) for i in ids],0,receiver=0x8b5214)!=1:raise ValueError('Original input rejected')
            w.u.mem_write(0x8a5d44,struct.pack('<I',23));before=people(ids);points=forces();world_before=bytes(w.u.mem_read(0x7200000,0x300000))
            w.call(0x51dd10,winner,outcome,receiver=0x8b5214,count=10000000)
            after=people(ids);next_points=forces();world_after=bytes(w.u.mem_read(0x7200000,0x300000))
            rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
            if rng!=23:raise ValueError('Unexpected source settlement RNG')
            cases.append(dict(context=context,winner=winner,outcome=outcome,boundary=boundary,before=before,after=after,
                crews=crews,locations=locations,forcesBefore=points,forcesAfter=next_points,
                originalRngBefore=23,originalRngAfter=rng,worldBeforeSha256=sha(world_before),worldAfterSha256=sha(world_after),
                worldChangedBytes=[dict(offset=i,before=a,after=b)for i,(a,b)in enumerate(zip(world_before,world_after))if a!=b]))
        print(json.dumps(dict(context=context,cases=24,originalGuidance=expected)),flush=True)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceManifestSha256=sha(manifest_raw),sharedSha256=sha(shared),
        sources=[dict(**source,cases=cases)],originalCompanions=companions,
        functions=[dict(start=hex(a),endExclusive=hex(z),sha256=sha(bytes(w.u.mem_read(a,z-a))))for a,z in [(0x4a54a0,0x4a553a),(0x51dd10,0x51dfa5),(0x4a70d0,0x4a7277)]],
        limits=['Only actual Shared/Scen000 source identities, skill84 and active states',
                'Unit crews and recipient locations explicitly assigned in VM memory; not actual opening units',
                'Complete original settlement/XP/guidance functions execute without replacement; UI pointer zero',
                'Original campaign event result, injury recovery and normal GUI remain separate'],completeGoal=False)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps(dict(cases=len(cases),sha256=sha(output.read_bytes()))),flush=True)
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
