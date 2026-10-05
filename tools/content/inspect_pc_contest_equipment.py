#!/usr/bin/env python3
"""Actual layered item ownership and effective native debate talk initialization.

Read boundary, before opening events. Original51d940 includes native deck/RNG
initialization; raw actor flags and effective talk pool are kept distinct.
"""
import argparse,gzip,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from inspect_pc_scenario_officers import BASE,STRIDE
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    exe=(installation/'san11pk.exe').read_bytes();manifest_raw=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();manifest=json.loads(manifest_raw)
    shared=(installation/'Media/scenario/Scenario.s11').read_bytes();sources=[]
    for source in manifest['scenarios']:
        raw=(installation/source['sourcePath']).read_bytes()
        if sha(raw)!=source['sourceSha256']:raise ValueError('Original scenario changed')
        d=NativeDebateFlow(installation,exe);w=d.world;shared_read=w.load(shared,True);loaded=w.load(raw)
        shared_items={v['native_index']:v for v in shared_read['records']if v['kind']=='item'}
        items=[]
        for row in loaded['records']:
            if row['kind']!='item':continue
            pointer=row['actor_address'];native=row['native_index'];state=bytes(w.u.mem_read(pointer,0x54))
            kind=struct.unpack_from('<i',state,0x38)[0];owner=struct.unpack_from('<i',state,0x40)[0]
            name=state[4:36].split(b'\0')[0];text=None
            try:text=name.decode('big5')
            except UnicodeDecodeError:pass
            shared_item=shared_items.get(native)
            items.append(dict(nativeItemId=native,originalValid=bool(w.call(0x47a630,pointer)),nativeKind=kind,nativeOwnerId=owner,
                              nameRawHex=name.hex(),name=text,scenarioRecordSha256=row['sha256'],scenarioOffset=row['offset'],scenarioRecordHex=row['raw_hex'],
                              sharedRecordSha256=shared_item['sha256']if shared_item else None,
                              originalItemHex=state.hex(),completeOpeningOwnershipProven=False))
        if len(items)!=100:raise ValueError('Original item array differs')
        world_before=sha(bytes(w.u.mem_read(0x7200000,0x300000)));people=[]
        for native in range(850):
            actor=w.root+0xc0bc+native*0x190;valid=w.call(0x47a600,actor)
            if w.call(0x4883c0,receiver=actor)!=native:raise ValueError('Original person source identity differs')
            raw_flags=[w.call(0x489780,i,receiver=actor)for i in range(5)];raw_mask=sum(bit<<i for i,bit in enumerate(raw_flags))
            if any(bit not in (0,1)for bit in raw_flags):raise ValueError('Original raw talk getter not boolean')
            w.call(0x51d420,receiver=d.fixture);w.u.mem_write(0x8a5d44,struct.pack('<I',23))
            result=w.call(0x51d940,actor,receiver=d.fixture+0x34,count=10000000)
            state=bytes(w.u.mem_read(d.fixture,0xa0));count=struct.unpack_from('<i',state,0x94)[0];pool=list(struct.unpack_from('<5i',state,0x80))
            if not 0<=count<=5 or any(card<10 or card>14 for card in pool[:count]):raise ValueError('Original effective talk pool differs')
            effective=sum(1<<(card-10)for card in set(pool[:count]));books=[i['nativeItemId']for i in items if i['originalValid']and i['nativeKind']==5 and i['nativeOwnerId']==native]
            expected=31 if books else raw_mask
            if valid and (result!=1 or effective!=expected):raise ValueError('Actual source book/raw talk initialization differs '+str(native))
            if not valid and result!=0:raise ValueError('Original invalid actor initializer branch differs')
            record=raw[BASE+native*STRIDE:BASE+(native+1)*STRIDE]
            people.append(dict(nativeId=native,sourceRecordSha256=sha(record),originalActorValid=bool(valid),rawNativeTalkMask=raw_mask,
                               effectiveNativeTalkMask=effective,nativeBookIds=books,originalPool=pool,originalPoolCount=count,
                               nativeRngAfterInitialization=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0],completeOpeningEquipmentProven=False,projectIdentityJoined=False))
        if world_before!=sha(bytes(w.u.mem_read(0x7200000,0x300000))):raise ValueError('Original item/talk initialization mutated source world')
        sources.append(dict(sourceVariant=source['sourceVariant'],scenarioId=source['scenarioId'],sourcePath=source['sourcePath'],sourceSha256=source['sourceSha256'],items=items,people=people))
        print(json.dumps(dict(path=source['sourcePath'],validItems=sum(i['originalValid']for i in items),bookCarriers=sum(bool(p['nativeBookIds'])for p in people if p['originalActorValid']),effectiveDifferences=sum(p['rawNativeTalkMask']!=p['effectiveNativeTalkMask']for p in people if p['originalActorValid'])),ensure_ascii=False),flush=True)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceManifestSha256=sha(manifest_raw),sharedSha256=sha(shared),sources=sources,
                originalInitializer='51d940 actual actor/actual item owner ->original talk pool and deck shuffle',
                limits=['Serializer read boundary; complete postload/opening event ownership changes unknown',
                        'Native item kind5 granting all talks is proven by actual original branch, not inferred from names',
                        'Native owner IDs are original IDs; project IDs require the separate validated identity mapping',
                        'Native initialization RNG recorded from explicit seed23; no campaign RNG or APK integration proof'])
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps(dict(records=sum(len(s['people'])for s in sources),items=sum(len(s['items'])for s in sources),sha256=sha(output.read_bytes()),completeContest=False)))
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();inspect(a.installation,a.output)
