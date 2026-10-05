#!/usr/bin/env python3
"""Build only proven source text, with per-source identity and explicit gaps.

This is not a scenario importer. It never changes numeric character data, media
manifests or old saves. Regeneration requires pinned reports and current PC bytes.
"""
import argparse
import csv
import gzip
import json
import struct
from pathlib import Path
from audit_pc_restoration_sources import ROOT,EXE_SHA,output_guard,json_bytes,sha


def read_report(path):
    raw=path.read_bytes();return json.loads(gzip.decompress(raw)),sha(raw)


def build(installation,fields_path,messages_path,output):
    output_guard(installation,output)
    if sha((installation/'san11pk.exe').read_bytes())!=EXE_SHA:raise ValueError('Executable changed')
    fields,fields_sha=read_report(fields_path);messages,messages_sha=read_report(messages_path)
    manifest=json.loads((ROOT/'docs/handoff/20261004/session1/source-manifest.json').read_bytes())
    metadata,metadata_sha=read_report(ROOT/'docs/handoff/20261004/session1/officer-metadata-manifest.json.gz')
    for report in (fields,messages,manifest):
        if report['sourceExecutableSha256']!=EXE_SHA:raise ValueError('Cross-executable source report')
    prior_raw=(ROOT/'docs/pc-data/scenario-officers-native.json.gz').read_bytes()
    if fields['identityAuditSha256']!=sha(prior_raw):raise ValueError('Identity provenance changed')
    prior=json.loads(gzip.decompress(prior_raw));raw_records={(r['source'],r['native_index']):r for r in prior['records']}
    if len(fields['sources'])!=16 or len(metadata['officers'])!=13600:raise ValueError('Incomplete source set')
    infos={(r['sourcePath'],r['nativeId']):r for r in metadata['officers']}
    source_infos={r['sourcePath']:r for r in manifest['scenarios']}
    message_rows={r['messageId']:r for r in messages['messages']}
    resource=(installation/messages['resourcePath']).read_bytes()
    if sha(resource)!=messages['resourceSha256']:raise ValueError('Biography resource changed')
    with (ROOT/'core/src/main/resources/content/officers.tsv').open() as f:
        catalog={int(r['id']):r['name'] for r in csv.DictReader(f,delimiter='\t')}
    with (ROOT/'core/src/main/resources/content/aliases.tsv').open() as f:
        aliases={int(r['id']):r['alias'] for r in csv.DictReader(f,delimiter='\t')}
    canonical={r['nativeId']:r['serializedNameRawHex'] for r in fields['sources'][0]['officers']}
    data=bytearray();summary=[];records=[]
    def number(n):data.extend(struct.pack('>i',n))
    def text(s):
        raw=s.encode('utf-8');number(len(raw));data.extend(raw)
    number(0x50435431);number(len(fields['sources']))
    for source in fields['sources']:
        source_info=source_infos[source['path']];pc=(installation/source['path']).read_bytes()
        if sha(pc)!=source['sha256'] or source['sha256']!=source_info['sourceSha256']:raise ValueError('Source changed')
        people=[]
        for row in source['officers']:
            native=row['nativeId'];key=(source['path'],native);info=infos[key];original=raw_records[key]
            raw=pc[original['offset']:original['offset']+152]
            if sha(raw)!=row['recordSha256'] or row['recordSha256']!=info['recordSha256'] or row['officerId']!=info['officerId']:
                raise ValueError('Record identity/source disagreement')
            if row['officerId'] is None:continue
            relocated=info['identityStatus']=='relocated_identity_verified' and source['path']=='Media/scenario/Scen014.S11' and (native,row['officerId']) in ((279,10333),(333,10279))
            if (info['identityStatus']!='name_birth_sex_verified' and not relocated) or not row['loadBoundaryValid']:
                raise ValueError('Unverified source person was assigned project ID: '+str((key,info['identityStatus'],row['loadBoundaryValid'])))
            courtesy=row['names']['courtesy_name'];unknown=[]
            if courtesy['unknown']:unknown.extend(['courtesyName','courtesyNameGaiji:'+courtesy['rawHex']])
            selector=row['biographySelector'];biography='';bio_sha=''
            if 'messageId' not in selector:unknown.append('biographySelector')
            elif row['serializedNameRawHex']!=canonical[native]:unknown.append('biographyIdentityDisagreement')
            elif selector['resourceNumber']!=2 or selector['messageId'] not in message_rows:unknown.append('biographyResourceIndex')
            else:
                msg=message_rows[selector['messageId']];bio_sha=msg['renderedSha256']
                if sha(bytes.fromhex(msg['renderedRawHex']))!=bio_sha:raise ValueError('Biography render changed')
                for span in msg['spans']:
                    if span['kind']=='text':biography+=span['text']
                    elif span['kind']=='unknown_glyph':biography+='〔未解码字形 '+span['rawHex']+'〕'
                    elif span['kind']!='format':raise ValueError('Unexamined biography control')
                unknown.extend('biographyGaiji:'+v for v in msg['unknownGlyphs'])
            if not biography:unknown.append('biography')
            unknown.append('activeResourcePriority')
            p=dict(officerId=row['officerId'],nativeId=native,worldName=catalog[row['officerId']],sourceVariant=info['sourceVariant'],
                sourcePath=source['path'],sourceSha=source['sha256'],recordSha=row['recordSha256'],courtesy=courtesy['text'] or '',
                courtesyRaw=courtesy['rawHex'],biography=biography,biographyResourceSha=messages['resourceSha256'],biographyRenderedSha=bio_sha,
                unknown=unknown,acceptedNames=sorted({catalog[row['officerId']],aliases.get(row['officerId'],catalog[row['officerId']])}))
            people.append(p);records.append(p)
        text(source_info['scenarioId']);text(source_info['name']);text(source['path']);text(source['sha256'])
        for n in source_info['date']:number(n)
        number(len(people))
        for p in people:
            number(p['officerId']);number(p['nativeId'])
            for key in ('worldName','sourceVariant','sourcePath','sourceSha','recordSha','courtesy','courtesyRaw','biography','biographyResourceSha','biographyRenderedSha'):text(p[key])
            number(len(p['unknown']))
            for s in p['unknown']:text(s)
            number(len(p['acceptedNames']))
            for s in p['acceptedNames']:text(s)
        summary.append(dict(sourcePath=source['path'],sourceSha256=source['sha256'],sourceId=source_info['scenarioId'],people=len(people),
            biographies=sum(bool(p['biography']) for p in people),courtesyNames=sum(bool(p['courtesy']) for p in people)))
    if len(records)!=10656:raise ValueError('Strict verified identity count changed')
    output.mkdir(parents=True,exist_ok=True);packed=gzip.compress(bytes(data),mtime=0)
    (output/'source-text.bin.gz').write_bytes(packed);(output/'index.txt').write_text(sha(data)+'\n')
    report=dict(schema=1,sourceExecutableSha256=EXE_SHA,fieldsReportSha256=fields_sha,messagesReportSha256=messages_sha,
        metadataSha256=metadata_sha,binarySha256=sha(data),packedSha256=sha(packed),bytes=len(data),packedBytes=len(packed),
        sources=summary,verifiedIdentityRecords=len(records),biographyIdentityDisagreements=[dict(officerId=p['officerId'],nativeId=p['nativeId'],sourcePath=p['sourcePath']) for p in records if 'biographyIdentityDisagreement' in p['unknown']],
        limits=['Explicit opening-only text attachment; no numeric/state/portrait import',
            'Active resource priority and all scenario starts remain unverified',
            'Gaiji placeholders are explicit missing glyphs, not claimed original Unicode',
            'All source records remain distinct; no project ID arithmetic selects messages'])
    (output/'build-report.json').write_bytes(json_bytes(report));return report


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('installation',type=Path);p.add_argument('--fields',type=Path,required=True)
    p.add_argument('--messages',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();print(json.dumps(build(a.installation,a.fields,a.messages,a.output),ensure_ascii=False))
