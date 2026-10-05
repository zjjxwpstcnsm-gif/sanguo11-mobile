#!/usr/bin/env python3
"""Read original runtime loyalty/ban/captive fields from16 actual layered loads.

Identity joins use the saved checked runtime registry, never slot arithmetic.
The read boundary precedes complete startup events; external overrides stay unknown.
"""
import argparse,gzip,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output);exe=(installation/'san11pk.exe').read_bytes()
    folder=ROOT/'docs/handoff/20261004/session1';manifest_raw=(folder/'source-manifest.json').read_bytes();registry_raw=(folder/'scenario-person-runtime-coverage.json.gz').read_bytes()
    registry=json.loads(gzip.decompress(registry_raw));ids={(r['sourcePath'],r['nativeId']):r for r in registry['people']}
    shared=(installation/'Media/scenario/Scenario.s11').read_bytes();sources=[]
    for source in json.loads(manifest_raw)['scenarios']:
        raw=(installation/source['sourcePath']).read_bytes()
        if sha(raw)!=source['sourceSha256']:raise ValueError('Original source changed')
        w=NativeDebateFlow(installation,exe).world;pages=load_original_data(w.u);w.load(shared,True);loaded=w.load(raw);w.call(0x73c500)
        records={r['native_index']:r for r in loaded['records']if r['kind']=='officer'};before=bytes(w.u.mem_read(0x7200000,0x300000));seed=bytes(w.u.mem_read(0x8a5d44,4));rows=[]
        for native in range(850):
            p=w.root+0xc0bc+native*0x190;b=bytes(w.u.mem_read(p,0x190));record=records[native];identity=ids.get((source['sourcePath'],native));officer_id=identity['officerId']if identity else -1
            if identity and (identity['recordSha256']!=record['sha256']or identity['sourceVariant']!=source['sourceVariant']):raise ValueError('Checked identity record differs')
            values={str(f):w.call(0x4c8720,p,f)&0xffffffff for f in [20,23,58,59,61]};values={k:v if v<0x80000000 else v-0x100000000 for k,v in values.items()}
            rows.append(dict(officerId=officer_id,nativeId=native,recordSha256=record['sha256'],rawLoyalty=b[0xac],banRulerNativeId=struct.unpack_from('<i',b,0x164)[0],banMonths=b[0x168],captiveMonths=b[0x169],properties=values,originalAllowed=bool(w.call(0x47a630,p))))
        forces=[]
        for native in range(47):
            p=w.root+0x7af8+native*0x12c;relations=[]
            for other in range(47):
                relation=w.call(0x4c4260,p,177+other)&0xffffffff;relation=relation if relation<0x80000000 else relation-0x100000000
                friendship=w.call(0x4814e0,other,receiver=p)&255;ally=bool(w.call(0x4811b0,other,receiver=p))
                relations.append(dict(nativeId=other,relation=relation,allied=ally,friendship=friendship))
            forces.append(dict(nativeId=native,allowed=bool(w.call(0x47a630,p)),relations=relations))
        if before!=bytes(w.u.mem_read(0x7200000,0x300000))or seed!=bytes(w.u.mem_read(0x8a5d44,4)):raise ValueError('Original field getters mutate world/RNG')
        sources.append(dict(**source,rows=rows,forces=forces,originalDataPages=pages,wholeWorldAndRngReadOnly=True));print(json.dumps(dict(source=source['sourcePath'],rows=len(rows),forces=len(forces))),flush=True)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceManifestSha256=sha(manifest_raw),runtimeRegistrySha256=sha(registry_raw),sharedSha256=sha(shared),sources=sources,
        limits=['Actual constructor+Shared+scenario read boundary and73c500; complete opening event controllers not run','Original58/59 are employment-ban ruler/months, not permanent dislike links','Original61 is captive month counter, not raw loyalty','No current catalog or native slot arithmetic used for runtime identities','No old-save backfill or Android integration implied'],completeGoal=False)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0));print(json.dumps(dict(sources=16,rows=13600,sha256=sha(output.read_bytes()))),flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
