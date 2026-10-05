#!/usr/bin/env python3
"""Append only64 committed font identities to media catalogs; never metadata.

Preserve runtime IDs, original pixels and all existing identity records.
Rebuild compact lookup from independently executed original age outputs.
"""
import argparse,copy,hashlib,json,struct
from pathlib import Path

def sha(raw):return hashlib.sha256(raw).hexdigest()
def text(value):raw=value.encode('utf-8');return struct.pack('>H',len(raw))+raw
def extend(args):
    if args.output.exists():raise ValueError('Fresh output required')
    owner=json.loads(args.metadata.read_bytes());native=json.loads(args.native.read_bytes());base=json.loads(args.manifest.read_bytes());dynamic=json.loads(args.dynamic_manifest.read_bytes())
    if owner.get('producer')!='session1 identity authority' or len(owner['rows'])!=64 or native['requestCount']!=64 or base['identityJoins']!=10656 or base['decodedImages']!=2892 or native['sourceFaceSha256']!=base['sourceFaceSha256']:raise ValueError('Exact original baseline/authority required')
    allowed={156234:184,844857:229,598828:249,850922:616}
    approved={(r['officerId'],r['nativeId'],r['sourceVariant']):r for r in owner['rows']}
    if len(approved)!=64 or {r['officerId']:r['nativeId'] for r in owner['rows']}!=allowed:raise ValueError('Runtime identities changed')
    keys={(r['officerId'],r['nativeId'],r['sourceVariant']) for r in base['identities']};old_rows=copy.deepcopy(base['identities'])
    for row in native['entries']:
        key=(row['officerId'],row['nativeId'],row['sourceVariant']);identity=approved.get(key)
        if identity is None or key in keys:raise ValueError('Missing authority or duplicate source')
        for name in ['sourcePath','sourceSha256','recordSha256','birth']:
            if row[name]!=identity[name]:raise ValueError('Native identity source differs')
        if row['sexRaw']!=identity['sex']:raise ValueError('Native sex differs')
        result={k:row[k] for k in ['officerId','nativeId','sourceVariant','sourcePath','sourceSha256','recordSha256','identityStatus','faceId','sexRaw','birth','ageThreshold','dynamicSelector','voiceTypeRaw','ageBoundaries']}
        result['canonicalOfficerIdEvidenceOnly']=identity['canonicalOfficerId'];base['identities'].append(result);keys.add(key)
    if base['identities'][:10656]!=old_rows or len(keys)!=10720:raise ValueError('Existing media records changed')
    base['identityJoins']=10720;base['supplementalIdentityAuthority']=dict(commit=args.metadata_commit,metadataSha256=sha(args.metadata.read_bytes()),nativePortraitReportSha256=sha(args.native.read_bytes()),runtimeIdsPreserved=True,legacySourceMissingRemainsUnknown=True)
    variants=sorted({(r['sourceVariant'],r['sourcePath'],r['sourceSha256']) for r in base['identities']});indices={r:i for i,r in enumerate(variants)}
    lookup=bytearray(b'PCDY0001'+struct.pack('>II',len(variants),len(base['identities'])))
    for variant,path,digest in variants:lookup.extend(text(variant)+text(path)+bytes.fromhex(digest))
    for row in base['identities']:
        older=next(b['dynamicSelector'] for b in row['ageBoundaries'] if b['age']>=row['ageThreshold']);index=indices[(row['sourceVariant'],row['sourcePath'],row['sourceSha256'])]
        lookup.extend(struct.pack('>iiHihBHH',row['officerId'],row['nativeId'],index,row['faceId'],row['birth'],row['ageThreshold'],row['dynamicSelector'],older)+bytes.fromhex(row['recordSha256']))
    raw=json.dumps(base,separators=(',',':'),ensure_ascii=False).encode('utf-8')
    dynamic['approvedPortraitManifestSha256']=sha(raw);dynamic['compactLookup'].update(sha256=sha(lookup),bytes=len(lookup),identityCount=10720,sourceRows='10656 original approved rows plus64 committed font identities; original native age selectors')
    args.output.mkdir(parents=True);(args.output/'media-manifest.json').write_bytes(raw);(args.output/'dynamic-lookup.pcd').write_bytes(lookup);(args.output/'dynamic-media-manifest.json').write_text(json.dumps(dynamic,indent=2)+'\n')
    report=dict(existingRowsByteEquivalent=True,existingPixelsManifestByteEquivalent=base['images']==json.loads(args.manifest.read_bytes())['images'],identityJoins=10720,addedIdentityJoins=64,variants=len(variants),lookupSha256=sha(lookup),metadataCommit=args.metadata_commit,metadataSha256=sha(args.metadata.read_bytes()),nativeReportSha256=sha(args.native.read_bytes()),normalRuntimeIntegrationProven=False)
    (args.output/'supplement-report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ['metadata','native','manifest','dynamic-manifest','output']:p.add_argument('--'+name,type=Path,required=True)
    p.add_argument('--metadata-commit',required=True);extend(p.parse_args())
