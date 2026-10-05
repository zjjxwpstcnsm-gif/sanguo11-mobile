#!/usr/bin/env python3
"""Execute original recruitment-result function referenced by debate callback.

Read-boundary source participants and coordinates; GUI/player controller and
script scheduler are not claimed to be complete original new-game contexts.
"""
import argparse,gzip,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha
from pc_original_pe_data import load_original_data

def inspect(installation,output,complete_pe_data=False):
    installation=installation.resolve();output_guard(installation,output)
    manifest_raw=(ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes();exe=(installation/'san11pk.exe').read_bytes()
    shared=(installation/'Media/scenario/Scenario.s11').read_bytes();sources=[]
    for source in json.loads(manifest_raw)['scenarios']:
        raw=(installation/source['sourcePath']).read_bytes()
        if sha(raw)!=source['sourceSha256']:raise ValueError('Source changed')
        w=NativeDebateFlow(installation,exe).world
        pages=load_original_data(w.u)if complete_pe_data else []
        w.load(shared,True);loaded=w.load(raw);w.call(0x73c500);w.call(0x73ca80)
        records={r['native_index']:r for r in loaded['records']if r['kind']=='officer'}
        actors=[w.root+0xc0bc+i*0x190 for i in [116,222]]
        fields=[20,21,22,23,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51]
        labels={}
        for field in fields:
            pointer=struct.unpack('<I',w.u.mem_read(0x8ab758+field*16,4))[0]
            labels[str(field)]=bytes(w.u.mem_read(pointer,128)).split(b'\0')[0].decode('big5')
        def people():
            rows=[];prior=bytes(w.u.mem_read(0x7200000,0x300000));seed=bytes(w.u.mem_read(0x8a5d44,4))
            for native,p in zip([116,222],actors):
                values={str(f):w.call(0x4c8720,p,f)&0xffffffff for f in fields}
                values={k:v if v<0x80000000 else v-0x100000000 for k,v in values.items()}
                rows.append(dict(nativeId=native,recordSha256=records[native]['sha256'],actorHex=bytes(w.u.mem_read(p,0x190)).hex(),
                                 originalActive=bool(w.call(0x47a630,p)),properties=values))
            if prior!=bytes(w.u.mem_read(0x7200000,0x300000))or seed!=bytes(w.u.mem_read(0x8a5d44,4)):raise ValueError('Query changed source world/RNG')
            return rows
        baseline=bytes(w.u.mem_read(0x7200000,0x300000));cases=[]
        for success in [False,True]:
            w.u.mem_write(0x7200000,baseline);w.u.mem_write(0x8a5d44,struct.pack('<I',23))
            coordinate=struct.unpack('<I',w.u.mem_read(actors[1]+0x98,4))[0]
            before=people();world_before=bytes(w.u.mem_read(0x7200000,0x300000))
            # Exact arguments from original5d3c90 ->5c4840. The caller's
            # remaining rewards/notification and script continuations are separate.
            failure=None
            try:w.call(0x5c4840,*actors,coordinate,int(success),1,count=10000000)
            except Exception as error:
                failure=dict(error=str(error),invalidMemory=list(w.invalid))
                print(json.dumps(dict(source=source['sourcePath'],success=success,failure=failure)),flush=True)
            after=people();world_after=bytes(w.u.mem_read(0x7200000,0x300000));rng=struct.unpack('<I',w.u.mem_read(0x8a5d44,4))[0]
            cases.append(dict(success=success,complete=failure is None,failure=failure,originalFunction='5c4840',coordinate=coordinate,alreadyPaid=1,
                before=before,after=after,originalRngBefore=23,originalRngAfter=rng,
                worldBeforeSha256=sha(world_before),worldAfterSha256=sha(world_after),
                worldChangedBytes=[dict(offset=i,before=a,after=b)for i,(a,b)in enumerate(zip(world_before,world_after))if a!=b]))
        sources.append(dict(**source,originalDataPages=pages,propertyLabels=labels,cases=cases));print(json.dumps(dict(source=source['sourcePath'],cases=2)),flush=True)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceManifestSha256=sha(manifest_raw),sharedSha256=sha(shared),sources=sources,
        functions=[dict(start=hex(a),endExclusive=hex(z),sha256=sha(exe[a-0x400000:z-0x400000]))for a,z in [(0x5c4840,0x5c4e6f),(0x5d3c90,0x5d3d3d),(0x5d3d40,0x5d3d8c),(0x51dc30,0x51dca3)]],
        limits=['Actual Shared/scenario116/222 and source target coordinate; read boundary before full opening events',
                'Only cases marked complete executed full5c4840; failed cases are partial/unknown and not settlement evidence',
                'No player GUI/controller setup or script continuation proof; original5d3c90 remaining rewards/notifications separate',
                'This function is referenced by recruitment debate result; diplomatic result uses separate scripts'],completeGoal=False)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps(dict(cases=32,complete=sum(c['complete']for s in sources for c in s['cases']),sha256=sha(output.read_bytes()))),flush=True)
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--complete-pe-data',action='store_true');a=p.parse_args();inspect(a.installation,a.output,a.complete_pe_data)
