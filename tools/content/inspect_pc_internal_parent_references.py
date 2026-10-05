#!/usr/bin/env python3
"""Read actual internal father references and execute48bb70 across16 sources.

Internal self references are preserved as native-domain evidence. They are not
public family links and never become runtime IDs by slot arithmetic.
"""
import argparse,gzip,json,struct
from pathlib import Path
from inspect_pc_debate_flow import NativeDebateFlow
from pc_original_pe_data import load_original_data
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha

def inspect(installation,output):
    installation=installation.resolve();output_guard(installation,output)
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('Executable changed')
    folder=ROOT/'docs/handoff/20261004/session1'
    manifest_raw=(folder/'source-manifest.json').read_bytes()
    registry_raw=(folder/'scenario-person-runtime-coverage.json.gz').read_bytes()
    registry=json.loads(gzip.decompress(registry_raw))
    ids={(r['sourcePath'],r['nativeId']):r for r in registry['people']}
    shared=(installation/'Media/scenario/Scenario.s11').read_bytes();sources=[]
    for source in json.loads(manifest_raw)['scenarios']:
        raw=(installation/source['sourcePath']).read_bytes()
        if sha(raw)!=source['sourceSha256']:raise ValueError('Source changed')
        w=NativeDebateFlow(installation,exe).world;load_original_data(w.u)
        w.load(shared,True);loaded=w.load(raw);w.call(0x73c500)
        records={r['native_index']:r for r in loaded['records']if r['kind']=='officer'}
        baseline=bytes(w.u.mem_read(0x7200000,0x300000));seed=bytes(w.u.mem_read(0x8a5d44,4))
        rows=[];groups={}
        for native in range(1100):
            p=w.root+0xc0bc+native*0x190;b=bytes(w.u.mem_read(p,0x190))
            father=struct.unpack_from('<i',b,0x54)[0];valid=bool(w.call(0x47a600,p))
            public=w.call(0x4c8720,p,12)&0xffffffff;public=public if public<0x80000000 else public-0x100000000
            record=records.get(native);identity=ids.get((source['sourcePath'],native))
            if identity and (record is None or identity['recordSha256']!=record['sha256']):raise ValueError('Identity record changed')
            rows.append(dict(nativeId=native,officerId=identity['officerId']if identity else None,
                recordSha256=record['sha256']if record else None,internalFatherNativeId=father,
                publicFatherNativeId=public,internalSelfReference=father==native,originalReferenceValid=valid))
            if valid and 0<=father<1100:groups.setdefault(father,[]).append(native)
        pairs=set()
        for members in groups.values():
            for a in members:
                for b in members:pairs.add((a,b))
        # Also test actual distinct roots, asymmetry, self and inactive records.
        for a in range(0,1100,17):
            for b in [a,(a+1)%1100,116,222,365,849,850,1099]:pairs.add((a,b))
        cases=[]
        for a,b in sorted(pairs):
            expected=(0<=rows[a]['internalFatherNativeId']<1100 and rows[b]['originalReferenceValid']
                and a!=b and rows[a]['internalFatherNativeId']==rows[b]['internalFatherNativeId'])
            actual=bool(w.call(0x48bb70,b,receiver=w.root+0xc0bc+a*0x190))
            if actual!=expected:raise ValueError(f'Original sibling rule differs:{a}/{b}')
            cases.append(dict(leftNativeId=a,rightNativeId=b,sameFather=actual))
        if baseline!=bytes(w.u.mem_read(0x7200000,0x300000))or seed!=bytes(w.u.mem_read(0x8a5d44,4)):raise ValueError('Parent query mutates authority')
        sources.append(dict(**source,rows=rows,cases=cases,wholeWorldAndRngReadOnly=True))
        print(json.dumps(dict(source=source['sourcePath'],rows=len(rows),cases=len(cases))),flush=True)
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceManifestSha256=sha(manifest_raw),runtimeRegistrySha256=sha(registry_raw),sharedSha256=sha(shared),sources=sources,
        originalFunction=dict(start='0x48bb70',endExclusive='0x48bbc5',sha256=sha(exe[0x8bb70:0x8bbc5])),
        limits=['Actual constructor/Shared/source/73c500 boundary; complete opening events remain separate',
                '850 serialized plus250 constructed actors per source; constructed actors are not verified runtime officers',
                'Internal self references are not public father links; no runtime native-slot ID arithmetic',
                'Live family editing and reference invalidation require a separate saved policy'],completeGoal=False)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(gzip.compress(json_bytes(report),mtime=0))
    print(json.dumps(dict(rows=sum(len(s['rows'])for s in sources),cases=sum(len(s['cases'])for s in sources),sha256=sha(output.read_bytes()))),flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args();inspect(a.installation,a.output)
