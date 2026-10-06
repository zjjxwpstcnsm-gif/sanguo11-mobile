#!/usr/bin/env python3
"""New data-only import of original governor inputs; no sealed policy code."""
import argparse
import gzip
import hashlib
import json
import struct
import tarfile
from pathlib import Path
from inspect_pc_scenario_officers import BASE, STRIDE
from audit_pc_restoration_sources import ROOT, EXE_SHA

ARCHIVE_SHA='dd3f2c05f8b7f17aadf2bab73962b3770cf973190563efced773cf63a5c0f27c'
REGISTRY_SHA='aa231c41f3e45ff9ecdd39aec3a9d8f1d01f16fd9fd5748de140991210f6d6c3'
SITE_SHA='c53b2d2d93ebd053bfdc590eeb5ad4d5d11b856818fc6bf72307a655d57dcfbb'

def sha(raw):return hashlib.sha256(raw).hexdigest()

def build(installation,output):
    folder=ROOT/'docs/handoff/20261004/session1'
    archive=folder/'batch28-governor-original-all16.tar.gz'
    assert sha(archive.read_bytes())==ARCHIVE_SHA
    registry_raw=(folder/'scenario-person-runtime-coverage.json.gz').read_bytes()
    assert sha(registry_raw)==REGISTRY_SHA
    registry=json.loads(gzip.decompress(registry_raw))
    identities={(r['sourcePath'],r['nativeId']):r for r in registry['people']}
    manifest_raw=(folder/'source-manifest.json').read_bytes()
    sources=json.loads(manifest_raw)['scenarios']
    domains_raw=(ROOT/'docs/pc-data/scenario-domains-native.json.gz').read_bytes()
    assert sha(domains_raw)==SITE_SHA
    domains=json.loads(gzip.decompress(domains_raw));assert domains['source_executable_sha256']==EXE_SHA
    sites={}
    for row in domains['shared_source']['records']:
        if row['kind']in ['city','gate','port']:
            native=row['native_index']+{'city':0,'gate':42,'port':52}[row['kind']]
            sites[native]=row['identity']['project_id']
    assert len(sites)==87 and len(set(sites.values()))==87
    data=bytearray()
    def integer(value):data.extend(struct.pack('>i',value))
    def text(value):raw=value.encode('utf8');integer(len(raw));data.extend(raw)
    integer(0x50474f31)
    for value in [EXE_SHA,ARCHIVE_SHA,REGISTRY_SHA,SITE_SHA]:text(value)
    integer(16);proofs=[]
    with tarfile.open(archive,'r:gz')as tar:
        for index,source in enumerate(sources):
            packed=tar.extractfile(f'source-{index}.json.gz').read()
            report=json.loads(gzip.decompress(packed))
            assert report['schema']==2 and report['source']==source
            assert report['sourceExecutableSha256']==EXE_SHA and report['sourceManifestSha256']==sha(manifest_raw)
            original=(installation/source['sourcePath']).read_bytes();assert sha(original)==source['sourceSha256']
            assert len(report['people'])==1100 and len(report['sites'])==87 and len(report['districts'])==47
            for key in ['scenarioId','sourcePath','sourceVariant','sourceSha256']:text(source[key])
            text(report['sharedSha256']);integer(87)
            for native,site in enumerate(report['sites']):
                assert site['nativeId']==native
                for value in [native,sites[native],site['districtNativeId'],site['after']['4'],site['after']['14']]:integer(value)
            integer(850)
            for native,person in enumerate(report['people'][:850]):
                assert person['nativeId']==native
                identity=identities.get((source['sourcePath'],native))
                record_sha=sha(original[BASE+native*STRIDE:BASE+(native+1)*STRIDE])
                if identity:
                    assert identity['recordSha256']==record_sha and identity['sourceVariant']==source['sourceVariant']
                integer(native);integer(-1 if identity is None or identity['officerId']is None else identity['officerId']);text(record_sha)
                for key in ['owner','districtNativeId','homeNativeId','currentLocationNativeId','status','commandCapacity','leadership','war','merit']:integer(person[key])
                for key in ['allowed','resident','rosterMask15Allowed']:integer(int(person[key]))
            integer(47)
            for native,army in enumerate(report['districts']):
                assert army['nativeId']==native
                integer(native);integer(int(army['allowed']))
                for key in range(3,10):integer(army['properties'][str(key)])
                integer(army['actionPoints']) # Evidence only; not actual opening budget.
            proofs.append(dict(source=source['sourcePath'],sha256=sha(packed)))
    output=output.resolve();relative=str(output.relative_to(ROOT))
    if relative!='core/src/main/resources/pc-governor-rosters'and not relative.startswith('out/session-b/'):
        raise ValueError('Exact B output ownership required')
    output.mkdir(parents=True,exist_ok=True)
    for name,raw in [('rosters.bin.gz',gzip.compress(data,mtime=0)),('index.txt',(sha(data)+'\n').encode())]:
        p=output/name
        if p.exists()and p.read_bytes()!=raw:raise ValueError('Preserve previous import')
        p.write_bytes(raw)
    print(json.dumps(dict(rawSha256=sha(data),rawBytes=len(data),sources=16,siteJoins=1392,proofs=proofs,
                         loadedArmyBudgetIsNotOpeningBudget=True,completeGoal=False)))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();build(a.installation.resolve(),a.output)
