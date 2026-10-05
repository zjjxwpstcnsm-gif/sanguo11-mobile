#!/usr/bin/env python3
"""Import original roster/army references with checked source/person/site joins.

The explicit source selection permits isolated prototype iterations. Production
acceptance still requires all16. Loaded army AP is not an opening budget.
"""
import argparse,gzip,json,struct
from pathlib import Path
from audit_pc_restoration_sources import ROOT,EXE_SHA,json_bytes,sha
from inspect_pc_scenario_officers import BASE,STRIDE

def build(native_folder,indices,expected_registry,output,installation):
    folder=ROOT/'docs/handoff/20261004/session1';registry_raw=(folder/'scenario-person-runtime-coverage.json.gz').read_bytes()
    if sha(registry_raw)!=expected_registry:raise ValueError('Runtime identity registry changed')
    registry=json.loads(gzip.decompress(registry_raw));ids={(r['sourcePath'],r['nativeId']):r for r in registry['people']}
    manifest_raw=(folder/'source-manifest.json').read_bytes();sources=json.loads(manifest_raw)['scenarios']
    domains_raw=(ROOT/'docs/pc-data/scenario-domains-native.json.gz').read_bytes();domains=json.loads(gzip.decompress(domains_raw))
    if domains['source_executable_sha256']!=EXE_SHA:raise ValueError('Site executable identity changed')
    sites={}
    for row in domains['shared_source']['records']:
        if row['kind']in ['city','gate','port']:
            n=row['native_index']+{'city':0,'gate':42,'port':52}[row['kind']];sites[n]=row['identity']['project_id']
    if len(sites)!=87 or len(set(sites.values()))!=87:raise ValueError('Original site join incomplete')
    if sorted(set(indices))!=indices or not indices or any(i<0 or i>=16 for i in indices):raise ValueError('Explicit source indices invalid')
    proofs=[];data=bytearray();fixtures=[];election_lines=[];capacity_lines=[]
    def integer(n):data.extend(struct.pack('>i',n))
    def text(s):b=s.encode('utf8');integer(len(b));data.extend(b)
    reports=[]
    for i in indices:
        raw=(native_folder/f'source-{i}.json.gz').read_bytes();r=json.loads(gzip.decompress(raw));s=sources[i]
        if r['schema']!=2 or r['sourceExecutableSha256']!=EXE_SHA or r['sourceManifestSha256']!=sha(manifest_raw)or r['source']!=s:raise ValueError('Original roster/source join differs')
        if len(r['people'])!=1100 or len(r['sites'])!=87 or len(r['districts'])!=47:raise ValueError('Original governance domain incomplete')
        reports.append(r);proofs.append(dict(sourceIndex=i,sourcePath=s['sourcePath'],sha256=sha(raw)))
    proof_sha=sha(json_bytes(proofs));integer(0x504e4731);text(EXE_SHA);text(proof_sha);text(expected_registry);text(sha(domains_raw));integer(len(reports))
    for r in reports:
        s=r['source']
        original=(installation/s['sourcePath']).read_bytes()
        if sha(original)!=s['sourceSha256']:raise ValueError('Readonly original source changed')
        for k in ['scenarioId','sourcePath','sourceVariant','sourceSha256']:text(s[k])
        integer(47)
        for i,d in enumerate(r['districts']):
            if d['nativeId']!=i:raise ValueError('Original army order differs')
            integer(i);integer(int(d['allowed']))
            for k in range(3,10):integer(d['properties'][str(k)])
            integer(d['actionPoints'])
        integer(87)
        for i,site in enumerate(r['sites']):
            if site['nativeId']!=i:raise ValueError('Original site order differs')
            integer(i);integer(sites[i]);integer(site['before']['4']);integer(site['districtNativeId'])
            winner=site['after']['14'];identity=ids.get((s['sourcePath'],winner));officer=identity['officerId']if identity else None
            if winner>=0 and(officer is None or officer<0):raise ValueError('Original elected governor identity is unknown:'+s['sourcePath']+'/'+str(winner))
            election_lines.append(f'{s["scenarioId"]}\t{i}\t{sites[i]}\t{winner}\t{-1 if officer is None else officer}')
        integer(850)
        for i,p in enumerate(r['people'][:850]):
            identity=ids.get((s['sourcePath'],i));officer=identity['officerId']if identity else None
            if p['nativeId']!=i or identity and identity['sourceVariant']!=s['sourceVariant']:raise ValueError('Original person identity differs')
            record_sha=sha(original[BASE+i*STRIDE:BASE+(i+1)*STRIDE])
            if identity and identity['recordSha256']!=record_sha:raise ValueError('Checked original record identity differs')
            integer(i);integer(-1 if officer is None else officer);text(record_sha)
            for k in ['owner','districtNativeId','homeNativeId','currentLocationNativeId','status']:
                integer(p[k])
            if officer is not None and officer>=0 and p['allowed']and 0<=p['owner']<=41 and p['rosterMask15Allowed']and p['resident']:
                capacity_lines.append(f'{s["scenarioId"]}\t{officer}\t{i}\t{p["commandCapacity"]}\t{p["leadership"]}\t{p["war"]}')
        fixtures.append(dict(sourceId=s['scenarioId'],people=r['people'],sites=r['sites']))
    output.mkdir(parents=True,exist_ok=False);(output/'governance.bin.gz').write_bytes(gzip.compress(data,mtime=0));(output/'index.txt').write_text(sha(data)+'\n')
    for name,lines in [('election-fixtures.tsv',election_lines),('capacity-fixtures.tsv',capacity_lines)]:
        (output/name).write_text('# original-proof-manifest='+proof_sha+'\n'+'\n'.join(lines)+'\n')
    (output/'original-fixtures.json.gz').write_bytes(gzip.compress(json_bytes(dict(proofManifestSha256=proof_sha,sources=fixtures)),mtime=0))
    report=dict(sourceProofs=proofs,proofManifestSha256=proof_sha,registrySha256=expected_registry,siteJoinSha256=sha(domains_raw),binarySha256=sha(data),bytes=len(data),sources=len(reports),all16Imported=len(reports)==16,loadedArmyBudgetIsNotOpeningBudget=True,completeGoal=False)
    (output/'build-report.json').write_bytes(json_bytes(report));print(json.dumps(report))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('native_folder',type=Path);p.add_argument('--sources',required=True);p.add_argument('--expected-registry-sha',required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--installation',type=Path,required=True);a=p.parse_args();build(a.native_folder,[int(i)for i in a.sources.split(',')],a.expected_registry_sha,a.output,a.installation.resolve())
