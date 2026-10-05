#!/usr/bin/env python3
"""Original duel fighter initialization against each actual layered source.

HP/spirit/stance are explicit fixture inputs. Only original gear enumeration is
audited; read-boundary equipment is not certified as complete post-opening gear.
"""
import argparse, gzip, json, struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from inspect_pc_scenario_officers import BASE, STRIDE
from audit_pc_restoration_sources import ROOT, EXE_SHA, output_guard, json_bytes, sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    exe=(installation/'san11pk.exe').read_bytes();manifest_raw=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes()
    manifest=json.loads(manifest_raw);shared=(installation/'Media/scenario/Scenario.s11').read_bytes();sources=[]
    for source in manifest['scenarios']:
        raw=(installation/source['sourcePath']).read_bytes()
        if sha(raw)!=source['sourceSha256']:raise ValueError('Actual scenario SHA changed')
        d=NativeDebateFlow(installation,exe);w=d.world;w.load(shared,True);w.load(raw)
        w.call(0x50ab90,receiver=d.fixture,count=10000000)
        world_before=sha(bytes(w.u.mem_read(0x7200000,0x300000)));rng=bytes(w.u.mem_read(0x8a5d44,4));people=[]
        for native in range(850):
            actor=w.root+0xc0bc+native*0x190
            valid=w.call(0x47a600,actor)
            original_id=w.call(0x4883c0,receiver=actor)
            if original_id!=native:raise ValueError('Original person identity did not match source slot '+str(native))
            w.call(0x50ce00,actor,100,0,0,receiver=d.fixture+0x24,count=10000000)
            state=bytes(w.u.mem_read(d.fixture+0x24,0x40));pointer=struct.unpack_from('<I',state)[0];mask=struct.unpack_from('<I',state,0x1c)[0]
            if valid not in (0,1) or (valid==1 and pointer!=actor) or (valid==0 and pointer!=0):raise ValueError('Original fighter validity branch differs')
            offset=BASE+native*STRIDE;record=raw[offset:offset+STRIDE]
            people.append(dict(nativeId=native,sourceRecordSha256=sha(record),originalActorValid=bool(valid),
                               initializedActorPointer=hex(pointer),nativeGearMask=mask,originalFighterHex=state.hex(),
                               completeOpeningEquipmentProven=False,projectIdentityJoined=False))
        if rng!=bytes(w.u.mem_read(0x8a5d44,4)) or world_before!=sha(bytes(w.u.mem_read(0x7200000,0x300000))):raise ValueError('Original fighter extraction mutated loaded world/RNG')
        sources.append(dict(sourceVariant=source['sourceVariant'],scenarioId=source['scenarioId'],sourcePath=source['sourcePath'],sourceSha256=source['sourceSha256'],people=people))
        print(json.dumps(dict(path=source['sourcePath'],valid=sum(p['originalActorValid']for p in people),carrying=sum(bool(p['nativeGearMask'])for p in people)),ensure_ascii=False),flush=True)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceManifestSha256=sha(manifest_raw),sharedSha256=sha(shared),sources=sources,
                originalModelConstructor='50ab90',originalFighterInitializer='50ce00 ->50ccb0 equipment enumeration4cdf40 ->50c080',
                limits=['Actual layered serializer read boundary, before original postload and opening events',
                        'Fighter HP100/spirit0/stance0 are explicit fixtures, not source campaign initial states',
                        'Native mask bits retained; effect/availability and complete equipment priority still require original rules',
                        'No canonical ID assignment, APK integration, Wine or PC writes'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps(dict(records=sum(len(s['people'])for s in sources),sha256=sha(output.read_bytes()),completeDuel=False)))
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',required=True,type=Path)
    a=p.parse_args();inspect(a.installation,a.output)
