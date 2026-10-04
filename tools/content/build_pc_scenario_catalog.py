#!/usr/bin/env python3
"""Convert independently audited installation candidates into distinct runtime inputs.

Unknown origins/activation/events remain explicit. No internet values or roster
slot-to-project arithmetic. Serialized bytes, checked identity, original getters
and every source report SHA are guarded before deterministic binary generation.
"""
import argparse,csv,gzip,json,struct
from pathlib import Path
from audit_pc_restoration_sources import ROOT,EXE_SHA,json_bytes,sha,output_guard
from pc_resources import Archive

GAPS=['sourceOriginAndActivePriority','externalDocumentsExpansionCoverage','completeSharedNlsSortAndStartup',
      'openingEventConditionsAndEffects','nativeMenuSettingsAndRandomSeed','fullDiplomacyAndTechnologyBinding',
      'fullDistrictBudgetAndAssignments','sourceFacilitiesAndTreasurePlacement','nativeUnappearedConditions',
      'extraTemplateAncientAndEventNpcActivation','gatePortOrderNotApplicable','nativeCompleteSkillEffectParity','completeStartupMerchantPrices','nativeTitleAcquisitionEvents']

def read(path):
    raw=path.read_bytes();return json.loads(gzip.decompress(raw)),sha(raw)

def build(installation,references,output):
    installation=installation.resolve();output_guard(installation,output)
    exe=(installation/'san11pk.exe').read_bytes()
    if sha(exe)!=EXE_SHA:raise ValueError('EXE changed')
    if exe[0x4839f0-0x400000:0x4839fc-0x400000].hex()!='8b4424040fb680b0c27900c3':raise ValueError('Native region parent getter changed')
    parents=list(exe[0x79c2b0-0x400000:0x79c2b0-0x400000+87])
    archive=Archive(installation/'Media/san11pkres.bin');shex=archive.read(4791);archive.close()
    if sha(shex)!='c726989df91d44c99ea3c3f613d41e7e725c43002a9775dfac2e43516f5c6112':raise ValueError('SHEX changed')
    folder=ROOT/'docs/handoff/20261004/session1';refs,refs_sha=read(references)
    fields,fields_sha=read(folder/'source-fields-native.json.gz');layered,layered_sha=read(folder/'layered-scenario-fields.json.gz')
    domains,domains_sha=read(ROOT/'docs/pc-data/scenario-domains-native.json.gz')
    manifest_bytes=(folder/'source-manifest.json').read_bytes();manifest=json.loads(manifest_bytes)
    if refs['sourceManifestSha256']!=sha(manifest_bytes)or any(r['sourceExecutableSha256']!=EXE_SHA for r in [refs,fields,layered]):raise ValueError('Report provenance changed')
    if domains['source_executable_sha256']!=EXE_SHA:raise ValueError('Site identity executable differs')
    shared=(installation/'Media/scenario/Scenario.s11').read_bytes()
    if domains['shared_source']['sha256']!=sha(shared):raise ValueError('Shared changed')
    site_mapping={}
    for row in domains['shared_source']['records']:
        if row['kind']not in ['city','gate','port']:continue
        native=row['native_index']+{'city':0,'gate':42,'port':52}[row['kind']]
        ident=row['identity'];site_mapping[native]=(ident['project_id'],ident['source_name'])
    if len(site_mapping)!=87 or len(set(x[0]for x in site_mapping.values()))!=87:raise ValueError('Site identities incomplete')
    by_refs={s['path']:s for s in refs['sources']};by_fields={s['path']:s for s in fields['sources']};by_layered={s['path']:s for s in layered['sources']}
    if any(len(d)!=16 for d in [by_refs,by_fields,by_layered]):raise ValueError('All16 sources required')
    opaque={};canonical=set()
    for s in fields['sources']:
        for row in s['officers'][:670]:
            if row['officerId'] is not None:canonical.add(row['officerId'])
            else:
                props=row['properties'];key=row['serializedNameRawHex']+':'+str(props['8']['value'])+':'+str(props['6']['value']);opaque.setdefault(key,100000+int(sha(key.encode())[:8],16)%800000)
    if len(opaque)!=4 or len(set(opaque.values()))!=4 or set(opaque.values())&canonical:raise ValueError('Ambiguous/source-only identity allocation')
    data=bytearray();sources_report=[];coverage=[];inputs=[]
    def integer(n):data.extend(struct.pack('>i',n))
    def boolean(b):data.extend(bytes([int(b)]))
    def text(s):raw=s.encode('utf-8');integer(len(raw));data.extend(raw)
    def properties(p):
        integer(len(p))
        for k,v in sorted(p.items()):integer(k);integer(v)
    integer(0x50434f31);text(EXE_SHA);integer(len(manifest['scenarios']))
    for entry in manifest['scenarios']:
        source_start=len(data)
        path=entry['sourcePath'];raw=(installation/path).read_bytes();ref=by_refs[path];f=by_fields[path];layer=by_layered[path]
        if any(s['sha256']!=sha(raw)for s in [ref,f,layer])or entry['sourceSha256']!=sha(raw)or ref['sharedSha256']!=sha(shared):raise ValueError('Mixed source bytes '+path)
        for v in [entry['scenarioId'],entry['name'],entry['sourceVariant'],path,sha(raw),sha(shared)]:text(v)
        for v in entry['date']:integer(v)
        integer(len(GAPS))
        for v in GAPS:text(v)
        integer(87)
        for site in ref['sites']:
            native=site['nativeBuildingId'];identity=site_mapping[native]
            if site['name']!=identity[1]:raise ValueError('Original site name does not match checked identity')
            integer(native);integer(identity[0]);text(identity[1])
            properties({int(k):v['value']for k,v in site['properties'].items()})
            integer(layer['domains']['city'][native]['properties']['10']['value']if native<42 else 0);integer(site_mapping[site['parentNativeCityId']][0])
        integer(47)
        for force in layer['domains']['force']:
            integer(force['nativeId']);boolean(force['activeAtReadBoundary']);properties({int(k):v['value']for k,v in force['properties'].items()if 'value'in v})
        plots=[]
        for x in range(200):
            for y in range(200):
                off=8+(x*200+y)*11
                if shex[off+5]!=1:continue
                region=shex[off+1]
                if region>=87 or parents[region]>=42:raise ValueError('Unmapped development source region')
                plots.append((site_mapping[parents[region]][0],x,y))
        if len(plots)!=591:raise ValueError('Development source coverage changed')
        integer(len(plots))
        for plot in plots:
            for value in plot:integer(value)
        integer(850);strict=0;states={}
        for row,native in zip(f['officers'],ref['people']):
            n=row['nativeId']
            if native['nativeId']!=n:raise ValueError('Actor join changed')
            record=raw[row['recordOffset']:row['recordOffset']+152]
            if sha(record)!=row['recordSha256']:raise ValueError('Record differs')
            strict_id=n<670 and row['officerId']is not None
            props={int(k):v['value']for k,v in row['properties'].items()if 'value'in v}
            props.update({int(k):v['value']for k,v in native['properties'].items()})
            ident=-1
            if n<670:
                if not row['loadBoundaryValid']or not native['valid']:raise ValueError('Historical record invalid')
                if strict_id:
                    if row['identity']not in ['name_birth_sex_verified','relocated_identity_verified']:raise ValueError('Unverified canonical identity')
                    ident=row['officerId'];strict+=1
                else:
                    key=row['serializedNameRawHex']+':'+str(props[8])+':'+str(props[6]);ident=opaque[key]
            name=row['names']['full_name']['text']
            if name is None:
                surname=row['names']['surname']['text']or'';given=row['names']['given_name'];name=surname+'〔原字形 '+given['rawHex']+'〕'
            gaps=list(row['coverage']['unknown'])
            if not strict_id:gaps.append('canonicalIdentityUnmapped'if n<670 else 'extraSlotActivationAndIdentity')
            gaps.append('notInvokedProperties:'+','.join(map(str,row['coverage']['notYetInvokedPropertyIds'])))
            gaps.extend('runtimeRelationUnbound:%d:%d'%(key,props[key])for key in [12,13,15,16,106,107,108,109,110,111,112,113,114,115]if props.get(key,-1)>=670)
            gaps+=['unappearedConditionEffectBinding','fullNativeSkillEffects','fullOtherPersonFieldBinding']
            integer(n);integer(ident);boolean(strict_id);text(name or'');text(row['serializedNameRawHex']);text(row['recordSha256']);text(row['names']['courtesy_name']['text']or'');text(row['names']['courtesy_name']['rawHex']);properties(props);integer(len(gaps))
            for v in gaps:text(v)
            if n<670:states[str(props[20])]=states.get(str(props[20]),0)+1
            coverage.append(dict(sourceVariant=entry['sourceVariant'],sourcePath=path,nativeId=n,officerId=None if ident<0 else ident,strictCanonicalIdentity=strict_id,sourceOnlyIdentity=ident>=100000,recordSha256=row['recordSha256'],originalFieldIds=sorted(props),unknown=gaps,complete=False))
        if strict!=666:raise ValueError('Strict canonical coverage changed')
        inputs.append((entry,bytes(data[source_start:])))
        sources_report.append(dict(sourceId=entry['scenarioId'],sourcePath=path,sourceSha256=sha(raw),sites=87,historicalRecords=670,strictCanonicalIdentities=666,sourceOnlyIdentities=4,extraSlots=180,statusCounts=states,playerForceCandidates=sum(x['activeAtReadBoundary']and x['nativeId']<42 for x in layer['domains']['force']),completeOpening=False,unknown=GAPS))
    output.mkdir(parents=True,exist_ok=True);packed=gzip.compress(bytes(data),mtime=0);(output/'openings.bin.gz').write_bytes(packed)
    data=bytearray();integer(0x50434931);text(EXE_SHA);integer(len(inputs));input_report=[]
    for (entry,raw_input),source_report in zip(inputs,sources_report):
        for v in [entry['scenarioId'],entry['name'],entry['sourceVariant'],entry['sourcePath'],entry['sourceSha256'],sha(shared)]:text(v)
        for v in entry['date']:integer(v)
        integer(len(GAPS))
        for v in GAPS:text(v)
        integer(666);integer(source_report['playerForceCandidates']);text(sha(raw_input))
        path=output/(entry['scenarioId']+'.bin.gz');path.write_bytes(gzip.compress(raw_input,mtime=0));input_report.append(dict(path=path.name,decodedSha256=sha(raw_input),packedSha256=sha(path.read_bytes()),bytes=len(raw_input)))
    catalog=gzip.compress(bytes(data),mtime=0);(output/'catalog.bin.gz').write_bytes(catalog);(output/'index.txt').write_text(sha(data)+'\n')
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,sourceManifestSha256=sha(manifest_bytes),fieldsReportSha256=fields_sha,layeredReportSha256=layered_sha,siteIdentityReportSha256=domains_sha,referencesReportSha256=refs_sha,shexSha256=sha(shex),parentNativeGetterCodeSha256=sha(exe[0x4839f0-0x400000:0x4839fc-0x400000]),parentNativeTableSha256=sha(bytes(parents)),developmentSourcePlots=591,catalogDecodedSha256=sha(data),catalogPackedSha256=sha(catalog),inputs=input_report,packedAggregateSha256=sha(packed),packedAggregateBytes=len(packed),sourceOnlyIdentityAllocation=opaque,sources=sources_report,completeGoal=False,unknown=GAPS)
    (output/'build-report.json').write_bytes(json_bytes(report));(output/'person-runtime-coverage.json.gz').write_bytes(gzip.compress(json_bytes(dict(schema=1,people=coverage,strictCanonicalIdentities=10656,sourceOnlyRecords=64,extraSlots=2880,complete=False)),mtime=0));return report
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--references',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();print(json.dumps(build(a.installation,a.references,a.output),ensure_ascii=False))
